package com.example.lifehub

import com.example.lifehub.data.HealthConnectAvailabilityStatus
import com.example.lifehub.data.HealthConnectData
import com.example.lifehub.data.HealthConnectPermissionType
import com.example.lifehub.data.HealthConnectSyncState
import com.example.lifehub.data.HealthConnectUtils
import com.example.lifehub.data.HealthDataRecord
import com.example.lifehub.data.HealthDataType
import com.example.lifehub.data.StepsRecord
import com.example.lifehub.data.HeartRateRecord
import com.example.lifehub.data.CaloriesRecord
import com.example.lifehub.data.ExerciseSessionRecord
import com.example.lifehub.data.HealthConnectDailySummary
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 44: Health Connect 集成单元测试
 *
 * 测试覆盖：
 * 1. 数据模型正确性
 * 2. 工具类方法
 * 3. 状态枚举覆盖
 * 4. 边界条件处理
 * 5. 数据转换与格式化
 */
class Phase44HealthConnectTest {

    // ==================== 1. 数据模型测试 ====================

    @Test
    fun `StepsRecord creation with valid data`() {
        val record = StepsRecord(
            startTime = 1000L,
            endTime = 2000L,
            count = 5000
        )
        assertEquals(5000, record.count)
        assertEquals(1000L, record.startTime)
        assertEquals(2000L, record.endTime)
    }

    @Test
    fun `StepsRecord with zero steps`() {
        val record = StepsRecord(
            startTime = 0L,
            endTime = 1000L,
            count = 0
        )
        assertEquals(0, record.count)
    }

    @Test
    fun `HeartRateRecord creation with valid data`() {
        val record = HeartRateRecord(
            time = System.currentTimeMillis(),
            beatsPerMinute = 72
        )
        assertEquals(72, record.beatsPerMinute)
    }

    @Test
    fun `HeartRateRecord with boundary values`() {
        // 极低心率
        val low = HeartRateRecord(time = 0L, beatsPerMinute = 30)
        assertEquals(30, low.beatsPerMinute)

        // 极高心率
        val high = HeartRateRecord(time = 0L, beatsPerMinute = 220)
        assertEquals(220, high.beatsPerMinute)
    }

    @Test
    fun `CaloriesRecord creation with valid data`() {
        val record = CaloriesRecord(
            startTime = 1000L,
            endTime = 2000L,
            totalCalories = 350.5
        )
        assertEquals(350.5, record.totalCalories, 0.01)
    }

    @Test
    fun `CaloriesRecord with zero calories`() {
        val record = CaloriesRecord(
            startTime = 0L,
            endTime = 1000L,
            totalCalories = 0.0
        )
        assertEquals(0.0, record.totalCalories, 0.01)
    }

    @Test
    fun `ExerciseSessionRecord creation`() {
        val record = ExerciseSessionRecord(
            startTime = 1000L,
            endTime = 2000L,
            exerciseType = "walking",
            title = "晨跑",
            calories = 200.0,
            distance = 3000.0,
            steps = 4000
        )
        assertEquals("walking", record.exerciseType)
        assertEquals("晨跑", record.title)
        assertEquals(200.0, record.calories!!, 0.01)
        assertEquals(3000.0, record.distance!!, 0.01)
        assertEquals(4000, record.steps)
    }

    @Test
    fun `ExerciseSessionRecord with optional fields null`() {
        val record = ExerciseSessionRecord(
            startTime = 1000L,
            endTime = 2000L,
            exerciseType = "running"
        )
        assertNull(record.title)
        assertNull(record.calories)
        assertNull(record.distance)
        assertNull(record.steps)
    }

    @Test
    fun `ExerciseSessionRecord duration calculation`() {
        val record = ExerciseSessionRecord(
            startTime = 0L,
            endTime = 3600000L, // 1小时
            exerciseType = "cycling"
        )
        assertEquals(3600000L, record.endTime - record.startTime)
    }

    // ==================== 2. HealthConnectData 聚合数据模型测试 ====================

