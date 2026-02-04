#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Phase 13: 餐前拍摄功能 - 代码验证脚本
验证前端代码结构和必要文件是否正确创建
"""

import os
import re
import sys

# 定义基础路径
BASE_PATH = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP_PATH = os.path.join(BASE_PATH, "app", "src", "main", "java", "com", "example", "lifehub")

def check_file_exists(file_path, description):
    """检查文件是否存在"""
    full_path = os.path.join(APP_PATH, file_path)
    exists = os.path.exists(full_path)
    status = "✓" if exists else "✗"
    print(f"  {status} {description}: {file_path}")
    return exists

def check_file_contains(file_path, patterns, description):
    """检查文件是否包含指定内容"""
    full_path = os.path.join(APP_PATH, file_path)
    if not os.path.exists(full_path):
        print(f"  ✗ 文件不存在: {file_path}")
        return False
    
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    all_found = True
    for pattern in patterns:
        if isinstance(pattern, str):
            found = pattern in content
        else:
            found = pattern.search(content) is not None
        
        if not found:
            print(f"  ✗ {description}: 未找到 '{pattern if isinstance(pattern, str) else pattern.pattern}'")
            all_found = False
    
    if all_found:
        print(f"  ✓ {description}")
    return all_found

def test_data_model():
    """测试数据模型文件"""
    print("\n1. 验证数据模型 (MealComparison.kt)")
    patterns = [
        "data class BeforeMealResponse",
        "data class BeforeMealData",
        "data class AfterMealResponse",
        "data class AfterMealData",
        "data class MealFeatures",
        "data class DishFeature",
        "data class MealComparisonRecord",
        "comparisonId",
        "beforeImageUrl",
        "consumptionRatio",
        "netCalories"
    ]
    return check_file_contains("data/MealComparison.kt", patterns, "数据模型定义完整")

def test_api_service():
    """测试API服务接口"""
    print("\n2. 验证API服务接口 (ApiService.kt)")
    patterns = [
        "uploadBeforeMealImage",
        "uploadAfterMealImage",
        "@POST(\"/api/food/meal/before\")",
        "@POST(\"/api/food/meal/after/{comparison_id}\")",
        "BeforeMealResponse",
        "AfterMealResponse"
    ]
    return check_file_contains("network/ApiService.kt", patterns, "API接口定义完整")

def test_view_model():
    """测试ViewModel"""
    print("\n3. 验证ViewModel (FoodViewModel.kt)")
    patterns = [
        "BeforeMealUploadState",
        "AfterMealUploadState",
        "_beforeMealUploadState",
        "_afterMealUploadState",
        "_currentComparisonRecord",
        "fun uploadBeforeMealImage",
        "fun uploadAfterMealImage",
        "fun resetBeforeMealUploadState",
        "fun resetMealComparisonState"
    ]
    return check_file_contains("viewmodel/FoodViewModel.kt", patterns, "ViewModel状态和方法完整")

def test_meal_comparison_page():
    """测试餐前餐后对比主页面"""
    print("\n4. 验证餐前餐后对比页面 (MealComparisonPage.kt)")
    patterns = [
        "@Composable",
        "fun MealComparisonPage",
        "BeforeMealUploadState",
        "拍摄餐前照片",
        "智能餐前餐后对比",
        "FeatureIntroCard",
        "CaptureGuidelinesCard"
    ]
    return check_file_contains("ui/screen/MealComparisonPage.kt", patterns, "对比页面组件完整")

def test_before_meal_camera_page():
    """测试餐前拍摄相机页面"""
    print("\n5. 验证餐前拍摄相机页面 (BeforeMealCameraPage.kt)")
    patterns = [
        "@Composable",
        "fun BeforeMealCameraPage",
        "餐前拍摄",
        "CaptureGuidanceOverlay",
        "居中",
        "光线充足",
        "垂直俯拍",
        "BeforeMealCameraController"
    ]
    return check_file_contains("ui/screen/BeforeMealCameraPage.kt", patterns, "相机页面和拍摄引导完整")

def test_navigation():
    """测试导航路由"""
    print("\n6. 验证导航路由")
    
    # 检查Screen.kt
    screen_patterns = [
        "MealComparison",
        "BeforeMealCamera",
        "meal_comparison",
        "before_meal_camera"
    ]
    screen_ok = check_file_contains("navigation/Screen.kt", screen_patterns, "Screen路由定义")
    
    # 检查MainNavigation.kt
    nav_patterns = [
        "Screen.MealComparison.route",
        "Screen.BeforeMealCamera.route",
        "MealComparisonPage",
        "BeforeMealCameraPage"
    ]
    nav_ok = check_file_contains("navigation/MainNavigation.kt", nav_patterns, "导航配置")
    
    return screen_ok and nav_ok

def test_home_page_entry():
    """测试首页入口"""
    print("\n7. 验证首页入口 (HomePage.kt)")
    patterns = [
        "餐前餐后对比",
        "Screen.MealComparison.route",
        "CompareArrows"
    ]
    return check_file_contains("ui/screen/HomePage.kt", patterns, "首页入口卡片")

def test_unit_test_file():
    """测试单元测试文件"""
    print("\n8. 验证单元测试文件")
    test_path = os.path.join(BASE_PATH, "app", "src", "test", "java", "com", "example", "lifehub", "Phase13MealComparisonTest.kt")
    exists = os.path.exists(test_path)
    status = "✓" if exists else "✗"
    print(f"  {status} 单元测试文件存在")
    
    if exists:
        with open(test_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        test_patterns = [
            "测试BeforeMealData数据模型创建",
            "测试净摄入热量计算逻辑",
            "测试消耗比例边界值",
            "@Test"
        ]
        all_found = all(p in content for p in test_patterns)
        status = "✓" if all_found else "✗"
        print(f"  {status} 单元测试用例完整")
        return exists and all_found
    return False

def main():
    """主函数"""
    print("=" * 60)
    print("Phase 13: 餐前拍摄功能 - 代码验证")
    print("=" * 60)
    
    results = []
    
    # 运行所有验证
    results.append(("数据模型", test_data_model()))
    results.append(("API服务接口", test_api_service()))
    results.append(("ViewModel", test_view_model()))
    results.append(("餐前餐后对比页面", test_meal_comparison_page()))
    results.append(("餐前拍摄相机页面", test_before_meal_camera_page()))
    results.append(("导航路由", test_navigation()))
    results.append(("首页入口", test_home_page_entry()))
    results.append(("单元测试文件", test_unit_test_file()))
    
    # 汇总结果
    print("\n" + "=" * 60)
    print("验证结果汇总")
    print("=" * 60)
    
    passed = sum(1 for _, r in results if r)
    total = len(results)
    
    for name, result in results:
        status = "✓ 通过" if result else "✗ 失败"
        print(f"  {status}: {name}")
    
    print(f"\n总计: {passed}/{total} 项验证通过")
    
    if passed == total:
        print("\n🎉 Phase 13 代码验证全部通过！")
        return 0
    else:
        print("\n⚠️ 部分验证未通过，请检查相关代码")
        return 1

if __name__ == "__main__":
    sys.exit(main())
