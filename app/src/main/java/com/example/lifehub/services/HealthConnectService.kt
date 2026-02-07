package com.example.lifehub.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord as HCExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord as HCHeartRateRecord
import androidx.health.connect.client.records.StepsRecord as HCStepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.lifehub.data.CaloriesRecord
import com.example.lifehub.data.ExerciseSessionRecord
import com.example.lifehub.data.HealthConnectAvailabilityStatus
import com.example.lifehub.data.HealthConnectData
import com.example.lifehub.data.HealthConnectDailySummary
import com.example.lifehub.data.HealthConnectSyncState
import com.example.lifehub.data.HealthDataRecord
import com.example.lifehub.data.HeartRateRecord
import com.example.lifehub.data.StepsRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Health Connect 服务 - Phase 44
 *
 * 封装 Google Health Connect SDK，提供健康数据读写能力：
 * - 读取步数、心率、卡路里、运动会话
 * - 写入运动记录（运动追踪完成后同步到Health Connect）
 * - 后台静默记录运动量
 *
 * 使用方式：
 * 1. 调用 checkAvailability() 检查Health Connect是否可用
 * 2. 请求权限（通过Activity的registerForActivityResult）
 * 3. 调用 readTodayData() 读取今日健康数据
 * 4. 运动结束后调用 writeExerciseSession() 同步数据
 *
 * @param context Application Context
 */
