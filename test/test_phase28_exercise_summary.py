"""
Phase 28: 运动结算展示 - 代码结构与逻辑验证

验证内容：
1. ExerciseSummaryPage.kt 文件存在且包含必要组件
2. Screen.kt 中定义了 ExerciseSummary 路由
3. MainNavigation.kt 中注册了 ExerciseSummary 路由
4. ApiService.kt 中有 createExerciseRecord 方法
5. ExerciseData.kt 中有必要的数据模型
6. ExerciseViewModel.kt 中有 saveExerciseRecord 方法
7. ExerciseTrackingPage.kt 导航到结算页
8. 前后端API契约一致性
"""
import os
import re
import unittest


class TestPhase28ExerciseSummary(unittest.TestCase):
    """Phase 28: 运动结算展示 - 全面代码验证"""

    @classmethod
    def setUpClass(cls):
        """设置文件路径"""
        # 获取项目根目录
        test_dir = os.path.dirname(os.path.abspath(__file__))
        cls.frontend_root = os.path.dirname(test_dir)  # lifehub-frontend/
        cls.src_main = os.path.join(
            cls.frontend_root, "app", "src", "main",
            "java", "com", "example", "lifehub"
        )
        cls.ui_screen = os.path.join(cls.src_main, "ui", "screen")
        cls.navigation = os.path.join(cls.src_main, "navigation")
        cls.network = os.path.join(cls.src_main, "network")
        cls.data = os.path.join(cls.src_main, "data")
        cls.viewmodel = os.path.join(cls.src_main, "viewmodel")

    def _read_file(self, filepath):
        """读取文件内容"""
        with open(filepath, "r", encoding="utf-8") as f:
            return f.read()

    # ==================== 1. 文件存在性验证 ====================

    def test_01_exercise_summary_page_exists(self):
        """验证 ExerciseSummaryPage.kt 文件存在"""
        filepath = os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        self.assertTrue(
            os.path.exists(filepath),
            f"ExerciseSummaryPage.kt 不存在: {filepath}"
        )

    def test_02_screen_kt_exists(self):
        """验证 Screen.kt 文件存在"""
        filepath = os.path.join(self.navigation, "Screen.kt")
        self.assertTrue(os.path.exists(filepath))

    def test_03_main_navigation_exists(self):
        """验证 MainNavigation.kt 文件存在"""
        filepath = os.path.join(self.navigation, "MainNavigation.kt")
        self.assertTrue(os.path.exists(filepath))

    # ==================== 2. Screen.kt 路由验证 ====================

    def test_04_screen_has_exercise_summary_route(self):
        """验证 Screen.kt 中定义了 ExerciseSummary 路由"""
        content = self._read_file(os.path.join(self.navigation, "Screen.kt"))
        self.assertIn("ExerciseSummary", content,
                       "Screen.kt 中未定义 ExerciseSummary 路由")

    def test_05_screen_exercise_summary_has_create_route(self):
        """验证 ExerciseSummary 有 createRoute 方法"""
        content = self._read_file(os.path.join(self.navigation, "Screen.kt"))
        self.assertIn("fun createRoute", content,
                       "ExerciseSummary 缺少 createRoute 方法")

    def test_06_screen_exercise_summary_route_has_parameters(self):
        """验证路由包含必要参数"""
        content = self._read_file(os.path.join(self.navigation, "Screen.kt"))
        # 验证包含关键参数
        for param in ["planId", "exerciseType", "distance", "duration", "calories"]:
            self.assertIn(param, content,
                          f"ExerciseSummary 路由缺少参数: {param}")

    # ==================== 3. MainNavigation.kt 验证 ====================

    def test_07_navigation_has_exercise_summary(self):
        """验证 MainNavigation.kt 中注册了 ExerciseSummary 路由"""
        content = self._read_file(
            os.path.join(self.navigation, "MainNavigation.kt")
        )
        self.assertIn("ExerciseSummary", content,
                       "MainNavigation.kt 中未注册 ExerciseSummary 路由")

    def test_08_navigation_exercise_summary_composable(self):
        """验证导航中有 ExerciseSummaryPage composable"""
        content = self._read_file(
            os.path.join(self.navigation, "MainNavigation.kt")
        )
        self.assertIn("ExerciseSummaryPage", content,
                       "MainNavigation.kt 中未引用 ExerciseSummaryPage")

    # ==================== 4. ApiService.kt 验证 ====================

    def test_09_api_service_has_create_exercise_record(self):
        """验证 ApiService.kt 中有 createExerciseRecord 方法"""
        content = self._read_file(
            os.path.join(self.network, "ApiService.kt")
        )
        self.assertIn("createExerciseRecord", content,
                       "ApiService.kt 缺少 createExerciseRecord 方法")

    def test_10_api_service_exercise_record_post(self):
        """验证接口使用 POST 方法"""
        content = self._read_file(
            os.path.join(self.network, "ApiService.kt")
        )
        self.assertIn('@POST("/api/exercise/record")', content,
                       "ApiService.kt 缺少 POST /api/exercise/record 注解")

    def test_11_api_service_uses_correct_request_type(self):
        """验证接口使用正确的请求类型"""
        content = self._read_file(
            os.path.join(self.network, "ApiService.kt")
        )
        self.assertIn("CreateExerciseRecordRequest", content,
                       "ApiService.kt 缺少 CreateExerciseRecordRequest 类型")

    def test_12_api_service_uses_correct_response_type(self):
        """验证接口使用正确的响应类型"""
        content = self._read_file(
            os.path.join(self.network, "ApiService.kt")
        )
        self.assertIn("CreateExerciseRecordResponse", content,
                       "ApiService.kt 缺少 CreateExerciseRecordResponse 类型")

    # ==================== 5. ExerciseData.kt 数据模型验证 ====================

    def test_13_exercise_data_has_create_request(self):
        """验证 ExerciseData.kt 中有 CreateExerciseRecordRequest"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("CreateExerciseRecordRequest", content,
                       "ExerciseData.kt 缺少 CreateExerciseRecordRequest 模型")

    def test_14_exercise_data_has_response(self):
        """验证 ExerciseData.kt 中有 CreateExerciseRecordResponse"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("CreateExerciseRecordResponse", content,
                       "ExerciseData.kt 缺少 CreateExerciseRecordResponse 模型")

    def test_15_exercise_data_has_save_state(self):
        """验证 ExerciseData.kt 中有 SaveExerciseState"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("SaveExerciseState", content,
                       "ExerciseData.kt 缺少 SaveExerciseState 状态类")

    def test_16_request_model_has_required_fields(self):
        """验证请求模型包含所有必要字段"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        required_fields = [
            "userId", "exerciseType", "actualCalories",
            "actualDuration", "exerciseDate"
        ]
        for field in required_fields:
            self.assertIn(field, content,
                          f"CreateExerciseRecordRequest 缺少字段: {field}")

    def test_17_request_model_has_optional_fields(self):
        """验证请求模型包含可选字段"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        optional_fields = [
            "planId", "distance", "startedAt", "endedAt",
            "notes", "plannedCalories", "plannedDuration"
        ]
        for field in optional_fields:
            self.assertIn(field, content,
                          f"CreateExerciseRecordRequest 缺少可选字段: {field}")

    def test_18_response_model_has_achievement_fields(self):
        """验证响应模型包含达成率字段"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("caloriesAchievement", content,
                       "响应模型缺少 caloriesAchievement 字段")
        self.assertIn("durationAchievement", content,
                       "响应模型缺少 durationAchievement 字段")

    # ==================== 6. ExerciseViewModel.kt 验证 ====================

    def test_19_viewmodel_has_save_method(self):
        """验证 ExerciseViewModel.kt 中有 saveExerciseRecord 方法"""
        content = self._read_file(
            os.path.join(self.viewmodel, "ExerciseViewModel.kt")
        )
        self.assertIn("saveExerciseRecord", content,
                       "ExerciseViewModel.kt 缺少 saveExerciseRecord 方法")

    def test_20_viewmodel_has_save_state(self):
        """验证 ExerciseViewModel.kt 中有 saveState"""
        content = self._read_file(
            os.path.join(self.viewmodel, "ExerciseViewModel.kt")
        )
        self.assertIn("saveState", content,
                       "ExerciseViewModel.kt 缺少 saveState 状态")

    def test_21_viewmodel_imports_retrofit(self):
        """验证 ViewModel 导入了网络相关依赖"""
        content = self._read_file(
            os.path.join(self.viewmodel, "ExerciseViewModel.kt")
        )
        self.assertIn("RetrofitClient", content,
                       "ExerciseViewModel.kt 缺少 RetrofitClient 导入")

    # ==================== 7. ExerciseTrackingPage.kt 验证 ====================

    def test_22_tracking_page_navigates_to_summary(self):
        """验证 ExerciseTrackingPage 中有导航到结算页的逻辑"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseTrackingPage.kt")
        )
        self.assertIn("ExerciseSummary", content,
                       "ExerciseTrackingPage.kt 中未包含导航到 ExerciseSummary 的逻辑")

    def test_23_tracking_page_passes_data_to_summary(self):
        """验证传递了运动数据到结算页"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseTrackingPage.kt")
        )
        self.assertIn("createRoute", content,
                       "ExerciseTrackingPage.kt 缺少 createRoute 调用")

    # ==================== 8. ExerciseSummaryPage.kt 内容验证 ====================

    def test_24_summary_page_has_composable_function(self):
        """验证 ExerciseSummaryPage 有 Composable 函数"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        self.assertIn("@Composable", content,
                       "ExerciseSummaryPage.kt 缺少 @Composable 注解")
        self.assertIn("fun ExerciseSummaryPage", content,
                       "ExerciseSummaryPage.kt 缺少 ExerciseSummaryPage 函数")

    def test_25_summary_page_has_nav_controller(self):
        """验证结算页接收 NavController"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        self.assertIn("navController", content,
                       "ExerciseSummaryPage.kt 缺少 navController 参数")

    def test_26_summary_page_shows_actual_vs_planned(self):
        """验证结算页展示实际vs计划对比"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        # 应包含对比相关的UI元素
        has_comparison = ("实际" in content or "actual" in content.lower()) and \
                         ("计划" in content or "planned" in content.lower() or "目标" in content)
        self.assertTrue(has_comparison,
                        "ExerciseSummaryPage.kt 缺少实际vs计划对比展示")

    def test_27_summary_page_shows_achievement_rate(self):
        """验证结算页展示达成率"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        has_achievement = "达成" in content or "achievement" in content.lower() or "%" in content
        self.assertTrue(has_achievement,
                        "ExerciseSummaryPage.kt 缺少达成率展示")

    def test_28_summary_page_has_save_button(self):
        """验证结算页有保存按钮"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        has_save = "保存" in content or "save" in content.lower()
        self.assertTrue(has_save,
                        "ExerciseSummaryPage.kt 缺少保存按钮")

    def test_29_summary_page_calls_save_method(self):
        """验证结算页调用保存方法"""
        content = self._read_file(
            os.path.join(self.ui_screen, "ExerciseSummaryPage.kt")
        )
        self.assertIn("saveExerciseRecord", content,
                       "ExerciseSummaryPage.kt 缺少 saveExerciseRecord 调用")

    # ==================== 9. API契约一致性验证 ====================

    def test_30_api_field_names_match_backend(self):
        """验证前端SerializedName与后端字段名一致"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        # 后端字段名（snake_case）
        backend_fields = [
            "user_id", "plan_id", "exercise_type",
            "actual_calories", "actual_duration", "distance",
            "exercise_date", "started_at", "ended_at",
            "planned_calories", "planned_duration"
        ]
        for field in backend_fields:
            self.assertIn(f'"{field}"', content,
                          f"前端缺少与后端匹配的字段 SerializedName: {field}")

    def test_31_response_field_names_match_backend(self):
        """验证响应模型字段名与后端一致"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        backend_response_fields = [
            "calories_achievement", "duration_achievement",
            "created_at"
        ]
        for field in backend_response_fields:
            self.assertIn(f'"{field}"', content,
                          f"响应模型缺少与后端匹配的字段: {field}")

    # ==================== 10. SaveExerciseState 完整性 ====================

    def test_32_save_state_has_idle(self):
        """验证 SaveExerciseState 有 Idle 状态"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("Idle", content)

    def test_33_save_state_has_saving(self):
        """验证 SaveExerciseState 有 Saving 状态"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("Saving", content)

    def test_34_save_state_has_success(self):
        """验证 SaveExerciseState 有 Success 状态"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("Success", content)

    def test_35_save_state_has_error(self):
        """验证 SaveExerciseState 有 Error 状态"""
        content = self._read_file(
            os.path.join(self.data, "ExerciseData.kt")
        )
        self.assertIn("Error", content)


if __name__ == "__main__":
    unittest.main(verbosity=2)
