package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.viewmodel.AiCallLogState
import com.example.lifehub.viewmodel.AiCallLogStatsState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 56: AI调用记录/日志查看 - 前端单元测试
 *
 * 测试内容：
 * 1. AiCallLogItem 数据模型序列化/反序列化
 * 2. AiCallLogResponse 完整响应解析
 * 3. AiCallLogStatsResponse 统计响应解析
 * 4. AiCallTypeLabels 标签映射
 * 5. AiCallLogState 状态机逻辑
 * 6. 边界情况：空数据、可选字段、特殊字符
 */
class Phase56AiCallLogTest {

    private val gson = Gson()

    // ============================================================
    // 1. AiCallLogItem 模型测试
    // ============================================================

    @Test
    fun `test AiCallLogItem deserialization success`() {
        val json = """
        {
            "id": 1,
            "user_id": 123,
            "call_type": "food_analysis",
            "model_name": "doubao-seed-1-6-251015",
            "input_summary": "番茄炒蛋",
            "output_summary": "calories=150.0, protein=10.5",
            "success": true,
            "error_message": null,
            "latency_ms": 1200,
            "token_usage": 350,
            "created_at": "2026-02-07 20:00:00"
        }
        """.trimIndent()

        val item = gson.fromJson(json, AiCallLogItem::class.java)

        assertEquals(1, item.id)
        assertEquals(123, item.userId)
        assertEquals("food_analysis", item.callType)
        assertEquals("doubao-seed-1-6-251015", item.modelName)
        assertEquals("番茄炒蛋", item.inputSummary)
        assertEquals("calories=150.0, protein=10.5", item.outputSummary)
        assertTrue(item.success)
        assertNull(item.errorMessage)
        assertEquals(1200, item.latencyMs)
        assertEquals(350, item.tokenUsage)
        assertEquals("2026-02-07 20:00:00", item.createdAt)
    }

    @Test
    fun `test AiCallLogItem with null optional fields`() {
        val json = """
        {
            "id": 2,
            "user_id": null,
            "call_type": "menu_recognition",
            "model_name": "doubao-seed-1-6-251015",
            "input_summary": null,
            "output_summary": null,
            "success": false,
            "error_message": "API调用超时",
            "latency_ms": null,
            "token_usage": null,
            "created_at": null
        }
        """.trimIndent()

        val item = gson.fromJson(json, AiCallLogItem::class.java)

        assertEquals(2, item.id)
        assertNull(item.userId)
        assertEquals("menu_recognition", item.callType)
        assertFalse(item.success)
        assertEquals("API调用超时", item.errorMessage)
        assertNull(item.latencyMs)
        assertNull(item.tokenUsage)
        assertNull(item.createdAt)
    }

    @Test
    fun `test AiCallLogItem with special characters in input`() {
        val json = """
        {
            "id": 3,
            "user_id": 1,
            "call_type": "food_analysis",
            "model_name": "doubao",
            "input_summary": "测试\"菜品'<script>",
            "output_summary": null,
            "success": true,
            "error_message": null,
            "latency_ms": 100,
            "token_usage": null,
            "created_at": "2026-02-07 20:00:00"
        }
        """.trimIndent()

        val item = gson.fromJson(json, AiCallLogItem::class.java)
        assertNotNull(item.inputSummary)
        assertTrue(item.inputSummary!!.contains("测试"))
    }

    // ============================================================
    // 2. AiCallLogResponse 完整响应测试
    // ============================================================

