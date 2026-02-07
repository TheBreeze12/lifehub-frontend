package com.example.lifehub.data.local

import com.example.lifehub.data.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.zip.ZipFile

/**
 * 离线运动包管理器（Phase 47）
 *
 * 职责：
 * 1. 管理本地离线包记录（增删改查）
 * 2. 解析离线包ZIP内容（plan.json, pois.json, tiles_meta.json）
 * 3. 跟踪下载状态与进度
 * 4. 管理本地文件存储
 */
class OfflinePackageManager(private val storageDir: File) {

    private val gson = Gson()

    // 内存中维护的包索引（packageId -> LocalOfflinePackage）
    private val packageIndex = mutableMapOf<String, LocalOfflinePackage>()

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    // ----------------------------------------------------------
    // 1. 包信息管理
    // ----------------------------------------------------------

    /**
     * 保存离线包信息（从服务端响应转换为本地记录）
     * 如果同一planId已有旧版本，则替换
     * 如果同一packageId已存在，则更新
     */
    fun savePackageInfo(
        info: OfflinePackageInfo,
        planTitle: String?,
        planDestination: String?
    ) {
        // 同一planId的旧版本移除
        val oldKeys = packageIndex.entries
            .filter { it.value.planId == info.planId && it.key != info.packageId }
            .map { it.key }
        oldKeys.forEach { packageIndex.remove(it) }

        // 保存或更新
        val existing = packageIndex[info.packageId]
        packageIndex[info.packageId] = LocalOfflinePackage(
            planId = info.planId,
            packageId = info.packageId,
            version = info.version,
            fileSize = info.fileSize,
            createdAt = info.createdAt,
            tileBounds = info.tileBounds,
            status = existing?.status ?: OfflinePackageStatus.NOT_DOWNLOADED,
            localFilePath = existing?.localFilePath,
            downloadProgress = existing?.downloadProgress ?: 0f,
            errorMessage = existing?.errorMessage,
            planTitle = planTitle,
            planDestination = planDestination
        )
    }

    /** 获取所有本地离线包 */
    fun getAllPackages(): List<LocalOfflinePackage> {
        return packageIndex.values.toList()
    }

    /** 通过planId查找离线包 */
    fun getPackageByPlanId(planId: Int): LocalOfflinePackage? {
        return packageIndex.values.firstOrNull { it.planId == planId }
    }

    /** 通过packageId查找离线包 */
    fun getPackageByPackageId(packageId: String): LocalOfflinePackage? {
        return packageIndex[packageId]
    }

    // ----------------------------------------------------------
    // 2. 下载状态管理
    // ----------------------------------------------------------

    /**
     * 更新离线包下载状态
     *
     * @param packageId 包唯一标识
     * @param status 新状态
     * @param progress 下载进度（0.0-1.0），仅DOWNLOADING时使用
     * @param localFilePath 本地文件路径，DOWNLOADED时设置
     * @param errorMessage 错误信息，ERROR时设置
     */
    fun updateStatus(
        packageId: String,
        status: OfflinePackageStatus,
        progress: Float = 0f,
        localFilePath: String? = null,
        errorMessage: String? = null
    ) {
        val existing = packageIndex[packageId] ?: return

        packageIndex[packageId] = existing.copy(
            status = status,
            downloadProgress = when (status) {
                OfflinePackageStatus.DOWNLOADED -> 1.0f
                OfflinePackageStatus.DOWNLOADING -> progress
                OfflinePackageStatus.ERROR -> existing.downloadProgress
                else -> 0f
            },
            localFilePath = localFilePath ?: existing.localFilePath,
            errorMessage = when (status) {
                OfflinePackageStatus.ERROR -> errorMessage
                else -> null
            }
        )
    }

    // ----------------------------------------------------------
    // 3. 包删除
    // ----------------------------------------------------------

    /**
     * 删除离线包（同时删除本地文件）
     * @return 是否成功删除
     */
    fun deletePackage(packageId: String): Boolean {
        val pkg = packageIndex[packageId] ?: return false

        // 删除本地文件
        pkg.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        packageIndex.remove(packageId)
        return true
    }

    // ----------------------------------------------------------
    // 4. 文件路径管理
    // ----------------------------------------------------------

    /** 获取离线包本地存储路径 */
    fun getPackageFilePath(packageId: String): String {
        return File(storageDir, "$packageId.zip").absolutePath
    }

    // ----------------------------------------------------------
    // 5. ZIP内容解析
    // ----------------------------------------------------------

