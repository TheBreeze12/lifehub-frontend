package com.example.lifehub

import com.example.lifehub.data.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/**
 * Phase 45: 语音识别（FunASR离线语音识别）单元测试
 *
 * 测试覆盖：
 * 1. 数据模型正确性（SpeechRecognitionState、Config等）
 * 2. 音频处理工具类（预加重、分帧、窗函数、FFT、Fbank）
 * 3. CTC解码逻辑
 * 4. 边界条件和异常处理
 * 5. 配置验证
 * 6. Mel频率转换
 * 7. 静音检测
 * 8. CMVN归一化
 */
class Phase45SpeechRecognitionTest {

    // ==================== 1. 数据模型测试 ====================

    @Test
    fun `SpeechRecognitionState Idle is correct type`() {
        val state: SpeechRecognitionState = SpeechRecognitionState.Idle
        assertTrue(state is SpeechRecognitionState.Idle)
    }

    @Test
    fun `SpeechRecognitionState Listening is correct type`() {
        val state: SpeechRecognitionState = SpeechRecognitionState.Listening
        assertTrue(state is SpeechRecognitionState.Listening)
    }

    @Test
    fun `SpeechRecognitionState Processing is correct type`() {
        val state: SpeechRecognitionState = SpeechRecognitionState.Processing
        assertTrue(state is SpeechRecognitionState.Processing)
    }

    @Test
    fun `SpeechRecognitionState Result with valid data`() {
        val state = SpeechRecognitionState.Result(
            text = "餐后散步三十分钟",
            confidence = 0.95f,
            engine = SpeechEngineType.FUNASR_ONNX,
            durationMs = 1500
        )
        assertEquals("餐后散步三十分钟", state.text)
        assertEquals(0.95f, state.confidence, 0.001f)
        assertEquals(SpeechEngineType.FUNASR_ONNX, state.engine)
        assertEquals(1500L, state.durationMs)
    }

    @Test
    fun `SpeechRecognitionState Result with default values`() {
        val state = SpeechRecognitionState.Result(text = "测试")
        assertEquals("测试", state.text)
        assertEquals(1.0f, state.confidence, 0.001f)
        assertEquals(SpeechEngineType.ANDROID_BUILTIN, state.engine)
        assertEquals(0L, state.durationMs)
    }

    @Test
    fun `SpeechRecognitionState PartialResult`() {
        val state = SpeechRecognitionState.PartialResult(text = "正在识别")
        assertEquals("正在识别", state.text)
    }

    @Test
    fun `SpeechRecognitionState Error with message and code`() {
        val state = SpeechRecognitionState.Error(message = "权限不足", code = 3)
        assertEquals("权限不足", state.message)
        assertEquals(3, state.code)
    }

    @Test
    fun `SpeechRecognitionState Error with default code`() {
        val state = SpeechRecognitionState.Error(message = "网络错误")
        assertEquals(-1, state.code)
    }

    @Test
    fun `SpeechEngineType enum values`() {
        assertEquals("FunASR离线", SpeechEngineType.FUNASR_ONNX.displayName)
        assertEquals("Android内置", SpeechEngineType.ANDROID_BUILTIN.displayName)
        assertEquals("不可用", SpeechEngineType.NONE.displayName)
        assertEquals(3, SpeechEngineType.values().size)
    }

    // ==================== 2. 配置测试 ====================

    @Test
    fun `SpeechRecognitionConfig default values`() {
        val config = SpeechRecognitionConfig()
        assertEquals(16000, config.sampleRate)
        assertEquals(1, config.channelCount)
        assertEquals(16, config.bitsPerSample)
        assertEquals(60_000L, config.maxDurationMs)
        assertEquals(2_000L, config.silenceTimeoutMs)
        assertTrue(config.preferOffline)
        assertEquals("zh-CN", config.language)
    }

    @Test
    fun `SpeechRecognitionConfig bytesPerSecond calculation`() {
        val config = SpeechRecognitionConfig(sampleRate = 16000, channelCount = 1, bitsPerSample = 16)
        assertEquals(32000, config.bytesPerSecond) // 16000 * 1 * 2
    }

    @Test
    fun `SpeechRecognitionConfig bytesPerSecond stereo`() {
        val config = SpeechRecognitionConfig(sampleRate = 16000, channelCount = 2, bitsPerSample = 16)
        assertEquals(64000, config.bytesPerSecond) // 16000 * 2 * 2
    }

    @Test
    fun `SpeechRecognitionConfig maxBytes calculation`() {
        val config = SpeechRecognitionConfig(sampleRate = 16000, channelCount = 1, bitsPerSample = 16, maxDurationMs = 10_000)
        assertEquals(320_000L, config.maxBytes) // 10秒 * 32000字节/秒
    }

