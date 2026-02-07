package com.example.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.*
import com.example.lifehub.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 用户ViewModel - 管理用户相关的状态和API调用 */
class UserViewModel : ViewModel() {

    private val apiService = RetrofitClient.apiService

    // 用户偏好更新状态
    private val _updatePreferencesState =
            MutableStateFlow<UpdatePreferencesState>(UpdatePreferencesState.Idle)
    val updatePreferencesState: StateFlow<UpdatePreferencesState> =
            _updatePreferencesState.asStateFlow()

    // 饮食历史状态
    private val _dietHistoryState = MutableStateFlow<DietHistoryState>(DietHistoryState.Idle)
    val dietHistoryState: StateFlow<DietHistoryState> = _dietHistoryState.asStateFlow()

    // 用户偏好状态
    private val _userPreferencesState =
            MutableStateFlow<UserPreferencesState>(UserPreferencesState.Idle)
    val userPreferencesState: StateFlow<UserPreferencesState> = _userPreferencesState.asStateFlow()

    // 登录状态
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // 注册状态
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    // Phase 48: 健康目标达成率状态
    private val _goalProgressState = MutableStateFlow<GoalProgressState>(GoalProgressState.Idle)
    val goalProgressState: StateFlow<GoalProgressState> = _goalProgressState.asStateFlow()

    // Phase 55: 一键遗忘状态
    private val _forgetDataState = MutableStateFlow<ForgetDataState>(ForgetDataState.Idle)
    val forgetDataState: StateFlow<ForgetDataState> = _forgetDataState.asStateFlow()

    // Phase 56: AI调用日志状态
    private val _aiCallLogState = MutableStateFlow<AiCallLogState>(AiCallLogState.Idle)
    val aiCallLogState: StateFlow<AiCallLogState> = _aiCallLogState.asStateFlow()

    // Phase 56: AI调用统计状态
    private val _aiCallLogStatsState = MutableStateFlow<AiCallLogStatsState>(AiCallLogStatsState.Idle)
    val aiCallLogStatsState: StateFlow<AiCallLogStatsState> = _aiCallLogStatsState.asStateFlow()

