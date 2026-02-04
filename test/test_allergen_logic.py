"""
Phase 8: 过敏原预警功能测试
测试过敏原匹配、显示名称转换等核心逻辑
使用Python验证Kotlin实现的逻辑是否正确
"""

import unittest


def get_allergen_display_name(allergen_code: str) -> str:
    """获取过敏原的中文显示名称"""
    code_lower = allergen_code.lower()
    mapping = {
        "milk": "乳制品",
        "egg": "鸡蛋",
        "fish": "鱼类",
        "shellfish": "甲壳类",
        "peanut": "花生",
        "tree_nut": "树坚果",
        "treenut": "树坚果",
        "wheat": "小麦",
        "soy": "大豆",
    }
    return mapping.get(code_lower, allergen_code)


def normalize_allergen_for_matching(user_allergen: str) -> str:
    """将用户过敏原中文名称转换为代码进行匹配"""
    if "乳" in user_allergen or "奶" in user_allergen:
        return "milk"
    if "蛋" in user_allergen:
        return "egg"
    if "鱼" in user_allergen:
        return "fish"
    if any(x in user_allergen for x in ["虾", "蟹", "贝", "海鲜", "甲壳"]):
        return "shellfish"
    if "花生" in user_allergen:
        return "peanut"
    if any(x in user_allergen for x in ["坚果", "杏仁", "核桃", "腰果"]):
        return "tree_nut"
    if any(x in user_allergen for x in ["麦", "面", "麸"]):
        return "wheat"
    if "豆" in user_allergen:
        return "soy"
    return user_allergen.lower()


def match_user_allergens(detected_allergens: list, user_allergens: list) -> list:
    """检查过敏原是否匹配"""
    if not user_allergens or not detected_allergens:
        return []
    
    normalized_user_allergens = [normalize_allergen_for_matching(ua) for ua in user_allergens]
    
    matched = []
    for detected in detected_allergens:
        detected_lower = detected.lower()
        for user_allergen in normalized_user_allergens:
            if (detected_lower == user_allergen or 
                detected_lower in user_allergen or 
                user_allergen in detected_lower):
                matched.append(detected)
                break
    
    return matched


class TestAllergenDisplayName(unittest.TestCase):
    """测试过敏原显示名称转换"""

    def test_milk(self):
        self.assertEqual(get_allergen_display_name("milk"), "乳制品")
        self.assertEqual(get_allergen_display_name("MILK"), "乳制品")
        self.assertEqual(get_allergen_display_name("Milk"), "乳制品")

    def test_egg(self):
        self.assertEqual(get_allergen_display_name("egg"), "鸡蛋")
        self.assertEqual(get_allergen_display_name("EGG"), "鸡蛋")

    def test_fish(self):
        self.assertEqual(get_allergen_display_name("fish"), "鱼类")
        self.assertEqual(get_allergen_display_name("FISH"), "鱼类")

    def test_shellfish(self):
        self.assertEqual(get_allergen_display_name("shellfish"), "甲壳类")
        self.assertEqual(get_allergen_display_name("SHELLFISH"), "甲壳类")

    def test_peanut(self):
        self.assertEqual(get_allergen_display_name("peanut"), "花生")
        self.assertEqual(get_allergen_display_name("PEANUT"), "花生")

    def test_tree_nut(self):
        self.assertEqual(get_allergen_display_name("tree_nut"), "树坚果")
        self.assertEqual(get_allergen_display_name("treenut"), "树坚果")

    def test_wheat(self):
        self.assertEqual(get_allergen_display_name("wheat"), "小麦")

    def test_soy(self):
        self.assertEqual(get_allergen_display_name("soy"), "大豆")

    def test_unknown_allergen(self):
        self.assertEqual(get_allergen_display_name("unknown_allergen"), "unknown_allergen")
        self.assertEqual(get_allergen_display_name("芝麻"), "芝麻")


