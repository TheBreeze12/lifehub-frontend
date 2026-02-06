package com.example.lifehub.ai

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

/**
 * ONNX Runtime 推理工具类
 *
 * 提供端侧AI模型的加载和推理能力，支持：
 * - 从assets或文件系统加载ONNX/ORT模型
 * - 配置推理线程数和执行提供者（CPU/GPU）
 * - 浮点张量的创建和推理
 * - 图像预处理辅助方法（归一化、缩放等）
 * - 推理结果后处理（argmax、topK等）
 *
 * 使用示例：
 * ```kotlin
 * val inference = OnnxInference(context)
 * val config = OnnxModelConfig(
 *     modelName = "ocr_det",
 *     modelPath = "models/ocr_det.onnx",
 *     inputNames = listOf("input"),
 *     outputNames = listOf("output"),
 *     inputShape = longArrayOf(1, 3, 640, 640)
 * )
 * inference.loadModel(config)
 * val result = inference.runInference("input", floatArrayData, longArrayOf(1, 3, 640, 640))
 * inference.close()
 * ```
 *
 * @param context Android上下文，用于从assets加载模型文件
 */
class OnnxInference(private val context: Context) {

    companion object {
        /** 支持的模型文件扩展名 */
        val SUPPORTED_EXTENSIONS = setOf("onnx", "ort")

        /** 最大推理线程数 */
        const val MAX_THREADS = 8

        /**
         * 归一化线程数到合理范围 [1, MAX_THREADS]
         * @param threads 原始线程数
         * @return 归一化后的线程数
         */
        fun normalizeThreadCount(threads: Int): Int {
            return threads.coerceIn(1, MAX_THREADS)
        }

        /**
         * 验证模型路径是否为支持的格式
         * @param path 模型文件路径
         * @return 是否为合法的ONNX模型路径
         */
        fun isValidModelPath(path: String): Boolean {
            if (path.isBlank()) return false
            val extension = path.substringAfterLast('.', "").lowercase()
            return extension in SUPPORTED_EXTENSIONS
        }

        /**
         * 像素值归一化到 [minVal, maxVal] 范围
         * 公式：normalized = pixel / 255.0 * (maxVal - minVal) + minVal
         *
         * @param pixel 原始像素值 [0, 255]
         * @param minVal 归一化最小值
         * @param maxVal 归一化最大值
         * @return 归一化后的浮点值
         */
        fun normalizePixelValue(pixel: Int, minVal: Float, maxVal: Float): Float {
            return pixel / 255f * (maxVal - minVal) + minVal
        }

        /**
         * 使用均值和标准差进行像素归一化（ImageNet风格）
         * 公式：normalized = (pixel / 255.0 - mean) / std
         *
         * @param pixel 原始像素值 [0, 255]
         * @param mean 通道均值
         * @param std 通道标准差
         * @return 归一化后的浮点值
         */
        fun normalizePixelValueWithMeanStd(pixel: Int, mean: Float, std: Float): Float {
            return (pixel / 255f - mean) / std
        }

        /**
         * 计算保持宽高比的缩放尺寸
         * 将图像最长边缩放到targetSize，短边按比例缩放
         *
         * @param originalWidth 原始宽度
         * @param originalHeight 原始高度
         * @param targetSize 目标最长边大小
         * @return Pair(新宽度, 新高度)
         */
        fun calculateResizedDimensions(
            originalWidth: Int,
            originalHeight: Int,
            targetSize: Int
        ): Pair<Int, Int> {
            if (originalWidth <= 0 || originalHeight <= 0) {
                return Pair(targetSize, targetSize)
            }

            val scale = targetSize.toFloat() / maxOf(originalWidth, originalHeight)
            val newWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
            val newHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
            return Pair(newWidth, newHeight)
        }

        /**
         * 获取概率数组中最大值的索引
         * @param probs 概率数组
         * @return 最大值索引，空数组返回-1
         */
        fun argmax(probs: FloatArray): Int {
            if (probs.isEmpty()) return -1
            var maxIdx = 0
            var maxVal = probs[0]
            for (i in 1 until probs.size) {
                if (probs[i] > maxVal) {
                    maxVal = probs[i]
                    maxIdx = i
                }
            }
            return maxIdx
        }

        /**
         * 获取概率数组中前K大值的索引（降序排列）
         * @param probs 概率数组
         * @param k 返回的索引数量
         * @return 前K大值的索引列表
         */
        fun topKIndices(probs: FloatArray, k: Int): List<Int> {
            if (probs.isEmpty()) return emptyList()
            val actualK = minOf(k, probs.size)
            return probs.indices
                .sortedByDescending { probs[it] }
                .take(actualK)
        }
    }