    /**
     * 解析离线包ZIP文件内容
     *
     * ZIP结构：
     * - metadata.json: 包元数据
     * - plan.json: 运动方案
     * - pois.json: POI数据列表
     * - tiles_meta.json: 瓦片元数据
     *
     * @param filePath ZIP文件路径
     * @return 解析后的内容，文件不存在或格式错误返回null
     */
    fun parsePackageContents(filePath: String): OfflinePackageContents? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val zipFile = ZipFile(file)
            zipFile.use { zf ->
                val planData = readZipEntry(zf, "plan.json")?.let { parsePlanJson(it) }
                val poisData = readZipEntry(zf, "pois.json")?.let { parsePoisJson(it) } ?: emptyList()
                val tilesData = readZipEntry(zf, "tiles_meta.json")?.let { parseTilesMetaJson(it) }

                OfflinePackageContents(
                    plan = planData,
                    pois = poisData,
                    tilesMetadata = tilesData
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 读取ZIP中指定条目的文本内容 */
    private fun readZipEntry(zipFile: ZipFile, entryName: String): String? {
        val entry = zipFile.getEntry(entryName) ?: return null
        return zipFile.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** 解析plan.json -> OfflinePlanData */
    private fun parsePlanJson(json: String): OfflinePlanData? {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            val itemsArray = obj.getAsJsonArray("items") ?: com.google.gson.JsonArray()
            val items = itemsArray.map { itemEl ->
                val itemObj = itemEl.asJsonObject
                OfflinePlanItem(
                    dayIndex = itemObj.get("day_index")?.asInt ?: 1,
                    startTime = itemObj.get("start_time")?.let { if (it.isJsonNull) null else it.asString },
                    placeName = itemObj.get("place_name")?.asString ?: "",
                    placeType = itemObj.get("place_type")?.asString ?: "walking",
                    duration = itemObj.get("duration")?.asInt ?: 0,
                    calories = itemObj.get("calories")?.asDouble ?: 0.0,
                    notes = itemObj.get("notes")?.asString ?: ""
                )
            }

            OfflinePlanData(
                title = obj.get("title")?.asString ?: "",
                destination = obj.get("destination")?.asString ?: "",
                date = obj.get("date")?.asString ?: "",
                totalDuration = obj.get("total_duration")?.asInt ?: 0,
                totalCalories = obj.get("total_calories")?.asDouble ?: 0.0,
                itemCount = obj.get("item_count")?.asInt ?: 0,
                items = items
            )
        } catch (e: Exception) {
            null
        }
    }

    /** 解析pois.json -> List<OfflinePoiData> */
    private fun parsePoisJson(json: String): List<OfflinePoiData> {
        return try {
            val listType = object : TypeToken<List<JsonObject>>() {}.type
            val jsonList: List<JsonObject> = gson.fromJson(json, listType)
            jsonList.map { obj ->
                OfflinePoiData(
                    name = obj.get("name")?.asString ?: "",
                    type = obj.get("type")?.asString ?: "unknown",
                    latitude = obj.get("latitude")?.let { if (it.isJsonNull) null else it.asDouble },
                    longitude = obj.get("longitude")?.let { if (it.isJsonNull) null else it.asDouble },
                    duration = obj.get("duration")?.asInt ?: 0,
                    notes = obj.get("notes")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 解析tiles_meta.json -> OfflineTilesMetadata */
    private fun parseTilesMetaJson(json: String): OfflineTilesMetadata? {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)

            val boundsObj = obj.getAsJsonObject("bounds")
            val bounds = if (boundsObj != null) {
                TileBounds(
                    minLat = boundsObj.get("min_lat")?.asDouble ?: 0.0,
                    maxLat = boundsObj.get("max_lat")?.asDouble ?: 0.0,
                    minLng = boundsObj.get("min_lng")?.asDouble ?: 0.0,
                    maxLng = boundsObj.get("max_lng")?.asDouble ?: 0.0
                )
            } else null

            val zoomLevels = obj.getAsJsonArray("zoom_levels")?.map { it.asInt } ?: emptyList()
            val tileCount = obj.get("tile_count_estimate")?.asInt ?: 0

            val centerObj = obj.getAsJsonObject("center")
            val center = if (centerObj != null) {
                OfflineCenter(
                    latitude = centerObj.get("latitude")?.asDouble ?: 0.0,
                    longitude = centerObj.get("longitude")?.asDouble ?: 0.0
                )
            } else null

            OfflineTilesMetadata(
                bounds = bounds,
                zoomLevels = zoomLevels,
                tileCountEstimate = tileCount,
                center = center
            )
        } catch (e: Exception) {
            null
        }
    }
}
