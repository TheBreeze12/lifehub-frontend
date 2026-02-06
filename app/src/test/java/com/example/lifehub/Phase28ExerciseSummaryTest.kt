package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 28: 运动结算展示 - 单元测试
 *
 * 测试内容：
 * 1. 数据模型正确性（CreateExerciseRecordRequest、ExerciseRecordResponse等）
 * 2. 达成率计算逻辑
 * 3. 时间/距离格式化
 * 4. 导航路由创建
 * 5. 保存状态管理
 * 6. 边界条件处理
 */
class Phase28ExerciseSummaryTest {

    // ==================== 1. 数据模型测试 ====================

    @Test
    fun `test CreateExerciseRecordRequest with all fields`() {
        val request = CreateExerciseRecordRequest(
            userId = 1,
            planId = 10,
            exerciseType = "running",
            actualCalories = 280.5,
            actualDuration = 35,
            distance = 4500.0,
            exerciseDate = "2026-02-06",
            startedAt = "2026-02-06T18:00:00",
            endedAt = "2026-02-06T18:35:00",
            plannedCalories = 300.0,
            plannedDuration = 30,
            notes = "沿河跑步"
        )

        assertEquals(1, request.userId)
        assertEquals(10, request.planId)
        assertEquals("running", request.exerciseType)
        assertEquals(280.5, request.actualCalories, 0.01)
        assertEquals(35, request.actualDuration)
        assertEquals(4500.0, request.distance!!, 0.01)
        assertEquals("2026-02-06", request.exerciseDate)
        assertEquals("2026-02-06T18:00:00", request.startedAt)
        assertEquals("2026-02-06T18:35:00", request.endedAt)
        assertEquals(300.0, request.plannedCalories!!, 0.01)
        assertEquals(30, request.plannedDuration)
        assertEquals("沿河跑步", request.notes)
    }

    @Test
    fun `test CreateExerciseRecordRequest with minimal fields`() {
        val request = CreateExerciseRecordRequest(
            userId = 1,
            exerciseType = "walking",
            actualCalories = 150.0,
            actualDuration = 30,
            exerciseDate = "2026-02-06"
        )

        assertEquals(1, request.userId)
        assertNull(request.planId)
        assertEquals("walking", request.exerciseType)
        assertEquals(150.0, request.actualCalories, 0.01)
        assertEquals(30, request.actualDuration)
        assertNull(request.distance)
        assertNull(request.startedAt)
        assertNull(request.endedAt)
        assertNull(request.notes)
        assertNull(request.plannedCalories)
        assertNull(request.plannedDuration)
    }

    @Test
    fun `test ExerciseRecordResponse with achievement rates`() {
        val data = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            planId = 10,
            exerciseType = "running",
            actualCalories = 280.0,
            actualDuration = 35,
            distance = 4500.0,
            exerciseDate = "2026-02-06",
            createdAt = "2026-02-06T18:40:00",
            plannedCalories = 300.0,
            plannedDuration = 30,
            caloriesAchievement = 93.3,
            durationAchievement = 116.7
        )

