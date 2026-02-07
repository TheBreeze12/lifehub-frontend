package com.example.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifehub.data.ChartDataPoint
import com.example.lifehub.data.DailyCalorieStats
import com.example.lifehub.data.DailyNutrientStats
import com.example.lifehub.data.ExerciseFrequencyData
import com.example.lifehub.data.StatsViewMode
import com.example.lifehub.data.WeeklyCalorieStats
import com.example.lifehub.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Phase 17: 热量收支统计 ViewModel
 * 管理热量统计页面的状态和数据
 */

/** 每日统计UI状态 */
sealed class DailyStatsUiState {
    object Idle : DailyStatsUiState()
    object Loading : DailyStatsUiState()
    data class Success(val data: DailyCalorieStats) : DailyStatsUiState()
    data class Error(val message: String) : DailyStatsUiState()
}

/** 每周统计UI状态 */
sealed class WeeklyStatsUiState {
    object Idle : WeeklyStatsUiState()
    object Loading : WeeklyStatsUiState()
    data class Success(val data: WeeklyCalorieStats) : WeeklyStatsUiState()
    data class Error(val message: String) : WeeklyStatsUiState()
}

/** Phase 18: 营养素统计UI状态 */
sealed class NutrientStatsUiState {
    object Idle : NutrientStatsUiState()
    object Loading : NutrientStatsUiState()
    data class Success(val data: DailyNutrientStats) : NutrientStatsUiState()
    data class Error(val message: String) : NutrientStatsUiState()
}

/** Phase 51: 运动频率分析UI状态 */
sealed class ExerciseFrequencyUiState {
    object Idle : ExerciseFrequencyUiState()
    object Loading : ExerciseFrequencyUiState()
    data class Success(val data: ExerciseFrequencyData) : ExerciseFrequencyUiState()
    data class Error(val message: String) : ExerciseFrequencyUiState()
}

class StatsViewModel : ViewModel() {

    private val _dailyStatsState = MutableStateFlow<DailyStatsUiState>(DailyStatsUiState.Idle)
    val dailyStatsState: StateFlow<DailyStatsUiState> = _dailyStatsState.asStateFlow()

    private val _weeklyStatsState = MutableStateFlow<WeeklyStatsUiState>(WeeklyStatsUiState.Idle)
    val weeklyStatsState: StateFlow<WeeklyStatsUiState> = _weeklyStatsState.asStateFlow()

    private val _viewMode = MutableStateFlow(StatsViewMode.DAILY)
    val viewMode: StateFlow<StatsViewMode> = _viewMode.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _chartDataPoints = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartDataPoints: StateFlow<List<ChartDataPoint>> = _chartDataPoints.asStateFlow()

    // Phase 18: 营养素统计状态
    private val _nutrientStatsState = MutableStateFlow<NutrientStatsUiState>(NutrientStatsUiState.Idle)
    val nutrientStatsState: StateFlow<NutrientStatsUiState> = _nutrientStatsState.asStateFlow()

    // Phase 51: 运动频率分析状态
    private val _exerciseFrequencyState = MutableStateFlow<ExerciseFrequencyUiState>(ExerciseFrequencyUiState.Idle)
    val exerciseFrequencyState: StateFlow<ExerciseFrequencyUiState> = _exerciseFrequencyState.asStateFlow()

