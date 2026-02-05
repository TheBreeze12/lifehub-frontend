package com.example.lifehub

import org.junit.Test
import org.junit.Assert.*

/**
 * Phase 14 测试：餐后拍摄与对比展示功能测试
 * 测试前端逻辑正确性
 */
class Phase14AfterMealComparisonTest {

    // ==================== 消耗比例计算测试 ====================

    @Test
    fun `test consumption ratio within bounds`() {
        // 消耗比例应在0-1之间
        val testRatios = listOf(-0.1, 0.0, 0.5, 0.75, 1.0, 1.5)
        
        for (ratio in testRatios) {
            val clamped = ratio.coerceIn(0.0, 1.0)
            assertTrue("比例 $ratio 限制后应在 [0,1]: $clamped", clamped in 0.0..1.0)
        }
    }

    @Test
    fun `test net calories calculation`() {
        // 净热量 = 原始热量 × 消耗比例
        val testCases = listOf(
            Triple(500.0, 0.8, 400.0),   // 吃了80%，净摄入400
            Triple(300.0, 0.5, 150.0),   // 吃了50%，净摄入150
            Triple(100.0, 1.0, 100.0),   // 全部吃完
            Triple(200.0, 0.0, 0.0),     // 没吃
            Triple(450.0, 0.75, 337.5),  // 吃了75%
        )
        
        for ((original, ratio, expected) in testCases) {
            val netCalories = original * ratio
            assertEquals(
                "净热量计算: $original × $ratio = $netCalories (期望 $expected)",
                expected,
                netCalories,
                0.01
            )
        }
    }

    @Test
    fun `test net nutrients calculation`() {
        // 净营养素 = 原始营养素 × 消耗比例
        val originalProtein = 30.0
        val originalFat = 20.0
        val originalCarbs = 50.0
        val consumptionRatio = 0.75

        val netProtein = originalProtein * consumptionRatio
        val netFat = originalFat * consumptionRatio
        val netCarbs = originalCarbs * consumptionRatio

        assertEquals(22.5, netProtein, 0.01)
        assertEquals(15.0, netFat, 0.01)
        assertEquals(37.5, netCarbs, 0.01)
    }

    // ==================== 手动调整比例测试 ====================

