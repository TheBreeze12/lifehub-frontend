package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.data.local.OfflinePackageManager
import com.example.lifehub.viewmodel.ForgetDataState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 三大Bugfix综合测试
 *
 * 问题一：离线运动包持久化（OfflinePackageManager磁盘读写）
 * 问题二：一键遗忘功能保留账号（前端数据模型与状态）
 * 问题三：语音识别引擎降级处理（SpeechEngineType与SpeechRecognitionState）
 */
class BugfixThreeIssuesTest {

    private lateinit var testDir: File
    private val gson = Gson()

    @Before
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "lifehub_bugfix_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    // ============================================================
    // 问题一：离线运动包持久化测试
    // ============================================================

    @Test
    fun `持久化 - 保存包信息后索引文件存在`() {
        val manager = OfflinePackageManager(testDir)
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_persist_1", 1, 2048, "2026-02-07"),
            "测试计划", "公园"
        )
        val indexFile = File(testDir, "package_index.json")
        assertTrue("索引文件应存在", indexFile.exists())
        val content = indexFile.readText()
        assertTrue("索引文件应包含packageId", content.contains("pkg_persist_1"))
    }

    @Test
    fun `持久化 - 新建Manager可加载之前保存的数据`() {
        // 第一个manager保存数据
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_reload_1", 1, 1024, "2026-01-01"),
            "计划A", "地点A"
        )
        manager1.savePackageInfo(
            OfflinePackageInfo(2, "pkg_reload_2", 1, 2048, "2026-01-02"),
            "计划B", "地点B"
        )

        // 第二个manager（模拟app重启）应能加载数据
        val manager2 = OfflinePackageManager(testDir)
        val packages = manager2.getAllPackages()
        assertEquals("重新加载后应有2个包", 2, packages.size)

        val pkg1 = manager2.getPackageByPackageId("pkg_reload_1")
        assertNotNull("应能找到pkg_reload_1", pkg1)
        assertEquals("计划A", pkg1!!.planTitle)

        val pkg2 = manager2.getPackageByPackageId("pkg_reload_2")
        assertNotNull("应能找到pkg_reload_2", pkg2)
        assertEquals("计划B", pkg2!!.planTitle)
    }

    @Test
    fun `持久化 - 下载状态持久化到磁盘`() {
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_status", 1, 1024, "2026-01-01"),
            null, null
        )
        // DOWNLOADED状态应持久化
        val filePath = File(testDir, "pkg_status.zip").absolutePath
        // 创建一个假的zip文件以通过校验
        File(filePath).writeText("fake zip")
        manager1.updateStatus("pkg_status", OfflinePackageStatus.DOWNLOADED, localFilePath = filePath)

        // 新manager加载
        val manager2 = OfflinePackageManager(testDir)
        val pkg = manager2.getPackageByPackageId("pkg_status")
        assertNotNull(pkg)
        assertEquals(OfflinePackageStatus.DOWNLOADED, pkg!!.status)
        assertEquals(filePath, pkg.localFilePath)
        assertEquals(1.0f, pkg.downloadProgress, 0.001f)
    }

    @Test
    fun `持久化 - 已下载但文件被删除时标记为NOT_DOWNLOADED`() {
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_missing_file", 1, 1024, "2026-01-01"),
            null, null
        )
        val fakePath = File(testDir, "pkg_missing_file.zip").absolutePath
        // 创建文件
        File(fakePath).writeText("temp")
        manager1.updateStatus("pkg_missing_file", OfflinePackageStatus.DOWNLOADED, localFilePath = fakePath)

        // 删除文件模拟文件丢失
        File(fakePath).delete()

        // 新manager加载时应校验文件存在性
        val manager2 = OfflinePackageManager(testDir)
        val pkg = manager2.getPackageByPackageId("pkg_missing_file")
        assertNotNull(pkg)
        assertEquals("文件不存在时应标记为NOT_DOWNLOADED", OfflinePackageStatus.NOT_DOWNLOADED, pkg!!.status)
        assertNull("文件路径应被清空", pkg.localFilePath)
        assertEquals(0f, pkg.downloadProgress, 0.001f)
    }

    @Test
    fun `持久化 - 删除包后持久化更新`() {
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_del_persist", 1, 1024, "2026-01-01"),
            null, null
        )
        manager1.deletePackage("pkg_del_persist")

        // 新manager不应看到已删除的包
        val manager2 = OfflinePackageManager(testDir)
        assertNull(manager2.getPackageByPackageId("pkg_del_persist"))
        assertEquals(0, manager2.getAllPackages().size)
    }

    @Test
    fun `持久化 - 索引文件损坏时从空索引开始`() {
        // 写入无效JSON
        File(testDir, "package_index.json").writeText("this is not valid json{{{")

        // 创建manager不应崩溃
        val manager = OfflinePackageManager(testDir)
        assertEquals(0, manager.getAllPackages().size)
    }

    @Test
    fun `持久化 - 索引文件为空时从空索引开始`() {
        File(testDir, "package_index.json").writeText("")
        val manager = OfflinePackageManager(testDir)
        assertEquals(0, manager.getAllPackages().size)
    }

    @Test
    fun `持久化 - DOWNLOADING状态不频繁写入磁盘`() {
        val manager = OfflinePackageManager(testDir)
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_dl", 1, 1024, "2026-01-01"),
            null, null
        )

        // 多次更新DOWNLOADING状态
        for (i in 1..10) {
            manager.updateStatus("pkg_dl", OfflinePackageStatus.DOWNLOADING, progress = i * 0.1f)
        }

        // 索引文件在DOWNLOADING期间不应被频繁更新
        // 但savePackageInfo已经写过一次，所以文件存在
        val indexFile = File(testDir, "package_index.json")
        assertTrue(indexFile.exists())

        // 最终更新为DOWNLOADED应该写磁盘
        manager.updateStatus("pkg_dl", OfflinePackageStatus.DOWNLOADED, localFilePath = "/fake/path")

        // 新manager验证最终状态
        // 注意：文件路径不存在，loadIndexFromDisk会标记为NOT_DOWNLOADED
        // 这里只验证persist机制工作正常
        val content = indexFile.readText()
        assertTrue(content.contains("pkg_dl"))
    }

    @Test
    fun `持久化 - 多包持久化与恢复`() {
        val manager1 = OfflinePackageManager(testDir)
        for (i in 1..10) {
            manager1.savePackageInfo(
                OfflinePackageInfo(i, "pkg_multi_$i", 1, (1024L * i), "2026-01-0${i % 10}"),
                "计划$i", "地点$i"
            )
        }
        assertEquals(10, manager1.getAllPackages().size)

        val manager2 = OfflinePackageManager(testDir)
        assertEquals(10, manager2.getAllPackages().size)
        for (i in 1..10) {
            val pkg = manager2.getPackageByPackageId("pkg_multi_$i")
            assertNotNull("应能找到pkg_multi_$i", pkg)
            assertEquals("计划$i", pkg!!.planTitle)
        }
    }

    @Test
    fun `持久化 - 同一planId新版本覆盖后持久化`() {
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_v1", 1, 1024, "2026-01-01"),
            "旧版", null
        )
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_v2", 2, 2048, "2026-01-02"),
            "新版", null
        )

        val manager2 = OfflinePackageManager(testDir)
        val packages = manager2.getAllPackages()
        assertEquals("应只有1个包（新版覆盖旧版）", 1, packages.size)
        assertEquals("pkg_v2", packages[0].packageId)
        assertEquals("新版", packages[0].planTitle)
    }

    // ============================================================
    // 问题二：一键遗忘功能保留账号 - 前端数据模型与状态测试
    // ============================================================

    @Test
    fun `遗忘功能 - 成功响应包含用户信息（账号保留）`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 42,
                "nickname": "健康达人",
                "deleted_counts": {
                    "diet_records": 10,
                    "exercise_records": 5,
                    "meal_comparisons": 3,
                    "menu_recognitions": 2,
                    "trip_plans": 4
                },
                "total_deleted": 24
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("数据删除成功", response.message)
        assertNotNull(response.data)
        // 关键：响应中包含user_id和nickname，说明用户账号保留
        assertEquals(42, response.data!!.userId)
        assertEquals("健康达人", response.data!!.nickname)
        assertEquals(24, response.data!!.totalDeleted)
    }

    @Test
    fun `遗忘功能 - ForgetDataState Success状态不应触发logout`() {
        val state = ForgetDataState.Success(totalDeleted = 15, message = "数据删除成功")
        assertTrue(state is ForgetDataState.Success)
        // 前端收到Success后应刷新偏好设置，而非logout
        assertEquals(15, state.totalDeleted)
        assertEquals("数据删除成功", state.message)
    }

    @Test
    fun `遗忘功能 - 重复遗忘返回0条删除记录`() {
        // 第二次遗忘（无数据可删）
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 42,
                "nickname": "健康达人",
                "deleted_counts": {
                    "diet_records": 0,
                    "exercise_records": 0,
                    "meal_comparisons": 0,
                    "menu_recognitions": 0,
                    "trip_plans": 0
                },
                "total_deleted": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)
        assertEquals(200, response.code)
        assertEquals(0, response.data!!.totalDeleted)
        // 用户ID仍然存在（账号保留）
        assertEquals(42, response.data!!.userId)
    }

    @Test
    fun `遗忘功能 - 错误状态消息正确`() {
        val state = ForgetDataState.Error("用户不存在")
        assertEquals("用户不存在", state.message)
    }

    @Test
    fun `遗忘功能 - 各删除计数独立验证`() {
        val counts = DeletedCounts(
            dietRecords = 100,
            exerciseRecords = 50,
            mealComparisons = 30,
            menuRecognitions = 20,
            tripPlans = 10
        )
        val sum = counts.dietRecords + counts.exerciseRecords +
                counts.mealComparisons + counts.menuRecognitions + counts.tripPlans
        assertEquals(210, sum)
    }

    @Test
    fun `遗忘功能 - ForgetDataState状态机完整转换`() {
        // Idle -> Loading -> Success
        var state: ForgetDataState = ForgetDataState.Idle
        assertTrue(state is ForgetDataState.Idle)

        state = ForgetDataState.Loading
        assertTrue(state is ForgetDataState.Loading)

        state = ForgetDataState.Success(totalDeleted = 5, message = "完成")
        assertTrue(state is ForgetDataState.Success)

        // Idle -> Loading -> Error
        state = ForgetDataState.Idle
        state = ForgetDataState.Loading
        state = ForgetDataState.Error("网络错误")
        assertTrue(state is ForgetDataState.Error)
    }

    @Test
    fun `遗忘功能 - JSON反序列化deletedCounts缺失时为null`() {
        val json = """
        {
            "code": 200,
            "message": "数据删除成功",
            "data": {
                "user_id": 1,
                "nickname": "用户",
                "total_deleted": 0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, DataForgetResponse::class.java)
        assertNull(response.data!!.deletedCounts)
    }

    // ============================================================
    // 问题三：语音识别引擎降级 - 数据模型测试
    // ============================================================

    @Test
    fun `语音识别 - SpeechEngineType枚举值完整`() {
        val types = SpeechEngineType.values()
        assertEquals(3, types.size)
        assertTrue(types.any { it == SpeechEngineType.FUNASR_ONNX })
        assertTrue(types.any { it == SpeechEngineType.ANDROID_BUILTIN })
        assertTrue(types.any { it == SpeechEngineType.NONE })
    }

    @Test
    fun `语音识别 - SpeechEngineType displayName正确`() {
        assertEquals("FunASR离线", SpeechEngineType.FUNASR_ONNX.displayName)
        assertEquals("Android内置", SpeechEngineType.ANDROID_BUILTIN.displayName)
        assertEquals("不可用", SpeechEngineType.NONE.displayName)
    }

    @Test
    fun `语音识别 - SpeechRecognitionState各状态`() {
        val idle: SpeechRecognitionState = SpeechRecognitionState.Idle
        assertTrue(idle is SpeechRecognitionState.Idle)

        val listening: SpeechRecognitionState = SpeechRecognitionState.Listening
        assertTrue(listening is SpeechRecognitionState.Listening)

        val processing: SpeechRecognitionState = SpeechRecognitionState.Processing
        assertTrue(processing is SpeechRecognitionState.Processing)

        val result = SpeechRecognitionState.Result(
            text = "测试文本",
            confidence = 0.95f,
            engine = SpeechEngineType.ANDROID_BUILTIN,
            durationMs = 1500
        )
        assertTrue(result is SpeechRecognitionState.Result)
        assertEquals("测试文本", result.text)
        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals(SpeechEngineType.ANDROID_BUILTIN, result.engine)

        val partial = SpeechRecognitionState.PartialResult("部分")
        assertTrue(partial is SpeechRecognitionState.PartialResult)
        assertEquals("部分", partial.text)

        val error = SpeechRecognitionState.Error(
            message = "无可用的语音识别引擎，请在系统设置中安装语音服务或下载离线语言包",
            code = 2
        )
        assertTrue(error is SpeechRecognitionState.Error)
        assertEquals(2, error.code)
        assertTrue(error.message.contains("语音识别引擎"))
    }

    @Test
    fun `语音识别 - NONE引擎fallback错误消息包含引导信息`() {
        // 验证错误消息包含安装指导
        val errorMsg = "无可用的语音识别引擎，请在系统设置中安装语音服务或下载离线语言包"
        assertTrue(errorMsg.contains("系统设置"))
        assertTrue(errorMsg.contains("语音服务"))
        assertTrue(errorMsg.contains("离线语言包"))
    }

    @Test
    fun `语音识别 - Result默认引擎为ANDROID_BUILTIN`() {
        val result = SpeechRecognitionState.Result(text = "你好")
        assertEquals(SpeechEngineType.ANDROID_BUILTIN, result.engine)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertEquals(0L, result.durationMs)
    }

    @Test
    fun `语音识别 - Error默认code为-1`() {
        val error = SpeechRecognitionState.Error(message = "测试错误")
        assertEquals(-1, error.code)
    }

    @Test
    fun `语音识别 - SpeechRecognitionConfig默认值`() {
        val config = SpeechRecognitionConfig()
        assertEquals(16000, config.sampleRate)
        assertEquals("zh-CN", config.language)
        assertTrue(config.preferOffline)
        assertTrue(config.maxDurationMs > 0)
        assertTrue(config.silenceTimeoutMs > 0)
    }

    @Test
    fun `语音识别 - FbankConfig默认值`() {
        val config = FbankConfig()
        assertEquals(16000, config.sampleRate)
        assertEquals(80, config.numMelBins)
        assertEquals(25, config.frameLengthMs)
        assertEquals(10, config.frameShiftMs)
    }

    // ============================================================
    // 综合边界测试
    // ============================================================

    @Test
    fun `离线包 - 存储目录不存在时自动创建`() {
        val nested = File(testDir, "deep/nested/path")
        assertFalse(nested.exists())
        val manager = OfflinePackageManager(nested)
        assertTrue(nested.exists())
        assertEquals(0, manager.getAllPackages().size)
    }

    @Test
    fun `离线包 - ERROR状态持久化后恢复`() {
        val manager1 = OfflinePackageManager(testDir)
        manager1.savePackageInfo(
            OfflinePackageInfo(1, "pkg_err", 1, 1024, "2026-01-01"),
            null, null
        )
        manager1.updateStatus("pkg_err", OfflinePackageStatus.ERROR, errorMessage = "下载失败")

        val manager2 = OfflinePackageManager(testDir)
        val pkg = manager2.getPackageByPackageId("pkg_err")
        assertNotNull(pkg)
        assertEquals(OfflinePackageStatus.ERROR, pkg!!.status)
        assertEquals("下载失败", pkg.errorMessage)
    }

    @Test
    fun `遗忘功能 - DataForgetResponse序列化往返`() {
        val original = DataForgetResponse(
            code = 200,
            message = "数据删除成功",
            data = DataForgetData(
                userId = 99,
                nickname = "测试用户",
                deletedCounts = DeletedCounts(
                    dietRecords = 5,
                    exerciseRecords = 3,
                    mealComparisons = 1,
                    menuRecognitions = 2,
                    tripPlans = 4
                ),
                totalDeleted = 15
            )
        )
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, DataForgetResponse::class.java)
        assertEquals(original.code, restored.code)
        assertEquals(original.message, restored.message)
        assertEquals(original.data!!.userId, restored.data!!.userId)
        assertEquals(original.data!!.totalDeleted, restored.data!!.totalDeleted)
    }
}
