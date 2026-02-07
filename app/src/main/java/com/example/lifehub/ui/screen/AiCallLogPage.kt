package com.example.lifehub.ui.screen

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.*
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.AiCallLogState
import com.example.lifehub.viewmodel.AiCallLogStatsState
import com.example.lifehub.viewmodel.UserViewModel

/**
 * Phase 56: AI调用日志查看页面
 * 展示用户的AI调用记录和统计信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCallLogPage(
        navController: NavController,
        viewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    UserSession.init(context)

    val userId = UserSession.getUserId()
    val logState by viewModel.aiCallLogState.collectAsState()
    val statsState by viewModel.aiCallLogStatsState.collectAsState()

    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        userId?.let {
            viewModel.getAiCallLogs(it)
            viewModel.getAiCallLogStats(it)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("AI调用记录", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = BackgroundBeige
                        )
                )
            }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundBeige)
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 统计卡片
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AiLogStatsCard(statsState)
            }

            // 过滤器
            item {
                AiLogFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterChanged = { filter ->
                            selectedFilter = filter
                            userId?.let {
                                viewModel.getAiCallLogs(it, callType = filter)
                            }
                        }
                )
            }

            // 日志列表
            when (val state = logState) {
                is AiCallLogState.Loading -> {
                    item {
                        Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ForestGreen)
                        }
                    }
                }
                is AiCallLogState.Success -> {
                    if (state.data.logs.isEmpty()) {
                        item {
                            EmptyLogPlaceholder()
                        }
                    } else {
                        item {
                            Text(
                                    "共 ${state.data.total} 条记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                            )
                        }
                        items(state.data.logs) { log ->
                            AiCallLogCard(log)
                        }
                    }
                }
                is AiCallLogState.Error -> {
                    item {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                else -> {}
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/** 统计卡片 */
@Composable
private fun AiLogStatsCard(statsState: AiCallLogStatsState) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    "调用统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (statsState) {
                is AiCallLogStatsState.Loading -> {
                    Box(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ForestGreen,
                                strokeWidth = 2.dp
                        )
                    }
                }
                is AiCallLogStatsState.Success -> {
                    val stats = statsState.data
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                                label = "总调用",
                                value = "${stats.totalCalls}",
                                icon = Icons.Filled.Analytics,
                                color = SkyBlue
                        )
                        StatItem(
                                label = "成功率",
                                value = "${(stats.successRate * 100).toInt()}%",
                                icon = Icons.Filled.CheckCircle,
                                color = SuccessGreen
                        )
                        StatItem(
                                label = "平均耗时",
                                value = "${stats.avgLatencyMs.toInt()}ms",
                                icon = Icons.Filled.Timer,
                                color = VitalOrange
                        )
                        StatItem(
                                label = "近7天",
                                value = "${stats.recent7daysCount}",
                                icon = Icons.Filled.DateRange,
                                color = LavenderPurple
                        )
                    }
                }
                is AiCallLogStatsState.Error -> {
                    Text(
                            "统计加载失败",
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }
        }
    }
}

/** 统计项 */
@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
                modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

/** 过滤器行 */
@Composable
private fun AiLogFilterRow(selectedFilter: String?, onFilterChanged: (String?) -> Unit) {
    val filters = listOf(
            null to "全部",
            "food_analysis" to "营养分析",
            "menu_recognition" to "菜单识别",
            "trip_generation" to "运动计划",
            "exercise_intent" to "意图提取"
    )

    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (type, label) ->
            val isSelected = selectedFilter == type
            FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(type) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen.copy(alpha = 0.15f),
                            selectedLabelColor = ForestGreenDark
                    )
            )
        }
    }
}

/** 单条日志卡片 */
@Composable
private fun AiCallLogCard(log: AiCallLogItem) {
    val typeLabel = AiCallTypeLabels.getLabel(log.callType)
    val statusColor = if (log.success) SuccessGreen else ErrorRed
    val statusIcon = if (log.success) Icons.Filled.CheckCircle else Icons.Filled.Error

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 顶部行：类型标签 + 状态
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                            modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            typeLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (log.latencyMs != null) {
                        Text(
                                "${log.latencyMs}ms",
                                fontSize = 12.sp,
                                color = TextTertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 模型名称
            Text(
                    "模型: ${log.modelName}",
                    fontSize = 12.sp,
                    color = TextSecondary
            )

            // 输入摘要
            if (!log.inputSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        "输入: ${log.inputSummary}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // 输出摘要
            if (!log.outputSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        "输出: ${log.outputSummary}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // 错误信息
            if (!log.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        "错误: ${log.errorMessage}",
                        fontSize = 12.sp,
                        color = ErrorRed,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // 时间
            if (!log.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                        log.createdAt,
                        fontSize = 11.sp,
                        color = TextTertiary
                )
            }
        }
    }
}

/** 空日志占位 */
@Composable
private fun EmptyLogPlaceholder() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    "暂无AI调用记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    "使用菜品分析或运动计划功能后，调用记录将在此显示",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
            )
        }
    }
}