    @Test
    fun `test AiCallLogResponse deserialization with logs`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "total": 2,
                "logs": [
                    {
                        "id": 1,
                        "user_id": 1,
                        "call_type": "food_analysis",
                        "model_name": "doubao-seed-1-6-251015",
                        "input_summary": "番茄炒蛋",
                        "output_summary": "calories=150",
                        "success": true,
                        "error_message": null,
                        "latency_ms": 1200,
                        "token_usage": 350,
                        "created_at": "2026-02-07 20:00:00"
                    },
                    {
                        "id": 2,
                        "user_id": 1,
                        "call_type": "trip_generation",
                        "model_name": "qwen-turbo",
                        "input_summary": "餐后散步",
                        "output_summary": "title=运动计划",
                        "success": true,
                        "error_message": null,
                        "latency_ms": 2000,
                        "token_usage": 500,
                        "created_at": "2026-02-07 19:00:00"
                    }
                ]
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogResponse::class.java)

        assertEquals(200, response.code)
        assertEquals("获取成功", response.message)
        assertNotNull(response.data)
        assertEquals(2, response.data!!.total)
        assertEquals(2, response.data!!.logs.size)
        assertEquals("food_analysis", response.data!!.logs[0].callType)
        assertEquals("trip_generation", response.data!!.logs[1].callType)
    }

    @Test
    fun `test AiCallLogResponse deserialization with empty logs`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "total": 0,
                "logs": []
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogResponse::class.java)

        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals(0, response.data!!.total)
        assertTrue(response.data!!.logs.isEmpty())
    }

    @Test
    fun `test AiCallLogResponse deserialization with null data`() {
        val json = """
        {
            "code": 500,
            "message": "服务器错误",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogResponse::class.java)

        assertEquals(500, response.code)
        assertNull(response.data)
    }

    // ============================================================
    // 3. AiCallLogStatsResponse 统计响应测试
    // ============================================================

    @Test
    fun `test AiCallLogStatsResponse deserialization`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "total_calls": 100,
                "success_count": 95,
                "failure_count": 5,
                "success_rate": 0.95,
                "avg_latency_ms": 1500.5,
                "call_type_distribution": {
                    "food_analysis": 50,
                    "trip_generation": 30,
                    "menu_recognition": 20
                },
                "model_distribution": {
                    "doubao-seed-1-6-251015": 70,
                    "qwen-turbo": 30
                },
                "recent_7days_count": 42
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogStatsResponse::class.java)

        assertEquals(200, response.code)
        assertNotNull(response.data)

        val stats = response.data!!
        assertEquals(100, stats.totalCalls)
        assertEquals(95, stats.successCount)
        assertEquals(5, stats.failureCount)
        assertEquals(0.95, stats.successRate, 0.001)
        assertEquals(1500.5, stats.avgLatencyMs, 0.1)
        assertEquals(42, stats.recent7daysCount)

        assertNotNull(stats.callTypeDistribution)
        assertEquals(50, stats.callTypeDistribution!!["food_analysis"])
        assertEquals(30, stats.callTypeDistribution!!["trip_generation"])

        assertNotNull(stats.modelDistribution)
        assertEquals(70, stats.modelDistribution!!["doubao-seed-1-6-251015"])
    }

    @Test
    fun `test AiCallLogStatsResponse with zero stats`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "total_calls": 0,
                "success_count": 0,
                "failure_count": 0,
                "success_rate": 0.0,
                "avg_latency_ms": 0.0,
                "call_type_distribution": {},
                "model_distribution": {},
                "recent_7days_count": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogStatsResponse::class.java)

        assertNotNull(response.data)
        assertEquals(0, response.data!!.totalCalls)
        assertEquals(0.0, response.data!!.successRate, 0.001)
        assertTrue(response.data!!.callTypeDistribution!!.isEmpty())
    }

    // ============================================================
    // 4. AiCallTypeLabels 标签映射测试
    // ============================================================

    @Test
    fun `test AiCallTypeLabels known types`() {
        assertEquals("菜品营养分析", AiCallTypeLabels.getLabel("food_analysis"))
        assertEquals("菜单图片识别", AiCallTypeLabels.getLabel("menu_recognition"))
        assertEquals("运动计划生成", AiCallTypeLabels.getLabel("trip_generation"))
        assertEquals("运动意图提取", AiCallTypeLabels.getLabel("exercise_intent"))
        assertEquals("过敏原检测", AiCallTypeLabels.getLabel("allergen_check"))
        assertEquals("餐前餐后对比", AiCallTypeLabels.getLabel("meal_comparison"))
    }

    @Test
    fun `test AiCallTypeLabels unknown type returns raw value`() {
        assertEquals("unknown_type", AiCallTypeLabels.getLabel("unknown_type"))
        assertEquals("", AiCallTypeLabels.getLabel(""))
    }

    @Test
    fun `test AiCallTypeLabels labels map has all entries`() {
        assertEquals(6, AiCallTypeLabels.labels.size)
    }

    // ============================================================
    // 5. AiCallLogState 状态机测试
    // ============================================================

    @Test
    fun `test AiCallLogState Idle`() {
        val state: AiCallLogState = AiCallLogState.Idle
        assertTrue(state is AiCallLogState.Idle)
    }

    @Test
    fun `test AiCallLogState Loading`() {
        val state: AiCallLogState = AiCallLogState.Loading
        assertTrue(state is AiCallLogState.Loading)
    }

    @Test
    fun `test AiCallLogState Success`() {
        val data = AiCallLogListData(total = 1, logs = listOf(
                AiCallLogItem(
                        id = 1,
                        userId = 1,
                        callType = "food_analysis",
                        modelName = "doubao",
                        inputSummary = "test",
                        outputSummary = null,
                        success = true,
                        errorMessage = null,
                        latencyMs = 100,
                        tokenUsage = null,
                        createdAt = "2026-02-07 20:00:00"
                )
        ))
        val state: AiCallLogState = AiCallLogState.Success(data)
        assertTrue(state is AiCallLogState.Success)
        assertEquals(1, (state as AiCallLogState.Success).data.total)
        assertEquals(1, state.data.logs.size)
    }

    @Test
    fun `test AiCallLogState Error`() {
        val state: AiCallLogState = AiCallLogState.Error("网络请求失败")
        assertTrue(state is AiCallLogState.Error)
        assertEquals("网络请求失败", (state as AiCallLogState.Error).message)
    }

    // ============================================================
    // 6. AiCallLogStatsState 状态机测试
    // ============================================================

    @Test
    fun `test AiCallLogStatsState Success`() {
        val data = AiCallLogStatsData(
                totalCalls = 50,
                successCount = 48,
                failureCount = 2,
                successRate = 0.96,
                avgLatencyMs = 1200.0,
                callTypeDistribution = mapOf("food_analysis" to 30, "trip_generation" to 20),
                modelDistribution = mapOf("doubao" to 30, "qwen-turbo" to 20),
                recent7daysCount = 15
        )
        val state: AiCallLogStatsState = AiCallLogStatsState.Success(data)
        assertTrue(state is AiCallLogStatsState.Success)
        assertEquals(50, (state as AiCallLogStatsState.Success).data.totalCalls)
    }

    @Test
    fun `test AiCallLogStatsState Error`() {
        val state: AiCallLogStatsState = AiCallLogStatsState.Error("获取统计失败")
        assertTrue(state is AiCallLogStatsState.Error)
        assertEquals("获取统计失败", (state as AiCallLogStatsState.Error).message)
    }

    // ============================================================
    // 7. 边界情况测试
    // ============================================================

    @Test
    fun `test large log list deserialization`() {
        val logsJson = (1..100).joinToString(",") { i ->
            """
            {
                "id": $i,
                "user_id": 1,
                "call_type": "food_analysis",
                "model_name": "doubao",
                "input_summary": "菜品$i",
                "output_summary": null,
                "success": true,
                "error_message": null,
                "latency_ms": ${100 + i},
                "token_usage": null,
                "created_at": "2026-02-07 20:00:00"
            }
            """
        }

        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "total": 100,
                "logs": [$logsJson]
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, AiCallLogResponse::class.java)

        assertEquals(100, response.data!!.total)
        assertEquals(100, response.data!!.logs.size)
        assertEquals("菜品1", response.data!!.logs[0].inputSummary)
        assertEquals("菜品100", response.data!!.logs[99].inputSummary)
    }

    @Test
    fun `test failed log item with error message`() {
        val json = """
        {
            "id": 1,
            "user_id": 1,
            "call_type": "food_analysis",
            "model_name": "doubao",
            "input_summary": "无效菜品",
            "output_summary": null,
            "success": false,
            "error_message": "API调用超时: Connection timeout after 30s",
            "latency_ms": 30000,
            "token_usage": null,
            "created_at": "2026-02-07 20:00:00"
        }
        """.trimIndent()

        val item = gson.fromJson(json, AiCallLogItem::class.java)

        assertFalse(item.success)
        assertNotNull(item.errorMessage)
        assertTrue(item.errorMessage!!.contains("超时"))
        assertEquals(30000, item.latencyMs)
    }

    @Test
    fun `test serialization roundtrip`() {
        val original = AiCallLogItem(
                id = 1,
                userId = 1,
                callType = "food_analysis",
                modelName = "doubao-seed-1-6-251015",
                inputSummary = "番茄炒蛋",
                outputSummary = "calories=150",
                success = true,
                errorMessage = null,
                latencyMs = 1200,
                tokenUsage = 350,
                createdAt = "2026-02-07 20:00:00"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, AiCallLogItem::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.callType, deserialized.callType)
        assertEquals(original.modelName, deserialized.modelName)
        assertEquals(original.success, deserialized.success)
    }
}
