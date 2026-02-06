"""
Phase 35: 端云数据同步 - 代码结构与完整性验证
验证所有同步相关文件的存在性、代码结构、关键方法签名等
"""
import os
import re
import unittest

# 项目根目录
FRONTEND_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_ROOT = os.path.join(FRONTEND_ROOT, "app", "src", "main", "java", "com", "example", "lifehub")
SYNC_DIR = os.path.join(SRC_ROOT, "data", "sync")
REPO_DIR = os.path.join(SRC_ROOT, "data", "repository")
TEST_DIR = os.path.join(FRONTEND_ROOT, "app", "src", "test", "java", "com", "example", "lifehub")


class TestPhase35FileStructure(unittest.TestCase):
    """验证Phase 35所需文件是否全部存在"""

    def test_sync_directory_exists(self):
        self.assertTrue(os.path.isdir(SYNC_DIR), f"sync目录不存在: {SYNC_DIR}")

    def test_sync_manager_exists(self):
        path = os.path.join(SYNC_DIR, "SyncManager.kt")
        self.assertTrue(os.path.isfile(path), f"SyncManager.kt不存在: {path}")

    def test_sync_status_exists(self):
        path = os.path.join(SYNC_DIR, "SyncStatus.kt")
        self.assertTrue(os.path.isfile(path), f"SyncStatus.kt不存在: {path}")

    def test_network_monitor_exists(self):
        path = os.path.join(SYNC_DIR, "NetworkMonitor.kt")
        self.assertTrue(os.path.isfile(path), f"NetworkMonitor.kt不存在: {path}")

    def test_conflict_resolver_exists(self):
        path = os.path.join(SYNC_DIR, "ConflictResolver.kt")
        self.assertTrue(os.path.isfile(path), f"ConflictResolver.kt不存在: {path}")

    def test_diet_repository_exists(self):
        path = os.path.join(REPO_DIR, "DietRepository.kt")
        self.assertTrue(os.path.isfile(path), f"DietRepository.kt不存在: {path}")

    def test_exercise_repository_exists(self):
        path = os.path.join(REPO_DIR, "ExerciseRepository.kt")
        self.assertTrue(os.path.isfile(path), f"ExerciseRepository.kt不存在: {path}")

    def test_kotlin_test_exists(self):
        path = os.path.join(TEST_DIR, "Phase35SyncManagerTest.kt")
        self.assertTrue(os.path.isfile(path), f"Phase35SyncManagerTest.kt不存在: {path}")


class TestSyncManagerContent(unittest.TestCase):
    """验证SyncManager.kt的关键内容"""

    @classmethod
    def setUpClass(cls):
        path = os.path.join(SYNC_DIR, "SyncManager.kt")
        with open(path, "r", encoding="utf-8") as f:
            cls.content = f.read()

    def test_package_declaration(self):
        self.assertIn("package com.example.lifehub.data.sync", self.content)

    def test_class_declaration(self):
        self.assertIn("class SyncManager", self.content)

    def test_sync_all_method(self):
        self.assertIn("suspend fun syncAll(userId: Int): SyncResult", self.content)

    def test_upload_unsynced_method(self):
        self.assertIn("suspend fun uploadUnsyncedData(userId: Int): SyncResult", self.content)

    def test_download_from_server_method(self):
        self.assertIn("suspend fun downloadFromServer(userId: Int): SyncResult", self.content)

    def test_upload_only_sync_method(self):
        self.assertIn("suspend fun uploadOnlySync(userId: Int): SyncResult", self.content)

    def test_start_auto_sync_method(self):
        self.assertIn("fun startAutoSync(userId: Int)", self.content)

    def test_stop_auto_sync_method(self):
        self.assertIn("fun stopAutoSync()", self.content)

    def test_update_pending_count_method(self):
        self.assertIn("suspend fun updatePendingCount(userId: Int)", self.content)

    def test_is_network_available_method(self):
        self.assertIn("fun isNetworkAvailable(): Boolean", self.content)

    def test_destroy_method(self):
        self.assertIn("fun destroy()", self.content)

    def test_uses_api_service(self):
        self.assertIn("apiService", self.content)

    def test_uses_diet_repository(self):
        self.assertIn("dietRepository", self.content)

    def test_uses_exercise_repository(self):
        self.assertIn("exerciseRepository", self.content)

    def test_uses_trip_repository(self):
        self.assertIn("tripRepository", self.content)

    def test_uses_user_repository(self):
        self.assertIn("userRepository", self.content)

    def test_uses_network_monitor(self):
        self.assertIn("networkMonitor", self.content)

    def test_uses_coroutines(self):
        self.assertIn("kotlinx.coroutines", self.content)

    def test_uses_flow(self):
        self.assertIn("StateFlow", self.content)

    def test_sync_status_flow(self):
        self.assertIn("syncStatus", self.content)

    def test_pending_changes_count(self):
        self.assertIn("pendingChangesCount", self.content)

    def test_singleton_pattern(self):
        self.assertIn("companion object", self.content)
        self.assertIn("INSTANCE", self.content)

    def test_phase35_comment(self):
        self.assertIn("Phase 35", self.content)


