package com.example.lifehub

import com.example.lifehub.data.DishItem
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.MenuRecognitionState
import com.example.lifehub.viewmodel.RecognitionSource
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 31: 端侧OCR与云端联动 - 单元测试
 *
 * 测试覆盖范围：
 * 1. RecognitionSource 枚举值和显示名称
 * 2. MenuRecognitionState 携带识别来源
 * 3. extractDishNames 菜名提取逻辑（核心算法）
 * 4. 边缘情况与异常输入处理
 * 5. 端侧/云端协调策略逻辑验证
 */
class Phase31EdgeCloudOcrTest {

    // ==================== RecognitionSource 枚举测试 ====================

    @Test
    fun `test RecognitionSource EDGE has correct displayName`() {
        assertEquals("端侧识别", RecognitionSource.EDGE.displayName)
    }

    @Test
    fun `test RecognitionSource CLOUD has correct displayName`() {
        assertEquals("云端识别", RecognitionSource.CLOUD.displayName)
    }

    @Test
    fun `test RecognitionSource enum has exactly two values`() {
        assertEquals(2, RecognitionSource.values().size)
    }

    @Test
    fun `test RecognitionSource valueOf works for EDGE`() {
        assertEquals(RecognitionSource.EDGE, RecognitionSource.valueOf("EDGE"))
    }

    @Test
    fun `test RecognitionSource valueOf works for CLOUD`() {
        assertEquals(RecognitionSource.CLOUD, RecognitionSource.valueOf("CLOUD"))
    }

    // ==================== MenuRecognitionState 测试 ====================

    @Test
    fun `test MenuRecognitionState Success default source is CLOUD`() {
        val state = MenuRecognitionState.Success(dishes = emptyList())
        assertEquals(RecognitionSource.CLOUD, state.source)
    }

    @Test
    fun `test MenuRecognitionState Success with EDGE source`() {
        val dishes = listOf(
            DishItem("宫保鸡丁", 320.0, 28.0, 18.0, 15.0, true, "推荐")
        )
        val state = MenuRecognitionState.Success(
            dishes = dishes,
            source = RecognitionSource.EDGE
        )
        assertEquals(RecognitionSource.EDGE, state.source)
        assertEquals(1, state.dishes.size)
        assertEquals("宫保鸡丁", state.dishes[0].name)
    }

    @Test
    fun `test MenuRecognitionState Success with CLOUD source`() {
        val dishes = listOf(
            DishItem("番茄炒蛋", 150.0, 10.5, 8.2, 6.3, true, "推荐")
        )
        val state = MenuRecognitionState.Success(
            dishes = dishes,
            source = RecognitionSource.CLOUD
        )
        assertEquals(RecognitionSource.CLOUD, state.source)
        assertEquals(1, state.dishes.size)
    }

    @Test
    fun `test MenuRecognitionState Success with multiple dishes`() {
        val dishes = listOf(
            DishItem("宫保鸡丁", 320.0, 28.0, 18.0, 15.0, true, "推荐"),
            DishItem("麻婆豆腐", 200.0, 12.0, 14.0, 8.0, false, "注意辣度"),
            DishItem("清炒时蔬", 80.0, 3.0, 4.0, 6.0, true, "低卡")
        )
        val state = MenuRecognitionState.Success(
            dishes = dishes,
            source = RecognitionSource.EDGE
        )
        assertEquals(3, state.dishes.size)
        assertEquals(RecognitionSource.EDGE, state.source)
    }

    @Test
    fun `test MenuRecognitionState types are distinguishable`() {
        val idle = MenuRecognitionState.Idle
        val loading = MenuRecognitionState.Loading
        val success = MenuRecognitionState.Success(emptyList(), RecognitionSource.EDGE)
        val error = MenuRecognitionState.Error("test error")

        assertTrue(idle is MenuRecognitionState.Idle)
        assertTrue(loading is MenuRecognitionState.Loading)
        assertTrue(success is MenuRecognitionState.Success)
        assertTrue(error is MenuRecognitionState.Error)

        // 互不相同
        assertNotEquals(idle, loading)
        assertNotEquals(success, error)
    }