    @Test
    fun `HealthConnectData default values`() {
        val data = HealthConnectData()
        assertEquals(0L, data.todaySteps)
        assertEquals(0.0, data.todayCalories, 0.01)
        assertNull(data.latestHeartRate)
        assertTrue(data.exerciseSessions.isEmpty())
        assertTrue(data.stepsRecords.isEmpty())
        assertTrue(data.heartRateRecords.isEmpty())
        assertTrue(data.caloriesRecords.isEmpty())
    }

    @Test
    fun `HealthConnectData with populated fields`() {
        val sessions = listOf(
            ExerciseSessionRecord(0L, 1000L, "walking"),
            ExerciseSessionRecord(2000L, 3000L, "running")
        )
        val data = HealthConnectData(
            todaySteps = 8000,
            todayCalories = 450.0,
            latestHeartRate = 75,
            exerciseSessions = sessions,
            stepsRecords = listOf(StepsRecord(0, 1000, 8000)),
            heartRateRecords = listOf(HeartRateRecord(500, 75)),
            caloriesRecords = listOf(CaloriesRecord(0, 1000, 450.0))
        )
        assertEquals(8000L, data.todaySteps)
        assertEquals(450.0, data.todayCalories, 0.01)
        assertEquals(75, data.latestHeartRate)
        assertEquals(2, data.exerciseSessions.size)
    }

    // ==================== 3. 状态枚举测试 ====================

    @Test
    fun `HealthConnectAvailabilityStatus enum values`() {
        val values = HealthConnectAvailabilityStatus.values()
        assertTrue(values.contains(HealthConnectAvailabilityStatus.AVAILABLE))
        assertTrue(values.contains(HealthConnectAvailabilityStatus.NOT_INSTALLED))
        assertTrue(values.contains(HealthConnectAvailabilityStatus.NOT_SUPPORTED))
        assertEquals(3, values.size)
    }

    @Test
    fun `HealthConnectSyncState sealed class variants`() {
        val idle = HealthConnectSyncState.Idle
        val syncing = HealthConnectSyncState.Syncing
        val success = HealthConnectSyncState.Success("同步成功")
        val error = HealthConnectSyncState.Error("同步失败")

        assertTrue(idle is HealthConnectSyncState)
        assertTrue(syncing is HealthConnectSyncState)
        assertTrue(success is HealthConnectSyncState)
        assertEquals("同步成功", success.message)
        assertEquals("同步失败", error.message)
    }

    @Test
    fun `HealthConnectPermissionType enum completeness`() {
        val values = HealthConnectPermissionType.values()
        assertTrue(values.any { it.name == "READ_STEPS" })
        assertTrue(values.any { it.name == "WRITE_STEPS" })
        assertTrue(values.any { it.name == "READ_HEART_RATE" })
        assertTrue(values.any { it.name == "WRITE_HEART_RATE" })
        assertTrue(values.any { it.name == "READ_CALORIES" })
        assertTrue(values.any { it.name == "WRITE_CALORIES" })
        assertTrue(values.any { it.name == "READ_EXERCISE" })
        assertTrue(values.any { it.name == "WRITE_EXERCISE" })
    }

    @Test
    fun `HealthDataType enum completeness`() {
        val values = HealthDataType.values()
        assertTrue(values.any { it.name == "STEPS" })
        assertTrue(values.any { it.name == "HEART_RATE" })
        assertTrue(values.any { it.name == "CALORIES" })
        assertTrue(values.any { it.name == "EXERCISE_SESSION" })
        assertEquals(4, values.size)
    }

    // ==================== 4. HealthConnectUtils 工具类测试 ====================

    @Test
    fun `formatSteps formats large numbers with comma`() {
        assertEquals("0", HealthConnectUtils.formatSteps(0))
        assertEquals("999", HealthConnectUtils.formatSteps(999))
        assertEquals("1,000", HealthConnectUtils.formatSteps(1000))
        assertEquals("10,000", HealthConnectUtils.formatSteps(10000))
        assertEquals("1,234,567", HealthConnectUtils.formatSteps(1234567))
    }

    @Test
    fun `formatCalories formats with one decimal place`() {
        assertEquals("0.0", HealthConnectUtils.formatCalories(0.0))
        assertEquals("100.5", HealthConnectUtils.formatCalories(100.5))
        assertEquals("1234.0", HealthConnectUtils.formatCalories(1234.0))
    }

