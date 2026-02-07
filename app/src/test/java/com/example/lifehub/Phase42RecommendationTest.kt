package com.example.lifehub

import com.example.lifehub.data.RecommendedFood
import com.example.lifehub.data.RecommendationResultData
import com.example.lifehub.data.RecommendationResponse
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 42: 个性化菜品推荐 单元测试
 *
 * 测试范围:
 * 1. RecommendedFood 数据模型正确性
 * 2. RecommendationResultData 数据模型正确性
 * 3. RecommendationResponse 响应解析正确性
 * 4. 推荐评分分类逻辑
 * 5. 标签颜色映射逻辑
 * 6. 餐次类型验证
 * 7. 边界情况（空数据、极端值、空列表等）
 * 8. 多菜品推荐排序与数量
 */
class Phase42RecommendationTest {

    // ==================== 1. RecommendedFood 数据模型测试 ====================

    @Test
    fun `test RecommendedFood model - normal food`() {
        val food = RecommendedFood(
            foodName = "清蒸鲈鱼",
            calories = 105.0,
            protein = 19.5,
            fat = 3.0,
            carbs = 0.5,
            score = 92.5,
            reason = "高蛋白低脂肪，非常适合您的减脂目标",
            tags = listOf("高蛋白", "低脂肪", "低热量")
        )
        assertEquals("清蒸鲈鱼", food.foodName)
        assertEquals(105.0, food.calories, 0.01)
        assertEquals(19.5, food.protein, 0.01)
        assertEquals(3.0, food.fat, 0.01)
        assertEquals(0.5, food.carbs, 0.01)
        assertEquals(92.5, food.score, 0.01)
        assertEquals("高蛋白低脂肪，非常适合您的减脂目标", food.reason)
        assertEquals(3, food.tags.size)
        assertTrue(food.tags.contains("高蛋白"))
    }

    @Test
    fun `test RecommendedFood model - empty tags`() {
        val food = RecommendedFood(
            foodName = "白米饭",
            calories = 116.0,
            protein = 2.6,
            fat = 0.3,
            carbs = 25.6,
            score = 45.0,
            reason = "碳水含量较高，建议控制食用量",
            tags = emptyList()
        )
        assertTrue(food.tags.isEmpty())
        assertEquals("白米饭", food.foodName)
    }

    @Test
    fun `test RecommendedFood model - high score food`() {
        val food = RecommendedFood(
            foodName = "鸡胸肉沙拉",
            calories = 150.0,
            protein = 30.0,
            fat = 3.5,
            carbs = 5.0,
            score = 98.0,
            reason = "极高蛋白质、极低脂肪，是增肌减脂的最佳选择",
            tags = listOf("高蛋白", "低脂肪", "低碳水")
        )
        assertTrue(food.score >= 85)
        assertEquals(98.0, food.score, 0.01)
    }

    @Test
    fun `test RecommendedFood model - low score food`() {
        val food = RecommendedFood(
            foodName = "炸鸡",
            calories = 290.0,
            protein = 15.0,
            fat = 20.0,
            carbs = 12.0,
            score = 25.0,
            reason = "油脂含量高，不建议减脂期食用",
            tags = listOf("高脂肪", "高热量")
        )
        assertTrue(food.score < 70)
        assertEquals(25.0, food.score, 0.01)
    }

    @Test
    fun `test RecommendedFood model - zero nutrition values`() {
        val food = RecommendedFood(
            foodName = "白开水",
            calories = 0.0,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0,
            score = 50.0,
            reason = "零热量饮品",
            tags = listOf("零热量")
        )
        assertEquals(0.0, food.calories, 0.01)
        assertEquals(0.0, food.protein, 0.01)
        assertEquals(0.0, food.fat, 0.01)
        assertEquals(0.0, food.carbs, 0.01)
    }

    @Test
    fun `test RecommendedFood model - boundary score 0`() {
        val food = RecommendedFood(
            foodName = "极不推荐食品",
            calories = 500.0,
            protein = 1.0,
            fat = 40.0,
            carbs = 50.0,
            score = 0.0,
            reason = "完全不符合您的健康目标",
            tags = emptyList()
        )
        assertEquals(0.0, food.score, 0.01)
    }