    /** Phase 55: 一键遗忘 - 删除用户所有云端数据 */
    fun forgetUserData(userId: Int) {
        viewModelScope.launch {
            _forgetDataState.value = ForgetDataState.Loading

            try {
                val response = apiService.deleteUserData(userId)

                if (response.code == 200 && response.data != null) {
                    _forgetDataState.value = ForgetDataState.Success(
                            totalDeleted = response.data.totalDeleted,
                            message = response.message ?: "数据删除成功"
                    )
                } else {
                    _forgetDataState.value =
                            ForgetDataState.Error(response.message ?: "数据删除失败")
                }
            } catch (e: Exception) {
                _forgetDataState.value = ForgetDataState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** Phase 55: 重置遗忘状态 */
    fun resetForgetDataState() {
        _forgetDataState.value = ForgetDataState.Idle
    }

    /** Phase 56: 获取AI调用日志列表 */
    fun getAiCallLogs(userId: Int, callType: String? = null, limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            _aiCallLogState.value = AiCallLogState.Loading

            try {
                val response = apiService.getAiCallLogs(userId, callType, limit, offset)

                if (response.code == 200 && response.data != null) {
                    _aiCallLogState.value = AiCallLogState.Success(response.data)
                } else {
                    _aiCallLogState.value =
                            AiCallLogState.Error(response.message ?: "获取AI调用日志失败")
                }
            } catch (e: Exception) {
                _aiCallLogState.value = AiCallLogState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** Phase 56: 获取AI调用统计 */
    fun getAiCallLogStats(userId: Int) {
        viewModelScope.launch {
            _aiCallLogStatsState.value = AiCallLogStatsState.Loading

            try {
                val response = apiService.getAiCallLogStats(userId)

                if (response.code == 200 && response.data != null) {
                    _aiCallLogStatsState.value = AiCallLogStatsState.Success(response.data)
                } else {
                    _aiCallLogStatsState.value =
                            AiCallLogStatsState.Error(response.message ?: "获取AI调用统计失败")
                }
            } catch (e: Exception) {
                _aiCallLogStatsState.value = AiCallLogStatsState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** Phase 48: 获取健康目标达成率 */
    fun getGoalProgress(userId: Int, days: Int = 7) {
        viewModelScope.launch {
            _goalProgressState.value = GoalProgressState.Loading

            try {
                val response = apiService.getGoalProgress(userId, days)

                if (response.code == 200 && response.data != null) {
                    _goalProgressState.value = GoalProgressState.Success(response.data)
                } else {
                    _goalProgressState.value =
                            GoalProgressState.Error(response.message ?: "获取健康目标达成率失败")
                }
            } catch (e: Exception) {
                _goalProgressState.value = GoalProgressState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 更新用户偏好设置 */
    fun updatePreferences(
            userId: Int,
            healthGoal: String? = null,
            allergens: List<String>? = null,
            travelPreference: String? = null,
            dailyBudget: Int? = null,
            weight: Double? = null,
            height: Double? = null,
            age: Int? = null,
            gender: String? = null
    ) {
        viewModelScope.launch {
            _updatePreferencesState.value = UpdatePreferencesState.Loading

            try {
                val response =
                        apiService.updateUserPreferences(
                                UpdatePreferencesRequest(
                                        userId = userId,
                                        healthGoal = healthGoal,
                                        allergens = allergens,
                                        travelPreference = travelPreference,
                                        dailyBudget = dailyBudget,
                                        weight = weight,
                                        height = height,
                                        age = age,
                                        gender = gender
                                )
                        )

                if (response.code == 200) {
                    _updatePreferencesState.value = UpdatePreferencesState.Success
                } else {
                    _updatePreferencesState.value =
                            UpdatePreferencesState.Error(response.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _updatePreferencesState.value = UpdatePreferencesState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 获取饮食历史记录 */
    fun getDietHistory(userId: Int, date: String? = null) {
        viewModelScope.launch {
            _dietHistoryState.value = DietHistoryState.Loading

            try {
                val response = apiService.getDietHistory(userId, date)

                if (response.code == 200 && response.data != null) {
                    _dietHistoryState.value = DietHistoryState.Success(response.data)
                } else {
                    _dietHistoryState.value = DietHistoryState.Error(response.message ?: "获取历史记录失败")
                }
            } catch (e: Exception) {
                _dietHistoryState.value = DietHistoryState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 获取用户偏好 */
    fun getUserPreferences(userId: Int) {
        viewModelScope.launch {
            _userPreferencesState.value = UserPreferencesState.Loading

            try {
                val response = apiService.getUserPreferences(userId)

                if (response.code == 200 && response.data != null) {
                    _userPreferencesState.value = UserPreferencesState.Success(response.data)
                } else {
                    _userPreferencesState.value =
                            UserPreferencesState.Error(response.message ?: "获取用户偏好失败")
                }
            } catch (e: Exception) {
                _userPreferencesState.value = UserPreferencesState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 登录（通过获取用户偏好验证用户是否存在） */
    fun login(nickname: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                // 通过获取用户偏好来验证用户是否存在
                //                val response = apiService.getUserPreferences(userId)
                // 通过用户ID和密码来进行验证
                val response = apiService.getUserData(nickname, password)
                if (response.code == 200 && response.data != null) {
                    _loginState.value =
                            LoginState.Success(
                                    userId = response.data.userId,
                                    nickname = response.data.nickname ?: "健康达人"
                            )
                    // 同时获取用户偏好
                    _userPreferencesState.value = UserPreferencesState.Success(response.data)
                } else {
                    _loginState.value = LoginState.Error(response.message ?: "用户不存在")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 注册 */
    fun register(nickname: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val response =
                        apiService.registerUser(
                                UserRegistrationRequest(nickname = nickname, password = password)
                        )
                if (response.code == 200 && response.userId != null) {
                    _registerState.value = RegisterState.Success(response.userId)
                } else {
                    _registerState.value = RegisterState.Error(response.message ?: "注册失败")
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 更新用户偏好（重写以返回UserPreferencesResponse） */
    fun updateUserPreferences(
            userId: Int,
            healthGoal: String? = null,
            allergens: List<String>? = null,
            travelPreference: String? = null,
            dailyBudget: Int? = null,
            weight: Double? = null,
            height: Double? = null,
            age: Int? = null,
            gender: String? = null
    ) {
        viewModelScope.launch {
            _updatePreferencesState.value = UpdatePreferencesState.Loading

            try {
                val response =
                        apiService.updateUserPreferences(
                                UpdatePreferencesRequest(
                                        userId = userId,
                                        healthGoal = healthGoal,
                                        allergens = allergens,
                                        travelPreference = travelPreference,
                                        dailyBudget = dailyBudget,
                                        weight = weight,
                                        height = height,
                                        age = age,
                                        gender = gender
                                )
                        )

                if (response.code == 200) {
                    _updatePreferencesState.value = UpdatePreferencesState.Success
                    // 更新后重新获取用户偏好
                    if (response.data != null) {
                        _userPreferencesState.value = UserPreferencesState.Success(response.data)
                    }
                } else {
                    _updatePreferencesState.value =
                            UpdatePreferencesState.Error(response.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _updatePreferencesState.value = UpdatePreferencesState.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 更新身体参数（体重、身高、年龄、性别） */
    fun updateBodyParams(
            userId: Int,
            weight: Double? = null,
            height: Double? = null,
            age: Int? = null,
            gender: String? = null
    ) {
        viewModelScope.launch {
            _updatePreferencesState.value = UpdatePreferencesState.Loading

            try {
                val response =
                        apiService.updateUserPreferences(
                                UpdatePreferencesRequest(
                                        userId = userId,
                                        healthGoal = null,
                                        allergens = null,
                                        travelPreference = null,
                                        dailyBudget = null,
                                        weight = weight,
                                        height = height,
                                        age = age,
                                        gender = gender
                                )
                        )

                if (response.code == 200) {
                    _updatePreferencesState.value = UpdatePreferencesState.Success
                    if (response.data != null) {
                        _userPreferencesState.value = UserPreferencesState.Success(response.data)
                    }
                } else {
                    _updatePreferencesState.value =
                            UpdatePreferencesState.Error(response.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _updatePreferencesState.value = UpdatePreferencesState.Error(e.message ?: "网络请求失败")
            }
        }
    }
}

/** 更新偏好状态 */
sealed class UpdatePreferencesState {
    object Idle : UpdatePreferencesState()
    object Loading : UpdatePreferencesState()
    object Success : UpdatePreferencesState()
    data class Error(val message: String) : UpdatePreferencesState()
}

/** 饮食历史状态 */
sealed class DietHistoryState {
    object Idle : DietHistoryState()
    object Loading : DietHistoryState()
    data class Success(val data: DietHistoryData) : DietHistoryState()
    data class Error(val message: String) : DietHistoryState()
}

/** 用户偏好状态 */
sealed class UserPreferencesState {
    object Idle : UserPreferencesState()
    object Loading : UserPreferencesState()
    data class Success(val data: UserPreferencesData) : UserPreferencesState()
    data class Error(val message: String) : UserPreferencesState()
}

/** 登录状态 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: Int, val nickname: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

// 注册状态
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val userId: Int) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

/** Phase 48: 健康目标达成率状态 */
sealed class GoalProgressState {
    object Idle : GoalProgressState()
    object Loading : GoalProgressState()
    data class Success(val data: GoalProgressData) : GoalProgressState()
    data class Error(val message: String) : GoalProgressState()
}

/** Phase 55: 一键遗忘状态 */
sealed class ForgetDataState {
    object Idle : ForgetDataState()
    object Loading : ForgetDataState()
    data class Success(val totalDeleted: Int, val message: String) : ForgetDataState()
    data class Error(val message: String) : ForgetDataState()
}

/** Phase 56: AI调用日志状态 */
sealed class AiCallLogState {
    object Idle : AiCallLogState()
    object Loading : AiCallLogState()
    data class Success(val data: AiCallLogListData) : AiCallLogState()
    data class Error(val message: String) : AiCallLogState()
}

/** Phase 56: AI调用统计状态 */
sealed class AiCallLogStatsState {
    object Idle : AiCallLogStatsState()
    object Loading : AiCallLogStatsState()
    data class Success(val data: AiCallLogStatsData) : AiCallLogStatsState()
    data class Error(val message: String) : AiCallLogStatsState()
}
