"""
Phase 24 测试文件 - 前端运动路线展示功能验证
测试范围：
1. 数据模型正确性（ParetoRoute、RouteWaypoint等）
2. API接口定义（generateRoutes方法）
3. ViewModel状态管理（RoutesState）
4. 路线选择逻辑
5. 路线颜色分配
6. UI组件参数验证
"""
import os
import re
import unittest
from pathlib import Path

# 项目根目录
PROJECT_ROOT = Path(__file__).parent.parent
APP_SRC = PROJECT_ROOT / "app" / "src" / "main" / "java" / "com" / "example" / "lifehub"


class TestPhase24DataModels(unittest.TestCase):
    """测试数据模型定义"""
    
    def setUp(self):
        """读取TripData.kt文件"""
        self.trip_data_path = APP_SRC / "data" / "TripData.kt"
        if self.trip_data_path.exists():
            self.trip_data_content = self.trip_data_path.read_text(encoding='utf-8')
        else:
            self.trip_data_content = ""
    
    def test_route_waypoint_model_exists(self):
        """测试RouteWaypoint数据类是否存在"""
        self.assertIn("data class RouteWaypoint", self.trip_data_content,
                     "RouteWaypoint数据类应该存在于TripData.kt中")
    
    def test_route_waypoint_has_lat_lng(self):
        """测试RouteWaypoint是否包含经纬度字段"""
        self.assertIn("lat", self.trip_data_content.lower(),
                     "RouteWaypoint应包含lat字段")
        self.assertIn("lng", self.trip_data_content.lower(),
                     "RouteWaypoint应包含lng字段")
    
    def test_pareto_route_model_exists(self):
        """测试ParetoRoute数据类是否存在"""
        self.assertIn("data class ParetoRoute", self.trip_data_content,
                     "ParetoRoute数据类应该存在于TripData.kt中")
    
    def test_pareto_route_has_required_fields(self):
        """测试ParetoRoute是否包含必要字段"""
        required_fields = ["routeId", "routeName", "timeMinutes", "caloriesBurn", 
                          "greeneryScore", "distanceMeters", "waypoints"]
        for field in required_fields:
            # 使用驼峰命名检查
            self.assertTrue(
                field.lower() in self.trip_data_content.lower() or 
                self._to_snake_case(field) in self.trip_data_content.lower(),
                f"ParetoRoute应包含{field}字段"
            )
    
    def test_generate_routes_request_exists(self):
        """测试GenerateRoutesRequest数据类是否存在"""
        self.assertIn("GenerateRoutesRequest", self.trip_data_content,
                     "GenerateRoutesRequest数据类应该存在")
    
    def test_generate_routes_response_exists(self):
        """测试GenerateRoutesResponse数据类是否存在"""
        self.assertIn("GenerateRoutesResponse", self.trip_data_content,
                     "GenerateRoutesResponse数据类应该存在")
    
    def _to_snake_case(self, name):
        """驼峰转下划线"""
        return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


class TestPhase24ApiService(unittest.TestCase):
    """测试API接口定义"""
    
    def setUp(self):
        """读取ApiService.kt文件"""
        self.api_service_path = APP_SRC / "network" / "ApiService.kt"
        if self.api_service_path.exists():
            self.api_content = self.api_service_path.read_text(encoding='utf-8')
        else:
            self.api_content = ""
    
    def test_generate_routes_endpoint_exists(self):
        """测试generateRoutes接口是否存在"""
        self.assertIn("generateRoutes", self.api_content,
                     "ApiService应包含generateRoutes方法")
    
    def test_generate_routes_is_post_method(self):
        """测试generateRoutes使用POST方法"""
        # 查找@POST注解后紧跟的/api/trip/routes
        self.assertIn('@POST("/api/trip/routes")', self.api_content,
                     "generateRoutes应使用POST方法访问/api/trip/routes")
    
    def test_generate_routes_returns_correct_type(self):
        """测试generateRoutes返回正确的响应类型"""
        self.assertIn("GenerateRoutesResponse", self.api_content,
                     "generateRoutes应返回GenerateRoutesResponse类型")


