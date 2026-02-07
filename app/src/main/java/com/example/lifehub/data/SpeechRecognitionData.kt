package com.example.lifehub.data

import kotlin.math.*

/**
 * Phase 45: 语音识别数据模型与工具类
 *
 * 支持FunASR离线语音识别（ONNX）和Android内置语音识别的数据层
 */

// ==================== 状态枚举 ====================

/**
 * 语音识别状态
 */
sealed class SpeechRecognitionState {
    /** 空闲状态 */
    object Idle : SpeechRecognitionState()

    /** 正在监听录音 */
    object Listening : SpeechRecognitionState()

    /** 正在处理音频（FunASR ONNX推理中） */
    object Processing : SpeechRecognitionState()

    /** 识别成功，返回结果 */
    data class Result(
        val text: String,
        val confidence: Float = 1.0f,
        val engine: SpeechEngineType = SpeechEngineType.ANDROID_BUILTIN,
        val durationMs: Long = 0
    ) : SpeechRecognitionState()

    /** 部分识别结果（实时反馈） */
    data class PartialResult(val text: String) : SpeechRecognitionState()

    /** 识别出错 */
    data class Error(val message: String, val code: Int = -1) : SpeechRecognitionState()
}

/**
 * 语音识别引擎类型
 */
enum class SpeechEngineType(val displayName: String) {
    /** FunASR离线ONNX引擎 */
    FUNASR_ONNX("FunASR离线"),

    /** Android内置语音识别 */
    ANDROID_BUILTIN("Android内置"),

    /** 无可用引擎 */
    NONE("不可用")
}

// ==================== 配置类 ====================

/**
 * 语音识别配置
 *
 * @param sampleRate 采样率（Hz），FunASR要求16000
 * @param channelCount 声道数，1=单声道
 * @param bitsPerSample 采样位深，16bit
 * @param maxDurationMs 最大录音时长（毫秒）
 * @param silenceTimeoutMs 静音超时（毫秒），检测到静音后自动停止
 * @param preferOffline 是否优先使用离线识别
 * @param language 识别语言
 */
data class SpeechRecognitionConfig(
    val sampleRate: Int = 16000,
    val channelCount: Int = 1,
    val bitsPerSample: Int = 16,
    val maxDurationMs: Long = 60_000,
    val silenceTimeoutMs: Long = 2_000,
    val preferOffline: Boolean = true,
    val language: String = "zh-CN"
) {
    /** 每秒字节数 = 采样率 × 声道数 × (位深/8) */
    val bytesPerSecond: Int get() = sampleRate * channelCount * (bitsPerSample / 8)

    /** 最大录音字节数 */
    val maxBytes: Long get() = (maxDurationMs * bytesPerSecond) / 1000

    /** 验证配置有效性 */
    fun isValid(): Boolean {
        return sampleRate in listOf(8000, 16000, 22050, 44100, 48000) &&
                channelCount in 1..2 &&
                bitsPerSample in listOf(8, 16) &&
                maxDurationMs in 1000..300_000 &&
                silenceTimeoutMs in 500..30_000 &&
                language.isNotBlank()
    }
}

/**
 * FunASR Fbank特征提取配置
 *
 * @param sampleRate 采样率
 * @param numMelBins Mel滤波器组数量（FunASR默认80）
 * @param frameLengthMs 帧长（毫秒）
 * @param frameShiftMs 帧移（毫秒）
 * @param preemphasis 预加重系数
 * @param windowType 窗函数类型
 */
