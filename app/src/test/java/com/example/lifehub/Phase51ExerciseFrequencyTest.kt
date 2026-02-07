package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.viewmodel.ExerciseFrequencyUiState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 51: 运动频率分析 - 综合单元测试
 *
 * 测试覆盖：
 * 1. DailyExerciseFrequency 数据模型（构造、默认值、字段验证）
 * 2. ExerciseTypeDistribution 数据模型（构造、百分比）
 * 3. ExerciseFrequencyData 数据模型（完整数据、空数据、评级字段）
 * 4. ExerciseFrequencyResponse JSON反序列化（完整响应、空data、周/月周期）
 * 5. ExerciseFrequencyUiState 状态机（Idle/Loading/Success/Error）
 * 6. 边界条件（零值、空列表、未知运动类型）
 * 7. 多运动类型分布场景
 * 8. 周/月周期切换场景
 */
class Phase51ExerciseFrequencyTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder().create()
    }

    // ==================== DailyExerciseFrequency 模型测试 ====================

    @Test
    fun `DailyExerciseFrequency - 基本构造和字段`() {
        val data = DailyExerciseFrequency(
            date = "2026-02-07",
            count = 2,
            totalDuration = 65,
            totalCalories = 350.0,
            exerciseTypes = listOf("walking", "running")
        )
        assertEquals("2026-02-07", data.date)
        assertEquals(2, data.count)
        assertEquals(65, data.totalDuration)
        assertEquals(350.0, data.totalCalories, 0.001)
        assertEquals(listOf("walking", "running"), data.exerciseTypes)
    }

    @Test
    fun `DailyExerciseFrequency - 默认值`() {
        val data = DailyExerciseFrequency(date = "2026-02-07")
        assertEquals(0, data.count)
        assertEquals(0, data.totalDuration)
        assertEquals(0.0, data.totalCalories, 0.001)
        assertEquals(emptyList<String>(), data.exerciseTypes)
    }

    @Test
    fun `DailyExerciseFrequency - JSON反序列化`() {
        val json = """
            {
                "date": "2026-02-05",
                "count": 3,
                "total_duration": 90,
                "total_calories": 500.0,
                "exercise_types": ["cycling", "swimming"]
            }
        """.trimIndent()
        val data = gson.fromJson(json, DailyExerciseFrequency::class.java)
        assertEquals("2026-02-05", data.date)
        assertEquals(3, data.count)
        assertEquals(90, data.totalDuration)
        assertEquals(500.0, data.totalCalories, 0.001)
        assertEquals(listOf("cycling", "swimming"), data.exerciseTypes)
    }

    @Test
    fun `DailyExerciseFrequency - 零值场景`() {
        val data = DailyExerciseFrequency(
            date = "2026-02-07",
            count = 0,
            totalDuration = 0,
            totalCalories = 0.0,
            exerciseTypes = emptyList()
        )
        assertEquals(0, data.count)
        assertEquals(0, data.totalDuration)
        assertEquals(0.0, data.totalCalories, 0.001)
        assertTrue(data.exerciseTypes.isEmpty())
    }

    // ==================== ExerciseTypeDistribution 模型测试 ====================

    @Test
    fun `ExerciseTypeDistribution - 基本构造`() {
        val dist = ExerciseTypeDistribution(
            exerciseType = "walking",
            label = "步行",
            count = 5,
            totalDuration = 150,
            totalCalories = 600.0,
            percentage = 35.7
        )
        assertEquals("walking", dist.exerciseType)
        assertEquals("步行", dist.label)
        assertEquals(5, dist.count)
        assertEquals(150, dist.totalDuration)
        assertEquals(600.0, dist.totalCalories, 0.001)
        assertEquals(35.7, dist.percentage, 0.001)
    }

    @Test
    fun `ExerciseTypeDistribution - JSON反序列化`() {
        val json = """
            {
                "exercise_type": "running",
                "label": "跑步",
                "count": 10,
                "total_duration": 300,
                "total_calories": 2500.0,
                "percentage": 50.0
            }
        """.trimIndent()
        val dist = gson.fromJson(json, ExerciseTypeDistribution::class.java)
        assertEquals("running", dist.exerciseType)
        assertEquals("跑步", dist.label)
        assertEquals(10, dist.count)
        assertEquals(300, dist.totalDuration)
        assertEquals(2500.0, dist.totalCalories, 0.001)
        assertEquals(50.0, dist.percentage, 0.001)
    }

    @Test
    fun `ExerciseTypeDistribution - 默认值`() {
        val dist = ExerciseTypeDistribution(
            exerciseType = "yoga",
            label = "yoga"
        )
        assertEquals(0, dist.count)
        assertEquals(0, dist.totalDuration)
        assertEquals(0.0, dist.totalCalories, 0.001)
        assertEquals(0.0, dist.percentage, 0.001)
    }

    // ==================== ExerciseFrequencyData 模型测试 ====================

    @Test
    fun `ExerciseFrequencyData - 完整数据构造`() {
        val data = ExerciseFrequencyData(
            userId = 1,
            period = "week",
            periodLabel = "最近一周",
            startDate = "2026-02-01",
            endDate = "2026-02-07",
            totalDays = 7,
            activeDays = 4,
            totalExerciseCount = 6,
            totalDuration = 210,
            totalCalories = 1200.0,
            avgFrequency = 6.0,
            avgDurationPerSession = 35.0,
            avgCaloriesPerSession = 200.0,
            dailyData = listOf(
                DailyExerciseFrequency(date = "2026-02-01", count = 2, totalDuration = 60, totalCalories = 400.0),
                DailyExerciseFrequency(date = "2026-02-02", count = 0)
            ),
            typeDistribution = listOf(
                ExerciseTypeDistribution(exerciseType = "walking", label = "步行", count = 3, percentage = 50.0),
                ExerciseTypeDistribution(exerciseType = "running", label = "跑步", count = 3, percentage = 50.0)
            ),
            frequencyRating = "good",
            frequencySuggestion = "运动频率良好，建议逐步增加到每周5天"
        )
        assertEquals(1, data.userId)
        assertEquals("week", data.period)
        assertEquals("最近一周", data.periodLabel)
        assertEquals(7, data.totalDays)
        assertEquals(4, data.activeDays)
        assertEquals(6, data.totalExerciseCount)
        assertEquals(210, data.totalDuration)
        assertEquals(1200.0, data.totalCalories, 0.001)
        assertEquals(6.0, data.avgFrequency, 0.001)
        assertEquals(35.0, data.avgDurationPerSession, 0.001)
        assertEquals(200.0, data.avgCaloriesPerSession, 0.001)
        assertEquals(2, data.dailyData.size)
        assertEquals(2, data.typeDistribution.size)
        assertEquals("good", data.frequencyRating)
        assertTrue(data.frequencySuggestion.isNotEmpty())
    }

    @Test
    fun `ExerciseFrequencyData - 空数据默认值`() {
        val data = ExerciseFrequencyData(
            userId = 1,
            period = "week",
            periodLabel = "最近一周",
            startDate = "2026-02-01",
            endDate = "2026-02-07"
        )
        assertEquals(0, data.totalDays)
        assertEquals(0, data.activeDays)
        assertEquals(0, data.totalExerciseCount)
        assertEquals(0, data.totalDuration)
        assertEquals(0.0, data.totalCalories, 0.001)
        assertEquals(0.0, data.avgFrequency, 0.001)
        assertTrue(data.dailyData.isEmpty())
        assertTrue(data.typeDistribution.isEmpty())
        assertEquals("insufficient", data.frequencyRating)
        assertEquals("", data.frequencySuggestion)
    }

    @Test
    fun `ExerciseFrequencyData - month周期数据`() {
        val data = ExerciseFrequencyData(
            userId = 1,
            period = "month",
            periodLabel = "最近一个月",
            startDate = "2026-01-09",
            endDate = "2026-02-07",
            totalDays = 30,
            activeDays = 15,
            totalExerciseCount = 20,
            frequencyRating = "good"
        )
        assertEquals("month", data.period)
        assertEquals("最近一个月", data.periodLabel)
        assertEquals(30, data.totalDays)
        assertEquals(15, data.activeDays)
        assertEquals("good", data.frequencyRating)
    }

    @Test
    fun `ExerciseFrequencyData - 各评级值`() {
        val ratings = listOf("excellent", "good", "fair", "insufficient")
        ratings.forEach { rating ->
            val data = ExerciseFrequencyData(
                userId = 1, period = "week", periodLabel = "最近一周",
                startDate = "2026-02-01", endDate = "2026-02-07",
                frequencyRating = rating
            )
            assertEquals(rating, data.frequencyRating)
        }
    }

    // ==================== ExerciseFrequencyResponse JSON反序列化测试 ====================

    @Test
    fun `ExerciseFrequencyResponse - 完整JSON反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 1,
                "period": "week",
                "period_label": "最近一周",
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "total_days": 7,
                "active_days": 4,
                "total_exercise_count": 6,
                "total_duration": 210,
                "total_calories": 1200.0,
                "avg_frequency": 6.0,
                "avg_duration_per_session": 35.0,
                "avg_calories_per_session": 200.0,
                "daily_data": [
                    {
                        "date": "2026-02-01",
                        "count": 2,
                        "total_duration": 60,
                        "total_calories": 400.0,
                        "exercise_types": ["walking", "running"]
                    },
                    {
                        "date": "2026-02-02",
                        "count": 0,
                        "total_duration": 0,
                        "total_calories": 0.0,
                        "exercise_types": []
                    }
                ],
                "type_distribution": [
                    {
                        "exercise_type": "walking",
                        "label": "步行",
                        "count": 3,
                        "total_duration": 90,
                        "total_calories": 400.0,
                        "percentage": 50.0
                    },
                    {
                        "exercise_type": "running",
                        "label": "跑步",
                        "count": 3,
                        "total_duration": 120,
                        "total_calories": 800.0,
                        "percentage": 50.0
                    }
                ],
                "frequency_rating": "good",
                "frequency_suggestion": "运动频率良好"
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseFrequencyResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("获取成功", response.message)
        assertNotNull(response.data)

        val data = response.data!!
        assertEquals(1, data.userId)
        assertEquals("week", data.period)
        assertEquals("最近一周", data.periodLabel)
        assertEquals(7, data.totalDays)
        assertEquals(4, data.activeDays)
        assertEquals(6, data.totalExerciseCount)
        assertEquals(210, data.totalDuration)
        assertEquals(1200.0, data.totalCalories, 0.001)
        assertEquals(6.0, data.avgFrequency, 0.001)
        assertEquals(35.0, data.avgDurationPerSession, 0.001)
        assertEquals(200.0, data.avgCaloriesPerSession, 0.001)

        // daily_data
        assertEquals(2, data.dailyData.size)
        assertEquals("2026-02-01", data.dailyData[0].date)
        assertEquals(2, data.dailyData[0].count)
        assertEquals(60, data.dailyData[0].totalDuration)
        assertEquals(400.0, data.dailyData[0].totalCalories, 0.001)
        assertEquals(listOf("walking", "running"), data.dailyData[0].exerciseTypes)
        assertEquals(0, data.dailyData[1].count)

        // type_distribution
        assertEquals(2, data.typeDistribution.size)
        assertEquals("walking", data.typeDistribution[0].exerciseType)
        assertEquals("步行", data.typeDistribution[0].label)
        assertEquals(3, data.typeDistribution[0].count)
        assertEquals(50.0, data.typeDistribution[0].percentage, 0.001)

        // rating
        assertEquals("good", data.frequencyRating)
        assertEquals("运动频率良好", data.frequencySuggestion)
    }

    @Test
    fun `ExerciseFrequencyResponse - 空data场景`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功（用户无记录）",
            "data": null
        }
        """.trimIndent()
        val response = gson.fromJson(json, ExerciseFrequencyResponse::class.java)
        assertEquals(200, response.code)
        assertNull(response.data)
    }

    @Test
    fun `ExerciseFrequencyResponse - month周期完整反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 2,
                "period": "month",
                "period_label": "最近一个月",
                "start_date": "2026-01-09",
                "end_date": "2026-02-07",
                "total_days": 30,
                "active_days": 20,
                "total_exercise_count": 25,
                "total_duration": 750,
                "total_calories": 5000.0,
                "avg_frequency": 5.8,
                "avg_duration_per_session": 30.0,
                "avg_calories_per_session": 200.0,
                "daily_data": [],
                "type_distribution": [],
                "frequency_rating": "excellent",
                "frequency_suggestion": "月均每周运动4.7天，频率优秀！"
            }
        }
        """.trimIndent()
        val response = gson.fromJson(json, ExerciseFrequencyResponse::class.java)
        val data = response.data!!
        assertEquals("month", data.period)
        assertEquals("最近一个月", data.periodLabel)
        assertEquals(30, data.totalDays)
        assertEquals(20, data.activeDays)
        assertEquals("excellent", data.frequencyRating)
    }

    @Test
    fun `ExerciseFrequencyResponse - 无运动记录的空数据`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 3,
                "period": "week",
                "period_label": "最近一周",
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "total_days": 7,
                "active_days": 0,
                "total_exercise_count": 0,
                "total_duration": 0,
                "total_calories": 0.0,
                "avg_frequency": 0.0,
                "avg_duration_per_session": 0.0,
                "avg_calories_per_session": 0.0,
                "daily_data": [],
                "type_distribution": [],
                "frequency_rating": "insufficient",
                "frequency_suggestion": "本周暂无运动记录，建议尽快开始运动"
            }
        }
        """.trimIndent()
        val response = gson.fromJson(json, ExerciseFrequencyResponse::class.java)
        val data = response.data!!
        assertEquals(0, data.activeDays)
        assertEquals(0, data.totalExerciseCount)
        assertEquals("insufficient", data.frequencyRating)
        assertTrue(data.dailyData.isEmpty())
        assertTrue(data.typeDistribution.isEmpty())
    }

    // ==================== ExerciseFrequencyUiState 状态机测试 ====================

    @Test
    fun `ExerciseFrequencyUiState - Idle状态`() {
        val state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Idle
        assertTrue(state is ExerciseFrequencyUiState.Idle)
    }

    @Test
    fun `ExerciseFrequencyUiState - Loading状态`() {
        val state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Loading
        assertTrue(state is ExerciseFrequencyUiState.Loading)
    }

    @Test
    fun `ExerciseFrequencyUiState - Success状态`() {
        val data = ExerciseFrequencyData(
            userId = 1, period = "week", periodLabel = "最近一周",
            startDate = "2026-02-01", endDate = "2026-02-07",
            activeDays = 3, frequencyRating = "good"
        )
        val state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Success(data)
        assertTrue(state is ExerciseFrequencyUiState.Success)
        assertEquals(data, (state as ExerciseFrequencyUiState.Success).data)
        assertEquals(3, state.data.activeDays)
        assertEquals("good", state.data.frequencyRating)
    }

    @Test
    fun `ExerciseFrequencyUiState - Error状态`() {
        val state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Error("网络连接失败")
        assertTrue(state is ExerciseFrequencyUiState.Error)
        assertEquals("网络连接失败", (state as ExerciseFrequencyUiState.Error).message)
    }

    @Test
    fun `ExerciseFrequencyUiState - 状态转换序列`() {
        var state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Idle
        assertTrue(state is ExerciseFrequencyUiState.Idle)

        state = ExerciseFrequencyUiState.Loading
        assertTrue(state is ExerciseFrequencyUiState.Loading)

        val data = ExerciseFrequencyData(
            userId = 1, period = "week", periodLabel = "最近一周",
            startDate = "2026-02-01", endDate = "2026-02-07"
        )
        state = ExerciseFrequencyUiState.Success(data)
        assertTrue(state is ExerciseFrequencyUiState.Success)

        state = ExerciseFrequencyUiState.Error("请求超时")
        assertTrue(state is ExerciseFrequencyUiState.Error)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `边界 - 单类型100%分布`() {
        val dist = listOf(
            ExerciseTypeDistribution(
                exerciseType = "walking", label = "步行",
                count = 10, totalDuration = 300, totalCalories = 1000.0,
                percentage = 100.0
            )
        )
        assertEquals(1, dist.size)
        assertEquals(100.0, dist[0].percentage, 0.001)
    }

    @Test
    fun `边界 - 多类型分布百分比总和`() {
        val dist = listOf(
            ExerciseTypeDistribution(exerciseType = "walking", label = "步行", count = 3, percentage = 42.9),
            ExerciseTypeDistribution(exerciseType = "running", label = "跑步", count = 2, percentage = 28.6),
            ExerciseTypeDistribution(exerciseType = "cycling", label = "骑行", count = 2, percentage = 28.6)
        )
        val totalPct = dist.sumOf { it.percentage }
        assertTrue("百分比之和应接近100%", totalPct in 99.5..100.5)
    }

    @Test
    fun `边界 - 大量每日数据（月模式30天）`() {
        val dailyData = (1..30).map { day ->
            DailyExerciseFrequency(
                date = "2026-01-%02d".format(day.coerceAtMost(31)),
                count = if (day % 2 == 0) 1 else 0,
                totalDuration = if (day % 2 == 0) 30 else 0,
                totalCalories = if (day % 2 == 0) 200.0 else 0.0
            )
        }
        assertEquals(30, dailyData.size)
        val activeDays = dailyData.count { it.count > 0 }
        assertEquals(15, activeDays)
    }

    @Test
    fun `边界 - 未知运动类型标签使用原始类型名`() {
        val dist = ExerciseTypeDistribution(
            exerciseType = "yoga",
            label = "yoga",
            count = 5,
            percentage = 100.0
        )
        assertEquals("yoga", dist.exerciseType)
        assertEquals("yoga", dist.label)
    }

    @Test
    fun `边界 - 同一天多种运动类型`() {
        val daily = DailyExerciseFrequency(
            date = "2026-02-07",
            count = 4,
            totalDuration = 120,
            totalCalories = 800.0,
            exerciseTypes = listOf("cycling", "running", "swimming", "walking")
        )
        assertEquals(4, daily.count)
        assertEquals(4, daily.exerciseTypes.size)
        assertTrue(daily.exerciseTypes.contains("swimming"))
    }

    @Test
    fun `边界 - avgFrequency为0时的场景`() {
        val data = ExerciseFrequencyData(
            userId = 1, period = "week", periodLabel = "最近一周",
            startDate = "2026-02-01", endDate = "2026-02-07",
            totalExerciseCount = 0,
            avgFrequency = 0.0,
            avgDurationPerSession = 0.0,
            avgCaloriesPerSession = 0.0
        )
        assertEquals(0.0, data.avgFrequency, 0.001)
        assertEquals(0.0, data.avgDurationPerSession, 0.001)
        assertEquals(0.0, data.avgCaloriesPerSession, 0.001)
    }

    // ==================== 集成场景测试 ====================

    @Test
    fun `集成 - 模拟真实周数据完整流程`() {
        // 模拟后端返回的JSON
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 1,
                "period": "week",
                "period_label": "最近一周",
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "total_days": 7,
                "active_days": 5,
                "total_exercise_count": 7,
                "total_duration": 245,
                "total_calories": 1750.0,
                "avg_frequency": 7.0,
                "avg_duration_per_session": 35.0,
                "avg_calories_per_session": 250.0,
                "daily_data": [
                    {"date": "2026-02-01", "count": 1, "total_duration": 30, "total_calories": 200.0, "exercise_types": ["walking"]},
                    {"date": "2026-02-02", "count": 0, "total_duration": 0, "total_calories": 0.0, "exercise_types": []},
                    {"date": "2026-02-03", "count": 2, "total_duration": 60, "total_calories": 450.0, "exercise_types": ["running", "walking"]},
                    {"date": "2026-02-04", "count": 1, "total_duration": 45, "total_calories": 350.0, "exercise_types": ["cycling"]},
                    {"date": "2026-02-05", "count": 0, "total_duration": 0, "total_calories": 0.0, "exercise_types": []},
                    {"date": "2026-02-06", "count": 1, "total_duration": 60, "total_calories": 400.0, "exercise_types": ["swimming"]},
                    {"date": "2026-02-07", "count": 2, "total_duration": 50, "total_calories": 350.0, "exercise_types": ["jogging", "walking"]}
                ],
                "type_distribution": [
                    {"exercise_type": "walking", "label": "步行", "count": 3, "total_duration": 80, "total_calories": 500.0, "percentage": 42.9},
                    {"exercise_type": "running", "label": "跑步", "count": 1, "total_duration": 30, "total_calories": 250.0, "percentage": 14.3},
                    {"exercise_type": "cycling", "label": "骑行", "count": 1, "total_duration": 45, "total_calories": 350.0, "percentage": 14.3},
                    {"exercise_type": "swimming", "label": "游泳", "count": 1, "total_duration": 60, "total_calories": 400.0, "percentage": 14.3},
                    {"exercise_type": "jogging", "label": "慢跑", "count": 1, "total_duration": 30, "total_calories": 250.0, "percentage": 14.3}
                ],
                "frequency_rating": "excellent",
                "frequency_suggestion": "运动频率优秀，保持每周5天以上运动习惯！"
            }
        }
        """.trimIndent()

        // 1. 反序列化
        val response = gson.fromJson(json, ExerciseFrequencyResponse::class.java)
        assertEquals(200, response.code)
        assertNotNull(response.data)

        val data = response.data!!

        // 2. 基本统计验证
        assertEquals(7, data.totalDays)
        assertEquals(5, data.activeDays)
        assertEquals(7, data.totalExerciseCount)
        assertEquals(245, data.totalDuration)
        assertEquals(1750.0, data.totalCalories, 0.001)

        // 3. 每日数据完整性
        assertEquals(7, data.dailyData.size)
        val zeroDays = data.dailyData.filter { it.count == 0 }
        assertEquals(2, zeroDays.size)

        // 4. 类型分布
        assertEquals(5, data.typeDistribution.size)
        val walkingDist = data.typeDistribution.first { it.exerciseType == "walking" }
        assertEquals(3, walkingDist.count)
        assertEquals(42.9, walkingDist.percentage, 0.1)

        // 5. 评级
        assertEquals("excellent", data.frequencyRating)
        assertTrue(data.frequencySuggestion.contains("优秀"))

        // 6. 模拟UI状态转换
        var state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Idle
        state = ExerciseFrequencyUiState.Loading
        state = ExerciseFrequencyUiState.Success(data)
        assertTrue(state is ExerciseFrequencyUiState.Success)
        assertEquals("excellent", (state as ExerciseFrequencyUiState.Success).data.frequencyRating)
    }

    @Test
    fun `集成 - 模拟网络错误后重试成功`() {
        // 第一次：网络错误
        var state: ExerciseFrequencyUiState = ExerciseFrequencyUiState.Error("网络连接失败，请检查后端服务是否启动")
        assertTrue(state is ExerciseFrequencyUiState.Error)

        // 重试加载中
        state = ExerciseFrequencyUiState.Loading
        assertTrue(state is ExerciseFrequencyUiState.Loading)

        // 重试成功
        val data = ExerciseFrequencyData(
            userId = 1, period = "week", periodLabel = "最近一周",
            startDate = "2026-02-01", endDate = "2026-02-07",
            activeDays = 3, frequencyRating = "good",
            frequencySuggestion = "运动频率良好"
        )
        state = ExerciseFrequencyUiState.Success(data)
        assertTrue(state is ExerciseFrequencyUiState.Success)
        assertEquals("good", (state as ExerciseFrequencyUiState.Success).data.frequencyRating)
    }
}
