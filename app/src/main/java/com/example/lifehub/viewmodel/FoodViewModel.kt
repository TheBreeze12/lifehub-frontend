package com.example.lifehub.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.ai.OcrResult
import com.example.lifehub.ai.OcrService
import com.example.lifehub.ai.OcrServiceState
import com.example.lifehub.data.AddDietRecordRequest
import com.example.lifehub.data.AfterMealData
import com.example.lifehub.data.BeforeMealData
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.DishItem
import com.example.lifehub.data.FoodData
import com.example.lifehub.data.FoodRequest
import com.example.lifehub.data.MealComparisonRecord
import com.example.lifehub.data.RecommendationResultData
import com.example.lifehub.data.UpdateDietRecordRequest
import com.example.lifehub.network.RetrofitClient
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

/** 识别来源：端侧OCR 或 云端AI */
enum class RecognitionSource(val displayName: String) {
    EDGE("端侧识别"),
    CLOUD("云端识别")
}

/** 菜单识别UI状态 */
sealed class MenuRecognitionState {
    object Idle : MenuRecognitionState()
    object Loading : MenuRecognitionState()
    data class Success(
        val dishes: List<DishItem>,
        val source: RecognitionSource = RecognitionSource.CLOUD
    ) : MenuRecognitionState()
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

/** 更新饮食记录UI状态 */
sealed class UpdateDietRecordState {
    object Idle : UpdateDietRecordState()
    object Loading : UpdateDietRecordState()
    data class Success(val message: String) : UpdateDietRecordState()
    data class Error(val message: String) : UpdateDietRecordState()
}

/** 删除饮食记录UI状态 */
sealed class DeleteDietRecordState {
    object Idle : DeleteDietRecordState()
    object Loading : DeleteDietRecordState()
    data class Success(val message: String) : DeleteDietRecordState()
    data class Error(val message: String) : DeleteDietRecordState()
}

/** 餐前图片上传UI状态 */
sealed class BeforeMealUploadState {
    object Idle : BeforeMealUploadState()
    object Loading : BeforeMealUploadState()
    data class Success(val data: BeforeMealData) : BeforeMealUploadState()
    data class Error(val message: String) : BeforeMealUploadState()
}

/** 餐后图片上传UI状态 */
sealed class AfterMealUploadState {
    object Idle : AfterMealUploadState()
    object Loading : AfterMealUploadState()
    data class Success(val data: AfterMealData) : AfterMealUploadState()
    data class Error(val message: String) : AfterMealUploadState()
}

/** 个性化菜品推荐UI状态 */
sealed class RecommendationState {
    object Idle : RecommendationState()
    object Loading : RecommendationState()
    data class Success(val data: RecommendationResultData) : RecommendationState()
    data class Error(val message: String) : RecommendationState()
}

/** 菜品查询ViewModel */
class FoodViewModel : ViewModel() {

    companion object {
        private const val TAG = "FoodViewModel"

        /** 端侧OCR识别最少需要提取到的菜名数量 */
        const val MIN_EDGE_OCR_DISHES = 1

        /** 菜名最短长度 */
        const val MIN_DISH_NAME_LENGTH = 2

        /** 菜名最长长度 */
        const val MAX_DISH_NAME_LENGTH = 20

        /** 需要过滤的非菜品文本（菜单分类标题等） */
        private val NON_DISH_PATTERNS = setOf(
            "热菜", "凉菜", "主食", "汤类", "饮品", "甜品", "小吃",
            "推荐", "招牌", "新品", "特价", "套餐", "合计", "总计",
            "菜单", "价格", "价目", "电话", "地址", "订餐", "外卖",
            "备注", "温馨提示", "欢迎光临", "谢谢惠顾", "扫码点餐"
        )

        /**
         * 从OCR识别的文本行中提取菜名
         * 过滤规则：
         * 1. 过滤掉太短或太长的文本
         * 2. 过滤掉纯数字/价格
         * 3. 过滤掉菜单分类标题
         * 4. 去重
         */
        fun extractDishNames(ocrTexts: List<String>): List<String> {
            return ocrTexts
                .map { it.trim() }
                .filter { text ->
                    text.length in MIN_DISH_NAME_LENGTH..MAX_DISH_NAME_LENGTH &&
                    !text.matches(Regex("^[\\d.¥￥元/\\s]+$")) &&
                    !NON_DISH_PATTERNS.any { pattern ->
                        text.equals(pattern, ignoreCase = true)
                    }
                }
                .distinct()
        }
    }

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

