package com.example.lifehub.ai

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PaddleOCR 端侧OCR服务
 *
 * 实现完整的OCR流水线：
 * 1. 文本检测 (DBNet) - 检测图像中的文本区域
 * 2. 方向分类 (Classifier) - 判断文本方向（0度/180度）
 * 3. 文本识别 (CRNN) - 识别文本内容
 *
 * 目标：小于500ms 完成一次菜单图片的文字识别
 */
class OcrService(private val context: android.content.Context) {

    companion object {
        // 模型名称常量
        const val MODEL_DET = "paddle_ocr_det"
        const val MODEL_REC = "paddle_ocr_rec"
        const val MODEL_CLS = "paddle_ocr_cls"

        // 模型文件路径（相对于assets）
        const val MODEL_DET_PATH = "models/ch_PP-OCRv4_det_infer.onnx"
        const val MODEL_REC_PATH = "models/ch_PP-OCRv4_rec_infer.onnx"
        const val MODEL_CLS_PATH = "models/ch_ppocr_mobile_v2.0_cls_infer.onnx"

        // 字典文件路径
        const val DICT_PATH = "models/ppocr_keys_v1.txt"

        // 检测后处理参数
        const val DET_DB_THRESH = 0.3f
        const val DET_DB_BOX_THRESH = 0.6f
        const val DET_DB_UNCLIP_RATIO = 1.5f
        const val DET_MIN_SIZE = 3

        // 分类参数
        const val CLS_THRESH = 0.9f

        /**
         * CTC解码：将模型输出的索引序列转换为文本
         * 规则：
         * 1. 每个时间步取argmax得到字符索引
         * 2. 去除连续重复的索引
         * 3. 去除blank token（索引0）
         * 4. 将索引映射为字符
         */
        fun ctcDecode(
            probabilities: FloatArray,
            timeSteps: Int,
            numClasses: Int,
            dictionary: List<String>
        ): String {
            if (timeSteps <= 0 || numClasses <= 0) return ""

            val indices = mutableListOf<Int>()

            // Step 1: 每个时间步取argmax
            for (t in 0 until timeSteps) {
                val offset = t * numClasses
                var maxIdx = 0
                var maxVal = probabilities[offset]
                for (c in 1 until numClasses) {
                    val idx = offset + c
                    if (idx < probabilities.size && probabilities[idx] > maxVal) {
                        maxVal = probabilities[idx]
                        maxIdx = c
                    }
                }
                indices.add(maxIdx)
            }

            // Step 2 & 3: 去重和去blank
            return ctcGreedyDecode(indices, dictionary)
        }

        /**
         * CTC贪心解码（从索引列表到文本）
         * 索引0为blank token，字典字符从索引1开始
         */
        fun ctcGreedyDecode(indices: List<Int>, dictionary: List<String>): String {
            val sb = StringBuilder()
            var prevIdx = -1
            for (idx in indices) {
                if (idx != 0 && idx != prevIdx) {
                    val charIdx = idx - 1
                    if (charIdx in dictionary.indices) {
                        sb.append(dictionary[charIdx])
                    }
                }
                prevIdx = idx
            }
            return sb.toString()
        }

        /**
         * DB后处理：从概率图提取文本框
         * 使用阈值二值化 + 连通区域分析
         */
        fun dbPostProcess(
            probMap: FloatArray,
            mapWidth: Int,
            mapHeight: Int,
            originalWidth: Int,
            originalHeight: Int,
            thresh: Float = DET_DB_THRESH,
            boxThresh: Float = DET_DB_BOX_THRESH,
            minSize: Int = DET_MIN_SIZE
        ): List<TextBox> {
            if (probMap.isEmpty() || mapWidth <= 0 || mapHeight <= 0) return emptyList()

            // 1. 二值化
            val binaryMap = BooleanArray(mapWidth * mapHeight)
            for (i in probMap.indices) {
                if (i < binaryMap.size) {
                    binaryMap[i] = probMap[i] > thresh
                }
            }

            // 2. 查找连通区域并提取边界框
            val boxes = findConnectedComponents(
                binaryMap, probMap, mapWidth, mapHeight, boxThresh, minSize
            )

            // 3. 将框坐标映射回原图
            val scaleW = originalWidth.toFloat() / mapWidth
            val scaleH = originalHeight.toFloat() / mapHeight

            return boxes.map { box ->
                TextBox(
                    left = (box.left * scaleW).toInt().coerceIn(0, originalWidth),
                    top = (box.top * scaleH).toInt().coerceIn(0, originalHeight),
                    right = (box.right * scaleW).toInt().coerceIn(0, originalWidth),
                    bottom = (box.bottom * scaleH).toInt().coerceIn(0, originalHeight),
                    score = box.score
                )
            }.filter { it.width() > 0 && it.height() > 0 }
        }

        /**
         * 连通区域分析，查找文本框
         * 使用BFS遍历二值图中的连通区域，提取边界框
         */
        fun findConnectedComponents(
            binaryMap: BooleanArray,
            probMap: FloatArray,
            width: Int,
            height: Int,
            boxThresh: Float,
            minSize: Int
        ): List<TextBox> {
            val visited = BooleanArray(width * height)
            val boxes = mutableListOf<TextBox>()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    if (binaryMap[idx] && !visited[idx]) {
                        var minX = x; var maxX = x
                        var minY = y; var maxY = y
                        var scoreSum = 0f
                        var count = 0

                        val queue = ArrayDeque<Pair<Int, Int>>()
                        queue.add(Pair(x, y))
                        visited[idx] = true

                        while (queue.isNotEmpty()) {
                            val (cx, cy) = queue.removeFirst()
                            val ci = cy * width + cx
                            scoreSum += probMap[ci]
                            count++

                            minX = minOf(minX, cx)
                            maxX = maxOf(maxX, cx)
                            minY = minOf(minY, cy)
                            maxY = maxOf(maxY, cy)

                            // 4邻域搜索
                            val neighbors = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                            for ((dx, dy) in neighbors) {
                                val nx = cx + dx
                                val ny = cy + dy
                                if (nx in 0 until width && ny in 0 until height) {
                                    val ni = ny * width + nx
                                    if (binaryMap[ni] && !visited[ni]) {
                                        visited[ni] = true
                                        queue.add(Pair(nx, ny))
                                    }
                                }
                            }
                        }

                        val avgScore = scoreSum / count
                        val boxW = maxX - minX + 1
                        val boxH = maxY - minY + 1

                        if (avgScore >= boxThresh && boxW >= minSize && boxH >= minSize) {
                            val expandX = (boxW * (DET_DB_UNCLIP_RATIO - 1) / 2).toInt()
                            val expandY = (boxH * (DET_DB_UNCLIP_RATIO - 1) / 2).toInt()

                            boxes.add(TextBox(
                                left = (minX - expandX).coerceAtLeast(0),
                                top = (minY - expandY).coerceAtLeast(0),
                                right = (maxX + expandX).coerceAtMost(width - 1),
                                bottom = (maxY + expandY).coerceAtMost(height - 1),
                                score = avgScore
                            ))
                        }
                    }
                }
            }

