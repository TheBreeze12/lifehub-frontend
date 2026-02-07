package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.viewmodel.ForgetDataState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 55: 一键"遗忘"功能 - 前端单元测试
 *
 * 测试内容：
 * 1. DataForgetResponse 数据模型序列化/反序列化
 * 2. DeletedCounts 各字段映射
 * 3. ForgetDataState 状态机逻辑
 * 4. 边界情况：空数据、大数据、零计数
 */
class Phase55DataForgetTest {

    private val gson = Gson()

    // ============================================================
    // DataForgetResponse 模型测试
    // ============================================================

    @Test
    fun `test DataForgetResponse deserialization success`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 123,
                "nickname": "测试用户",
                "deleted_counts": {
                    "diet_records": 15,
                    "exercise_records": 8,
                    "meal_comparisons": 3,
                    "menu_recognitions": 5,
                    "trip_plans": 4
                },
                "total_deleted": 35
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)

        assertEquals(200, response.code)
        assertEquals("数据删除成功", response.message)
        assertNotNull(response.data)
        assertEquals(123, response.data!!.userId)
        assertEquals("测试用户", response.data!!.nickname)
        assertEquals(35, response.data!!.totalDeleted)

        val counts = response.data!!.deletedCounts
        assertNotNull(counts)
        assertEquals(15, counts!!.dietRecords)
        assertEquals(8, counts.exerciseRecords)
        assertEquals(3, counts.mealComparisons)
        assertEquals(5, counts.menuRecognitions)
        assertEquals(4, counts.tripPlans)
    }

    @Test
    fun `test DataForgetResponse deserialization with zero counts`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 456,
                "nickname": "空用户",
                "deleted_counts": {
                    "diet_records": 0,
                    "exercise_records": 0,
                    "meal_comparisons": 0,
                    "menu_recognitions": 0,
                    "trip_plans": 0
                },
                "total_deleted": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)

        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals(0, response.data!!.totalDeleted)
        assertEquals(0, response.data!!.deletedCounts!!.dietRecords)
        assertEquals(0, response.data!!.deletedCounts!!.exerciseRecords)
    }

    @Test
    fun `test DataForgetResponse deserialization with null data`() {
        val json = """
        {
            "code": 404,
            "message": "用户不存在",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)

        assertEquals(404, response.code)
        assertEquals("用户不存在", response.message)
        assertNull(response.data)
    }

    @Test
    fun `test DataForgetResponse deserialization with large counts`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 1,
                "nickname": "大数据用户",
                "deleted_counts": {
                    "diet_records": 10000,
                    "exercise_records": 5000,
                    "meal_comparisons": 3000,
                    "menu_recognitions": 2000,
                    "trip_plans": 1000
                },
                "total_deleted": 21000
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)

        assertEquals(200, response.code)
        assertEquals(21000, response.data!!.totalDeleted)
        assertEquals(10000, response.data!!.deletedCounts!!.dietRecords)
    }

    // ============================================================
    // DeletedCounts 模型测试
    // ============================================================

    @Test
    fun `test DeletedCounts default values`() {
        val counts = DeletedCounts()
        assertEquals(0, counts.dietRecords)
        assertEquals(0, counts.exerciseRecords)
        assertEquals(0, counts.mealComparisons)
        assertEquals(0, counts.menuRecognitions)
        assertEquals(0, counts.tripPlans)
    }

    @Test
    fun `test DeletedCounts with specific values`() {
        val counts = DeletedCounts(
                dietRecords = 10,
                exerciseRecords = 5,
                mealComparisons = 3,
                menuRecognitions = 2,
                tripPlans = 7
        )
        assertEquals(10, counts.dietRecords)
        assertEquals(5, counts.exerciseRecords)
        assertEquals(3, counts.mealComparisons)
        assertEquals(2, counts.menuRecognitions)
        assertEquals(7, counts.tripPlans)
    }

    @Test
    fun `test DeletedCounts serialization roundtrip`() {
        val counts = DeletedCounts(
                dietRecords = 15,
                exerciseRecords = 8,
                mealComparisons = 3,
                menuRecognitions = 5,
                tripPlans = 4
        )
        val json = gson.toJson(counts)
        val restored = gson.fromJson(json, DeletedCounts::class.java)
        assertEquals(counts.dietRecords, restored.dietRecords)
        assertEquals(counts.exerciseRecords, restored.exerciseRecords)
        assertEquals(counts.mealComparisons, restored.mealComparisons)
        assertEquals(counts.menuRecognitions, restored.menuRecognitions)
        assertEquals(counts.tripPlans, restored.tripPlans)
    }

    // ============================================================
    // DataForgetData 模型测试
    // ============================================================

    @Test
    fun `test DataForgetData creation`() {
        val counts = DeletedCounts(dietRecords = 5, exerciseRecords = 3)
        val data = DataForgetData(
                userId = 42,
                nickname = "测试昵称",
                deletedCounts = counts,
                totalDeleted = 8
        )
        assertEquals(42, data.userId)
        assertEquals("测试昵称", data.nickname)
        assertEquals(8, data.totalDeleted)
        assertNotNull(data.deletedCounts)
    }

    @Test
    fun `test DataForgetData with null nickname`() {
        val data = DataForgetData(
                userId = 1,
                nickname = null,
                deletedCounts = DeletedCounts(),
                totalDeleted = 0
        )
        assertNull(data.nickname)
    }

    @Test
    fun `test DataForgetData with null deletedCounts`() {
        val data = DataForgetData(
                userId = 1,
                nickname = "测试",
                deletedCounts = null,
                totalDeleted = 0
        )
        assertNull(data.deletedCounts)
    }

    // ============================================================
    // ForgetDataState 状态测试
    // ============================================================

    @Test
    fun `test ForgetDataState Idle`() {
        val state: ForgetDataState = ForgetDataState.Idle
        assertTrue(state is ForgetDataState.Idle)
        assertFalse(state is ForgetDataState.Loading)
        assertFalse(state is ForgetDataState.Success)
        assertFalse(state is ForgetDataState.Error)
    }

    @Test
    fun `test ForgetDataState Loading`() {
        val state: ForgetDataState = ForgetDataState.Loading
        assertTrue(state is ForgetDataState.Loading)
    }

    @Test
    fun `test ForgetDataState Success with data`() {
        val state: ForgetDataState = ForgetDataState.Success(
                totalDeleted = 35,
                message = "数据删除成功"
        )
        assertTrue(state is ForgetDataState.Success)
        val success = state as ForgetDataState.Success
        assertEquals(35, success.totalDeleted)
        assertEquals("数据删除成功", success.message)
    }

    @Test
    fun `test ForgetDataState Success with zero deleted`() {
        val state = ForgetDataState.Success(totalDeleted = 0, message = "数据删除成功")
        assertEquals(0, state.totalDeleted)
    }

    @Test
    fun `test ForgetDataState Error with message`() {
        val state: ForgetDataState = ForgetDataState.Error("网络请求失败")
        assertTrue(state is ForgetDataState.Error)
        val error = state as ForgetDataState.Error
        assertEquals("网络请求失败", error.message)
    }

    @Test
    fun `test ForgetDataState Error with empty message`() {
        val state = ForgetDataState.Error("")
        assertEquals("", state.message)
    }

    // ============================================================
    // JSON边界测试
    // ============================================================

    @Test
    fun `test response with missing optional fields`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 1,
                "nickname": "用户",
                "total_deleted": 5
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals(5, response.data!!.totalDeleted)
        // deleted_counts missing should be null
        assertNull(response.data!!.deletedCounts)
    }

    @Test
    fun `test DataForgetResponse serialization`() {
        val response = DataForgetResponse(
                code = 200,
                message = "数据删除成功",
                data = DataForgetData(
                        userId = 1,
                        nickname = "测试",
                        deletedCounts = DeletedCounts(dietRecords = 5),
                        totalDeleted = 5
                )
        )
        val json = gson.toJson(response)
        assertTrue(json.contains("\"code\":200"))
        assertTrue(json.contains("数据删除成功"))
    }

    @Test
    fun `test total_deleted matches sum of counts`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 1,
                "nickname": "用户",
                "deleted_counts": {
                    "diet_records": 10,
                    "exercise_records": 5,
                    "meal_comparisons": 3,
                    "menu_recognitions": 2,
                    "trip_plans": 1
                },
                "total_deleted": 21
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)
        val counts = response.data!!.deletedCounts!!
        val sum = counts.dietRecords + counts.exerciseRecords +
                counts.mealComparisons + counts.menuRecognitions + counts.tripPlans
        assertEquals(response.data!!.totalDeleted, sum)
    }
}