    private val _updateDietRecordState = MutableStateFlow<UpdateDietRecordState>(UpdateDietRecordState.Idle)
    val updateDietRecordState: StateFlow<UpdateDietRecordState> = _updateDietRecordState.asStateFlow()

    private val _deleteDietRecordState = MutableStateFlow<DeleteDietRecordState>(DeleteDietRecordState.Idle)
    val deleteDietRecordState: StateFlow<DeleteDietRecordState> = _deleteDietRecordState.asStateFlow()

    private val _beforeMealUploadState = MutableStateFlow<BeforeMealUploadState>(BeforeMealUploadState.Idle)
    val beforeMealUploadState: StateFlow<BeforeMealUploadState> = _beforeMealUploadState.asStateFlow()

    private val _afterMealUploadState = MutableStateFlow<AfterMealUploadState>(AfterMealUploadState.Idle)
    val afterMealUploadState: StateFlow<AfterMealUploadState> = _afterMealUploadState.asStateFlow()

    private val _currentComparisonRecord = MutableStateFlow<MealComparisonRecord?>(null)
    val currentComparisonRecord: StateFlow<MealComparisonRecord?> = _currentComparisonRecord.asStateFlow()

    // Phase 42: 个性化菜品推荐
    private val _recommendationState = MutableStateFlow<RecommendationState>(RecommendationState.Idle)
    val recommendationState: StateFlow<RecommendationState> = _recommendationState.asStateFlow()

    /** 端侧OCR服务实例（懒初始化） */
    private var ocrService: OcrService? = null

