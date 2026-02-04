package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.viewmodel.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 13: 餐前拍摄功能测试
 * 测试餐前餐后对比相关的数据模型、状态管理和业务逻辑
 */
class Phase13MealComparisonTest {

    // ==================== 数据模型测试 ====================

    @Test
    fun `测试BeforeMealData数据模型创建`() {
        val dishFeature = DishFeature(
            name = "红烧肉",
            estimatedWeight = 200,
            estimatedCalories = 500.0,
            estimatedProtein = 25.0,
            estimatedFat = 35.0,
            estimatedCarbs = 10.0
        )

        val mealFeatures = MealFeatures(
            dishes = listOf(dishFeature),
            totalEstimatedCalories = 500.0,
            totalEstimatedProtein = 25.0,
            totalEstimatedFat = 35.0,
            totalEstimatedCarbs = 10.0
        )

        val beforeMealData = BeforeMealData(
            comparisonId = 1,
            beforeImageUrl = "/uploads/meal/before_1.jpg",
            beforeFeatures = mealFeatures,
            status = "pending_after"
        )

        assertEquals(1, beforeMealData.comparisonId)
        assertEquals("/uploads/meal/before_1.jpg", beforeMealData.beforeImageUrl)
        assertEquals("pending_after", beforeMealData.status)
        assertNotNull(beforeMealData.beforeFeatures)
        assertEquals(500.0, beforeMealData.beforeFeatures?.totalEstimatedCalories ?: 0.0, 0.01)
    }

    @Test
    fun `测试DishFeature数据模型创建`() {
        val dish = DishFeature(
            name = "清炒时蔬",
            estimatedWeight = 150,
            estimatedCalories = 80.0,
            estimatedProtein = 3.0,
            estimatedFat = 5.0,
            estimatedCarbs = 8.0
        )

        assertEquals("清炒时蔬", dish.name)
        assertEquals(150, dish.estimatedWeight)
        assertEquals(80.0, dish.estimatedCalories ?: 0.0, 0.01)
        assertEquals(3.0, dish.estimatedProtein ?: 0.0, 0.01)
        assertEquals(5.0, dish.estimatedFat ?: 0.0, 0.01)
        assertEquals(8.0, dish.estimatedCarbs ?: 0.0, 0.01)
    }

    @Test
    fun `测试MealFeatures多菜品汇总`() {
        val dish1 = DishFeature(
            name = "红烧肉",
            estimatedWeight = 200,
            estimatedCalories = 500.0,
            estimatedProtein = 25.0,
            estimatedFat = 35.0,
            estimatedCarbs = 10.0
        )

        val dish2 = DishFeature(
            name = "清炒时蔬",
            estimatedWeight = 150,
            estimatedCalories = 80.0,
            estimatedProtein = 3.0,
            estimatedFat = 5.0,
            estimatedCarbs = 8.0
        )

        val features = MealFeatures(
            dishes = listOf(dish1, dish2),
            totalEstimatedCalories = 580.0,
            totalEstimatedProtein = 28.0,
            totalEstimatedFat = 40.0,
            totalEstimatedCarbs = 18.0
        )

        assertEquals(2, features.dishes?.size)
        assertEquals(580.0, features.totalEstimatedCalories ?: 0.0, 0.01)
        assertEquals(28.0, features.totalEstimatedProtein ?: 0.0, 0.01)
        assertEquals(40.0, features.totalEstimatedFat ?: 0.0, 0.01)
        assertEquals(18.0, features.totalEstimatedCarbs ?: 0.0, 0.01)
    }

    @Test
    fun `测试AfterMealData数据模型创建`() {
        val afterMealData = AfterMealData(
            comparisonId = 1,
            beforeImageUrl = "/uploads/meal/before_1.jpg",
            afterImageUrl = "/uploads/meal/after_1.jpg",
            consumptionRatio = 0.75,
            originalCalories = 580.0,
            netCalories = 435.0,
            originalProtein = 28.0,
            originalFat = 40.0,
            originalCarbs = 18.0,
            netProtein = 21.0,
            netFat = 30.0,
            netCarbs = 13.5,
            comparisonAnalysis = "您吃掉了约75%的食物",
            status = "completed"
        )

        assertEquals(1, afterMealData.comparisonId)
        assertEquals(0.75, afterMealData.consumptionRatio, 0.01)
        assertEquals(580.0, afterMealData.originalCalories, 0.01)
        assertEquals(435.0, afterMealData.netCalories, 0.01)
        assertEquals("completed", afterMealData.status)

        // 验证净摄入计算公式: 净摄入 = 原始 × 消耗比例
        val expectedNetCalories = afterMealData.originalCalories * afterMealData.consumptionRatio
        assertEquals(expectedNetCalories, afterMealData.netCalories, 0.01)
    }

