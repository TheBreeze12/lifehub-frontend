package com.example.lifehub.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.AddDietRecordRequest
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.DishItem
import com.example.lifehub.data.FoodData
import com.example.lifehub.data.FoodRequest
import com.example.lifehub.network.RetrofitClient
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** UI状态 */
sealed class FoodUiState {
    object Idle : FoodUiState()
    object Loading : FoodUiState()
    data class Success(val foodData: FoodData) : FoodUiState()
    data class Error(val message: String) : FoodUiState()
}

/** 菜单识别UI状态 */
sealed class MenuRecognitionState {
    object Idle : MenuRecognitionState()
    object Loading : MenuRecognitionState()
    data class Success(val dishes: List<DishItem>) : MenuRecognitionState()
    data class Error(val message: String) : MenuRecognitionState()
}

/** 添加饮食记录UI状态 */
sealed class AddDietRecordState {
    object Idle : AddDietRecordState()
    object Loading : AddDietRecordState()
    data class Success(val message: String) : AddDietRecordState()
    data class Error(val message: String) : AddDietRecordState()
}

/** 饮食记录列表UI状态 */
sealed class DietRecordsState {
    object Idle : DietRecordsState()
    object Loading : DietRecordsState()
    data class Success(val records: Map<String, List<DietRecord>>) : DietRecordsState()
    data class Error(val message: String) : DietRecordsState()
}

