package com.example.lifehub

import com.example.lifehub.ai.ImagePreprocessor
import com.example.lifehub.ai.OcrService
import com.example.lifehub.ai.OcrServiceState
import com.example.lifehub.ai.TextBox
import com.example.lifehub.ai.RecognizedText
import com.example.lifehub.ai.OcrResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 30: PaddleOCR模型集成 - 单元测试
 *
 * 测试内容：
 * 1. ImagePreprocessor 图像预处理逻辑
 * 2. OcrService CTC解码逻辑
 * 3. OcrService DB后处理逻辑
 * 4. 数据模型正确性（TextBox, OcrResult等）
 * 5. 边界条件和异常处理
 */
class Phase30PaddleOcrTest {

    // ==================== 1. ImagePreprocessor 检测尺寸计算 ====================

    @Test
    fun `test calculateDetSize normal landscape image`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(1920, 1080)
        // 最长边1920 > 960, ratio = 960/1920 = 0.5
        // newW = 960, newH = 540 -> pad to 32: 960, 544
        assertEquals(960, w)
        assertEquals(544, h)
    }

    @Test
    fun `test calculateDetSize normal portrait image`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(1080, 1920)
        assertEquals(544, w)
        assertEquals(960, h)
    }

    @Test
    fun `test calculateDetSize square image larger than limit`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(1200, 1200)
        // ratio = 960/1200 = 0.8, newW = 960, newH = 960
        assertEquals(960, w)
        assertEquals(960, h)
    }

    @Test
    fun `test calculateDetSize small image no upscale`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(640, 480)
        // 最长边640 < 960, ratio = 1.0, pad to 32
        assertEquals(640, w)
        assertEquals(480, h)
    }

    @Test
    fun `test calculateDetSize pad to multiple of 32`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(500, 300)
        // ratio = 1.0 (< 960)
        // 500 -> ceil to 512, 300 -> ceil to 320
        assertEquals(512, w)
        assertEquals(320, h)
    }

    @Test
    fun `test calculateDetSize zero dimensions`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(0, 0)
        assertEquals(32, w)
        assertEquals(32, h)
    }

    @Test
    fun `test calculateDetSize negative dimensions`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(-100, -200)
        assertEquals(32, w)
        assertEquals(32, h)
    }

    @Test
    fun `test calculateDetSize exactly 960`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(960, 720)
        // ratio = 1.0, 960 already multiple of 32
        // 720 -> pad to 736
        assertEquals(960, w)
        assertEquals(736, h)
    }

    @Test
    fun `test calculateDetSize custom maxSideLen`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(1000, 800, 640)
        // ratio = 640/1000 = 0.64, newW=640, newH=512
        assertEquals(640, w)
        assertEquals(512, h)
    }

    @Test
    fun `test calculateDetSize result always multiple of 32`() {
        val testSizes = listOf(
            100 to 100, 333 to 444, 1920 to 1080, 4000 to 3000,
            31 to 31, 33 to 33, 1 to 1, 960 to 960
        )
        for ((origW, origH) in testSizes) {
            val (w, h) = ImagePreprocessor.calculateDetSize(origW, origH)
            assertEquals("Width $w for ($origW,$origH) not multiple of 32", 0, w % 32)
            assertEquals("Height $h for ($origW,$origH) not multiple of 32", 0, h % 32)
            assertTrue("Width should be at least 32", w >= 32)
            assertTrue("Height should be at least 32", h >= 32)
        }
    }

    // ==================== 2. ImagePreprocessor 识别宽度计算 ====================

    @Test
    fun `test calculateRecWidth normal text line`() {
        val w = ImagePreprocessor.calculateRecWidth(200, 50)
        assertEquals(192, w)
    }

    @Test
    fun `test calculateRecWidth wide text line clamped`() {
        val w = ImagePreprocessor.calculateRecWidth(2000, 30)
        assertEquals(320, w)
    }

    @Test
    fun `test calculateRecWidth narrow text`() {
        val w = ImagePreprocessor.calculateRecWidth(10, 48)
        assertEquals(10, w)
    }

    @Test
    fun `test calculateRecWidth zero dimensions`() {
        val w = ImagePreprocessor.calculateRecWidth(0, 0)
        assertEquals(320, w)
    }

    @Test
    fun `test calculateRecWidth very tall narrow text`() {
        val w = ImagePreprocessor.calculateRecWidth(5, 100)
        assertEquals(2, w)
    }

    // ==================== 3. ImagePreprocessor 归一化 ====================

    @Test
    fun `test normalizeChannel zero pixel`() {
        val result = ImagePreprocessor.normalizeChannel(0, 0.485f, 0.229f)
        val expected = (0f / 255f - 0.485f) / 0.229f
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun `test normalizeChannel max pixel`() {
        val result = ImagePreprocessor.normalizeChannel(255, 0.485f, 0.229f)
        val expected = (1.0f - 0.485f) / 0.229f
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun `test normalizeChannel mid pixel`() {
        val result = ImagePreprocessor.normalizeChannel(128, 0.456f, 0.224f)
        val expected = (128f / 255f - 0.456f) / 0.224f
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun `test normalizeChannel all three channels`() {
        val pixel = 0xFF804020.toInt()
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        val normR = ImagePreprocessor.normalizeChannel(r, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        val normG = ImagePreprocessor.normalizeChannel(g, ImagePreprocessor.MEAN_G, ImagePreprocessor.STD_G)
        val normB = ImagePreprocessor.normalizeChannel(b, ImagePreprocessor.MEAN_B, ImagePreprocessor.STD_B)

        assertTrue("normR in range", normR > -3f && normR < 3f)
        assertTrue("normG in range", normG > -3f && normG < 3f)
        assertTrue("normB in range", normB > -3f && normB < 3f)
    }

    // ==================== 4. pixelsToNchwFloat ====================

    @Test
    fun `test pixelsToNchwFloat single white pixel`() {
        val pixels = intArrayOf(0xFFFFFFFF.toInt())
        val result = ImagePreprocessor.pixelsToNchwFloat(pixels, 1, 1)

        assertEquals(3, result.size)
        val expectedR = (1.0f - ImagePreprocessor.MEAN_R) / ImagePreprocessor.STD_R
        assertEquals(expectedR, result[0], 0.001f)
    }

    @Test
    fun `test pixelsToNchwFloat single black pixel`() {
        val pixels = intArrayOf(0xFF000000.toInt())
        val result = ImagePreprocessor.pixelsToNchwFloat(pixels, 1, 1)

        assertEquals(3, result.size)
        val expectedR = (0f - ImagePreprocessor.MEAN_R) / ImagePreprocessor.STD_R
        assertEquals(expectedR, result[0], 0.001f)
    }

    @Test
    fun `test pixelsToNchwFloat NCHW layout 2x2`() {
        val pixels = intArrayOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
            0xFFFFFFFF.toInt()
        )
        val result = ImagePreprocessor.pixelsToNchwFloat(pixels, 2, 2)

        assertEquals(12, result.size)

        val r0 = ImagePreprocessor.normalizeChannel(255, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        val r1 = ImagePreprocessor.normalizeChannel(0, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        assertEquals(r0, result[0], 0.001f)
        assertEquals(r1, result[1], 0.001f)
    }

    @Test
    fun `test pixelsToNchwFloat output size`() {
        val w = 10
        val h = 5
        val pixels = IntArray(w * h) { 0xFF808080.toInt() }
        val result = ImagePreprocessor.pixelsToNchwFloat(pixels, w, h)
        assertEquals(3 * w * h, result.size)
    }

    // ==================== 5. fillNchwDataFromPixels ====================

    @Test
    fun `test fillNchwDataFromPixels with padding`() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val output = FloatArray(3 * 1 * 4)
        ImagePreprocessor.fillNchwDataFromPixels(pixels, output, 2, 1, 4)

        val r0 = ImagePreprocessor.normalizeChannel(255, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        val r1 = ImagePreprocessor.normalizeChannel(0, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        assertEquals(r0, output[0], 0.001f)
        assertEquals(r1, output[1], 0.001f)
        assertEquals(0f, output[2], 0.001f)
        assertEquals(0f, output[3], 0.001f)
    }

    @Test
    fun `test fillNchwDataFromPixels no padding`() {
        val pixels = intArrayOf(0xFF808080.toInt(), 0xFF808080.toInt())
        val output = FloatArray(3 * 1 * 2)
        ImagePreprocessor.fillNchwDataFromPixels(pixels, output, 2, 1, 2)

        val expected = ImagePreprocessor.normalizeChannel(128, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        assertEquals(expected, output[0], 0.001f)
        assertEquals(expected, output[1], 0.001f)
    }

    @Test
    fun `test fillNchwDataFromPixels multirow`() {
        val pixels = IntArray(6) { 0xFFFF0000.toInt() }
        val output = FloatArray(3 * 2 * 4)
        ImagePreprocessor.fillNchwDataFromPixels(pixels, output, 3, 2, 4)

        assertEquals(0f, output[3], 0.001f)
        assertEquals(0f, output[7], 0.001f)

        val expectedR = ImagePreprocessor.normalizeChannel(255, ImagePreprocessor.MEAN_R, ImagePreprocessor.STD_R)
        assertEquals(expectedR, output[0], 0.001f)
        assertEquals(expectedR, output[4], 0.001f)
    }

    // ==================== 6. CTC解码测试 ====================

    @Test
    fun `test ctcGreedyDecode simple case`() {
        val dictionary = listOf("a", "b", "c")
        val indices = listOf(0, 1, 1, 0, 2, 3)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("abc", result)
    }

    @Test
    fun `test ctcGreedyDecode all blanks`() {
        val dictionary = listOf("a", "b")
        val indices = listOf(0, 0, 0, 0)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("", result)
    }

    @Test
    fun `test ctcGreedyDecode repeated chars`() {
        val dictionary = listOf("h", "e", "l", "o")
        val indices = listOf(1, 0, 2, 3, 3, 0, 4)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("helo", result)
    }

    @Test
    fun `test ctcGreedyDecode double chars with blank separator`() {
        val dictionary = listOf("l", "e")
        val indices = listOf(1, 0, 1)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("ll", result)
    }

    @Test
    fun `test ctcGreedyDecode empty indices`() {
        val dictionary = listOf("a", "b")
        val result = OcrService.ctcGreedyDecode(emptyList(), dictionary)
        assertEquals("", result)
    }

    @Test
    fun `test ctcGreedyDecode index out of dictionary bounds`() {
        val dictionary = listOf("a", "b")
        val indices = listOf(1, 4, 2)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("ab", result)
    }

    @Test
    fun `test ctcGreedyDecode chinese characters`() {
        val dictionary = listOf("\u756a", "\u8304", "\u7092", "\u86cb")
        val indices = listOf(0, 1, 0, 2, 3, 0, 4)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("\u756a\u8304\u7092\u86cb", result)
    }

    @Test
    fun `test ctcDecode from probabilities`() {
        val dictionary = listOf("a", "b", "c")
        val numClasses = 4
        val timeSteps = 5

        val probs = FloatArray(timeSteps * numClasses)
        probs[0] = 0.9f; probs[1] = 0.03f; probs[2] = 0.04f; probs[3] = 0.03f
        probs[4] = 0.05f; probs[5] = 0.85f; probs[6] = 0.05f; probs[7] = 0.05f
        probs[8] = 0.1f; probs[9] = 0.8f; probs[10] = 0.05f; probs[11] = 0.05f
        probs[12] = 0.9f; probs[13] = 0.03f; probs[14] = 0.04f; probs[15] = 0.03f
        probs[16] = 0.05f; probs[17] = 0.05f; probs[18] = 0.85f; probs[19] = 0.05f

        val result = OcrService.ctcDecode(probs, timeSteps, numClasses, dictionary)
        assertEquals("ab", result)
    }

    @Test
    fun `test ctcDecode zero timeSteps`() {
        val result = OcrService.ctcDecode(floatArrayOf(), 0, 4, listOf("a"))
        assertEquals("", result)
    }

    @Test
    fun `test ctcDecode zero numClasses`() {
        val result = OcrService.ctcDecode(floatArrayOf(), 5, 0, listOf("a"))
        assertEquals("", result)
    }

    @Test
    fun `test ctcDecode single timestep`() {
        val dictionary = listOf("x")
        val probs = floatArrayOf(0.2f, 0.8f)
        val result = OcrService.ctcDecode(probs, 1, 2, dictionary)
        assertEquals("x", result)
    }

    @Test
    fun `test ctcGreedyDecode all same char no blanks`() {
        val dictionary = listOf("a", "b")
        val indices = listOf(1, 1, 1, 1)
        val result = OcrService.ctcGreedyDecode(indices, dictionary)
        assertEquals("a", result)
    }

    // ==================== 7. DB后处理测试 ====================

    @Test
    fun `test dbPostProcess empty probMap`() {
        val result = OcrService.dbPostProcess(floatArrayOf(), 0, 0, 100, 100)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test dbPostProcess all below threshold`() {
        val probMap = FloatArray(10 * 10) { 0.1f }
        val result = OcrService.dbPostProcess(probMap, 10, 10, 100, 100)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test dbPostProcess single text region`() {
        val mapW = 10
        val mapH = 10
        val probMap = FloatArray(mapW * mapH) { 0.1f }

        for (y in 3..6) {
            for (x in 3..6) {
                probMap[y * mapW + x] = 0.8f
            }
        }

        val result = OcrService.dbPostProcess(probMap, mapW, mapH, 100, 100)

        assertTrue("Should detect at least one box", result.isNotEmpty())
        val box = result[0]
        assertTrue(box.left >= 0)
        assertTrue(box.top >= 0)
        assertTrue(box.right <= 100)
        assertTrue(box.bottom <= 100)
        assertTrue("Score should meet threshold", box.score >= 0.6f)
    }

    @Test
    fun `test dbPostProcess two separate regions`() {
        val mapW = 20
        val mapH = 10
        val probMap = FloatArray(mapW * mapH) { 0.1f }

        for (y in 2..4) {
            for (x in 2..5) {
                probMap[y * mapW + x] = 0.8f
            }
        }
        for (y in 2..4) {
            for (x in 14..17) {
                probMap[y * mapW + x] = 0.8f
            }
        }

        val result = OcrService.dbPostProcess(probMap, mapW, mapH, 200, 100)
        assertTrue("Should detect two boxes", result.size >= 2)
    }

    @Test
    fun `test dbPostProcess region too small filtered out`() {
        val mapW = 10
        val mapH = 10
        val probMap = FloatArray(mapW * mapH) { 0.1f }

        for (y in 4..5) {
            for (x in 4..5) {
                probMap[y * mapW + x] = 0.8f
            }
        }

        val result = OcrService.dbPostProcess(probMap, mapW, mapH, 100, 100)
        assertTrue("Small region should be filtered", result.isEmpty())
    }

    @Test
    fun `test dbPostProcess low score region filtered`() {
        val mapW = 10
        val mapH = 10
        val probMap = FloatArray(mapW * mapH) { 0.1f }

        for (y in 3..6) {
            for (x in 3..6) {
                probMap[y * mapW + x] = 0.35f
            }
        }

        val result = OcrService.dbPostProcess(probMap, mapW, mapH, 100, 100)
        assertTrue("Low score region should be filtered", result.isEmpty())
    }

    @Test
    fun `test dbPostProcess coordinate scaling`() {
        val mapW = 10
        val mapH = 10
        val origW = 1000
        val origH = 500
        val probMap = FloatArray(mapW * mapH) { 0.1f }

        for (y in 5..8) {
            for (x in 5..8) {
                probMap[y * mapW + x] = 0.9f
            }
        }

        val result = OcrService.dbPostProcess(probMap, mapW, mapH, origW, origH)

        if (result.isNotEmpty()) {
            val box = result[0]
            assertTrue("Right should be within original width", box.right <= origW)
            assertTrue("Bottom should be within original height", box.bottom <= origH)
        }
    }

    @Test
    fun `test dbPostProcess negative dimensions`() {
        val result = OcrService.dbPostProcess(floatArrayOf(0.5f), -1, -1, 100, 100)
        assertTrue(result.isEmpty())
    }

    // ==================== 8. findConnectedComponents ====================

    @Test
    fun `test findConnectedComponents empty map`() {
        val result = OcrService.findConnectedComponents(
            BooleanArray(0), FloatArray(0), 0, 0, 0.6f, 3
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test findConnectedComponents all false`() {
        val binaryMap = BooleanArray(25) { false }
        val probMap = FloatArray(25) { 0.1f }
        val result = OcrService.findConnectedComponents(binaryMap, probMap, 5, 5, 0.6f, 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test findConnectedComponents single component`() {
        val w = 10
        val h = 10
        val binaryMap = BooleanArray(w * h) { false }
        val probMap = FloatArray(w * h) { 0.1f }

        for (y in 2..6) {
            for (x in 2..6) {
                binaryMap[y * w + x] = true
                probMap[y * w + x] = 0.8f
            }
        }

        val result = OcrService.findConnectedComponents(binaryMap, probMap, w, h, 0.6f, 3)
        assertEquals(1, result.size)
        assertTrue("Score should meet threshold", result[0].score >= 0.6f)
    }

    @Test
    fun `test findConnectedComponents sorted by position`() {
        val w = 20
        val h = 20
        val binaryMap = BooleanArray(w * h) { false }
        val probMap = FloatArray(w * h) { 0.1f }

        for (y in 10..14) {
            for (x in 2..6) {
                binaryMap[y * w + x] = true
                probMap[y * w + x] = 0.8f
            }
        }
        for (y in 2..6) {
            for (x in 10..14) {
                binaryMap[y * w + x] = true
                probMap[y * w + x] = 0.8f
            }
        }

        val result = OcrService.findConnectedComponents(binaryMap, probMap, w, h, 0.6f, 3)
        assertTrue(result.size >= 2)
        assertTrue("Should be sorted by top", result[0].top <= result[1].top)
    }

    // ==================== 9. TextBox 数据模型测试 ====================

    @Test
    fun `test TextBox width and height`() {
        val box = TextBox(10, 20, 110, 70, 0.9f)
        assertEquals(100, box.width())
        assertEquals(50, box.height())
    }

    @Test
    fun `test TextBox zero dimensions`() {
        val box = TextBox(50, 50, 50, 50, 0.5f)
        assertEquals(0, box.width())
        assertEquals(0, box.height())
    }

    @Test
    fun `test TextBox score`() {
        val box = TextBox(0, 0, 100, 100, 0.95f)
        assertEquals(0.95f, box.score, 0.001f)
    }

    // ==================== 10. OcrResult 数据模型测试 ====================

    @Test
    fun `test OcrResult success`() {
        val result = OcrResult(
            texts = listOf(RecognizedText("hello", TextBox(0, 0, 100, 50, 0.9f))),
            totalTimeMs = 100
        )
        assertTrue(result.isSuccess)
        assertNull(result.error)
        assertEquals("hello", result.fullText())
    }

    @Test
    fun `test OcrResult error`() {
        val result = OcrResult(
            texts = emptyList(),
            totalTimeMs = 0,
            error = "\u6a21\u578b\u672a\u52a0\u8f7d"
        )
        assertFalse(result.isSuccess)
        assertNotNull(result.error)
    }

    @Test
    fun `test OcrResult fullText multiple lines`() {
        val result = OcrResult(
            texts = listOf(
                RecognizedText("line1", TextBox(0, 0, 100, 30, 0.9f)),
                RecognizedText("line2", TextBox(0, 30, 100, 60, 0.8f)),
                RecognizedText("line3", TextBox(0, 60, 100, 90, 0.7f))
            ),
            totalTimeMs = 200
        )
        assertEquals("line1\nline2\nline3", result.fullText())
    }

    @Test
    fun `test OcrResult empty texts`() {
        val result = OcrResult(texts = emptyList(), totalTimeMs = 50)
        assertTrue(result.isSuccess)
        assertEquals("", result.fullText())
    }

    // ==================== 11. RecognizedText ====================

    @Test
    fun `test RecognizedText creation`() {
        val box = TextBox(10, 20, 200, 50, 0.85f)
        val text = RecognizedText("\u756a\u8304\u7092\u86cb", box)
        assertEquals("\u756a\u8304\u7092\u86cb", text.text)
        assertEquals(10, text.box.left)
        assertEquals(0.85f, text.box.score, 0.001f)
    }

    // ==================== 12. OcrServiceState ====================

    @Test
    fun `test OcrServiceState transitions`() {
        var state: OcrServiceState = OcrServiceState.NotReady
        assertTrue(state is OcrServiceState.NotReady)

        state = OcrServiceState.Initializing
        assertTrue(state is OcrServiceState.Initializing)

        state = OcrServiceState.Ready
        assertTrue(state is OcrServiceState.Ready)
    }

    @Test
    fun `test OcrServiceState error`() {
        val state = OcrServiceState.Error("\u521d\u59cb\u5316\u5931\u8d25")
        assertTrue(state is OcrServiceState.Error)
        assertEquals("\u521d\u59cb\u5316\u5931\u8d25", (state as OcrServiceState.Error).message)
    }

    // ==================== 13. 模型常量验证 ====================

    @Test
    fun `test OcrService model paths`() {
        assertTrue(OcrService.MODEL_DET_PATH.endsWith(".onnx"))
        assertTrue(OcrService.MODEL_REC_PATH.endsWith(".onnx"))
        assertTrue(OcrService.MODEL_CLS_PATH.endsWith(".onnx"))
    }

    @Test
    fun `test OcrService model names unique`() {
        val names = setOf(OcrService.MODEL_DET, OcrService.MODEL_REC, OcrService.MODEL_CLS)
        assertEquals(3, names.size)
    }

    @Test
    fun `test OcrService thresholds reasonable`() {
        assertTrue("DET_DB_THRESH in (0,1)", OcrService.DET_DB_THRESH > 0f && OcrService.DET_DB_THRESH < 1f)
        assertTrue("DET_DB_BOX_THRESH in (0,1)", OcrService.DET_DB_BOX_THRESH > 0f && OcrService.DET_DB_BOX_THRESH < 1f)
        assertTrue("CLS_THRESH in (0,1)", OcrService.CLS_THRESH > 0f && OcrService.CLS_THRESH < 1f)
        assertTrue("DET_DB_UNCLIP_RATIO > 1", OcrService.DET_DB_UNCLIP_RATIO > 1f)
        assertTrue("DET_MIN_SIZE > 0", OcrService.DET_MIN_SIZE > 0)
    }

    // ==================== 14. ImagePreprocessor 常量验证 ====================

    @Test
    fun `test ImagePreprocessor constants`() {
        assertEquals(960, ImagePreprocessor.DET_MAX_SIDE_LEN)
        assertEquals(48, ImagePreprocessor.REC_IMG_HEIGHT)
        assertEquals(320, ImagePreprocessor.REC_MAX_WIDTH)
        assertEquals(48, ImagePreprocessor.CLS_IMG_HEIGHT)
        assertEquals(192, ImagePreprocessor.CLS_IMG_WIDTH)
    }

    @Test
    fun `test ImagePreprocessor mean std values`() {
        assertEquals(0.485f, ImagePreprocessor.MEAN_R, 0.001f)
        assertEquals(0.456f, ImagePreprocessor.MEAN_G, 0.001f)
        assertEquals(0.406f, ImagePreprocessor.MEAN_B, 0.001f)
        assertEquals(0.229f, ImagePreprocessor.STD_R, 0.001f)
        assertEquals(0.224f, ImagePreprocessor.STD_G, 0.001f)
        assertEquals(0.225f, ImagePreprocessor.STD_B, 0.001f)
    }

    // ==================== 15. 边界条件 ====================

    @Test
    fun `test pixelsToNchwFloat empty`() {
        val result = ImagePreprocessor.pixelsToNchwFloat(intArrayOf(), 0, 0)
        assertEquals(0, result.size)
    }

    @Test
    fun `test calculateDetSize width 1 height 1`() {
        val (w, h) = ImagePreprocessor.calculateDetSize(1, 1)
        assertEquals(32, w)
        assertEquals(32, h)
    }
}