        assertEquals(1, data.id)
        assertEquals(93.3, data.caloriesAchievement!!, 0.01)
        assertEquals(116.7, data.durationAchievement!!, 0.01)
    }

    @Test
    fun `test ExerciseRecordResponse without achievement rates`() {
        val data = ExerciseRecordResponseData(
            id = 2,
            userId = 1,
            exerciseType = "walking",
            actualCalories = 150.0,
            actualDuration = 30,
            exerciseDate = "2026-02-06",
            createdAt = "2026-02-06T19:00:00"
        )

        assertNull(data.planId)
        assertNull(data.distance)
        assertNull(data.caloriesAchievement)
        assertNull(data.durationAchievement)
    }

    // ==================== 2. 达成率计算测试 ====================

    @Test
    fun `test achievement rate calculation - exact match`() {
        val actual = 300.0
        val planned = 300.0
        val rate = calculateAchievementRate(actual, planned)
        assertNotNull(rate)
        assertEquals(100.0, rate!!, 0.01)
    }

    @Test
    fun `test achievement rate calculation - exceeded`() {
        val actual = 350.0
        val planned = 300.0
        val rate = calculateAchievementRate(actual, planned)
        assertNotNull(rate)
        assertEquals(116.67, rate!!, 0.01)
    }

    @Test
    fun `test achievement rate calculation - under target`() {
        val actual = 200.0
        val planned = 300.0
        val rate = calculateAchievementRate(actual, planned)
        assertNotNull(rate)
        assertEquals(66.67, rate!!, 0.01)
    }

    @Test
    fun `test achievement rate calculation - zero planned`() {
        val rate = calculateAchievementRate(200.0, 0.0)
        assertNull(rate)
    }

    @Test
    fun `test achievement rate calculation - null planned`() {
        val rate = calculateAchievementRate(200.0, null)
        assertNull(rate)
    }

    @Test
    fun `test achievement rate calculation - zero actual`() {
        val rate = calculateAchievementRate(0.0, 300.0)
        assertEquals(0.0, rate!!, 0.01)
    }

    // ==================== 3. 时间/距离格式化测试 ====================

    @Test
    fun `test duration formatting - seconds only`() {
        val formatted = ExerciseTrackingUtils.formatDuration(45000) // 45 seconds
        assertEquals("00:45", formatted)
    }

    @Test
    fun `test duration formatting - minutes and seconds`() {
        val formatted = ExerciseTrackingUtils.formatDuration(1830000) // 30:30
        assertEquals("30:30", formatted)
    }

    @Test
    fun `test duration formatting - hours`() {
        val formatted = ExerciseTrackingUtils.formatDuration(3661000) // 1:01:01
        assertEquals("1:01:01", formatted)
    }

    @Test
    fun `test duration formatting - zero`() {
        val formatted = ExerciseTrackingUtils.formatDuration(0)
        assertEquals("00:00", formatted)
    }

    @Test
    fun `test distance formatting - meters`() {
        val formatted = ExerciseTrackingUtils.formatDistance(500.0)
        assertEquals("500 m", formatted)
    }

    @Test
    fun `test distance formatting - kilometers`() {
        val formatted = ExerciseTrackingUtils.formatDistance(4500.0)
        assertEquals("4.50 km", formatted)
    }

    @Test
    fun `test distance formatting - zero`() {
        val formatted = ExerciseTrackingUtils.formatDistance(0.0)
        assertEquals("0 m", formatted)
    }

    @Test
    fun `test pace formatting - normal pace`() {
        val formatted = ExerciseTrackingUtils.formatPace(5.5)
        assertEquals("5'30\"", formatted)
    }

    @Test
    fun `test pace formatting - zero pace`() {
        val formatted = ExerciseTrackingUtils.formatPace(0.0)
        assertEquals("--'--\"", formatted)
    }

    @Test
    fun `test pace formatting - very slow pace`() {
        val formatted = ExerciseTrackingUtils.formatPace(61.0)
        assertEquals("--'--\"", formatted)
    }

    // ==================== 4. 导航路由测试 ====================

    @Test
    fun `test ExerciseSummary route with all parameters`() {
        val route = Screen.ExerciseSummary.createRoute(
            planId = 10,
            exerciseType = "running",
            distance = 4500.0,
            duration = 2100000L,
            calories = 280.0,
            pace = 5.5
        )
        assertTrue(route.contains("exercise_summary"))
        assertTrue(route.contains("planId=10"))
        assertTrue(route.contains("exerciseType=running"))
        assertTrue(route.contains("distance=4500.0"))
        assertTrue(route.contains("duration=2100000"))
        assertTrue(route.contains("calories=280.0"))
        assertTrue(route.contains("pace=5.5"))
    }

    @Test
    fun `test ExerciseSummary route without planId`() {
        val route = Screen.ExerciseSummary.createRoute(
            exerciseType = "walking",
            distance = 1000.0,
            duration = 1800000L,
            calories = 100.0,
            pace = 8.0
        )
        assertTrue(route.contains("planId=&"))
        assertTrue(route.contains("exerciseType=walking"))
    }

    @Test
    fun `test ExerciseSummary route default values`() {
        val route = Screen.ExerciseSummary.createRoute()
        assertTrue(route.contains("exerciseType=walking"))
        assertTrue(route.contains("distance=0.0"))
        assertTrue(route.contains("duration=0"))
        assertTrue(route.contains("calories=0.0"))
    }

    // ==================== 5. 保存状态管理测试 ====================

    @Test
    fun `test SaveExerciseState Idle`() {
        val state: SaveExerciseState = SaveExerciseState.Idle
        assertTrue(state is SaveExerciseState.Idle)
    }

    @Test
    fun `test SaveExerciseState Saving`() {
        val state: SaveExerciseState = SaveExerciseState.Saving
        assertTrue(state is SaveExerciseState.Saving)
    }

    @Test
    fun `test SaveExerciseState Success`() {
        val state = SaveExerciseState.Success(recordId = 42)
        assertTrue(state is SaveExerciseState.Success)
        assertEquals(42, state.recordId)
    }

    @Test
    fun `test SaveExerciseState Error`() {
        val state = SaveExerciseState.Error("网络错误")
        assertTrue(state is SaveExerciseState.Error)
        assertEquals("网络错误", state.message)
    }

    // ==================== 6. 边界条件测试 ====================

    @Test
    fun `test calorie estimation - walking 0 minutes`() {
        val calories = ExerciseTrackingUtils.estimateCalories("walking", 0.0)
        assertEquals(0.0, calories, 0.01)
    }

    @Test
    fun `test calorie estimation - walking 30 minutes`() {
        val calories = ExerciseTrackingUtils.estimateCalories("walking", 30.0)
        // 3.5 METs * 70kg * 0.5h = 122.5 kcal
        assertEquals(122.5, calories, 0.01)
    }

    @Test
    fun `test calorie estimation - running 30 minutes`() {
        val calories = ExerciseTrackingUtils.estimateCalories("running", 30.0)
        // 8.0 METs * 70kg * 0.5h = 280 kcal
        assertEquals(280.0, calories, 0.01)
    }

    @Test
    fun `test calorie estimation - cycling 45 minutes`() {
        val calories = ExerciseTrackingUtils.estimateCalories("cycling", 45.0)
        // 6.0 METs * 70kg * 0.75h = 315 kcal
        assertEquals(315.0, calories, 0.01)
    }

    @Test
    fun `test calorie estimation - unknown type`() {
        val calories = ExerciseTrackingUtils.estimateCalories("yoga", 30.0)
        // Default 4.0 METs * 70kg * 0.5h = 140 kcal
        assertEquals(140.0, calories, 0.01)
    }

    @Test
    fun `test duration millis to minutes conversion`() {
        // 35 minutes = 2100000 ms
        val millis = 2100000L
        val minutes = (millis / 60000).toInt()
        assertEquals(35, minutes)
    }

    @Test
    fun `test duration millis to minutes conversion - edge case`() {
        // 59999 ms = 0 minutes (integer division)
        val millis = 59999L
        val minutes = (millis / 60000).toInt()
        assertEquals(0, minutes)
    }

    @Test
    fun `test duration millis to minutes conversion - 1 minute`() {
        val millis = 60000L
        val minutes = (millis / 60000).toInt()
        assertEquals(1, minutes)
    }

    @Test
    fun `test exercise type display name`() {
        assertEquals("散步", getExerciseTypeDisplayName("walking"))
        assertEquals("跑步", getExerciseTypeDisplayName("running"))
        assertEquals("骑行", getExerciseTypeDisplayName("cycling"))
        assertEquals("徒步", getExerciseTypeDisplayName("hiking"))
        assertEquals("运动", getExerciseTypeDisplayName("unknown"))
    }

    @Test
    fun `test large distance formatting`() {
        val formatted = ExerciseTrackingUtils.formatDistance(42195.0) // marathon
        assertEquals("42.20 km", formatted)
    }

    @Test
    fun `test very small distance formatting`() {
        val formatted = ExerciseTrackingUtils.formatDistance(5.0)
        assertEquals("5 m", formatted)
    }

    @Test
    fun `test achievement rate - very high`() {
        val rate = calculateAchievementRate(1000.0, 100.0)
        assertEquals(1000.0, rate!!, 0.01)
    }

    @Test
    fun `test achievement rate - tiny actual`() {
        val rate = calculateAchievementRate(0.1, 1000.0)
        assertEquals(0.01, rate!!, 0.01)
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算达成率（模拟ExerciseSummaryPage中的逻辑）
     * @return 达成率百分比，如果planned为null或0则返回null
     */
    private fun calculateAchievementRate(actual: Double, planned: Double?): Double? {
        if (planned == null || planned <= 0) return null
        return Math.round(actual / planned * 10000.0) / 100.0
    }

    /**
     * 获取运动类型的显示名称（模拟UI中的逻辑）
     */
    private fun getExerciseTypeDisplayName(type: String): String {
        return when (type) {
            "walking" -> "散步"
            "running" -> "跑步"
            "cycling" -> "骑行"
            "hiking" -> "徒步"
            else -> "运动"
        }
    }
}