    // ONNX Runtime 环境（单例，整个应用共享）
    private var ortEnvironment: OrtEnvironment? = null

    // 已加载的会话缓存：modelName -> OrtSession
    private val sessions = mutableMapOf<String, OrtSession>()

    // 当前模型加载状态
    private var _modelLoadState: ModelLoadState = ModelLoadState.NotLoaded
    val modelLoadState: ModelLoadState get() = _modelLoadState

    /**
     * 初始化ONNX Runtime环境
     * 应在应用启动时调用一次
     */
    fun initEnvironment() {
        if (ortEnvironment == null) {
            ortEnvironment = OrtEnvironment.getEnvironment()
        }
    }

    /**
     * 从assets目录加载ONNX模型
     *
     * @param config 模型配置
     * @throws IllegalArgumentException 模型路径无效
     * @throws RuntimeException 模型加载失败
     */
    suspend fun loadModel(config: OnnxModelConfig) = withContext(Dispatchers.IO) {
        require(isValidModelPath(config.modelPath)) {
            "不支持的模型格式: ${config.modelPath}，支持的格式: $SUPPORTED_EXTENSIONS"
        }

        _modelLoadState = ModelLoadState.Loading

        try {
            initEnvironment()
            val env = ortEnvironment ?: throw RuntimeException("ONNX Runtime环境初始化失败")

            // 从assets读取模型文件到临时目录
            val modelBytes = context.assets.open(config.modelPath).use { it.readBytes() }

            // 配置会话选项
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(normalizeThreadCount(config.numThreads))
                // CPU执行优先，保证兼容性
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            // 创建推理会话
            val session = env.createSession(modelBytes, sessionOptions)
            sessions[config.modelName] = session

            _modelLoadState = ModelLoadState.Loaded(config.modelName)
        } catch (e: Exception) {
            _modelLoadState = ModelLoadState.Error("模型加载失败: ${e.message}")
            throw RuntimeException("加载模型 ${config.modelName} 失败", e)
        }
    }

    /**
     * 从文件系统加载ONNX模型
     *
     * @param config 模型配置
     * @param modelFile 模型文件
     * @throws IllegalArgumentException 模型路径无效或文件不存在
     * @throws RuntimeException 模型加载失败
     */
    suspend fun loadModelFromFile(config: OnnxModelConfig, modelFile: File) = withContext(Dispatchers.IO) {
        require(modelFile.exists()) { "模型文件不存在: ${modelFile.absolutePath}" }
        require(isValidModelPath(modelFile.name)) {
            "不支持的模型格式: ${modelFile.name}"
        }

        _modelLoadState = ModelLoadState.Loading

        try {
            initEnvironment()
            val env = ortEnvironment ?: throw RuntimeException("ONNX Runtime环境初始化失败")

            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(normalizeThreadCount(config.numThreads))
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            val session = env.createSession(modelFile.absolutePath, sessionOptions)
            sessions[config.modelName] = session

            _modelLoadState = ModelLoadState.Loaded(config.modelName)
        } catch (e: Exception) {
            _modelLoadState = ModelLoadState.Error("模型加载失败: ${e.message}")
            throw RuntimeException("加载模型 ${config.modelName} 失败", e)
        }
    }

