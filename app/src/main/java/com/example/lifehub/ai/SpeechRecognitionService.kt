package com.example.lifehub.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.lifehub.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 45: 语音识别服务
 *
 * 集成FunASR离线语音识别（ONNX）和Android内置语音识别，
 * 支持语音输入运动偏好，将语音转文本结果接入运动计划生成。
 *
 * 引擎选择策略：
 * 1. 优先检测FunASR ONNX模型是否存在（assets/models/funasr/）
 * 2. 若FunASR模型可用，使用ONNX端侧推理（完全离线）
 * 3. 否则回退到Android内置SpeechRecognizer（支持离线，需下载语言包）
 *
 * 使用示例：
 * ```kotlin
 * val service = SpeechRecognitionService(context)
 * // 观察状态
 * service.recognitionState.collect { state -> ... }
 * // 开始识别
 * service.startListening()
 * // 停止识别
 * service.stopListening()
 * // 释放资源
 * service.close()
 * ```
 */
class SpeechRecognitionService(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionService"

        /** 音频缓冲区大小倍数 */
        private const val BUFFER_SIZE_FACTOR = 2

        /** 静音检测窗口大小（采样点数，约100ms） */
        private const val SILENCE_WINDOW_SIZE = 1600

        /** 静音检测阈值（RMS值） */
        private const val SILENCE_THRESHOLD = 200.0f

        /** 最大连续静音帧数（超过则自动停止） */
        private const val MAX_SILENCE_FRAMES = 20
    }

    // ==================== 状态管理 ====================

    private val _recognitionState = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    /** 语音识别状态流，UI层观察此状态 */
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private val _availableEngine = MutableStateFlow(SpeechEngineType.NONE)
    /** 当前可用的识别引擎 */
    val availableEngine: StateFlow<SpeechEngineType> = _availableEngine.asStateFlow()

    // ==================== 配置 ====================

    private val config = SpeechRecognitionConfig()
    private val fbankConfig = FbankConfig()

    // ==================== 内部状态 ====================

    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var androidRecognizer: SpeechRecognizer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // FunASR ONNX引擎（延迟初始化）
    private var onnxInference: OnnxInference? = null
    private var funasrVocabulary: List<String> = emptyList()
    private var funasrReady = false

    init {
        detectAvailableEngine()
    }

    // ==================== 公开方法 ====================

    /**
     * 检测可用的识别引擎
     * 优先检测FunASR ONNX模型，其次检测Android内置SpeechRecognizer
     */
    fun detectAvailableEngine() {
        _availableEngine.value = when {
            isFunASRAvailable() -> SpeechEngineType.FUNASR_ONNX
            isAndroidSpeechAvailable() -> SpeechEngineType.ANDROID_BUILTIN
            else -> SpeechEngineType.NONE
        }
    }

    /**
     * 检查FunASR ONNX模型文件是否可用
     */
    fun isFunASRAvailable(): Boolean {
        return try {
            FunASRModelPaths.REQUIRED_FILES.all { path ->
                context.assets.open(path).use { true }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查Android内置语音识别是否可用
     */
    fun isAndroidSpeechAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * 检查是否有录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 开始语音识别
     * 根据可用引擎自动选择识别方式
     * 当检测结果为NONE时，仍尝试Android内置识别器（部分国产ROM可用但不报告）
     */
    fun startListening() {
        if (isListening) {
            return
        }

        if (!hasRecordPermission()) {
            _recognitionState.value = SpeechRecognitionState.Error(
                message = "未授予录音权限",
                code = 1
            )
            return
        }

        isListening = true
        _recognitionState.value = SpeechRecognitionState.Listening

        when (_availableEngine.value) {
            SpeechEngineType.FUNASR_ONNX -> startFunASRListening()
            SpeechEngineType.ANDROID_BUILTIN -> startAndroidListening()
            SpeechEngineType.NONE -> {
                // 部分国产ROM虽然isRecognitionAvailable返回false但实际可用，
                // 尝试强制使用Android内置识别器作为最后手段
                try {
                    startAndroidListening()
                } catch (e: Exception) {
                    _recognitionState.value = SpeechRecognitionState.Error(
                        message = "无可用的语音识别引擎，请在系统设置中安装语音服务或下载离线语言包",
                        code = 2
                    )
                    isListening = false
                }
            }
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (!isListening) return

        isListening = false

        when (_availableEngine.value) {
            SpeechEngineType.FUNASR_ONNX -> stopFunASRListening()
            SpeechEngineType.ANDROID_BUILTIN -> stopAndroidListening()
            SpeechEngineType.NONE -> {
                // NONE时也可能已使用Android识别器作为fallback，尝试停止
                stopAndroidListening()
            }
        }
    }

    /**
     * 取消语音识别（丢弃结果）
     */
    fun cancel() {
        isListening = false
        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                // 静默处理
            }
        }
        audioRecord = null

        androidRecognizer?.cancel()

        _recognitionState.value = SpeechRecognitionState.Idle
    }

    /**
     * 释放所有资源
     */
    fun close() {
        cancel()
        androidRecognizer?.destroy()
        androidRecognizer = null
        onnxInference?.close()
        onnxInference = null
        coroutineScope.cancel()
    }

    // ==================== FunASR ONNX引擎 ====================

    /**
     * 初始化FunASR ONNX引擎
     * 加载编码器、解码器模型和词表
     */
    private suspend fun initFunASR() = withContext(Dispatchers.IO) {
        if (funasrReady) return@withContext

        try {
            val inference = OnnxInference(context)

            // 加载编码器模型
            inference.loadModel(OnnxModelConfig(
                modelName = "funasr_encoder",
                modelPath = FunASRModelPaths.ENCODER_MODEL,
                inputNames = listOf("speech", "speech_lengths"),
                outputNames = listOf("encoder_out", "encoder_out_lens"),
                numThreads = 4
            ))

            // 加载解码器模型
            inference.loadModel(OnnxModelConfig(
                modelName = "funasr_decoder",
                modelPath = FunASRModelPaths.DECODER_MODEL,
                inputNames = listOf("encoder_out", "encoder_out_lens"),
                outputNames = listOf("logits"),
                numThreads = 4
            ))

            // 加载词表
            funasrVocabulary = context.assets.open(FunASRModelPaths.VOCAB_FILE)
                .bufferedReader()
                .readLines()
                .filter { it.isNotEmpty() }

            onnxInference = inference
            funasrReady = true
        } catch (e: Exception) {
            println("$TAG: FunASR初始化失败: ${e.message}")
            funasrReady = false
            // 回退到Android内置引擎
            _availableEngine.value = if (isAndroidSpeechAvailable()) {
                SpeechEngineType.ANDROID_BUILTIN
            } else {
                SpeechEngineType.NONE
            }
        }
    }

    /**
     * 使用FunASR引擎开始录音和识别
     */
    private fun startFunASRListening() {
        recordingJob = coroutineScope.launch {
            try {
                // 确保FunASR已初始化
                initFunASR()

                if (!funasrReady) {
                    // 回退到Android内置
                    if (isAndroidSpeechAvailable()) {
                        _availableEngine.value = SpeechEngineType.ANDROID_BUILTIN
                        startAndroidListening()
                    } else {
                        _recognitionState.value = SpeechRecognitionState.Error(
                            message = "FunASR模型加载失败，无可用引擎"
                        )
                        isListening = false
                    }
                    return@launch
                }

                // 录音并处理
                val audioData = recordAudio()

                if (audioData.isEmpty()) {
                    _recognitionState.value = SpeechRecognitionState.Error(
                        message = "未录到有效音频"
                    )
                    isListening = false
                    return@launch
                }

                // 切换到处理状态
                _recognitionState.value = SpeechRecognitionState.Processing

                // FunASR推理
                val startTime = System.currentTimeMillis()
                val text = processFunASR(audioData)
                val duration = System.currentTimeMillis() - startTime

                if (text.isNotBlank()) {
                    _recognitionState.value = SpeechRecognitionState.Result(
                        text = text,
                        confidence = 1.0f,
                        engine = SpeechEngineType.FUNASR_ONNX,
                        durationMs = duration
                    )
                } else {
                    _recognitionState.value = SpeechRecognitionState.Error(
                        message = "未识别到语音内容"
                    )
                }
            } catch (e: CancellationException) {
                // 被取消，不处理
            } catch (e: Exception) {
                _recognitionState.value = SpeechRecognitionState.Error(
                    message = "语音识别失败: ${e.message}"
                )
            } finally {
                isListening = false
            }
        }
    }

    /**
     * 录制音频
     * 使用AudioRecord录制PCM 16kHz 16bit单声道音频
     *
     * @return 录制的音频样本
     */
    private suspend fun recordAudio(): ShortArray = withContext(Dispatchers.IO) {
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                config.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * BUFFER_SIZE_FACTOR,
            config.sampleRate // 至少1秒的缓冲
        )

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            audioRecord = null
            return@withContext ShortArray(0)
        }

        val audioBuffer = mutableListOf<Short>()
        val readBuffer = ShortArray(SILENCE_WINDOW_SIZE)
        var silenceFrameCount = 0
        val maxSamples = (config.maxDurationMs * config.sampleRate / 1000).toInt()

        record.startRecording()

        try {
            while (isListening && audioBuffer.size < maxSamples) {
                val readCount = record.read(readBuffer, 0, readBuffer.size)
                if (readCount > 0) {
                    for (i in 0 until readCount) {
                        audioBuffer.add(readBuffer[i])
                    }

                    // 静音检测
                    val windowSamples = readBuffer.copyOfRange(0, readCount)
                    if (AudioProcessingUtils.isSilence(windowSamples, SILENCE_THRESHOLD)) {
                        silenceFrameCount++
                        if (silenceFrameCount >= MAX_SILENCE_FRAMES && audioBuffer.size > config.sampleRate) {
                            // 检测到持续静音且已录制超过1秒，自动停止
                            break
                        }
                    } else {
                        silenceFrameCount = 0
                    }
                } else {
                    break
                }
            }
        } finally {
            try {
                record.stop()
                record.release()
            } catch (e: Exception) {
                // 静默处理
            }
            audioRecord = null
        }

        audioBuffer.toShortArray()
    }

    /**
     * 使用FunASR ONNX模型处理音频
     *
     * 流水线：
     * 1. 计算Fbank特征
     * 2. CMVN归一化
     * 3. 编码器推理
     * 4. 解码器推理
     * 5. CTC贪心解码
     *
     * @param audioSamples PCM 16bit音频样本
     * @return 识别出的文本
     */
    private suspend fun processFunASR(audioSamples: ShortArray): String = withContext(Dispatchers.IO) {
        val inference = onnxInference ?: return@withContext ""

        // 1. 计算Fbank特征
        val fbankFeatures = AudioProcessingUtils.computeFbank(audioSamples, fbankConfig)
        if (fbankFeatures.isEmpty()) return@withContext ""

        // 2. CMVN归一化（使用句级归一化）
        val normalizedFeatures = AudioProcessingUtils.applyCMVN(fbankFeatures)

        val numFrames = normalizedFeatures.size
        val numBins = fbankConfig.numMelBins

        // 3. 展平特征为一维数组 [1, T, D]
        val flatFeatures = FloatArray(numFrames * numBins)
        for (t in 0 until numFrames) {
            System.arraycopy(normalizedFeatures[t], 0, flatFeatures, t * numBins, numBins)
        }

        // 4. 编码器推理
        val encoderResult = inference.runInference(
            "funasr_encoder",
            "speech",
            flatFeatures,
            longArrayOf(1, numFrames.toLong(), numBins.toLong())
        )

        if (encoderResult is InferenceResult.Error) {
            println("$TAG: 编码器推理失败: ${encoderResult.message}")
            return@withContext ""
        }

        val encoderOutput = (encoderResult as InferenceResult.Success)
            .outputs.values.first() as FloatArray

        // 5. 解码器推理（简化：直接对编码器输出做CTC解码）
        val decoderResult = inference.runInference(
            "funasr_decoder",
            "encoder_out",
            encoderOutput,
            longArrayOf(1, (encoderOutput.size / 512).toLong(), 512)
        )

        if (decoderResult is InferenceResult.Error) {
            println("$TAG: 解码器推理失败: ${decoderResult.message}")
            return@withContext ""
        }

        val logits = (decoderResult as InferenceResult.Success)
            .outputs.values.first() as FloatArray

        // 6. CTC贪心解码
        val vocabSize = funasrVocabulary.size + 1
        val timeSteps = logits.size / vocabSize

        AudioProcessingUtils.ctcGreedyDecode(logits, timeSteps, vocabSize, funasrVocabulary)
    }

    /** 停止FunASR录音 */
    private fun stopFunASRListening() {
        // 设置isListening=false会触发录音循环退出
        // recordingJob会继续处理已录制的音频
    }

    // ==================== Android内置语音识别引擎 ====================

    /**
     * 使用Android内置SpeechRecognizer开始识别
     */
    private fun startAndroidListening() {
        try {
            if (androidRecognizer == null) {
                androidRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            androidRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _recognitionState.value = SpeechRecognitionState.Listening
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _recognitionState.value = SpeechRecognitionState.Processing
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音输入超时"
                        else -> "未知错误 ($error)"
                    }
                    _recognitionState.value = SpeechRecognitionState.Error(
                        message = message,
                        code = error
                    )
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val confidence = results?.getFloatArray(
                        SpeechRecognizer.CONFIDENCE_SCORES
                    )

                    if (!matches.isNullOrEmpty()) {
                        _recognitionState.value = SpeechRecognitionState.Result(
                            text = matches[0],
                            confidence = confidence?.firstOrNull() ?: 1.0f,
                            engine = SpeechEngineType.ANDROID_BUILTIN
                        )
                    } else {
                        _recognitionState.value = SpeechRecognitionState.Error(
                            message = "未识别到语音内容"
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                        _recognitionState.value = SpeechRecognitionState.PartialResult(
                            text = matches[0]
                        )
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            // 创建识别Intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    config.silenceTimeoutMs
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS, 3
                )
                // 优先使用离线识别（如果设备支持）
                if (config.preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            androidRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            _recognitionState.value = SpeechRecognitionState.Error(
                message = "启动语音识别失败: ${e.message}"
            )
        }
    }

    /** 停止Android内置语音识别 */
    private fun stopAndroidListening() {
        try {
            androidRecognizer?.stopListening()
        } catch (e: Exception) {
            // 静默处理
        }
    }

    /**
     * 重置状态为空闲
     */
    fun resetState() {
        if (!isListening) {
            _recognitionState.value = SpeechRecognitionState.Idle
        }
    }
}