class TestSyncStatusContent(unittest.TestCase):
    """验证SyncStatus.kt的关键内容"""

    @classmethod
    def setUpClass(cls):
        path = os.path.join(SYNC_DIR, "SyncStatus.kt")
        with open(path, "r", encoding="utf-8") as f:
            cls.content = f.read()

    def test_sealed_class(self):
        self.assertIn("sealed class SyncStatus", self.content)

    def test_idle_state(self):
        self.assertIn("object Idle", self.content)

    def test_syncing_state(self):
        self.assertIn("object Syncing", self.content)

    def test_success_state(self):
        self.assertIn("data class Success", self.content)

    def test_error_state(self):
        self.assertIn("data class Error", self.content)

    def test_sync_result_class(self):
        self.assertIn("data class SyncResult", self.content)

    def test_sync_result_total_synced(self):
        self.assertIn("totalSynced", self.content)

    def test_sync_result_has_errors(self):
        self.assertIn("hasErrors", self.content)

    def test_sync_result_is_full_success(self):
        self.assertIn("isFullSuccess", self.content)

    def test_network_state_enum(self):
        self.assertIn("enum class NetworkState", self.content)

    def test_network_state_available(self):
        self.assertIn("AVAILABLE", self.content)

    def test_network_state_unavailable(self):
        self.assertIn("UNAVAILABLE", self.content)

    def test_sync_config_class(self):
        self.assertIn("data class SyncConfig", self.content)

    def test_sync_config_interval(self):
        self.assertIn("syncIntervalMs", self.content)

    def test_sync_config_retry(self):
        self.assertIn("retryDelayMs", self.content)

    def test_sync_config_max_retry(self):
        self.assertIn("maxRetryCount", self.content)

    def test_sync_config_auto_sync(self):
        self.assertIn("enableAutoSync", self.content)


class TestNetworkMonitorContent(unittest.TestCase):
    """验证NetworkMonitor.kt的关键内容"""

    @classmethod
    def setUpClass(cls):
        path = os.path.join(SYNC_DIR, "NetworkMonitor.kt")
        with open(path, "r", encoding="utf-8") as f:
            cls.content = f.read()

    def test_class_declaration(self):
        self.assertIn("class NetworkMonitor", self.content)

    def test_is_network_available(self):
        self.assertIn("fun isNetworkAvailable(): Boolean", self.content)

    def test_observe_network_state(self):
        self.assertIn("fun observeNetworkState(): Flow<NetworkState>", self.content)

    def test_uses_connectivity_manager(self):
        self.assertIn("ConnectivityManager", self.content)

    def test_uses_network_callback(self):
        self.assertIn("NetworkCallback", self.content)

    def test_uses_callback_flow(self):
        self.assertIn("callbackFlow", self.content)

    def test_uses_distinct_until_changed(self):
        self.assertIn("distinctUntilChanged", self.content)

    def test_checks_internet_capability(self):
        self.assertIn("NET_CAPABILITY_INTERNET", self.content)

    def test_checks_validated_capability(self):
        self.assertIn("NET_CAPABILITY_VALIDATED", self.content)


class TestConflictResolverContent(unittest.TestCase):
    """验证ConflictResolver.kt的关键内容"""

    @classmethod
    def setUpClass(cls):
        path = os.path.join(SYNC_DIR, "ConflictResolver.kt")
        with open(path, "r", encoding="utf-8") as f:
            cls.content = f.read()

    def test_object_declaration(self):
        self.assertIn("object ConflictResolver", self.content)

    def test_resolve_diet_record_method(self):
        self.assertIn("fun resolveDietRecord", self.content)

    def test_should_update_method(self):
        self.assertIn("fun shouldUpdateLocalDietRecord", self.content)

    def test_find_matching_local_record(self):
        self.assertIn("fun findMatchingLocalRecord", self.content)

    def test_find_matching_exercise_record(self):
        self.assertIn("fun findMatchingLocalExerciseRecord", self.content)

    def test_server_wins_strategy_comment(self):
        self.assertIn("服务端优先", self.content)

    def test_handles_unsynced_records(self):
        # 检查是否处理了未同步的离线记录
        self.assertIn("isSynced", self.content)
        self.assertIn("serverId", self.content)


