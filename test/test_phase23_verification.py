"""
Phase 23: 前端 - 地图SDK集成 验证测试

该脚本验证Phase 23实现的代码结构正确性：
1. build.gradle.kts中是否添加了高德地图SDK依赖
2. AndroidManifest.xml中是否配置了必要的权限和API Key
3. MapView.kt组件是否正确实现
4. 测试文件是否存在且结构正确
"""

import os
import re
import sys

# 项目路径
FRONTEND_PATH = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP_PATH = os.path.join(FRONTEND_PATH, "app")
SRC_PATH = os.path.join(APP_PATH, "src", "main", "java", "com", "example", "lifehub")

# 测试结果
test_results = []
passed = 0
failed = 0


def test_passed(name, message=""):
    global passed
    passed += 1
    test_results.append(f"✅ {name}: {message}" if message else f"✅ {name}")
    print(f"✅ PASS: {name}")


def test_failed(name, message=""):
    global failed
    failed += 1
    test_results.append(f"❌ {name}: {message}" if message else f"❌ {name}")
    print(f"❌ FAIL: {name} - {message}")


def read_file_content(filepath):
    """读取文件内容"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        return None


# ========== 测试1: build.gradle.kts 高德地图SDK依赖 ==========
def test_gradle_amap_dependency():
    """验证build.gradle.kts中是否添加了高德地图SDK依赖"""
    gradle_path = os.path.join(APP_PATH, "build.gradle.kts")
    content = read_file_content(gradle_path)
    
    if content is None:
        test_failed("Gradle依赖检查", "无法读取build.gradle.kts文件")
        return
    
    # 检查高德地图SDK依赖
    required_deps = [
        r'com\.amap\.api:map2d',
        r'com\.amap\.api:location',
        r'com\.amap\.api:search'
    ]
    
    all_found = True
    for dep in required_deps:
        if not re.search(dep, content):
            test_failed(f"Gradle依赖: {dep}", "未找到该依赖")
            all_found = False
    
    if all_found:
        test_passed("Gradle高德地图SDK依赖", "map2d, location, search 依赖均已添加")
    
    # 检查是否使用固定版本号（不是latest.integration）
    if 'latest.integration' in content:
        test_failed("Gradle版本号检查", "不应使用latest.integration，应使用固定版本号")
    else:
        test_passed("Gradle版本号检查", "使用固定版本号")


# ========== 测试2: AndroidManifest.xml 权限和API Key配置 ==========
def test_manifest_configuration():
    """验证AndroidManifest.xml中的权限和API Key配置"""
    manifest_path = os.path.join(APP_PATH, "src", "main", "AndroidManifest.xml")
    content = read_file_content(manifest_path)
    
    if content is None:
        test_failed("Manifest配置检查", "无法读取AndroidManifest.xml文件")
        return
    
    # 检查必要权限
    required_permissions = [
        'ACCESS_FINE_LOCATION',
        'ACCESS_COARSE_LOCATION',
        'INTERNET',
        'ACCESS_NETWORK_STATE',
        'ACCESS_WIFI_STATE'
    ]
    
    permissions_found = []
    for perm in required_permissions:
        if perm in content:
            permissions_found.append(perm)
    
    if len(permissions_found) == len(required_permissions):
        test_passed("Manifest权限配置", f"所有{len(required_permissions)}个必要权限已声明")
    else:
        missing = set(required_permissions) - set(permissions_found)
        test_failed("Manifest权限配置", f"缺少权限: {missing}")
    
    # 检查API Key配置
    if 'com.amap.api.v2.apikey' in content:
        test_passed("Manifest API Key配置", "高德地图API Key配置项已添加")
    else:
        test_failed("Manifest API Key配置", "未找到高德地图API Key配置")
    
    # 检查定位服务配置
    if 'com.amap.api.location.APSService' in content:
        test_passed("Manifest定位服务配置", "高德定位服务已配置")
    else:
        test_failed("Manifest定位服务配置", "未找到高德定位服务配置")


# ========== 测试3: MapView.kt 组件实现 ==========
def test_mapview_component():
    """验证MapView.kt组件的实现"""
    mapview_path = os.path.join(SRC_PATH, "ui", "components", "MapView.kt")
    content = read_file_content(mapview_path)
    
    if content is None:
        test_failed("MapView组件", "MapView.kt文件不存在")
        return
    
    test_passed("MapView组件存在", "MapView.kt文件已创建")
    
    # 检查关键组件
    checks = [
        ('AMapComposeView', '主Compose组件'),
        ('AMapViewState', '状态类'),
        ('MarkerData', '标记点数据类'),
        ('PolylineData', '路线数据类'),
        ('LatLngPoint', '经纬度点数据类'),
        ('SimpleMapView', '简化地图组件'),
        ('RouteMapView', '路线地图组件'),
    ]
    
    for keyword, desc in checks:
        if keyword in content:
            test_passed(f"MapView组件: {desc}", f"包含 {keyword}")
        else:
            test_failed(f"MapView组件: {desc}", f"缺少 {keyword}")
    
    # 检查高德地图SDK导入
    amap_imports = [
        'com.amap.api.maps.AMap',
        'com.amap.api.maps.MapView',
        'com.amap.api.maps.model.LatLng',
        'com.amap.api.maps.model.MarkerOptions',
        'com.amap.api.maps.model.PolylineOptions'
    ]
    
    imports_found = sum(1 for imp in amap_imports if imp in content)
    if imports_found == len(amap_imports):
        test_passed("MapView高德SDK导入", f"所有{len(amap_imports)}个必要导入已添加")
    else:
        test_failed("MapView高德SDK导入", f"只找到 {imports_found}/{len(amap_imports)} 个导入")
    
    # 检查生命周期管理
    if 'DisposableEffect' in content and 'onDispose' in content:
        test_passed("MapView生命周期管理", "使用DisposableEffect管理生命周期")
    else:
        test_failed("MapView生命周期管理", "缺少DisposableEffect生命周期管理")
    
    # 检查remember同步初始化（修复后的代码）
    if 'val mapView = remember' in content:
        test_passed("MapView同步初始化", "使用remember同步创建MapView")
    else:
        test_failed("MapView同步初始化", "应使用remember同步创建MapView，避免竞态条件")


# ========== 测试4: 测试文件存在性 ==========
def test_test_files_exist():
    """验证测试文件是否存在"""
    test_files = [
        ("app/src/test/java/com/example/lifehub/ui/components/MapViewTest.kt", "单元测试"),
        ("app/src/androidTest/java/com/example/lifehub/ui/components/AmapSdkIntegrationTest.kt", "集成测试")
    ]
    
    for rel_path, desc in test_files:
        full_path = os.path.join(FRONTEND_PATH, rel_path)
        if os.path.exists(full_path):
            test_passed(f"测试文件: {desc}", f"{rel_path} 存在")
        else:
            test_failed(f"测试文件: {desc}", f"{rel_path} 不存在")


# ========== 测试5: 单元测试内容验证 ==========
def test_unit_test_content():
    """验证单元测试文件的内容"""
    test_path = os.path.join(FRONTEND_PATH, "app/src/test/java/com/example/lifehub/ui/components/MapViewTest.kt")
    content = read_file_content(test_path)
    
    if content is None:
        test_failed("单元测试内容", "无法读取MapViewTest.kt")
        return
    
    # 检查测试方法数量
    test_methods = re.findall(r'@Test\s+fun\s+\w+', content)
    if len(test_methods) >= 10:
        test_passed("单元测试方法数量", f"包含 {len(test_methods)} 个测试方法")
    else:
        test_failed("单元测试方法数量", f"只有 {len(test_methods)} 个测试方法，应至少有10个")
    
    # 检查关键测试
    key_tests = [
        'testMarkerDataCreation',
        'testPolylineDataCreation',
        'testLatLngPointCreation',
        'testAMapViewState'
    ]
    
    for test_name in key_tests:
        if test_name in content:
            test_passed(f"单元测试: {test_name}", "测试存在")
        else:
            test_failed(f"单元测试: {test_name}", "测试不存在")


# ========== 测试6: 集成测试内容验证 ==========
def test_integration_test_content():
    """验证集成测试文件的内容"""
    test_path = os.path.join(FRONTEND_PATH, "app/src/androidTest/java/com/example/lifehub/ui/components/AmapSdkIntegrationTest.kt")
    content = read_file_content(test_path)
    
    if content is None:
        test_failed("集成测试内容", "无法读取AmapSdkIntegrationTest.kt")
        return
    
    # 检查测试方法
    test_methods = re.findall(r'@Test\s+fun\s+\w+', content)
    if len(test_methods) >= 3:
        test_passed("集成测试方法数量", f"包含 {len(test_methods)} 个测试方法")
    else:
        test_failed("集成测试方法数量", f"只有 {len(test_methods)} 个测试方法")
    
    # 检查SDK依赖验证测试
    if 'testAmapSdkDependencyAdded' in content:
        test_passed("集成测试: SDK依赖验证", "测试存在")
    else:
        test_failed("集成测试: SDK依赖验证", "测试不存在")
    
    # 检查API Key配置验证测试
    if 'testAmapApiKeyConfiguredInManifest' in content:
        test_passed("集成测试: API Key配置验证", "测试存在")
    else:
        test_failed("集成测试: API Key配置验证", "测试不存在")


def main():
    print("=" * 60)
    print("Phase 23: 前端 - 地图SDK集成 验证测试")
    print("=" * 60)
    print()
    
    # 运行所有测试
    test_gradle_amap_dependency()
    print()
    test_manifest_configuration()
    print()
    test_mapview_component()
    print()
    test_test_files_exist()
    print()
    test_unit_test_content()
    print()
    test_integration_test_content()
    
    # 输出总结
    print()
    print("=" * 60)
    print("测试总结")
    print("=" * 60)
    print(f"通过: {passed}")
    print(f"失败: {failed}")
    print(f"总计: {passed + failed}")
    print(f"通过率: {passed / (passed + failed) * 100:.1f}%")
    print()
    
    if failed > 0:
        print("失败的测试:")
        for result in test_results:
            if result.startswith("❌"):
                print(f"  {result}")
    
    # 返回退出码
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
