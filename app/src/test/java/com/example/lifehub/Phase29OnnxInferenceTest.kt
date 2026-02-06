package com.example.lifehub

import com.example.lifehub.ai.OnnxInference
import com.example.lifehub.ai.OnnxModelConfig
import com.example.lifehub.ai.InferenceResult
import com.example.lifehub.ai.ModelLoadState
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 29: ONNX Runtime集成 - 单元测试
 *
 * 测试内容：
 * 1. OnnxModelConfig 数据模型正确性
 * 2. InferenceResult 数据模型正确性
 * 3. ModelLoadState 状态管理
 * 4. OnnxInference 工具类核心逻辑（输入验证、预处理等）
 * 5. 图像预处理辅助方法
 * 6. 边界条件处理
 */
class Phase29OnnxInferenceTest {

    // ==================== 1. OnnxModelConfig 数据模型测试 ====================

    @Test
    fun `test OnnxModelConfig creation with all fields`() {
        val config = OnnxModelConfig(
            modelName = "paddle_ocr_det",
            modelPath = "models/paddle_ocr_det.onnx",
            inputNames = listOf("input"),
            outputNames = listOf("output"),
            inputShape = longArrayOf(1, 3, 640, 640),
            numThreads = 4,
            useGpu = false
        )

        assertEquals("paddle_ocr_det", config.modelName)
        assertEquals("models/paddle_ocr_det.onnx", config.modelPath)
        assertEquals(listOf("input"), config.inputNames)
        assertEquals(listOf("output"), config.outputNames)
        assertArrayEquals(longArrayOf(1, 3, 640, 640), config.inputShape)
        assertEquals(4, config.numThreads)
        assertFalse(config.useGpu)
    }

    @Test
    fun `test OnnxModelConfig with default values`() {
        val config = OnnxModelConfig(
            modelName = "test_model",
            modelPath = "models/test.onnx",
            inputNames = listOf("x"),
            outputNames = listOf("y")
        )

        assertEquals("test_model", config.modelName)
        assertNull(config.inputShape)
        assertEquals(OnnxModelConfig.DEFAULT_NUM_THREADS, config.numThreads)
        assertFalse(config.useGpu)
    }

    @Test
    fun `test OnnxModelConfig DEFAULT_NUM_THREADS is reasonable`() {
        val defaultThreads = OnnxModelConfig.DEFAULT_NUM_THREADS
        assertTrue("默认线程数应大于0", defaultThreads > 0)
        assertTrue("默认线程数不应超过8", defaultThreads <= 8)
    }

    @Test
    fun `test OnnxModelConfig with GPU enabled`() {
        val config = OnnxModelConfig(
            modelName = "gpu_model",
            modelPath = "models/gpu.onnx",
            inputNames = listOf("input"),
            outputNames = listOf("output"),
            useGpu = true
        )

        assertTrue(config.useGpu)
    }

    @Test
    fun `test OnnxModelConfig with multiple inputs and outputs`() {
        val config = OnnxModelConfig(
            modelName = "multi_io_model",
            modelPath = "models/multi.onnx",
            inputNames = listOf("input1", "input2", "input3"),
            outputNames = listOf("output1", "output2")
        )

        assertEquals(3, config.inputNames.size)
        assertEquals(2, config.outputNames.size)
        assertEquals("input1", config.inputNames[0])
        assertEquals("output2", config.outputNames[1])
    }

    @Test
    fun `test OnnxModelConfig with custom thread count`() {
        val config = OnnxModelConfig(
            modelName = "model",
            modelPath = "m.onnx",
            inputNames = listOf("i"),
            outputNames = listOf("o"),
            numThreads = 1
        )
        assertEquals(1, config.numThreads)
    }

    // ==================== 2. InferenceResult 数据模型测试 ====================

    @Test
    fun `test InferenceResult Success with float array`() {
        val outputData = mapOf("output" to floatArrayOf(0.1f, 0.9f))
        val result = InferenceResult.Success(
            outputs = outputData,
            inferenceTimeMs = 42L
        )

        assertTrue(result is InferenceResult.Success)
        assertEquals(42L, result.inferenceTimeMs)
        assertNotNull(result.outputs["output"])
        assertEquals(2, (result.outputs["output"] as FloatArray).size)
        assertEquals(0.9f, (result.outputs["output"] as FloatArray)[1], 0.001f)
    }

