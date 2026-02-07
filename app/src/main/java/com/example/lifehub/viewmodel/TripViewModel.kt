package com.example.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.*
import com.example.lifehub.data.local.OfflinePackageManager
import com.example.lifehub.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** 行程ViewModel - 管理行程相关的状态和API调用 */
class TripViewModel : ViewModel() {

    private val apiService = RetrofitClient.apiService

    // 生成行程状态
    private val _generateTripState = MutableStateFlow<GenerateTripState>(GenerateTripState.Idle)
    val generateTripState: StateFlow<GenerateTripState> = _generateTripState.asStateFlow()

    // 行程详情状态
    private val _tripDetailState = MutableStateFlow<TripDetailState>(TripDetailState.Idle)
    val tripDetailState: StateFlow<TripDetailState> = _tripDetailState.asStateFlow()

    // 首页行程状态
    private val _homeTripsState = MutableStateFlow<HomeTripsState>(HomeTripsState.Idle)
    val homeTripsState: StateFlow<HomeTripsState> = _homeTripsState.asStateFlow()

    // 最近行程状态
    private val _recentTripsState = MutableStateFlow<RecentTripsState>(RecentTripsState.Idle)
    val recentTripsState: StateFlow<RecentTripsState> = _recentTripsState.asStateFlow()

    // 全部行程列表状态
    private val _tripListState = MutableStateFlow<TripListState>(TripListState.Idle)
    val tripListState: StateFlow<TripListState> = _tripListState.asStateFlow()

    // Phase 24: 帕累托路径状态
    private val _routesState = MutableStateFlow<RoutesState>(RoutesState.Idle)
    val routesState: StateFlow<RoutesState> = _routesState.asStateFlow()

    // Phase 24: 当前选中的路线索引
    private val _selectedRouteIndex = MutableStateFlow(0)
    val selectedRouteIndex: StateFlow<Int> = _selectedRouteIndex.asStateFlow()

    // Phase 33: Plan B（天气动态调整）状态
    private val _planBState = MutableStateFlow<PlanBState>(PlanBState.Idle)
    val planBState: StateFlow<PlanBState> = _planBState.asStateFlow()

    // Phase 47: 离线运动包状态
    private val _offlinePackageState = MutableStateFlow<OfflinePackageState>(OfflinePackageState.Idle)
    val offlinePackageState: StateFlow<OfflinePackageState> = _offlinePackageState.asStateFlow()

    // Phase 47: 离线包列表状态
    private val _offlinePackageListState = MutableStateFlow<OfflinePackageListState>(OfflinePackageListState.Idle)
    val offlinePackageListState: StateFlow<OfflinePackageListState> = _offlinePackageListState.asStateFlow()

    // Phase 47: 离线包管理器（延迟初始化，需要Context设置存储目录）
    private var _offlinePackageManager: OfflinePackageManager? = null
    val offlinePackageManager: OfflinePackageManager?
        get() = _offlinePackageManager

    /** Phase 47: 初始化离线包管理器 */
    fun initOfflinePackageManager(storageDir: File) {
        if (_offlinePackageManager == null) {
            _offlinePackageManager = OfflinePackageManager(storageDir)
        }
    }