            // 按从上到下、从左到右排序
            return boxes.sortedWith(compareBy({ it.top }, { it.left }))
        }
    }

    // 推理引擎
    private val onnxInference = OnnxInference(context)

    // 字符字典
    private var dictionary: List<String> = emptyList()

    // 服务状态
    private var _state: OcrServiceState = OcrServiceState.NotReady
    val state: OcrServiceState get() = _state

    /**
     * 初始化OCR服务：加载所有模型和字典
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        _state = OcrServiceState.Initializing

        try {
            // 加载字典
            dictionary = loadDictionary()

            // 加载检测模型
            onnxInference.loadModel(OnnxModelConfig(
                modelName = MODEL_DET,
                modelPath = MODEL_DET_PATH,
                inputNames = listOf("x"),
                outputNames = listOf("sigmoid_0.tmp_0"),
                numThreads = 4
            ))

            // 加载识别模型
            onnxInference.loadModel(OnnxModelConfig(
                modelName = MODEL_REC,
                modelPath = MODEL_REC_PATH,
                inputNames = listOf("x"),
                outputNames = listOf("softmax_0.tmp_0"),
                numThreads = 4
            ))

            // 加载分类模型
            onnxInference.loadModel(OnnxModelConfig(
                modelName = MODEL_CLS,
                modelPath = MODEL_CLS_PATH,
                inputNames = listOf("x"),
                outputNames = listOf("softmax_0.tmp_0"),
                numThreads = 2
            ))

            _state = OcrServiceState.Ready
        } catch (e: Exception) {
            _state = OcrServiceState.Error("OCR服务初始化失败: ${e.message}")
            throw e
        }
    }

    /**
     * 执行完整OCR识别
     * @param bitmap 输入图像
     * @return OCR识别结果
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult = withContext(Dispatchers.IO) {
        if (_state !is OcrServiceState.Ready) {
            return@withContext OcrResult(
                texts = emptyList(),
                totalTimeMs = 0,
                error = "OCR服务未就绪: $_state"
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            // Step 1: 文本检测
            val (detInput, detSize) = ImagePreprocessor.preprocessForDetection(bitmap)
            val detResult = onnxInference.runInference(
                MODEL_DET, "x", detInput,
                longArrayOf(1, 3, detSize.second.toLong(), detSize.first.toLong())
            )

            if (detResult is InferenceResult.Error) {
                return@withContext OcrResult(
                    texts = emptyList(),
                    totalTimeMs = System.currentTimeMillis() - startTime,
                    error = detResult.message
                )
            }

            val detOutput = (detResult as InferenceResult.Success)
                .outputs.values.first() as FloatArray
            val textBoxes = dbPostProcess(
                detOutput, detSize.first, detSize.second,
                bitmap.width, bitmap.height
            )

            if (textBoxes.isEmpty()) {
                return@withContext OcrResult(
                    texts = emptyList(),
                    totalTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Step 2 & 3: 对每个文本框进行分类和识别
            val recognizedTexts = mutableListOf<RecognizedText>()

            for (box in textBoxes) {
                val cropped = cropBitmap(bitmap, box) ?: continue

                // 方向分类
                val needRotate = classifyDirection(cropped)
                val oriented = if (needRotate) rotateBitmap180(cropped) else cropped

                // 文本识别
                val text = recognizeSingleLine(oriented)

                if (text.isNotBlank()) {
                    recognizedTexts.add(RecognizedText(text = text, box = box))
                }

                if (oriented !== cropped) oriented.recycle()
                if (cropped !== bitmap) cropped.recycle()
            }

            OcrResult(
                texts = recognizedTexts,
                totalTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            OcrResult(
                texts = emptyList(),
                totalTimeMs = System.currentTimeMillis() - startTime,
                error = "OCR识别失败: ${e.message}"
            )
        }
    }

    /**
     * 方向分类
     * @return true 如果需要旋转180度
     */
    private suspend fun classifyDirection(bitmap: Bitmap): Boolean {
        val clsInput = ImagePreprocessor.preprocessForClassification(bitmap)
        val result = onnxInference.runInference(
            MODEL_CLS, "x", clsInput,
            longArrayOf(
                1, 3,
                ImagePreprocessor.CLS_IMG_HEIGHT.toLong(),
                ImagePreprocessor.CLS_IMG_WIDTH.toLong()
            )
        )

        if (result is InferenceResult.Error) return false

        val output = (result as InferenceResult.Success).outputs.values.first() as FloatArray
        return output.size >= 2 && output[1] > CLS_THRESH
    }

    /**
     * 单行文本识别
     */
    private suspend fun recognizeSingleLine(bitmap: Bitmap): String {
        val recInput = ImagePreprocessor.preprocessForRecognition(bitmap)
        val result = onnxInference.runInference(
            MODEL_REC, "x", recInput,
            longArrayOf(
                1, 3,
                ImagePreprocessor.REC_IMG_HEIGHT.toLong(),
                ImagePreprocessor.REC_MAX_WIDTH.toLong()
            )
        )

        if (result is InferenceResult.Error) return ""

        val output = (result as InferenceResult.Success).outputs.values.first() as FloatArray
        val numClasses = dictionary.size + 1  // +1 for blank token
        if (numClasses <= 1) return ""
        val timeSteps = output.size / numClasses

        return ctcDecode(output, timeSteps, numClasses, dictionary)
    }

    /**
     * 从assets加载字符字典
     */
    private fun loadDictionary(): List<String> {
        return try {
            context.assets.open(DICT_PATH).bufferedReader().readLines()
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            throw RuntimeException("字符字典加载失败: ${e.message}", e)
        }
    }

    /**
     * 裁剪文本区域
     */
    private fun cropBitmap(source: Bitmap, box: TextBox): Bitmap? {
        val left = box.left.coerceIn(0, source.width - 1)
        val top = box.top.coerceIn(0, source.height - 1)
        val right = box.right.coerceIn(left + 1, source.width)
        val bottom = box.bottom.coerceIn(top + 1, source.height)

        if (right - left <= 0 || bottom - top <= 0) return null

        return try {
            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 旋转Bitmap 180度
     */
    private fun rotateBitmap180(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(180f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 释放所有资源
     * 应在不再需要OCR时调用（如Activity/Service销毁时）
     */
    fun close() {
        onnxInference.close()
        dictionary = emptyList()
        _state = OcrServiceState.NotReady
    }
}

/**
 * OCR服务状态
 */
sealed class OcrServiceState {
    object NotReady : OcrServiceState()
    object Initializing : OcrServiceState()
    object Ready : OcrServiceState()
    data class Error(val message: String) : OcrServiceState()
}

/**
 * 文本框（检测结果）
 */
data class TextBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val score: Float
) {
    fun width() = right - left
    fun height() = bottom - top
}

/**
 * 识别出的文本（含位置信息）
 */
data class RecognizedText(
    val text: String,
    val box: TextBox
)

/**
 * OCR识别结果
 */
data class OcrResult(
    val texts: List<RecognizedText>,
    val totalTimeMs: Long,
    val error: String? = null
) {
    /** 获取所有识别文本拼接 */
    fun fullText(): String = texts.joinToString("\n") { it.text }

    /** 是否识别成功 */
    val isSuccess: Boolean get() = error == null
}
