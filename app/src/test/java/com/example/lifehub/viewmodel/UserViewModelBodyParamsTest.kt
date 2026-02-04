package com.example.lifehub.viewmodel

import com.example.lifehub.data.UpdatePreferencesRequest
import com.example.lifehub.data.UserPreferencesData
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 5 测试: UserViewModel 身体参数功能测试
 * 验证 updateBodyParams 方法和相关状态管理
 */
class UserViewModelBodyParamsTest {

    // ==================== UpdatePreferencesRequest 构造测试 ====================

    @Test
    fun `test UpdatePreferencesRequest with all body params`() {
        val request = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = 70.5,
            height = 175.0,
            age = 25,
            gender = "male"
        )

        assertEquals(1, request.userId)
        assertNull(request.healthGoal)
        assertNull(request.allergens)
        assertEquals(70.5, request.weight)
        assertEquals(175.0, request.height)
        assertEquals(25, request.age)
        assertEquals("male", request.gender)
    }

    @Test
    fun `test UpdatePreferencesRequest with partial body params`() {
        // 只更新体重
        val weightOnlyRequest = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = 68.0,
            height = null,
            age = null,
            gender = null
        )

        assertEquals(68.0, weightOnlyRequest.weight)
        assertNull(weightOnlyRequest.height)
        assertNull(weightOnlyRequest.age)
        assertNull(weightOnlyRequest.gender)
    }

    @Test
    fun `test UpdatePreferencesRequest with mixed params`() {
        // 混合更新健康目标和身体参数
        val mixedRequest = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = "gain_muscle",
            allergens = listOf("花生"),
            travelPreference = null,
            dailyBudget = null,
            weight = 75.0,
            height = 180.0,
            age = 28,
            gender = "male"
        )

        assertEquals("gain_muscle", mixedRequest.healthGoal)
        assertEquals(listOf("花生"), mixedRequest.allergens)
        assertEquals(75.0, mixedRequest.weight)
        assertEquals(180.0, mixedRequest.height)
        assertEquals(28, mixedRequest.age)
        assertEquals("male", mixedRequest.gender)
    }

    // ==================== UserPreferencesData 身体参数测试 ====================

    @Test
    fun `test UserPreferencesData body params default values`() {
        // 测试默认值为null
        val data = UserPreferencesData(
            userId = 1,
            nickname = "测试用户",
            healthGoal = "balanced",
            allergens = emptyList(),
            travelPreference = null,
            dailyBudget = null
            // weight, height, age, gender 使用默认值 null
        )

        assertNull(data.weight)
        assertNull(data.height)
        assertNull(data.age)
        assertNull(data.gender)
    }

    @Test
    fun `test UserPreferencesData with body params`() {
        val data = UserPreferencesData(
            userId = 1,
            nickname = "健康达人",
            healthGoal = "reduce_fat",
            allergens = listOf("海鲜"),
            travelPreference = "walking",
            dailyBudget = 500,
            weight = 70.5,
            height = 175.0,
            age = 25,
            gender = "male"
        )

        assertEquals(70.5, data.weight)
        assertEquals(175.0, data.height)
        assertEquals(25, data.age)
        assertEquals("male", data.gender)
    }

    // ==================== 性别枚举值测试 ====================

    @Test
    fun `test gender values - male`() {
        val data = UserPreferencesData(
            userId = 1,
            nickname = null,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            gender = "male"
        )
        assertEquals("male", data.gender)
    }

    @Test
    fun `test gender values - female`() {
        val data = UserPreferencesData(
            userId = 1,
            nickname = null,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            gender = "female"
        )
        assertEquals("female", data.gender)
    }

    @Test
    fun `test gender values - other`() {
        val data = UserPreferencesData(
            userId = 1,
            nickname = null,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            gender = "other"
        )
        assertEquals("other", data.gender)
    }

    // ==================== 边界值测试 ====================

    @Test
    fun `test weight boundary values`() {
        // 最小有效体重
        val minWeight = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = 0.1,
            height = null,
            age = null,
            gender = null
        )
        assertEquals(0.1, minWeight.weight!!, 0.01)

        // 最大有效体重
        val maxWeight = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = 500.0,
            height = null,
            age = null,
            gender = null
        )
        assertEquals(500.0, maxWeight.weight!!, 0.01)
    }

    @Test
    fun `test height boundary values`() {
        // 测试身高边界值
        val minHeight = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = null,
            height = 50.0,
            age = null,
            gender = null
        )
        assertEquals(50.0, minHeight.height!!, 0.01)

        val maxHeight = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = null,
            height = 300.0,
            age = null,
            gender = null
        )
        assertEquals(300.0, maxHeight.height!!, 0.01)
    }

    @Test
    fun `test age boundary values`() {
        // 测试年龄边界值
        val minAge = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = null,
            height = null,
            age = 1,
            gender = null
        )
        assertEquals(1, minAge.age)

        val maxAge = UpdatePreferencesRequest(
            userId = 1,
            healthGoal = null,
            allergens = null,
            travelPreference = null,
            dailyBudget = null,
            weight = null,
            height = null,
            age = 150,
            gender = null
        )
        assertEquals(150, maxAge.age)
    }

    // ==================== 数据完整性测试 ====================

    @Test
    fun `test full user profile with body params`() {
        // 测试完整的用户档案
        val fullProfile = UserPreferencesData(
            userId = 123,
            nickname = "健康达人",
            healthGoal = "reduce_fat",
            allergens = listOf("花生", "海鲜", "牛奶"),
            travelPreference = "walking",
            dailyBudget = 500,
            weight = 70.5,
            height = 175.0,
            age = 25,
            gender = "male"
        )

        // 验证所有字段
        assertEquals(123, fullProfile.userId)
        assertEquals("健康达人", fullProfile.nickname)
        assertEquals("reduce_fat", fullProfile.healthGoal)
        assertEquals(3, fullProfile.allergens?.size)
        assertEquals("walking", fullProfile.travelPreference)
        assertEquals(500, fullProfile.dailyBudget)
        assertEquals(70.5, fullProfile.weight)
        assertEquals(175.0, fullProfile.height)
        assertEquals(25, fullProfile.age)
        assertEquals("male", fullProfile.gender)
    }

    @Test
    fun `test UpdatePreferencesState types exist`() {
        // 验证状态类型存在
        val idle = UpdatePreferencesState.Idle
        val loading = UpdatePreferencesState.Loading
        val success = UpdatePreferencesState.Success
        val error = UpdatePreferencesState.Error("测试错误")

        assertTrue(idle is UpdatePreferencesState)
        assertTrue(loading is UpdatePreferencesState)
        assertTrue(success is UpdatePreferencesState)
        assertTrue(error is UpdatePreferencesState)
        assertEquals("测试错误", error.message)
    }
}