class HealthConnectService(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectService"

        /** 所需的Health Connect权限集合 */
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HCStepsRecord::class),
            HealthPermission.getWritePermission(HCStepsRecord::class),
            HealthPermission.getReadPermission(HCHeartRateRecord::class),
            HealthPermission.getWritePermission(HCHeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HCExerciseSessionRecord::class),
            HealthPermission.getWritePermission(HCExerciseSessionRecord::class)
        )
    }

    /** Health Connect客户端（延迟初始化，仅在可用时创建） */
    private var healthConnectClient: HealthConnectClient? = null

    /** 可用性状态 */
    private val _availability = MutableStateFlow(HealthConnectAvailabilityStatus.NOT_SUPPORTED)
    val availability: StateFlow<HealthConnectAvailabilityStatus> = _availability.asStateFlow()

    /** 同步状态 */
    private val _syncState = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Idle)
    val syncState: StateFlow<HealthConnectSyncState> = _syncState.asStateFlow()

    /** 今日健康数据 */
    private val _healthData = MutableStateFlow(HealthConnectData())
    val healthData: StateFlow<HealthConnectData> = _healthData.asStateFlow()

    /** 权限是否已授予 */
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    /**
     * 检查Health Connect可用性
     *
     * @return 可用性状态
     */
    fun checkAvailability(): HealthConnectAvailabilityStatus {
        val status = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> {
                Log.w(TAG, "设备API级别 < 26，不支持Health Connect")
                HealthConnectAvailabilityStatus.NOT_SUPPORTED
            }
            else -> {
                try {
                    val sdkStatus = HealthConnectClient.getSdkStatus(context)
                    when (sdkStatus) {
                        HealthConnectClient.SDK_AVAILABLE -> {
                            healthConnectClient = HealthConnectClient.getOrCreate(context)
                            Log.i(TAG, "Health Connect 可用")
                            HealthConnectAvailabilityStatus.AVAILABLE
                        }
                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                            Log.w(TAG, "Health Connect 需要更新")
                            HealthConnectAvailabilityStatus.NOT_INSTALLED
                        }
                        else -> {
                            Log.w(TAG, "Health Connect 不可用，SDK状态: $sdkStatus")
                            HealthConnectAvailabilityStatus.NOT_INSTALLED
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "检查Health Connect可用性时出错", e)
                    HealthConnectAvailabilityStatus.NOT_INSTALLED
                }
            }
        }
        _availability.value = status
        return status
    }

    /**
     * 检查权限是否已授予
     *
     * @return 是否所有所需权限都已授予
     */
    suspend fun checkPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val allGranted = REQUIRED_PERMISSIONS.all { it in granted }
            _permissionsGranted.value = allGranted
            Log.d(TAG, "权限检查: ${if (allGranted) "全部已授予" else "部分未授予"}")
            allGranted
        } catch (e: Exception) {
            Log.e(TAG, "检查权限时出错", e)
            _permissionsGranted.value = false
            false
        }
    }

    /**
     * 读取今日健康数据（步数、心率、卡路里、运动会话）
     *
     * @return 今日健康数据聚合对象
     */
    suspend fun readTodayData(): HealthConnectData {
        val client = healthConnectClient
        if (client == null) {
            Log.w(TAG, "Health Connect客户端未初始化")
            _syncState.value = HealthConnectSyncState.Error("Health Connect不可用")
            return HealthConnectData()
        }

        _syncState.value = HealthConnectSyncState.Syncing
        return try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            val timeRange = TimeRangeFilter.between(startOfDay, now)

            // 并行读取各类数据
            val steps = readSteps(client, timeRange)
            val heartRates = readHeartRate(client, timeRange)
            val calories = readCalories(client, timeRange)
            val exercises = readExerciseSessions(client, timeRange)

            val totalSteps = steps.sumOf { it.count.toLong() }
            val totalCalories = calories.sumOf { it.totalCalories }
            val latestHr = heartRates.maxByOrNull { it.time }?.beatsPerMinute

            val data = HealthConnectData(
                todaySteps = totalSteps,
                todayCalories = totalCalories,
                latestHeartRate = latestHr,
                exerciseSessions = exercises,
                stepsRecords = steps,
                heartRateRecords = heartRates,
                caloriesRecords = calories
            )

            _healthData.value = data
            _syncState.value = HealthConnectSyncState.Success("同步成功")
            Log.i(TAG, "今日数据读取完成: 步数=$totalSteps, 卡路里=$totalCalories, 心率=$latestHr")
            data
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足，无法读取健康数据", e)
            _syncState.value = HealthConnectSyncState.Error("权限不足: ${e.message}")
            HealthConnectData()
        } catch (e: Exception) {
            Log.e(TAG, "读取今日数据失败", e)
            _syncState.value = HealthConnectSyncState.Error("读取失败: ${e.message}")
            HealthConnectData()
        }
    }

    /**
     * 读取指定时间范围内的步数记录
     */
    private suspend fun readSteps(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter
    ): List<StepsRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = HCStepsRecord::class,
                timeRangeFilter = timeRange
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                StepsRecord(
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    count = record.count.toInt()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取步数失败", e)
            emptyList()
        }
    }

    /**
     * 读取指定时间范围内的心率记录
     */
    private suspend fun readHeartRate(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter
    ): List<HeartRateRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = HCHeartRateRecord::class,
                timeRangeFilter = timeRange
            )
            val response = client.readRecords(request)
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateRecord(
                        time = sample.time.toEpochMilli(),
                        beatsPerMinute = sample.beatsPerMinute.toInt()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取心率失败", e)
            emptyList()
        }
    }

    /**
     * 读取指定时间范围内的卡路里消耗记录
     */
    private suspend fun readCalories(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter
    ): List<CaloriesRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = timeRange
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                CaloriesRecord(
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    totalCalories = record.energy.inKilocalories
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取卡路里失败", e)
            emptyList()
        }
    }

    /**
     * 读取指定时间范围内的运动会话记录
     */
    private suspend fun readExerciseSessions(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter
    ): List<ExerciseSessionRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = HCExerciseSessionRecord::class,
                timeRangeFilter = timeRange
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                ExerciseSessionRecord(
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    exerciseType = mapHCExerciseType(record.exerciseType),
                    title = record.title
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取运动会话失败", e)
            emptyList()
        }
    }

    // ==================== 写入操作 ====================

    /**
     * 写入运动会话到Health Connect
     *
     * 在运动追踪完成后调用，将运动数据同步到Health Connect。
     *
     * @param exerciseType 运动类型
     * @param startTimeMillis 开始时间（毫秒）
     * @param endTimeMillis 结束时间（毫秒）
     * @param title 运动标题（可选）
     * @param totalCalories 消耗卡路里（可选）
     * @param totalSteps 步数（可选）
     * @return 是否写入成功
     */
    suspend fun writeExerciseSession(
        exerciseType: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        title: String? = null,
        totalCalories: Double? = null,
        totalSteps: Long? = null
    ): Boolean {
        val client = healthConnectClient ?: run {
            Log.w(TAG, "Health Connect客户端未初始化，无法写入运动会话")
            return false
        }

        return try {
            val startInstant = Instant.ofEpochMilli(startTimeMillis)
            val endInstant = Instant.ofEpochMilli(endTimeMillis)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val exerciseSession = HCExerciseSessionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                exerciseType = mapToHCExerciseType(exerciseType),
                title = title
            )

            val recordsToInsert = mutableListOf<androidx.health.connect.client.records.Record>(exerciseSession)

            // 写入卡路里数据
            if (totalCalories != null && totalCalories > 0) {
                val caloriesRecord = TotalCaloriesBurnedRecord(
                    startTime = startInstant,
                    startZoneOffset = zoneOffset,
                    endTime = endInstant,
                    endZoneOffset = zoneOffset,
                    energy = androidx.health.connect.client.units.Energy.kilocalories(totalCalories)
                )
                recordsToInsert.add(caloriesRecord)
            }

            // 写入步数数据
            if (totalSteps != null && totalSteps > 0) {
                val stepsRecord = HCStepsRecord(
                    startTime = startInstant,
                    startZoneOffset = zoneOffset,
                    endTime = endInstant,
                    endZoneOffset = zoneOffset,
                    count = totalSteps
                )
                recordsToInsert.add(stepsRecord)
            }

            client.insertRecords(recordsToInsert)
            Log.i(TAG, "运动会话写入成功: type=$exerciseType, calories=$totalCalories, steps=$totalSteps")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足，无法写入运动会话", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "写入运动会话失败", e)
            false
        }
    }

    /**
     * 写入通用健康数据记录
     *
     * @param record 健康数据记录（步数/心率/卡路里/运动会话）
     * @return 是否写入成功
     */
    suspend fun writeRecord(record: HealthDataRecord): Boolean {
        val client = healthConnectClient ?: return false

        return try {
            when (record) {
                is HealthDataRecord.Steps -> {
                    val startInstant = Instant.ofEpochMilli(record.startTime)
                    val endInstant = Instant.ofEpochMilli(record.endTime)
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)
                    client.insertRecords(listOf(
                        HCStepsRecord(
                            startTime = startInstant,
                            startZoneOffset = zoneOffset,
                            endTime = endInstant,
                            endZoneOffset = zoneOffset,
                            count = record.count.toLong()
                        )
                    ))
                    true
                }
                is HealthDataRecord.HeartRate -> {
                    val instant = Instant.ofEpochMilli(record.time)
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(instant)
                    client.insertRecords(listOf(
                        HCHeartRateRecord(
                            startTime = instant,
                            startZoneOffset = zoneOffset,
                            endTime = instant.plusSeconds(1),
                            endZoneOffset = zoneOffset,
                            samples = listOf(
                                HCHeartRateRecord.Sample(
                                    time = instant,
                                    beatsPerMinute = record.bpm.toLong()
                                )
                            )
                        )
                    ))
                    true
                }
                is HealthDataRecord.Calories -> {
                    val startInstant = Instant.ofEpochMilli(record.startTime)
                    val endInstant = Instant.ofEpochMilli(record.endTime)
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)
                    client.insertRecords(listOf(
                        TotalCaloriesBurnedRecord(
                            startTime = startInstant,
                            startZoneOffset = zoneOffset,
                            endTime = endInstant,
                            endZoneOffset = zoneOffset,
                            energy = androidx.health.connect.client.units.Energy.kilocalories(record.kcal)
                        )
                    ))
                    true
                }
                is HealthDataRecord.Exercise -> {
                    writeExerciseSession(
                        exerciseType = record.type,
                        startTimeMillis = record.startTime,
                        endTimeMillis = record.endTime,
                        title = record.title,
                        totalCalories = record.calories
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入健康数据记录失败", e)
            false
        }
    }

    /**
     * 获取每日摘要数据
     *
     * @param date 目标日期
     * @return 每日摘要
     */
    suspend fun getDailySummary(date: LocalDate = LocalDate.now()): HealthConnectDailySummary {
        val client = healthConnectClient ?: return HealthConnectDailySummary(date = date.toString())

        return try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            val steps = readSteps(client, timeRange)
            val heartRates = readHeartRate(client, timeRange)
            val calories = readCalories(client, timeRange)
            val exercises = readExerciseSessions(client, timeRange)

            val totalSteps = steps.sumOf { it.count.toLong() }
            val totalCalories = calories.sumOf { it.totalCalories }
            val avgHr = if (heartRates.isNotEmpty()) {
                heartRates.map { it.beatsPerMinute }.average().toInt()
            } else null
            val exerciseMinutes = exercises.sumOf { record ->
                ((record.endTime - record.startTime) / 60_000).toInt()
            }

            HealthConnectDailySummary(
                date = date.toString(),
                totalSteps = totalSteps,
                totalCalories = totalCalories,
                averageHeartRate = avgHr,
                exerciseMinutes = exerciseMinutes,
                exerciseCount = exercises.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取每日摘要失败", e)
            HealthConnectDailySummary(date = date.toString())
        }
    }

    // ==================== 运动类型映射 ====================

    /**
     * 将Health Connect运动类型常量映射为本地字符串
     */
    private fun mapHCExerciseType(hcType: Int): String {
        return when (hcType) {
            HCExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
            HCExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
            HCExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "cycling"
            HCExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
            HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "swimming"
            HCExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
            HCExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "dancing"
            HCExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "badminton"
            HCExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "basketball"
            HCExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
            HCExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "football"
            HCExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "tennis"
            HCExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "table_tennis"
            else -> "other"
        }
    }

    /**
     * 将本地运动类型字符串映射为Health Connect运动类型常量
     */
    private fun mapToHCExerciseType(type: String): Int {
        return when (type.lowercase()) {
            "walking" -> HCExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "running" -> HCExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "cycling" -> HCExerciseSessionRecord.EXERCISE_TYPE_BIKING
            "hiking" -> HCExerciseSessionRecord.EXERCISE_TYPE_HIKING
            "swimming" -> HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "yoga" -> HCExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "dancing" -> HCExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "badminton" -> HCExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            "basketball" -> HCExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            "football" -> HCExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
            "tennis" -> HCExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            "table_tennis" -> HCExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            else -> HCExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }
}