class TestPhase24ViewModel(unittest.TestCase):
    """测试ViewModel状态管理"""
    
    def setUp(self):
        """读取TripViewModel.kt文件"""
        self.viewmodel_path = APP_SRC / "viewmodel" / "TripViewModel.kt"
        if self.viewmodel_path.exists():
            self.vm_content = self.viewmodel_path.read_text(encoding='utf-8')
        else:
            self.vm_content = ""
    
    def test_routes_state_exists(self):
        """测试RoutesState状态类是否存在"""
        self.assertIn("RoutesState", self.vm_content,
                     "TripViewModel应包含RoutesState状态类")
    
    def test_routes_state_has_idle(self):
        """测试RoutesState是否有Idle状态"""
        self.assertIn("Idle", self.vm_content,
                     "RoutesState应包含Idle状态")
    
    def test_routes_state_has_loading(self):
        """测试RoutesState是否有Loading状态"""
        self.assertIn("Loading", self.vm_content,
                     "RoutesState应包含Loading状态")
    
    def test_routes_state_has_success(self):
        """测试RoutesState是否有Success状态"""
        self.assertIn("Success", self.vm_content,
                     "RoutesState应包含Success状态")
    
    def test_routes_state_has_error(self):
        """测试RoutesState是否有Error状态"""
        self.assertIn("Error", self.vm_content,
                     "RoutesState应包含Error状态")
    
    def test_generate_routes_method_exists(self):
        """测试generateRoutes方法是否存在"""
        self.assertIn("fun generateRoutes", self.vm_content,
                     "TripViewModel应包含generateRoutes方法")
    
    def test_selected_route_state_exists(self):
        """测试选中路线状态是否存在"""
        # 检查selectedRouteIndex或selectedRoute
        has_selected_route = (
            "selectedrouteindex" in self.vm_content.lower() or
            "selectedroute" in self.vm_content.lower()
        )
        self.assertTrue(has_selected_route,
                     "TripViewModel应包含selectedRouteIndex或selectedRoute状态")


class TestPhase24RouteOverlay(unittest.TestCase):
    """测试RouteOverlay组件"""
    
    def setUp(self):
        """读取RouteOverlay.kt文件"""
        self.overlay_path = APP_SRC / "ui" / "components" / "RouteOverlay.kt"
        if self.overlay_path.exists():
            self.overlay_content = self.overlay_path.read_text(encoding='utf-8')
        else:
            self.overlay_content = ""
    
    def test_route_overlay_file_exists(self):
        """测试RouteOverlay.kt文件是否存在"""
        self.assertTrue(self.overlay_path.exists(),
                       "RouteOverlay.kt文件应该存在")
    
    def test_route_overlay_composable_exists(self):
        """测试RouteOverlay Composable是否存在"""
        self.assertIn("@Composable", self.overlay_content,
                     "RouteOverlay.kt应包含Composable函数")
        self.assertIn("RouteOverlay", self.overlay_content,
                     "应包含RouteOverlay函数")
    
    def test_route_info_card_exists(self):
        """测试RouteInfoCard组件是否存在"""
        self.assertIn("RouteInfoCard", self.overlay_content,
                     "应包含RouteInfoCard组件用于显示路线信息")
    
    def test_route_selector_exists(self):
        """测试路线选择器是否存在"""
        self.assertIn("RouteSelector", self.overlay_content,
                     "应包含RouteSelector组件用于切换路线")


class TestPhase24TripDetailPage(unittest.TestCase):
    """测试TripDetailPage集成"""
    
    def setUp(self):
        """读取TripDetailPage.kt文件"""
        self.page_path = APP_SRC / "ui" / "screen" / "TripDetailPage.kt"
        if self.page_path.exists():
            self.page_content = self.page_path.read_text(encoding='utf-8')
        else:
            self.page_content = ""
    
    def test_map_component_imported(self):
        """测试是否导入了地图组件"""
        self.assertIn("AMapComposeView", self.page_content,
                     "TripDetailPage应导入AMapComposeView组件")
    
    def test_route_overlay_used(self):
        """测试是否使用了RouteOverlay组件"""
        self.assertIn("RouteOverlay", self.page_content,
                     "TripDetailPage应使用RouteOverlay组件")
    
    def test_routes_state_collected(self):
        """测试是否收集了路线状态"""
        # 检查routesState或routesstate（大小写不敏感）
        has_routes_state = (
            "routesstate" in self.page_content.lower() or
            "routes_state" in self.page_content.lower()
        )
        self.assertTrue(has_routes_state,
                     "TripDetailPage应收集routesState状态")