    private val _frequencyPeriod = MutableStateFlow("week")
    val frequencyPeriod: StateFlow<String> = _frequencyPeriod.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 获取每日热量统计
     * @param userId 用户ID
     * @param date 统计日期
     */
    fun getDailyCalorieStats(userId: Int, date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            _dailyStatsState.value = DailyStatsUiState.Loading

            try {
                val dateStr = date.format(dateFormatter)
                val response = RetrofitClient.apiService.getDailyCalorieStats(userId, dateStr)

                if (response.code == 200 && response.data != null) {
                    _dailyStatsState.value = DailyStatsUiState.Success(response.data)
                    // 更新图表数据点（单日显示餐次分布）
                    updateDailyChartData(response.data)
                } else {
                    _dailyStatsState.value = DailyStatsUiState.Error(
                        response.message ?: "获取每日统计失败"
                    )
                }
            } catch (e: Exception) {
                _dailyStatsState.value = DailyStatsUiState.Error(
                    when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "网络连接失败，请检查后端服务是否启动"
                        e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        else -> "获取统计失败：${e.message ?: "未知错误"}"
                    }
                )
            }
        }
    }

    /**
     * 获取每周热量统计
     * @param userId 用户ID
     * @param weekStart 周起始日期（周一）
     */
    fun getWeeklyCalorieStats(userId: Int, weekStart: LocalDate? = null) {
        viewModelScope.launch {
            _weeklyStatsState.value = WeeklyStatsUiState.Loading

            try {
                // 计算本周周一
                val monday = weekStart ?: _selectedDate.value.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                )
                val weekStartStr = monday.format(dateFormatter)
                
                val response = RetrofitClient.apiService.getWeeklyCalorieStats(userId, weekStartStr)

                if (response.code == 200 && response.data != null) {
                    _weeklyStatsState.value = WeeklyStatsUiState.Success(response.data)
                    // 更新图表数据点（周视图显示每日数据）
                    updateWeeklyChartData(response.data)
                } else {
                    _weeklyStatsState.value = WeeklyStatsUiState.Error(
                        response.message ?: "获取每周统计失败"
                    )
                }
            } catch (e: Exception) {
                _weeklyStatsState.value = WeeklyStatsUiState.Error(
                    when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "网络连接失败，请检查后端服务是否启动"
                        e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        else -> "获取统计失败：${e.message ?: "未知错误"}"
                    }
                )
            }
        }
    }

    /**
     * 切换视图模式（日/周）
     */
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            StatsViewMode.DAILY -> StatsViewMode.WEEKLY
            StatsViewMode.WEEKLY -> StatsViewMode.DAILY
        }
    }

    /**
     * 设置视图模式
     */
    fun setViewMode(mode: StatsViewMode) {
        _viewMode.value = mode
    }

    /**
     * 设置选中日期
     */
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * 前一天/前一周
     */
    fun goToPrevious() {
        _selectedDate.value = when (_viewMode.value) {
            StatsViewMode.DAILY -> _selectedDate.value.minusDays(1)
            StatsViewMode.WEEKLY -> _selectedDate.value.minusWeeks(1)
        }
    }

    /**
     * 后一天/后一周
     */
    fun goToNext() {
        val today = LocalDate.now()
        val newDate = when (_viewMode.value) {
            StatsViewMode.DAILY -> _selectedDate.value.plusDays(1)
            StatsViewMode.WEEKLY -> _selectedDate.value.plusWeeks(1)
        }
        // 不允许超过今天
        if (!newDate.isAfter(today)) {
            _selectedDate.value = newDate
        }
    }

    /**
     * 回到今天
     */
    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    /**
     * 更新每日图表数据（餐次分布）
     */
    private fun updateDailyChartData(stats: DailyCalorieStats) {
        val mealLabels = listOf("早餐" to "breakfast", "午餐" to "lunch", "晚餐" to "dinner", "加餐" to "snack")
        val points = mealLabels.map { (label, key) ->
            val intake = stats.mealBreakdown?.get(key)?.toFloat() ?: 0f
            ChartDataPoint(
                label = label,
                intake = intake,
                burn = 0f // 餐次分布不显示消耗
            )
        }
        _chartDataPoints.value = points
    }

    /**
     * 更新每周图表数据（每日数据）
     */
    private fun updateWeeklyChartData(stats: WeeklyCalorieStats) {
        val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val points = stats.dailyBreakdown.mapIndexed { index, breakdown ->
            ChartDataPoint(
                label = dayLabels.getOrElse(index) { "Day ${index + 1}" },
                intake = breakdown.intakeCalories.toFloat(),
                burn = breakdown.burnCalories.toFloat()
            )
        }
        _chartDataPoints.value = if (points.isNotEmpty()) points else {
            // 如果没有数据，显示空的7天
            dayLabels.map { ChartDataPoint(it, 0f, 0f) }
        }
    }

    /**
     * 刷新当前数据
     */
    fun refresh(userId: Int) {
        when (_viewMode.value) {
            StatsViewMode.DAILY -> {
                getDailyCalorieStats(userId)
                getDailyNutrientStats(userId)  // Phase 18: 同时获取营养素统计
                getExerciseFrequency(userId)   // Phase 51: 同时获取运动频率
            }
            StatsViewMode.WEEKLY -> {
                getWeeklyCalorieStats(userId)
                getExerciseFrequency(userId)   // Phase 51: 同时获取运动频率
            }
        }
    }

    // ============== Phase 18: 营养素统计 ==============

    /**
     * 获取每日营养素统计
     * @param userId 用户ID
     * @param date 统计日期
     */
    fun getDailyNutrientStats(userId: Int, date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            _nutrientStatsState.value = NutrientStatsUiState.Loading

            try {
                val dateStr = date.format(dateFormatter)
                val response = RetrofitClient.apiService.getDailyNutrientStats(userId, dateStr)

                if (response.code == 200 && response.data != null) {
                    _nutrientStatsState.value = NutrientStatsUiState.Success(response.data)
                } else {
                    _nutrientStatsState.value = NutrientStatsUiState.Error(
                        response.message ?: "获取营养素统计失败"
                    )
                }
            } catch (e: Exception) {
                _nutrientStatsState.value = NutrientStatsUiState.Error(
                    when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "网络连接失败，请检查后端服务是否启动"
                        e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        else -> "获取营养素统计失败：${e.message ?: "未知错误"}"
                    }
                )
            }
        }
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _dailyStatsState.value = DailyStatsUiState.Idle
        _weeklyStatsState.value = WeeklyStatsUiState.Idle
        _nutrientStatsState.value = NutrientStatsUiState.Idle  // Phase 18
        _exerciseFrequencyState.value = ExerciseFrequencyUiState.Idle  // Phase 51
        _chartDataPoints.value = emptyList()
    }

    // ============== Phase 51: 运动频率分析 ==============

    /**
     * 设置运动频率统计周期
     */
    fun setFrequencyPeriod(period: String) {
        _frequencyPeriod.value = period
    }

    /**
     * 获取运动频率分析
     * @param userId 用户ID
     * @param period 统计周期（week/month）
     */
    fun getExerciseFrequency(userId: Int, period: String = _frequencyPeriod.value) {
        viewModelScope.launch {
            _exerciseFrequencyState.value = ExerciseFrequencyUiState.Loading

            try {
                val response = RetrofitClient.apiService.getExerciseFrequency(userId, period)

                if (response.code == 200 && response.data != null) {
                    _exerciseFrequencyState.value = ExerciseFrequencyUiState.Success(response.data)
                } else {
                    _exerciseFrequencyState.value = ExerciseFrequencyUiState.Error(
                        response.message ?: "获取运动频率分析失败"
                    )
                }
            } catch (e: Exception) {
                _exerciseFrequencyState.value = ExerciseFrequencyUiState.Error(
                    when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "网络连接失败，请检查后端服务是否启动"
                        e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        else -> "获取运动频率分析失败：${e.message ?: "未知错误"}"
                    }
                )
            }
        }
    }

    /**
     * 获取格式化的日期显示文本
     */
    fun getDateDisplayText(): String {
        return when (_viewMode.value) {
            StatsViewMode.DAILY -> {
                val date = _selectedDate.value
                val today = LocalDate.now()
                when {
                    date.isEqual(today) -> "今天"
                    date.isEqual(today.minusDays(1)) -> "昨天"
                    else -> date.format(DateTimeFormatter.ofPattern("MM月dd日"))
                }
            }
            StatsViewMode.WEEKLY -> {
                val monday = _selectedDate.value.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                )
                val sunday = monday.plusDays(6)
                "${monday.format(DateTimeFormatter.ofPattern("MM/dd"))} - ${sunday.format(DateTimeFormatter.ofPattern("MM/dd"))}"
            }
        }
    }

    /**
     * 检查是否可以前往下一个日期/周
     */
    fun canGoNext(): Boolean {
        val today = LocalDate.now()
        return when (_viewMode.value) {
            StatsViewMode.DAILY -> _selectedDate.value.isBefore(today)
            StatsViewMode.WEEKLY -> {
                val nextMonday = _selectedDate.value.plusWeeks(1).with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                )
                !nextMonday.isAfter(today)
            }
        }
    }
}