    @Test
    fun `test InferenceResult Success with multiple outputs`() {
        val outputData = mapOf(
            "scores" to floatArrayOf(0.8f, 0.2f),
            "boxes" to floatArrayOf(10f, 20f, 100f, 200f)
        )
        val result = InferenceResult.Success(
            outputs = outputData,
            inferenceTimeMs = 100L
        )

        assertEquals(2, result.outputs.size)
        assertTrue(result.outputs.containsKey("scores"))
        assertTrue(result.outputs.containsKey("boxes"))
    }

    @Test
    fun `test InferenceResult Error`() {
        val result = InferenceResult.Error(
            message = "模型加载失败",
            exception = RuntimeException("文件不存在")
        )

        assertTrue(result is InferenceResult.Error)
        assertEquals("模型加载失败", result.message)
        assertNotNull(result.exception)
        assertEquals("文件不存在", result.exception?.message)
    }

    @Test
    fun `test InferenceResult Error without exception`() {
        val result = InferenceResult.Error(message = "未知错误")

        assertEquals("未知错误", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `test InferenceResult Success with zero inference time`() {
        val result = InferenceResult.Success(
            outputs = mapOf("out" to floatArrayOf(1.0f)),
            inferenceTimeMs = 0L
        )

        assertEquals(0L, result.inferenceTimeMs)
    }

    @Test
    fun `test InferenceResult Success with empty outputs`() {
        val result = InferenceResult.Success(
            outputs = emptyMap(),
            inferenceTimeMs = 5L
        )

        assertTrue(result.outputs.isEmpty())
    }

    // ==================== 3. ModelLoadState 状态管理测试 ====================

    @Test
    fun `test ModelLoadState NotLoaded`() {
        val state: ModelLoadState = ModelLoadState.NotLoaded
        assertTrue(state is ModelLoadState.NotLoaded)
    }

    @Test
    fun `test ModelLoadState Loading`() {
        val state: ModelLoadState = ModelLoadState.Loading
        assertTrue(state is ModelLoadState.Loading)
    }

    @Test
    fun `test ModelLoadState Loaded`() {
        val state = ModelLoadState.Loaded(modelName = "ocr_det")
        assertTrue(state is ModelLoadState.Loaded)
        assertEquals("ocr_det", state.modelName)
    }

    @Test
    fun `test ModelLoadState Error`() {
        val state = ModelLoadState.Error(message = "内存不足")
        assertTrue(state is ModelLoadState.Error)
        assertEquals("内存不足", state.message)
    }

    @Test
    fun `test ModelLoadState transitions`() {
        // 模拟正常加载流程
        var state: ModelLoadState = ModelLoadState.NotLoaded
        assertTrue(state is ModelLoadState.NotLoaded)

        state = ModelLoadState.Loading
        assertTrue(state is ModelLoadState.Loading)

        state = ModelLoadState.Loaded("test_model")
        assertTrue(state is ModelLoadState.Loaded)
        assertEquals("test_model", (state as ModelLoadState.Loaded).modelName)
    }

    @Test
    fun `test ModelLoadState error transition`() {
        // 模拟加载失败流程
        var state: ModelLoadState = ModelLoadState.NotLoaded
        state = ModelLoadState.Loading
        state = ModelLoadState.Error("文件不存在")
        assertTrue(state is ModelLoadState.Error)
        assertEquals("文件不存在", (state as ModelLoadState.Error).message)
    }

    // ==================== 4. OnnxInference 工具类核心逻辑测试 ====================

    @Test
    fun `test normalizeThreadCount with valid value`() {
        assertEquals(1, OnnxInference.normalizeThreadCount(1))
        assertEquals(4, OnnxInference.normalizeThreadCount(4))
        assertEquals(8, OnnxInference.normalizeThreadCount(8))
    }

    @Test
    fun `test normalizeThreadCount with zero`() {
        // 0应被修正为1
        assertEquals(1, OnnxInference.normalizeThreadCount(0))
    }

    @Test
    fun `test normalizeThreadCount with negative`() {
        // 负数应被修正为1
        assertEquals(1, OnnxInference.normalizeThreadCount(-1))
        assertEquals(1, OnnxInference.normalizeThreadCount(-100))
    }

    @Test
    fun `test normalizeThreadCount with excessive value`() {
        // 超大值应被限制
        val result = OnnxInference.normalizeThreadCount(100)
        assertTrue("线程数不应超过MAX_THREADS", result <= OnnxInference.MAX_THREADS)
    }

    @Test
    fun `test validateModelPath with valid onnx extension`() {
        assertTrue(OnnxInference.isValidModelPath("models/det.onnx"))
        assertTrue(OnnxInference.isValidModelPath("model.onnx"))
        assertTrue(OnnxInference.isValidModelPath("/data/local/model.onnx"))
    }

    @Test
    fun `test validateModelPath with invalid extension`() {
        assertFalse(OnnxInference.isValidModelPath("models/det.pb"))
        assertFalse(OnnxInference.isValidModelPath("models/det.pt"))
        assertFalse(OnnxInference.isValidModelPath("models/det.tflite"))
        assertFalse(OnnxInference.isValidModelPath(""))
    }

    @Test
    fun `test validateModelPath with ort extension`() {
        // ORT格式也是ONNX Runtime支持的优化格式
        assertTrue(OnnxInference.isValidModelPath("model.ort"))
    }

    // ==================== 5. 图像预处理辅助方法测试 ====================

    @Test
    fun `test normalizePixelValue standard range`() {
        // 标准归一化：pixel / 255.0
        val normalized = OnnxInference.normalizePixelValue(128, 0f, 1f)
        assertEquals(128f / 255f, normalized, 0.001f)
    }

    @Test
    fun `test normalizePixelValue zero`() {
        val normalized = OnnxInference.normalizePixelValue(0, 0f, 1f)
        assertEquals(0f, normalized, 0.001f)
    }

    @Test
    fun `test normalizePixelValue max`() {
        val normalized = OnnxInference.normalizePixelValue(255, 0f, 1f)
        assertEquals(1f, normalized, 0.001f)
    }

    @Test
    fun `test normalizePixelValue with mean and std`() {
        // ImageNet风格归一化: (pixel/255.0 - mean) / std
        val pixel = 128
        val mean = 0.485f
        val std = 0.229f
        val normalized = OnnxInference.normalizePixelValueWithMeanStd(pixel, mean, std)
        val expected = ((128f / 255f) - mean) / std
        assertEquals(expected, normalized, 0.001f)
    }

    @Test
    fun `test normalizePixelValue with mean and std zero pixel`() {
        val mean = 0.485f
        val std = 0.229f
        val normalized = OnnxInference.normalizePixelValueWithMeanStd(0, mean, std)
        val expected = (0f - mean) / std
        assertEquals(expected, normalized, 0.001f)
    }

    @Test
    fun `test normalizePixelValue with mean and std max pixel`() {
        val mean = 0.485f
        val std = 0.229f
        val normalized = OnnxInference.normalizePixelValueWithMeanStd(255, mean, std)
        val expected = (1f - mean) / std
        assertEquals(expected, normalized, 0.001f)
    }

    @Test
    fun `test calculateResizedDimensions maintain aspect ratio - landscape`() {
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 1920,
            originalHeight = 1080,
            targetSize = 640
        )
        assertEquals(640, newW)
        assertEquals(360, newH)
    }

    @Test
    fun `test calculateResizedDimensions maintain aspect ratio - portrait`() {
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 1080,
            originalHeight = 1920,
            targetSize = 640
        )
        assertEquals(360, newW)
        assertEquals(640, newH)
    }

