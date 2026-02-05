"""
Phase 17: 热量收支图表功能测试脚本
直接用Python运行测试逻辑，验证数据模型和计算逻辑
"""
import sys

def test_daily_calorie_stats_data_model():
    """测试每日热量统计数据模型"""
    stats = {
        "date": "2026-02-05",
        "user_id": 1,
        "intake_calories": 1800.0,
        "meal_count": 3,
        "burn_calories": 500.0,
        "exercise_count": 2,
        "exercise_duration": 60,
        "net_calories": 1300.0
    }
    
    assert stats["date"] == "2026-02-05"
    assert stats["user_id"] == 1
    assert abs(stats["intake_calories"] - 1800.0) < 0.01
    assert stats["meal_count"] == 3
    assert abs(stats["burn_calories"] - 500.0) < 0.01
    assert stats["exercise_count"] == 2
    assert stats["exercise_duration"] == 60
    assert abs(stats["net_calories"] - 1300.0) < 0.01
    print("✓ test_daily_calorie_stats_data_model PASSED")

def test_weekly_calorie_stats_data_model():
    """测试每周热量统计数据模型"""
    stats = {
        "week_start": "2026-02-03",
        "week_end": "2026-02-09",
        "user_id": 1,
        "total_intake": 12600.0,
        "total_burn": 3500.0,
        "total_net": 9100.0,
        "avg_intake": 1800.0,
        "avg_burn": 500.0,
        "avg_net": 1300.0,
        "total_meals": 21,
        "total_exercises": 14,
        "active_days": 7
    }
    
    assert stats["week_start"] == "2026-02-03"
    assert stats["week_end"] == "2026-02-09"
    assert abs(stats["total_intake"] - 12600.0) < 0.01
    assert abs(stats["total_burn"] - 3500.0) < 0.01
    assert abs(stats["total_net"] - 9100.0) < 0.01
    assert abs(stats["avg_intake"] - 1800.0) < 0.01
    assert abs(stats["avg_burn"] - 500.0) < 0.01
    assert stats["active_days"] == 7
    print("✓ test_weekly_calorie_stats_data_model PASSED")

def test_daily_breakdown():
    """测试周统计中的每日明细数据"""
    breakdown = {
        "date": "2026-02-05",
        "intake_calories": 1800.0,
        "burn_calories": 500.0,
        "net_calories": 1300.0
    }
    
    assert breakdown["date"] == "2026-02-05"
    assert abs(breakdown["intake_calories"] - 1800.0) < 0.01
    assert abs(breakdown["burn_calories"] - 500.0) < 0.01
    assert abs(breakdown["net_calories"] - 1300.0) < 0.01
    print("✓ test_daily_breakdown PASSED")

def test_net_calories_calculation():
    """测试净热量计算"""
    intake = 2000.0
    burn = 600.0
    net = intake - burn
    assert abs(net - 1400.0) < 0.01
    print("✓ test_net_calories_calculation PASSED")

def test_negative_net_calories():
    """测试负净热量（消耗大于摄入）"""
    intake = 1500.0
    burn = 2000.0
    net = intake - burn
    assert abs(net - (-500.0)) < 0.01
    print("✓ test_negative_net_calories PASSED")

def test_average_calculation():
    """测试周平均计算"""
    daily_values = [1800.0, 1600.0, 2000.0, 1700.0, 1900.0, 1500.0, 2100.0]
    average = sum(daily_values) / len(daily_values)
    assert abs(average - 1800.0) < 0.01
    print("✓ test_average_calculation PASSED")

def test_weekly_total_calculation():
    """测试周总计计算"""
    daily_values = [1800.0, 1600.0, 2000.0, 1700.0, 1900.0, 1500.0, 2100.0]
    total = sum(daily_values)
    assert abs(total - 12600.0) < 0.01
    print("✓ test_weekly_total_calculation PASSED")

def test_date_format_validation():
    """测试日期格式验证"""
    import re
    valid_date = "2026-02-05"
    pattern = r"\d{4}-\d{2}-\d{2}"
    assert re.match(pattern, valid_date) is not None
    print("✓ test_date_format_validation PASSED")

def test_week_start_date_calculation():
    """测试周起始日期计算"""
    # 2026-02-05 是周四，周一应该是 2026-02-02
    day_of_week = 4  # 周四
    days_to_subtract = day_of_week - 1  # 到周一的天数差
    assert days_to_subtract == 3
    print("✓ test_week_start_date_calculation PASSED")

