package com.example.lifehub.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 8: 过敏原预警功能测试
 * 测试过敏原匹配、显示名称转换等核心逻辑
 */
class AllergenWarningTest {

    // ==================== getAllergenDisplayName 测试 ====================

    @Test
    fun `test allergen display name for milk`() {
        assertEquals("乳制品", getAllergenDisplayName("milk"))
        assertEquals("乳制品", getAllergenDisplayName("MILK"))
        assertEquals("乳制品", getAllergenDisplayName("Milk"))
    }

    @Test
    fun `test allergen display name for egg`() {
        assertEquals("鸡蛋", getAllergenDisplayName("egg"))
        assertEquals("鸡蛋", getAllergenDisplayName("EGG"))
    }

    @Test
    fun `test allergen display name for fish`() {
        assertEquals("鱼类", getAllergenDisplayName("fish"))
        assertEquals("鱼类", getAllergenDisplayName("FISH"))
    }

    @Test
    fun `test allergen display name for shellfish`() {
        assertEquals("甲壳类", getAllergenDisplayName("shellfish"))
        assertEquals("甲壳类", getAllergenDisplayName("SHELLFISH"))
    }

    @Test
    fun `test allergen display name for peanut`() {
        assertEquals("花生", getAllergenDisplayName("peanut"))
        assertEquals("花生", getAllergenDisplayName("PEANUT"))
    }

    @Test
    fun `test allergen display name for tree nut`() {
        assertEquals("树坚果", getAllergenDisplayName("tree_nut"))
        assertEquals("树坚果", getAllergenDisplayName("treenut"))
        assertEquals("树坚果", getAllergenDisplayName("TREE_NUT"))
    }

    @Test
    fun `test allergen display name for wheat`() {
        assertEquals("小麦", getAllergenDisplayName("wheat"))
        assertEquals("小麦", getAllergenDisplayName("WHEAT"))
    }

    @Test
    fun `test allergen display name for soy`() {
        assertEquals("大豆", getAllergenDisplayName("soy"))
        assertEquals("大豆", getAllergenDisplayName("SOY"))
    }

    @Test
    fun `test allergen display name for unknown allergen`() {
        assertEquals("unknown_allergen", getAllergenDisplayName("unknown_allergen"))
        assertEquals("芝麻", getAllergenDisplayName("芝麻"))
    }

    // ==================== normalizeAllergenForMatching 测试 ====================

    @Test
    fun `test normalize chinese allergen to code - milk`() {
        assertEquals("milk", normalizeAllergenForMatching("乳制品"))
        assertEquals("milk", normalizeAllergenForMatching("牛奶"))
        assertEquals("milk", normalizeAllergenForMatching("奶制品"))
    }

    @Test
    fun `test normalize chinese allergen to code - egg`() {
        assertEquals("egg", normalizeAllergenForMatching("鸡蛋"))
        assertEquals("egg", normalizeAllergenForMatching("蛋类"))
        assertEquals("egg", normalizeAllergenForMatching("鸭蛋"))
    }

    @Test
    fun `test normalize chinese allergen to code - fish`() {
        assertEquals("fish", normalizeAllergenForMatching("鱼类"))
        assertEquals("fish", normalizeAllergenForMatching("鱼"))
    }

    @Test
    fun `test normalize chinese allergen to code - shellfish`() {
        assertEquals("shellfish", normalizeAllergenForMatching("海鲜"))
        assertEquals("shellfish", normalizeAllergenForMatching("虾"))
        assertEquals("shellfish", normalizeAllergenForMatching("螃蟹"))
        assertEquals("shellfish", normalizeAllergenForMatching("蟹"))
        assertEquals("shellfish", normalizeAllergenForMatching("贝类"))
        assertEquals("shellfish", normalizeAllergenForMatching("甲壳类"))
    }

    @Test
    fun `test normalize chinese allergen to code - peanut`() {
        assertEquals("peanut", normalizeAllergenForMatching("花生"))
    }

    @Test
    fun `test normalize chinese allergen to code - tree nut`() {
        assertEquals("tree_nut", normalizeAllergenForMatching("坚果"))
        assertEquals("tree_nut", normalizeAllergenForMatching("杏仁"))
        assertEquals("tree_nut", normalizeAllergenForMatching("核桃"))
        assertEquals("tree_nut", normalizeAllergenForMatching("腰果"))
    }

    @Test
    fun `test normalize chinese allergen to code - wheat`() {
        assertEquals("wheat", normalizeAllergenForMatching("小麦"))
        assertEquals("wheat", normalizeAllergenForMatching("面粉"))
        assertEquals("wheat", normalizeAllergenForMatching("麸质"))
    }

    @Test
    fun `test normalize chinese allergen to code - soy`() {
        assertEquals("soy", normalizeAllergenForMatching("大豆"))
        assertEquals("soy", normalizeAllergenForMatching("豆类"))
        assertEquals("soy", normalizeAllergenForMatching("豆腐"))
    }

    // ==================== matchUserAllergens 测试 ====================