class TestRepositoryModifications(unittest.TestCase):
    """验证Repository层的Phase 35修改"""

    def test_diet_repository_has_get_all_entities(self):
        path = os.path.join(REPO_DIR, "DietRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("suspend fun getAllEntities(userId: Int)", content,
                       "DietRepository缺少getAllEntities方法")

    def test_exercise_repository_has_get_all_entities(self):
        path = os.path.join(REPO_DIR, "ExerciseRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("suspend fun getAllEntities(userId: Int)", content,
                       "ExerciseRepository缺少getAllEntities方法")

    def test_diet_repository_has_get_unsynced(self):
        path = os.path.join(REPO_DIR, "DietRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("getUnsyncedRecords", content)

    def test_diet_repository_has_mark_as_synced(self):
        path = os.path.join(REPO_DIR, "DietRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("markAsSynced", content)

    def test_exercise_repository_has_get_unsynced(self):
        path = os.path.join(REPO_DIR, "ExerciseRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("getUnsyncedRecords", content)

    def test_exercise_repository_has_mark_as_synced(self):
        path = os.path.join(REPO_DIR, "ExerciseRepository.kt")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("markAsSynced", content)


class TestCodeQuality(unittest.TestCase):
    """验证代码质量：注释、命名规范等"""

    def _read_file(self, *path_parts):
        path = os.path.join(*path_parts)
        with open(path, "r", encoding="utf-8") as f:
            return f.read()

    def test_all_sync_files_have_phase35_comment(self):
        for filename in ["SyncManager.kt", "SyncStatus.kt", "NetworkMonitor.kt", "ConflictResolver.kt"]:
            content = self._read_file(SYNC_DIR, filename)
            self.assertIn("Phase 35", content, f"{filename}缺少Phase 35注释")

    def test_all_sync_files_have_package_declaration(self):
        for filename in ["SyncManager.kt", "SyncStatus.kt", "NetworkMonitor.kt", "ConflictResolver.kt"]:
            content = self._read_file(SYNC_DIR, filename)
            self.assertIn("package com.example.lifehub.data.sync", content,
                          f"{filename}缺少正确的package声明")

    def test_no_hardcoded_api_keys(self):
        for filename in os.listdir(SYNC_DIR):
            if filename.endswith(".kt"):
                content = self._read_file(SYNC_DIR, filename)
                # 检查没有硬编码API密钥
                self.assertNotIn("DASHSCOPE_API_KEY", content,
                                 f"{filename}中包含硬编码API密钥")
                self.assertNotIn("VOLC_ACCESS_KEY", content,
                                 f"{filename}中包含硬编码API密钥")

    def test_no_hardcoded_urls(self):
        for filename in os.listdir(SYNC_DIR):
            if filename.endswith(".kt"):
                content = self._read_file(SYNC_DIR, filename)
                # 不应有硬编码URL
                self.assertNotIn("http://localhost", content,
                                 f"{filename}中包含硬编码URL")
                self.assertNotIn("http://10.0.2.2", content,
                                 f"{filename}中包含硬编码URL")

    def test_sync_manager_uses_log_tag(self):
        content = self._read_file(SYNC_DIR, "SyncManager.kt")
        self.assertIn('TAG', content, "SyncManager缺少LOG TAG")

    def test_sync_manager_error_handling(self):
        content = self._read_file(SYNC_DIR, "SyncManager.kt")
        # 应有try-catch错误处理
        self.assertIn("try {", content, "SyncManager缺少错误处理")
        self.assertIn("catch", content, "SyncManager缺少错误处理")


class TestKotlinTestContent(unittest.TestCase):
    """验证Kotlin测试文件的质量和覆盖率"""

    @classmethod
    def setUpClass(cls):
        path = os.path.join(TEST_DIR, "Phase35SyncManagerTest.kt")
        with open(path, "r", encoding="utf-8") as f:
            cls.content = f.read()

    def test_has_test_annotation(self):
        count = cls_content_count(self.content, "@Test")
        self.assertGreaterEqual(count, 20, f"测试数量不足，当前只有{count}个@Test")

    def test_tests_sync_result(self):
        self.assertIn("SyncResult", self.content, "缺少SyncResult测试")

    def test_tests_sync_status(self):
        self.assertIn("SyncStatus", self.content, "缺少SyncStatus测试")

    def test_tests_sync_config(self):
        self.assertIn("SyncConfig", self.content, "缺少SyncConfig测试")

    def test_tests_network_state(self):
        self.assertIn("NetworkState", self.content, "缺少NetworkState测试")

    def test_tests_conflict_resolver(self):
        self.assertIn("ConflictResolver", self.content, "缺少ConflictResolver测试")

    def test_tests_edge_cases(self):
        # 检查边缘情况测试
        self.assertIn("empty", self.content.lower(), "缺少空列表边缘测试")

    def test_tests_assertions(self):
        assert_count = cls_content_count(self.content, "assert")
        self.assertGreaterEqual(assert_count, 30, f"断言数量不足，当前只有{assert_count}个")


def cls_content_count(content: str, substring: str) -> int:
    """统计子串出现次数"""
    return content.count(substring)


if __name__ == "__main__":
    unittest.main(verbosity=2)