data class FbankConfig(
    val sampleRate: Int = 16000,
    val numMelBins: Int = 80,
    val frameLengthMs: Int = 25,
    val frameShiftMs: Int = 10,
    val preemphasis: Float = 0.97f,
    val windowType: String = "hamming"
) {
    /** 帧长对应的采样点数 */
    val frameLengthSamples: Int get() = sampleRate * frameLengthMs / 1000

    /** 帧移对应的采样点数 */
    val frameShiftSamples: Int get() = sampleRate * frameShiftMs / 1000

    /** FFT点数（取帧长对应的下一个2的幂次） */
    val fftSize: Int get() {
        var n = 1
        while (n < frameLengthSamples) n *= 2
        return n
    }

    /** 验证配置有效性 */
    fun isValid(): Boolean {
        return sampleRate > 0 &&
                numMelBins in 1..256 &&
                frameLengthMs in 1..100 &&
                frameShiftMs in 1..frameLengthMs &&
                preemphasis in 0.0f..1.0f
    }
}

// ==================== 结果类 ====================

/**
 * 语音识别结果
 */
data class SpeechRecognitionResult(
    val text: String,
    val confidence: Float = 1.0f,
    val engine: SpeechEngineType = SpeechEngineType.NONE,
    val durationMs: Long = 0,
    val isPartial: Boolean = false
) {
    /** 识别是否成功（有非空文本） */
    val isSuccess: Boolean get() = text.isNotBlank()

    /** 获取去除首尾空白的文本 */
    val trimmedText: String get() = text.trim()
}

// ==================== FunASR模型文件路径常量 ====================

/**
 * FunASR ONNX模型文件路径
 */
object FunASRModelPaths {
    /** 模型目录（assets下） */
    const val MODEL_DIR = "models/funasr"

    /** Paraformer编码器模型 */
    const val ENCODER_MODEL = "models/funasr/paraformer_encoder.onnx"

    /** Paraformer解码器模型 */
    const val DECODER_MODEL = "models/funasr/paraformer_decoder.onnx"

    /** 词表文件 */
    const val VOCAB_FILE = "models/funasr/vocab.txt"

    /** CMVN统计文件（均值和方差） */
    const val CMVN_FILE = "models/funasr/cmvn.txt"

    /** 所有必需文件列表 */
    val REQUIRED_FILES = listOf(ENCODER_MODEL, DECODER_MODEL, VOCAB_FILE)
}

// ==================== 音频处理工具类 ====================

/**
 * 音频处理工具类（纯函数，可独立测试）
 *
 * 提供FunASR所需的音频预处理功能：
 * - 预加重滤波
 * - 分帧
 * - 窗函数
 * - 功率谱计算
 * - Mel滤波器组
 * - Fbank特征提取
 */
object AudioProcessingUtils {

    /**
     * Hz频率转Mel频率
     * 公式：mel = 2595 * log10(1 + hz / 700)
     */
    fun hzToMel(hz: Float): Float {
        return 2595.0f * log10(1.0f + hz / 700.0f)
    }

    /**
     * Mel频率转Hz频率
     * 公式：hz = 700 * (10^(mel/2595) - 1)
     */
    fun melToHz(mel: Float): Float {
        return 700.0f * (10.0f.pow(mel / 2595.0f) - 1.0f)
    }

    /**
     * 预加重滤波
     * 公式：y[n] = x[n] - coeff * x[n-1]
     * 增强高频成分，改善语音识别效果
     *
     * @param samples 原始PCM样本（short→float归一化）
     * @param coeff 预加重系数，通常0.97
     * @return 预加重后的信号
     */
    fun preEmphasis(samples: FloatArray, coeff: Float = 0.97f): FloatArray {
        if (samples.isEmpty()) return floatArrayOf()
        val result = FloatArray(samples.size)
        result[0] = samples[0]
        for (i in 1 until samples.size) {
            result[i] = samples[i] - coeff * samples[i - 1]
        }
        return result
    }

    /**
     * Short数组转Float数组（归一化到[-1, 1]）
     */
    fun shortToFloat(samples: ShortArray): FloatArray {
        return FloatArray(samples.size) { samples[it].toFloat() / 32768.0f }
    }