    /**
     * 生成运动计划
     * @param userId 用户ID
     * @param query 用户输入的运动需求
     * @param preferences 用户偏好（可选）
     * @param latitude 用户当前位置纬度（可选）
     * @param longitude 用户当前位置经度（可选）
     */
    fun generateTrip(
            userId: Int,
            query: String,
            preferences: UserPreferences? = null,
            latitude: Double? = null,
            longitude: Double? = null
    ) {
        viewModelScope.launch {
            _generateTripState.value = GenerateTripState.Loading

            try {
                val response =
                        apiService.generateTrip(
                                GenerateTripRequest(
                                        userId = userId,
                                        query = query,
                                        preferences = preferences,
                                        latitude = latitude,
                                        longitude = longitude
                                )
                        )

                if (response.code == 200 && response.data != null) {
                    _generateTripState.value = GenerateTripState.Success(response.data)
                } else {
                    _generateTripState.value =
                            GenerateTripState.Error(response.message ?: "生成运动计划失败")
                }
            } catch (e: Exception) {
                _generateTripState.value = GenerateTripState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /**
     * 获取行程详情
     * @param tripId 行程ID
     */
    fun getTripDetail(tripId: Int) {
        viewModelScope.launch {
            _tripDetailState.value = TripDetailState.Loading

            try {
                val response = apiService.getTripDetail(tripId)

                if (response.code == 200 && response.data != null) {
                    _tripDetailState.value = TripDetailState.Success(response.data)
                } else {
                    _tripDetailState.value = TripDetailState.Error(response.message ?: "获取行程详情失败")
                }
            } catch (e: Exception) {
                _tripDetailState.value = TripDetailState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /**
     * 获取首页行程
     * @param userId 用户ID
     * @param limit 返回数量限制（默认3条）
     */
    fun getHomeTrips(userId: Int, limit: Int = 3) {
        viewModelScope.launch {
            _homeTripsState.value = HomeTripsState.Loading

            try {
                val response = apiService.getHomeTrips(userId, limit)

                if (response.code == 200 && response.data != null) {
                    _homeTripsState.value = HomeTripsState.Success(response.data)
                } else {
                    _homeTripsState.value = HomeTripsState.Error(response.message ?: "获取首页行程失败")
                }
            } catch (e: Exception) {
                _homeTripsState.value = HomeTripsState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /**
     * 获取最近行程
     * @param userId 用户ID
     * @param limit 返回数量限制（默认5条）
     */
    fun getRecentTrips(userId: Int, limit: Int = 5) {
        viewModelScope.launch {
            _recentTripsState.value = RecentTripsState.Loading

            try {
                val response = apiService.getRecentTrips(userId, limit)

                if (response.code == 200 && response.data != null) {
                    _recentTripsState.value = RecentTripsState.Success(response.data)
                } else {
                    _recentTripsState.value = RecentTripsState.Error(response.message ?: "获取最近行程失败")
                }
            } catch (e: Exception) {
                _recentTripsState.value = RecentTripsState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /**
     * 获取用户全部行程列表
     * @param userId 用户ID
     */
    fun getTripList(userId: Int) {
        viewModelScope.launch {
            _tripListState.value = TripListState.Loading

            try {
                val response = apiService.getTripList(userId)

                if (response.code == 200 && response.data != null) {
                    _tripListState.value = TripListState.Success(response.data)
                } else {
                    _tripListState.value = TripListState.Error(response.message ?: "获取行程列表失败")
                }
            } catch (e: Exception) {
                _tripListState.value = TripListState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 重置生成行程状态 */
    fun resetGenerateTripState() {
        _generateTripState.value = GenerateTripState.Idle
    }

    /** 刷新首页行程 */
    fun refreshHomeTrips(userId: Int) {
        getHomeTrips(userId)
    }

    /** 刷新最近行程 */
    fun refreshRecentTrips(userId: Int) {
        getRecentTrips(userId)
    }

    /** 重置所有状态（用于清除缓存） */
    fun resetAllStates() {
        _generateTripState.value = GenerateTripState.Idle
        _tripDetailState.value = TripDetailState.Idle
        _homeTripsState.value = HomeTripsState.Idle
        _recentTripsState.value = RecentTripsState.Idle
        _tripListState.value = TripListState.Idle
    }

    /** 重置首页行程状态 */
    fun resetHomeTripsState() {
        _homeTripsState.value = HomeTripsState.Idle
    }

    /** 重置最近行程状态 */
    fun resetRecentTripsState() {
        _recentTripsState.value = RecentTripsState.Idle
    }

    /** 重置行程列表状态 */
    fun resetTripListState() {
        _tripListState.value = TripListState.Idle
    }

    // ==================== Phase 24: 帕累托路径方法 ====================

    /**
     * 生成帕累托最优路径
     * @param startLat 起点纬度
     * @param startLng 起点经度
     * @param targetCalories 目标热量消耗
     * @param maxTimeMinutes 最大运动时间（分钟）
     * @param exerciseType 运动类型
     * @param weightKg 用户体重
     */
    fun generateRoutes(
            startLat: Double,
            startLng: Double,
            targetCalories: Double,
            maxTimeMinutes: Int = 60,
            exerciseType: String = "walking",
            weightKg: Double = 70.0
    ) {
        viewModelScope.launch {
            _routesState.value = RoutesState.Loading

            try {
                val response =
                        apiService.generateRoutes(
                                GenerateRoutesRequest(
                                        startLat = startLat,
                                        startLng = startLng,
                                        targetCalories = targetCalories,
                                        maxTimeMinutes = maxTimeMinutes,
                                        exerciseType = exerciseType,
                                        weightKg = weightKg
                                )
                        )

                if (response.code == 200 && response.data != null) {
                    _routesState.value = RoutesState.Success(response.data)
                    _selectedRouteIndex.value = 0 // 默认选中第一条路线
                } else {
                    _routesState.value =
                            RoutesState.Error(response.message ?: "生成路径失败")
                }
            } catch (e: Exception) {
                _routesState.value = RoutesState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /**
     * 选择路线
     * @param index 路线索引
     */
    fun selectRoute(index: Int) {
        val currentState = _routesState.value
        if (currentState is RoutesState.Success) {
            val maxIndex = currentState.data.routes.size - 1
            if (index in 0..maxIndex) {
                _selectedRouteIndex.value = index
            }
        }
    }

    /** 获取当前选中的路线 */
    fun getSelectedRoute(): ParetoRoute? {
        val currentState = _routesState.value
        return if (currentState is RoutesState.Success) {
            val index = _selectedRouteIndex.value
            currentState.data.routes.getOrNull(index)
        } else {
            null
        }
    }

    /** 重置路径状态 */
    fun resetRoutesState() {
        _routesState.value = RoutesState.Idle
        _selectedRouteIndex.value = 0
    }

    // ==================== Phase 33: Plan B 天气动态调整方法 ====================

    /**
     * 获取运动计划的Plan B（天气动态调整方案）
     * @param planId 运动计划ID
     */
    fun fetchPlanB(planId: Int) {
        viewModelScope.launch {
            _planBState.value = PlanBState.Loading

            try {
                val response = apiService.getPlanB(planId)

                if (response.code == 200 && response.data != null) {
                    _planBState.value = PlanBState.Success(response.data)
                } else {
                    _planBState.value =
                            PlanBState.Error(response.message ?: "获取天气调整方案失败")
                }
            } catch (e: Exception) {
                _planBState.value = PlanBState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 重置Plan B状态 */
    fun resetPlanBState() {
        _planBState.value = PlanBState.Idle
    }

    // ==================== Phase 47: 离线运动包方法 ====================

    /**
     * 生成并下载离线运动包
     * 流程：1.请求服务端生成 -> 2.获取package_id -> 3.下载ZIP文件 -> 4.保存到本地
     *
     * @param planId 运动计划ID
     * @param planTitle 计划标题（用于本地显示）
     * @param planDestination 计划目的地（用于本地显示）
     */
    fun generateAndDownloadOfflinePackage(
        planId: Int,
        planTitle: String? = null,
        planDestination: String? = null
    ) {
        val manager = _offlinePackageManager ?: run {
            _offlinePackageState.value = OfflinePackageState.Error("离线包管理器未初始化")
            return
        }

        viewModelScope.launch {
            _offlinePackageState.value = OfflinePackageState.Generating

            try {
                // Step 1: 请求服务端生成离线包
                val generateResponse = withContext(Dispatchers.IO) {
                    apiService.generateOfflinePackage(OfflinePackageRequest(planId = planId))
                }

                if (generateResponse.code != 200 || generateResponse.data == null) {
                    _offlinePackageState.value =
                        OfflinePackageState.Error(generateResponse.message ?: "生成离线包失败")
                    return@launch
                }

                val packageInfo = generateResponse.data
                manager.savePackageInfo(packageInfo, planTitle, planDestination)
                manager.updateStatus(packageInfo.packageId, OfflinePackageStatus.DOWNLOADING)
                _offlinePackageState.value = OfflinePackageState.Downloading(0f)

                // Step 2: 下载ZIP文件
                val responseBody = withContext(Dispatchers.IO) {
                    apiService.downloadOfflinePackage(packageInfo.packageId)
                }

                // Step 3: 保存到本地文件
                val localPath = manager.getPackageFilePath(packageInfo.packageId)
                withContext(Dispatchers.IO) {
                    val totalBytes = responseBody.contentLength()
                    val inputStream = responseBody.byteStream()
                    val outputStream = FileOutputStream(File(localPath))

                    inputStream.use { input ->
                        outputStream.use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Long = 0
                            var read: Int

                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesRead += read
                                if (totalBytes > 0) {
                                    val progress = bytesRead.toFloat() / totalBytes.toFloat()
                                    manager.updateStatus(
                                        packageInfo.packageId,
                                        OfflinePackageStatus.DOWNLOADING,
                                        progress = progress
                                    )
                                    _offlinePackageState.value = OfflinePackageState.Downloading(progress)
                                }
                            }
                        }
                    }
                }

                // Step 4: 更新状态为已下载
                manager.updateStatus(
                    packageInfo.packageId,
                    OfflinePackageStatus.DOWNLOADED,
                    localFilePath = localPath
                )
                _offlinePackageState.value = OfflinePackageState.Success(packageInfo)

            } catch (e: Exception) {
                _offlinePackageState.value = OfflinePackageState.Error(e.message ?: "下载离线包失败")
            }
        }
    }

    /** Phase 47: 加载已下载的离线包列表 */
    fun loadOfflinePackages() {
        val manager = _offlinePackageManager ?: return
        val packages = manager.getAllPackages()
        _offlinePackageListState.value = OfflinePackageListState.Success(packages)
    }

    /** Phase 47: 删除离线包 */
    fun deleteOfflinePackage(packageId: String) {
        val manager = _offlinePackageManager ?: return
        manager.deletePackage(packageId)
        loadOfflinePackages()
    }

    /** Phase 47: 查看离线包内容 */
    fun getOfflinePackageContents(packageId: String): OfflinePackageContents? {
        val manager = _offlinePackageManager ?: return null
        val pkg = manager.getPackageByPackageId(packageId) ?: return null
        val filePath = pkg.localFilePath ?: return null
        return manager.parsePackageContents(filePath)
    }

    /** Phase 47: 重置离线包状态 */
    fun resetOfflinePackageState() {
        _offlinePackageState.value = OfflinePackageState.Idle
    }
}

/** 生成行程状态 */
sealed class GenerateTripState {
    object Idle : GenerateTripState()
    object Loading : GenerateTripState()
    data class Success(val tripPlan: TripPlan) : GenerateTripState()
    data class Error(val message: String) : GenerateTripState()
}

/** 行程详情状态 */
sealed class TripDetailState {
    object Idle : TripDetailState()
    object Loading : TripDetailState()
    data class Success(val tripPlan: TripPlan) : TripDetailState()
    data class Error(val message: String) : TripDetailState()
}

/** 首页行程状态 */
sealed class HomeTripsState {
    object Idle : HomeTripsState()
    object Loading : HomeTripsState()
    data class Success(val trips: List<TripSummary>) : HomeTripsState()
    data class Error(val message: String) : HomeTripsState()
}

/** 最近行程状态 */
sealed class RecentTripsState {
    object Idle : RecentTripsState()
    object Loading : RecentTripsState()
    data class Success(val trips: List<TripSummary>) : RecentTripsState()
    data class Error(val message: String) : RecentTripsState()
}

/** 行程列表状态 */
sealed class TripListState {
    object Idle : TripListState()
    object Loading : TripListState()
    data class Success(val trips: List<TripSummary>) : TripListState()
    data class Error(val message: String) : TripListState()
}

/** Phase 24: 帕累托路径状态 */
sealed class RoutesState {
    object Idle : RoutesState()
    object Loading : RoutesState()
    data class Success(val data: RoutesResponseData) : RoutesState()
    data class Error(val message: String) : RoutesState()
}

/** Phase 33: Plan B（天气动态调整）状态 */
sealed class PlanBState {
    object Idle : PlanBState()
    object Loading : PlanBState()
    data class Success(val data: PlanBData) : PlanBState()
    data class Error(val message: String) : PlanBState()
}

/** Phase 47: 离线运动包操作状态 */
sealed class OfflinePackageState {
    object Idle : OfflinePackageState()
    object Generating : OfflinePackageState()
    data class Downloading(val progress: Float) : OfflinePackageState()
    data class Success(val packageInfo: OfflinePackageInfo) : OfflinePackageState()
    data class Error(val message: String) : OfflinePackageState()
}

/** Phase 47: 离线包列表状态 */
sealed class OfflinePackageListState {
    object Idle : OfflinePackageListState()
    data class Success(val packages: List<LocalOfflinePackage>) : OfflinePackageListState()
}
