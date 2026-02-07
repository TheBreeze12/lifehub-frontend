package com.example.lifehub.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.*
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.GoalProgressState
import com.example.lifehub.viewmodel.UserViewModel

/**
 * Phase 48: 健康目标达成情况页面
 * 展示多维度达成率、综合得分、个性化建议、连续记录天数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalProgressPage(
    navController: NavController,
    viewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    com.example.lifehub.data.UserSession.init(context)

    val userId = UserSession.getUserId()
    val goalProgressState by viewModel.goalProgressState.collectAsState()

    // 当前选择的统计天数
    var selectedPeriod by remember { mutableIntStateOf(7) }

    // 加载数据
    LaunchedEffect(userId, selectedPeriod) {
        userId?.let { viewModel.getGoalProgress(it, selectedPeriod) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健康目标达成", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundBeige
                )
            )
        },
        containerColor = BackgroundBeige
    ) { innerPadding ->

        if (userId == null) {
            // 未登录提示
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("请先登录后查看健康目标达成情况", color = TextSecondary, fontSize = 16.sp)
            }
            return@Scaffold
        }

        when (val state = goalProgressState) {
            is GoalProgressState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ForestGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("加载中...", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            is GoalProgressState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.getGoalProgress(userId, selectedPeriod) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }

            is GoalProgressState.Success -> {
                GoalProgressContent(
                    data = state.data,
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    onRefresh = { viewModel.getGoalProgress(userId, selectedPeriod) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is GoalProgressState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            }
        }
    }
}

/** 健康目标达成内容主体 */
@Composable
private fun GoalProgressContent(
    data: GoalProgressData,
    selectedPeriod: Int,
    onPeriodChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 时间段选择器
        item {
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = onPeriodChange
            )
        }

        // 综合得分卡片
        item {
            OverallScoreCard(
                score = data.overallScore,
                status = data.overallStatus,
                healthGoalLabel = data.healthGoalLabel,
                streakDays = data.streakDays,
                startDate = data.startDate,
                endDate = data.endDate
            )
        }

        // 连续记录天数卡片
        item {
            StreakDaysCard(streakDays = data.streakDays)
        }

        // 各维度达成率
        item {
            Text(
                text = "各维度达成详情",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(data.dimensions) { dimension ->
            DimensionCard(dimension = dimension)
        }

        // 个性化建议
        if (data.suggestions.isNotEmpty()) {
            item {
                SuggestionsCard(suggestions = data.suggestions)
            }
        }

        // 底部间距
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** 时间段选择器 */
@Composable
private fun PeriodSelector(
    selectedPeriod: Int,
    onPeriodChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalStatusUtil.periodOptions.forEach { days ->
            val isSelected = days == selectedPeriod
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodChange(days) },
                label = {
                    Text(
                        text = GoalStatusUtil.getPeriodLabel(days),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen.copy(alpha = 0.15f),
                    selectedLabelColor = ForestGreenDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = ForestGreen,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

/** 综合得分卡片 */
@Composable
private fun OverallScoreCard(
    score: Double,
    status: String,
    healthGoalLabel: String,
    streakDays: Int,
    startDate: String,
    endDate: String
) {
    val statusColor = getStatusColor(status)
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "scoreAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.12f),
                            CardBackground
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 健康目标标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "目标：$healthGoalLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 综合得分圆形指示器
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (animatedScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.1f),
                        strokeWidth = 10.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${animatedScore.toInt()}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            text = GoalStatusUtil.getStatusLabel(status),
                            fontSize = 13.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 统计区间
                Text(
                    text = "$startDate ~ $endDate",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

/** 连续记录天数卡片 */
@Composable
private fun StreakDaysCard(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 火焰图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VitalOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = VitalOrange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连续记录",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$streakDays",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalOrange
                    )
                    Text(
                        text = " 天",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // 鼓励文案
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (streakDays >= 7) SuccessGreen.copy(alpha = 0.12f)
                       else VitalOrange.copy(alpha = 0.12f)
            ) {
                Text(
                    text = getStreakEncouragement(streakDays),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (streakDays >= 7) SuccessGreen else VitalOrange,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** 单个维度达成卡片 */
@Composable
private fun DimensionCard(dimension: GoalDimension) {
    val statusColor = getStatusColor(dimension.status)
    val animatedProgress by animateFloatAsState(
        targetValue = dimension.progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "dimensionProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 维度名称和得分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 状态指示点
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = dimension.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // 得分标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${dimension.score.toInt()}分",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 当前值 vs 目标值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "当前：${"%.1f".format(dimension.currentValue)} ${dimension.unit}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "目标：${"%.1f".format(dimension.targetValue)} ${dimension.unit}",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            // 描述
            if (dimension.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dimension.description,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/** 个性化建议卡片 */
@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = WarningYellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "个性化建议",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = suggestion,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/** 根据状态获取颜色 */
private fun getStatusColor(status: String): Color = when (status) {
    "excellent" -> SuccessGreen
    "good" -> ForestGreen
    "fair" -> WarningYellow
    "poor" -> ErrorRed
    else -> TextSecondary
}

/** 根据连续天数获取鼓励文案 */
private fun getStreakEncouragement(days: Int): String = when {
    days >= 30 -> "坚持一个月！"
    days >= 14 -> "两周达人！"
    days >= 7 -> "一周达成！"
    days >= 3 -> "继续加油"
    days >= 1 -> "好的开始"
    else -> "开始记录吧"
}