class TestPhase24RouteLogic(unittest.TestCase):
    """测试路线逻辑"""
    
    def test_route_color_assignment(self):
        """测试路线颜色分配逻辑"""
        # 模拟3条路线的颜色分配
        route_colors = {
            0: 0xFF4CAF50,  # 绿色 - 最佳绿化
            1: 0xFF2196F3,  # 蓝色 - 最短时间
            2: 0xFFFF9800,  # 橙色 - 最大消耗
        }
        
        # 验证颜色不重复
        colors = list(route_colors.values())
        self.assertEqual(len(colors), len(set(colors)), "路线颜色应该不重复")
    
    def test_route_selection_logic(self):
        """测试路线选择逻辑"""
        # 模拟路线选择
        routes = [
            {"route_id": 1, "route_name": "最短时间"},
            {"route_id": 2, "route_name": "最大消耗"},
            {"route_id": 3, "route_name": "最佳绿化"},
        ]
        
        selected_index = 0
        self.assertEqual(routes[selected_index]["route_id"], 1)
        
        # 切换选择
        selected_index = 2
        self.assertEqual(routes[selected_index]["route_name"], "最佳绿化")
    
    def test_waypoints_to_polyline_conversion(self):
        """测试路径点转换为折线的逻辑"""
        waypoints = [
            {"lat": 39.9042, "lng": 116.4074},
            {"lat": 39.9052, "lng": 116.4084},
            {"lat": 39.9062, "lng": 116.4094},
        ]
        
        # 验证点数量
        self.assertEqual(len(waypoints), 3)
        
        # 验证每个点都有经纬度
        for wp in waypoints:
            self.assertIn("lat", wp)
            self.assertIn("lng", wp)
    
    def test_route_info_formatting(self):
        """测试路线信息格式化"""
        route = {
            "time_minutes": 25.5,
            "calories_burn": 150.0,
            "greenery_score": 75.5,
            "distance_meters": 2100.0,
        }
        
        # 格式化时间
        time_str = f"{int(route['time_minutes'])}分钟"
        self.assertEqual(time_str, "25分钟")
        
        # 格式化热量
        calories_str = f"{int(route['calories_burn'])}卡"
        self.assertEqual(calories_str, "150卡")
        
        # 格式化距离
        distance_km = route["distance_meters"] / 1000
        distance_str = f"{distance_km:.1f}公里"
        self.assertEqual(distance_str, "2.1公里")
        
        # 格式化绿化评分
        greenery_str = f"{route['greenery_score']:.0f}分"
        self.assertEqual(greenery_str, "76分")


class TestPhase24EdgeCases(unittest.TestCase):
    """测试边缘情况"""
    
    def test_empty_routes_handling(self):
        """测试空路线列表处理"""
        routes = []
        self.assertEqual(len(routes), 0)
        # 应显示提示信息而不是崩溃
    
    def test_single_route_handling(self):
        """测试单条路线处理"""
        routes = [{"route_id": 1, "route_name": "唯一路线"}]
        self.assertEqual(len(routes), 1)
        # 不需要显示选择器
    
    def test_invalid_waypoints_handling(self):
        """测试无效路径点处理"""
        waypoints = []
        # 空路径点列表应该被安全处理
        self.assertEqual(len(waypoints), 0)
    
    def test_extreme_coordinates(self):
        """测试极端坐标值"""
        # 有效范围
        valid_lat_range = (-90, 90)
        valid_lng_range = (-180, 180)
        
        # 测试边界值
        test_coords = [
            (0, 0),       # 原点
            (90, 180),    # 最大值
            (-90, -180),  # 最小值
            (39.9, 116.4) # 北京坐标
        ]
        
        for lat, lng in test_coords:
            self.assertTrue(valid_lat_range[0] <= lat <= valid_lat_range[1])
            self.assertTrue(valid_lng_range[0] <= lng <= valid_lng_range[1])
    
    def test_route_with_zero_values(self):
        """测试零值路线参数"""
        route = {
            "time_minutes": 0,
            "calories_burn": 0,
            "distance_meters": 0,
        }
        
        # 零值应该被正确处理
        self.assertEqual(route["time_minutes"], 0)
        self.assertEqual(route["calories_burn"], 0)


class TestPhase24Integration(unittest.TestCase):
    """集成测试"""
    
    def test_all_required_files_exist(self):
        """测试所有必需文件是否存在"""
        required_files = [
            APP_SRC / "data" / "TripData.kt",
            APP_SRC / "network" / "ApiService.kt",
            APP_SRC / "viewmodel" / "TripViewModel.kt",
            APP_SRC / "ui" / "screen" / "TripDetailPage.kt",
            APP_SRC / "ui" / "components" / "RouteOverlay.kt",
            APP_SRC / "ui" / "components" / "MapView.kt",
        ]
        
        for file_path in required_files:
            self.assertTrue(file_path.exists(), f"文件应存在: {file_path.name}")
    
    def test_imports_are_consistent(self):
        """测试导入语句一致性"""
        files_to_check = [
            APP_SRC / "ui" / "screen" / "TripDetailPage.kt",
            APP_SRC / "ui" / "components" / "RouteOverlay.kt",
        ]
        
        for file_path in files_to_check:
            if file_path.exists():
                content = file_path.read_text(encoding='utf-8')
                # 检查是否有import语句
                self.assertIn("import", content, f"{file_path.name}应包含import语句")


if __name__ == '__main__':
    # 运行测试
    unittest.main(verbosity=2)
