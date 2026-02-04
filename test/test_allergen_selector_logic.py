"""
AllergenSelector组件逻辑测试
测试Phase 9实现的过敏原档案配置功能

运行方式: python test_allergen_selector_logic.py
"""

import unittest

# 模拟Kotlin代码中的八大类过敏原数据
EIGHT_MAJOR_ALLERGENS = [
    {"code": "milk", "name": "乳制品", "nameEn": "Milk", "description": "牛奶、奶酪、黄油、酸奶、奶油等", "icon": "🥛"},
    {"code": "egg", "name": "鸡蛋", "nameEn": "Egg", "description": "各种蛋类及其制品", "icon": "🥚"},
    {"code": "fish", "name": "鱼类", "nameEn": "Fish", "description": "各种鱼类及鱼制品", "icon": "🐟"},
    {"code": "shellfish", "name": "甲壳类", "nameEn": "Shellfish", "description": "虾、蟹、贝类等海鲜", "icon": "🦐"},
    {"code": "peanut", "name": "花生", "nameEn": "Peanut", "description": "花生及花生制品", "icon": "🥜"},
    {"code": "tree_nut", "name": "树坚果", "nameEn": "Tree Nuts", "description": "杏仁、核桃、腰果、榛子等", "icon": "🌰"},
    {"code": "wheat", "name": "小麦", "nameEn": "Wheat", "description": "小麦及含麸质食品", "icon": "🌾"},
    {"code": "soy", "name": "大豆", "nameEn": "Soy", "description": "豆腐、豆浆、酱油等豆制品", "icon": "🫘"},
]


def get_by_code(code: str):
    """根据代码获取过敏原类别"""
    code_lower = code.lower()
    for allergen in EIGHT_MAJOR_ALLERGENS:
        if allergen["code"] == code_lower:
            return allergen
    return None


def get_code_by_name(name: str):
    """根据中文名称获取代码"""
    for allergen in EIGHT_MAJOR_ALLERGENS:
        if allergen["name"] == name:
            return allergen["code"]
    return None


def is_major_allergen(allergen: str) -> bool:
    """判断是否为八大类过敏原"""
    lower_allergen = allergen.lower()
    for a in EIGHT_MAJOR_ALLERGENS:
        if a["code"] == lower_allergen or a["name"] == allergen:
            return True
    return False


def get_allergen_display_text(allergens: list) -> str:
    """获取过敏原的展示文本"""
    if not allergens:
        return "未设置"
    
    display_list = []
    for allergen in allergens[:3]:
        # 尝试匹配八大类
        matched = None
        for a in EIGHT_MAJOR_ALLERGENS:
            if a["name"] == allergen:
                matched = a
                break
        display_list.append(matched["name"] if matched else allergen)
    
    if len(allergens) > 3:
        return "、".join(display_list) + f" 等{len(allergens)}项"
    else:
        return "、".join(display_list)


class TestAllergenCategories(unittest.TestCase):
    """测试八大类过敏原常量"""

    def test_eight_major_allergens_count(self):
        """测试八大类过敏原数量正确"""
        self.assertEqual(len(EIGHT_MAJOR_ALLERGENS), 8)

    def test_all_categories_have_required_fields(self):
        """测试所有过敏原都有必填字段"""
        for category in EIGHT_MAJOR_ALLERGENS:
            self.assertTrue(category["code"], "Category code should not be empty")
            self.assertTrue(category["name"], "Category name should not be empty")
            self.assertTrue(category["nameEn"], "Category nameEn should not be empty")
            self.assertTrue(category["description"], "Category description should not be empty")
            self.assertTrue(category["icon"], "Category icon should not be empty")

    def test_allergen_codes_match_backend(self):
        """测试过敏原代码与后端一致"""
        expected_codes = {"milk", "egg", "fish", "shellfish", "peanut", "tree_nut", "wheat", "soy"}
        actual_codes = {a["code"] for a in EIGHT_MAJOR_ALLERGENS}
        self.assertEqual(expected_codes, actual_codes)

    def test_allergen_chinese_names(self):
        """测试中文名称"""
        expected_names = {"乳制品", "鸡蛋", "鱼类", "甲壳类", "花生", "树坚果", "小麦", "大豆"}
        actual_names = {a["name"] for a in EIGHT_MAJOR_ALLERGENS}
        self.assertEqual(expected_names, actual_names)