    /**
     * 执行模型推理
     *
     * @param modelName 模型名称（loadModel时指定的名称）
     * @param inputName 输入张量名称
     * @param inputData 输入数据（浮点数组）
     * @param inputShape 输入形状
     * @return InferenceResult 推理结果
     */
    suspend fun runInference(
        modelName: String,
        inputName: String,
        inputData: FloatArray,
        inputShape: LongArray
    ): InferenceResult = withContext(Dispatchers.IO) {
        val session = sessions[modelName]
            ?: return@withContext InferenceResult.Error("模型 $modelName 未加载")

        val env = ortEnvironment
            ?: return@withContext InferenceResult.Error("ONNX Runtime环境未初始化")

        try {
            val startTime = System.currentTimeMillis()

            // 创建输入张量
            val tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(inputData),
                inputShape
            )

            // 执行推理
            val result = session.run(mapOf(inputName to tensor))
            val inferenceTime = System.currentTimeMillis() - startTime

            // 提取输出
            val outputs = mutableMapOf<String, Any>()
            for ((name, value) in result) {
                val onnxValue = value
                if (onnxValue is OnnxTensor) {
                    outputs[name] = onnxValue.floatBuffer.array()
                }
            }

            // 释放资源
            tensor.close()
            result.close()

            InferenceResult.Success(
                outputs = outputs,
                inferenceTimeMs = inferenceTime
            )
        } catch (e: Exception) {
            InferenceResult.Error(
                message = "推理失败: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * 执行多输入模型推理
     *
     * @param modelName 模型名称
     * @param inputs 输入映射 Map<inputName, Pair<data, shape>>
     * @return InferenceResult 推理结果
     */
    suspend fun runInferenceMultiInput(
        modelName: String,
        inputs: Map<String, Pair<FloatArray, LongArray>>
    ): InferenceResult = withContext(Dispatchers.IO) {
        val session = sessions[modelName]
            ?: return@withContext InferenceResult.Error("模型 $modelName 未加载")

        val env = ortEnvironment
            ?: return@withContext InferenceResult.Error("ONNX Runtime环境未初始化")

        val tensors = mutableListOf<OnnxTensor>()

        try {
            val startTime = System.currentTimeMillis()

            // 创建所有输入张量
            val inputMap = mutableMapOf<String, OnnxTensor>()
            for ((name, dataAndShape) in inputs) {
                val (data, shape) = dataAndShape
                val tensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(data),
                    shape
                )
                tensors.add(tensor)
                inputMap[name] = tensor
            }

            // 执行推理
            val result = session.run(inputMap)
            val inferenceTime = System.currentTimeMillis() - startTime

            // 提取输出
            val outputs = mutableMapOf<String, Any>()
            for ((name, value) in result) {
                if (value is OnnxTensor) {
                    outputs[name] = value.floatBuffer.array()
                }
            }

            result.close()

            InferenceResult.Success(
                outputs = outputs,
                inferenceTimeMs = inferenceTime
            )
        } catch (e: Exception) {
            InferenceResult.Error(
                message = "多输入推理失败: ${e.message}",
                exception = e
            )
        } finally {
            tensors.forEach { it.close() }
        }
    }

    /**
     * 获取已加载模型的输入信息
     * @param modelName 模型名称
     * @return 输入名称和形状的映射，模型未加载则返回null
     */
    fun getModelInputInfo(modelName: String): Map<String, LongArray>? {
        val session = sessions[modelName] ?: return null
        val inputInfo = mutableMapOf<String, LongArray>()
        for (entry in session.inputInfo) {
            val tensorInfo = entry.value.info as? ai.onnxruntime.TensorInfo
            if (tensorInfo != null) {
                inputInfo[entry.key] = tensorInfo.shape
            }
        }
        return inputInfo
    }

    /**
     * 获取已加载模型的输出信息
     * @param modelName 模型名称
     * @return 输出名称和形状的映射，模型未加载则返回null
     */
    fun getModelOutputInfo(modelName: String): Map<String, LongArray>? {
        val session = sessions[modelName] ?: return null
        val outputInfo = mutableMapOf<String, LongArray>()
        for (entry in session.outputInfo) {
            val tensorInfo = entry.value.info as? ai.onnxruntime.TensorInfo
            if (tensorInfo != null) {
                outputInfo[entry.key] = tensorInfo.shape
            }
        }
        return outputInfo
    }

    /**
     * 检查指定模型是否已加载
     * @param modelName 模型名称
     * @return 是否已加载
     */
    fun isModelLoaded(modelName: String): Boolean {
        return sessions.containsKey(modelName)
    }

    /**
     * 卸载指定模型，释放资源
     * @param modelName 模型名称
     */
    fun unloadModel(modelName: String) {
        sessions.remove(modelName)?.close()
        if (sessions.isEmpty()) {
            _modelLoadState = ModelLoadState.NotLoaded
        }
    }

    /**
     * 释放所有资源
     * 应在不再需要推理时调用（如Activity/Service销毁时）
     */
    fun close() {
        sessions.values.forEach { it.close() }
        sessions.clear()
        // OrtEnvironment是全局单例，不需要手动关闭
        _modelLoadState = ModelLoadState.NotLoaded
    }
}

/**
 * ONNX模型配置
 *
 * @param modelName 模型名称（用于缓存和引用）
 * @param modelPath 模型文件路径（相对于assets目录或绝对路径）
 * @param inputNames 输入张量名称列表
 * @param outputNames 输出张量名称列表
 * @param inputShape 输入张量形状（可选，如 [1, 3, 640, 640]）
 * @param numThreads 推理线程数，默认2
 * @param useGpu 是否使用GPU加速（需要设备支持）
 */
data class OnnxModelConfig(
    val modelName: String,
    val modelPath: String,
    val inputNames: List<String>,
    val outputNames: List<String>,
    val inputShape: LongArray? = null,
    val numThreads: Int = DEFAULT_NUM_THREADS,
    val useGpu: Boolean = false
) {
    companion object {
        /** 默认推理线程数 */
        const val DEFAULT_NUM_THREADS = 2
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnnxModelConfig) return false
        return modelName == other.modelName &&
                modelPath == other.modelPath &&
                inputNames == other.inputNames &&
                outputNames == other.outputNames &&
                inputShape.contentEquals(other.inputShape) &&
                numThreads == other.numThreads &&
                useGpu == other.useGpu
    }

    override fun hashCode(): Int {
        var result = modelName.hashCode()
        result = 31 * result + modelPath.hashCode()
        result = 31 * result + inputNames.hashCode()
        result = 31 * result + outputNames.hashCode()
        result = 31 * result + (inputShape?.contentHashCode() ?: 0)
        result = 31 * result + numThreads
        result = 31 * result + useGpu.hashCode()
        return result
    }
}

/**
 * 模型加载状态
 */
sealed class ModelLoadState {
    /** 未加载 */
    object NotLoaded : ModelLoadState()

    /** 加载中 */
    object Loading : ModelLoadState()

    /** 已加载 */
    data class Loaded(val modelName: String) : ModelLoadState()

    /** 加载失败 */
    data class Error(val message: String) : ModelLoadState()
}

/**
 * 推理结果
 */
sealed class InferenceResult {
    /**
     * 推理成功
     * @param outputs 输出数据映射（输出名称 -> 数据）
     * @param inferenceTimeMs 推理耗时（毫秒）
     */
    data class Success(
        val outputs: Map<String, Any>,
        val inferenceTimeMs: Long
    ) : InferenceResult()

    /**
     * 推理失败
     * @param message 错误消息
     * @param exception 异常对象（可选）
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : InferenceResult()
}