    @Test
    fun `test match user allergens - exact match`() {
        val detectedAllergens = listOf("peanut", "egg")
        val userAllergens = listOf("花生", "鸡蛋")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(2, matched.size)
        assertTrue(matched.contains("peanut"))
        assertTrue(matched.contains("egg"))
    }

    @Test
    fun `test match user allergens - partial match`() {
        val detectedAllergens = listOf("peanut", "egg", "milk")
        val userAllergens = listOf("花生")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(1, matched.size)
        assertTrue(matched.contains("peanut"))
    }

    @Test
    fun `test match user allergens - no match`() {
        val detectedAllergens = listOf("peanut", "egg")
        val userAllergens = listOf("海鲜", "牛奶")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `test match user allergens - empty detected allergens`() {
        val detectedAllergens = emptyList<String>()
        val userAllergens = listOf("花生", "鸡蛋")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `test match user allergens - null user allergens`() {
        val detectedAllergens = listOf("peanut", "egg")
        val userAllergens: List<String>? = null
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `test match user allergens - empty user allergens`() {
        val detectedAllergens = listOf("peanut", "egg")
        val userAllergens = emptyList<String>()
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `test match user allergens - shellfish variants`() {
        val detectedAllergens = listOf("shellfish")
        
        // 测试各种海鲜相关的中文表述
        assertTrue(matchUserAllergens(detectedAllergens, listOf("海鲜")).isNotEmpty())
        assertTrue(matchUserAllergens(detectedAllergens, listOf("虾")).isNotEmpty())
        assertTrue(matchUserAllergens(detectedAllergens, listOf("螃蟹")).isNotEmpty())
    }

    @Test
    fun `test match user allergens - tree nut variants`() {
        val detectedAllergens = listOf("tree_nut")
        
        assertTrue(matchUserAllergens(detectedAllergens, listOf("坚果")).isNotEmpty())
        assertTrue(matchUserAllergens(detectedAllergens, listOf("杏仁")).isNotEmpty())
        assertTrue(matchUserAllergens(detectedAllergens, listOf("核桃")).isNotEmpty())
    }

    // ==================== 综合场景测试 ====================

    @Test
    fun `test real scenario - 宫保鸡丁 with peanut allergy`() {
        // 模拟AI检测到宫保鸡丁含有花生
        val detectedAllergens = listOf("peanut")
        val userAllergens = listOf("花生")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(1, matched.size)
        assertEquals("peanut", matched[0])
        assertEquals("花生", getAllergenDisplayName(matched[0]))
    }

    @Test
    fun `test real scenario - 番茄炒蛋 with egg allergy`() {
        // 模拟AI检测到番茄炒蛋含有鸡蛋
        val detectedAllergens = listOf("egg")
        val userAllergens = listOf("鸡蛋", "海鲜")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(1, matched.size)
        assertEquals("egg", matched[0])
    }

    @Test
    fun `test real scenario - seafood dish with shellfish allergy`() {
        // 模拟海鲜菜品
        val detectedAllergens = listOf("shellfish", "fish")
        val userAllergens = listOf("海鲜")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        // 海鲜应该匹配shellfish
        assertTrue(matched.contains("shellfish"))
    }

    @Test
    fun `test real scenario - no allergy user`() {
        // 用户没有设置过敏原
        val detectedAllergens = listOf("peanut", "egg", "milk")
        val userAllergens = emptyList<String>()
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `test real scenario - multiple allergens detected and matched`() {
        // 菜品含有多种过敏原，用户也对多种过敏
        val detectedAllergens = listOf("peanut", "egg", "soy", "wheat")
        val userAllergens = listOf("花生", "大豆", "牛奶")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(2, matched.size)
        assertTrue(matched.contains("peanut"))
        assertTrue(matched.contains("soy"))
        assertFalse(matched.contains("egg"))  // 用户不对鸡蛋过敏
        assertFalse(matched.contains("wheat"))  // 用户不对小麦过敏
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `test case insensitivity in allergen codes`() {
        val detectedAllergens = listOf("PEANUT", "EGG", "Milk")
        val userAllergens = listOf("花生", "鸡蛋", "牛奶")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(3, matched.size)
    }

    @Test
    fun `test allergen with special characters`() {
        // 测试带下划线的过敏原代码
        val detectedAllergens = listOf("tree_nut")
        val userAllergens = listOf("坚果")
        
        val matched = matchUserAllergens(detectedAllergens, userAllergens)
        
        assertEquals(1, matched.size)
    }

    @Test
    fun `test display name round trip`() {
        // 测试代码 -> 中文名称 -> 代码的转换
        val originalCodes = listOf("milk", "egg", "fish", "shellfish", "peanut", "tree_nut", "wheat", "soy")
        
        originalCodes.forEach { code ->
            val displayName = getAllergenDisplayName(code)
            val normalizedCode = normalizeAllergenForMatching(displayName)
            
            // 验证转换后的代码能正确匹配原始代码
            val detectedAllergens = listOf(code)
            val userAllergens = listOf(displayName)
            val matched = matchUserAllergens(detectedAllergens, userAllergens)
            
            assertTrue("Round trip failed for $code -> $displayName -> $normalizedCode", 
                       matched.isNotEmpty())
        }
    }
}