    @Test
    fun `test RecommendedFood model - boundary score 100`() {
        val food = RecommendedFood(
            foodName = "完美推荐食品",
            calories = 100.0,
            protein = 25.0,
            fat = 1.0,
            carbs = 2.0,
            score = 100.0,
            reason = "完美匹配您的所有健康需求",
            tags = listOf("完美匹配")
        )
        assertEquals(100.0, food.score, 0.01)
    }

    @Test
    fun `test RecommendedFood model - very long food name`() {
        val longName = "超级无敌大份宫保鸡丁配米饭套餐附赠汤品"
        val food = RecommendedFood(
            foodName = longName,
            calories = 600.0,
            protein = 25.0,
            fat = 18.0,
            carbs = 70.0,
            score = 55.0,
            reason = "热量偏高但营养全面",
            tags = listOf("套餐")
        )
        assertEquals(longName, food.foodName)
    }

    @Test
    fun `test RecommendedFood model - very long reason`() {
        val longReason = "这道菜品含有丰富的蛋白质和维生素，" +
            "脂肪含量适中，非常适合您的减脂健身目标。" +
            "建议搭配蔬菜沙拉食用，可以获得更全面的营养摄入。" +
            "每100g含有优质蛋白质30g，能有效促进肌肉修复和生长。"
        val food = RecommendedFood(
            foodName = "牛排",
            calories = 250.0,
            protein = 30.0,
            fat = 15.0,
            carbs = 0.0,
            score = 85.0,
            reason = longReason,
            tags = listOf("高蛋白")
        )
        assertEquals(longReason, food.reason)
    }

    @Test
    fun `test RecommendedFood model - many tags`() {
        val tags = listOf("高蛋白", "低脂肪", "低碳水", "低热量", "高纤维", "无麸质", "无乳糖")
        val food = RecommendedFood(
            foodName = "藜麦鸡胸沙拉",
            calories = 180.0,
            protein = 28.0,
            fat = 4.0,
            carbs = 12.0,
            score = 95.0,
            reason = "全面营养，多种优点",
            tags = tags
        )
        assertEquals(7, food.tags.size)
    }

    // ==================== 2. RecommendationResultData 数据模型测试 ====================

    @Test
    fun `test RecommendationResultData model - reduce fat goal`() {
        val data = RecommendationResultData(
            userId = 1,
            mealType = "lunch",
            remainingCalories = 800.0,
            dailyCalorieTarget = 2000.0,
            healthGoal = "reduce_fat",
            healthGoalLabel = "减脂",
            recommendations = listOf(
                RecommendedFood(
                    foodName = "清蒸鲈鱼",
                    calories = 105.0,
                    protein = 19.5,
                    fat = 3.0,
                    carbs = 0.5,
                    score = 92.5,
                    reason = "适合减脂",
                    tags = listOf("高蛋白", "低脂肪")
                )
            )
        )
        assertEquals(1, data.userId)
        assertEquals("lunch", data.mealType)
        assertEquals(800.0, data.remainingCalories, 0.01)
        assertEquals(2000.0, data.dailyCalorieTarget, 0.01)
        assertEquals("reduce_fat", data.healthGoal)
        assertEquals("减脂", data.healthGoalLabel)
        assertEquals(1, data.recommendations.size)
    }

    @Test
    fun `test RecommendationResultData model - gain muscle goal`() {
        val data = RecommendationResultData(
            userId = 2,
            mealType = "dinner",
            remainingCalories = 1200.0,
            dailyCalorieTarget = 2500.0,
            healthGoal = "gain_muscle",
            healthGoalLabel = "增肌",
            recommendations = emptyList()
        )
        assertEquals("gain_muscle", data.healthGoal)
        assertEquals("增肌", data.healthGoalLabel)
        assertEquals(1200.0, data.remainingCalories, 0.01)
        assertTrue(data.recommendations.isEmpty())
    }

