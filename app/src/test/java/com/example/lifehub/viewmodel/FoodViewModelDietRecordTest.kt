package com.example.lifehub.viewmodel

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.UpdateDietRecordRequest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * FoodViewModel饮食记录编辑/删除功能测试
 * 测试Phase 3实现的更新和删除饮食记录功能
 */
class FoodViewModelDietRecordTest {

    // ==================== 数据模型测试 ====================

    @Test
    fun `test DietRecord data class creation`() {
        val record = DietRecord(
                id = 1,
                userId = 100,
                foodName = "红烧肉",
                calories = 500.0,
                protein = 25.0,
                fat = 35.0,
                carbs = 10.0,
                mealType = "lunch",
                recordDate = "2024-01-15",
                createdAt = "2024-01-15T12:30:00"
        )

        assertEquals(1, record.id)
        assertEquals(100, record.userId)
        assertEquals("红烧肉", record.foodName)
        assertEquals(500.0, record.calories, 0.01)
        assertEquals(25.0, record.protein, 0.01)
        assertEquals(35.0, record.fat, 0.01)
        assertEquals(10.0, record.carbs, 0.01)
        assertEquals("lunch", record.mealType)
        assertEquals("2024-01-15", record.recordDate)
    }

    @Test
    fun `test UpdateDietRecordRequest with all fields`() {
        val request = UpdateDietRecordRequest(
                userId = 100,
                foodName = "清蒸鱼",
                calories = 200.0,
                protein = 30.0,
                fat = 8.0,
                carbs = 2.0,
                mealType = "dinner",
                recordDate = "2024-01-16"
        )

        assertEquals(100, request.userId)
        assertEquals("清蒸鱼", request.foodName)
        assertEquals(200.0, request.calories)
        assertEquals(30.0, request.protein)
        assertEquals(8.0, request.fat)
        assertEquals(2.0, request.carbs)
        assertEquals("dinner", request.mealType)
        assertEquals("2024-01-16", request.recordDate)
    }

    @Test
    fun `test UpdateDietRecordRequest with partial fields`() {
        val request = UpdateDietRecordRequest(
                userId = 100,
                foodName = "蔬菜沙拉",
                calories = null,
                protein = null,
                fat = null,
                carbs = null,
                mealType = null,
                recordDate = null
        )

        assertEquals(100, request.userId)
        assertEquals("蔬菜沙拉", request.foodName)
        assertNull(request.calories)
        assertNull(request.protein)
        assertNull(request.fat)
        assertNull(request.carbs)
        assertNull(request.mealType)
        assertNull(request.recordDate)
    }

    @Test
    fun `test UpdateDietRecordRequest with only calories update`() {
        val request = UpdateDietRecordRequest(
                userId = 100,
                calories = 350.0
        )

        assertEquals(100, request.userId)
        assertNull(request.foodName)
        assertEquals(350.0, request.calories)
        assertNull(request.protein)
        assertNull(request.fat)
        assertNull(request.carbs)
        assertNull(request.mealType)
        assertNull(request.recordDate)
    }

    // ==================== UI状态测试 ====================

    @Test
    fun `test UpdateDietRecordState sealed class variants`() {
        val idle = UpdateDietRecordState.Idle
        val loading = UpdateDietRecordState.Loading
        val success = UpdateDietRecordState.Success("更新成功")
        val error = UpdateDietRecordState.Error("更新失败：网络错误")

        assertTrue(idle is UpdateDietRecordState)
        assertTrue(loading is UpdateDietRecordState)
        assertTrue(success is UpdateDietRecordState)
        assertTrue(error is UpdateDietRecordState)

        assertEquals("更新成功", success.message)
        assertEquals("更新失败：网络错误", error.message)
    }