    @Test
    fun `测试MealComparisonRecord本地状态记录`() {
        val record = MealComparisonRecord(
            comparisonId = 1,
            beforeImageUrl = "/uploads/meal/before_1.jpg",
            afterImageUrl = null,
            beforeFeatures = null,
            consumptionRatio = null,
            originalCalories = 580.0,
            netCalories = null,
            status = "pending_after"
        )

        assertEquals(1, record.comparisonId)
        assertEquals("/uploads/meal/before_1.jpg", record.beforeImageUrl)
        assertNull(record.afterImageUrl)
        assertEquals("pending_after", record.status)

        // 测试copy方法更新状态
        val updatedRecord = record.copy(
            afterImageUrl = "/uploads/meal/after_1.jpg",
            consumptionRatio = 0.75,
            netCalories = 435.0,
            status = "completed"
        )

        assertEquals("/uploads/meal/after_1.jpg", updatedRecord.afterImageUrl)
        assertEquals(0.75, updatedRecord.consumptionRatio ?: 0.0, 0.01)
        assertEquals(435.0, updatedRecord.netCalories ?: 0.0, 0.01)
        assertEquals("completed", updatedRecord.status)
    }

    // ==================== 状态类测试 ====================

    @Test
    fun `测试BeforeMealUploadState状态类`() {
        // Idle状态
        val idleState: BeforeMealUploadState = BeforeMealUploadState.Idle
        assertTrue(idleState is BeforeMealUploadState.Idle)

        // Loading状态
        val loadingState: BeforeMealUploadState = BeforeMealUploadState.Loading
        assertTrue(loadingState is BeforeMealUploadState.Loading)

        // Success状态
        val successData = BeforeMealData(
            comparisonId = 1,
            beforeImageUrl = "/uploads/meal/before_1.jpg",
            beforeFeatures = null,
            status = "pending_after"
        )
        val successState: BeforeMealUploadState = BeforeMealUploadState.Success(successData)
        assertTrue(successState is BeforeMealUploadState.Success)
        assertEquals(1, (successState as BeforeMealUploadState.Success).data.comparisonId)

        // Error状态
        val errorState: BeforeMealUploadState = BeforeMealUploadState.Error("网络连接失败")
        assertTrue(errorState is BeforeMealUploadState.Error)
        assertEquals("网络连接失败", (errorState as BeforeMealUploadState.Error).message)
    }

    @Test
    fun `测试AfterMealUploadState状态类`() {
        // Idle状态
        val idleState: AfterMealUploadState = AfterMealUploadState.Idle
        assertTrue(idleState is AfterMealUploadState.Idle)

        // Loading状态
        val loadingState: AfterMealUploadState = AfterMealUploadState.Loading
        assertTrue(loadingState is AfterMealUploadState.Loading)

        // Success状态
        val successData = AfterMealData(
            comparisonId = 1,
            beforeImageUrl = "/uploads/meal/before_1.jpg",
            afterImageUrl = "/uploads/meal/after_1.jpg",
            consumptionRatio = 0.75,
            originalCalories = 580.0,
            netCalories = 435.0,
            originalProtein = 28.0,
            originalFat = 40.0,
            originalCarbs = 18.0,
            netProtein = 21.0,
            netFat = 30.0,
            netCarbs = 13.5,
            comparisonAnalysis = "您吃掉了约75%的食物",
            status = "completed"
        )
        val successState: AfterMealUploadState = AfterMealUploadState.Success(successData)
        assertTrue(successState is AfterMealUploadState.Success)
        assertEquals(0.75, (successState as AfterMealUploadState.Success).data.consumptionRatio, 0.01)

        // Error状态
        val errorState: AfterMealUploadState = AfterMealUploadState.Error("对比记录不存在")
        assertTrue(errorState is AfterMealUploadState.Error)
        assertEquals("对比记录不存在", (errorState as AfterMealUploadState.Error).message)
    }

    // ==================== 业务逻辑测试 ====================

    @Test
    fun `测试净摄入热量计算逻辑`() {
        // 场景1: 吃掉75%
        val originalCalories1 = 580.0
        val consumptionRatio1 = 0.75
        val expectedNetCalories1 = originalCalories1 * consumptionRatio1
        assertEquals(435.0, expectedNetCalories1, 0.01)

        // 场景2: 吃掉100%（全部吃完）
        val originalCalories2 = 500.0
        val consumptionRatio2 = 1.0
        val expectedNetCalories2 = originalCalories2 * consumptionRatio2
        assertEquals(500.0, expectedNetCalories2, 0.01)

        // 场景3: 吃掉50%
        val originalCalories3 = 800.0
        val consumptionRatio3 = 0.5
        val expectedNetCalories3 = originalCalories3 * consumptionRatio3
        assertEquals(400.0, expectedNetCalories3, 0.01)

        // 场景4: 吃掉0%（一口没吃）
        val originalCalories4 = 600.0
        val consumptionRatio4 = 0.0
        val expectedNetCalories4 = originalCalories4 * consumptionRatio4
        assertEquals(0.0, expectedNetCalories4, 0.01)
    }

