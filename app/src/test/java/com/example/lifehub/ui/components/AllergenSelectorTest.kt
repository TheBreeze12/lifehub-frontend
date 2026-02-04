package com.example.lifehub.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * AllergenSelector组件测试
 * 测试Phase 9实现的过敏原档案配置功能
 */
class AllergenSelectorTest {

    // ==================== 八大类过敏原常量测试 ====================

    @Test
    fun `test EIGHT_MAJOR_ALLERGENS contains exactly 8 categories`() {
        assertEquals(8, AllergenCategories.EIGHT_MAJOR_ALLERGENS.size)
    }

    @Test
    fun `test all allergen categories have required fields`() {
        AllergenCategories.EIGHT_MAJOR_ALLERGENS.forEach { category ->
            assertTrue("Category code should not be empty", category.code.isNotEmpty())
            assertTrue("Category name should not be empty", category.name.isNotEmpty())
            assertTrue("Category nameEn should not be empty", category.nameEn.isNotEmpty())
            assertTrue("Category description should not be empty", category.description.isNotEmpty())
            assertTrue("Category icon should not be empty", category.icon.isNotEmpty())
        }
    }

    @Test
    fun `test allergen codes match backend allergen_service`() {
        val expectedCodes = setOf("milk", "egg", "fish", "shellfish", "peanut", "tree_nut", "wheat", "soy")
        val actualCodes = AllergenCategories.EIGHT_MAJOR_ALLERGENS.map { it.code }.toSet()
        assertEquals(expectedCodes, actualCodes)
    }

    @Test
    fun `test allergen Chinese names`() {
        val expectedNames = setOf("乳制品", "鸡蛋", "鱼类", "甲壳类", "花生", "树坚果", "小麦", "大豆")
        val actualNames = AllergenCategories.EIGHT_MAJOR_ALLERGENS.map { it.name }.toSet()
        assertEquals(expectedNames, actualNames)
    }

    // ==================== getByCode 方法测试 ====================

    @Test
    fun `test getByCode with valid lowercase code`() {
        val result = AllergenCategories.getByCode("milk")
        assertNotNull(result)
        assertEquals("乳制品", result?.name)
    }

    @Test
    fun `test getByCode with valid uppercase code`() {
        val result = AllergenCategories.getByCode("MILK")
        assertNotNull(result)
        assertEquals("乳制品", result?.name)
    }

    @Test
    fun `test getByCode with valid mixed case code`() {
        val result = AllergenCategories.getByCode("Peanut")
        assertNotNull(result)
        assertEquals("花生", result?.name)
    }

    @Test
    fun `test getByCode with invalid code returns null`() {
        val result = AllergenCategories.getByCode("unknown")
        assertNull(result)
    }

    @Test
    fun `test getByCode with empty string returns null`() {
        val result = AllergenCategories.getByCode("")
        assertNull(result)
    }

    @Test
    fun `test getByCode for all eight categories`() {
        val testCases = mapOf(
            "milk" to "乳制品",
            "egg" to "鸡蛋",
            "fish" to "鱼类",
            "shellfish" to "甲壳类",
            "peanut" to "花生",
            "tree_nut" to "树坚果",
            "wheat" to "小麦",
            "soy" to "大豆"
        )
        
        testCases.forEach { (code, expectedName) ->
            val result = AllergenCategories.getByCode(code)
            assertNotNull("Should find allergen for code: $code", result)
            assertEquals("Name mismatch for code: $code", expectedName, result?.name)
        }
    }

    // ==================== getCodeByName 方法测试 ====================

    @Test
    fun `test getCodeByName with valid Chinese name`() {
        val result = AllergenCategories.getCodeByName("乳制品")
        assertEquals("milk", result)
    }

    @Test
    fun `test getCodeByName with invalid name returns null`() {
        val result = AllergenCategories.getCodeByName("未知过敏原")
        assertNull(result)
    }

    @Test
    fun `test getCodeByName for all eight categories`() {
        val testCases = mapOf(
            "乳制品" to "milk",
            "鸡蛋" to "egg",
            "鱼类" to "fish",
            "甲壳类" to "shellfish",
            "花生" to "peanut",
            "树坚果" to "tree_nut",
            "小麦" to "wheat",
            "大豆" to "soy"
        )
        
        testCases.forEach { (name, expectedCode) ->
            val result = AllergenCategories.getCodeByName(name)
            assertEquals("Code mismatch for name: $name", expectedCode, result)
        }
    }

    // ==================== isMajorAllergen 方法测试 ====================

    @Test
    fun `test isMajorAllergen with code returns true`() {
        assertTrue(AllergenCategories.isMajorAllergen("milk"))
        assertTrue(AllergenCategories.isMajorAllergen("egg"))
        assertTrue(AllergenCategories.isMajorAllergen("peanut"))
    }

    @Test
    fun `test isMajorAllergen with Chinese name returns true`() {
        assertTrue(AllergenCategories.isMajorAllergen("乳制品"))
        assertTrue(AllergenCategories.isMajorAllergen("鸡蛋"))
        assertTrue(AllergenCategories.isMajorAllergen("花生"))
    }

    @Test
    fun `test isMajorAllergen with custom allergen returns false`() {
        assertFalse(AllergenCategories.isMajorAllergen("芒果"))
        assertFalse(AllergenCategories.isMajorAllergen("蜂蜜"))
        assertFalse(AllergenCategories.isMajorAllergen("酒精"))
    }