    /**
     * 生成Hamming窗函数
     * 公式：w[n] = 0.54 - 0.46 * cos(2π * n / (N-1))
     *
     * @param length 窗长度
     * @return 窗函数数组
     */
    fun hammingWindow(length: Int): FloatArray {
        if (length <= 0) return floatArrayOf()
        if (length == 1) return floatArrayOf(1.0f)
        return FloatArray(length) { n ->
            0.54f - 0.46f * cos(2.0f * PI.toFloat() * n / (length - 1))
        }
    }

    /**
     * 生成Hanning窗函数
     * 公式：w[n] = 0.5 * (1 - cos(2π * n / (N-1)))
     */
    fun hanningWindow(length: Int): FloatArray {
        if (length <= 0) return floatArrayOf()
        if (length == 1) return floatArrayOf(1.0f)
        return FloatArray(length) { n ->
            0.5f * (1.0f - cos(2.0f * PI.toFloat() * n / (length - 1)))
        }
    }

    /**
     * 信号分帧
     * 将连续信号切分为重叠的帧
     *
     * @param signal 输入信号
     * @param frameLength 帧长（采样点数）
     * @param frameShift 帧移（采样点数）
     * @return 帧数组，每帧长度为frameLength
     */
    fun frameSignal(
        signal: FloatArray,
        frameLength: Int,
        frameShift: Int
    ): Array<FloatArray> {
        if (signal.isEmpty() || frameLength <= 0 || frameShift <= 0) {
            return emptyArray()
        }

        val numFrames = if (signal.size >= frameLength) {
            1 + (signal.size - frameLength) / frameShift
        } else {
            0
        }

        if (numFrames <= 0) return emptyArray()

        return Array(numFrames) { i ->
            val start = i * frameShift
            FloatArray(frameLength) { j ->
                val idx = start + j
                if (idx < signal.size) signal[idx] else 0.0f
            }
        }
    }

    /**
     * 对帧应用窗函数
     *
     * @param frame 输入帧
     * @param window 窗函数
     * @return 加窗后的帧
     */
    fun applyWindow(frame: FloatArray, window: FloatArray): FloatArray {
        val length = minOf(frame.size, window.size)
        return FloatArray(length) { i -> frame[i] * window[i] }
    }

    /**
     * 计算功率谱（使用实数FFT）
     * |X(k)|^2 / N
     *
     * @param frame 加窗后的帧（长度应为2的幂次）
     * @return 功率谱（长度为 fftSize/2 + 1）
     */
    fun powerSpectrum(frame: FloatArray, fftSize: Int): FloatArray {
        // 零填充到fftSize
        val paddedReal = FloatArray(fftSize)
        val paddedImag = FloatArray(fftSize)
        for (i in frame.indices) {
            if (i < fftSize) paddedReal[i] = frame[i]
        }

        // 执行FFT
        fft(paddedReal, paddedImag, fftSize)

        // 计算功率谱（只取前半部分+1个点）
        val specSize = fftSize / 2 + 1
        return FloatArray(specSize) { k ->
            (paddedReal[k] * paddedReal[k] + paddedImag[k] * paddedImag[k]) / fftSize
        }
    }

    /**
     * Cooley-Tukey基2 FFT（就地计算）
     * 要求n为2的幂次
     */
    fun fft(real: FloatArray, imag: FloatArray, n: Int) {
        if (n <= 1) return

        // 位反转排列
        var j = 0
        for (i in 0 until n) {
            if (j > i) {
                var temp = real[j]; real[j] = real[i]; real[i] = temp
                temp = imag[j]; imag[j] = imag[i]; imag[i] = temp
            }
            var m = n / 2
            while (m >= 1 && j >= m) {
                j -= m
                m /= 2
            }
            j += m
        }

        // 蝶形运算
        var step = 2
        while (step <= n) {
            val halfStep = step / 2
            val angle = -2.0 * PI / step
            for (k in 0 until halfStep) {
                val wr = cos(angle * k).toFloat()
                val wi = sin(angle * k).toFloat()
                var i = k
                while (i < n) {
                    val jIdx = i + halfStep
                    val tr = wr * real[jIdx] - wi * imag[jIdx]
                    val ti = wr * imag[jIdx] + wi * real[jIdx]
                    real[jIdx] = real[i] - tr
                    imag[jIdx] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                    i += step
                }
            }
            step *= 2
        }
    }

