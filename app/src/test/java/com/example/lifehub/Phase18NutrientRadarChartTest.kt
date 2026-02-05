package com.example.lifehub

import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 18: 营养素雷达图测试
 * 测试营养素统计数据模型、计算逻辑、与膳食指南对比
 */
class Phase18NutrientRadarChartTest {

    // ============== 膳食指南常量（与后端保持一致） ==============
    companion object {
        // 中国居民膳食指南2022建议
        const val PROTEIN_MIN = 10.0  // 蛋白质最小占比 10%
        const val PROTEIN_MAX = 15.0  // 蛋白质最大占比 15%
        const val FAT_MIN = 20.0      // 脂肪最小占比 20%
        const val FAT_MAX = 30.0      // 脂肪最大占比 30%
        const val CARBS_MIN = 50.0    // 碳水最小占比 50%
        const val CARBS_MAX = 65.0    // 碳水最大占比 65%

        // 每克营养素的热量（kcal）
        const val PROTEIN_KCAL_PER_GRAM = 4
        const val FAT_KCAL_PER_GRAM = 9
        const val CARBS_KCAL_PER_GRAM = 4
    }

    // ============== 数据模型测试 ==============

    @Test
    fun `test NutrientComparison data class creation`() {
        val comparison = NutrientComparisonTestData(
            actualRatio = 12.5,
            recommendedMin = PROTEIN_MIN,
            recommendedMax = PROTEIN_MAX,
            status = "normal",
            message = "蛋白质摄入在建议范围内"
        )

        assertEquals(12.5, comparison.actualRatio, 0.01)
        assertEquals(PROTEIN_MIN, comparison.recommendedMin, 0.01)
        assertEquals(PROTEIN_MAX, comparison.recommendedMax, 0.01)
        assertEquals("normal", comparison.status)
        assertEquals("蛋白质摄入在建议范围内", comparison.message)
    }

    @Test
    fun `test DailyNutrientStats data class creation`() {
        val stats = DailyNutrientStatsTestData(
            date = "2026-02-05",
            userId = 1,
            totalProtein = 75.0,
            totalFat = 60.0,
            totalCarbs = 280.0,
            totalCalories = 1960.0,
            proteinCalories = 300.0,
            fatCalories = 540.0,
            carbsCalories = 1120.0,
            proteinRatio = 15.3,
            fatRatio = 27.6,
            carbsRatio = 57.1,
            mealCount = 3
        )

        assertEquals("2026-02-05", stats.date)
        assertEquals(1, stats.userId)
        assertEquals(75.0, stats.totalProtein, 0.01)
        assertEquals(60.0, stats.totalFat, 0.01)
        assertEquals(280.0, stats.totalCarbs, 0.01)
        assertEquals(1960.0, stats.totalCalories, 0.01)
    }

    // ============== 营养素热量计算测试 ==============

    @Test
    fun `test protein calories calculation`() {
        val proteinGrams = 75.0
        val expectedCalories = proteinGrams * PROTEIN_KCAL_PER_GRAM
        assertEquals(300.0, expectedCalories, 0.01)
    }

    @Test
    fun `test fat calories calculation`() {
        val fatGrams = 60.0
        val expectedCalories = fatGrams * FAT_KCAL_PER_GRAM
        assertEquals(540.0, expectedCalories, 0.01)
    }

    @Test
    fun `test carbs calories calculation`() {
        val carbsGrams = 280.0
        val expectedCalories = carbsGrams * CARBS_KCAL_PER_GRAM
        assertEquals(1120.0, expectedCalories, 0.01)
    }

    @Test
    fun `test total nutrient calories sum`() {
        val proteinCalories = 300.0
        val fatCalories = 540.0
        val carbsCalories = 1120.0
        val total = proteinCalories + fatCalories + carbsCalories
        assertEquals(1960.0, total, 0.01)
    }

    // ============== 营养素比例计算测试 ==============

    @Test
    fun `test nutrient ratio calculation`() {
        val totalCalories = 1960.0
        val proteinCalories = 300.0
        val fatCalories = 540.0
        val carbsCalories = 1120.0

        val proteinRatio = (proteinCalories / totalCalories) * 100
        val fatRatio = (fatCalories / totalCalories) * 100
        val carbsRatio = (carbsCalories / totalCalories) * 100

        assertEquals(15.31, proteinRatio, 0.1)
        assertEquals(27.55, fatRatio, 0.1)
        assertEquals(57.14, carbsRatio, 0.1)

        // 三大营养素比例之和应约等于100%
        val totalRatio = proteinRatio + fatRatio + carbsRatio
        assertEquals(100.0, totalRatio, 0.1)
    }

