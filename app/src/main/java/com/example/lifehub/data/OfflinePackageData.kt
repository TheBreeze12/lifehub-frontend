package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

// ==================== Phase 47: 离线运动包数据模型 ====================

/** 离线包生成请求 */
data class OfflinePackageRequest(
    @SerializedName("plan_id") val planId: Int
)

/** 地图瓦片边界 */
data class TileBounds(
    @SerializedName("min_lat") val minLat: Double,
    @SerializedName("max_lat") val maxLat: Double,
    @SerializedName("min_lng") val minLng: Double,
    @SerializedName("max_lng") val maxLng: Double
) {
    /** 计算中心点纬度 */
    fun centerLat(): Double = (minLat + maxLat) / 2.0

    /** 计算中心点经度 */
    fun centerLng(): Double = (minLng + maxLng) / 2.0

    /** 检查坐标是否在边界内 */
    fun contains(lat: Double, lng: Double): Boolean {
        return lat in minLat..maxLat && lng in minLng..maxLng
    }

    /** 检查边界是否有效（非零区域） */
    fun isValid(): Boolean {
        return minLat != 0.0 || maxLat != 0.0 || minLng != 0.0 || maxLng != 0.0
    }
}

/** 离线包服务端响应数据 */
data class OfflinePackageInfo(
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("package_id") val packageId: String,
    @SerializedName("version") val version: Int,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("tile_bounds") val tileBounds: TileBounds? = null
)

/** 离线包生成响应 */
data class OfflinePackageResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: OfflinePackageInfo?
)

/** 离线包下载状态 */
enum class OfflinePackageStatus {
    /** 未下载 */
    NOT_DOWNLOADED,
    /** 正在生成（服务端） */
    GENERATING,
    /** 正在下载 */
    DOWNLOADING,
    /** 已下载 */
    DOWNLOADED,
    /** 下载/生成失败 */
    ERROR
}

/** 本地离线包记录（含下载状态和本地文件信息） */
data class LocalOfflinePackage(
    val planId: Int,
    val packageId: String,
    val version: Int,
    val fileSize: Long,
    val createdAt: String,
    val tileBounds: TileBounds? = null,
    val status: OfflinePackageStatus = OfflinePackageStatus.NOT_DOWNLOADED,
    val localFilePath: String? = null,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val planTitle: String? = null,
    val planDestination: String? = null
) {
    /** 文件大小格式化显示 */
    fun fileSizeFormatted(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> String.format("%.1fMB", fileSize / (1024.0 * 1024.0))
        }
    }

    /** 是否可以离线使用 */
    fun isAvailableOffline(): Boolean {
        return status == OfflinePackageStatus.DOWNLOADED && localFilePath != null
    }
}

/** 离线包内的运动方案数据（从plan.json解析） */
data class OfflinePlanData(
    val title: String = "",
    val destination: String = "",
    val date: String = "",
    val totalDuration: Int = 0,
    val totalCalories: Double = 0.0,
    val itemCount: Int = 0,
    val items: List<OfflinePlanItem> = emptyList()
)

/** 离线包内的运动项目 */
data class OfflinePlanItem(
    val dayIndex: Int = 1,
    val startTime: String? = null,
    val placeName: String = "",
    val placeType: String = "walking",
    val duration: Int = 0,
    val calories: Double = 0.0,
    val notes: String = ""
)

/** 离线包内的POI数据（从pois.json解析） */
data class OfflinePoiData(
    val name: String = "",
    val type: String = "unknown",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val duration: Int = 0,
    val notes: String = ""
)

/** 离线包内的瓦片元数据（从tiles_meta.json解析） */
data class OfflineTilesMetadata(
    val bounds: TileBounds? = null,
    val zoomLevels: List<Int> = emptyList(),
    val tileCountEstimate: Int = 0,
    val center: OfflineCenter? = null
)

/** 离线包中心点 */
data class OfflineCenter(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/** 离线包完整内容（解压后） */
data class OfflinePackageContents(
    val plan: OfflinePlanData? = null,
    val pois: List<OfflinePoiData> = emptyList(),
    val tilesMetadata: OfflineTilesMetadata? = null
)
