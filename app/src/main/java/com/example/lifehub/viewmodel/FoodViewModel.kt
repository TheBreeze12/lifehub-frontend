package com.example.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.FoodData
import com.example.lifehub.data.FoodRequest
import com.example.lifehub.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI状态 */
sealed class FoodUiState {
    object Idle : FoodUiState()
    object Loading : FoodUiState()
    data class Success(val foodData: FoodData) : FoodUiState()
    data class Error(val message: String) : FoodUiState()
}

/** 菜品查询ViewModel */
class FoodViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FoodUiState>(FoodUiState.Idle)
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    /**
     * 分析菜品营养成分
     * @param foodName 菜品名称
     */
    fun analyzeFoodNutrition(foodName: String) {
        if (foodName.isBlank()) {
            _uiState.value = FoodUiState.Error("请输入菜品名称")
            return
        }

        viewModelScope.launch {
            _uiState.value = FoodUiState.Loading

            try {
                val response =
                        RetrofitClient.apiService.analyzeFoodNutrition(
                                FoodRequest(foodName = foodName.trim())
                        )

                if (response.success && response.data != null) {
                    _uiState.value = FoodUiState.Success(response.data)
                } else {
                    _uiState.value = FoodUiState.Error(response.message ?: "查询失败，请稍后重试")
                }
            } catch (e: Exception) {
                _uiState.value =
                        FoodUiState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "查询失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /** 重置状态 */
    fun resetState() {
        _uiState.value = FoodUiState.Idle
    }
}