    @Test
    fun `test isMajorAllergen is case insensitive for codes`() {
        assertTrue(AllergenCategories.isMajorAllergen("MILK"))
        assertTrue(AllergenCategories.isMajorAllergen("Egg"))
        assertTrue(AllergenCategories.isMajorAllergen("PEANUT"))
    }

    // ==================== getAllergenDisplayText 方法测试 ====================

    @Test
    fun `test getAllergenDisplayText with empty list`() {
        val result = getAllergenDisplayText(emptyList())
        assertEquals("未设置", result)
    }

    @Test
    fun `test getAllergenDisplayText with one allergen`() {
        val result = getAllergenDisplayText(listOf("乳制品"))
        assertEquals("乳制品", result)
    }

    @Test
    fun `test getAllergenDisplayText with two allergens`() {
        val result = getAllergenDisplayText(listOf("乳制品", "鸡蛋"))
        assertEquals("乳制品、鸡蛋", result)
    }

    @Test
    fun `test getAllergenDisplayText with three allergens`() {
        val result = getAllergenDisplayText(listOf("乳制品", "鸡蛋", "花生"))
        assertEquals("乳制品、鸡蛋、花生", result)
    }

    @Test
    fun `test getAllergenDisplayText with more than three allergens`() {
        val result = getAllergenDisplayText(listOf("乳制品", "鸡蛋", "花生", "鱼类", "大豆"))
        assertEquals("乳制品、鸡蛋、花生 等5项", result)
    }

    @Test
    fun `test getAllergenDisplayText with custom allergens`() {
        val result = getAllergenDisplayText(listOf("芒果", "猕猴桃"))
        assertEquals("芒果、猕猴桃", result)
    }

    @Test
    fun `test getAllergenDisplayText with mixed allergens`() {
        val result = getAllergenDisplayText(listOf("乳制品", "芒果", "鸡蛋"))
        assertEquals("乳制品、芒果、鸡蛋", result)
    }

    // ==================== 数据类测试 ====================

    @Test
    fun `test AllergenCategory data class equals`() {
        val category1 = AllergenCategory(
            code = "milk",
            name = "乳制品",
            nameEn = "Milk",
            description = "牛奶及其制品",
            icon = "🥛"
        )
        val category2 = AllergenCategory(
            code = "milk",
            name = "乳制品",
            nameEn = "Milk",
            description = "牛奶及其制品",
            icon = "🥛"
        )
        assertEquals(category1, category2)
    }

    @Test
    fun `test AllergenCategory data class copy`() {
        val original = AllergenCategories.EIGHT_MAJOR_ALLERGENS.first()
        val copy = original.copy(name = "测试名称")
        
        assertEquals(original.code, copy.code)
        assertNotEquals(original.name, copy.name)
        assertEquals("测试名称", copy.name)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `test allergen icons are valid emoji`() {
        AllergenCategories.EIGHT_MAJOR_ALLERGENS.forEach { category ->
            // 简单验证图标不为空且是单个字符或表情
            assertTrue(
                "Icon for ${category.name} should be valid",
                category.icon.isNotEmpty() && category.icon.length <= 4
            )
        }
    }

    @Test
    fun `test allergen descriptions contain relevant keywords`() {
        val milkCategory = AllergenCategories.getByCode("milk")
        assertTrue(milkCategory?.description?.contains("奶") == true)
        
        val eggCategory = AllergenCategories.getByCode("egg")
        assertTrue(eggCategory?.description?.contains("蛋") == true)
        
        val fishCategory = AllergenCategories.getByCode("fish")
        assertTrue(fishCategory?.description?.contains("鱼") == true)
    }

    // ==================== 用户场景测试 ====================

    @Test
    fun `test user selects multiple major allergens scenario`() {
        val userSelection = listOf("乳制品", "鸡蛋", "花生")
        
        // 验证所有选择都是有效的八大类过敏原
        userSelection.forEach { allergen ->
            assertTrue(
                "$allergen should be a major allergen",
                AllergenCategories.isMajorAllergen(allergen)
            )
        }
        
        // 验证显示文本正确
        val displayText = getAllergenDisplayText(userSelection)
        assertEquals("乳制品、鸡蛋、花生", displayText)
    }

    @Test
    fun `test user adds custom allergen scenario`() {
        val customAllergen = "芒果"
        
        // 自定义过敏原不应该是八大类
        assertFalse(AllergenCategories.isMajorAllergen(customAllergen))
        
        // 但应该可以正常显示
        val displayText = getAllergenDisplayText(listOf(customAllergen))
        assertEquals("芒果", displayText)
    }

    @Test
    fun `test user combines major and custom allergens scenario`() {
        val combinedSelection = listOf("乳制品", "芒果", "花生", "猕猴桃")
        
        // 验证混合列表的显示
        val displayText = getAllergenDisplayText(combinedSelection)
        assertEquals("乳制品、芒果、花生 等4项", displayText)
    }

    @Test
    fun `test allergen code to name conversion for API integration`() {
        // 模拟从后端接收过敏原代码并转换为显示名称
        val backendCodes = listOf("milk", "egg", "peanut")
        
        val displayNames = backendCodes.map { code ->
            AllergenCategories.getByCode(code)?.name ?: code
        }
        
        assertEquals(listOf("乳制品", "鸡蛋", "花生"), displayNames)
    }

    @Test
    fun `test allergen name to code conversion for API submission`() {
        // 模拟将用户选择的中文名称转换为代码发送给后端
        val userSelection = listOf("乳制品", "鸡蛋", "花生")
        
        val codes = userSelection.mapNotNull { name ->
            AllergenCategories.getCodeByName(name)
        }
        
        assertEquals(listOf("milk", "egg", "peanut"), codes)
    }
}