    @Test
    fun `formatHeartRate formats correctly`() {
        assertEquals("72 bpm", HealthConnectUtils.formatHeartRate(72))
        assertEquals("0 bpm", HealthConnectUtils.formatHeartRate(0))
        assertEquals("200 bpm", HealthConnectUtils.formatHeartRate(200))
    }

    @Test
    fun `formatDuration formats correctly`() {
        // 0毫秒
        assertEquals("0分钟", HealthConnectUtils.formatDuration(0L))
        // 30秒（小于1分钟）
        assertEquals("0分钟", HealthConnectUtils.formatDuration(30_000L))
        // 1分钟
        assertEquals("1分钟", HealthConnectUtils.formatDuration(60_000L))
        // 90分钟（1.5小时）
        assertEquals("1小时30分钟", HealthConnectUtils.formatDuration(90 * 60_000L))
        // 2小时整
        assertEquals("2小时0分钟", HealthConnectUtils.formatDuration(120 * 60_000L))
    }

    @Test
    fun `mapExerciseTypeToLabel maps known types`() {
        assertEquals("步行", HealthConnectUtils.mapExerciseTypeToLabel("walking"))
        assertEquals("跑步", HealthConnectUtils.mapExerciseTypeToLabel("running"))
        assertEquals("骑行", HealthConnectUtils.mapExerciseTypeToLabel("cycling"))
        assertEquals("徒步", HealthConnectUtils.mapExerciseTypeToLabel("hiking"))
        assertEquals("游泳", HealthConnectUtils.mapExerciseTypeToLabel("swimming"))
    }

    @Test
    fun `mapExerciseTypeToLabel returns original for unknown types`() {
        // yoga已被映射为"瑜伽"
        assertEquals("瑜伽", HealthConnectUtils.mapExerciseTypeToLabel("yoga"))
        assertEquals("unknown", HealthConnectUtils.mapExerciseTypeToLabel("unknown"))
        assertEquals("", HealthConnectUtils.mapExerciseTypeToLabel(""))
    }

    @Test
    fun `isValidHeartRate validates correctly`() {
        assertFalse(HealthConnectUtils.isValidHeartRate(0))
        assertFalse(HealthConnectUtils.isValidHeartRate(-1))
        assertFalse(HealthConnectUtils.isValidHeartRate(19))
        assertTrue(HealthConnectUtils.isValidHeartRate(20))
        assertTrue(HealthConnectUtils.isValidHeartRate(72))
        assertTrue(HealthConnectUtils.isValidHeartRate(250))
        assertFalse(HealthConnectUtils.isValidHeartRate(251))
        assertFalse(HealthConnectUtils.isValidHeartRate(300))
    }

    @Test
    fun `isValidStepCount validates correctly`() {
        assertTrue(HealthConnectUtils.isValidStepCount(0))
        assertTrue(HealthConnectUtils.isValidStepCount(1))
        assertTrue(HealthConnectUtils.isValidStepCount(100000))
        assertFalse(HealthConnectUtils.isValidStepCount(-1))
        assertFalse(HealthConnectUtils.isValidStepCount(100001))
    }

    @Test
    fun `calculateStepGoalProgress calculates correctly`() {
        assertEquals(0.0, HealthConnectUtils.calculateStepGoalProgress(0, 10000), 0.01)
        assertEquals(0.5, HealthConnectUtils.calculateStepGoalProgress(5000, 10000), 0.01)
        assertEquals(1.0, HealthConnectUtils.calculateStepGoalProgress(10000, 10000), 0.01)
        // 超过目标不应超过1.0
        assertEquals(1.0, HealthConnectUtils.calculateStepGoalProgress(15000, 10000), 0.01)
    }

    @Test
    fun `calculateStepGoalProgress with zero goal`() {
        assertEquals(0.0, HealthConnectUtils.calculateStepGoalProgress(5000, 0), 0.01)
    }

    @Test
    fun `calculateCalorieGoalProgress calculates correctly`() {
        assertEquals(0.0, HealthConnectUtils.calculateCalorieGoalProgress(0.0, 500.0), 0.01)
        assertEquals(0.5, HealthConnectUtils.calculateCalorieGoalProgress(250.0, 500.0), 0.01)
        assertEquals(1.0, HealthConnectUtils.calculateCalorieGoalProgress(500.0, 500.0), 0.01)
        assertEquals(1.0, HealthConnectUtils.calculateCalorieGoalProgress(600.0, 500.0), 0.01)
    }

