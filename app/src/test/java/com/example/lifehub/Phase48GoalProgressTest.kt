package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.viewmodel.GoalProgressState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 48: 健康目标达成情况展示 - 综合单元测试
 *
 * 测试覆盖：
 * 1. GoalDimension 数据模型（达成率计算、进度条比例、边界条件）
 * 2. GoalProgressData 数据模型（完整数据、空数据、默认值）
 * 3. GoalProgressResponse JSON反序列化（完整响应、空data、各目标类型）
 * 4. GoalStatusUtil 工具类（状态标签、目标标签、时间段标签）
 * 5. GoalProgressState 状态机（Idle/Loading/Success/Error）
 * 6. 边界条件（零值、超大值、负值、空列表）
 * 7. 多目标类型场景（减脂/增肌/控糖/均衡）
 */
class Phase48GoalProgressTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder().create()
    }

    // ==================== GoalDimension 模型测试 ====================

    @Test
    fun `GoalDimension - 基本构造和字段`() {
        val dim = GoalDimension(
            name = "热量控制",
            score = 85.0,
            status = "good",
            currentValue = 1600.0,
            targetValue = 1756.0,
            unit = "kcal/天",
            description = "日均摄入1600kcal，建议1756kcal"
        )
        assertEquals("热量控制", dim.name)
        assertEquals(85.0, dim.score, 0.001)
        assertEquals("good", dim.status)
        assertEquals(1600.0, dim.currentValue, 0.001)
        assertEquals(1756.0, dim.targetValue, 0.001)
        assertEquals("kcal/天", dim.unit)
        assertEquals("日均摄入1600kcal，建议1756kcal", dim.description)
    }

    @Test
    fun `GoalDimension - achievementRate正常计算`() {
        val dim = GoalDimension(name = "热量控制", currentValue = 1600.0, targetValue = 2000.0)
        assertEquals(80.0, dim.achievementRate, 0.001)
    }

    @Test
    fun `GoalDimension - achievementRate超过100%时cap到150%`() {
        val dim = GoalDimension(name = "蛋白质", currentValue = 200.0, targetValue = 100.0)
        // 200/100 * 100 = 200.0, capped to 150.0
        assertEquals(150.0, dim.achievementRate, 0.001)
    }

    @Test
    fun `GoalDimension - achievementRate目标值为0时返回0`() {
        val dim = GoalDimension(name = "测试", currentValue = 100.0, targetValue = 0.0)
        assertEquals(0.0, dim.achievementRate, 0.001)
    }

    @Test
    fun `GoalDimension - achievementRate当前值为0时返回0`() {
        val dim = GoalDimension(name = "测试", currentValue = 0.0, targetValue = 100.0)
        assertEquals(0.0, dim.achievementRate, 0.001)
    }

    @Test
    fun `GoalDimension - progressFraction正常计算`() {
        val dim = GoalDimension(name = "测试", currentValue = 75.0, targetValue = 100.0)
        assertEquals(0.75f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - progressFraction超过1时cap到1`() {
        val dim = GoalDimension(name = "测试", currentValue = 150.0, targetValue = 100.0)
        assertEquals(1.0f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - progressFraction目标值为0时返回0`() {
        val dim = GoalDimension(name = "测试", currentValue = 100.0, targetValue = 0.0)
        assertEquals(0.0f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - progressFraction当前值为0时返回0`() {
        val dim = GoalDimension(name = "测试", currentValue = 0.0, targetValue = 100.0)
        assertEquals(0.0f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - progressFraction半值精确`() {
        val dim = GoalDimension(name = "测试", currentValue = 50.0, targetValue = 100.0)
        assertEquals(0.5f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - 默认值`() {
        val dim = GoalDimension(name = "测试维度")
        assertEquals(0.0, dim.score, 0.001)
        assertEquals("fair", dim.status)
        assertEquals(0.0, dim.currentValue, 0.001)
        assertEquals(0.0, dim.targetValue, 0.001)
        assertEquals("", dim.unit)
        assertEquals("", dim.description)
    }

    // ==================== GoalProgressData 模型测试 ====================

    @Test
    fun `GoalProgressData - 完整构造`() {
        val data = createSampleGoalProgressData()
        assertEquals(1, data.userId)
        assertEquals("reduce_fat", data.healthGoal)
        assertEquals("减脂", data.healthGoalLabel)
        assertEquals(7, data.periodDays)
        assertEquals("2026-02-01", data.startDate)
        assertEquals("2026-02-07", data.endDate)
        assertEquals(75.0, data.overallScore, 0.001)
        assertEquals("good", data.overallStatus)
        assertEquals(3, data.dimensions.size)
        assertEquals(2, data.suggestions.size)
        assertEquals(7, data.streakDays)
    }

    @Test
    fun `GoalProgressData - 默认值`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡"
        )
        assertEquals(7, data.periodDays)
        assertEquals("", data.startDate)
        assertEquals("", data.endDate)
        assertEquals(0.0, data.overallScore, 0.001)
        assertEquals("fair", data.overallStatus)
        assertTrue(data.dimensions.isEmpty())
        assertTrue(data.suggestions.isEmpty())
        assertEquals(0, data.streakDays)
    }

    @Test
    fun `GoalProgressData - 空维度列表`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            dimensions = emptyList()
        )
        assertTrue(data.dimensions.isEmpty())
    }

    @Test
    fun `GoalProgressData - 空建议列表`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            suggestions = emptyList()
        )
        assertTrue(data.suggestions.isEmpty())
    }

    // ==================== GoalProgressResponse JSON反序列化测试 ====================

    @Test
    fun `GoalProgressResponse - 完整JSON反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 1,
                "health_goal": "reduce_fat",
                "health_goal_label": "减脂",
                "period_days": 7,
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "overall_score": 75.0,
                "overall_status": "good",
                "dimensions": [
                    {
                        "name": "热量控制",
                        "score": 85.0,
                        "status": "good",
                        "current_value": 1600.0,
                        "target_value": 1756.0,
                        "unit": "kcal/天",
                        "description": "日均摄入1600kcal，建议1756kcal"
                    },
                    {
                        "name": "脂肪比例",
                        "score": 90.0,
                        "status": "excellent",
                        "current_value": 22.5,
                        "target_value": 30.0,
                        "unit": "%",
                        "description": "脂肪占比22.5%，在建议范围内"
                    }
                ],
                "suggestions": [
                    "运动消耗不足，建议增加有氧运动频率和时长",
                    "已连续记录7天，非常棒，继续保持！"
                ],
                "streak_days": 7
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("获取成功", response.message)
        assertNotNull(response.data)

        val data = response.data!!
        assertEquals(1, data.userId)
        assertEquals("reduce_fat", data.healthGoal)
        assertEquals("减脂", data.healthGoalLabel)
        assertEquals(7, data.periodDays)
        assertEquals("2026-02-01", data.startDate)
        assertEquals("2026-02-07", data.endDate)
        assertEquals(75.0, data.overallScore, 0.001)
        assertEquals("good", data.overallStatus)
        assertEquals(2, data.dimensions.size)
        assertEquals(2, data.suggestions.size)
        assertEquals(7, data.streakDays)

        // 验证第一个维度
        val dim1 = data.dimensions[0]
        assertEquals("热量控制", dim1.name)
        assertEquals(85.0, dim1.score, 0.001)
        assertEquals("good", dim1.status)
        assertEquals(1600.0, dim1.currentValue, 0.001)
        assertEquals(1756.0, dim1.targetValue, 0.001)
        assertEquals("kcal/天", dim1.unit)

        // 验证第二个维度
        val dim2 = data.dimensions[1]
        assertEquals("脂肪比例", dim2.name)
        assertEquals(90.0, dim2.score, 0.001)
        assertEquals("excellent", dim2.status)
    }

    @Test
    fun `GoalProgressResponse - data为null时正确反序列化`() {
        val json = """
        {
            "code": 404,
            "message": "用户不存在",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        assertEquals(404, response.code)
        assertEquals("用户不存在", response.message)
        assertNull(response.data)
    }

    @Test
    fun `GoalProgressResponse - 增肌目标类型反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 2,
                "health_goal": "gain_muscle",
                "health_goal_label": "增肌",
                "period_days": 14,
                "start_date": "2026-01-25",
                "end_date": "2026-02-07",
                "overall_score": 60.0,
                "overall_status": "fair",
                "dimensions": [
                    {
                        "name": "蛋白质摄入",
                        "score": 70.0,
                        "status": "good",
                        "current_value": 95.0,
                        "target_value": 120.0,
                        "unit": "g/天",
                        "description": "日均蛋白质95g，建议120g"
                    }
                ],
                "suggestions": ["建议增加鸡胸肉、鱼肉等高蛋白食物"],
                "streak_days": 3
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        val data = response.data!!
        assertEquals("gain_muscle", data.healthGoal)
        assertEquals("增肌", data.healthGoalLabel)
        assertEquals(14, data.periodDays)
        assertEquals(60.0, data.overallScore, 0.001)
        assertEquals("fair", data.overallStatus)
        assertEquals(1, data.dimensions.size)
        assertEquals("蛋白质摄入", data.dimensions[0].name)
    }

    @Test
    fun `GoalProgressResponse - 控糖目标类型反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 3,
                "health_goal": "control_sugar",
                "health_goal_label": "控糖",
                "period_days": 30,
                "start_date": "2026-01-09",
                "end_date": "2026-02-07",
                "overall_score": 45.0,
                "overall_status": "poor",
                "dimensions": [
                    {
                        "name": "碳水比例",
                        "score": 40.0,
                        "status": "poor",
                        "current_value": 65.0,
                        "target_value": 50.0,
                        "unit": "%",
                        "description": "碳水占比65%，超出建议范围"
                    }
                ],
                "suggestions": ["碳水摄入偏高，建议减少精制碳水"],
                "streak_days": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        val data = response.data!!
        assertEquals("control_sugar", data.healthGoal)
        assertEquals("控糖", data.healthGoalLabel)
        assertEquals(30, data.periodDays)
        assertEquals(45.0, data.overallScore, 0.001)
        assertEquals("poor", data.overallStatus)
        assertEquals(0, data.streakDays)
    }

    @Test
    fun `GoalProgressResponse - 均衡目标类型反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 4,
                "health_goal": "balanced",
                "health_goal_label": "均衡",
                "period_days": 7,
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "overall_score": 92.0,
                "overall_status": "excellent",
                "dimensions": [
                    {
                        "name": "营养均衡",
                        "score": 95.0,
                        "status": "excellent",
                        "current_value": 95.0,
                        "target_value": 100.0,
                        "unit": "分",
                        "description": "营养均衡度很好"
                    },
                    {
                        "name": "运动规律",
                        "score": 90.0,
                        "status": "excellent",
                        "current_value": 5.0,
                        "target_value": 5.0,
                        "unit": "次/周",
                        "description": "运动频率达标"
                    },
                    {
                        "name": "饮食规律",
                        "score": 91.0,
                        "status": "excellent",
                        "current_value": 3.0,
                        "target_value": 3.0,
                        "unit": "餐/天",
                        "description": "三餐规律"
                    }
                ],
                "suggestions": ["表现非常好，继续保持！"],
                "streak_days": 30
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        val data = response.data!!
        assertEquals("balanced", data.healthGoal)
        assertEquals("均衡", data.healthGoalLabel)
        assertEquals(92.0, data.overallScore, 0.001)
        assertEquals("excellent", data.overallStatus)
        assertEquals(3, data.dimensions.size)
        assertEquals(30, data.streakDays)
    }

    @Test
    fun `GoalProgressResponse - 空dimensions列表反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "user_id": 1,
                "health_goal": "balanced",
                "health_goal_label": "均衡",
                "period_days": 7,
                "start_date": "2026-02-01",
                "end_date": "2026-02-07",
                "overall_score": 0.0,
                "overall_status": "poor",
                "dimensions": [],
                "suggestions": [],
                "streak_days": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GoalProgressResponse::class.java)
        assertNotNull(response.data)
        assertTrue(response.data!!.dimensions.isEmpty())
        assertTrue(response.data!!.suggestions.isEmpty())
    }

    // ==================== GoalStatusUtil 测试 ====================

    @Test
    fun `GoalStatusUtil - getStatusLabel正确返回中文`() {
        assertEquals("优秀", GoalStatusUtil.getStatusLabel("excellent"))
        assertEquals("良好", GoalStatusUtil.getStatusLabel("good"))
        assertEquals("一般", GoalStatusUtil.getStatusLabel("fair"))
        assertEquals("待改善", GoalStatusUtil.getStatusLabel("poor"))
        assertEquals("未知", GoalStatusUtil.getStatusLabel("unknown_status"))
        assertEquals("未知", GoalStatusUtil.getStatusLabel(""))
    }

    @Test
    fun `GoalStatusUtil - getStatusDescription正确返回`() {
        assertEquals("90-100分", GoalStatusUtil.getStatusDescription("excellent"))
        assertEquals("70-89分", GoalStatusUtil.getStatusDescription("good"))
        assertEquals("50-69分", GoalStatusUtil.getStatusDescription("fair"))
        assertEquals("0-49分", GoalStatusUtil.getStatusDescription("poor"))
        assertEquals("", GoalStatusUtil.getStatusDescription("unknown"))
    }

    @Test
    fun `GoalStatusUtil - getGoalLabel正确返回中文`() {
        assertEquals("减脂", GoalStatusUtil.getGoalLabel("reduce_fat"))
        assertEquals("增肌", GoalStatusUtil.getGoalLabel("gain_muscle"))
        assertEquals("控糖", GoalStatusUtil.getGoalLabel("control_sugar"))
        assertEquals("均衡", GoalStatusUtil.getGoalLabel("balanced"))
        assertEquals("custom_goal", GoalStatusUtil.getGoalLabel("custom_goal"))
    }

    @Test
    fun `GoalStatusUtil - periodOptions包含预期选项`() {
        assertEquals(listOf(7, 14, 30), GoalStatusUtil.periodOptions)
    }

    @Test
    fun `GoalStatusUtil - getPeriodLabel正确返回`() {
        assertEquals("近7天", GoalStatusUtil.getPeriodLabel(7))
        assertEquals("近14天", GoalStatusUtil.getPeriodLabel(14))
        assertEquals("近30天", GoalStatusUtil.getPeriodLabel(30))
        assertEquals("近90天", GoalStatusUtil.getPeriodLabel(90))
        assertEquals("近1天", GoalStatusUtil.getPeriodLabel(1))
    }

    // ==================== GoalProgressState 状态机测试 ====================

    @Test
    fun `GoalProgressState - Idle状态`() {
        val state: GoalProgressState = GoalProgressState.Idle
        assertTrue(state is GoalProgressState.Idle)
        assertFalse(state is GoalProgressState.Loading)
        assertFalse(state is GoalProgressState.Success)
        assertFalse(state is GoalProgressState.Error)
    }

    @Test
    fun `GoalProgressState - Loading状态`() {
        val state: GoalProgressState = GoalProgressState.Loading
        assertFalse(state is GoalProgressState.Idle)
        assertTrue(state is GoalProgressState.Loading)
    }

    @Test
    fun `GoalProgressState - Success状态携带数据`() {
        val data = createSampleGoalProgressData()
        val state: GoalProgressState = GoalProgressState.Success(data)
        assertTrue(state is GoalProgressState.Success)
        assertEquals(data, (state as GoalProgressState.Success).data)
        assertEquals(75.0, state.data.overallScore, 0.001)
        assertEquals(3, state.data.dimensions.size)
    }

    @Test
    fun `GoalProgressState - Error状态携带消息`() {
        val state: GoalProgressState = GoalProgressState.Error("网络请求失败")
        assertTrue(state is GoalProgressState.Error)
        assertEquals("网络请求失败", (state as GoalProgressState.Error).message)
    }

    @Test
    fun `GoalProgressState - Error状态空消息`() {
        val state: GoalProgressState = GoalProgressState.Error("")
        assertEquals("", (state as GoalProgressState.Error).message)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `GoalDimension - 极大当前值`() {
        val dim = GoalDimension(name = "测试", currentValue = 99999.0, targetValue = 100.0)
        // 达成率cap到150%
        assertEquals(150.0, dim.achievementRate, 0.001)
        // 进度条cap到1.0
        assertEquals(1.0f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalDimension - 极小正数`() {
        val dim = GoalDimension(name = "测试", currentValue = 0.001, targetValue = 100.0)
        assertEquals(0.001, dim.achievementRate, 0.001)
        assertEquals(0.00001f, dim.progressFraction, 0.0001f)
    }

    @Test
    fun `GoalDimension - 当前值等于目标值`() {
        val dim = GoalDimension(name = "测试", currentValue = 100.0, targetValue = 100.0)
        assertEquals(100.0, dim.achievementRate, 0.001)
        assertEquals(1.0f, dim.progressFraction, 0.001f)
    }

    @Test
    fun `GoalProgressData - overallScore为0`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            overallScore = 0.0
        )
        assertEquals(0.0, data.overallScore, 0.001)
    }

    @Test
    fun `GoalProgressData - overallScore为100`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            overallScore = 100.0
        )
        assertEquals(100.0, data.overallScore, 0.001)
    }

    @Test
    fun `GoalProgressData - streakDays为0`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            streakDays = 0
        )
        assertEquals(0, data.streakDays)
    }

    @Test
    fun `GoalProgressData - 大量建议`() {
        val suggestions = (1..20).map { "建议$it" }
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            suggestions = suggestions
        )
        assertEquals(20, data.suggestions.size)
        assertEquals("建议1", data.suggestions[0])
        assertEquals("建议20", data.suggestions[19])
    }

    @Test
    fun `GoalProgressData - 大量维度`() {
        val dimensions = (1..10).map {
            GoalDimension(
                name = "维度$it",
                score = it * 10.0,
                currentValue = it * 50.0,
                targetValue = it * 100.0
            )
        }
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "balanced",
            healthGoalLabel = "均衡",
            dimensions = dimensions
        )
        assertEquals(10, data.dimensions.size)
        assertEquals("维度1", data.dimensions[0].name)
        assertEquals(10.0, data.dimensions[0].score, 0.001)
        assertEquals("维度10", data.dimensions[9].name)
        assertEquals(100.0, data.dimensions[9].score, 0.001)
    }

    // ==================== JSON序列化/反序列化往返测试 ====================

    @Test
    fun `GoalDimension - JSON往返序列化`() {
        val original = GoalDimension(
            name = "热量控制",
            score = 85.5,
            status = "good",
            currentValue = 1600.5,
            targetValue = 1756.3,
            unit = "kcal/天",
            description = "日均摄入1600kcal"
        )
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, GoalDimension::class.java)

        assertEquals(original.name, deserialized.name)
        assertEquals(original.score, deserialized.score, 0.001)
        assertEquals(original.status, deserialized.status)
        assertEquals(original.currentValue, deserialized.currentValue, 0.001)
        assertEquals(original.targetValue, deserialized.targetValue, 0.001)
        assertEquals(original.unit, deserialized.unit)
        assertEquals(original.description, deserialized.description)
    }

    @Test
    fun `GoalProgressData - JSON往返序列化`() {
        val original = createSampleGoalProgressData()
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, GoalProgressData::class.java)

        assertEquals(original.userId, deserialized.userId)
        assertEquals(original.healthGoal, deserialized.healthGoal)
        assertEquals(original.healthGoalLabel, deserialized.healthGoalLabel)
        assertEquals(original.periodDays, deserialized.periodDays)
        assertEquals(original.overallScore, deserialized.overallScore, 0.001)
        assertEquals(original.overallStatus, deserialized.overallStatus)
        assertEquals(original.dimensions.size, deserialized.dimensions.size)
        assertEquals(original.suggestions.size, deserialized.suggestions.size)
        assertEquals(original.streakDays, deserialized.streakDays)
    }

    @Test
    fun `GoalProgressResponse - JSON往返序列化`() {
        val original = GoalProgressResponse(
            code = 200,
            message = "获取成功",
            data = createSampleGoalProgressData()
        )
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, GoalProgressResponse::class.java)

        assertEquals(original.code, deserialized.code)
        assertEquals(original.message, deserialized.message)
        assertNotNull(deserialized.data)
        assertEquals(original.data!!.userId, deserialized.data!!.userId)
    }

    // ==================== 各维度组合场景测试 ====================

    @Test
    fun `减脂目标 - 三维度完整场景`() {
        val data = GoalProgressData(
            userId = 1,
            healthGoal = "reduce_fat",
            healthGoalLabel = "减脂",
            periodDays = 7,
            overallScore = 75.0,
            overallStatus = "good",
            dimensions = listOf(
                GoalDimension("热量控制", 85.0, "good", 1600.0, 1756.0, "kcal/天"),
                GoalDimension("脂肪比例", 90.0, "excellent", 22.5, 30.0, "%"),
                GoalDimension("运动消耗", 60.0, "fair", 180.0, 300.0, "kcal/天")
            ),
            suggestions = listOf("增加有氧运动"),
            streakDays = 5
        )

        assertEquals(3, data.dimensions.size)
        // 热量控制维度
        val calDim = data.dimensions[0]
        assertEquals(0.91f, calDim.progressFraction, 0.01f) // 1600/1756 ≈ 0.91
        // 脂肪比例维度
        val fatDim = data.dimensions[1]
        assertEquals(0.75f, fatDim.progressFraction, 0.01f) // 22.5/30 = 0.75
        // 运动消耗维度
        val exDim = data.dimensions[2]
        assertEquals(0.6f, exDim.progressFraction, 0.01f) // 180/300 = 0.6
    }

    @Test
    fun `增肌目标 - 三维度完整场景`() {
        val data = GoalProgressData(
            userId = 2,
            healthGoal = "gain_muscle",
            healthGoalLabel = "增肌",
            periodDays = 14,
            overallScore = 65.0,
            overallStatus = "fair",
            dimensions = listOf(
                GoalDimension("蛋白质摄入", 70.0, "good", 95.0, 120.0, "g/天"),
                GoalDimension("热量充足", 55.0, "fair", 2200.0, 2800.0, "kcal/天"),
                GoalDimension("运动消耗", 70.0, "good", 350.0, 500.0, "kcal/天")
            )
        )

        assertEquals("gain_muscle", data.healthGoal)
        assertEquals(3, data.dimensions.size)
        val proteinDim = data.dimensions[0]
        // 95/120 ≈ 0.792
        assertEquals(0.79f, proteinDim.progressFraction, 0.01f)
    }

    // ==================== 序列化字段名snake_case验证 ====================

    @Test
    fun `GoalDimension - snake_case字段名正确映射`() {
        val json = """
        {
            "name": "热量",
            "score": 80.0,
            "status": "good",
            "current_value": 1500.0,
            "target_value": 2000.0,
            "unit": "kcal",
            "description": "描述"
        }
        """.trimIndent()
        val dim = gson.fromJson(json, GoalDimension::class.java)
        assertEquals(1500.0, dim.currentValue, 0.001)
        assertEquals(2000.0, dim.targetValue, 0.001)
    }

    @Test
    fun `GoalProgressData - snake_case字段名正确映射`() {
        val json = """
        {
            "user_id": 5,
            "health_goal": "reduce_fat",
            "health_goal_label": "减脂",
            "period_days": 7,
            "start_date": "2026-01-01",
            "end_date": "2026-01-07",
            "overall_score": 88.0,
            "overall_status": "good",
            "dimensions": [],
            "suggestions": [],
            "streak_days": 14
        }
        """.trimIndent()
        val data = gson.fromJson(json, GoalProgressData::class.java)
        assertEquals(5, data.userId)
        assertEquals("reduce_fat", data.healthGoal)
        assertEquals("减脂", data.healthGoalLabel)
        assertEquals(7, data.periodDays)
        assertEquals("2026-01-01", data.startDate)
        assertEquals("2026-01-07", data.endDate)
        assertEquals(88.0, data.overallScore, 0.001)
        assertEquals("good", data.overallStatus)
        assertEquals(14, data.streakDays)
    }

    // ==================== 辅助方法 ====================

    private fun createSampleGoalProgressData(): GoalProgressData {
        return GoalProgressData(
            userId = 1,
            healthGoal = "reduce_fat",
            healthGoalLabel = "减脂",
            periodDays = 7,
            startDate = "2026-02-01",
            endDate = "2026-02-07",
            overallScore = 75.0,
            overallStatus = "good",
            dimensions = listOf(
                GoalDimension("热量控制", 85.0, "good", 1600.0, 1756.0, "kcal/天", "日均摄入1600kcal，建议1756kcal"),
                GoalDimension("脂肪比例", 90.0, "excellent", 22.5, 30.0, "%", "脂肪占比22.5%，在建议范围内"),
                GoalDimension("运动消耗", 80.0, "good", 240.0, 300.0, "kcal/天", "日均运动消耗240kcal，建议300kcal")
            ),
            suggestions = listOf(
                "运动消耗不足，建议增加有氧运动频率和时长",
                "已连续记录7天，非常棒，继续保持！"
            ),
            streakDays = 7
        )
    }
}
