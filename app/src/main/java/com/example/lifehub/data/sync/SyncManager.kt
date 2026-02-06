package com.example.lifehub.data.sync

import android.content.Context
import android.util.Log
import com.example.lifehub.data.AddDietRecordRequest
import com.example.lifehub.data.CreateExerciseRecordRequest
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.repository.DietRepository
import com.example.lifehub.data.repository.ExerciseRepository
import com.example.lifehub.data.repository.TripRepository
import com.example.lifehub.data.repository.UserRepository
import com.example.lifehub.network.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 端云数据同步管理器 - Phase 35
 *
 * 核心同步策略：
 * 1. 本地优先（Local-first）：所有数据操作先写入本地Room，标记isSynced=false
 * 2. 定时同步：每15分钟自动检查并同步未上传数据
 * 3. 网络恢复同步：监听网络状态，恢复时自动触发同步
 * 4. 冲突解决：服务端优先（Server-wins），离线新增数据保留本地版本
 * 5. 弱网缓存后补传：离线状态下数据缓存在本地，联网后自动补传
 */
class SyncManager(
    private val apiService: ApiService,
    private val dietRepository: DietRepository,
    private val exerciseRepository: ExerciseRepository,
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val networkMonitor: NetworkMonitor,
    private val config: SyncConfig = SyncConfig()
) {
    companion object {
        private const val TAG = "SyncManager"

        @Volatile
        private var INSTANCE: SyncManager? = null

        /**
         * 获取SyncManager单例（需要Android Context）
         */
        fun getInstance(
            context: Context,
            apiService: ApiService,
            dietRepository: DietRepository,
            exerciseRepository: ExerciseRepository,
            tripRepository: TripRepository,
            userRepository: UserRepository
        ): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(
                    apiService = apiService,
                    dietRepository = dietRepository,
                    exerciseRepository = exerciseRepository,
                    tripRepository = tripRepository,
                    userRepository = userRepository,
                    networkMonitor = NetworkMonitor(context)
                ).also { INSTANCE = it }
            }
        }

        /**
         * 仅用于测试：允许注入自定义实例
         */
        internal fun setInstance(instance: SyncManager) {
            INSTANCE = instance
        }
    }

    /** 当前同步状态 */
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** 待同步记录数量 */
    private val _pendingChangesCount = MutableStateFlow(0)
    val pendingChangesCount: StateFlow<Int> = _pendingChangesCount.asStateFlow()

    private var syncJob: Job? = null
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 启动自动同步
     * 1. 监听网络变化，网络恢复时自动同步
     * 2. 定时周期性同步
     */
    fun startAutoSync(userId: Int) {
        if (!config.enableAutoSync) return

        stopAutoSync()
        syncJob = syncScope.launch {
            // 监听网络变化，网络恢复时触发同步
            launch {
                networkMonitor.observeNetworkState()
                    .filter { it == NetworkState.AVAILABLE }
                    .collect {
                        Log.d(TAG, "网络恢复，触发自动同步")
                        syncAll(userId)
                    }
            }

            // 定时周期性同步
            launch {
                while (isActive) {
                    delay(config.syncIntervalMs)
                    if (networkMonitor.isNetworkAvailable()) {
                        Log.d(TAG, "定时同步触发")
                        syncAll(userId)
                    }
                }
            }
        }
    }

    /**
     * 停止自动同步
     */
    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * 执行全量同步（上传+下载）
     * @return 同步结果
     */
    suspend fun syncAll(userId: Int): SyncResult {
        if (_syncStatus.value is SyncStatus.Syncing) {
            Log.d(TAG, "同步进行中，跳过重复请求")
            return SyncResult(errors = listOf("同步进行中"))
        }

        _syncStatus.value = SyncStatus.Syncing
        Log.d(TAG, "开始全量同步，userId=$userId")

        var result = SyncResult()
        val errors = mutableListOf<String>()

        try {
            // 1. 先上传本地未同步数据（本地优先）
            val uploadResult = uploadUnsyncedData(userId)
            result = result.copy(
                uploadedDietRecords = uploadResult.uploadedDietRecords,
                uploadedExerciseRecords = uploadResult.uploadedExerciseRecords
            )
            errors.addAll(uploadResult.errors)

            // 2. 再从云端下载最新数据
            val downloadResult = downloadFromServer(userId)
            result = result.copy(
                downloadedDietRecords = downloadResult.downloadedDietRecords,
                downloadedExerciseRecords = downloadResult.downloadedExerciseRecords,
                downloadedTripPlans = downloadResult.downloadedTripPlans,
                userPreferencesSynced = downloadResult.userPreferencesSynced
            )
            errors.addAll(downloadResult.errors)

            result = result.copy(errors = errors)

            _syncStatus.value = if (result.hasErrors) {
                SyncStatus.Error(errors.joinToString("; "))
            } else {
                SyncStatus.Success(result.totalSynced)
            }

            // 更新待同步计数
            updatePendingCount(userId)

        } catch (e: Exception) {
            Log.e(TAG, "同步异常", e)
            val errorMsg = e.message ?: "未知同步错误"
            result = result.copy(errors = errors + errorMsg)
            _syncStatus.value = SyncStatus.Error(errorMsg)
        }

        Log.d(TAG, "同步完成: uploaded=${result.uploadedDietRecords}+${result.uploadedExerciseRecords}, " +
                "downloaded=${result.downloadedDietRecords}+${result.downloadedTripPlans}, " +
                "errors=${result.errors.size}")
        return result
    }

    /**
     * 上传本地未同步的数据到云端
     * 遍历所有isSynced=false的记录，逐条上传到服务端
     */
    suspend fun uploadUnsyncedData(userId: Int): SyncResult {
        var uploadedDiet = 0
        var uploadedExercise = 0
        val errors = mutableListOf<String>()

        // 上传未同步的饮食记录
        try {
            val unsyncedDiet = dietRepository.getUnsyncedRecords(userId)
            Log.d(TAG, "待上传饮食记录: ${unsyncedDiet.size}条")

            for (record in unsyncedDiet) {
                try {
                    val request = AddDietRecordRequest(
                        userId = record.userId,
                        foodName = record.foodName,
                        calories = record.calories,
                        protein = record.protein,
                        fat = record.fat,
                        carbs = record.carbs,
                        mealType = record.mealType,
                        recordDate = record.recordDate
                    )
                    val response = apiService.addDietRecord(request)
                    if (response.code == 200) {
                        // 标记为已同步
                        dietRepository.markAsSynced(record.localId, record.localId.toInt())
                        uploadedDiet++
                        Log.d(TAG, "饮食记录上传成功: localId=${record.localId}")
                    } else {
                        errors.add("饮食记录上传失败(localId=${record.localId}): ${response.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "上传饮食记录异常: localId=${record.localId}", e)
                    errors.add("饮食记录上传异常(localId=${record.localId}): ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取未同步饮食记录异常", e)
            errors.add("获取未同步饮食记录失败: ${e.message}")
        }

        // 上传未同步的运动记录
        try {
            val unsyncedExercise = exerciseRepository.getUnsyncedRecords(userId)
            Log.d(TAG, "待上传运动记录: ${unsyncedExercise.size}条")

            for (record in unsyncedExercise) {
                try {
                    val request = CreateExerciseRecordRequest(
                        userId = record.userId,
                        planId = record.planId,
                        exerciseType = record.exerciseType,
                        actualCalories = record.actualCalories,
                        actualDuration = record.actualDuration,
                        distance = record.distance,
                        exerciseDate = record.exerciseDate,
                        startedAt = record.startedAt,
                        endedAt = record.endedAt,
                        notes = record.notes
                    )
                    val response = apiService.createExerciseRecord(request)
                    if (response.code == 200 || response.code == 201) {
                        val serverId = response.data?.id ?: record.localId.toInt()
                        exerciseRepository.markAsSynced(record.localId, serverId)
                        uploadedExercise++
                        Log.d(TAG, "运动记录上传成功: localId=${record.localId}")
                    } else {
                        errors.add("运动记录上传失败(localId=${record.localId}): ${response.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "上传运动记录异常: localId=${record.localId}", e)
                    errors.add("运动记录上传异常(localId=${record.localId}): ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取未同步运动记录异常", e)
            errors.add("获取未同步运动记录失败: ${e.message}")
        }

        return SyncResult(
            uploadedDietRecords = uploadedDiet,
            uploadedExerciseRecords = uploadedExercise,
            errors = errors
        )
    }

    /**
     * 从云端下载最新数据到本地
     * 获取服务端最新数据，与本地比较后合并（冲突时服务端优先）
     */
    suspend fun downloadFromServer(userId: Int): SyncResult {
        var downloadedDiet = 0
        var downloadedTrips = 0
        var prefsSynced = false
        val errors = mutableListOf<String>()

        // 下载用户偏好
        try {
            val prefsResponse = apiService.getUserPreferences(userId)
            if (prefsResponse.code == 200 && prefsResponse.data != null) {
                userRepository.saveUserPreferences(prefsResponse.data)
                prefsSynced = true
                Log.d(TAG, "用户偏好同步成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步用户偏好异常", e)
            errors.add("同步用户偏好失败: ${e.message}")
        }

        // 下载饮食记录并与本地合并
        try {
            val dietResponse = apiService.getDietRecords(userId)
            if (dietResponse.code == 200 && dietResponse.data != null) {
                // 获取本地所有记录用于冲突比较
                val localEntities = dietRepository.getAllEntities(userId)

                for ((_, records) in dietResponse.data) {
                    for (serverRecord in records) {
                        val existing = ConflictResolver.findMatchingLocalRecord(
                            localEntities, serverRecord
                        )
                        if (existing != null) {
                            // 本地已存在，检查是否需要更新
                            if (ConflictResolver.shouldUpdateLocalDietRecord(existing, serverRecord)) {
                                val resolved = ConflictResolver.resolveDietRecord(existing, serverRecord)
                                dietRepository.updateRecord(resolved)
                                downloadedDiet++
                            }
                        } else {
                            // 本地不存在，从服务端插入
                            dietRepository.saveFromServer(serverRecord)
                            downloadedDiet++
                        }
                    }
                }
                Log.d(TAG, "饮食记录下载同步: ${downloadedDiet}条")
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载饮食记录异常", e)
            errors.add("下载饮食记录失败: ${e.message}")
        }

        // 下载运动计划
        try {
            val tripsResponse = apiService.getTripList(userId)
            if (tripsResponse.code == 200 && tripsResponse.data != null) {
                for (tripSummary in tripsResponse.data) {
                    try {
                        val detailResponse = apiService.getTripDetail(tripSummary.tripId)
                        if (detailResponse.code == 200 && detailResponse.data != null) {
                            tripRepository.savePlan(detailResponse.data, userId)
                            downloadedTrips++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "下载运动计划详情异常: tripId=${tripSummary.tripId}", e)
                    }
                }
                Log.d(TAG, "运动计划下载同步: ${downloadedTrips}条")
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载运动计划异常", e)
            errors.add("下载运动计划失败: ${e.message}")
        }

        return SyncResult(
            downloadedDietRecords = downloadedDiet,
            downloadedTripPlans = downloadedTrips,
            userPreferencesSynced = prefsSynced,
            errors = errors
        )
    }

    /**
     * 仅上传同步（用于弱网恢复后的快速同步）
     * 只上传本地未同步数据，不下载云端数据
     */
    suspend fun uploadOnlySync(userId: Int): SyncResult {
        _syncStatus.value = SyncStatus.Syncing
        val result = uploadUnsyncedData(userId)
        _syncStatus.value = if (result.hasErrors) {
            SyncStatus.Error(result.errors.joinToString("; "))
        } else {
            SyncStatus.Success(result.totalSynced)
        }
        updatePendingCount(userId)
        return result
    }

    /**
     * 更新待同步记录计数
     */
    suspend fun updatePendingCount(userId: Int) {
        try {
            val dietCount = dietRepository.getUnsyncedRecords(userId).size
            val exerciseCount = exerciseRepository.getUnsyncedRecords(userId).size
            _pendingChangesCount.value = dietCount + exerciseCount
        } catch (e: Exception) {
            Log.e(TAG, "更新待同步计数异常", e)
        }
    }

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return networkMonitor.isNetworkAvailable()
    }

    /**
     * 释放资源
     */
    fun destroy() {
        stopAutoSync()
        syncScope.cancel()
        INSTANCE = null
    }
}