/** 菜品查询ViewModel */
class FoodViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FoodUiState>(FoodUiState.Idle)
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    private val _recognitionState =
            MutableStateFlow<MenuRecognitionState>(MenuRecognitionState.Idle)
    val recognitionState: StateFlow<MenuRecognitionState> = _recognitionState.asStateFlow()

    private val _addDietRecordState = MutableStateFlow<AddDietRecordState>(AddDietRecordState.Idle)
    val addDietRecordState: StateFlow<AddDietRecordState> = _addDietRecordState.asStateFlow()

    private val _dietRecordsState = MutableStateFlow<DietRecordsState>(DietRecordsState.Idle)
    val dietRecordsState: StateFlow<DietRecordsState> = _dietRecordsState.asStateFlow()

    private val _todayDietRecordsState = MutableStateFlow<DietRecordsState>(DietRecordsState.Idle)
    val todayDietRecordsState: StateFlow<DietRecordsState> = _todayDietRecordsState.asStateFlow()

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

    /**
     * 识别菜单图片
     * @param imageUri 图片URI
     * @param context Android Context（用于读取文件）
     * @param userId 用户ID（可选，用于保存识别结果）
     */
    fun recognizeMenu(imageUri: Uri, context: android.content.Context, userId: Int? = null) {
        viewModelScope.launch {
            _recognitionState.value = MenuRecognitionState.Loading

            try {
                // 将Uri转换为MultipartBody.Part
                val imagePart =
                        createImagePart(imageUri, context)
                                ?: run {
                                    _recognitionState.value = MenuRecognitionState.Error("无法读取图片文件")
                                    return@launch
                                }

                // 调用API（传递userId以保存识别结果）
                val userIdBody = userId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = RetrofitClient.apiService.recognizeMenu(imagePart, userIdBody)

                if (response.code == 200 && response.data?.dishes != null) {
                    _recognitionState.value = MenuRecognitionState.Success(response.data.dishes)
                } else {
                    _recognitionState.value =
                            MenuRecognitionState.Error(response.message ?: "识别失败，请稍后重试")
                }
            } catch (e: Exception) {
                _recognitionState.value =
                        MenuRecognitionState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "识别失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /**
     * 获取最新的菜单识别结果
     * @param userId 用户ID（可选）
     */
    fun getLatestRecognition(userId: Int? = null) {
        viewModelScope.launch {
            _recognitionState.value = MenuRecognitionState.Loading

            try {
                val response = RetrofitClient.apiService.getLatestRecognition(userId)

                if (response.code == 200 && response.data?.dishes != null) {
                    if (response.data.dishes.isNotEmpty()) {
                        _recognitionState.value = MenuRecognitionState.Success(response.data.dishes)
                    } else {
                        _recognitionState.value = MenuRecognitionState.Idle
                    }
                } else if (response.code == 404) {
                    // 未找到记录，返回Idle状态
                    _recognitionState.value = MenuRecognitionState.Idle
                } else {
                    _recognitionState.value =
                            MenuRecognitionState.Error(response.message ?: "获取失败，请稍后重试")
                }
            } catch (e: Exception) {
                _recognitionState.value =
                        MenuRecognitionState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "获取失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /** 将Uri转换为MultipartBody.Part */
    private fun createImagePart(uri: Uri, context: android.content.Context): MultipartBody.Part? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return null
            }

            // 创建临时文件
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
            inputStream.close()

            // 创建RequestBody
            val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

            // 创建MultipartBody.Part
            MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 重置识别状态 */
    fun resetRecognitionState() {
        _recognitionState.value = MenuRecognitionState.Idle
    }

    /**
     * 添加饮食记录
     * @param userId 用户ID
     * @param foodName 菜品名称
     * @param calories 热量
     * @param protein 蛋白质
     * @param fat 脂肪
     * @param carbs 碳水化合物
     * @param mealType 餐次（breakfast/lunch/dinner/snack 或 早餐/午餐/晚餐/加餐）
     * @param recordDate 记录日期（YYYY-MM-DD格式，可选，默认为今天）
     */
    fun addDietRecord(
            userId: Int,
            foodName: String,
            calories: Double,
            protein: Double,
            fat: Double,
            carbs: Double,
            mealType: String = "lunch",
            recordDate: String? = null
    ) {
        viewModelScope.launch {
            _addDietRecordState.value = AddDietRecordState.Loading

            try {
                val date = recordDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val request =
                        AddDietRecordRequest(
                                userId = userId,
                                foodName = foodName,
                                calories = calories,
                                protein = protein,
                                fat = fat,
                                carbs = carbs,
                                mealType = mealType,
                                recordDate = date
                        )

                val response = RetrofitClient.apiService.addDietRecord(request)

                if (response.code == 200) {
                    _addDietRecordState.value =
                            AddDietRecordState.Success(response.message ?: "记录成功")
                } else {
                    _addDietRecordState.value =
                            AddDietRecordState.Error(response.message ?: "添加记录失败")
                }
            } catch (e: Exception) {
                _addDietRecordState.value =
                        AddDietRecordState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "添加记录失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /**
     * 获取用户所有饮食记录（按日期分组）
     * @param userId 用户ID
     */
    fun getDietRecords(userId: Int) {
        viewModelScope.launch {
            _dietRecordsState.value = DietRecordsState.Loading

            try {
                val response = RetrofitClient.apiService.getDietRecords(userId)

                if (response.code == 200 && response.data != null) {
                    _dietRecordsState.value = DietRecordsState.Success(response.data)
                } else {
                    _dietRecordsState.value = DietRecordsState.Error(response.message ?: "获取记录失败")
                }
            } catch (e: Exception) {
                _dietRecordsState.value =
                        DietRecordsState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "获取记录失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /**
     * 获取用户今天的饮食记录
     * @param userId 用户ID
     */
    fun getTodayDietRecords(userId: Int) {
        viewModelScope.launch {
            _todayDietRecordsState.value = DietRecordsState.Loading

            try {
                val response = RetrofitClient.apiService.getTodayDietRecords(userId)

                if (response.code == 200 && response.data != null) {
                    _todayDietRecordsState.value = DietRecordsState.Success(response.data)
                } else {
                    _todayDietRecordsState.value =
                            DietRecordsState.Error(response.message ?: "获取今日记录失败")
                }
            } catch (e: Exception) {
                _todayDietRecordsState.value =
                        DietRecordsState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    else -> "获取今日记录失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /** 重置添加记录状态 */
    fun resetAddDietRecordState() {
        _addDietRecordState.value = AddDietRecordState.Idle
    }
}