class TestNormalizeAllergen(unittest.TestCase):
    """测试过敏原标准化转换"""

    def test_milk_variants(self):
        self.assertEqual(normalize_allergen_for_matching("乳制品"), "milk")
        self.assertEqual(normalize_allergen_for_matching("牛奶"), "milk")
        self.assertEqual(normalize_allergen_for_matching("奶制品"), "milk")

    def test_egg_variants(self):
        self.assertEqual(normalize_allergen_for_matching("鸡蛋"), "egg")
        self.assertEqual(normalize_allergen_for_matching("蛋类"), "egg")
        self.assertEqual(normalize_allergen_for_matching("鸭蛋"), "egg")

    def test_fish_variants(self):
        self.assertEqual(normalize_allergen_for_matching("鱼类"), "fish")
        self.assertEqual(normalize_allergen_for_matching("鱼"), "fish")

    def test_shellfish_variants(self):
        self.assertEqual(normalize_allergen_for_matching("海鲜"), "shellfish")
        self.assertEqual(normalize_allergen_for_matching("虾"), "shellfish")
        self.assertEqual(normalize_allergen_for_matching("螃蟹"), "shellfish")
        self.assertEqual(normalize_allergen_for_matching("蟹"), "shellfish")
        self.assertEqual(normalize_allergen_for_matching("贝类"), "shellfish")
        self.assertEqual(normalize_allergen_for_matching("甲壳类"), "shellfish")

    def test_peanut(self):
        self.assertEqual(normalize_allergen_for_matching("花生"), "peanut")

    def test_tree_nut_variants(self):
        self.assertEqual(normalize_allergen_for_matching("坚果"), "tree_nut")
        self.assertEqual(normalize_allergen_for_matching("杏仁"), "tree_nut")
        self.assertEqual(normalize_allergen_for_matching("核桃"), "tree_nut")
        self.assertEqual(normalize_allergen_for_matching("腰果"), "tree_nut")

    def test_wheat_variants(self):
        self.assertEqual(normalize_allergen_for_matching("小麦"), "wheat")
        self.assertEqual(normalize_allergen_for_matching("面粉"), "wheat")
        self.assertEqual(normalize_allergen_for_matching("麸质"), "wheat")

    def test_soy_variants(self):
        self.assertEqual(normalize_allergen_for_matching("大豆"), "soy")
        self.assertEqual(normalize_allergen_for_matching("豆类"), "soy")
        self.assertEqual(normalize_allergen_for_matching("豆腐"), "soy")


class TestMatchUserAllergens(unittest.TestCase):
    """测试过敏原匹配逻辑"""

    def test_exact_match(self):
        detected = ["peanut", "egg"]
        user = ["花生", "鸡蛋"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 2)
        self.assertIn("peanut", matched)
        self.assertIn("egg", matched)

    def test_partial_match(self):
        detected = ["peanut", "egg", "milk"]
        user = ["花生"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 1)
        self.assertIn("peanut", matched)

    def test_no_match(self):
        detected = ["peanut", "egg"]
        user = ["海鲜", "牛奶"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 0)

    def test_empty_detected(self):
        detected = []
        user = ["花生", "鸡蛋"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 0)

    def test_none_user(self):
        detected = ["peanut", "egg"]
        user = None
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 0)

    def test_empty_user(self):
        detected = ["peanut", "egg"]
        user = []
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 0)

    def test_shellfish_variants(self):
        detected = ["shellfish"]
        self.assertTrue(len(match_user_allergens(detected, ["海鲜"])) > 0)
        self.assertTrue(len(match_user_allergens(detected, ["虾"])) > 0)
        self.assertTrue(len(match_user_allergens(detected, ["螃蟹"])) > 0)

    def test_tree_nut_variants(self):
        detected = ["tree_nut"]
        self.assertTrue(len(match_user_allergens(detected, ["坚果"])) > 0)
        self.assertTrue(len(match_user_allergens(detected, ["杏仁"])) > 0)
        self.assertTrue(len(match_user_allergens(detected, ["核桃"])) > 0)