    /**
     * 端侧OCR优先、云端兜底的菜单识别
     * 流程：
     * 1. 尝试端侧OCR识别图片中的文字
     * 2. 从OCR文本中提取菜名
     * 3. 对每个菜名调用云端API获取营养数据
     * 4. 若端侧OCR失败或提取菜名不足，回退到云端图片识别
     *
     * @param imageUri 图片URI
     * @param context Android Context
     * @param userId 用户ID（可选）
     */
    fun recognizeMenuWithEdgeFirst(imageUri: Uri, context: android.content.Context, userId: Int? = null) {
        viewModelScope.launch {
            _recognitionState.value = MenuRecognitionState.Loading

            try {
                // Step 1: 尝试端侧OCR
                val edgeResult = tryEdgeOcr(imageUri, context)

                if (edgeResult != null) {
                    val dishNames = extractDishNames(edgeResult.texts.map { it.text })
                    Log.d(TAG, "端侧OCR提取到${dishNames.size}个菜名: $dishNames")

                    if (dishNames.size >= MIN_EDGE_OCR_DISHES) {
                        // Step 2: 端侧OCR成功，为每个菜名获取营养数据
                        val dishes = fetchNutritionForDishes(dishNames)

                        if (dishes.isNotEmpty()) {
                            Log.d(TAG, "端侧OCR成功，获取到${dishes.size}道菜品营养数据")
                            _recognitionState.value = MenuRecognitionState.Success(
                                dishes = dishes,
                                source = RecognitionSource.EDGE
                            )
                            return@launch
                        }
                        Log.d(TAG, "端侧OCR提取到菜名但获取营养数据失败，回退到云端")
                    } else {
                        Log.d(TAG, "端侧OCR提取菜名不足(${dishNames.size}个)，回退到云端")
                    }
                } else {
                    Log.d(TAG, "端侧OCR失败，回退到云端")
                }

                // Step 3: 回退到云端识别
                recognizeMenuCloud(imageUri, context, userId)
            } catch (e: Exception) {
                Log.e(TAG, "端侧OCR异常，回退到云端", e)
                try {
                    recognizeMenuCloud(imageUri, context, userId)
                } catch (cloudError: Exception) {
                    _recognitionState.value = MenuRecognitionState.Error(
                        "识别失败：${cloudError.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    /**
     * 尝试端侧OCR识别
     * @return OcrResult 或 null（初始化/识别失败时）
     */
    private suspend fun tryEdgeOcr(imageUri: Uri, context: android.content.Context): OcrResult? {
        return try {
            // 懒初始化OCR服务
            if (ocrService == null) {
                ocrService = OcrService(context.applicationContext)
            }
            val service = ocrService!!

            if (service.state is OcrServiceState.NotReady) {
                Log.d(TAG, "正在初始化端侧OCR...")
                service.initialize()
            }

            if (service.state !is OcrServiceState.Ready) {
                Log.w(TAG, "端侧OCR服务状态异常: ${service.state}")
                return null
            }

            // 加载Bitmap
            val bitmap = loadBitmapFromUri(imageUri, context) ?: return null

            val result = service.recognizeText(bitmap)
            bitmap.recycle()

            Log.d(TAG, "端侧OCR完成: ${result.texts.size}行文本, 耗时${result.totalTimeMs}ms")

            if (result.isSuccess && result.texts.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.e(TAG, "端侧OCR异常: ${e.message}", e)
            null
        }
    }

    /**
     * 云端菜单图片识别（原有逻辑提取为独立方法）
     */
    private suspend fun recognizeMenuCloud(
        imageUri: Uri,
        context: android.content.Context,
        userId: Int? = null
    ) {
        val imagePart = createImagePart(imageUri, context)
            ?: run {
                _recognitionState.value = MenuRecognitionState.Error("无法读取图片文件")
                return
            }

        val userIdBody = userId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = RetrofitClient.apiService.recognizeMenu(imagePart, userIdBody)

        if (response.code == 200 && response.data?.dishes != null) {
            _recognitionState.value = MenuRecognitionState.Success(
                dishes = response.data.dishes,
                source = RecognitionSource.CLOUD
            )
        } else {
            _recognitionState.value = MenuRecognitionState.Error(
                response.message ?: "云端识别失败，请稍后重试"
            )
        }
    }

    /**
     * 批量获取菜品营养数据（并发请求）
     * @param dishNames 菜名列表
     * @return 带营养数据的DishItem列表
     */
    private suspend fun fetchNutritionForDishes(dishNames: List<String>): List<DishItem> {
        return supervisorScope {
            dishNames.map { name ->
                async {
                    try {
                        val response = RetrofitClient.apiService.analyzeFoodNutrition(
                            FoodRequest(foodName = name)
                        )
                        if (response.success && response.data != null) {
                            DishItem(
                                name = response.data.name,
                                calories = response.data.calories,
                                protein = response.data.protein,
                                fat = response.data.fat,
                                carbs = response.data.carbs,
                                isRecommended = true,
                                reason = response.data.recommendation,
                                cookingMethodComparisons = response.data.cookingMethodComparisons
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.w(TAG, "获取菜品[$name]营养数据失败: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * 从URI加载Bitmap
     */
    private fun loadBitmapFromUri(uri: Uri, context: android.content.Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: ${e.message}", e)
            null
        }
    }

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

    /**
     * 更新饮食记录
     * @param recordId 记录ID
     * @param userId 用户ID
     * @param foodName 菜品名称（可选）
     * @param calories 热量（可选）
     * @param protein 蛋白质（可选）
     * @param fat 脂肪（可选）
     * @param carbs 碳水化合物（可选）
     * @param mealType 餐次（可选）
     * @param recordDate 记录日期（可选）
     */
    fun updateDietRecord(
            recordId: Int,
            userId: Int,
            foodName: String? = null,
            calories: Double? = null,
            protein: Double? = null,
            fat: Double? = null,
            carbs: Double? = null,
            mealType: String? = null,
            recordDate: String? = null
    ) {
        viewModelScope.launch {
            _updateDietRecordState.value = UpdateDietRecordState.Loading

            try {
                val request = UpdateDietRecordRequest(
                        userId = userId,
                        foodName = foodName,
                        calories = calories,
                        protein = protein,
                        fat = fat,
                        carbs = carbs,
                        mealType = mealType,
                        recordDate = recordDate
                )

                val response = RetrofitClient.apiService.updateDietRecord(recordId, request)

                if (response.code == 200) {
                    _updateDietRecordState.value =
                            UpdateDietRecordState.Success(response.message ?: "更新成功")
                } else {
                    _updateDietRecordState.value =
                            UpdateDietRecordState.Error(response.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _updateDietRecordState.value =
                        UpdateDietRecordState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    e.message?.contains("403") == true -> "无权操作此记录"
                                    e.message?.contains("404") == true -> "记录不存在"
                                    else -> "更新失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /**
     * 删除饮食记录
     * @param recordId 记录ID
     * @param userId 用户ID
     */
    fun deleteDietRecord(recordId: Int, userId: Int) {
        viewModelScope.launch {
            _deleteDietRecordState.value = DeleteDietRecordState.Loading

            try {
                val response = RetrofitClient.apiService.deleteDietRecord(recordId, userId)

                if (response.code == 200) {
                    _deleteDietRecordState.value =
                            DeleteDietRecordState.Success(response.message ?: "删除成功")
                } else {
                    _deleteDietRecordState.value =
                            DeleteDietRecordState.Error(response.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _deleteDietRecordState.value =
                        DeleteDietRecordState.Error(
                                when {
                                    e.message?.contains("Unable to resolve host") == true ->
                                            "网络连接失败，请检查后端服务是否启动"
                                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                                    e.message?.contains("403") == true -> "无权操作此记录"
                                    e.message?.contains("404") == true -> "记录不存在"
                                    else -> "删除失败：${e.message ?: "未知错误"}"
                                }
                        )
            }
        }
    }

    /** 重置更新记录状态 */
    fun resetUpdateDietRecordState() {
        _updateDietRecordState.value = UpdateDietRecordState.Idle
    }

    /** 重置删除记录状态 */
    fun resetDeleteDietRecordState() {
        _deleteDietRecordState.value = DeleteDietRecordState.Idle
    }

    // ==================== Phase 42: 个性化菜品推荐 ====================

    /**
     * 获取个性化菜品推荐
     * @param userId 用户ID
     * @param mealType 餐次（breakfast/lunch/dinner/snack）
     * @param limit 返回推荐数量
     */
    fun getFoodRecommendations(userId: Int, mealType: String = "lunch", limit: Int = 5) {
        viewModelScope.launch {
            _recommendationState.value = RecommendationState.Loading

            try {
                val response = RetrofitClient.apiService.getFoodRecommendations(
                    userId = userId,
                    mealType = mealType,
                    limit = limit
                )

                if (response.code == 200 && response.data != null) {
                    _recommendationState.value = RecommendationState.Success(response.data)
                } else {
                    _recommendationState.value = RecommendationState.Error(
                        response.message ?: "获取推荐失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取菜品推荐失败", e)
                _recommendationState.value = RecommendationState.Error(
                    when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "网络连接失败，请检查后端服务是否启动"
                        e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        e.message?.contains("404") == true -> "用户不存在，请先登录"
                        else -> "获取推荐失败：${e.message ?: "未知错误"}"
                    }
                )
            }
        }
    }

    /** 重置推荐状态 */
    fun resetRecommendationState() {
        _recommendationState.value = RecommendationState.Idle
    }

    // ==================== 餐前餐后对比功能 ====================

    /**
     * 上传餐前图片
     * @param imageUri 图片URI
     * @param context Android Context（用于读取文件）
     * @param userId 用户ID
     */
    fun uploadBeforeMealImage(imageUri: Uri, context: android.content.Context, userId: Int) {
        viewModelScope.launch {
            _beforeMealUploadState.value = BeforeMealUploadState.Loading

            try {
                val imagePart = createImagePart(imageUri, context)
                        ?: run {
                            _beforeMealUploadState.value = BeforeMealUploadState.Error("无法读取图片文件")
                            return@launch
                        }

                val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val response = RetrofitClient.apiService.uploadBeforeMealImage(imagePart, userIdBody)

                if (response.code == 200 && response.data != null) {
                    _beforeMealUploadState.value = BeforeMealUploadState.Success(response.data)
                    // 保存当前对比记录
                    _currentComparisonRecord.value = MealComparisonRecord(
                            comparisonId = response.data.comparisonId,
                            beforeImageUrl = response.data.beforeImageUrl,
                            beforeFeatures = response.data.beforeFeatures,
                            originalCalories = response.data.beforeFeatures?.totalEstimatedCalories,
                            status = response.data.status ?: "pending_after"
                    )
                } else {
                    _beforeMealUploadState.value = BeforeMealUploadState.Error(
                            response.message ?: "餐前图片上传失败"
                    )
                }
            } catch (e: Exception) {
                _beforeMealUploadState.value = BeforeMealUploadState.Error(
                        when {
                            e.message?.contains("Unable to resolve host") == true ->
                                    "网络连接失败，请检查后端服务是否启动"
                            e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                            else -> "上传失败：${e.message ?: "未知错误"}"
                        }
                )
            }
        }
    }

    /**
     * 上传餐后图片并计算净摄入
     * @param imageUri 图片URI
     * @param context Android Context（用于读取文件）
     * @param comparisonId 对比记录ID（餐前上传时返回）
     */
    fun uploadAfterMealImage(imageUri: Uri, context: android.content.Context, comparisonId: Int) {
        viewModelScope.launch {
            _afterMealUploadState.value = AfterMealUploadState.Loading

            try {
                val imagePart = createImagePart(imageUri, context)
                        ?: run {
                            _afterMealUploadState.value = AfterMealUploadState.Error("无法读取图片文件")
                            return@launch
                        }

                val response = RetrofitClient.apiService.uploadAfterMealImage(comparisonId, imagePart)

                if (response.code == 200 && response.data != null) {
                    _afterMealUploadState.value = AfterMealUploadState.Success(response.data)
                    // 更新当前对比记录
                    _currentComparisonRecord.value = _currentComparisonRecord.value?.copy(
                            afterImageUrl = response.data.afterImageUrl,
                            consumptionRatio = response.data.consumptionRatio,
                            netCalories = response.data.netCalories,
                            status = response.data.status ?: "completed"
                    )
                } else {
                    _afterMealUploadState.value = AfterMealUploadState.Error(
                            response.message ?: "餐后图片上传失败"
                    )
                }
            } catch (e: Exception) {
                _afterMealUploadState.value = AfterMealUploadState.Error(
                        when {
                            e.message?.contains("Unable to resolve host") == true ->
                                    "网络连接失败，请检查后端服务是否启动"
                            e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                            e.message?.contains("404") == true -> "对比记录不存在"
                            e.message?.contains("400") == true -> "该对比记录已完成或状态异常"
                            else -> "上传失败：${e.message ?: "未知错误"}"
                        }
                )
            }
        }
    }

    /** 重置餐前上传状态 */
    fun resetBeforeMealUploadState() {
        _beforeMealUploadState.value = BeforeMealUploadState.Idle
    }

    /** 重置餐后上传状态 */
    fun resetAfterMealUploadState() {
        _afterMealUploadState.value = AfterMealUploadState.Idle
    }

    /** 重置所有餐前餐后对比状态 */
    fun resetMealComparisonState() {
        _beforeMealUploadState.value = BeforeMealUploadState.Idle
        _afterMealUploadState.value = AfterMealUploadState.Idle
        _currentComparisonRecord.value = null
    }
}
