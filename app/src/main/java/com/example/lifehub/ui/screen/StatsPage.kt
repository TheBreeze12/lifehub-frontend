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
import com.example.lifehub.ui.components.WeeklySummaryCard
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.viewmodel.DailyStatsUiState
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

    // 获取当前用户ID
    val userId = UserSession.userId ?: 1

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
                    chartDataPoints = chartDataPoints,
                    onRetry = { statsViewModel.getDailyCalorieStats(userId) }
                )
                StatsViewMode.WEEKLY -> WeeklyStatsContent(
                    state = weeklyStatsState,
                    chartDataPoints = chartDataPoints,
                    onRetry = { statsViewModel.getWeeklyCalorieStats(userId) }
                )
            }
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
    chartDataPoints: List<com.example.lifehub.data.ChartDataPoint>,
    onRetry: () -> Unit
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
