package com.example.lifehub.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.StatsViewMode
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.components.CalorieChart
import com.example.lifehub.ui.components.CalorieSummaryCard
import com.example.lifehub.ui.components.ExerciseFrequencyChart
import com.example.lifehub.ui.components.NutrientRadarChart
import com.example.lifehub.ui.components.NutrientSummaryCard
import com.example.lifehub.ui.components.WeeklySummaryCard
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.viewmodel.DailyStatsUiState
import com.example.lifehub.viewmodel.ExerciseFrequencyUiState
import com.example.lifehub.viewmodel.NutrientStatsUiState
import com.example.lifehub.viewmodel.StatsViewModel
import com.example.lifehub.viewmodel.WeeklyStatsUiState

/**
 * Phase 17: 热量收支统计页面
 * 展示每日/每周热量摄入与消耗的图表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsPage(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel()
) {
    val viewMode by statsViewModel.viewMode.collectAsState()
    val selectedDate by statsViewModel.selectedDate.collectAsState()
    val dailyStatsState by statsViewModel.dailyStatsState.collectAsState()
    val weeklyStatsState by statsViewModel.weeklyStatsState.collectAsState()
    val chartDataPoints by statsViewModel.chartDataPoints.collectAsState()
    val nutrientStatsState by statsViewModel.nutrientStatsState.collectAsState()  // Phase 18
    val exerciseFrequencyState by statsViewModel.exerciseFrequencyState.collectAsState()  // Phase 51
    val frequencyPeriod by statsViewModel.frequencyPeriod.collectAsState()  // Phase 51

    // 获取当前用户ID
    val userId = UserSession.getUserId() ?: 1

    // 初始加载数据
    LaunchedEffect(viewMode, selectedDate) {
        statsViewModel.refresh(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据统计") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestGreen,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // 视图模式切换
            ViewModeSelector(
                currentMode = viewMode,
                onModeChange = { statsViewModel.setViewMode(it) }
            )

            // 日期导航
            DateNavigator(
                dateText = statsViewModel.getDateDisplayText(),
                canGoNext = statsViewModel.canGoNext(),
                onPrevious = { statsViewModel.goToPrevious() },
                onNext = { statsViewModel.goToNext() },
                onToday = { statsViewModel.goToToday() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 内容区域
            when (viewMode) {
                StatsViewMode.DAILY -> DailyStatsContent(
                    state = dailyStatsState,
                    nutrientState = nutrientStatsState,  // Phase 18
                    chartDataPoints = chartDataPoints,
                    onRetry = { statsViewModel.getDailyCalorieStats(userId) },
                    onRetryNutrient = { statsViewModel.getDailyNutrientStats(userId) }  // Phase 18
                )
                StatsViewMode.WEEKLY -> WeeklyStatsContent(
                    state = weeklyStatsState,
                    chartDataPoints = chartDataPoints,
                    onRetry = { statsViewModel.getWeeklyCalorieStats(userId) }
                )
            }

            // Phase 51: 运动频率分析区域
            ExerciseFrequencySection(
                state = exerciseFrequencyState,
                currentPeriod = frequencyPeriod,
                onPeriodChange = { period ->
                    statsViewModel.setFrequencyPeriod(period)
                    statsViewModel.getExerciseFrequency(userId, period)
                },
                onRetry = { statsViewModel.getExerciseFrequency(userId) }
            )
        }
    }
}

@Composable
private fun ViewModeSelector(
    currentMode: StatsViewMode,
    onModeChange: (StatsViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = currentMode == StatsViewMode.DAILY,
            onClick = { onModeChange(StatsViewMode.DAILY) },
            label = { Text("日统计") },
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = currentMode == StatsViewMode.WEEKLY,
            onClick = { onModeChange(StatsViewMode.WEEKLY) },
            label = { Text("周统计") }
        )
    }
}

@Composable
private fun DateNavigator(
    dateText: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一个")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onToday) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = "回到今天",
                    tint = ForestGreen
                )
            }
        }

        IconButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "下一个",
                tint = if (canGoNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
    }
}

@Composable
private fun DailyStatsContent(
    state: DailyStatsUiState,
    nutrientState: NutrientStatsUiState,  // Phase 18
    chartDataPoints: List<com.example.lifehub.data.ChartDataPoint>,
    onRetry: () -> Unit,
    onRetryNutrient: () -> Unit  // Phase 18
) {
    when (state) {
        is DailyStatsUiState.Idle -> {
            LoadingIndicator()
        }
        is DailyStatsUiState.Loading -> {
            LoadingIndicator()
        }
        is DailyStatsUiState.Success -> {
            val data = state.data
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 热量摘要卡片
                CalorieSummaryCard(
                    intakeCalories = data.intakeCalories,
                    burnCalories = data.burnCalories,
                    netCalories = data.netCalories
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 餐次分布图表
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "餐次分布",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CalorieChart(
                            dataPoints = chartDataPoints,
                            showBurn = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phase 18: 营养素雷达图区域
                NutrientStatsSection(
                    nutrientState = nutrientState,
                    onRetry = onRetryNutrient
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 详细信息
                DetailInfoCard(
                    items = listOf(
                        "餐次" to "${data.mealCount} 餐",
                        "运动次数" to "${data.exerciseCount} 次",
                        "运动时长" to "${data.exerciseDuration} 分钟"
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        is DailyStatsUiState.Error -> {
            ErrorContent(
                message = state.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun WeeklyStatsContent(
    state: WeeklyStatsUiState,
    chartDataPoints: List<com.example.lifehub.data.ChartDataPoint>,
    onRetry: () -> Unit
) {
    when (state) {
        is WeeklyStatsUiState.Idle -> {
            LoadingIndicator()
        }
        is WeeklyStatsUiState.Loading -> {
            LoadingIndicator()
        }
        is WeeklyStatsUiState.Success -> {
            val data = state.data
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 周统计摘要
                WeeklySummaryCard(
                    totalIntake = data.totalIntake,
                    totalBurn = data.totalBurn,
                    avgIntake = data.avgIntake,
                    avgBurn = data.avgBurn,
                    activeDays = data.activeDays
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 每日对比图表
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "每日热量对比",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CalorieChart(
                            dataPoints = chartDataPoints,
                            showBurn = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 详细信息
                DetailInfoCard(
                    items = listOf(
                        "总餐次" to "${data.totalMeals} 餐",
                        "总运动" to "${data.totalExercises} 次",
                        "活跃天数" to "${data.activeDays} / 7 天"
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        is WeeklyStatsUiState.Error -> {
            ErrorContent(
                message = state.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ForestGreen)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreen
            )
        ) {
            Text("重试")
        }
    }
}

@Composable
private fun DetailInfoCard(
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ============== Phase 18: 营养素统计区域 ==============

/**
 * Phase 18: 营养素统计展示区域
 * 包含营养素摘要卡片和雷达图
 */
