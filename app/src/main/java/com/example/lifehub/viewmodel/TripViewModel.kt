package com.example.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.*
import com.example.lifehub.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