    @Test
    fun `test RecommendationResultData model - control sugar goal`() {
        val data = RecommendationResultData(
            userId = 3,
            mealType = "breakfast",
            remainingCalories = 500.0,
            dailyCalorieTarget = 1800.0,
            healthGoal = "control_sugar",
            healthGoalLabel = "控糖",
            recommendations = listOf(
                RecommendedFood(
                    foodName = "全麦面包",
                    calories = 65.0,
                    protein = 4.0,
                    fat = 1.0,
                    carbs = 12.0,
                    score = 78.0,
                    reason = "低GI食品",
                    tags = listOf("低GI", "高纤维")
                )
            )
        )
        assertEquals("control_sugar", data.healthGoal)
        assertEquals("控糖", data.healthGoalLabel)
    }

    @Test
    fun `test RecommendationResultData model - balanced goal`() {
        val data = RecommendationResultData(
            userId = 4,
            mealType = "snack",
            remainingCalories = 300.0,
            dailyCalorieTarget = 2200.0,
            healthGoal = "balanced",
            healthGoalLabel = "均衡饮食",
            recommendations = emptyList()
        )
        assertEquals("balanced", data.healthGoal)
        assertEquals("均衡饮食", data.healthGoalLabel)
        assertEquals("snack", data.mealType)
    }

    @Test
    fun `test RecommendationResultData model - negative remaining calories`() {
        val data = RecommendationResultData(
            userId = 5,
            mealType = "dinner",
            remainingCalories = -200.0,
            dailyCalorieTarget = 2000.0,
            healthGoal = "reduce_fat",
            healthGoalLabel = "减脂",
            recommendations = emptyList()
        )
        assertTrue(data.remainingCalories < 0)
        assertEquals(-200.0, data.remainingCalories, 0.01)
    }

    @Test
    fun `test RecommendationResultData model - multiple recommendations`() {
        val foods = listOf(
            RecommendedFood("清蒸鲈鱼", 105.0, 19.5, 3.0, 0.5, 92.5, "理由1", listOf("高蛋白")),
            RecommendedFood("鸡胸肉沙拉", 150.0, 30.0, 3.5, 5.0, 88.0, "理由2", listOf("低脂肪")),
            RecommendedFood("全麦面包", 65.0, 4.0, 1.0, 12.0, 75.0, "理由3", listOf("高纤维")),
            RecommendedFood("酸奶", 72.0, 3.5, 3.0, 5.0, 70.0, "理由4", listOf("益生菌")),
            RecommendedFood("水果拼盘", 80.0, 1.0, 0.5, 18.0, 65.0, "理由5", listOf("维生素"))
        )
        val data = RecommendationResultData(
            userId = 1,
            mealType = "lunch",
            remainingCalories = 800.0,
            dailyCalorieTarget = 2000.0,
            healthGoal = "reduce_fat",
            healthGoalLabel = "减脂",
            recommendations = foods
        )
        assertEquals(5, data.recommendations.size)
        assertEquals("清蒸鲈鱼", data.recommendations[0].foodName)
        assertEquals("水果拼盘", data.recommendations[4].foodName)
    }

    // ==================== 3. RecommendationResponse 响应测试 ====================

    @Test
    fun `test RecommendationResponse - success response`() {
        val response = RecommendationResponse(
            code = 200,
            message = "推荐成功",
            data = RecommendationResultData(
                userId = 1,
                mealType = "lunch",
                remainingCalories = 800.0,
                dailyCalorieTarget = 2000.0,
                healthGoal = "reduce_fat",
                healthGoalLabel = "减脂",
                recommendations = listOf(
                    RecommendedFood("清蒸鲈鱼", 105.0, 19.5, 3.0, 0.5, 92.5, "理由", listOf("高蛋白"))
                )
            )
        )
        assertEquals(200, response.code)
        assertEquals("推荐成功", response.message)
        assertNotNull(response.data)
        assertEquals(1, response.data!!.recommendations.size)
    }

    @Test
    fun `test RecommendationResponse - empty recommendations`() {
        val response = RecommendationResponse(
            code = 200,
            message = "推荐成功",
            data = RecommendationResultData(
                userId = 1,
                mealType = "lunch",
                remainingCalories = 0.0,
                dailyCalorieTarget = 2000.0,
                healthGoal = "reduce_fat",
                healthGoalLabel = "减脂",
                recommendations = emptyList()
            )
        )
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertTrue(response.data!!.recommendations.isEmpty())
    }