@Composable
private fun NutrientStatsSection(
    nutrientState: NutrientStatsUiState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "营养素分析",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (nutrientState) {
                is NutrientStatsUiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
                is NutrientStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
                is NutrientStatsUiState.Success -> {
                    val nutrientData = nutrientState.data
                    
                    // 营养素摘要卡片
                    NutrientSummaryCard(stats = nutrientData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 营养素雷达图
                    Text(
                        text = "营养素比例（与膳食指南对比）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    NutrientRadarChart(
                        stats = nutrientData,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 显示建议信息
                    nutrientData.guidelinesComparison?.let { comparison ->
                        Spacer(modifier = Modifier.height(12.dp))
                        NutrientAdviceSection(comparison = comparison)
                    }
                }
                is NutrientStatsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = nutrientState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestGreen
                            )
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Phase 18: 营养素建议区域
 * 显示每个营养素的摄入建议
 */
@Composable
private fun NutrientAdviceSection(
    comparison: com.example.lifehub.data.GuidelinesComparison
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        comparison.protein?.let { protein ->
            if (protein.status != "normal") {
                NutrientAdviceItem(
                    nutrientName = "蛋白质",
                    status = protein.status,
                    message = protein.message
                )
            }
        }
        comparison.fat?.let { fat ->
            if (fat.status != "normal") {
                NutrientAdviceItem(
                    nutrientName = "脂肪",
                    status = fat.status,
                    message = fat.message
                )
            }
        }
        comparison.carbs?.let { carbs ->
            if (carbs.status != "normal") {
                NutrientAdviceItem(
                    nutrientName = "碳水化合物",
                    status = carbs.status,
                    message = carbs.message
                )
            }
        }
    }
}

/**
 * Phase 18: 单个营养素建议项
 */
@Composable
private fun NutrientAdviceItem(
    nutrientName: String,
    status: String,
    message: String
) {
    val statusColor = when (status) {
        "low" -> androidx.compose.ui.graphics.Color(0xFFFF9800)  // Orange
        "high" -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = statusColor)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.ifEmpty { 
                when (status) {
                    "low" -> "${nutrientName}摄入偏低，建议适当增加"
                    "high" -> "${nutrientName}摄入偏高，建议适当控制"
                    else -> "${nutrientName}摄入正常"
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = statusColor
        )
    }
}

// ============== Phase 51: 运动频率分析区域 ==============

/**
 * Phase 51: 运动频率分析展示区域
 * 包含周期切换、频率图表和建议
 */
@Composable
private fun ExerciseFrequencySection(
    state: ExerciseFrequencyUiState,
    currentPeriod: String,
    onPeriodChange: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 标题行 + 周期切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "运动频率分析",
                style = MaterialTheme.typography.titleMedium
            )

            Row {
                FilterChip(
                    selected = currentPeriod == "week",
                    onClick = { onPeriodChange("week") },
                    label = { Text("周") },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = currentPeriod == "month",
                    onClick = { onPeriodChange("month") },
                    label = { Text("月") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            is ExerciseFrequencyUiState.Idle,
            is ExerciseFrequencyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            }
            is ExerciseFrequencyUiState.Success -> {
                ExerciseFrequencyChart(data = state.data)
            }
            is ExerciseFrequencyUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text("重试")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