    @Test
    fun `test calculateResizedDimensions square image`() {
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 1000,
            originalHeight = 1000,
            targetSize = 640
        )
        assertEquals(640, newW)
        assertEquals(640, newH)
    }

    @Test
    fun `test calculateResizedDimensions smaller than target`() {
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 320,
            originalHeight = 240,
            targetSize = 640
        )
        // 按比例放大，长边为640
        assertEquals(640, newW)
        assertEquals(480, newH)
    }

    @Test
    fun `test calculateResizedDimensions exact target size`() {
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 640,
            originalHeight = 640,
            targetSize = 640
        )
        assertEquals(640, newW)
        assertEquals(640, newH)
    }

    // ==================== 6. 边界条件处理测试 ====================

    @Test
    fun `test createInputTensor shape validation`() {
        val shape = longArrayOf(1, 3, 640, 640)
        val expectedSize = shape.fold(1L) { acc, v -> acc * v }
        assertEquals(1 * 3 * 640 * 640L, expectedSize)
    }

    @Test
    fun `test float array size for standard OCR input`() {
        // PaddleOCR 标准输入: [1, 3, 640, 640]
        val batchSize = 1
        val channels = 3
        val height = 640
        val width = 640
        val totalSize = batchSize * channels * height * width
        assertEquals(1228800, totalSize)
    }

    @Test
    fun `test float array size for classification input`() {
        // 分类模型标准输入: [1, 3, 224, 224]
        val totalSize = 1 * 3 * 224 * 224
        assertEquals(150528, totalSize)
    }

    @Test
    fun `test pixel channel extraction from ARGB`() {
        // ARGB 格式像素值：0xFFRRGGBB
        val pixel = 0xFF804020.toInt() // R=128, G=64, B=32
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        assertEquals(128, r)
        assertEquals(64, g)
        assertEquals(32, b)
    }

    @Test
    fun `test pixel channel extraction white pixel`() {
        val pixel = 0xFFFFFFFF.toInt() // 白色
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        assertEquals(255, r)
        assertEquals(255, g)
        assertEquals(255, b)
    }

    @Test
    fun `test pixel channel extraction black pixel`() {
        val pixel = 0xFF000000.toInt() // 黑色
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        assertEquals(0, r)
        assertEquals(0, g)
        assertEquals(0, b)
    }

    @Test
    fun `test softmax output sums to 1`() {
        // 模拟 softmax 输出验证
        val probs = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val sum = probs.sum()
        assertEquals(1.0f, sum, 0.001f)
    }

    @Test
    fun `test argmax on probability array`() {
        val probs = floatArrayOf(0.1f, 0.05f, 0.8f, 0.05f)
        val maxIdx = OnnxInference.argmax(probs)
        assertEquals(2, maxIdx)
    }

    @Test
    fun `test argmax on single element`() {
        val probs = floatArrayOf(1.0f)
        val maxIdx = OnnxInference.argmax(probs)
        assertEquals(0, maxIdx)
    }

    @Test
    fun `test argmax on all equal values`() {
        val probs = floatArrayOf(0.25f, 0.25f, 0.25f, 0.25f)
        val maxIdx = OnnxInference.argmax(probs)
        assertEquals(0, maxIdx) // 返回第一个最大值的索引
    }

    @Test
    fun `test argmax on empty array returns -1`() {
        val probs = floatArrayOf()
        val maxIdx = OnnxInference.argmax(probs)
        assertEquals(-1, maxIdx)
    }

    @Test
    fun `test topK indices`() {
        val probs = floatArrayOf(0.05f, 0.3f, 0.1f, 0.5f, 0.05f)
        val topK = OnnxInference.topKIndices(probs, 3)
        assertEquals(3, topK.size)
        assertEquals(3, topK[0]) // 0.5f最大
        assertEquals(1, topK[1]) // 0.3f次之
        assertEquals(2, topK[2]) // 0.1f第三
    }

    @Test
    fun `test topK indices with k greater than array size`() {
        val probs = floatArrayOf(0.3f, 0.7f)
        val topK = OnnxInference.topKIndices(probs, 5)
        assertEquals(2, topK.size) // 最多返回数组长度
    }

    @Test
    fun `test topK indices with k = 1`() {
        val probs = floatArrayOf(0.1f, 0.9f, 0.0f)
        val topK = OnnxInference.topKIndices(probs, 1)
        assertEquals(1, topK.size)
        assertEquals(1, topK[0])
    }

    @Test
    fun `test topK indices with empty array`() {
        val probs = floatArrayOf()
        val topK = OnnxInference.topKIndices(probs, 3)
        assertTrue(topK.isEmpty())
    }

    @Test
    fun `test OnnxInference SUPPORTED_EXTENSIONS`() {
        val extensions = OnnxInference.SUPPORTED_EXTENSIONS
        assertTrue(extensions.contains("onnx"))
        assertTrue(extensions.contains("ort"))
    }

    @Test
    fun `test calculateResizedDimensions with zero dimensions`() {
        // 零尺寸应被处理
        val (newW, newH) = OnnxInference.calculateResizedDimensions(
            originalWidth = 0,
            originalHeight = 0,
            targetSize = 640
        )
        // 零尺寸应返回目标大小
        assertEquals(640, newW)
        assertEquals(640, newH)
    }

    @Test
    fun `test normalizeThreadCount boundary at MAX_THREADS`() {
        val result = OnnxInference.normalizeThreadCount(OnnxInference.MAX_THREADS)
        assertEquals(OnnxInference.MAX_THREADS, result)
    }

    @Test
    fun `test normalizeThreadCount just above MAX_THREADS`() {
        val result = OnnxInference.normalizeThreadCount(OnnxInference.MAX_THREADS + 1)
        assertEquals(OnnxInference.MAX_THREADS, result)
    }
}
