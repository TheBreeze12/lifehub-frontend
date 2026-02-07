package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.data.local.OfflinePackageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Phase 47: 离线运动包下载管理 - 单元测试
 *
 * 测试覆盖：
 * 1. 数据模型正确性（TileBounds, OfflinePackageInfo, LocalOfflinePackage等）
 * 2. OfflinePackageManager核心逻辑（解析ZIP、管理本地包、状态追踪）
 * 3. 边界条件（空数据、无效输入、文件不存在等）
 * 4. 文件大小格式化
 * 5. 离线包内容解析
 */
class Phase47OfflinePackageTest {

    private lateinit var testDir: File
    private lateinit var manager: OfflinePackageManager

    @Before
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "lifehub_test_offline_${System.currentTimeMillis()}")
        testDir.mkdirs()
        manager = OfflinePackageManager(testDir)
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    // ==================== 数据模型测试 ====================

    @Test
    fun `TileBounds - centerLat计算正确`() {
        val bounds = TileBounds(minLat = 39.0, maxLat = 40.0, minLng = 116.0, maxLng = 117.0)
        assertEquals(39.5, bounds.centerLat(), 0.001)
    }

    @Test
    fun `TileBounds - centerLng计算正确`() {
        val bounds = TileBounds(minLat = 39.0, maxLat = 40.0, minLng = 116.0, maxLng = 117.0)
        assertEquals(116.5, bounds.centerLng(), 0.001)
    }

    @Test
    fun `TileBounds - contains判断坐标在边界内`() {
        val bounds = TileBounds(minLat = 39.0, maxLat = 40.0, minLng = 116.0, maxLng = 117.0)
        assertTrue(bounds.contains(39.5, 116.5))
        assertTrue(bounds.contains(39.0, 116.0)) // 边界上
        assertTrue(bounds.contains(40.0, 117.0)) // 边界上
    }

    @Test
    fun `TileBounds - contains判断坐标在边界外`() {
        val bounds = TileBounds(minLat = 39.0, maxLat = 40.0, minLng = 116.0, maxLng = 117.0)
        assertFalse(bounds.contains(38.9, 116.5)) // 纬度过小
        assertFalse(bounds.contains(40.1, 116.5)) // 纬度过大
        assertFalse(bounds.contains(39.5, 115.9)) // 经度过小
        assertFalse(bounds.contains(39.5, 117.1)) // 经度过大
    }

    @Test
    fun `TileBounds - isValid判断非零边界`() {
        val validBounds = TileBounds(minLat = 39.0, maxLat = 40.0, minLng = 116.0, maxLng = 117.0)
        assertTrue(validBounds.isValid())

        val zeroBounds = TileBounds(minLat = 0.0, maxLat = 0.0, minLng = 0.0, maxLng = 0.0)
        assertFalse(zeroBounds.isValid())
    }

    @Test
    fun `TileBounds - 负坐标边界有效`() {
        val bounds = TileBounds(minLat = -34.0, maxLat = -33.0, minLng = -58.5, maxLng = -58.0)
        assertTrue(bounds.isValid())
        assertTrue(bounds.contains(-33.5, -58.3))
    }

    @Test
    fun `OfflinePackageInfo - 基本属性`() {
        val info = OfflinePackageInfo(
            planId = 1,
            packageId = "pkg_1_1_abc12345",
            version = 1,
            fileSize = 2048,
            createdAt = "2026-02-07T15:00:00",
            tileBounds = TileBounds(39.0, 40.0, 116.0, 117.0)
        )
        assertEquals(1, info.planId)
        assertEquals("pkg_1_1_abc12345", info.packageId)
        assertEquals(1, info.version)
        assertEquals(2048L, info.fileSize)
        assertEquals("2026-02-07T15:00:00", info.createdAt)
        assertNotNull(info.tileBounds)
    }

    @Test
    fun `OfflinePackageInfo - tileBounds为null`() {
        val info = OfflinePackageInfo(
            planId = 2,
            packageId = "pkg_2_1_xyz",
            version = 1,
            fileSize = 1024,
            createdAt = "2026-02-07T16:00:00"
        )
        assertNull(info.tileBounds)
    }

    @Test
    fun `OfflinePackageResponse - 成功响应`() {
        val response = OfflinePackageResponse(
            code = 200,
            message = "离线包生成成功",
            data = OfflinePackageInfo(
                planId = 1,
                packageId = "pkg_1_1_abc",
                version = 1,
                fileSize = 512,
                createdAt = "2026-02-07T15:00:00"
            )
        )
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals("pkg_1_1_abc", response.data?.packageId)
    }

    @Test
    fun `OfflinePackageResponse - 错误响应`() {
        val response = OfflinePackageResponse(
            code = 404,
            message = "运动计划不存在",
            data = null
        )
        assertEquals(404, response.code)
        assertNull(response.data)
    }

    @Test
    fun `OfflinePackageRequest - 创建请求`() {
        val request = OfflinePackageRequest(planId = 42)
        assertEquals(42, request.planId)
    }

    // ==================== LocalOfflinePackage 测试 ====================

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted bytes`() {
        val pkg = createTestLocalPackage(fileSize = 500)
        assertEquals("500B", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted KB`() {
        val pkg = createTestLocalPackage(fileSize = 2048)
        assertEquals("2KB", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted MB`() {
        val pkg = createTestLocalPackage(fileSize = 1572864) // 1.5MB
        assertEquals("1.5MB", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted 0 bytes`() {
        val pkg = createTestLocalPackage(fileSize = 0)
        assertEquals("0B", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted 1023 bytes`() {
        val pkg = createTestLocalPackage(fileSize = 1023)
        assertEquals("1023B", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - fileSizeFormatted 1024 bytes equals 1KB`() {
        val pkg = createTestLocalPackage(fileSize = 1024)
        assertEquals("1KB", pkg.fileSizeFormatted())
    }

    @Test
    fun `LocalOfflinePackage - isAvailableOffline 已下载且有文件路径`() {
        val pkg = createTestLocalPackage(
            status = OfflinePackageStatus.DOWNLOADED,
            localFilePath = "/path/to/file.zip"
        )
        assertTrue(pkg.isAvailableOffline())
    }

    @Test
    fun `LocalOfflinePackage - isAvailableOffline 已下载但无文件路径`() {
        val pkg = createTestLocalPackage(
            status = OfflinePackageStatus.DOWNLOADED,
            localFilePath = null
        )
        assertFalse(pkg.isAvailableOffline())
    }

    @Test
    fun `LocalOfflinePackage - isAvailableOffline 正在下载`() {
        val pkg = createTestLocalPackage(status = OfflinePackageStatus.DOWNLOADING)
        assertFalse(pkg.isAvailableOffline())
    }

    @Test
    fun `LocalOfflinePackage - isAvailableOffline 未下载`() {
        val pkg = createTestLocalPackage(status = OfflinePackageStatus.NOT_DOWNLOADED)
        assertFalse(pkg.isAvailableOffline())
    }

    @Test
    fun `LocalOfflinePackage - isAvailableOffline 错误状态`() {
        val pkg = createTestLocalPackage(status = OfflinePackageStatus.ERROR)
        assertFalse(pkg.isAvailableOffline())
    }

    @Test
    fun `LocalOfflinePackage - 默认值`() {
        val pkg = LocalOfflinePackage(
            planId = 1,
            packageId = "test",
            version = 1,
            fileSize = 100,
            createdAt = "2026-01-01"
        )
        assertEquals(OfflinePackageStatus.NOT_DOWNLOADED, pkg.status)
        assertNull(pkg.localFilePath)
        assertEquals(0f, pkg.downloadProgress, 0.001f)
        assertNull(pkg.errorMessage)
        assertNull(pkg.planTitle)
        assertNull(pkg.planDestination)
    }

    // ==================== OfflinePackageStatus 枚举测试 ====================

    @Test
    fun `OfflinePackageStatus - 所有状态值`() {
        val statuses = OfflinePackageStatus.values()
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(OfflinePackageStatus.NOT_DOWNLOADED))
        assertTrue(statuses.contains(OfflinePackageStatus.GENERATING))
        assertTrue(statuses.contains(OfflinePackageStatus.DOWNLOADING))
        assertTrue(statuses.contains(OfflinePackageStatus.DOWNLOADED))
        assertTrue(statuses.contains(OfflinePackageStatus.ERROR))
    }

    // ==================== OfflinePlanData 测试 ====================

    @Test
    fun `OfflinePlanData - 默认值`() {
        val plan = OfflinePlanData()
        assertEquals("", plan.title)
        assertEquals("", plan.destination)
        assertEquals("", plan.date)
        assertEquals(0, plan.totalDuration)
        assertEquals(0.0, plan.totalCalories, 0.001)
        assertEquals(0, plan.itemCount)
        assertTrue(plan.items.isEmpty())
    }

    @Test
    fun `OfflinePlanData - 完整数据`() {
        val items = listOf(
            OfflinePlanItem(1, "19:00", "公园", "walking", 30, 150.0, "散步"),
            OfflinePlanItem(1, "19:30", "跑道", "running", 20, 200.0, "慢跑")
        )
        val plan = OfflinePlanData(
            title = "餐后运动计划",
            destination = "奥林匹克公园",
            date = "2026-02-07",
            totalDuration = 50,
            totalCalories = 350.0,
            itemCount = 2,
            items = items
        )
        assertEquals("餐后运动计划", plan.title)
        assertEquals(2, plan.items.size)
        assertEquals(350.0, plan.totalCalories, 0.001)
    }

    @Test
    fun `OfflinePlanItem - 默认值`() {
        val item = OfflinePlanItem()
        assertEquals(1, item.dayIndex)
        assertNull(item.startTime)
        assertEquals("", item.placeName)
        assertEquals("walking", item.placeType)
        assertEquals(0, item.duration)
        assertEquals(0.0, item.calories, 0.001)
        assertEquals("", item.notes)
    }

    // ==================== OfflinePoiData 测试 ====================

    @Test
    fun `OfflinePoiData - 基本属性`() {
        val poi = OfflinePoiData(
            name = "奥林匹克公园",
            type = "park",
            latitude = 39.99,
            longitude = 116.39,
            duration = 30,
            notes = "适合散步"
        )
        assertEquals("奥林匹克公园", poi.name)
        assertEquals("park", poi.type)
        assertEquals(39.99, poi.latitude!!, 0.001)
        assertEquals(116.39, poi.longitude!!, 0.001)
    }

    @Test
    fun `OfflinePoiData - 无坐标`() {
        val poi = OfflinePoiData(name = "某地点")
        assertNull(poi.latitude)
        assertNull(poi.longitude)
    }

    // ==================== OfflineTilesMetadata 测试 ====================

    @Test
    fun `OfflineTilesMetadata - 默认值`() {
        val meta = OfflineTilesMetadata()
        assertNull(meta.bounds)
        assertTrue(meta.zoomLevels.isEmpty())
        assertEquals(0, meta.tileCountEstimate)
        assertNull(meta.center)
    }

    @Test
    fun `OfflineTilesMetadata - 完整数据`() {
        val meta = OfflineTilesMetadata(
            bounds = TileBounds(39.0, 40.0, 116.0, 117.0),
            zoomLevels = listOf(14, 15, 16),
            tileCountEstimate = 48,
            center = OfflineCenter(39.5, 116.5)
        )
        assertNotNull(meta.bounds)
        assertEquals(3, meta.zoomLevels.size)
        assertEquals(48, meta.tileCountEstimate)
        assertEquals(39.5, meta.center!!.latitude, 0.001)
    }

    // ==================== OfflinePackageManager 测试 ====================

    @Test
    fun `Manager - 初始状态无包`() {
        val packages = manager.getAllPackages()
        assertTrue(packages.isEmpty())
    }

    @Test
    fun `Manager - 保存包信息`() {
        val info = OfflinePackageInfo(
            planId = 1,
            packageId = "pkg_1_1_abc",
            version = 1,
            fileSize = 2048,
            createdAt = "2026-02-07T15:00:00",
            tileBounds = TileBounds(39.0, 40.0, 116.0, 117.0)
        )
        manager.savePackageInfo(info, "餐后运动计划", "公园")
        val packages = manager.getAllPackages()
        assertEquals(1, packages.size)
        assertEquals("pkg_1_1_abc", packages[0].packageId)
        assertEquals("餐后运动计划", packages[0].planTitle)
        assertEquals("公园", packages[0].planDestination)
    }

    @Test
    fun `Manager - 通过planId查找包`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_1_1_abc", 1, 1024, "2026-01-01"),
            "计划A", null
        )
        manager.savePackageInfo(
            OfflinePackageInfo(2, "pkg_2_1_xyz", 1, 2048, "2026-01-02"),
            "计划B", null
        )

        val pkg = manager.getPackageByPlanId(1)
        assertNotNull(pkg)
        assertEquals("pkg_1_1_abc", pkg!!.packageId)

        val pkg2 = manager.getPackageByPlanId(2)
        assertNotNull(pkg2)
        assertEquals("pkg_2_1_xyz", pkg2!!.packageId)
    }

    @Test
    fun `Manager - 查找不存在的planId返回null`() {
        val pkg = manager.getPackageByPlanId(999)
        assertNull(pkg)
    }

    @Test
    fun `Manager - 通过packageId查找包`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_1_1_abc", 1, 1024, "2026-01-01"),
            null, null
        )
        val pkg = manager.getPackageByPackageId("pkg_1_1_abc")
        assertNotNull(pkg)
        assertEquals(1, pkg!!.planId)
    }

    @Test
    fun `Manager - 查找不存在的packageId返回null`() {
        val pkg = manager.getPackageByPackageId("nonexistent")
        assertNull(pkg)
    }

    @Test
    fun `Manager - 更新下载状态为DOWNLOADING`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_test", 1, 1024, "2026-01-01"),
            null, null
        )
        manager.updateStatus("pkg_test", OfflinePackageStatus.DOWNLOADING, progress = 0.5f)
        val pkg = manager.getPackageByPackageId("pkg_test")
        assertEquals(OfflinePackageStatus.DOWNLOADING, pkg!!.status)
        assertEquals(0.5f, pkg.downloadProgress, 0.001f)
    }

    @Test
    fun `Manager - 更新下载状态为DOWNLOADED并设置文件路径`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_test", 1, 1024, "2026-01-01"),
            null, null
        )
        val filePath = File(testDir, "pkg_test.zip").absolutePath
        manager.updateStatus("pkg_test", OfflinePackageStatus.DOWNLOADED, localFilePath = filePath)
        val pkg = manager.getPackageByPackageId("pkg_test")
        assertEquals(OfflinePackageStatus.DOWNLOADED, pkg!!.status)
        assertEquals(filePath, pkg.localFilePath)
        assertEquals(1.0f, pkg.downloadProgress, 0.001f)
    }

    @Test
    fun `Manager - 更新下载状态为ERROR并设置错误信息`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_test", 1, 1024, "2026-01-01"),
            null, null
        )
        manager.updateStatus("pkg_test", OfflinePackageStatus.ERROR, errorMessage = "网络超时")
        val pkg = manager.getPackageByPackageId("pkg_test")
        assertEquals(OfflinePackageStatus.ERROR, pkg!!.status)
        assertEquals("网络超时", pkg.errorMessage)
    }

    @Test
    fun `Manager - 更新不存在的packageId不会崩溃`() {
        // 应静默忽略
        manager.updateStatus("nonexistent", OfflinePackageStatus.DOWNLOADING)
        val packages = manager.getAllPackages()
        assertTrue(packages.isEmpty())
    }

    @Test
    fun `Manager - 删除包`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_del", 1, 1024, "2026-01-01"),
            null, null
        )
        assertEquals(1, manager.getAllPackages().size)
        val deleted = manager.deletePackage("pkg_del")
        assertTrue(deleted)
        assertEquals(0, manager.getAllPackages().size)
    }

    @Test
    fun `Manager - 删除包同时删除本地文件`() {
        val localFile = File(testDir, "pkg_local.zip")
        localFile.writeText("test content")
        assertTrue(localFile.exists())

        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_local", 1, 1024, "2026-01-01"),
            null, null
        )
        manager.updateStatus("pkg_local", OfflinePackageStatus.DOWNLOADED, localFilePath = localFile.absolutePath)
        manager.deletePackage("pkg_local")
        assertFalse(localFile.exists())
    }

    @Test
    fun `Manager - 删除不存在的包返回false`() {
        val result = manager.deletePackage("nonexistent")
        assertFalse(result)
    }

    @Test
    fun `Manager - 同一planId更新版本`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_1_v1", 1, 1024, "2026-01-01"),
            "计划A", null
        )
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_1_v2", 2, 2048, "2026-01-02"),
            "计划A", null
        )
        // 应保留最新版本，旧版本被替换
        val pkg = manager.getPackageByPlanId(1)
        assertNotNull(pkg)
        assertEquals("pkg_1_v2", pkg!!.packageId)
        assertEquals(2, pkg.version)
    }

    @Test
    fun `Manager - 多个不同planId的包`() {
        for (i in 1..5) {
            manager.savePackageInfo(
                OfflinePackageInfo(i, "pkg_${i}_1", 1, (1024 * i).toLong(), "2026-01-0$i"),
                "计划$i", "地点$i"
            )
        }
        val packages = manager.getAllPackages()
        assertEquals(5, packages.size)
    }

    // ==================== ZIP解析测试 ====================

    @Test
    fun `Manager - 解析有效的离线包ZIP文件`() {
        val zipFile = createTestZipFile(
            planJson = """{"title":"测试计划","destination":"公园","date":"2026-02-07","total_duration":30,"total_calories":150.0,"item_count":1,"items":[{"day_index":1,"start_time":"19:00","place_name":"散步道","place_type":"walking","duration":30,"calories":150.0,"notes":"餐后散步"}]}""",
            poisJson = """[{"name":"公园入口","type":"park","latitude":39.99,"longitude":116.39,"duration":5,"notes":"集合点"}]""",
            tilesMetaJson = """{"bounds":{"min_lat":39.9,"max_lat":40.0,"min_lng":116.3,"max_lng":116.4},"zoom_levels":[14,15,16],"tile_count_estimate":48,"center":{"latitude":39.95,"longitude":116.35}}"""
        )

        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents)

        // 验证plan数据
        assertNotNull(contents!!.plan)
        assertEquals("测试计划", contents.plan!!.title)
        assertEquals("公园", contents.plan!!.destination)
        assertEquals(30, contents.plan!!.totalDuration)
        assertEquals(150.0, contents.plan!!.totalCalories, 0.001)
        assertEquals(1, contents.plan!!.items.size)
        assertEquals("散步道", contents.plan!!.items[0].placeName)

        // 验证POI数据
        assertEquals(1, contents.pois.size)
        assertEquals("公园入口", contents.pois[0].name)
        assertEquals(39.99, contents.pois[0].latitude!!, 0.001)

        // 验证瓦片元数据
        assertNotNull(contents.tilesMetadata)
        assertNotNull(contents.tilesMetadata!!.bounds)
        assertEquals(3, contents.tilesMetadata!!.zoomLevels.size)
        assertEquals(48, contents.tilesMetadata!!.tileCountEstimate)
    }

    @Test
    fun `Manager - 解析空ZIP文件返回空内容`() {
        val zipFile = File(testDir, "empty.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { it.finish() }
        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents)
        assertNull(contents!!.plan)
        assertTrue(contents.pois.isEmpty())
        assertNull(contents.tilesMetadata)
    }

    @Test
    fun `Manager - 解析不存在的文件返回null`() {
        val contents = manager.parsePackageContents("/nonexistent/file.zip")
        assertNull(contents)
    }

    @Test
    fun `Manager - 解析非ZIP文件返回null`() {
        val notZip = File(testDir, "notazip.txt")
        notZip.writeText("this is not a zip file")
        val contents = manager.parsePackageContents(notZip.absolutePath)
        assertNull(contents)
    }

    @Test
    fun `Manager - 解析只含plan的ZIP`() {
        val zipFile = createTestZipFile(
            planJson = """{"title":"仅计划","destination":"","date":"","total_duration":0,"total_calories":0,"item_count":0,"items":[]}"""
        )
        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents)
        assertNotNull(contents!!.plan)
        assertEquals("仅计划", contents.plan!!.title)
        assertTrue(contents.pois.isEmpty())
        assertNull(contents.tilesMetadata)
    }

    @Test
    fun `Manager - 解析含多个POI的ZIP`() {
        val poisJson = """[
            {"name":"POI1","type":"park","latitude":39.1,"longitude":116.1,"duration":10,"notes":""},
            {"name":"POI2","type":"gym","latitude":39.2,"longitude":116.2,"duration":20,"notes":""},
            {"name":"POI3","type":"walking","latitude":39.3,"longitude":116.3,"duration":30,"notes":""}
        ]"""
        val zipFile = createTestZipFile(poisJson = poisJson)
        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents)
        assertEquals(3, contents!!.pois.size)
        assertEquals("POI1", contents.pois[0].name)
        assertEquals("POI2", contents.pois[1].name)
        assertEquals("POI3", contents.pois[2].name)
    }

    @Test
    fun `Manager - 解析含空items的plan`() {
        val zipFile = createTestZipFile(
            planJson = """{"title":"空计划","destination":"","date":"","total_duration":0,"total_calories":0,"item_count":0,"items":[]}"""
        )
        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents!!.plan)
        assertTrue(contents.plan!!.items.isEmpty())
        assertEquals(0, contents.plan!!.itemCount)
    }

    // ==================== 存储路径测试 ====================

    @Test
    fun `Manager - 存储目录自动创建`() {
        val subDir = File(testDir, "sub/nested/dir")
        assertFalse(subDir.exists())
        val m = OfflinePackageManager(subDir)
        assertTrue(subDir.exists())
    }

    @Test
    fun `Manager - 获取包存储路径`() {
        val path = manager.getPackageFilePath("pkg_test_123")
        assertTrue(path.endsWith("pkg_test_123.zip"))
        assertTrue(path.startsWith(testDir.absolutePath))
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `TileBounds - 极小区域`() {
        val bounds = TileBounds(39.999999, 40.000001, 116.999999, 117.000001)
        assertTrue(bounds.isValid())
        assertEquals(40.0, bounds.centerLat(), 0.001)
        assertEquals(117.0, bounds.centerLng(), 0.001)
    }

    @Test
    fun `TileBounds - 跨越日期变更线`() {
        val bounds = TileBounds(minLat = -10.0, maxLat = 10.0, minLng = 170.0, maxLng = -170.0)
        assertTrue(bounds.isValid())
        // 注意：跨日期变更线时minLng > maxLng，contains逻辑可能不适用
        // 这里只测试isValid
    }

    @Test
    fun `LocalOfflinePackage - 大文件大小格式化`() {
        val pkg = createTestLocalPackage(fileSize = 1073741824) // 1GB
        assertEquals("1024.0MB", pkg.fileSizeFormatted())
    }

    @Test
    fun `Manager - 保存包含特殊字符的planTitle`() {
        manager.savePackageInfo(
            OfflinePackageInfo(1, "pkg_special", 1, 1024, "2026-01-01"),
            "计划 (第一版) - 测试/更新", "北京·朝阳区"
        )
        val pkg = manager.getPackageByPackageId("pkg_special")
        assertNotNull(pkg)
        assertEquals("计划 (第一版) - 测试/更新", pkg!!.planTitle)
        assertEquals("北京·朝阳区", pkg.planDestination)
    }

    @Test
    fun `Manager - 解析含中文的plan JSON`() {
        val zipFile = createTestZipFile(
            planJson = """{"title":"中文运动计划","destination":"北京奥林匹克公园","date":"2026-02-07","total_duration":60,"total_calories":300.5,"item_count":2,"items":[{"day_index":1,"start_time":"18:30","place_name":"健身步道","place_type":"walking","duration":30,"calories":150.0,"notes":"建议慢走热身"},{"day_index":1,"start_time":"19:00","place_name":"跑道","place_type":"running","duration":30,"calories":150.5,"notes":"中等配速"}]}"""
        )
        val contents = manager.parsePackageContents(zipFile.absolutePath)
        assertNotNull(contents)
        assertEquals("中文运动计划", contents!!.plan!!.title)
        assertEquals("北京奥林匹克公园", contents.plan!!.destination)
        assertEquals(2, contents.plan!!.items.size)
        assertEquals("建议慢走热身", contents.plan!!.items[0].notes)
    }

    @Test
    fun `Manager - 保存后再保存同一packageId会更新`() {
        val info = OfflinePackageInfo(1, "pkg_same", 1, 1024, "2026-01-01")
        manager.savePackageInfo(info, "旧标题", "旧地点")
        manager.savePackageInfo(info, "新标题", "新地点")
        val packages = manager.getAllPackages()
        // 同一packageId不应重复
        val matches = packages.filter { it.packageId == "pkg_same" }
        assertEquals(1, matches.size)
        assertEquals("新标题", matches[0].planTitle)
    }

    @Test
    fun `OfflinePackageContents - 默认值`() {
        val contents = OfflinePackageContents()
        assertNull(contents.plan)
        assertTrue(contents.pois.isEmpty())
        assertNull(contents.tilesMetadata)
    }

    @Test
    fun `OfflineCenter - 默认值`() {
        val center = OfflineCenter()
        assertEquals(0.0, center.latitude, 0.001)
        assertEquals(0.0, center.longitude, 0.001)
    }

    // ==================== 辅助方法 ====================

    private fun createTestLocalPackage(
        planId: Int = 1,
        packageId: String = "pkg_test",
        version: Int = 1,
        fileSize: Long = 1024,
        status: OfflinePackageStatus = OfflinePackageStatus.NOT_DOWNLOADED,
        localFilePath: String? = null
    ): LocalOfflinePackage {
        return LocalOfflinePackage(
            planId = planId,
            packageId = packageId,
            version = version,
            fileSize = fileSize,
            createdAt = "2026-01-01T00:00:00",
            status = status,
            localFilePath = localFilePath
        )
    }

    private fun createTestZipFile(
        planJson: String? = null,
        poisJson: String? = null,
        tilesMetaJson: String? = null
    ): File {
        val zipFile = File(testDir, "test_${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val metadataJson = """{"package_id":"test","plan_id":1,"version":1,"created_at":"2026-01-01","format_version":"1.0","contents":["plan.json","pois.json","tiles_meta.json"]}"""
            zos.putNextEntry(ZipEntry("metadata.json"))
            zos.write(metadataJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            if (planJson != null) {
                zos.putNextEntry(ZipEntry("plan.json"))
                zos.write(planJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            if (poisJson != null) {
                zos.putNextEntry(ZipEntry("pois.json"))
                zos.write(poisJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            if (tilesMetaJson != null) {
                zos.putNextEntry(ZipEntry("tiles_meta.json"))
                zos.write(tilesMetaJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