class TestGetByCode(unittest.TestCase):
    """测试getByCode方法"""

    def test_valid_lowercase_code(self):
        """测试小写代码"""
        result = get_by_code("milk")
        self.assertIsNotNone(result)
        self.assertEqual("乳制品", result["name"])

    def test_valid_uppercase_code(self):
        """测试大写代码"""
        result = get_by_code("MILK")
        self.assertIsNotNone(result)
        self.assertEqual("乳制品", result["name"])

    def test_valid_mixed_case_code(self):
        """测试混合大小写代码"""
        result = get_by_code("Peanut")
        self.assertIsNotNone(result)
        self.assertEqual("花生", result["name"])

    def test_invalid_code_returns_none(self):
        """测试无效代码返回None"""
        result = get_by_code("unknown")
        self.assertIsNone(result)

    def test_empty_string_returns_none(self):
        """测试空字符串返回None"""
        result = get_by_code("")
        self.assertIsNone(result)

    def test_all_eight_categories(self):
        """测试所有八大类"""
        test_cases = {
            "milk": "乳制品",
            "egg": "鸡蛋",
            "fish": "鱼类",
            "shellfish": "甲壳类",
            "peanut": "花生",
            "tree_nut": "树坚果",
            "wheat": "小麦",
            "soy": "大豆"
        }
        for code, expected_name in test_cases.items():
            with self.subTest(code=code):
                result = get_by_code(code)
                self.assertIsNotNone(result, f"Should find allergen for code: {code}")
                self.assertEqual(expected_name, result["name"], f"Name mismatch for code: {code}")


class TestGetCodeByName(unittest.TestCase):
    """测试getCodeByName方法"""

    def test_valid_chinese_name(self):
        """测试有效中文名称"""
        result = get_code_by_name("乳制品")
        self.assertEqual("milk", result)

    def test_invalid_name_returns_none(self):
        """测试无效名称返回None"""
        result = get_code_by_name("未知过敏原")
        self.assertIsNone(result)

    def test_all_eight_categories(self):
        """测试所有八大类"""
        test_cases = {
            "乳制品": "milk",
            "鸡蛋": "egg",
            "鱼类": "fish",
            "甲壳类": "shellfish",
            "花生": "peanut",
            "树坚果": "tree_nut",
            "小麦": "wheat",
            "大豆": "soy"
        }
        for name, expected_code in test_cases.items():
            with self.subTest(name=name):
                result = get_code_by_name(name)
                self.assertEqual(expected_code, result, f"Code mismatch for name: {name}")


class TestIsMajorAllergen(unittest.TestCase):
    """测试isMajorAllergen方法"""

    def test_code_returns_true(self):
        """测试代码返回True"""
        self.assertTrue(is_major_allergen("milk"))
        self.assertTrue(is_major_allergen("egg"))
        self.assertTrue(is_major_allergen("peanut"))

    def test_chinese_name_returns_true(self):
        """测试中文名称返回True"""
        self.assertTrue(is_major_allergen("乳制品"))
        self.assertTrue(is_major_allergen("鸡蛋"))
        self.assertTrue(is_major_allergen("花生"))

    def test_custom_allergen_returns_false(self):
        """测试自定义过敏原返回False"""
        self.assertFalse(is_major_allergen("芒果"))
        self.assertFalse(is_major_allergen("蜂蜜"))
        self.assertFalse(is_major_allergen("酒精"))

    def test_case_insensitive_for_codes(self):
        """测试代码不区分大小写"""
        self.assertTrue(is_major_allergen("MILK"))
        self.assertTrue(is_major_allergen("Egg"))
        self.assertTrue(is_major_allergen("PEANUT"))


