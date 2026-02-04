package com.example.lifehub.data

import org.junit.Assert.*
import org.junit.Test

/**
 * 饮食记录数据模型测试
 * 测试Phase 3相关的数据类序列化和验证
 */
class DietRecordDataTest {

    // ==================== UpdateDietRecordRequest测试 ====================

    @Test
    fun `test UpdateDietRecordRequest full construction`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                foodName = "红烧排骨",
                calories = 450.0,
                protein = 35.0,
                fat = 25.0,
                carbs = 15.0,
                mealType = "dinner",
                recordDate = "2024-01-20"
        )

        assertEquals(1, request.userId)
        assertEquals("红烧排骨", request.foodName)
        assertEquals(450.0, request.calories!!, 0.01)
        assertEquals(35.0, request.protein!!, 0.01)
        assertEquals(25.0, request.fat!!, 0.01)
        assertEquals(15.0, request.carbs!!, 0.01)
        assertEquals("dinner", request.mealType)
        assertEquals("2024-01-20", request.recordDate)
    }

    @Test
    fun `test UpdateDietRecordRequest minimal construction`() {
        val request = UpdateDietRecordRequest(userId = 1)

        assertEquals(1, request.userId)
        assertNull(request.foodName)
        assertNull(request.calories)
        assertNull(request.protein)
        assertNull(request.fat)
        assertNull(request.carbs)
        assertNull(request.mealType)
        assertNull(request.recordDate)
    }

    @Test
    fun `test UpdateDietRecordRequest partial update - only food name`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                foodName = "新菜名"
        )

        assertEquals(1, request.userId)
        assertEquals("新菜名", request.foodName)
        assertNull(request.calories)
        assertNull(request.protein)
    }

    @Test
    fun `test UpdateDietRecordRequest partial update - only nutrition`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                calories = 300.0,
                protein = 20.0,
                fat = 10.0,
                carbs = 35.0
        )

        assertEquals(1, request.userId)
        assertNull(request.foodName)
        assertEquals(300.0, request.calories!!, 0.01)
        assertEquals(20.0, request.protein!!, 0.01)
        assertEquals(10.0, request.fat!!, 0.01)
        assertEquals(35.0, request.carbs!!, 0.01)
    }

    @Test
    fun `test UpdateDietRecordRequest partial update - only meal type`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                mealType = "breakfast"
        )

        assertEquals(1, request.userId)
        assertEquals("breakfast", request.mealType)
        assertNull(request.foodName)
        assertNull(request.calories)
    }

    // ==================== UpdateDietRecordResponse测试 ====================

    @Test
    fun `test UpdateDietRecordResponse success`() {
        val data = DietRecordData(
                id = 1,
                foodName = "更新后的菜名",
                calories = 400.0,
                protein = 30.0,
                fat = 18.0,
                carbs = 40.0,
                mealType = "lunch",
                recordDate = "2024-01-20"
        )

        val response = UpdateDietRecordResponse(
                code = 200,
                message = "更新成功",
                data = data
        )

        assertEquals(200, response.code)
        assertEquals("更新成功", response.message)
        assertNotNull(response.data)
        assertEquals("更新后的菜名", response.data!!.foodName)
        assertEquals(400.0, response.data!!.calories, 0.01)
    }

    @Test
    fun `test UpdateDietRecordResponse failure`() {
        val response = UpdateDietRecordResponse(
                code = 404,
                message = "记录不存在",
                data = null
        )

        assertEquals(404, response.code)
        assertEquals("记录不存在", response.message)
        assertNull(response.data)
    }

    @Test
    fun `test UpdateDietRecordResponse permission denied`() {
        val response = UpdateDietRecordResponse(
                code = 403,
                message = "无权操作此记录",
                data = null
        )

        assertEquals(403, response.code)
        assertEquals("无权操作此记录", response.message)
        assertNull(response.data)
    }

    // ==================== DietRecordData测试 ====================

    @Test
    fun `test DietRecordData complete construction`() {
        val data = DietRecordData(
                id = 123,
                foodName = "宫保鸡丁",
                calories = 380.0,
                protein = 28.0,
                fat = 22.0,
                carbs = 18.0,
                mealType = "lunch",
                recordDate = "2024-01-20"
        )

        assertEquals(123, data.id)
        assertEquals("宫保鸡丁", data.foodName)
        assertEquals(380.0, data.calories, 0.01)
        assertEquals(28.0, data.protein, 0.01)
        assertEquals(22.0, data.fat, 0.01)
        assertEquals(18.0, data.carbs, 0.01)
        assertEquals("lunch", data.mealType)
        assertEquals("2024-01-20", data.recordDate)
    }

    // ==================== ApiResponse测试 ====================

    @Test
    fun `test ApiResponse delete success`() {
        val response = ApiResponse(
                code = 200,
                message = "删除成功",
                data = null
        )

        assertEquals(200, response.code)
        assertEquals("删除成功", response.message)
        assertNull(response.data)
    }

    @Test
    fun `test ApiResponse delete failure - not found`() {
        val response = ApiResponse(
                code = 404,
                message = "记录不存在",
                data = null
        )

        assertEquals(404, response.code)
        assertEquals("记录不存在", response.message)
    }

    @Test
    fun `test ApiResponse delete failure - permission denied`() {
        val response = ApiResponse(
                code = 403,
                message = "无权操作此记录",
                data = null
        )

        assertEquals(403, response.code)
        assertEquals("无权操作此记录", response.message)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `test UpdateDietRecordRequest with zero values`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                calories = 0.0,
                protein = 0.0,
                fat = 0.0,
                carbs = 0.0
        )

        assertEquals(0.0, request.calories!!, 0.01)
        assertEquals(0.0, request.protein!!, 0.01)
        assertEquals(0.0, request.fat!!, 0.01)
        assertEquals(0.0, request.carbs!!, 0.01)
    }

    @Test
    fun `test UpdateDietRecordRequest with large values`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                calories = 5000.0,
                protein = 300.0,
                fat = 250.0,
                carbs = 600.0
        )

        assertEquals(5000.0, request.calories!!, 0.01)
        assertEquals(300.0, request.protein!!, 0.01)
        assertEquals(250.0, request.fat!!, 0.01)
        assertEquals(600.0, request.carbs!!, 0.01)
    }

    @Test
    fun `test UpdateDietRecordRequest with empty food name`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                foodName = ""
        )

        assertEquals("", request.foodName)
    }

    @Test
    fun `test UpdateDietRecordRequest with special characters in food name`() {
        val specialNames = listOf(
                "麻辣火锅（特辣）",
                "Sushi 🍣",
                "Pizza \"Margherita\"",
                "Café au lait",
                "汉堡\t薯条"
        )

        specialNames.forEach { name ->
            val request = UpdateDietRecordRequest(userId = 1, foodName = name)
            assertEquals(name, request.foodName)
        }
    }

    @Test
    fun `test all meal type values`() {
        val mealTypes = listOf("breakfast", "lunch", "dinner", "snack")

        mealTypes.forEach { mealType ->
            val request = UpdateDietRecordRequest(userId = 1, mealType = mealType)
            assertEquals(mealType, request.mealType)
        }
    }

    @Test
    fun `test Chinese meal type values`() {
        val mealTypes = listOf("早餐", "午餐", "晚餐", "加餐")

        mealTypes.forEach { mealType ->
            val request = UpdateDietRecordRequest(userId = 1, mealType = mealType)
            assertEquals(mealType, request.mealType)
        }
    }

    // ==================== 业务逻辑测试 ====================

    @Test
    fun `test create update request from existing record`() {
        val originalRecord = DietRecord(
                id = 1,
                userId = 100,
                foodName = "原始菜名",
                calories = 500.0,
                protein = 25.0,
                fat = 20.0,
                carbs = 45.0,
                mealType = "lunch",
                recordDate = "2024-01-15",
                createdAt = "2024-01-15T12:00:00"
        )

        val updateRequest = UpdateDietRecordRequest(
                userId = originalRecord.userId,
                foodName = "修改后的菜名",
                calories = originalRecord.calories + 50,
                protein = originalRecord.protein,
                fat = originalRecord.fat,
                carbs = originalRecord.carbs,
                mealType = originalRecord.mealType
        )

        assertEquals(originalRecord.userId, updateRequest.userId)
        assertEquals("修改后的菜名", updateRequest.foodName)
        assertEquals(550.0, updateRequest.calories!!, 0.01)
        assertEquals(originalRecord.protein, updateRequest.protein!!, 0.01)
    }

    @Test
    fun `test validate nutrition values are non-negative`() {
        val request = UpdateDietRecordRequest(
                userId = 1,
                calories = 100.0,
                protein = 10.0,
                fat = 5.0,
                carbs = 15.0
        )

        assertTrue(request.calories!! >= 0)
        assertTrue(request.protein!! >= 0)
        assertTrue(request.fat!! >= 0)
        assertTrue(request.carbs!! >= 0)
    }

    @Test
    fun `test DietRecordData equals and hashCode`() {
        val data1 = DietRecordData(
                id = 1,
                foodName = "测试菜品",
                calories = 300.0,
                protein = 20.0,
                fat = 15.0,
                carbs = 25.0,
                mealType = "lunch",
                recordDate = "2024-01-20"
        )

        val data2 = DietRecordData(
                id = 1,
                foodName = "测试菜品",
                calories = 300.0,
                protein = 20.0,
                fat = 15.0,
                carbs = 25.0,
                mealType = "lunch",
                recordDate = "2024-01-20"
        )

        assertEquals(data1, data2)
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun `test DietRecordData not equals with different id`() {
        val data1 = DietRecordData(1, "菜品", 300.0, 20.0, 15.0, 25.0, "lunch", "2024-01-20")
        val data2 = DietRecordData(2, "菜品", 300.0, 20.0, 15.0, 25.0, "lunch", "2024-01-20")

        assertNotEquals(data1, data2)
    }
}