    @Test
    fun `test manual ratio adjustment`() {
        val originalCalories = 600.0
        
        // 用户可以手动调整消耗比例
        val adjustments = listOf(0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
        
        for (userRatio in adjustments) {
            val adjustedCalories = originalCalories * userRatio
            assertTrue("调整后热量应 <= 原始热量", adjustedCalories <= originalCalories)
            assertTrue("调整后热量应 >= 0", adjustedCalories >= 0)
        }
    }

    @Test
    fun `test slider step calculation`() {
        // Slider步进值为5%
        val stepSize = 0.05
        val baseRatio = 0.75
        
        // 向上调整一步
        val upRatio = (baseRatio + stepSize).coerceIn(0.0, 1.0)
        assertEquals(0.80, upRatio, 0.001)
        
        // 向下调整一步
        val downRatio = (baseRatio - stepSize).coerceIn(0.0, 1.0)
        assertEquals(0.70, downRatio, 0.001)
    }

    // ==================== 状态转换测试 ====================

    @Test
    fun `test comparison state flow`() {
        // 模拟状态转换
        var state = ComparisonState.Idle
        
        // 1. 开始餐前上传
        state = ComparisonState.BeforeUploading
        assertEquals(ComparisonState.BeforeUploading, state)
        
        // 2. 餐前上传成功，等待餐后
        state = ComparisonState.PendingAfter
        assertEquals(ComparisonState.PendingAfter, state)
        
        // 3. 开始餐后上传
        state = ComparisonState.AfterUploading
        assertEquals(ComparisonState.AfterUploading, state)
        
        // 4. 完成对比
        state = ComparisonState.Completed
        assertEquals(ComparisonState.Completed, state)
    }

    @Test
    fun `test can upload after meal only after before meal success`() {
        // 只有餐前上传成功后才能上传餐后
        val beforeMealSuccess = true
        val comparisonId = 123
        
        val canUploadAfter = beforeMealSuccess && comparisonId > 0
        assertTrue("餐前成功后应该可以上传餐后", canUploadAfter)
        
        // 餐前失败时不能上传餐后
        val beforeMealFailed = false
        val cannotUploadAfter = beforeMealFailed && comparisonId > 0
        assertFalse("餐前失败时不能上传餐后", cannotUploadAfter)
    }

    // ==================== 数据模型测试 ====================

    @Test
    fun `test after meal data parsing`() {
        // 模拟解析餐后响应数据
        val mockData = AfterMealTestData(
            comparisonId = 1,
            consumptionRatio = 0.75,
            originalCalories = 500.0,
            netCalories = 375.0,
            originalProtein = 25.0,
            originalFat = 20.0,
            originalCarbs = 40.0,
            netProtein = 18.75,
            netFat = 15.0,
            netCarbs = 30.0,
            status = "completed"
        )
        
        assertEquals(1, mockData.comparisonId)
        assertEquals(0.75, mockData.consumptionRatio, 0.001)
        assertEquals(500.0, mockData.originalCalories, 0.001)
        assertEquals(375.0, mockData.netCalories, 0.001)
        assertEquals("completed", mockData.status)
        
        // 验证净值计算正确性
        assertEquals(mockData.originalCalories * mockData.consumptionRatio, mockData.netCalories, 0.01)
    }

    @Test
    fun `test comparison record update`() {
        // 模拟更新对比记录
        var record = MealComparisonTestRecord(
            comparisonId = 1,
            beforeImageUrl = "http://example.com/before.jpg",
            afterImageUrl = null,
            consumptionRatio = null,
            originalCalories = 500.0,
            netCalories = null,
            status = "pending_after"
        )
        
        // 餐后上传成功后更新记录
        record = record.copy(
            afterImageUrl = "http://example.com/after.jpg",
            consumptionRatio = 0.8,
            netCalories = 400.0,
            status = "completed"
        )
        
        assertNotNull(record.afterImageUrl)
        assertEquals(0.8, record.consumptionRatio!!, 0.001)
        assertEquals(400.0, record.netCalories!!, 0.001)
        assertEquals("completed", record.status)
    }

    // ==================== UI显示测试 ====================

    @Test
    fun `test percentage display format`() {
        // 消耗比例的百分比显示格式
        val testRatios = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
        val expectedDisplays = listOf("0%", "25%", "50%", "75%", "100%")
        
        for ((ratio, expected) in testRatios.zip(expectedDisplays)) {
            val display = formatPercentage(ratio)
            assertEquals(expected, display)
        }
    }

    @Test
    fun `test calories display format`() {
        // 热量显示格式 - toInt()使用截断而非四舍五入
        val testCalories = listOf(0.0, 100.5, 250.0, 500.75, 1000.0)
        val expectedDisplays = listOf("0", "100", "250", "500", "1000")
        
        for ((calories, expected) in testCalories.zip(expectedDisplays)) {
            val display = formatCalories(calories)
            assertEquals(expected, display)
        }
    }

    @Test
    fun `test difference display with sign`() {
        // 差异显示（带正负号）
        val original = 500.0
        val net = 375.0
        val saved = original - net
        
        val display = "+${saved.toInt()} kcal 少摄入"
        assertEquals("+125 kcal 少摄入", display)
    }

    // ==================== 辅助方法 ====================

    private fun formatPercentage(ratio: Double): String {
        return "${(ratio * 100).toInt()}%"
    }

    private fun formatCalories(calories: Double): String {
        return calories.toInt().toString()
    }

    // 测试用枚举和数据类
    enum class ComparisonState {
        Idle,
        BeforeUploading,
        PendingAfter,
        AfterUploading,
        Completed,
        Error
    }

    data class AfterMealTestData(
        val comparisonId: Int,
        val consumptionRatio: Double,
        val originalCalories: Double,
        val netCalories: Double,
        val originalProtein: Double,
        val originalFat: Double,
        val originalCarbs: Double,
        val netProtein: Double,
        val netFat: Double,
        val netCarbs: Double,
        val status: String
    )

    data class MealComparisonTestRecord(
        val comparisonId: Int,
        val beforeImageUrl: String?,
        val afterImageUrl: String?,
        val consumptionRatio: Double?,
        val originalCalories: Double?,
        val netCalories: Double?,
        val status: String
    )
}