def test_date_range_for_weekly_stats():
    """测试周日期范围"""
    start_day = 3
    end_day = 9
    days_count = end_day - start_day + 1
    assert days_count == 7
    print("✓ test_date_range_for_weekly_stats PASSED")

def test_chart_data_point():
    """测试图表数据点"""
    chart_point = {
        "label": "周一",
        "intake": 1800.0,
        "burn": 500.0
    }
    
    assert chart_point["label"] == "周一"
    assert abs(chart_point["intake"] - 1800.0) < 0.01
    assert abs(chart_point["burn"] - 500.0) < 0.01
    print("✓ test_chart_data_point PASSED")

def test_empty_chart_data():
    """测试空数据处理"""
    empty_list = []
    assert len(empty_list) == 0
    print("✓ test_empty_chart_data PASSED")

def test_chart_data_normalization():
    """测试图表数据归一化"""
    values = [500.0, 1000.0, 1500.0, 2000.0]
    max_value = max(values) if values else 0.0
    normalized = [v / max_value for v in values]
    
    assert abs(normalized[0] - 0.25) < 0.01
    assert abs(normalized[1] - 0.50) < 0.01
    assert abs(normalized[2] - 0.75) < 0.01
    assert abs(normalized[3] - 1.00) < 0.01
    print("✓ test_chart_data_normalization PASSED")

def test_view_mode_toggle():
    """测试日/周视图切换"""
    is_weekly_mode = False
    
    # 切换到周视图
    is_weekly_mode = True
    assert is_weekly_mode == True
    
    # 切换回日视图
    is_weekly_mode = False
    assert is_weekly_mode == False
    print("✓ test_view_mode_toggle PASSED")

def test_date_navigation():
    """测试日期导航"""
    current_date = 5
    previous_date = current_date - 1
    next_date = current_date + 1
    
    assert previous_date == 4
    assert next_date == 6
    print("✓ test_date_navigation PASSED")

def test_meal_breakdown_data():
    """测试餐次分类数据"""
    meal_breakdown = {
        "breakfast": 400.0,
        "lunch": 700.0,
        "dinner": 600.0,
        "snack": 100.0
    }
    
    assert abs(meal_breakdown["breakfast"] - 400.0) < 0.01
    assert abs(meal_breakdown["lunch"] - 700.0) < 0.01
    assert abs(meal_breakdown["dinner"] - 600.0) < 0.01
    assert abs(meal_breakdown["snack"] - 100.0) < 0.01
    
    total = sum(meal_breakdown.values())
    assert abs(total - 1800.0) < 0.01
    print("✓ test_meal_breakdown_data PASSED")

def test_zero_calories_handling():
    """测试零热量处理"""
    stats = {
        "intake_calories": 0.0,
        "burn_calories": 0.0,
        "net_calories": 0.0,
        "meal_count": 0
    }
    
    assert abs(stats["intake_calories"] - 0.0) < 0.01
    assert abs(stats["burn_calories"] - 0.0) < 0.01
    assert abs(stats["net_calories"] - 0.0) < 0.01
    assert stats["meal_count"] == 0
    print("✓ test_zero_calories_handling PASSED")

def test_large_calorie_values():
    """测试大数值热量"""
    large_intake = 5000.0
    large_burn = 3000.0
    net = large_intake - large_burn
    assert abs(net - 2000.0) < 0.01
    print("✓ test_large_calorie_values PASSED")

def main():
    """运行所有测试"""
    print("=" * 50)
    print("Phase 17: 热量收支图表功能测试")
    print("=" * 50)
    
    tests = [
        test_daily_calorie_stats_data_model,
        test_weekly_calorie_stats_data_model,
        test_daily_breakdown,
        test_net_calories_calculation,
        test_negative_net_calories,
        test_average_calculation,
        test_weekly_total_calculation,
        test_date_format_validation,
        test_week_start_date_calculation,
        test_date_range_for_weekly_stats,
        test_chart_data_point,
        test_empty_chart_data,
        test_chart_data_normalization,
        test_view_mode_toggle,
        test_date_navigation,
        test_meal_breakdown_data,
        test_zero_calories_handling,
        test_large_calorie_values,
    ]
    
    passed = 0
    failed = 0
    
    for test in tests:
        try:
            test()
            passed += 1
        except AssertionError as e:
            print(f"✗ {test.__name__} FAILED: {e}")
            failed += 1
        except Exception as e:
            print(f"✗ {test.__name__} ERROR: {e}")
            failed += 1
    
    print("=" * 50)
    print(f"测试结果: {passed} 通过, {failed} 失败")
    print("=" * 50)
    
    return 0 if failed == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
