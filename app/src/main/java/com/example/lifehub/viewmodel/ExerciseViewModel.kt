package com.example.lifehub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.ActivityRecognitionResult
import com.example.lifehub.data.ActivityRecognitionState
import com.example.lifehub.data.ActivityRecognitionUtils
import com.example.lifehub.data.CreateExerciseRecordRequest
import com.example.lifehub.data.ExerciseTrackingData
import com.example.lifehub.data.ExerciseTrackingState
import com.example.lifehub.data.ExerciseTrackingUtils
import com.example.lifehub.data.SaveExerciseState
import com.example.lifehub.data.TrackPoint
import com.example.lifehub.network.RetrofitClient
import com.example.lifehub.data.HealthConnectAvailabilityStatus
import com.example.lifehub.data.HealthConnectData
import com.example.lifehub.data.HealthConnectSyncState
import com.example.lifehub.services.ActivityRecognitionService
import com.example.lifehub.services.HealthConnectService
import com.example.lifehub.services.LocationTrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 运动追踪ViewModel - Phase 27
 *
 * 管理运动追踪的完整生命周期：
 * - 启动/暂停/恢复/停止追踪
 * - 实时计算距离、配速、时间
 * - 管理GPS位置更新
 * - 估算热量消耗
 * - Phase 43: Activity Recognition自动识别运动状态
 * - Phase 44: Health Connect健康数据读写与后台运动记录
 */