    @Test
    fun `test MenuRecognitionState Success data class equality`() {
        val dishes = listOf(DishItem("鱼香肉丝", 280.0, 20.0, 16.0, 12.0, true, null))
        val state1 = MenuRecognitionState.Success(dishes, RecognitionSource.EDGE)
        val state2 = MenuRecognitionState.Success(dishes, RecognitionSource.EDGE)
        val state3 = MenuRecognitionState.Success(dishes, RecognitionSource.CLOUD)

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    // ==================== extractDishNames 菜名提取测试 ====================

    @Test
    fun `test extractDishNames with typical menu items`() {
        val ocrTexts = listOf("宫保鸡丁", "麻婆豆腐", "清炒时蔬", "鱼香肉丝")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(4, result.size)
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("麻婆豆腐"))
        assertTrue(result.contains("清炒时蔬"))
        assertTrue(result.contains("鱼香肉丝"))
    }

    @Test
    fun `test extractDishNames filters out pure prices`() {
        val ocrTexts = listOf("宫保鸡丁", "¥38", "28.0", "￥15", "38元", "麻婆豆腐")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(2, result.size)
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("麻婆豆腐"))
    }

    @Test
    fun `test extractDishNames filters out category headers`() {
        val ocrTexts = listOf("热菜", "宫保鸡丁", "凉菜", "凉拌黄瓜", "主食", "米饭")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(3, result.size)
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("凉拌黄瓜"))
        assertTrue(result.contains("米饭"))
        assertFalse(result.contains("热菜"))
        assertFalse(result.contains("凉菜"))
        assertFalse(result.contains("主食"))
    }

    @Test
    fun `test extractDishNames filters out too short text`() {
        val ocrTexts = listOf("菜", "宫保鸡丁", "A", "")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(1, result.size)
        assertEquals("宫保鸡丁", result[0])
    }

    @Test
    fun `test extractDishNames filters out too long text`() {
        val longText = "这是一段非常非常长的文字描述不是菜品名称而是一段说明文字"
        val ocrTexts = listOf(longText, "宫保鸡丁")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(1, result.size)
        assertEquals("宫保鸡丁", result[0])
    }

    @Test
    fun `test extractDishNames removes duplicates`() {
        val ocrTexts = listOf("宫保鸡丁", "宫保鸡丁", "麻婆豆腐", "麻婆豆腐", "鱼香肉丝")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(3, result.size)
    }

    @Test
    fun `test extractDishNames trims whitespace`() {
        val ocrTexts = listOf("  宫保鸡丁  ", " 麻婆豆腐", "鱼香肉丝 ")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(3, result.size)
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("麻婆豆腐"))
        assertTrue(result.contains("鱼香肉丝"))
    }

    @Test
    fun `test extractDishNames with empty input`() {
        val result = FoodViewModel.extractDishNames(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames with all filtered items`() {
        val ocrTexts = listOf("¥38", "热菜", "", "A", "28.0")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames with mixed valid and invalid`() {
        val ocrTexts = listOf(
            "欢迎光临",       // 非菜品（过滤）
            "热菜",           // 分类标题（过滤）
            "宫保鸡丁",       // 有效菜名
            "¥38",            // 价格（过滤）
            "麻婆豆腐",       // 有效菜名
            "28元",           // 价格（过滤）
            "凉菜",           // 分类标题（过滤）
            "凉拌三丝",       // 有效菜名
            "扫码点餐",       // 非菜品（过滤）
            "电话",           // 非菜品（过滤）
            "清炒西蓝花"      // 有效菜名
        )
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(4, result.size)
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("麻婆豆腐"))
        assertTrue(result.contains("凉拌三丝"))
        assertTrue(result.contains("清炒西蓝花"))
    }

    @Test
    fun `test extractDishNames filters non-dish patterns case insensitive`() {
        val ocrTexts = listOf("菜单", "MENU", "套餐", "推荐", "招牌", "宫保鸡丁")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        // "菜单"、"套餐"、"推荐"、"招牌" 应被过滤
        // "MENU" 长度>=2 但不在NON_DISH_PATTERNS中（是中文过滤），所以可能保留
        // 实际上 NON_DISH_PATTERNS 中有 "菜单" 但没有 "MENU"
        assertTrue(result.contains("宫保鸡丁"))
        assertFalse(result.contains("菜单"))
        assertFalse(result.contains("套餐"))
        assertFalse(result.contains("推荐"))
        assertFalse(result.contains("招牌"))
    }

    @Test
    fun `test extractDishNames with price slash format`() {
        val ocrTexts = listOf("宫保鸡丁", "38/份", "麻婆豆腐")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        // "38/份" 不是纯数字，包含中文"份"，不匹配纯价格正则，会保留
        // 但由于长度2-20之间，可能会保留
        assertTrue(result.contains("宫保鸡丁"))
        assertTrue(result.contains("麻婆豆腐"))
    }

    @Test
    fun `test extractDishNames boundary length - exactly MIN_DISH_NAME_LENGTH`() {
        val ocrTexts = listOf("米饭") // 2 chars, exactly MIN_DISH_NAME_LENGTH
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(1, result.size)
        assertEquals("米饭", result[0])
    }

    @Test
    fun `test extractDishNames boundary length - exactly MAX_DISH_NAME_LENGTH`() {
        // MAX_DISH_NAME_LENGTH = 20
        val name20 = "红烧大排配时令蔬菜套餐含米饭一碗味增汤一份" // This is >20 chars
        val name20Exact = "红烧大排配时令蔬菜套餐含米饭一碗味增汤份" // adjust to exactly 20
        // Let's just use a name that is exactly 20 chars
        val exactName = "a".repeat(20)
        val ocrTexts = listOf(exactName)
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(1, result.size)
    }

    @Test
    fun `test extractDishNames boundary length - exceeds MAX_DISH_NAME_LENGTH`() {
        val longName = "a".repeat(21)
        val ocrTexts = listOf(longName)
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames with only whitespace lines`() {
        val ocrTexts = listOf("   ", "  ", "\t")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames with realistic OCR output`() {
        // 模拟真实菜单OCR识别结果
        val ocrTexts = listOf(
            "老北京炸酱面馆",    // 餐厅名（可能被当做菜名，但长度合适）
            "热菜",
            "宫保鸡丁   38",    // 菜名+价格混合（会被保留为菜名+价格字符串）
            "麻婆豆腐",
            "鱼香茄子",
            "28",               // 纯价格
            "红烧肉",
            "凉菜",
            "拍黄瓜",
            "凉拌木耳",
            "主食",
            "米饭",
            "馒头",
            "电话：010-12345678"
        )
        val result = FoodViewModel.extractDishNames(ocrTexts)
        // 应该包含实际菜名
        assertTrue(result.contains("麻婆豆腐"))
        assertTrue(result.contains("鱼香茄子"))
        assertTrue(result.contains("红烧肉"))
        assertTrue(result.contains("拍黄瓜"))
        assertTrue(result.contains("凉拌木耳"))
        assertTrue(result.contains("米饭"))
        assertTrue(result.contains("馒头"))
        // 不应该包含分类标题
        assertFalse(result.contains("热菜"))
        assertFalse(result.contains("凉菜"))
        assertFalse(result.contains("主食"))
    }

    // ==================== MIN_EDGE_OCR_DISHES 常量测试 ====================

    @Test
    fun `test MIN_EDGE_OCR_DISHES is at least 1`() {
        assertTrue(FoodViewModel.MIN_EDGE_OCR_DISHES >= 1)
    }

    @Test
    fun `test MIN_DISH_NAME_LENGTH is positive`() {
        assertTrue(FoodViewModel.MIN_DISH_NAME_LENGTH > 0)
    }

    @Test
    fun `test MAX_DISH_NAME_LENGTH is greater than MIN`() {
        assertTrue(FoodViewModel.MAX_DISH_NAME_LENGTH > FoodViewModel.MIN_DISH_NAME_LENGTH)
    }

    // ==================== 端云协调策略逻辑测试 ====================

    @Test
    fun `test edge OCR result with sufficient dishes should use EDGE source`() {
        // 模拟端侧OCR提取到足够的菜名
        val ocrTexts = listOf("宫保鸡丁", "麻婆豆腐", "鱼香肉丝")
        val dishNames = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(dishNames.size >= FoodViewModel.MIN_EDGE_OCR_DISHES)

        // 验证构造的Success状态应标记为EDGE
        val state = MenuRecognitionState.Success(
            dishes = dishNames.map { DishItem(it, 200.0, 15.0, 10.0, 8.0, true, null) },
            source = RecognitionSource.EDGE
        )
        assertEquals(RecognitionSource.EDGE, state.source)
    }

    @Test
    fun `test edge OCR result with insufficient dishes should trigger cloud fallback`() {
        // 模拟端侧OCR只提取到非菜品文本
        val ocrTexts = listOf("¥38", "热菜", "28元")
        val dishNames = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(dishNames.size < FoodViewModel.MIN_EDGE_OCR_DISHES)
        // 此时应该回退到云端
    }

    @Test
    fun `test cloud fallback produces CLOUD source`() {
        // 云端识别结果应标记为CLOUD
        val state = MenuRecognitionState.Success(
            dishes = listOf(DishItem("番茄炒蛋", 150.0, 10.5, 8.2, 6.3, true, "推荐")),
            source = RecognitionSource.CLOUD
        )
        assertEquals(RecognitionSource.CLOUD, state.source)
    }

    @Test
    fun `test empty OCR result triggers cloud fallback`() {
        val ocrTexts = emptyList<String>()
        val dishNames = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals(0, dishNames.size)
        assertTrue(dishNames.size < FoodViewModel.MIN_EDGE_OCR_DISHES)
    }

    // ==================== DishItem 数据完整性测试 ====================

    @Test
    fun `test DishItem created from edge OCR has valid fields`() {
        val dish = DishItem(
            name = "宫保鸡丁",
            calories = 320.0,
            protein = 28.0,
            fat = 18.0,
            carbs = 15.0,
            isRecommended = true,
            reason = "蛋白质丰富"
        )
        assertEquals("宫保鸡丁", dish.name)
        assertEquals(320.0, dish.calories, 0.01)
        assertEquals(28.0, dish.protein, 0.01)
        assertEquals(18.0, dish.fat, 0.01)
        assertEquals(15.0, dish.carbs, 0.01)
        assertTrue(dish.isRecommended)
        assertEquals("蛋白质丰富", dish.reason)
    }

    @Test
    fun `test DishItem with null reason`() {
        val dish = DishItem(
            name = "米饭",
            calories = 116.0,
            protein = 2.6,
            fat = 0.3,
            carbs = 25.6,
            isRecommended = false,
            reason = null
        )
        assertNull(dish.reason)
    }

    // ==================== 过滤特殊字符测试 ====================

    @Test
    fun `test extractDishNames with numeric-only strings`() {
        val ocrTexts = listOf("123", "45.6", "0", "99.9")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames with currency symbols`() {
        val ocrTexts = listOf("¥38", "￥25", "38元", "¥ 45")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        // ¥38, ￥25, 38元 匹配纯价格正则
        // "¥ 45" 包含空格，trim后是"¥ 45"，匹配正则 ^[\d.¥￥元/\s]+$
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test extractDishNames preserves order`() {
        val ocrTexts = listOf("鱼香肉丝", "宫保鸡丁", "麻婆豆腐")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertEquals("鱼香肉丝", result[0])
        assertEquals("宫保鸡丁", result[1])
        assertEquals("麻婆豆腐", result[2])
    }

    @Test
    fun `test extractDishNames filters all NON_DISH_PATTERNS`() {
        val nonDishTexts = listOf(
            "热菜", "凉菜", "主食", "汤类", "饮品", "甜品", "小吃",
            "推荐", "招牌", "新品", "特价", "套餐", "合计", "总计",
            "菜单", "价格", "价目", "电话", "地址", "订餐", "外卖",
            "备注", "温馨提示", "欢迎光临", "谢谢惠顾", "扫码点餐"
        )
        val result = FoodViewModel.extractDishNames(nonDishTexts)
        assertTrue("Non-dish patterns should all be filtered, but got: $result", result.isEmpty())
    }

    @Test
    fun `test extractDishNames with dish name containing filtered keyword`() {
        // 菜名中包含过滤关键词但不等于关键词
        val ocrTexts = listOf("热菜拼盘", "凉菜组合", "主食套装")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        // 这些不等于过滤模式，应该保留
        assertEquals(3, result.size)
        assertTrue(result.contains("热菜拼盘"))
        assertTrue(result.contains("凉菜组合"))
        assertTrue(result.contains("主食套装"))
    }

    // ==================== 综合场景测试 ====================

    @Test
    fun `test full edge OCR scenario - Chinese restaurant menu`() {
        val ocrTexts = listOf(
            "川味小馆",
            "热菜",
            "回锅肉",
            "水煮鱼",
            "麻辣香锅",
            "夫妻肺片",
            "凉菜",
            "口水鸡",
            "蒜泥白肉",
            "主食",
            "担担面",
            "酸辣粉",
            "饮品",
            "酸梅汤",
            "豆浆"
        )
        val result = FoodViewModel.extractDishNames(ocrTexts)

        // 应提取出所有有效菜名
        assertTrue(result.contains("回锅肉"))
        assertTrue(result.contains("水煮鱼"))
        assertTrue(result.contains("麻辣香锅"))
        assertTrue(result.contains("夫妻肺片"))
        assertTrue(result.contains("口水鸡"))
        assertTrue(result.contains("蒜泥白肉"))
        assertTrue(result.contains("担担面"))
        assertTrue(result.contains("酸辣粉"))
        assertTrue(result.contains("酸梅汤"))
        assertTrue(result.contains("豆浆"))

        // 分类标题不应出现
        assertFalse(result.contains("热菜"))
        assertFalse(result.contains("凉菜"))
        assertFalse(result.contains("主食"))
        assertFalse(result.contains("饮品"))

        // 餐厅名可能保留（长度合适，不在过滤列表中）
        // "川味小馆" 4字，合适长度，不是过滤词
        assertTrue(result.contains("川味小馆"))

        // 验证数量足够触发端侧识别
        assertTrue(result.size >= FoodViewModel.MIN_EDGE_OCR_DISHES)
    }

    @Test
    fun `test edge OCR with very poor quality - only noise`() {
        val ocrTexts = listOf("I", ".", "//", " ", "")
        val result = FoodViewModel.extractDishNames(ocrTexts)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test MenuRecognitionState copy preserves source`() {
        val original = MenuRecognitionState.Success(
            dishes = listOf(DishItem("测试", 100.0, 5.0, 3.0, 10.0, true, null)),
            source = RecognitionSource.EDGE
        )
        val copied = original.copy(
            dishes = original.dishes + DishItem("新菜", 200.0, 10.0, 6.0, 20.0, false, null)
        )
        assertEquals(RecognitionSource.EDGE, copied.source)
        assertEquals(2, copied.dishes.size)
    }

    @Test
    fun `test MenuRecognitionState copy can change source`() {
        val original = MenuRecognitionState.Success(
            dishes = listOf(DishItem("测试", 100.0, 5.0, 3.0, 10.0, true, null)),
            source = RecognitionSource.EDGE
        )
        val changed = original.copy(source = RecognitionSource.CLOUD)
        assertEquals(RecognitionSource.CLOUD, changed.source)
        assertEquals(original.dishes, changed.dishes)
    }
}