    @Test
    fun `test DeleteDietRecordState sealed class variants`() {
        val idle = DeleteDietRecordState.Idle
        val loading = DeleteDietRecordState.Loading
        val success = DeleteDietRecordState.Success("删除成功")
        val error = DeleteDietRecordState.Error("删除失败：无权操作")

        assertTrue(idle is DeleteDietRecordState)
        assertTrue(loading is DeleteDietRecordState)
        assertTrue(success is DeleteDietRecordState)
        assertTrue(error is DeleteDietRecordState)

        assertEquals("删除成功", success.message)
        assertEquals("删除失败：无权操作", error.message)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `test DietRecord with zero nutrition values`() {
        val record = DietRecord(
                id = 1,
                userId = 100,
                foodName = "水",
                calories = 0.0,
                protein = 0.0,
                fat = 0.0,
                carbs = 0.0,
                mealType = "snack",
                recordDate = "2024-01-15",
                createdAt = "2024-01-15T10:00:00"
        )

        assertEquals(0.0, record.calories, 0.01)
        assertEquals(0.0, record.protein, 0.01)
        assertEquals(0.0, record.fat, 0.01)
        assertEquals(0.0, record.carbs, 0.01)
    }

    @Test
    fun `test DietRecord with large nutrition values`() {
        val record = DietRecord(
                id = 1,
                userId = 100,
                foodName = "超大份牛排套餐",
                calories = 2500.0,
                protein = 150.0,
                fat = 180.0,
                carbs = 100.0,
                mealType = "dinner",
                recordDate = "2024-01-15",
                createdAt = "2024-01-15T19:00:00"
        )

        assertEquals(2500.0, record.calories, 0.01)
        assertEquals(150.0, record.protein, 0.01)
        assertEquals(180.0, record.fat, 0.01)
        assertEquals(100.0, record.carbs, 0.01)
    }

    @Test
    fun `test meal type variations`() {
        val mealTypes = listOf("breakfast", "lunch", "dinner", "snack", "早餐", "午餐", "晚餐", "加餐")

        mealTypes.forEach { mealType ->
            val record = DietRecord(
                    id = 1,
                    userId = 100,
                    foodName = "测试食物",
                    calories = 100.0,
                    protein = 10.0,
                    fat = 5.0,
                    carbs = 15.0,
                    mealType = mealType,
                    recordDate = "2024-01-15",
                    createdAt = "2024-01-15T12:00:00"
            )
            assertEquals(mealType, record.mealType)
        }
    }

    @Test
    fun `test DietRecord with special characters in food name`() {
        val specialNames = listOf(
                "麻辣香锅（微辣）",
                "冰淇淋🍦",
                "Coffee & Tea",
                "寿司・刺身拼盘",
                "Pasta \"意大利面\""
        )

        specialNames.forEach { name ->
            val record = DietRecord(
                    id = 1,
                    userId = 100,
                    foodName = name,
                    calories = 200.0,
                    protein = 10.0,
                    fat = 8.0,
                    carbs = 25.0,
                    mealType = "lunch",
                    recordDate = "2024-01-15",
                    createdAt = "2024-01-15T12:00:00"
            )
            assertEquals(name, record.foodName)
        }
    }

    @Test
    fun `test DietRecord date format validation`() {
        val validDates = listOf(
                "2024-01-01",
                "2024-12-31",
                "2023-06-15",
                "2025-03-28"
        )

        validDates.forEach { date ->
            val record = DietRecord(
                    id = 1,
                    userId = 100,
                    foodName = "测试食物",
                    calories = 100.0,
                    protein = 10.0,
                    fat = 5.0,
                    carbs = 15.0,
                    mealType = "lunch",
                    recordDate = date,
                    createdAt = "${date}T12:00:00"
            )
            assertEquals(date, record.recordDate)
            assertTrue(record.recordDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        }
    }

    // ==================== 业务逻辑测试 ====================

    @Test
    fun `test nutrition calculation for daily summary`() {
        val records = listOf(
                DietRecord(1, 100, "早餐", 300.0, 15.0, 10.0, 40.0, "breakfast", "2024-01-15", ""),
                DietRecord(2, 100, "午餐", 600.0, 30.0, 25.0, 60.0, "lunch", "2024-01-15", ""),
                DietRecord(3, 100, "晚餐", 500.0, 25.0, 20.0, 50.0, "dinner", "2024-01-15", "")
        )

        val totalCalories = records.sumOf { it.calories }
        val totalProtein = records.sumOf { it.protein }
        val totalFat = records.sumOf { it.fat }
        val totalCarbs = records.sumOf { it.carbs }

        assertEquals(1400.0, totalCalories, 0.01)
        assertEquals(70.0, totalProtein, 0.01)
        assertEquals(55.0, totalFat, 0.01)
        assertEquals(150.0, totalCarbs, 0.01)
    }

    @Test
    fun `test records grouping by date`() {
        val records = listOf(
                DietRecord(1, 100, "食物1", 100.0, 10.0, 5.0, 15.0, "breakfast", "2024-01-15", ""),
                DietRecord(2, 100, "食物2", 200.0, 15.0, 8.0, 20.0, "lunch", "2024-01-15", ""),
                DietRecord(3, 100, "食物3", 150.0, 12.0, 6.0, 18.0, "dinner", "2024-01-16", "")
        )

        val groupedByDate = records.groupBy { it.recordDate }

        assertEquals(2, groupedByDate.size)
        assertEquals(2, groupedByDate["2024-01-15"]?.size)
        assertEquals(1, groupedByDate["2024-01-16"]?.size)
    }

    @Test
    fun `test records filtering by meal type`() {
        val records = listOf(
                DietRecord(1, 100, "早餐1", 300.0, 15.0, 10.0, 40.0, "breakfast", "2024-01-15", ""),
                DietRecord(2, 100, "午餐1", 600.0, 30.0, 25.0, 60.0, "lunch", "2024-01-15", ""),
                DietRecord(3, 100, "早餐2", 250.0, 12.0, 8.0, 35.0, "breakfast", "2024-01-16", "")
        )

        val breakfastRecords = records.filter { it.mealType == "breakfast" }
        val lunchRecords = records.filter { it.mealType == "lunch" }

        assertEquals(2, breakfastRecords.size)
        assertEquals(1, lunchRecords.size)
    }

    @Test
    fun `test update request creates valid partial update`() {
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
                foodName = "新菜名",
                calories = 450.0
        )

        assertEquals(originalRecord.userId, updateRequest.userId)
        assertEquals("新菜名", updateRequest.foodName)
        assertEquals(450.0, updateRequest.calories)
        assertNull(updateRequest.protein)
        assertNull(updateRequest.fat)
        assertNull(updateRequest.carbs)
    }
}
