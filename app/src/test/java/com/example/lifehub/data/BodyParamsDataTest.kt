package com.example.lifehub.data

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 5 测试: 身体参数数据模型测试
 * 验证 UserPreferencesData 和 UpdatePreferencesRequest 的身体参数字段
 */
class BodyParamsDataTest {

    private val gson = Gson()

    // ==================== UserPreferencesData 测试 ====================

    @Test
    fun `test UserPreferencesData contains body params fields`() {
        // 测试包含所有身体参数的完整数据
        val jsonWithBodyParams = """
            {
                "userId": 1,
                "nickname": "测试用户",
                "healthGoal": "reduce_fat",
                "allergens": ["花生", "海鲜"],
                "travelPreference": "walking",
                "dailyBudget": 500,
                "weight": 70.5,
                "height": 175.0,
                "age": 25,
                "gender": "male"
            }
        """.trimIndent()

        val data = gson.fromJson(jsonWithBodyParams, UserPreferencesData::class.java)

        assertEquals(1, data.userId)
        assertEquals("测试用户", data.nickname)
        assertEquals("reduce_fat", data.healthGoal)
        assertEquals(listOf("花生", "海鲜"), data.allergens)
        assertEquals("walking", data.travelPreference)
        assertEquals(500, data.dailyBudget)
        
        // 身体参数字段
        assertEquals(70.5, data.weight)
        assertEquals(175.0, data.height)
        assertEquals(25, data.age)
        assertEquals("male", data.gender)
    }

    @Test
    fun `test UserPreferencesData with null body params`() {
        // 测试身体参数为null的情况
        val jsonWithoutBodyParams = """
            {
                "userId": 1,
                "nickname": "测试用户",
                "healthGoal": "balanced",
                "allergens": [],
                "travelPreference": null,
                "dailyBudget": null,
                "weight": null,
                "height": null,
                "age": null,
                "gender": null
            }
        """.trimIndent()

        val data = gson.fromJson(jsonWithoutBodyParams, UserPreferencesData::class.java)

        assertEquals(1, data.userId)
        assertNull(data.weight)
        assertNull(data.height)
        assertNull(data.age)
        assertNull(data.gender)
    }

    @Test
    fun `test UserPreferencesData female gender`() {
        val json = """
            {
                "userId": 2,
                "nickname": "女性用户",
                "weight": 55.0,
                "height": 165.0,
                "age": 28,
                "gender": "female"
            }
        """.trimIndent()

        val data = gson.fromJson(json, UserPreferencesData::class.java)

        assertEquals(55.0, data.weight)
        assertEquals(165.0, data.height)
        assertEquals(28, data.age)
        assertEquals("female", data.gender)
    }

    @Test
    fun `test UserPreferencesData other gender`() {
        val json = """
            {
                "userId": 3,
                "weight": 65.0,
                "height": 170.0,
                "age": 30,
                "gender": "other"
            }
        """.trimIndent()

        val data = gson.fromJson(json, UserPreferencesData::class.java)

        assertEquals("other", data.gender)
    }

    // ==================== UpdatePreferencesRequest 测试 ====================

    @Test
    fun `test UpdatePreferencesRequest with body params`() {
        val request = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = "gain_muscle",
            allergens = listOf("牛奶"),
            travelPreference = "self_driving",
            dailyBudget = 300,
            weight = 75.0,
            height = 180.0,
            age = 30,
            gender = "male"
        )

        val json = gson.toJson(request)

        // 验证序列化后包含所有字段
        assertTrue(json.contains("\"userId\":1"))
        assertTrue(json.contains("\"healthGoal\":\"gain_muscle\""))
        assertTrue(json.contains("\"weight\":75.0"))
        assertTrue(json.contains("\"height\":180.0"))
        assertTrue(json.contains("\"age\":30"))
        assertTrue(json.contains("\"gender\":\"male\""))
    }

    @Test
    fun `test UpdatePreferencesRequest with only body params`() {
        // 测试只更新身体参数的情况
        val request = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = 68.5,
            height = 172.0,
            age = 26,
            gender = "female"
        )

        val json = gson.toJson(request)

        // 验证身体参数存在
        assertTrue(json.contains("\"weight\":68.5"))
        assertTrue(json.contains("\"height\":172.0"))
        assertTrue(json.contains("\"age\":26"))
        assertTrue(json.contains("\"gender\":\"female\""))
    }

    @Test
    fun `test UpdatePreferencesRequest deserialization`() {
        val json = """
            {
                "userId": 5,
                "healthGoal": null,
                "allergens": null,
                "travelPreference": null,
                "dailyBudget": null,
                "weight": 80.0,
                "height": 185.0,
                "age": 35,
                "gender": "male"
            }
        """.trimIndent()

        val request = gson.fromJson(json, UpdatePreferencesRequest::class.java)

        assertEquals(5, request.userId)
        assertNull(request.healthGoal)
        assertEquals(80.0, request.weight)
        assertEquals(185.0, request.height)
        assertEquals(35, request.age)
        assertEquals("male", request.gender)
    }

    // ==================== 边界值测试 ====================

    @Test
    fun `test body params boundary values`() {
        // 测试边界值
        val jsonMinValues = """
            {
                "userId": 1,
                "weight": 0.1,
                "height": 50.0,
                "age": 1,
                "gender": "other"
            }
        """.trimIndent()

        val minData = gson.fromJson(jsonMinValues, UserPreferencesData::class.java)
        assertEquals(0.1, minData.weight)
        assertEquals(50.0, minData.height)
        assertEquals(1, minData.age)

        val jsonMaxValues = """
            {
                "userId": 1,
                "weight": 500.0,
                "height": 300.0,
                "age": 150,
                "gender": "male"
            }
        """.trimIndent()

        val maxData = gson.fromJson(jsonMaxValues, UserPreferencesData::class.java)
        assertEquals(500.0, maxData.weight)
        assertEquals(300.0, maxData.height)
        assertEquals(150, maxData.age)
    }

    @Test
    fun `test decimal weight and height values`() {
        // 测试小数精度
        val json = """
            {
                "userId": 1,
                "weight": 70.55,
                "height": 175.5
            }
        """.trimIndent()

        val data = gson.fromJson(json, UserPreferencesData::class.java)
        assertEquals(70.55, data.weight!!, 0.01)
        assertEquals(175.5, data.height!!, 0.01)
    }

    // ==================== 完整API响应测试 ====================

    @Test
    fun `test UserPreferencesResponse with body params`() {
        val json = """
            {
                "code": 200,
                "message": "获取成功",
                "data": {
                    "userId": 1,
                    "nickname": "健康达人",
                    "healthGoal": "reduce_fat",
                    "allergens": ["花生"],
                    "travelPreference": "walking",
                    "dailyBudget": 500,
                    "weight": 70.5,
                    "height": 175.0,
                    "age": 25,
                    "gender": "male"
                }
            }
        """.trimIndent()

        val response = gson.fromJson(json, UserPreferencesResponse::class.java)

        assertEquals(200, response.code)
        assertEquals("获取成功", response.message)
        assertNotNull(response.data)
        
        val data = response.data!!
        assertEquals(70.5, data.weight)
        assertEquals(175.0, data.height)
        assertEquals(25, data.age)
        assertEquals("male", data.gender)
    }
}