    /**
     * 生成Mel滤波器组矩阵
     *
     * @param numMelBins Mel滤波器数量
     * @param fftSize FFT点数
     * @param sampleRate 采样率
     * @return 滤波器组矩阵 [numMelBins][fftSize/2+1]
     */
    fun melFilterbank(
        numMelBins: Int,
        fftSize: Int,
        sampleRate: Int
    ): Array<FloatArray> {
        val specSize = fftSize / 2 + 1
        val lowFreq = 0.0f
        val highFreq = sampleRate / 2.0f

        val lowMel = hzToMel(lowFreq)
        val highMel = hzToMel(highFreq)

        // 在Mel域均匀分布的中心频率点
        val melPoints = FloatArray(numMelBins + 2) { i ->
            lowMel + i * (highMel - lowMel) / (numMelBins + 1)
        }

        // 转回Hz并映射到FFT bin索引
        val binPoints = IntArray(numMelBins + 2) { i ->
            val hz = melToHz(melPoints[i])
            ((fftSize + 1).toFloat() * hz / sampleRate).toInt().coerceIn(0, specSize - 1)
        }

        // 构建三角滤波器
        return Array(numMelBins) { m ->
            val filterBank = FloatArray(specSize)
            val startBin = binPoints[m]
            val centerBin = binPoints[m + 1]
            val endBin = binPoints[m + 2]

            // 上升斜坡
            for (k in startBin until centerBin) {
                if (centerBin != startBin) {
                    filterBank[k] = (k - startBin).toFloat() / (centerBin - startBin)
                }
            }

            // 下降斜坡
            for (k in centerBin..endBin) {
                if (endBin != centerBin) {
                    filterBank[k] = (endBin - k).toFloat() / (endBin - centerBin)
                }
            }

            filterBank
        }
    }

    /**
     * 计算Fbank特征（完整流水线）
     *
     * 1. short→float归一化
     * 2. 预加重
     * 3. 分帧
     * 4. 加Hamming窗
     * 5. 功率谱
     * 6. Mel滤波器组
     * 7. 取对数
     *
     * @param audioSamples PCM 16bit音频样本
     * @param config Fbank配置
     * @return Fbank特征矩阵 [numFrames][numMelBins]
     */
    fun computeFbank(
        audioSamples: ShortArray,
        config: FbankConfig = FbankConfig()
    ): Array<FloatArray> {
        if (audioSamples.isEmpty()) return emptyArray()

        // 1. short→float归一化
        val floatSamples = shortToFloat(audioSamples)

        // 2. 预加重
        val emphasized = preEmphasis(floatSamples, config.preemphasis)

        // 3. 分帧
        val frames = frameSignal(
            emphasized,
            config.frameLengthSamples,
            config.frameShiftSamples
        )
        if (frames.isEmpty()) return emptyArray()

        // 4. 窗函数
        val window = when (config.windowType) {
            "hanning" -> hanningWindow(config.frameLengthSamples)
            else -> hammingWindow(config.frameLengthSamples)
        }

        // 5. 生成Mel滤波器组
        val melFilters = melFilterbank(config.numMelBins, config.fftSize, config.sampleRate)

        // 6. 对每帧计算fbank特征
        return Array(frames.size) { frameIdx ->
            val windowedFrame = applyWindow(frames[frameIdx], window)
            val spectrum = powerSpectrum(windowedFrame, config.fftSize)

            // 应用Mel滤波器组并取对数
            FloatArray(config.numMelBins) { m ->
                var energy = 0.0f
                val specSize = minOf(spectrum.size, melFilters[m].size)
                for (k in 0 until specSize) {
                    energy += spectrum[k] * melFilters[m][k]
                }
                // 取对数，避免log(0)
                ln(maxOf(energy, 1e-10f))
            }
        }
    }