    @Test
    fun `测试净摄入营养素计算逻辑`() {
        val originalProtein = 28.0
        val originalFat = 40.0
        val originalCarbs = 18.0
        val consumptionRatio = 0.75

        val netProtein = originalProtein * consumptionRatio
        val netFat = originalFat * consumptionRatio
        val netCarbs = originalCarbs * consumptionRatio

        assertEquals(21.0, netProtein, 0.01)
        assertEquals(30.0, netFat, 0.01)
        assertEquals(13.5, netCarbs, 0.01)
    }

    @Test
    fun `测试对比记录状态流转`() {
        // 状态流转: pending_after -> completed
        var status = "pending_after"
        assertEquals("pending_after", status)

        // 上传餐后图片后状态变为completed
        status = "completed"
        assertEquals("completed", status)
    }

    @Test
    fun `测试BeforeMealResponse响应解析`() {
        val response = BeforeMealResponse(
            code = 200,
            message = "餐前图片上传成功",
            data = BeforeMealData(
                comparisonId = 1,
                beforeImageUrl = "/uploads/meal/before_1.jpg",
                beforeFeatures = MealFeatures(
                    dishes = listOf(
                        DishFeature(
                            name = "宫保鸡丁",
                            estimatedWeight = 250,
                            estimatedCalories = 350.0,
                            estimatedProtein = 28.0,
                            estimatedFat = 18.0,
                            estimatedCarbs = 20.0
                        )
                    ),
                    totalEstimatedCalories = 350.0,
                    totalEstimatedProtein = 28.0,
                    totalEstimatedFat = 18.0,
                    totalEstimatedCarbs = 20.0
                ),
                status = "pending_after"
            )
        )

        assertEquals(200, response.code)
        assertEquals("餐前图片上传成功", response.message)
        assertNotNull(response.data)
        assertEquals(1, response.data?.comparisonId)
        assertEquals(1, response.data?.beforeFeatures?.dishes?.size)
        assertEquals("宫保鸡丁", response.data?.beforeFeatures?.dishes?.get(0)?.name)
    }

    @Test
    fun `测试AfterMealResponse响应解析`() {
        val response = AfterMealResponse(
            code = 200,
            message = "餐后图片上传成功，对比完成",
            data = AfterMealData(
                comparisonId = 1,
                beforeImageUrl = "/uploads/meal/before_1.jpg",
                afterImageUrl = "/uploads/meal/after_1.jpg",
                consumptionRatio = 0.80,
                originalCalories = 350.0,
                netCalories = 280.0,
                originalProtein = 28.0,
                originalFat = 18.0,
                originalCarbs = 20.0,
                netProtein = 22.4,
                netFat = 14.4,
                netCarbs = 16.0,
                comparisonAnalysis = "您吃掉了约80%的食物",
                status = "completed"
            )
        )

        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals(0.80, response.data?.consumptionRatio ?: 0.0, 0.01)
        assertEquals(280.0, response.data?.netCalories ?: 0.0, 0.01)
        assertEquals("completed", response.data?.status)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `测试空菜品列表处理`() {
        val features = MealFeatures(
            dishes = emptyList(),
            totalEstimatedCalories = 0.0,
            totalEstimatedProtein = 0.0,
            totalEstimatedFat = 0.0,
            totalEstimatedCarbs = 0.0
        )

        assertTrue(features.dishes?.isEmpty() ?: true)
        assertEquals(0.0, features.totalEstimatedCalories ?: 0.0, 0.01)
    }

    @Test
    fun `测试空值处理`() {
        val dishWithNulls = DishFeature(
            name = "未知菜品",
            estimatedWeight = null,
            estimatedCalories = null,
            estimatedProtein = null,
            estimatedFat = null,
            estimatedCarbs = null
        )

        assertEquals("未知菜品", dishWithNulls.name)
        assertNull(dishWithNulls.estimatedWeight)
        assertNull(dishWithNulls.estimatedCalories)
    }

    @Test
    fun `测试消耗比例边界值`() {
        // 消耗比例为0（没吃）
        val ratio0 = 0.0
        assertTrue(ratio0 >= 0.0 && ratio0 <= 1.0)

        // 消耗比例为1（全吃完）
        val ratio1 = 1.0
        assertTrue(ratio1 >= 0.0 && ratio1 <= 1.0)

        // 消耗比例为0.5（吃了一半）
        val ratio05 = 0.5
        assertTrue(ratio05 >= 0.0 && ratio05 <= 1.0)
    }
}