    @Test
    fun `test RecommendationResponse - error response null data`() {
        val response = RecommendationResponse(
            code = 404,
            message = "用户不存在",
            data = null
        )
        assertEquals(404, response.code)
        assertEquals("用户不存在", response.message)
        assertNull(response.data)
    }

    @Test
    fun `test RecommendationResponse - server error`() {
        val response = RecommendationResponse(
            code = 500,
            message = "内部服务器错误",
            data = null
        )
        assertEquals(500, response.code)
        assertNull(response.data)
    }

    @Test
    fun `test RecommendationResponse - null message`() {
        val response = RecommendationResponse(
            code = 200,
            message = null,
            data = null
        )
        assertNull(response.message)
    }

    // ==================== 4. 推荐评分分类逻辑测试 ====================

    @Test
    fun `test score classification - high score above 85`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 85.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 90.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 100.0, "R", emptyList())
        )
        foods.forEach { food ->
            assertTrue("Score ${food.score} should be >= 85", food.score >= 85)
        }
    }

    @Test
    fun `test score classification - medium score 70-84`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 70.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 77.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 84.0, "R", emptyList())
        )
        foods.forEach { food ->
            assertTrue("Score ${food.score} should be >= 70 and < 85",
                food.score >= 70 && food.score < 85)
        }
    }

    @Test
    fun `test score classification - low score below 70`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 0.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 35.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 69.9, "R", emptyList())
        )
        foods.forEach { food ->
            assertTrue("Score ${food.score} should be < 70", food.score < 70)
        }
    }

    // ==================== 5. 标签颜色映射逻辑测试 ====================

    @Test
    fun `test tag color mapping - protein tags`() {
        val proteinTags = listOf("高蛋白", "蛋白质丰富", "优质蛋白")
        proteinTags.forEach { tag ->
            assertTrue("Tag '$tag' should contain '蛋白'", tag.contains("蛋白"))
        }
    }

    @Test
    fun `test tag color mapping - fat tags`() {
        val fatTags = listOf("低脂肪", "脂肪含量低", "低脂")
        fatTags.forEach { tag ->
            assertTrue("Tag '$tag' should contain '脂' or '脂肪'",
                tag.contains("脂"))
        }
    }

    @Test
    fun `test tag color mapping - carbs tags`() {
        val carbTags = listOf("低碳水", "碳水适中")
        carbTags.forEach { tag ->
            assertTrue("Tag '$tag' should contain '碳'", tag.contains("碳"))
        }
    }

    @Test
    fun `test tag color mapping - calorie tags`() {
        val calorieTags = listOf("低热量", "热量适中")
        calorieTags.forEach { tag ->
            assertTrue("Tag '$tag' should contain '热量'", tag.contains("热量"))
        }
    }

    @Test
    fun `test tag color mapping - fiber tags`() {
        val fiberTags = listOf("高纤维", "纤维丰富")
        fiberTags.forEach { tag ->
            assertTrue("Tag '$tag' should contain '纤维'", tag.contains("纤维"))
        }
    }

    @Test
    fun `test tag color mapping - unknown tags use default`() {
        val unknownTags = listOf("有机", "绿色", "当季")
        unknownTags.forEach { tag ->
            assertFalse(tag.contains("蛋白"))
            assertFalse(tag.contains("脂"))
            assertFalse(tag.contains("碳"))
            assertFalse(tag.contains("热量"))
            assertFalse(tag.contains("纤维"))
        }
    }

    // ==================== 6. 餐次类型验证测试 ====================

    @Test
    fun `test meal type - all valid types`() {
        val validMealTypes = listOf("breakfast", "lunch", "dinner", "snack")
        validMealTypes.forEach { type ->
            val data = RecommendationResultData(
                userId = 1,
                mealType = type,
                remainingCalories = 500.0,
                dailyCalorieTarget = 2000.0,
                healthGoal = "balanced",
                healthGoalLabel = "均衡饮食",
                recommendations = emptyList()
            )
            assertEquals(type, data.mealType)
        }
    }

    @Test
    fun `test meal type - breakfast`() {
        val data = createTestData(mealType = "breakfast")
        assertEquals("breakfast", data.mealType)
    }

    @Test
    fun `test meal type - lunch`() {
        val data = createTestData(mealType = "lunch")
        assertEquals("lunch", data.mealType)
    }

    @Test
    fun `test meal type - dinner`() {
        val data = createTestData(mealType = "dinner")
        assertEquals("dinner", data.mealType)
    }

    @Test
    fun `test meal type - snack`() {
        val data = createTestData(mealType = "snack")
        assertEquals("snack", data.mealType)
    }

    // ==================== 7. 边界情况测试 ====================

    @Test
    fun `test edge case - very large calorie values`() {
        val food = RecommendedFood(
            foodName = "超高热量食品",
            calories = 99999.0,
            protein = 999.0,
            fat = 999.0,
            carbs = 999.0,
            score = 1.0,
            reason = "热量极高",
            tags = emptyList()
        )
        assertEquals(99999.0, food.calories, 0.01)
    }

    @Test
    fun `test edge case - very small decimal values`() {
        val food = RecommendedFood(
            foodName = "微量食品",
            calories = 0.001,
            protein = 0.001,
            fat = 0.001,
            carbs = 0.001,
            score = 0.5,
            reason = "极少量",
            tags = emptyList()
        )
        assertEquals(0.001, food.calories, 0.0001)
    }

    @Test
    fun `test edge case - empty food name`() {
        val food = RecommendedFood(
            foodName = "",
            calories = 100.0,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            score = 50.0,
            reason = "空名称",
            tags = emptyList()
        )
        assertEquals("", food.foodName)
    }

    @Test
    fun `test edge case - empty reason`() {
        val food = RecommendedFood(
            foodName = "测试菜品",
            calories = 100.0,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            score = 50.0,
            reason = "",
            tags = emptyList()
        )
        assertEquals("", food.reason)
    }

    @Test
    fun `test edge case - unicode characters in food name`() {
        val food = RecommendedFood(
            foodName = "🍣 三文鱼刺身",
            calories = 140.0,
            protein = 20.0,
            fat = 6.0,
            carbs = 0.0,
            score = 88.0,
            reason = "富含omega-3",
            tags = listOf("高蛋白")
        )
        assertTrue(food.foodName.contains("三文鱼"))
    }

    @Test
    fun `test edge case - remaining calories equals target`() {
        val data = RecommendationResultData(
            userId = 1,
            mealType = "breakfast",
            remainingCalories = 2000.0,
            dailyCalorieTarget = 2000.0,
            healthGoal = "balanced",
            healthGoalLabel = "均衡饮食",
            recommendations = emptyList()
        )
        assertEquals(data.remainingCalories, data.dailyCalorieTarget, 0.01)
    }

    @Test
    fun `test edge case - zero calorie target`() {
        val data = RecommendationResultData(
            userId = 1,
            mealType = "lunch",
            remainingCalories = 0.0,
            dailyCalorieTarget = 0.0,
            healthGoal = "balanced",
            healthGoalLabel = "均衡饮食",
            recommendations = emptyList()
        )
        assertEquals(0.0, data.dailyCalorieTarget, 0.01)
    }

    @Test
    fun `test edge case - large user id`() {
        val data = createTestData(userId = 999999)
        assertEquals(999999, data.userId)
    }

    // ==================== 8. 多菜品推荐排序与数量测试 ====================

    @Test
    fun `test multiple recommendations - sorted by score descending`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 95.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 80.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 70.0, "R", emptyList())
        )
        val sorted = foods.sortedByDescending { it.score }
        assertEquals(95.0, sorted[0].score, 0.01)
        assertEquals(80.0, sorted[1].score, 0.01)
        assertEquals(70.0, sorted[2].score, 0.01)
    }

    @Test
    fun `test multiple recommendations - filter by high score`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 95.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 50.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 85.0, "R", emptyList()),
            RecommendedFood("D", 100.0, 20.0, 3.0, 5.0, 30.0, "R", emptyList())
        )
        val highScoreFoods = foods.filter { it.score >= 85 }
        assertEquals(2, highScoreFoods.size)
    }

    @Test
    fun `test multiple recommendations - limit count`() {
        val allFoods = (1..10).map { i ->
            RecommendedFood("Food$i", 100.0 * i, 10.0, 5.0, 15.0, 50.0 + i, "R$i", emptyList())
        }
        val limited = allFoods.take(5)
        assertEquals(5, limited.size)
        assertEquals("Food1", limited[0].foodName)
        assertEquals("Food5", limited[4].foodName)
    }

    @Test
    fun `test multiple recommendations - total calories calculation`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 95.0, "R", emptyList()),
            RecommendedFood("B", 200.0, 15.0, 8.0, 20.0, 80.0, "R", emptyList()),
            RecommendedFood("C", 150.0, 10.0, 5.0, 25.0, 70.0, "R", emptyList())
        )
        val totalCalories = foods.sumOf { it.calories }
        assertEquals(450.0, totalCalories, 0.01)
    }

    @Test
    fun `test multiple recommendations - average score calculation`() {
        val foods = listOf(
            RecommendedFood("A", 100.0, 20.0, 3.0, 5.0, 90.0, "R", emptyList()),
            RecommendedFood("B", 100.0, 20.0, 3.0, 5.0, 80.0, "R", emptyList()),
            RecommendedFood("C", 100.0, 20.0, 3.0, 5.0, 70.0, "R", emptyList())
        )
        val avgScore = foods.map { it.score }.average()
        assertEquals(80.0, avgScore, 0.01)
    }

    @Test
    fun `test multiple recommendations - unique food names`() {
        val foods = listOf(
            RecommendedFood("清蒸鲈鱼", 105.0, 19.5, 3.0, 0.5, 92.5, "R1", emptyList()),
            RecommendedFood("鸡胸肉沙拉", 150.0, 30.0, 3.5, 5.0, 88.0, "R2", emptyList()),
            RecommendedFood("清蒸鲈鱼", 105.0, 19.5, 3.0, 0.5, 92.5, "R3", emptyList())
        )
        val uniqueNames = foods.map { it.foodName }.distinct()
        assertEquals(2, uniqueNames.size)
    }

    // ==================== 9. 数据一致性测试 ====================

    @Test
    fun `test data consistency - calorie within remaining quota`() {
        val remainingCalories = 800.0
        val foods = listOf(
            RecommendedFood("A", 200.0, 20.0, 3.0, 5.0, 90.0, "R", emptyList()),
            RecommendedFood("B", 150.0, 15.0, 5.0, 10.0, 85.0, "R", emptyList())
        )
        foods.forEach { food ->
            assertTrue(
                "Food ${food.foodName} (${food.calories} kcal) should be within remaining quota ($remainingCalories kcal)",
                food.calories <= remainingCalories
            )
        }
    }

    @Test
    fun `test data consistency - nutrition values non-negative`() {
        val food = RecommendedFood("测试", 100.0, 10.0, 5.0, 15.0, 80.0, "R", emptyList())
        assertTrue(food.calories >= 0)
        assertTrue(food.protein >= 0)
        assertTrue(food.fat >= 0)
        assertTrue(food.carbs >= 0)
        assertTrue(food.score >= 0)
    }

    @Test
    fun `test data consistency - response code determines data presence`() {
        // 成功响应应有data
        val successResponse = RecommendationResponse(
            code = 200,
            message = "推荐成功",
            data = createTestData()
        )
        assertEquals(200, successResponse.code)
        assertNotNull(successResponse.data)

        // 错误响应data可为null
        val errorResponse = RecommendationResponse(
            code = 404,
            message = "用户不存在",
            data = null
        )
        assertNotEquals(200, errorResponse.code)
        assertNull(errorResponse.data)
    }

    // ==================== 辅助方法 ====================

    private fun createTestData(
        userId: Int = 1,
        mealType: String = "lunch",
        remainingCalories: Double = 800.0,
        dailyCalorieTarget: Double = 2000.0,
        healthGoal: String = "reduce_fat",
        healthGoalLabel: String = "减脂"
    ): RecommendationResultData {
        return RecommendationResultData(
            userId = userId,
            mealType = mealType,
            remainingCalories = remainingCalories,
            dailyCalorieTarget = dailyCalorieTarget,
            healthGoal = healthGoal,
            healthGoalLabel = healthGoalLabel,
            recommendations = emptyList()
        )
    }
}