class TestGetAllergenDisplayText(unittest.TestCase):
    """测试getAllergenDisplayText方法"""

    def test_empty_list(self):
        """测试空列表"""
        result = get_allergen_display_text([])
        self.assertEqual("未设置", result)

    def test_one_allergen(self):
        """测试一个过敏原"""
        result = get_allergen_display_text(["乳制品"])
        self.assertEqual("乳制品", result)

    def test_two_allergens(self):
        """测试两个过敏原"""
        result = get_allergen_display_text(["乳制品", "鸡蛋"])
        self.assertEqual("乳制品、鸡蛋", result)

    def test_three_allergens(self):
        """测试三个过敏原"""
        result = get_allergen_display_text(["乳制品", "鸡蛋", "花生"])
        self.assertEqual("乳制品、鸡蛋、花生", result)

    def test_more_than_three_allergens(self):
        """测试超过三个过敏原"""
        result = get_allergen_display_text(["乳制品", "鸡蛋", "花生", "鱼类", "大豆"])
        self.assertEqual("乳制品、鸡蛋、花生 等5项", result)

    def test_custom_allergens(self):
        """测试自定义过敏原"""
        result = get_allergen_display_text(["芒果", "猕猴桃"])
        self.assertEqual("芒果、猕猴桃", result)

    def test_mixed_allergens(self):
        """测试混合过敏原"""
        result = get_allergen_display_text(["乳制品", "芒果", "鸡蛋"])
        self.assertEqual("乳制品、芒果、鸡蛋", result)


class TestUserScenarios(unittest.TestCase):
    """用户场景测试"""

    def test_select_multiple_major_allergens(self):
        """测试用户选择多个八大类过敏原"""
        user_selection = ["乳制品", "鸡蛋", "花生"]
        
        # 验证所有选择都是有效的八大类过敏原
        for allergen in user_selection:
            self.assertTrue(is_major_allergen(allergen), f"{allergen} should be a major allergen")
        
        # 验证显示文本正确
        display_text = get_allergen_display_text(user_selection)
        self.assertEqual("乳制品、鸡蛋、花生", display_text)

    def test_add_custom_allergen(self):
        """测试用户添加自定义过敏原"""
        custom_allergen = "芒果"
        
        # 自定义过敏原不应该是八大类
        self.assertFalse(is_major_allergen(custom_allergen))
        
        # 但应该可以正常显示
        display_text = get_allergen_display_text([custom_allergen])
        self.assertEqual("芒果", display_text)

    def test_combine_major_and_custom_allergens(self):
        """测试用户组合八大类和自定义过敏原"""
        combined_selection = ["乳制品", "芒果", "花生", "猕猴桃"]
        
        # 验证混合列表的显示
        display_text = get_allergen_display_text(combined_selection)
        self.assertEqual("乳制品、芒果、花生 等4项", display_text)

    def test_allergen_code_to_name_conversion(self):
        """测试过敏原代码到名称的转换（API集成）"""
        # 模拟从后端接收过敏原代码并转换为显示名称
        backend_codes = ["milk", "egg", "peanut"]
        
        display_names = []
        for code in backend_codes:
            allergen = get_by_code(code)
            display_names.append(allergen["name"] if allergen else code)
        
        self.assertEqual(["乳制品", "鸡蛋", "花生"], display_names)

    def test_allergen_name_to_code_conversion(self):
        """测试过敏原名称到代码的转换（API提交）"""
        # 模拟将用户选择的中文名称转换为代码发送给后端
        user_selection = ["乳制品", "鸡蛋", "花生"]
        
        codes = []
        for name in user_selection:
            code = get_code_by_name(name)
            if code:
                codes.append(code)
        
        self.assertEqual(["milk", "egg", "peanut"], codes)


class TestEdgeCases(unittest.TestCase):
    """边界条件测试"""

    def test_allergen_icons_valid(self):
        """测试过敏原图标有效"""
        for category in EIGHT_MAJOR_ALLERGENS:
            self.assertTrue(len(category["icon"]) > 0, f"Icon for {category['name']} should not be empty")

    def test_allergen_descriptions_contain_keywords(self):
        """测试过敏原描述包含相关关键词"""
        milk_category = get_by_code("milk")
        self.assertIn("奶", milk_category["description"])
        
        egg_category = get_by_code("egg")
        self.assertIn("蛋", egg_category["description"])
        
        fish_category = get_by_code("fish")
        self.assertIn("鱼", fish_category["description"])

    def test_empty_allergen_name(self):
        """测试空过敏原名称"""
        result = is_major_allergen("")
        self.assertFalse(result)

    def test_special_characters_in_allergen_name(self):
        """测试特殊字符"""
        result = is_major_allergen("过敏原#1")
        self.assertFalse(result)


if __name__ == "__main__":
    # 运行所有测试
    unittest.main(verbosity=2)