    @Test
    fun `calculateCalorieGoalProgress with zero goal`() {
        assertEquals(0.0, HealthConnectUtils.calculateCalorieGoalProgress(100.0, 0.0), 0.01)
    }

    @Test
    fun `estimateDistanceFromSteps estimates correctly`() {
        // 默认步长0.75m
        assertEquals(0.0, HealthConnectUtils.estimateDistanceFromSteps(0), 0.01)
        assertEquals(750.0, HealthConnectUtils.estimateDistanceFromSteps(1000), 0.01)
        assertEquals(7500.0, HealthConnectUtils.estimateDistanceFromSteps(10000), 0.01)
    }

    @Test
    fun `estimateDistanceFromSteps with custom stride`() {
        assertEquals(800.0, HealthConnectUtils.estimateDistanceFromSteps(1000, 0.8), 0.01)
        assertEquals(600.0, HealthConnectUtils.estimateDistanceFromSteps(1000, 0.6), 0.01)
    }

    @Test
    fun `estimateCaloriesFromSteps estimates correctly`() {
        // 默认70kg体重
        val cal = HealthConnectUtils.estimateCaloriesFromSteps(10000)
        assertTrue(cal > 0.0)
        // 0步 → 0卡
        assertEquals(0.0, HealthConnectUtils.estimateCaloriesFromSteps(0), 0.01)
    }

    @Test
    fun `estimateCaloriesFromSteps with custom weight`() {
        val cal70 = HealthConnectUtils.estimateCaloriesFromSteps(10000, 70.0)
        val cal90 = HealthConnectUtils.estimateCaloriesFromSteps(10000, 90.0)
        assertTrue(cal90 > cal70)
    }

    // ==================== 5. HealthConnectDailySummary 测试 ====================

    @Test
    fun `HealthConnectDailySummary creation and defaults`() {
        val summary = HealthConnectDailySummary(
            date = "2026-02-07",
            totalSteps = 8500,
            totalCalories = 320.5,
            averageHeartRate = 72,
            exerciseMinutes = 45,
            exerciseCount = 2
        )
        assertEquals("2026-02-07", summary.date)
        assertEquals(8500L, summary.totalSteps)
        assertEquals(320.5, summary.totalCalories, 0.01)
        assertEquals(72, summary.averageHeartRate)
        assertEquals(45, summary.exerciseMinutes)
        assertEquals(2, summary.exerciseCount)
    }

    @Test
    fun `HealthConnectDailySummary with zero values`() {
        val summary = HealthConnectDailySummary(
            date = "2026-01-01"
        )
        assertEquals(0L, summary.totalSteps)
        assertEquals(0.0, summary.totalCalories, 0.01)
        assertNull(summary.averageHeartRate)
        assertEquals(0, summary.exerciseMinutes)
        assertEquals(0, summary.exerciseCount)
    }

    // ==================== 6. HealthDataRecord 多态数据记录测试 ====================

    @Test
    fun `HealthDataRecord Steps variant`() {
        val record = HealthDataRecord.Steps(
            startTime = 1000L,
            endTime = 2000L,
            count = 500
        )
        assertEquals(500, record.count)
        assertTrue(record is HealthDataRecord)
    }

    @Test
    fun `HealthDataRecord HeartRate variant`() {
        val record = HealthDataRecord.HeartRate(
            time = 1000L,
            bpm = 80
        )
        assertEquals(80, record.bpm)
        assertTrue(record is HealthDataRecord)
    }

    @Test
    fun `HealthDataRecord Calories variant`() {
        val record = HealthDataRecord.Calories(
            startTime = 1000L,
            endTime = 2000L,
            kcal = 150.0
        )
        assertEquals(150.0, record.kcal, 0.01)
        assertTrue(record is HealthDataRecord)
    }