    @Test
    fun `SpeechRecognitionConfig validation - valid default`() {
        assertTrue(SpeechRecognitionConfig().isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - invalid sample rate`() {
        assertFalse(SpeechRecognitionConfig(sampleRate = 12000).isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - invalid channel count`() {
        assertFalse(SpeechRecognitionConfig(channelCount = 0).isValid())
        assertFalse(SpeechRecognitionConfig(channelCount = 3).isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - invalid duration`() {
        assertFalse(SpeechRecognitionConfig(maxDurationMs = 500).isValid())
        assertFalse(SpeechRecognitionConfig(maxDurationMs = 400_000).isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - invalid silence timeout`() {
        assertFalse(SpeechRecognitionConfig(silenceTimeoutMs = 100).isValid())
        assertFalse(SpeechRecognitionConfig(silenceTimeoutMs = 50_000).isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - blank language`() {
        assertFalse(SpeechRecognitionConfig(language = "").isValid())
    }

    @Test
    fun `SpeechRecognitionConfig validation - all valid sample rates`() {
        for (rate in listOf(8000, 16000, 22050, 44100, 48000)) {
            assertTrue("Sample rate $rate should be valid",
                SpeechRecognitionConfig(sampleRate = rate).isValid())
        }
    }

    // ==================== 3. FbankConfig测试 ====================

    @Test
    fun `FbankConfig default values`() {
        val config = FbankConfig()
        assertEquals(16000, config.sampleRate)
        assertEquals(80, config.numMelBins)
        assertEquals(25, config.frameLengthMs)
        assertEquals(10, config.frameShiftMs)
        assertEquals(0.97f, config.preemphasis, 0.001f)
        assertEquals("hamming", config.windowType)
    }

    @Test
    fun `FbankConfig frameLengthSamples calculation`() {
        val config = FbankConfig(sampleRate = 16000, frameLengthMs = 25)
        assertEquals(400, config.frameLengthSamples) // 16000 * 25 / 1000
    }

    @Test
    fun `FbankConfig frameShiftSamples calculation`() {
        val config = FbankConfig(sampleRate = 16000, frameShiftMs = 10)
        assertEquals(160, config.frameShiftSamples) // 16000 * 10 / 1000
    }

    @Test
    fun `FbankConfig fftSize is next power of 2`() {
        val config = FbankConfig(sampleRate = 16000, frameLengthMs = 25)
        // frameLengthSamples = 400, next power of 2 = 512
        assertEquals(512, config.fftSize)
    }

    @Test
    fun `FbankConfig fftSize when frameLengthSamples is power of 2`() {
        // sampleRate=16000, frameLengthMs=32 => 512 samples, which is already power of 2
        val config = FbankConfig(sampleRate = 16000, frameLengthMs = 32)
        assertEquals(512, config.fftSize)
    }

    @Test
    fun `FbankConfig validation - valid default`() {
        assertTrue(FbankConfig().isValid())
    }

    @Test
    fun `FbankConfig validation - invalid numMelBins`() {
        assertFalse(FbankConfig(numMelBins = 0).isValid())
        assertFalse(FbankConfig(numMelBins = 300).isValid())
    }

    @Test
    fun `FbankConfig validation - frameShift larger than frameLength`() {
        assertFalse(FbankConfig(frameLengthMs = 10, frameShiftMs = 20).isValid())
    }

    @Test
    fun `FbankConfig validation - invalid preemphasis`() {
        assertFalse(FbankConfig(preemphasis = -0.1f).isValid())
        assertFalse(FbankConfig(preemphasis = 1.1f).isValid())
    }

    // ==================== 4. SpeechRecognitionResult测试 ====================

    @Test
    fun `SpeechRecognitionResult isSuccess with non-blank text`() {
        val result = SpeechRecognitionResult(text = "测试文本")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SpeechRecognitionResult isSuccess with blank text`() {
        val result = SpeechRecognitionResult(text = "")
        assertFalse(result.isSuccess)
    }

    @Test
    fun `SpeechRecognitionResult isSuccess with whitespace-only text`() {
        val result = SpeechRecognitionResult(text = "   ")
        assertFalse(result.isSuccess)
    }

    @Test
    fun `SpeechRecognitionResult trimmedText`() {
        val result = SpeechRecognitionResult(text = "  散步三十分钟  ")
        assertEquals("散步三十分钟", result.trimmedText)
    }

    @Test
    fun `SpeechRecognitionResult with all fields`() {
        val result = SpeechRecognitionResult(
            text = "测试",
            confidence = 0.88f,
            engine = SpeechEngineType.FUNASR_ONNX,
            durationMs = 2000,
            isPartial = true
        )
        assertEquals("测试", result.text)
        assertEquals(0.88f, result.confidence, 0.001f)
        assertEquals(SpeechEngineType.FUNASR_ONNX, result.engine)
        assertEquals(2000L, result.durationMs)
        assertTrue(result.isPartial)
    }

    // ==================== 5. FunASR模型路径常量测试 ====================

    @Test
    fun `FunASRModelPaths constants are not blank`() {
        assertTrue(FunASRModelPaths.MODEL_DIR.isNotBlank())
        assertTrue(FunASRModelPaths.ENCODER_MODEL.isNotBlank())
        assertTrue(FunASRModelPaths.DECODER_MODEL.isNotBlank())
        assertTrue(FunASRModelPaths.VOCAB_FILE.isNotBlank())
        assertTrue(FunASRModelPaths.CMVN_FILE.isNotBlank())
    }

    @Test
    fun `FunASRModelPaths REQUIRED_FILES contains encoder and decoder`() {
        assertTrue(FunASRModelPaths.REQUIRED_FILES.contains(FunASRModelPaths.ENCODER_MODEL))
        assertTrue(FunASRModelPaths.REQUIRED_FILES.contains(FunASRModelPaths.DECODER_MODEL))
        assertTrue(FunASRModelPaths.REQUIRED_FILES.contains(FunASRModelPaths.VOCAB_FILE))
    }

    @Test
    fun `FunASRModelPaths models in correct directory`() {
        assertTrue(FunASRModelPaths.ENCODER_MODEL.startsWith(FunASRModelPaths.MODEL_DIR))
        assertTrue(FunASRModelPaths.DECODER_MODEL.startsWith(FunASRModelPaths.MODEL_DIR))
        assertTrue(FunASRModelPaths.VOCAB_FILE.startsWith(FunASRModelPaths.MODEL_DIR))
    }

    @Test
    fun `FunASRModelPaths encoder is onnx format`() {
        assertTrue(FunASRModelPaths.ENCODER_MODEL.endsWith(".onnx"))
    }

    @Test
    fun `FunASRModelPaths decoder is onnx format`() {
        assertTrue(FunASRModelPaths.DECODER_MODEL.endsWith(".onnx"))
    }

    // ==================== 6. Hz/Mel频率转换测试 ====================

    @Test
    fun `hzToMel at 0 Hz returns 0`() {
        assertEquals(0.0f, AudioProcessingUtils.hzToMel(0.0f), 0.01f)
    }

    @Test
    fun `hzToMel at 1000 Hz`() {
        // mel = 2595 * log10(1 + 1000/700) = 2595 * log10(2.4286) ≈ 999.98
        val mel = AudioProcessingUtils.hzToMel(1000.0f)
        assertEquals(1000.0f, mel, 1.0f) // 1000Hz ≈ 1000mel
    }

    @Test
    fun `melToHz at 0 mel returns 0`() {
        assertEquals(0.0f, AudioProcessingUtils.melToHz(0.0f), 0.01f)
    }

    @Test
    fun `hzToMel and melToHz are inverse functions`() {
        val testFreqs = floatArrayOf(0f, 100f, 500f, 1000f, 2000f, 4000f, 8000f)
        for (hz in testFreqs) {
            val mel = AudioProcessingUtils.hzToMel(hz)
            val recoveredHz = AudioProcessingUtils.melToHz(mel)
            assertEquals("Round trip for $hz Hz", hz, recoveredHz, 0.1f)
        }
    }

    @Test
    fun `hzToMel is monotonically increasing`() {
        var prevMel = -1.0f
        for (hz in 0..8000 step 100) {
            val mel = AudioProcessingUtils.hzToMel(hz.toFloat())
            assertTrue("mel should increase: $prevMel < $mel at $hz Hz", mel >= prevMel)
            prevMel = mel
        }
    }

    // ==================== 7. 预加重测试 ====================

    @Test
    fun `preEmphasis with empty array`() {
        val result = AudioProcessingUtils.preEmphasis(floatArrayOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preEmphasis with single element`() {
        val result = AudioProcessingUtils.preEmphasis(floatArrayOf(1.0f), 0.97f)
        assertEquals(1, result.size)
        assertEquals(1.0f, result[0], 0.001f)
    }

    @Test
    fun `preEmphasis formula correctness`() {
        // y[0] = x[0] = 1.0
        // y[1] = x[1] - 0.97 * x[0] = 2.0 - 0.97 * 1.0 = 1.03
        // y[2] = x[2] - 0.97 * x[1] = 3.0 - 0.97 * 2.0 = 1.06
        val input = floatArrayOf(1.0f, 2.0f, 3.0f)
        val result = AudioProcessingUtils.preEmphasis(input, 0.97f)
        assertEquals(3, result.size)
        assertEquals(1.0f, result[0], 0.001f)
        assertEquals(1.03f, result[1], 0.001f)
        assertEquals(1.06f, result[2], 0.001f)
    }

    @Test
    fun `preEmphasis with zero coefficient is identity`() {
        val input = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val result = AudioProcessingUtils.preEmphasis(input, 0.0f)
        for (i in input.indices) {
            assertEquals(input[i], result[i], 0.001f)
        }
    }

    @Test
    fun `preEmphasis with coefficient 1 computes differences`() {
        val input = floatArrayOf(1.0f, 3.0f, 6.0f, 10.0f)
        val result = AudioProcessingUtils.preEmphasis(input, 1.0f)
        assertEquals(1.0f, result[0], 0.001f) // x[0]
        assertEquals(2.0f, result[1], 0.001f) // 3 - 1
        assertEquals(3.0f, result[2], 0.001f) // 6 - 3
        assertEquals(4.0f, result[3], 0.001f) // 10 - 6
    }

    // ==================== 8. Short到Float转换测试 ====================

    @Test
    fun `shortToFloat empty array`() {
        val result = AudioProcessingUtils.shortToFloat(shortArrayOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `shortToFloat normalization`() {
        val input = shortArrayOf(0, 32767, -32768, 16384)
        val result = AudioProcessingUtils.shortToFloat(input)
        assertEquals(0.0f, result[0], 0.001f)
        assertEquals(32767.0f / 32768.0f, result[1], 0.001f) // ≈ 0.99997
        assertEquals(-1.0f, result[2], 0.001f)
        assertEquals(0.5f, result[3], 0.001f)
    }

    // ==================== 9. 窗函数测试 ====================

    @Test
    fun `hammingWindow with length 0`() {
        assertTrue(AudioProcessingUtils.hammingWindow(0).isEmpty())
    }

    @Test
    fun `hammingWindow with length 1`() {
        val window = AudioProcessingUtils.hammingWindow(1)
        assertEquals(1, window.size)
        assertEquals(1.0f, window[0], 0.001f)
    }

    @Test
    fun `hammingWindow symmetry`() {
        val window = AudioProcessingUtils.hammingWindow(400)
        for (i in 0 until 200) {
            assertEquals("Symmetric at index $i",
                window[i], window[399 - i], 0.001f)
        }
    }

    @Test
    fun `hammingWindow edge values`() {
        val window = AudioProcessingUtils.hammingWindow(100)
        // Hamming window: w[0] = 0.54 - 0.46 * cos(0) = 0.54 - 0.46 = 0.08
        assertEquals(0.08f, window[0], 0.01f)
        assertEquals(0.08f, window[99], 0.01f)
    }

    @Test
    fun `hammingWindow center value close to 1`() {
        val window = AudioProcessingUtils.hammingWindow(101)
        // Center: w[50] = 0.54 - 0.46 * cos(π) = 0.54 + 0.46 = 1.0
        assertEquals(1.0f, window[50], 0.01f)
    }

    @Test
    fun `hammingWindow all values in range`() {
        val window = AudioProcessingUtils.hammingWindow(400)
        for (v in window) {
            assertTrue("Value $v should be >= 0", v >= 0.0f)
            assertTrue("Value $v should be <= 1", v <= 1.001f)
        }
    }

    @Test
    fun `hanningWindow with length 0`() {
        assertTrue(AudioProcessingUtils.hanningWindow(0).isEmpty())
    }

    @Test
    fun `hanningWindow edge values are zero`() {
        val window = AudioProcessingUtils.hanningWindow(100)
        // Hanning: w[0] = 0.5 * (1 - cos(0)) = 0
        assertEquals(0.0f, window[0], 0.01f)
        assertEquals(0.0f, window[99], 0.01f)
    }

    @Test
    fun `hanningWindow center value close to 1`() {
        val window = AudioProcessingUtils.hanningWindow(101)
        assertEquals(1.0f, window[50], 0.01f)
    }

    // ==================== 10. 信号分帧测试 ====================

    @Test
    fun `frameSignal empty signal`() {
        val result = AudioProcessingUtils.frameSignal(floatArrayOf(), 400, 160)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `frameSignal zero frame length`() {
        val result = AudioProcessingUtils.frameSignal(FloatArray(1000), 0, 160)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `frameSignal zero frame shift`() {
        val result = AudioProcessingUtils.frameSignal(FloatArray(1000), 400, 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `frameSignal signal shorter than frame length`() {
        val result = AudioProcessingUtils.frameSignal(FloatArray(100), 400, 160)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `frameSignal correct number of frames`() {
        // signal=1000, frameLength=400, frameShift=160
        // numFrames = 1 + (1000-400)/160 = 1 + 3 = 4
        val signal = FloatArray(1000)
        val result = AudioProcessingUtils.frameSignal(signal, 400, 160)
        assertEquals(4, result.size)
    }

    @Test
    fun `frameSignal frame length is correct`() {
        val signal = FloatArray(1000) { it.toFloat() }
        val result = AudioProcessingUtils.frameSignal(signal, 400, 160)
        for (frame in result) {
            assertEquals(400, frame.size)
        }
    }

    @Test
    fun `frameSignal correct frame content`() {
        val signal = FloatArray(10) { it.toFloat() }
        // frameLength=4, frameShift=2
        val result = AudioProcessingUtils.frameSignal(signal, 4, 2)
        // Frame 0: [0,1,2,3], Frame 1: [2,3,4,5], Frame 2: [4,5,6,7], Frame 3: [6,7,8,9]
        assertEquals(4, result.size)
        assertArrayEquals(floatArrayOf(0f, 1f, 2f, 3f), result[0], 0.001f)
        assertArrayEquals(floatArrayOf(2f, 3f, 4f, 5f), result[1], 0.001f)
        assertArrayEquals(floatArrayOf(4f, 5f, 6f, 7f), result[2], 0.001f)
        assertArrayEquals(floatArrayOf(6f, 7f, 8f, 9f), result[3], 0.001f)
    }

    @Test
    fun `frameSignal with non-overlapping frames`() {
        val signal = FloatArray(8) { it.toFloat() }
        val result = AudioProcessingUtils.frameSignal(signal, 4, 4)
        assertEquals(2, result.size)
        assertArrayEquals(floatArrayOf(0f, 1f, 2f, 3f), result[0], 0.001f)
        assertArrayEquals(floatArrayOf(4f, 5f, 6f, 7f), result[1], 0.001f)
    }

    @Test
    fun `frameSignal zero-pads last frame`() {
        // signal=5 samples, frameLength=4, frameShift=4
        // Only 1 frame: [0,1,2,3], second frame starts at 4 with only 1 sample
        // numFrames = 1 + (5-4)/4 = 1 + 0 = 1
        val signal = FloatArray(5) { it.toFloat() }
        val result = AudioProcessingUtils.frameSignal(signal, 4, 4)
        assertEquals(1, result.size)
    }

    // ==================== 11. 窗函数应用测试 ====================

    @Test
    fun `applyWindow empty arrays`() {
        val result = AudioProcessingUtils.applyWindow(floatArrayOf(), floatArrayOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `applyWindow element-wise multiplication`() {
        val frame = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val window = floatArrayOf(0.5f, 1.0f, 1.0f, 0.5f)
        val result = AudioProcessingUtils.applyWindow(frame, window)
        assertArrayEquals(floatArrayOf(0.5f, 2.0f, 3.0f, 2.0f), result, 0.001f)
    }

    @Test
    fun `applyWindow different sizes uses minimum`() {
        val frame = floatArrayOf(1.0f, 2.0f, 3.0f)
        val window = floatArrayOf(0.5f, 1.0f)
        val result = AudioProcessingUtils.applyWindow(frame, window)
        assertEquals(2, result.size)
    }

    // ==================== 12. FFT测试 ====================

    @Test
    fun `fft of DC signal`() {
        val n = 8
        val real = FloatArray(n) { 1.0f }
        val imag = FloatArray(n)
        AudioProcessingUtils.fft(real, imag, n)
        // DC component should be n (sum of all values)
        assertEquals(n.toFloat(), real[0], 0.01f)
        assertEquals(0.0f, imag[0], 0.01f)
        // All other components should be 0
        for (i in 1 until n) {
            assertEquals("real[$i] should be 0", 0.0f, real[i], 0.01f)
            assertEquals("imag[$i] should be 0", 0.0f, imag[i], 0.01f)
        }
    }

    @Test
    fun `fft of single impulse`() {
        val n = 8
        val real = FloatArray(n)
        val imag = FloatArray(n)
        real[0] = 1.0f
        AudioProcessingUtils.fft(real, imag, n)
        // FFT of delta should be all 1s
        for (i in 0 until n) {
            assertEquals("real[$i] should be 1", 1.0f, real[i], 0.01f)
            assertEquals("imag[$i] should be 0", 0.0f, imag[i], 0.01f)
        }
    }

    @Test
    fun `fft preserves Parseval theorem`() {
        // Sum of |x[n]|^2 = (1/N) * Sum of |X[k]|^2
        val n = 16
        val real = FloatArray(n) { (it % 3).toFloat() - 1.0f }
        val imag = FloatArray(n)

        val timeEnergy = real.sumOf { (it * it).toDouble() }

        AudioProcessingUtils.fft(real, imag, n)

        val freqEnergy = (0 until n).sumOf { (real[it] * real[it] + imag[it] * imag[it]).toDouble() } / n

        assertEquals(timeEnergy, freqEnergy, 0.1)
    }

    @Test
    fun `fft with size 1`() {
        val real = floatArrayOf(5.0f)
        val imag = floatArrayOf(0.0f)
        AudioProcessingUtils.fft(real, imag, 1)
        assertEquals(5.0f, real[0], 0.01f)
    }

    // ==================== 13. 功率谱测试 ====================

    @Test
    fun `powerSpectrum output size`() {
        val frame = FloatArray(256)
        val result = AudioProcessingUtils.powerSpectrum(frame, 256)
        assertEquals(129, result.size) // 256/2 + 1
    }

    @Test
    fun `powerSpectrum of zeros is all zeros`() {
        val frame = FloatArray(64)
        val result = AudioProcessingUtils.powerSpectrum(frame, 64)
        for (v in result) {
            assertEquals(0.0f, v, 0.001f)
        }
    }

    @Test
    fun `powerSpectrum of DC signal`() {
        val n = 8
        val frame = FloatArray(n) { 1.0f }
        val result = AudioProcessingUtils.powerSpectrum(frame, n)
        // DC power = n^2 / n = n
        assertEquals(n.toFloat(), result[0], 0.1f)
        // Other bins should be ~0
        for (i in 1 until result.size) {
            assertEquals("Bin $i should be ~0", 0.0f, result[i], 0.01f)
        }
    }

    @Test
    fun `powerSpectrum values are non-negative`() {
        val frame = FloatArray(128) { sin(2.0 * PI * it / 128.0).toFloat() }
        val result = AudioProcessingUtils.powerSpectrum(frame, 128)
        for (v in result) {
            assertTrue("Power should be >= 0, got $v", v >= -0.001f)
        }
    }

    // ==================== 14. Mel滤波器组测试 ====================

    @Test
    fun `melFilterbank correct dimensions`() {
        val filters = AudioProcessingUtils.melFilterbank(80, 512, 16000)
        assertEquals(80, filters.size)
        for (filter in filters) {
            assertEquals(257, filter.size) // 512/2 + 1
        }
    }

    @Test
    fun `melFilterbank values are non-negative`() {
        val filters = AudioProcessingUtils.melFilterbank(80, 512, 16000)
        for (filter in filters) {
            for (v in filter) {
                assertTrue("Filter value should be >= 0, got $v", v >= -0.001f)
            }
        }
    }

    @Test
    fun `melFilterbank triangular shape - values between 0 and 1`() {
        val filters = AudioProcessingUtils.melFilterbank(40, 256, 16000)
        for (filter in filters) {
            for (v in filter) {
                assertTrue("Value should be <= 1, got $v", v <= 1.001f)
            }
        }
    }

    @Test
    fun `melFilterbank with small number of bins`() {
        val filters = AudioProcessingUtils.melFilterbank(4, 64, 8000)
        assertEquals(4, filters.size)
        assertEquals(33, filters[0].size) // 64/2 + 1
    }

    // ==================== 15. Fbank特征计算测试 ====================

    @Test
    fun `computeFbank empty audio`() {
        val result = AudioProcessingUtils.computeFbank(shortArrayOf())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `computeFbank short audio returns no frames`() {
        // 10 samples at 16kHz < 25ms frame (400 samples)
        val result = AudioProcessingUtils.computeFbank(ShortArray(10))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `computeFbank 1 second audio produces correct number of frames`() {
        // 1秒 = 16000个样本, frameLengthMs=25, frameShiftMs=10
        // numFrames = 1 + (16000-400)/160 = 1 + 97 = 98
        val audio = ShortArray(16000) { (sin(2.0 * PI * 440 * it / 16000.0) * 1000).toInt().toShort() }
        val result = AudioProcessingUtils.computeFbank(audio)
        assertEquals(98, result.size)
    }

    @Test
    fun `computeFbank output dimensions match config`() {
        val config = FbankConfig(numMelBins = 80)
        val audio = ShortArray(16000) { (sin(2.0 * PI * 440 * it / 16000.0) * 1000).toInt().toShort() }
        val result = AudioProcessingUtils.computeFbank(audio, config)
        assertTrue(result.isNotEmpty())
        assertEquals(80, result[0].size)
    }

    @Test
    fun `computeFbank with hanning window`() {
        val config = FbankConfig(windowType = "hanning")
        val audio = ShortArray(16000) { (sin(2.0 * PI * 440 * it / 16000.0) * 1000).toInt().toShort() }
        val result = AudioProcessingUtils.computeFbank(audio, config)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `computeFbank output values are finite`() {
        val audio = ShortArray(16000) { (sin(2.0 * PI * 440 * it / 16000.0) * 10000).toInt().toShort() }
        val result = AudioProcessingUtils.computeFbank(audio)
        for (frame in result) {
            for (v in frame) {
                assertTrue("Fbank value should be finite, got $v", v.isFinite())
            }
        }
    }

    @Test
    fun `computeFbank with silence has low energy`() {
        val silence = ShortArray(16000) // all zeros
        val result = AudioProcessingUtils.computeFbank(silence)
        if (result.isNotEmpty()) {
            // Log energy of silence should be very negative (close to ln(1e-10))
            val avgEnergy = result.flatMap { it.toList() }.average()
            assertTrue("Average energy of silence should be very low", avgEnergy < 0)
        }
    }

    // ==================== 16. CMVN归一化测试 ====================

    @Test
    fun `applyCMVN empty features`() {
        val result = AudioProcessingUtils.applyCMVN(emptyArray())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `applyCMVN normalizes to zero mean`() {
        val features = Array(10) { FloatArray(4) { (it + 1).toFloat() * 10 } }
        val result = AudioProcessingUtils.applyCMVN(features)
        // After CMVN, mean should be approximately 0
        for (j in 0 until 4) {
            var sum = 0.0f
            for (i in result.indices) {
                sum += result[i][j]
            }
            assertEquals("Mean of dimension $j should be ~0", 0.0f, sum / result.size, 0.01f)
        }
    }

    @Test
    fun `applyCMVN with global mean and variance`() {
        val features = Array(5) { FloatArray(2) { 10.0f } }
        val globalMean = floatArrayOf(10.0f, 10.0f)
        val globalVar = floatArrayOf(1.0f, 1.0f)
        val result = AudioProcessingUtils.applyCMVN(features, globalMean, globalVar)
        // (10 - 10) / sqrt(1 + 1e-6) ≈ 0
        for (frame in result) {
            for (v in frame) {
                assertEquals(0.0f, v, 0.01f)
            }
        }
    }

    @Test
    fun `applyCMVN preserves dimensions`() {
        val features = Array(20) { FloatArray(80) { it.toFloat() } }
        val result = AudioProcessingUtils.applyCMVN(features)
        assertEquals(20, result.size)
        assertEquals(80, result[0].size)
    }

    @Test
    fun `applyCMVN single frame`() {
        val features = Array(1) { floatArrayOf(5.0f, 10.0f) }
        val result = AudioProcessingUtils.applyCMVN(features)
        // Single frame: mean=value, var=0 => (5-5)/sqrt(0+eps) ≈ 0
        assertEquals(1, result.size)
        assertEquals(0.0f, result[0][0], 0.01f)
        assertEquals(0.0f, result[0][1], 0.01f)
    }

    // ==================== 17. CTC贪心解码测试 ====================

    @Test
    fun `ctcGreedyDecode empty input`() {
        val result = AudioProcessingUtils.ctcGreedyDecode(floatArrayOf(), 0, 0, emptyList())
        assertEquals("", result)
    }

    @Test
    fun `ctcGreedyDecode all blanks returns empty`() {
        // vocabSize=3, timeSteps=3
        // logits: [10,0,0, 10,0,0, 10,0,0] => all argmax=0 (blank)
        val logits = floatArrayOf(10f, 0f, 0f, 10f, 0f, 0f, 10f, 0f, 0f)
        val vocab = listOf("a", "b")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 3, 3, vocab)
        assertEquals("", result)
    }

    @Test
    fun `ctcGreedyDecode simple sequence`() {
        // vocabSize=4 (blank=0, a=1, b=2, c=3)
        // timeSteps=5
        // Expected output: "abc"
        // logits: argmax sequence = [1, 1, 2, 0, 3]
        // After dedup: [1, 2, 0, 3] => remove blank => [1, 2, 3] => "abc"
        val logits = floatArrayOf(
            0f, 10f, 0f, 0f,  // t=0: argmax=1 (a)
            0f, 10f, 0f, 0f,  // t=1: argmax=1 (a) - duplicate, removed
            0f, 0f, 10f, 0f,  // t=2: argmax=2 (b)
            10f, 0f, 0f, 0f,  // t=3: argmax=0 (blank)
            0f, 0f, 0f, 10f   // t=4: argmax=3 (c)
        )
        val vocab = listOf("a", "b", "c")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 5, 4, vocab)
        assertEquals("abc", result)
    }

    @Test
    fun `ctcGreedyDecode repeated chars with blank separator`() {
        // To produce "aa", we need: a, blank, a
        // vocabSize=3 (blank=0, a=1, b=2)
        val logits = floatArrayOf(
            0f, 10f, 0f,   // t=0: a
            10f, 0f, 0f,   // t=1: blank
            0f, 10f, 0f    // t=2: a
        )
        val vocab = listOf("a", "b")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 3, 3, vocab)
        assertEquals("aa", result)
    }

    @Test
    fun `ctcGreedyDecode Chinese characters`() {
        // vocabSize=4 (blank=0, 你=1, 好=2, 吗=3)
        val logits = floatArrayOf(
            0f, 10f, 0f, 0f,  // 你
            0f, 0f, 10f, 0f,  // 好
            0f, 0f, 0f, 10f   // 吗
        )
        val vocab = listOf("你", "好", "吗")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 3, 4, vocab)
        assertEquals("你好吗", result)
    }

    @Test
    fun `ctcGreedyDecode with out-of-bounds index`() {
        // argmax produces an index beyond vocab size
        val logits = floatArrayOf(
            0f, 0f, 0f, 0f, 10f  // argmax=4, but vocab only has 3 items (idx 1,2,3)
        )
        val vocab = listOf("a", "b", "c")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 1, 5, vocab)
        // charIdx = 4-1 = 3, but vocab.indices = 0..2, so 3 is out of bounds
        assertEquals("", result)
    }

    @Test
    fun `ctcGreedyDecode single character`() {
        val logits = floatArrayOf(0f, 10f, 0f)
        val vocab = listOf("x", "y")
        val result = AudioProcessingUtils.ctcGreedyDecode(logits, 1, 3, vocab)
        assertEquals("x", result)
    }

    @Test
    fun `ctcGreedyDecode negative vocabSize returns empty`() {
        val result = AudioProcessingUtils.ctcGreedyDecode(floatArrayOf(1f), 1, -1, listOf("a"))
        assertEquals("", result)
    }

    @Test
    fun `ctcGreedyDecode zero timeSteps returns empty`() {
        val result = AudioProcessingUtils.ctcGreedyDecode(floatArrayOf(1f), 0, 1, listOf("a"))
        assertEquals("", result)
    }

    // ==================== 18. 静音检测测试 ====================

    @Test
    fun `calculateRMS empty array`() {
        assertEquals(0.0f, AudioProcessingUtils.calculateRMS(shortArrayOf()), 0.001f)
    }

    @Test
    fun `calculateRMS all zeros`() {
        assertEquals(0.0f, AudioProcessingUtils.calculateRMS(ShortArray(100)), 0.001f)
    }

    @Test
    fun `calculateRMS constant signal`() {
        val samples = ShortArray(100) { 1000 }
        assertEquals(1000.0f, AudioProcessingUtils.calculateRMS(samples), 0.1f)
    }

    @Test
    fun `calculateRMS positive and negative`() {
        // RMS should be the same for positive and negative values
        val positive = ShortArray(100) { 500 }
        val negative = ShortArray(100) { -500 }
        assertEquals(
            AudioProcessingUtils.calculateRMS(positive),
            AudioProcessingUtils.calculateRMS(negative),
            0.001f
        )
    }

    @Test
    fun `isSilence with zeros`() {
        assertTrue(AudioProcessingUtils.isSilence(ShortArray(100)))
    }

    @Test
    fun `isSilence with loud signal`() {
        val loud = ShortArray(100) { 10000 }
        assertFalse(AudioProcessingUtils.isSilence(loud))
    }

    @Test
    fun `isSilence with custom threshold`() {
        val samples = ShortArray(100) { 100 }
        assertTrue(AudioProcessingUtils.isSilence(samples, 200.0f))
        assertFalse(AudioProcessingUtils.isSilence(samples, 50.0f))
    }

    @Test
    fun `isSilence empty array is silent`() {
        assertTrue(AudioProcessingUtils.isSilence(shortArrayOf()))
    }

    // ==================== 19. nextPowerOf2测试 ====================

    @Test
    fun `nextPowerOf2 for various inputs`() {
        assertEquals(1, AudioProcessingUtils.nextPowerOf2(0))
        assertEquals(1, AudioProcessingUtils.nextPowerOf2(1))
        assertEquals(2, AudioProcessingUtils.nextPowerOf2(2))
        assertEquals(4, AudioProcessingUtils.nextPowerOf2(3))
        assertEquals(4, AudioProcessingUtils.nextPowerOf2(4))
        assertEquals(8, AudioProcessingUtils.nextPowerOf2(5))
        assertEquals(256, AudioProcessingUtils.nextPowerOf2(200))
        assertEquals(512, AudioProcessingUtils.nextPowerOf2(400))
        assertEquals(512, AudioProcessingUtils.nextPowerOf2(512))
        assertEquals(1024, AudioProcessingUtils.nextPowerOf2(513))
    }

    @Test
    fun `nextPowerOf2 negative returns 1`() {
        assertEquals(1, AudioProcessingUtils.nextPowerOf2(-5))
    }

    // ==================== 20. 端到端集成测试 ====================

    @Test
    fun `full fbank pipeline with sine wave`() {
        // 生成440Hz正弦波（1秒，16kHz采样率）
        val sampleRate = 16000
        val freq = 440.0
        val audio = ShortArray(sampleRate) {
            (sin(2.0 * PI * freq * it / sampleRate) * 10000).toInt().toShort()
        }

        val config = FbankConfig()
        val features = AudioProcessingUtils.computeFbank(audio, config)

        // 验证维度
        assertTrue("Should have frames", features.isNotEmpty())
        assertEquals("Each frame should have 80 mel bins", 80, features[0].size)

        // 验证所有值有限
        for (frame in features) {
            for (v in frame) {
                assertTrue("Value should be finite", v.isFinite())
            }
        }
    }

    @Test
    fun `full fbank pipeline with CMVN`() {
        val audio = ShortArray(16000) {
            (sin(2.0 * PI * 440 * it / 16000.0) * 5000).toInt().toShort()
        }
        val features = AudioProcessingUtils.computeFbank(audio)
        val normalized = AudioProcessingUtils.applyCMVN(features)

        assertEquals(features.size, normalized.size)
        assertEquals(features[0].size, normalized[0].size)

        // 归一化后的均值应该接近0
        val numBins = normalized[0].size
        for (j in 0 until numBins) {
            var sum = 0.0f
            for (frame in normalized) {
                sum += frame[j]
            }
            val mean = sum / normalized.size
            assertEquals("Bin $j mean should be ~0", 0.0f, mean, 0.1f)
        }
    }

    @Test
    fun `CTC decode with fbank-like output`() {
        // 模拟一个简化的识别场景
        val vocab = listOf("散", "步", "三", "十", "分", "钟")
        val vocabSize = vocab.size + 1 // +1 for blank

        // 构造logits使得每个时间步选择正确的字符
        // 预期输出："散步三十分钟"
        val expectedIndices = listOf(1, 2, 3, 4, 5, 6) // 对应vocab索引+1
        val timeSteps = expectedIndices.size * 2 // 每个字符重复2个时间步
        val logits = FloatArray(timeSteps * vocabSize)

        for (t in 0 until timeSteps) {
            val charIdx = expectedIndices[t / 2]
            logits[t * vocabSize + charIdx] = 10.0f
        }

        val result = AudioProcessingUtils.ctcGreedyDecode(logits, timeSteps, vocabSize, vocab)
        assertEquals("散步三十分钟", result)
    }

    // ==================== 21. 状态转换测试 ====================

    @Test
    fun `SpeechRecognitionState type checking covers all cases`() {
        val states = listOf<SpeechRecognitionState>(
            SpeechRecognitionState.Idle,
            SpeechRecognitionState.Listening,
            SpeechRecognitionState.Processing,
            SpeechRecognitionState.Result("text"),
            SpeechRecognitionState.PartialResult("partial"),
            SpeechRecognitionState.Error("error")
        )

        // Verify each state is a distinct type
        val types = states.map { it::class }.toSet()
        assertEquals(6, types.size)
    }

    @Test
    fun `SpeechRecognitionState Result equality`() {
        val r1 = SpeechRecognitionState.Result("测试", 0.9f, SpeechEngineType.FUNASR_ONNX, 100)
        val r2 = SpeechRecognitionState.Result("测试", 0.9f, SpeechEngineType.FUNASR_ONNX, 100)
        assertEquals(r1, r2)
    }

    @Test
    fun `SpeechRecognitionState Result inequality`() {
        val r1 = SpeechRecognitionState.Result("测试1")
        val r2 = SpeechRecognitionState.Result("测试2")
        assertNotEquals(r1, r2)
    }

    @Test
    fun `SpeechRecognitionState Error equality`() {
        val e1 = SpeechRecognitionState.Error("err", 1)
        val e2 = SpeechRecognitionState.Error("err", 1)
        assertEquals(e1, e2)
    }
}