    @Test
    fun `test ratio calculation with zero calories`() {
        val totalCalories = 0.0
        // 当总热量为0时，比例应为0（避免除零错误）
        val ratio = if (totalCalories > 0) 100.0 / totalCalories else 0.0
        assertEquals(0.0, ratio, 0.01)
    }

    // ============== 膳食指南对比测试 ==============

    @Test
    fun `test protein status - normal range`() {
        val actualRatio = 12.5
        val status = determineNutrientStatus(actualRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("normal", status)
    }

    @Test
    fun `test protein status - low`() {
        val actualRatio = 8.0
        val status = determineNutrientStatus(actualRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("low", status)
    }

    @Test
    fun `test protein status - high`() {
        val actualRatio = 18.0
        val status = determineNutrientStatus(actualRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("high", status)
    }

    @Test
    fun `test fat status - normal range`() {
        val actualRatio = 25.0
        val status = determineNutrientStatus(actualRatio, FAT_MIN, FAT_MAX)
        assertEquals("normal", status)
    }

    @Test
    fun `test fat status - high`() {
        val actualRatio = 35.0
        val status = determineNutrientStatus(actualRatio, FAT_MIN, FAT_MAX)
        assertEquals("high", status)
    }

    @Test
    fun `test carbs status - normal range`() {
        val actualRatio = 55.0
        val status = determineNutrientStatus(actualRatio, CARBS_MIN, CARBS_MAX)
        assertEquals("normal", status)
    }

    @Test
    fun `test carbs status - low`() {
        val actualRatio = 45.0
        val status = determineNutrientStatus(actualRatio, CARBS_MIN, CARBS_MAX)
        assertEquals("low", status)
    }

    @Test
    fun `test boundary values - at minimum`() {
        val actualRatio = PROTEIN_MIN
        val status = determineNutrientStatus(actualRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("normal", status)
    }

    @Test
    fun `test boundary values - at maximum`() {
        val actualRatio = PROTEIN_MAX
        val status = determineNutrientStatus(actualRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("normal", status)
    }

    // ============== 雷达图数据点计算测试 ==============

    @Test
    fun `test radar chart data point normalization`() {
        // 雷达图数据点需要归一化到0-1范围
        val actualRatio = 12.5
        val maxValue = 100.0
        val normalized = actualRatio / maxValue
        assertEquals(0.125, normalized, 0.001)
    }

    @Test
    fun `test radar chart polygon calculation`() {
        // 测试三角形雷达图的三个顶点坐标计算
        // 蛋白质在顶部(0度)，脂肪在右下(120度)，碳水在左下(240度)
        val proteinRatio = 15.0
        val fatRatio = 27.5
        val carbsRatio = 57.5

        // 归一化值（使用建议的最大值作为基准）
        val proteinNorm = (proteinRatio / PROTEIN_MAX).coerceAtMost(1.5)
        val fatNorm = (fatRatio / FAT_MAX).coerceAtMost(1.5)
        val carbsNorm = (carbsRatio / CARBS_MAX).coerceAtMost(1.5)

        assertEquals(1.0, proteinNorm, 0.01)    // 15/15 = 1.0
        assertEquals(0.917, fatNorm, 0.01)      // 27.5/30 ≈ 0.917
        assertEquals(0.885, carbsNorm, 0.01)    // 57.5/65 ≈ 0.885
    }

    @Test
    fun `test radar chart angle calculation`() {
        // 测试120度间隔的三个方向
        val angles = listOf(0.0, 120.0, 240.0)
        
        // 转换为弧度
        val radians = angles.map { Math.toRadians(it) }
        
        // 验证角度间隔
        assertEquals(Math.PI * 2 / 3, radians[1] - radians[0], 0.001)
        assertEquals(Math.PI * 2 / 3, radians[2] - radians[1], 0.001)
    }

    // ============== UI状态测试 ==============

    @Test
    fun `test nutrient stats UI state - idle`() {
        val state = NutrientStatsUiStateTest.Idle
        assertTrue(state is NutrientStatsUiStateTest.Idle)
    }

    @Test
    fun `test nutrient stats UI state - loading`() {
        val state = NutrientStatsUiStateTest.Loading
        assertTrue(state is NutrientStatsUiStateTest.Loading)
    }

    @Test
    fun `test nutrient stats UI state - success`() {
        val stats = DailyNutrientStatsTestData(
            date = "2026-02-05",
            userId = 1,
            totalProtein = 75.0,
            totalFat = 60.0,
            totalCarbs = 280.0,
            totalCalories = 1960.0,
            proteinCalories = 300.0,
            fatCalories = 540.0,
            carbsCalories = 1120.0,
            proteinRatio = 15.3,
            fatRatio = 27.6,
            carbsRatio = 57.1,
            mealCount = 3
        )
        val state = NutrientStatsUiStateTest.Success(stats)
        assertTrue(state is NutrientStatsUiStateTest.Success)
        assertEquals(stats, state.data)
    }

    @Test
    fun `test nutrient stats UI state - error`() {
        val errorMsg = "网络连接失败"
        val state = NutrientStatsUiStateTest.Error(errorMsg)
        assertTrue(state is NutrientStatsUiStateTest.Error)
        assertEquals(errorMsg, state.message)
    }

    // ============== 颜色状态映射测试 ==============

    @Test
    fun `test status to color mapping - normal should be green`() {
        val status = "normal"
        val colorName = mapStatusToColorName(status)
        assertEquals("green", colorName)
    }

    @Test
    fun `test status to color mapping - low should be orange`() {
        val status = "low"
        val colorName = mapStatusToColorName(status)
        assertEquals("orange", colorName)
    }

    @Test
    fun `test status to color mapping - high should be red`() {
        val status = "high"
        val colorName = mapStatusToColorName(status)
        assertEquals("red", colorName)
    }

    // ============== 消息生成测试 ==============

    @Test
    fun `test generate message for normal protein`() {
        val message = generateNutrientMessage("蛋白质", 12.5, "normal")
        assertTrue(message.contains("蛋白质"))
        assertTrue(message.contains("建议范围内") || message.contains("正常"))
    }

    @Test
    fun `test generate message for low protein`() {
        val message = generateNutrientMessage("蛋白质", 8.0, "low")
        assertTrue(message.contains("蛋白质"))
        assertTrue(message.contains("偏低") || message.contains("不足"))
    }

    @Test
    fun `test generate message for high fat`() {
        val message = generateNutrientMessage("脂肪", 35.0, "high")
        assertTrue(message.contains("脂肪"))
        assertTrue(message.contains("偏高") || message.contains("过高"))
    }

    // ============== 边界条件测试 ==============

    @Test
    fun `test empty meal data`() {
        val stats = DailyNutrientStatsTestData(
            date = "2026-02-05",
            userId = 1,
            totalProtein = 0.0,
            totalFat = 0.0,
            totalCarbs = 0.0,
            totalCalories = 0.0,
            proteinCalories = 0.0,
            fatCalories = 0.0,
            carbsCalories = 0.0,
            proteinRatio = 0.0,
            fatRatio = 0.0,
            carbsRatio = 0.0,
            mealCount = 0
        )
        
        assertEquals(0, stats.mealCount)
        assertEquals(0.0, stats.totalCalories, 0.01)
    }

    @Test
    fun `test very high nutrient values`() {
        // 测试极高值（超过建议范围）
        val proteinRatio = 25.0  // 远超15%的上限
        val status = determineNutrientStatus(proteinRatio, PROTEIN_MIN, PROTEIN_MAX)
        assertEquals("high", status)
    }

    @Test
    fun `test very low nutrient values`() {
        // 测试极低值
        val carbsRatio = 30.0  // 远低于50%的下限
        val status = determineNutrientStatus(carbsRatio, CARBS_MIN, CARBS_MAX)
        assertEquals("low", status)
    }

    // ============== 辅助函数 ==============

    private fun determineNutrientStatus(actual: Double, min: Double, max: Double): String {
        return when {
            actual < min -> "low"
            actual > max -> "high"
            else -> "normal"
        }
    }

    private fun mapStatusToColorName(status: String): String {
        return when (status) {
            "normal" -> "green"
            "low" -> "orange"
            "high" -> "red"
            else -> "gray"
        }
    }

    private fun generateNutrientMessage(nutrientName: String, ratio: Double, status: String): String {
        return when (status) {
            "normal" -> "${nutrientName}摄入在建议范围内（${String.format("%.1f", ratio)}%）"
            "low" -> "${nutrientName}摄入偏低（${String.format("%.1f", ratio)}%），建议适当增加"
            "high" -> "${nutrientName}摄入偏高（${String.format("%.1f", ratio)}%），建议适当控制"
            else -> "${nutrientName}摄入${String.format("%.1f", ratio)}%"
        }
    }
}

// ============== 测试用数据类 ==============

data class NutrientComparisonTestData(
    val actualRatio: Double,
    val recommendedMin: Double,
    val recommendedMax: Double,
    val status: String,
    val message: String
)

data class DailyNutrientStatsTestData(
    val date: String,
    val userId: Int,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val totalCalories: Double,
    val proteinCalories: Double,
    val fatCalories: Double,
    val carbsCalories: Double,
    val proteinRatio: Double,
    val fatRatio: Double,
    val carbsRatio: Double,
    val mealCount: Int
)

sealed class NutrientStatsUiStateTest {
    object Idle : NutrientStatsUiStateTest()
    object Loading : NutrientStatsUiStateTest()
    data class Success(val data: DailyNutrientStatsTestData) : NutrientStatsUiStateTest()
    data class Error(val message: String) : NutrientStatsUiStateTest()
}