    @Test
    fun `HealthDataRecord Exercise variant`() {
        val record = HealthDataRecord.Exercise(
            startTime = 0L,
            endTime = 3600000L,
            type = "running",
            title = "早晨跑步",
            calories = 300.0,
            distance = 5000.0
        )
        assertEquals("running", record.type)
        assertEquals("早晨跑步", record.title)
        assertEquals(300.0, record.calories!!, 0.01)
        assertEquals(5000.0, record.distance!!, 0.01)
    }

    // ==================== 7. 边界条件与异常场景测试 ====================

    @Test
    fun `formatSteps with negative value`() {
        // 负数步数应正常格式化（显示负号）
        val result = HealthConnectUtils.formatSteps(-1)
        assertNotNull(result)
    }

    @Test
    fun `formatCalories with negative value`() {
        val result = HealthConnectUtils.formatCalories(-10.0)
        assertNotNull(result)
    }

    @Test
    fun `formatDuration with negative duration`() {
        val result = HealthConnectUtils.formatDuration(-1000L)
        assertEquals("0分钟", result)
    }

    @Test
    fun `calculateStepGoalProgress with negative steps`() {
        assertEquals(0.0, HealthConnectUtils.calculateStepGoalProgress(-100, 10000), 0.01)
    }

    @Test
    fun `estimateDistanceFromSteps with negative steps returns zero`() {
        assertEquals(0.0, HealthConnectUtils.estimateDistanceFromSteps(-100), 0.01)
    }

    @Test
    fun `ExerciseSessionRecord with same start and end time`() {
        val record = ExerciseSessionRecord(
            startTime = 1000L,
            endTime = 1000L,
            exerciseType = "walking"
        )
        assertEquals(0L, record.endTime - record.startTime)
    }

    // ==================== 8. 综合场景测试 ====================

    @Test
    fun `complete daily health scenario`() {
        // 模拟一天的健康数据
        val morningSteps = StepsRecord(
            startTime = 1000L,
            endTime = 2000L,
            count = 3000
        )
        val afternoonSteps = StepsRecord(
            startTime = 3000L,
            endTime = 4000L,
            count = 5000
        )
        val totalSteps = morningSteps.count + afternoonSteps.count
        assertEquals(8000, totalSteps)

        val exercise = ExerciseSessionRecord(
            startTime = 2000L,
            endTime = 3000L,
            exerciseType = "running",
            calories = 250.0,
            distance = 3000.0,
            steps = 4000
        )

        val heartRate = HeartRateRecord(
            time = 2500L,
            beatsPerMinute = 130
        )
        assertTrue(HealthConnectUtils.isValidHeartRate(heartRate.beatsPerMinute))

        val data = HealthConnectData(
            todaySteps = totalSteps.toLong(),
            todayCalories = 250.0,
            latestHeartRate = heartRate.beatsPerMinute,
            exerciseSessions = listOf(exercise),
            stepsRecords = listOf(morningSteps, afternoonSteps),
            heartRateRecords = listOf(heartRate),
            caloriesRecords = listOf(CaloriesRecord(2000, 3000, 250.0))
        )

        assertEquals(8000L, data.todaySteps)
        val progress = HealthConnectUtils.calculateStepGoalProgress(data.todaySteps, 10000)
        assertEquals(0.8, progress, 0.01)
    }

    @Test
    fun `exercise type label mapping completeness`() {
        // 确保所有已知运动类型都有中文映射
        val knownTypes = listOf("walking", "running", "cycling", "hiking", "swimming")
        knownTypes.forEach { type ->
            val label = HealthConnectUtils.mapExerciseTypeToLabel(type)
            assertNotEquals("未知类型应该有映射", type, label)
        }
    }

    @Test
    fun `step goal progress is clamped between 0 and 1`() {
        // 多种边界值
        val testCases = listOf(
            Triple(-100L, 10000L, 0.0),
            Triple(0L, 10000L, 0.0),
            Triple(5000L, 10000L, 0.5),
            Triple(10000L, 10000L, 1.0),
            Triple(20000L, 10000L, 1.0)
        )
        testCases.forEach { (steps, goal, expected) ->
            val progress = HealthConnectUtils.calculateStepGoalProgress(steps, goal)
            assertEquals(
                "steps=$steps, goal=$goal should give progress=$expected",
                expected,
                progress,
                0.01
            )
        }
    }
}
