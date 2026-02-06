package com.example.lifehub.data.sync

/**
 * 同步状态 - Phase 35
 * 表示当前同步操作的状态
 */
sealed class SyncStatus {
    /** 空闲状态 */
    object Idle : SyncStatus()
    /** 同步进行中 */
    object Syncing : SyncStatus()
    /** 同步成功 */
    data class Success(val syncedCount: Int, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    /** 同步失败 */
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
}

/**
 * 同步结果 - Phase 35
 * 记录一次同步操作的详细结果
 */
data class SyncResult(
    val uploadedDietRecords: Int = 0,
    val uploadedExerciseRecords: Int = 0,
    val downloadedDietRecords: Int = 0,
    val downloadedExerciseRecords: Int = 0,
    val downloadedTripPlans: Int = 0,
    val userPreferencesSynced: Boolean = false,
    val errors: List<String> = emptyList()
) {
    /** 总同步条数 */
    val totalSynced: Int
        get() = uploadedDietRecords + uploadedExerciseRecords +
                downloadedDietRecords + downloadedExerciseRecords + downloadedTripPlans +
                if (userPreferencesSynced) 1 else 0

    /** 是否存在错误 */
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /** 是否完全成功 */
    val isFullSuccess: Boolean get() = !hasErrors
}

/**
 * 网络状态 - Phase 35
 */
enum class NetworkState {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}

/**
 * 同步配置 - Phase 35
 * 控制自动同步的行为参数
 */
data class SyncConfig(
    /** 自动同步间隔（毫秒），默认15分钟 */
    val syncIntervalMs: Long = 15 * 60 * 1000L,
    /** 重试延迟（毫秒），默认30秒 */
    val retryDelayMs: Long = 30 * 1000L,
    /** 最大重试次数 */
    val maxRetryCount: Int = 3,
    /** 是否启用自动同步 */
    val enableAutoSync: Boolean = true
)