    /**
     * CMVN归一化（倒谱均值方差归一化）
     * 公式：x_norm = (x - mean) / sqrt(var + eps)
     *
     * @param features 特征矩阵 [numFrames][numMelBins]
     * @param globalMean 全局均值（可选，为null时使用句级均值）
     * @param globalVar 全局方差（可选，为null时使用句级方差）
     * @return 归一化后的特征
     */
    fun applyCMVN(
        features: Array<FloatArray>,
        globalMean: FloatArray? = null,
        globalVar: FloatArray? = null
    ): Array<FloatArray> {
        if (features.isEmpty()) return emptyArray()

        val numFrames = features.size
        val numBins = features[0].size
        val eps = 1e-6f

        // 计算均值
        val mean = globalMean ?: FloatArray(numBins).also { m ->
            for (frame in features) {
                for (j in 0 until numBins) {
                    m[j] += frame[j]
                }
            }
            for (j in 0 until numBins) {
                m[j] /= numFrames
            }
        }

        // 计算方差
        val variance = globalVar ?: FloatArray(numBins).also { v ->
            for (frame in features) {
                for (j in 0 until numBins) {
                    val diff = frame[j] - mean[j]
                    v[j] += diff * diff
                }
            }
            for (j in 0 until numBins) {
                v[j] /= numFrames
            }
        }

        // 归一化
        return Array(numFrames) { i ->
            FloatArray(numBins) { j ->
                (features[i][j] - mean[j]) / sqrt(variance[j] + eps)
            }
        }
    }

    /**
     * CTC贪心解码
     * 将模型输出的token索引序列转换为文本
     *
     * 规则：
     * 1. 每个时间步取argmax
     * 2. 去除连续重复
     * 3. 去除blank token（索引0）
     * 4. 映射为字符
     *
     * @param logits 模型输出 [timeSteps * vocabSize]
     * @param timeSteps 时间步数
     * @param vocabSize 词表大小
     * @param vocabulary 词表
     * @return 解码后的文本
     */
    fun ctcGreedyDecode(
        logits: FloatArray,
        timeSteps: Int,
        vocabSize: Int,
        vocabulary: List<String>
    ): String {
        if (timeSteps <= 0 || vocabSize <= 0 || logits.isEmpty()) return ""

        val sb = StringBuilder()
        var prevIdx = -1

        for (t in 0 until timeSteps) {
            val offset = t * vocabSize
            var maxIdx = 0
            var maxVal = Float.NEGATIVE_INFINITY

            for (c in 0 until vocabSize) {
                val idx = offset + c
                if (idx < logits.size && logits[idx] > maxVal) {
                    maxVal = logits[idx]
                    maxIdx = c
                }
            }

            // 去重并去除blank（索引0）
            if (maxIdx != 0 && maxIdx != prevIdx) {
                val charIdx = maxIdx - 1
                if (charIdx in vocabulary.indices) {
                    sb.append(vocabulary[charIdx])
                }
            }
            prevIdx = maxIdx
        }

        return sb.toString()
    }

    /**
     * 计算音频能量（RMS）
     * 用于静音检测
     *
     * @param samples PCM样本
     * @return RMS能量值
     */
    fun calculateRMS(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0.0f
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sumSquares / samples.size).toFloat()
    }

    /**
     * 检测是否为静音
     *
     * @param samples PCM样本
     * @param threshold 静音阈值（RMS值）
     * @return 是否为静音
     */
    fun isSilence(samples: ShortArray, threshold: Float = 200.0f): Boolean {
        return calculateRMS(samples) < threshold
    }

    /**
     * 计算下一个2的幂次
     */
    fun nextPowerOf2(n: Int): Int {
        if (n <= 0) return 1
        var v = n - 1
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        return v + 1
    }
}