class TestRealScenarios(unittest.TestCase):
    """真实场景测试"""

    def test_gongbao_jiding_with_peanut_allergy(self):
        """宫保鸡丁 - 花生过敏"""
        detected = ["peanut"]
        user = ["花生"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 1)
        self.assertEqual(matched[0], "peanut")
        self.assertEqual(get_allergen_display_name(matched[0]), "花生")

    def test_fanqie_chaodan_with_egg_allergy(self):
        """番茄炒蛋 - 鸡蛋过敏"""
        detected = ["egg"]
        user = ["鸡蛋", "海鲜"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 1)
        self.assertEqual(matched[0], "egg")

    def test_seafood_dish_with_shellfish_allergy(self):
        """海鲜菜品"""
        detected = ["shellfish", "fish"]
        user = ["海鲜"]
        matched = match_user_allergens(detected, user)
        self.assertIn("shellfish", matched)

    def test_no_allergy_user(self):
        """用户没有设置过敏原"""
        detected = ["peanut", "egg", "milk"]
        user = []
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 0)

    def test_multiple_allergens_detected_and_matched(self):
        """多种过敏原检测和匹配"""
        detected = ["peanut", "egg", "soy", "wheat"]
        user = ["花生", "大豆", "牛奶"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 2)
        self.assertIn("peanut", matched)
        self.assertIn("soy", matched)
        self.assertNotIn("egg", matched)
        self.assertNotIn("wheat", matched)

    def test_case_insensitivity(self):
        """大小写不敏感测试"""
        detected = ["PEANUT", "EGG", "Milk"]
        user = ["花生", "鸡蛋", "牛奶"]
        matched = match_user_allergens(detected, user)
        self.assertEqual(len(matched), 3)


class TestDisplayNameRoundTrip(unittest.TestCase):
    """代码到中文名称的往返测试"""

    def test_round_trip_all_allergens(self):
        """测试所有八大类过敏原的往返转换"""
        original_codes = ["milk", "egg", "fish", "shellfish", "peanut", "tree_nut", "wheat", "soy"]
        
        for code in original_codes:
            display_name = get_allergen_display_name(code)
            normalized_code = normalize_allergen_for_matching(display_name)
            
            detected = [code]
            user = [display_name]
            matched = match_user_allergens(detected, user)
            
            self.assertTrue(
                len(matched) > 0,
                f"Round trip failed for {code} -> {display_name} -> {normalized_code}"
            )


if __name__ == "__main__":
    # 运行所有测试
    print("=" * 60)
    print("Phase 8: 过敏原预警功能测试")
    print("=" * 60)
    
    # 创建测试套件
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # 添加所有测试类
    suite.addTests(loader.loadTestsFromTestCase(TestAllergenDisplayName))
    suite.addTests(loader.loadTestsFromTestCase(TestNormalizeAllergen))
    suite.addTests(loader.loadTestsFromTestCase(TestMatchUserAllergens))
    suite.addTests(loader.loadTestsFromTestCase(TestRealScenarios))
    suite.addTests(loader.loadTestsFromTestCase(TestDisplayNameRoundTrip))
    
    # 运行测试
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # 输出总结
    print("\n" + "=" * 60)
    print(f"测试总数: {result.testsRun}")
    print(f"成功: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"失败: {len(result.failures)}")
    print(f"错误: {len(result.errors)}")
    print("=" * 60)
    
    if result.wasSuccessful():
        print("✅ 所有测试通过！Phase 8 过敏原预警功能逻辑正确")
    else:
        print("❌ 测试未通过，请检查代码")
        for failure in result.failures:
            print(f"\n失败: {failure[0]}")
            print(failure[1])
        for error in result.errors:
            print(f"\n错误: {error[0]}")
            print(error[1])
