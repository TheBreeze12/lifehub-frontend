package com.example.lifehub.services

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.lifehub.data.ActivityRecognitionResult
import com.example.lifehub.data.ActivityRecognitionState
import com.example.lifehub.data.ActivityRecognitionUtils
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult as GmsActivityRecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity Recognition 服务 - Phase 43
 *
 * 集成Android Activity Recognition API，自动识别用户运动状态。
 * 支持识别：静止/步行/跑步/骑行/乘车等状态。
 *
 * 使用方式：
 * 1. 创建实例（传入Application Context）
 * 2. 调用 startRecognition() 开始监听
 * 3. 通过 currentActivity / activityHistory StateFlow 获取结果
 * 4. 调用 stopRecognition() 停止监听
 *
 * 权限需求：
 * - Android 10 (API 29)+ 需要 ACTIVITY_RECOGNITION 运行时权限
 * - Android 9 及以下自动授予
 *
 * @param context Application Context（避免Activity Context导致内存泄漏）
 */
class ActivityRecognitionService(private val context: Context) {

    companion object {
        private const val TAG = "ActivityRecognition"

        /** 活动检测的时间间隔（毫秒），3秒一次 */
        const val DETECTION_INTERVAL_MS = 3000L

        /** 活动历史记录最大保存数量 */
        const val MAX_HISTORY_SIZE = 100

        /** BroadcastReceiver action */
        const val ACTION_ACTIVITY_RECOGNIZED =
            "com.example.lifehub.ACTION_ACTIVITY_RECOGNIZED"
    }

    private val activityRecognitionClient: ActivityRecognitionClient =
        ActivityRecognition.getClient(context)

    /** 服务状态 */
    private val _state = MutableStateFlow<ActivityRecognitionState>(ActivityRecognitionState.Idle)
    val state: StateFlow<ActivityRecognitionState> = _state.asStateFlow()

    /** 当前检测到的活动（置信度最高且超过阈值的） */
    private val _currentActivity = MutableStateFlow<ActivityRecognitionResult?>(null)
    val currentActivity: StateFlow<ActivityRecognitionResult?> = _currentActivity.asStateFlow()

    /** 活动识别历史记录 */
    private val _activityHistory = MutableStateFlow<List<ActivityRecognitionResult>>(emptyList())
    val activityHistory: StateFlow<List<ActivityRecognitionResult>> = _activityHistory.asStateFlow()

    private var pendingIntent: PendingIntent? = null
    private var activityReceiver: BroadcastReceiver? = null

    /**
     * 开始活动识别
     *
     * 注意：Android 10+ 需要 ACTIVITY_RECOGNITION 权限，
     * 请在调用前确保已获取该权限。
     */
    fun startRecognition() {
        if (_state.value is ActivityRecognitionState.Monitoring) {
            Log.d(TAG, "活动识别已在运行中，跳过重复启动")
            return
        }

        // Android 10 (API 29) 以上需要 ACTIVITY_RECOGNITION 运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "缺少ACTIVITY_RECOGNITION权限")
                _state.value = ActivityRecognitionState.Error(
                    "缺少活动识别权限(ACTIVITY_RECOGNITION)，请在设置中授予"
                )
                return
            }
        }

        try {
            registerReceiver()

            val intent = Intent(ACTION_ACTIVITY_RECOGNIZED)
            intent.setPackage(context.packageName)

            pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            activityRecognitionClient.requestActivityUpdates(
                DETECTION_INTERVAL_MS,
                pendingIntent!!
            ).addOnSuccessListener {
                Log.i(TAG, "活动识别启动成功，检测间隔: ${DETECTION_INTERVAL_MS}ms")
                _state.value = ActivityRecognitionState.Monitoring
            }.addOnFailureListener { e ->
                Log.e(TAG, "活动识别启动失败", e)
                _state.value = ActivityRecognitionState.Error(
                    "活动识别启动失败: ${e.message}"
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "权限被拒绝", e)
            _state.value = ActivityRecognitionState.Error("权限被拒绝: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "启动失败", e)
            _state.value = ActivityRecognitionState.Error("启动失败: ${e.message}")
        }
    }

    /**
     * 停止活动识别
     */
    fun stopRecognition() {
        try {
            pendingIntent?.let { pi ->
                activityRecognitionClient.removeActivityUpdates(pi)
                Log.i(TAG, "活动识别已停止")
            }
            unregisterReceiver()
            pendingIntent = null
            _state.value = ActivityRecognitionState.Idle
        } catch (e: Exception) {
            Log.w(TAG, "停止活动识别时发生错误", e)
        }
    }

    /**
     * 清空活动识别历史记录
     */
    fun clearHistory() {
        _activityHistory.value = emptyList()
    }

    /**
     * 处理活动识别结果（由BroadcastReceiver回调）
     *
     * 从GMS ActivityRecognitionResult中提取所有检测到的活动，
     * 映射为本地数据模型，选择最可能的活动并更新状态。
     */
    internal fun handleActivityResult(intent: Intent) {
        if (!GmsActivityRecognitionResult.hasResult(intent)) {
            return
        }

        val gmsResult = GmsActivityRecognitionResult.extractResult(intent) ?: return
        val detectedActivities = gmsResult.probableActivities

        val mappedResults = detectedActivities.map { activity ->
            ActivityRecognitionResult(
                activityType = ActivityRecognitionUtils.mapFromGmsActivityType(activity.type),
                confidence = activity.confidence,
                timestamp = System.currentTimeMillis()
            )
        }

        Log.d(TAG, "检测到 ${mappedResults.size} 个活动: ${
            mappedResults.joinToString { "${it.activityType.label}(${it.confidence}%)" }
        }")

        // 获取最可能的活动
        val mostProbable = ActivityRecognitionUtils.getMostProbableActivity(mappedResults)
        mostProbable?.let { activity ->
            if (ActivityRecognitionUtils.isAcceptableConfidence(activity.confidence)) {
                val previous = _currentActivity.value
                _currentActivity.value = activity

                // 检测活动变化并记录日志
                if (ActivityRecognitionUtils.hasActivityChanged(previous, activity)) {
                    Log.i(TAG, "活动状态变化: ${previous?.activityType?.label ?: "无"} -> " +
                        "${activity.activityType.label} (置信度: ${activity.confidence}%)")
                }
            }
        }

        // 更新历史记录
        mostProbable?.let { activity ->
            val currentHistory = _activityHistory.value.toMutableList()
            currentHistory.add(activity)
            _activityHistory.value = if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.takeLast(MAX_HISTORY_SIZE)
            } else {
                currentHistory.toList()
            }
        }
    }

    /**
     * 注册BroadcastReceiver接收活动识别结果
     */
    private fun registerReceiver() {
        activityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { handleActivityResult(it) }
            }
        }

        val filter = IntentFilter(ACTION_ACTIVITY_RECOGNIZED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                activityReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(activityReceiver, filter)
        }
        Log.d(TAG, "BroadcastReceiver已注册")
    }

    /**
     * 注销BroadcastReceiver
     */
    private fun unregisterReceiver() {
        try {
            activityReceiver?.let {
                context.unregisterReceiver(it)
                Log.d(TAG, "BroadcastReceiver已注销")
            }
            activityReceiver = null
        } catch (e: IllegalArgumentException) {
            // 接收器可能尚未注册
            Log.w(TAG, "注销BroadcastReceiver时发生错误: ${e.message}")
        }
    }
}