class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    // 追踪状态
    private val _trackingState = MutableStateFlow<ExerciseTrackingState>(ExerciseTrackingState.Idle)
    val trackingState: StateFlow<ExerciseTrackingState> = _trackingState.asStateFlow()

    // 实时追踪数据
    private val _trackingData = MutableStateFlow(ExerciseTrackingData())
    val trackingData: StateFlow<ExerciseTrackingData> = _trackingData.asStateFlow()

    // 当前位置（用于地图定位）
    private val _currentLocation = MutableStateFlow<TrackPoint?>(null)
    val currentLocation: StateFlow<TrackPoint?> = _currentLocation.asStateFlow()

    // Phase 28: 保存运动记录状态
    private val _saveState = MutableStateFlow<SaveExerciseState>(SaveExerciseState.Idle)
    val saveState: StateFlow<SaveExerciseState> = _saveState.asStateFlow()

    // Phase 43: Activity Recognition 状态
    private val _detectedActivity = MutableStateFlow<ActivityRecognitionResult?>(null)
    val detectedActivity: StateFlow<ActivityRecognitionResult?> = _detectedActivity.asStateFlow()

    private val _activityRecognitionState = MutableStateFlow<ActivityRecognitionState>(ActivityRecognitionState.Idle)
    val activityRecognitionState: StateFlow<ActivityRecognitionState> = _activityRecognitionState.asStateFlow()

    // Phase 44: Health Connect 状态
    private val _healthConnectAvailability = MutableStateFlow(HealthConnectAvailabilityStatus.NOT_SUPPORTED)
    val healthConnectAvailability: StateFlow<HealthConnectAvailabilityStatus> = _healthConnectAvailability.asStateFlow()

    private val _healthConnectSyncState = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Idle)
    val healthConnectSyncState: StateFlow<HealthConnectSyncState> = _healthConnectSyncState.asStateFlow()

    private val _healthConnectData = MutableStateFlow(HealthConnectData())
    val healthConnectData: StateFlow<HealthConnectData> = _healthConnectData.asStateFlow()

    private val _healthConnectPermissionsGranted = MutableStateFlow(false)
    val healthConnectPermissionsGranted: StateFlow<Boolean> = _healthConnectPermissionsGranted.asStateFlow()

    // 内部状态
    private val trackPoints = mutableListOf<TrackPoint>()
    private var startTime = 0L
    private var pausedDuration = 0L
    private var pauseStartTime = 0L
    private var timerJob: Job? = null
    private var locationService: LocationTrackingService? = null
    private var activityRecognitionService: ActivityRecognitionService? = null
    private var healthConnectService: HealthConnectService? = null
    private var activityCollectJob: Job? = null
    private var exerciseType: String = "walking"
    private var planId: Int? = null

    /**
     * 初始化位置服务
     */
    private fun ensureLocationService() {
        if (locationService == null) {
            locationService = LocationTrackingService(
                context = getApplication(),
                onLocationUpdate = { point -> onNewLocation(point) },
                onError = { /* 静默处理，UI层通过状态判断 */ }
            )
        }
    }

    /**
     * 开始运动追踪
     * @param type 运动类型（walking/running/cycling等）
     * @param associatedPlanId 关联的运动计划ID（可选）
     */
    fun startTracking(type: String = "walking", associatedPlanId: Int? = null) {
        if (_trackingState.value is ExerciseTrackingState.Tracking) return

        ensureLocationService()
        exerciseType = type
        planId = associatedPlanId
        trackPoints.clear()
        startTime = System.currentTimeMillis()
        pausedDuration = 0L

        _trackingData.value = ExerciseTrackingData(
            exerciseType = exerciseType,
            planId = planId
        )

        locationService?.startTracking()
        startTimer()
        _trackingState.value = ExerciseTrackingState.Tracking
    }

    /**
     * 暂停追踪
     */
    fun pauseTracking() {
        if (_trackingState.value !is ExerciseTrackingState.Tracking) return

        pauseStartTime = System.currentTimeMillis()
        locationService?.stopTracking()
        timerJob?.cancel()
        _trackingState.value = ExerciseTrackingState.Paused
    }

    /**
     * 恢复追踪
     */
    fun resumeTracking() {
        if (_trackingState.value !is ExerciseTrackingState.Paused) return

        pausedDuration += System.currentTimeMillis() - pauseStartTime
        ensureLocationService()
        locationService?.startTracking()
        startTimer()
        _trackingState.value = ExerciseTrackingState.Tracking
    }

    /**
     * 停止追踪并生成结果
     */
    fun stopTracking() {
        val currentState = _trackingState.value
        if (currentState !is ExerciseTrackingState.Tracking &&
            currentState !is ExerciseTrackingState.Paused
        ) return

        locationService?.stopTracking()
        timerJob?.cancel()

        val totalDuration = calculateElapsedTime()
        val totalDistance = ExerciseTrackingUtils.calculateTotalDistance(trackPoints.toList())
        val avgPace = ExerciseTrackingUtils.calculatePace(totalDistance, totalDuration)

        _trackingState.value = ExerciseTrackingState.Completed(
            totalDistance = totalDistance,
            totalDuration = totalDuration,
            averagePace = avgPace,
            trackPoints = trackPoints.toList()
        )
    }

    /**
     * 重置状态（返回空闲）
     */
    fun resetTracking() {
        locationService?.stopTracking()
        timerJob?.cancel()
        trackPoints.clear()
        startTime = 0L
        pausedDuration = 0L
        _trackingState.value = ExerciseTrackingState.Idle
        _trackingData.value = ExerciseTrackingData()
        _currentLocation.value = null
    }

    /**
     * 获取初始位置用于地图定位
     */
    fun fetchInitialLocation() {
        ensureLocationService()
        locationService?.getLastKnownLocation { point ->
            point?.let { _currentLocation.value = it }
        }
    }

    /**
     * 处理新的位置更新
     */
    private fun onNewLocation(point: TrackPoint) {
        _currentLocation.value = point
        trackPoints.add(point)
        updateTrackingData()
    }

    /**
     * 更新实时追踪数据
     */
    private fun updateTrackingData() {
        val points = trackPoints.toList()
        val totalDistance = ExerciseTrackingUtils.calculateTotalDistance(points)
        val elapsed = calculateElapsedTime()
        val avgPace = ExerciseTrackingUtils.calculatePace(totalDistance, elapsed)
        val avgSpeed = ExerciseTrackingUtils.calculateSpeed(totalDistance, elapsed)

        // 计算当前配速（使用最近2个点）
        val currentPace = if (points.size >= 2) {
            val last = points.last()
            val prev = points[points.size - 2]
            val segmentDist = ExerciseTrackingUtils.calculateDistance(
                prev.latitude, prev.longitude,
                last.latitude, last.longitude
            )
            val segmentTime = last.timestamp - prev.timestamp
            ExerciseTrackingUtils.calculatePace(segmentDist, segmentTime)
        } else 0.0

        // 估算热量
        val durationMinutes = elapsed / 60000.0
        val calories = ExerciseTrackingUtils.estimateCalories(exerciseType, durationMinutes)

        _trackingData.value = ExerciseTrackingData(
            trackPoints = points,
            totalDistance = totalDistance,
            elapsedTime = elapsed,
            currentPace = currentPace,
            averagePace = avgPace,
            currentSpeed = avgSpeed,
            caloriesBurned = calories,
            planId = planId,
            exerciseType = exerciseType
        )
    }

    /**
     * 启动计时器（每秒更新时间）
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = calculateElapsedTime()
                val currentData = _trackingData.value
                val durationMinutes = elapsed / 60000.0
                val calories = ExerciseTrackingUtils.estimateCalories(exerciseType, durationMinutes)
                _trackingData.value = currentData.copy(
                    elapsedTime = elapsed,
                    caloriesBurned = calories
                )
            }
        }
    }

    /**
     * 计算有效运动时间（排除暂停时间）
     */
    private fun calculateElapsedTime(): Long {
        if (startTime == 0L) return 0L
        val now = System.currentTimeMillis()
        val currentPauseDuration = if (_trackingState.value is ExerciseTrackingState.Paused) {
            now - pauseStartTime
        } else 0L
        return now - startTime - pausedDuration - currentPauseDuration
    }

    // ==================== Phase 28: 运动记录保存 ====================

    /**
     * 保存运动记录到后端
     * @param userId 用户ID
     * @param actualCalories 实际消耗热量（kcal）
     * @param actualDuration 实际运动时长（分钟）
     * @param distance 运动距离（米）
     * @param exerciseDate 运动日期（YYYY-MM-DD）
     * @param startedAt 开始时间（ISO格式，可选）
     * @param endedAt 结束时间（ISO格式，可选）
     * @param plannedCalories 计划热量（可选）
     * @param plannedDuration 计划时长（可选）
     * @param notes 运动备注（可选）
     */
    fun saveExerciseRecord(
        userId: Int,
        actualCalories: Double,
        actualDuration: Int,
        distance: Double?,
        exerciseDate: String,
        startedAt: String? = null,
        endedAt: String? = null,
        plannedCalories: Double? = null,
        plannedDuration: Int? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = SaveExerciseState.Saving
            try {
                val request = CreateExerciseRecordRequest(
                    userId = userId,
                    planId = planId,
                    exerciseType = exerciseType,
                    actualCalories = actualCalories,
                    actualDuration = actualDuration,
                    distance = distance,
                    exerciseDate = exerciseDate,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    plannedCalories = plannedCalories,
                    plannedDuration = plannedDuration,
                    notes = notes
                )
                val response = RetrofitClient.apiService.createExerciseRecord(request)
                if (response.code == 200 && response.data != null) {
                    _saveState.value = SaveExerciseState.Success(response.data.id)
                } else {
                    _saveState.value = SaveExerciseState.Error(response.message)
                }
            } catch (e: Exception) {
                _saveState.value = SaveExerciseState.Error(
                    e.message ?: "保存运动记录失败"
                )
            }
        }
    }

    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveExerciseState.Idle
    }

    // ==================== Phase 43: Activity Recognition ====================

    /**
     * 启动活动识别 - Phase 43
     *
     * 开始监听用户的物理活动状态（静止/步行/跑步/骑行）。
     * 识别结果通过 detectedActivity StateFlow 暴露给UI层。
     * 若正在追踪运动，会自动更新运动类型。
     */
    fun startActivityRecognition() {
        if (activityRecognitionService == null) {
            activityRecognitionService = ActivityRecognitionService(getApplication())
        }
        activityRecognitionService?.startRecognition()

        // 收集识别结果
        activityCollectJob?.cancel()
        activityCollectJob = viewModelScope.launch {
            activityRecognitionService?.currentActivity?.collect { result ->
                _detectedActivity.value = result
                // 若正在追踪且检测到有效运动类型，自动更新exerciseType
                if (_trackingState.value is ExerciseTrackingState.Tracking && result != null) {
                    val newType = ActivityRecognitionUtils.mapToExerciseType(result.activityType)
                    if (newType != "still" && newType != "unknown" && newType != "in_vehicle") {
                        exerciseType = newType
                    }
                }
            }
        }

        // 收集状态
        viewModelScope.launch {
            activityRecognitionService?.state?.collect { state ->
                _activityRecognitionState.value = state
            }
        }
    }

    /**
     * 停止活动识别 - Phase 43
     */
    fun stopActivityRecognition() {
        activityCollectJob?.cancel()
        activityCollectJob = null
        activityRecognitionService?.stopRecognition()
    }

    /**
     * 基于活动识别结果自动开始追踪 - Phase 43
     *
     * 当检测到用户从静止转为步行/跑步/骑行，且置信度足够时，
     * 自动开始运动追踪。仅在Idle状态下触发。
     */
    fun autoStartTrackingIfNeeded() {
        val currentResult = _detectedActivity.value ?: return
        if (ActivityRecognitionUtils.shouldAutoStartTracking(currentResult) &&
            _trackingState.value is ExerciseTrackingState.Idle
        ) {
            val type = ActivityRecognitionUtils.mapToExerciseType(currentResult.activityType)
            startTracking(type = type)
        }
    }

    /**
     * 获取当前检测到的运动类型名称 - Phase 43
     * 用于UI显示
     */
    fun getDetectedExerciseTypeLabel(): String {
        val result = _detectedActivity.value ?: return "未检测"
        return result.activityType.label
    }

    // ==================== Phase 44: Health Connect ====================

    /**
     * 初始化Health Connect服务 - Phase 44
     *
     * 检查Health Connect可用性并创建服务实例。
     * 应在ViewModel创建后尽早调用。
     */
    fun initHealthConnect() {
        if (healthConnectService == null) {
            healthConnectService = HealthConnectService(getApplication())
        }
        val status = healthConnectService!!.checkAvailability()
        _healthConnectAvailability.value = status

        // 收集服务状态
        viewModelScope.launch {
            healthConnectService?.syncState?.collect { state ->
                _healthConnectSyncState.value = state
            }
        }
        viewModelScope.launch {
            healthConnectService?.healthData?.collect { data ->
                _healthConnectData.value = data
            }
        }
        viewModelScope.launch {
            healthConnectService?.permissionsGranted?.collect { granted ->
                _healthConnectPermissionsGranted.value = granted
            }
        }
    }

    /**
     * 检查Health Connect权限是否已授予 - Phase 44
     */
    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            healthConnectService?.checkPermissions()
        }
    }

    /**
     * 从Health Connect读取今日健康数据 - Phase 44
     *
     * 读取步数、心率、卡路里、运动会话等数据。
     * 结果通过 healthConnectData StateFlow 暴露给UI层。
     */
    fun syncHealthConnectData() {
        viewModelScope.launch {
            healthConnectService?.readTodayData()
        }
    }

    /**
     * 将运动追踪结果写入Health Connect - Phase 44
     *
     * 在运动追踪完成并保存到后端后调用，
     * 将运动数据同步写入Health Connect，实现后台静默记录运动量。
     *
     * @param startTimeMillis 运动开始时间（毫秒）
     * @param endTimeMillis 运动结束时间（毫秒）
     * @param calories 消耗卡路里
     * @param steps 步数（可选）
     */
    fun syncExerciseToHealthConnect(
        startTimeMillis: Long,
        endTimeMillis: Long,
        calories: Double,
        steps: Long? = null
    ) {
        if (_healthConnectAvailability.value != HealthConnectAvailabilityStatus.AVAILABLE) {
            return
        }
        viewModelScope.launch {
            val success = healthConnectService?.writeExerciseSession(
                exerciseType = exerciseType,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                title = "LifeHub运动记录",
                totalCalories = calories,
                totalSteps = steps
            ) ?: false

            if (success) {
                // 写入成功后刷新今日数据
                healthConnectService?.readTodayData()
            }
        }
    }

    /**
     * 获取Health Connect所需权限集合 - Phase 44
     * 供Activity层请求权限时使用
     */
    fun getHealthConnectPermissions(): Set<String> {
        return HealthConnectService.REQUIRED_PERMISSIONS
    }

    /**
     * 获取Health Connect服务实例 - Phase 44
     * 供需要直接操作服务的场景使用
     */
    fun getHealthConnectService(): HealthConnectService? {
        return healthConnectService
    }

    override fun onCleared() {
        super.onCleared()
        locationService?.stopTracking()
        activityRecognitionService?.stopRecognition()
        activityCollectJob?.cancel()
        timerJob?.cancel()
    }
}
