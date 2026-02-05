"""
验证 MapView.kt 代码结构的脚本
检查：
1. 括号匹配（大括号、小括号、方括号）
2. import 语句完整性
3. 基本语法结构
"""

import os
import re


def check_bracket_balance(content: str) -> tuple[bool, str]:
    """检查括号是否匹配"""
    stack = []
    brackets = {'{': '}', '(': ')', '[': ']'}
    closing = set(brackets.values())
    
    # 移除字符串和注释以避免误判
    # 简单处理：移除单行注释
    lines = content.split('\n')
    cleaned_lines = []
    in_multiline_comment = False
    
    for line in lines:
        # 处理多行注释
        if '/*' in line:
            in_multiline_comment = True
        if '*/' in line:
            in_multiline_comment = False
            continue
        if in_multiline_comment:
            continue
        
        # 移除单行注释
        if '//' in line:
            line = line[:line.index('//')]
        
        cleaned_lines.append(line)
    
    cleaned_content = '\n'.join(cleaned_lines)
    
    # 简单移除字符串内容（不完美但足够基本检查）
    cleaned_content = re.sub(r'"[^"]*"', '""', cleaned_content)
    
    line_num = 1
    for i, char in enumerate(cleaned_content):
        if char == '\n':
            line_num += 1
        elif char in brackets:
            stack.append((char, line_num))
        elif char in closing:
            if not stack:
                return False, f"第{line_num}行：多余的闭括号 '{char}'"
            last_open, open_line = stack.pop()
            expected_close = brackets[last_open]
            if char != expected_close:
                return False, f"第{line_num}行：括号不匹配，期望 '{expected_close}' 但得到 '{char}'"
    
    if stack:
        unclosed = stack[-1]
        return False, f"第{unclosed[1]}行：未闭合的括号 '{unclosed[0]}'"
    
    return True, "括号匹配正确"


def check_imports(content: str) -> tuple[bool, list[str]]:
    """检查import语句"""
    required_imports = [
        'com.amap.api.maps.AMap',
        'com.amap.api.maps.MapView',
        'com.amap.api.maps.model.LatLng',
        'com.amap.api.maps.model.MarkerOptions',
        'com.amap.api.maps.model.PolylineOptions',
    ]
    
    missing = []
    for imp in required_imports:
        if f'import {imp}' not in content:
            missing.append(imp)
    
    return len(missing) == 0, missing


def check_function_declarations(content: str) -> tuple[bool, list[str]]:
    """检查函数声明是否完整"""
    expected_functions = [
        'fun AMapComposeView',
        'fun setupMap',
        'fun setupLocation',
        'fun addMarkersToMap',
        'fun addPolylinesToMap',
        'fun SimpleMapView',
        'fun RouteMapView',
    ]
    
    missing = []
    for func in expected_functions:
        if func not in content:
            missing.append(func)
    
    return len(missing) == 0, missing


def check_data_classes(content: str) -> tuple[bool, list[str]]:
    """检查数据类声明"""
    expected_classes = [
        'data class MarkerData',
        'data class PolylineData',
        'data class LatLngPoint',
        'sealed class AMapViewState',
    ]
    
    missing = []
    for cls in expected_classes:
        if cls not in content:
            missing.append(cls)
    
    return len(missing) == 0, missing


def check_disposable_effect_structure(content: str) -> tuple[bool, str]:
    """检查DisposableEffect结构"""
    # 检查是否有DisposableEffect且包含onDispose
    if 'DisposableEffect' not in content:
        return False, "缺少DisposableEffect"
    
    # 找到DisposableEffect的位置
    de_start = content.find('DisposableEffect(')
    if de_start == -1:
        return False, "未找到DisposableEffect"
    
    # 找到第一个 { 开始计数
    first_brace = content.find('{', de_start)
    if first_brace == -1:
        return False, "DisposableEffect缺少块"
    
    # 找到这个块的结束位置
    brace_count = 1
    de_end = first_brace + 1
    
    for i in range(first_brace + 1, len(content)):
        if content[i] == '{':
            brace_count += 1
        elif content[i] == '}':
            brace_count -= 1
            if brace_count == 0:
                de_end = i
                break
    
    de_block = content[first_brace:de_end+1]
    
    if 'onDispose' not in de_block:
        return False, "DisposableEffect块内缺少onDispose"
    
    return True, "DisposableEffect结构正确"


def main():
    # 读取MapView.kt文件 - 使用绝对路径
    mapview_path = r"D:\CSLearning\Software_Contest\Frontend\lifehub-frontend\app\src\main\java\com\example\lifehub\ui\components\MapView.kt"
    
    print(f"验证文件: {mapview_path}")
    print("=" * 60)
    
    if not os.path.exists(mapview_path):
        print(f"错误: 文件不存在 - {mapview_path}")
        return False
    
    with open(mapview_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    all_passed = True
    
    # 1. 检查括号匹配
    print("\n1. 检查括号匹配...")
    passed, msg = check_bracket_balance(content)
    print(f"   结果: {'✓ 通过' if passed else '✗ 失败'} - {msg}")
    all_passed = all_passed and passed
    
    # 2. 检查import语句
    print("\n2. 检查高德地图SDK import...")
    passed, missing = check_imports(content)
    if passed:
        print("   结果: ✓ 通过 - 所有必需import都存在")
    else:
        print(f"   结果: ✗ 失败 - 缺少: {missing}")
    all_passed = all_passed and passed
    
    # 3. 检查函数声明
    print("\n3. 检查函数声明...")
    passed, missing = check_function_declarations(content)
    if passed:
        print("   结果: ✓ 通过 - 所有函数都已声明")
    else:
        print(f"   结果: ✗ 失败 - 缺少: {missing}")
    all_passed = all_passed and passed
    
    # 4. 检查数据类
    print("\n4. 检查数据类声明...")
    passed, missing = check_data_classes(content)
    if passed:
        print("   结果: ✓ 通过 - 所有数据类都已声明")
    else:
        print(f"   结果: ✗ 失败 - 缺少: {missing}")
    all_passed = all_passed and passed
    
    # 5. 检查DisposableEffect结构
    print("\n5. 检查DisposableEffect结构...")
    passed, msg = check_disposable_effect_structure(content)
    print(f"   结果: {'✓ 通过' if passed else '✗ 失败'} - {msg}")
    all_passed = all_passed and passed
    
    # 6. 检查3D地图SDK包名
    print("\n6. 检查3D地图SDK包名...")
    if 'com.amap.api.maps' in content and 'com.amap.api.map2d' not in content:
        print("   结果: ✓ 通过 - 使用正确的3D地图SDK包名")
    else:
        print("   结果: ✗ 失败 - 包名可能不正确")
        all_passed = False
    
    print("\n" + "=" * 60)
    if all_passed:
        print("总结: ✓ 所有检查通过！代码结构正确。")
    else:
        print("总结: ✗ 部分检查未通过，请检查上述问题。")
    
    return all_passed


if __name__ == '__main__':
    import sys
    success = main()
    sys.exit(0 if success else 1)
