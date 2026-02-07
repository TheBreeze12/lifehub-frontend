package com.example.lifehub.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.*
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.ExerciseViewModel

/**
 * 运动历史记录页面 - Phase 49
 *
 * 功能：
 * - 展示运动历史记录列表（按日期倒序）
 * - 支持按日期筛选
 * - 支持按运动类型筛选
 * - 点击查看单条运动详情（轨迹、消耗、配速等）
 * - 从个人中心入口进入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryPage(
    navController: NavController,
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    UserSession.init(context)

    val userId = UserSession.getUserId()
    val historyState by exerciseViewModel.historyState.collectAsState()
    val detailState by exerciseViewModel.detailState.collectAsState()

    // 筛选条件
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // 加载运动历史记录
    LaunchedEffect(userId, selectedDate, selectedType) {
        userId?.let {
            exerciseViewModel.loadExerciseHistory(
                userId = it,
                exerciseDate = selectedDate,
                exerciseType = selectedType
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运动历史", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 筛选按钮
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "筛选",
                            tint = if (selectedDate != null || selectedType != null)
                                ForestGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundBeige
                )
            )
        },
        containerColor = BackgroundBeige
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 筛选面板
            AnimatedVisibility(
                visible = showFilterPanel,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterPanel(
                    selectedDate = selectedDate,
                    selectedType = selectedType,
                    onDateSelected = { date ->
                        selectedDate = date
                    },
                    onTypeSelected = { type ->
                        selectedType = type
                    },
                    onClearFilters = {
                        selectedDate = null
                        selectedType = null
                    }
                )
            }

            // 内容区域
            when (val state = historyState) {
                is ExerciseHistoryState.Idle -> {
                    if (userId == null) {
                        EmptyContent("请先登录后查看运动历史记录")
                    }
                }
                is ExerciseHistoryState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
                is ExerciseHistoryState.Success -> {
                    if (state.records.isEmpty()) {
                        EmptyContent(
                            if (selectedDate != null || selectedType != null)
                                "没有符合筛选条件的运动记录"
                            else
                                "暂无运动记录\n完成运动后记录会显示在这里"
                        )
                    } else {
                        ExerciseHistoryList(
                            records = state.records,
                            total = state.total,
                            onRecordClick = { record ->
                                userId?.let {
                                    exerciseViewModel.loadExerciseDetail(record.id, it)
                                    showDetailDialog = true
                                }
                            }
                        )
                    }
                }
                is ExerciseHistoryState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            userId?.let {
                                exerciseViewModel.loadExerciseHistory(
                                    userId = it,
                                    exerciseDate = selectedDate,
                                    exerciseType = selectedType
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // 详情弹窗
    if (showDetailDialog) {
        ExerciseDetailDialog(
            detailState = detailState,
            onDismiss = {
                showDetailDialog = false
                exerciseViewModel.resetDetailState()
            }
        )
    }
}

/**
 * 筛选面板 - 支持日期和运动类型筛选
 */
@Composable
private fun FilterPanel(
    selectedDate: String?,
    selectedType: String?,
    onDateSelected: (String?) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    val exerciseTypes = listOf(
        null to "全部",
        "walking" to "散步",
        "running" to "跑步",
        "cycling" to "骑行",
        "jogging" to "慢跑",
        "hiking" to "徒步",
        "swimming" to "游泳",
        "gym" to "健身房",
        "indoor" to "室内",
        "outdoor" to "户外"
    )

    // 日期快捷筛选
    val dateOptions = remember {
        val today = java.time.LocalDate.now()
        listOf(
            null to "全部日期",
            today.toString() to "今天",
            today.minusDays(1).toString() to "昨天",
            today.minusDays(7).toString() to "最近7天"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 日期筛选
            Text(
                text = "日期筛选",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateOptions.forEach { (dateValue, label) ->
                    FilterChip(
                        selected = selectedDate == dateValue,
                        onClick = { onDateSelected(dateValue) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen.copy(alpha = 0.2f),
                            selectedLabelColor = ForestGreenDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 运动类型筛选
            Text(
                text = "运动类型",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 使用FlowRow布局（简化为多行Row）
            val chunkedTypes = exerciseTypes.chunked(5)
            chunkedTypes.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (typeValue, label) ->
                        FilterChip(
                            selected = selectedType == typeValue,
                            onClick = { onTypeSelected(typeValue) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VitalOrange.copy(alpha = 0.2f),
                                selectedLabelColor = VitalOrange
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 清除按钮
            if (selectedDate != null || selectedType != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清除筛选", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * 运动历史记录列表
 */
@Composable
private fun ExerciseHistoryList(
    records: List<ExerciseRecordResponseData>,
    total: Int,
    onRecordClick: (ExerciseRecordResponseData) -> Unit
) {
    // 按日期分组
    val groupedRecords = records.groupBy { it.exerciseDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 统计摘要
        item {
            SummaryCard(records = records, total = total)
        }

        // 按日期分组展示
        groupedRecords.forEach { (date, dayRecords) ->
            item {
                // 日期标题
                Text(
                    text = formatDateHeader(date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )
            }

            items(dayRecords, key = { it.id }) { record ->
                ExerciseRecordCard(
                    record = record,
                    onClick = { onRecordClick(record) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 统计摘要卡片
 */
@Composable
private fun SummaryCard(records: List<ExerciseRecordResponseData>, total: Int) {
    val totalCalories = records.sumOf { it.actualCalories }
    val totalDuration = records.sumOf { it.actualDuration }
    val totalDistance = records.sumOf { it.distance ?: 0.0 }

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
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ForestGreen.copy(alpha = 0.15f),
                            ForestGreenLight.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    value = total.toString(),
                    label = "运动次数",
                    color = ForestGreen
                )
                SummaryItem(
                    value = String.format("%.0f", totalCalories),
                    label = "总消耗(kcal)",
                    color = VitalOrange
                )
                SummaryItem(
                    value = "${totalDuration}",
                    label = "总时长(分钟)",
                    color = SkyBlue
                )
                SummaryItem(
                    value = ExerciseTrackingUtils.formatDistance(totalDistance),
                    label = "总距离",
                    color = CoralPink
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

/**
 * 单条运动记录卡片
 */
@Composable
private fun ExerciseRecordCard(
    record: ExerciseRecordResponseData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 运动类型图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ForestGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ExerciseTypeUtils.getTypeEmoji(record.exerciseType),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 运动信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ExerciseTypeUtils.getTypeLabel(record.exerciseType),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    // 达成率标签
                    record.caloriesAchievement?.let { achievement ->
                        val achievementColor = when {
                            achievement >= 100 -> Color(0xFF10B981)
                            achievement >= 80 -> VitalOrange
                            else -> Color(0xFFEF4444)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = achievementColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${achievement.toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = achievementColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 运动数据摘要
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DataChip(
                        label = "${record.actualDuration}分钟",
                        color = SkyBlue
                    )
                    DataChip(
                        label = "${String.format("%.0f", record.actualCalories)}kcal",
                        color = VitalOrange
                    )
                    record.distance?.let { dist ->
                        if (dist > 0) {
                            DataChip(
                                label = ExerciseTrackingUtils.formatDistance(dist),
                                color = ForestGreen
                            )
                        }
                    }
                }

                // 时间区间
                val timeRange = buildTimeRange(record.startedAt, record.endedAt)
                if (timeRange.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeRange,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // 右侧箭头
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "查看详情",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DataChip(label: String, color: Color) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

/**
 * 运动记录详情弹窗
 */
@Composable
private fun ExerciseDetailDialog(
    detailState: ExerciseDetailState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("运动详情", fontWeight = FontWeight.Bold)
        },
        text = {
            when (detailState) {
                is ExerciseDetailState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
                is ExerciseDetailState.Success -> {
                    ExerciseDetailContent(record = detailState.record)
                }
                is ExerciseDetailState.Error -> {
                    Text(
                        text = "加载失败: ${detailState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 运动详情内容
 */
@Composable
private fun ExerciseDetailContent(record: ExerciseRecordResponseData) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 运动类型和日期
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = ExerciseTypeUtils.getTypeEmoji(record.exerciseType),
                fontSize = 28.sp
            )
            Column {
                Text(
                    text = ExerciseTypeUtils.getTypeLabel(record.exerciseType),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = formatDateHeader(record.exerciseDate),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Divider(color = Color(0xFFF0F0F0))

        // 核心数据
        Text("运动数据", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        DetailRow("消耗热量", "${String.format("%.1f", record.actualCalories)} kcal")
        DetailRow("运动时长", "${record.actualDuration} 分钟")
        record.distance?.let { dist ->
            if (dist > 0) {
                DetailRow("运动距离", ExerciseTrackingUtils.formatDistance(dist))
                // 计算配速
                val durationMillis = record.actualDuration * 60000L
                val pace = ExerciseTrackingUtils.calculatePace(dist, durationMillis)
                if (pace > 0 && pace < 60) {
                    DetailRow("平均配速", ExerciseTrackingUtils.formatPace(pace))
                }
                // 计算速度
                val speed = ExerciseTrackingUtils.calculateSpeed(dist, durationMillis)
                if (speed > 0) {
                    DetailRow("平均速度", String.format("%.1f km/h", speed))
                }
            }
        }

        // 计划对比
        if (record.plannedCalories != null || record.plannedDuration != null) {
            Divider(color = Color(0xFFF0F0F0))
            Text("计划对比", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            record.plannedCalories?.let { planned ->
                DetailRow(
                    "热量目标",
                    "${String.format("%.0f", planned)} kcal"
                )
                record.caloriesAchievement?.let { achievement ->
                    DetailRow(
                        "热量达成率",
                        "${String.format("%.1f", achievement)}%",
                        valueColor = getAchievementColor(achievement)
                    )
                }
            }
            record.plannedDuration?.let { planned ->
                DetailRow("时长目标", "$planned 分钟")
                record.durationAchievement?.let { achievement ->
                    DetailRow(
                        "时长达成率",
                        "${String.format("%.1f", achievement)}%",
                        valueColor = getAchievementColor(achievement)
                    )
                }
            }
        }

        // 时间信息
        val timeRange = buildTimeRange(record.startedAt, record.endedAt)
        if (timeRange.isNotEmpty()) {
            Divider(color = Color(0xFFF0F0F0))
            Text("时间信息", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            DetailRow("运动时段", timeRange)
        }

        // 备注
        record.notes?.let { notes ->
            if (notes.isNotBlank()) {
                Divider(color = Color(0xFFF0F0F0))
                Text("备注", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(
                    text = notes,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF8F8F8),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * 空内容提示
 */
@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🏃", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 错误内容提示
 */
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载失败: $message",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("重试")
            }
        }
    }
}

// ==================== 工具函数 ====================

/**
 * 格式化日期头部显示
 * 输入: "2026-02-07"
 * 输出: "2026年02月07日 (周六)" 或 "今天" / "昨天"
 */
private fun formatDateHeader(dateStr: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)
        when (date) {
            today -> "今天 (${date.monthValue}月${date.dayOfMonth}日)"
            yesterday -> "昨天 (${date.monthValue}月${date.dayOfMonth}日)"
            else -> {
                val dayOfWeek = when (date.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> "周一"
                    java.time.DayOfWeek.TUESDAY -> "周二"
                    java.time.DayOfWeek.WEDNESDAY -> "周三"
                    java.time.DayOfWeek.THURSDAY -> "周四"
                    java.time.DayOfWeek.FRIDAY -> "周五"
                    java.time.DayOfWeek.SATURDAY -> "周六"
                    java.time.DayOfWeek.SUNDAY -> "周日"
                }
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ($dayOfWeek)"
            }
        }
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * 构建时间区间文本
 * 例："18:00 - 18:35"
 */
private fun buildTimeRange(startedAt: String?, endedAt: String?): String {
    if (startedAt == null && endedAt == null) return ""
    return try {
        val start = startedAt?.let {
            val dt = java.time.LocalDateTime.parse(it)
            String.format("%02d:%02d", dt.hour, dt.minute)
        } ?: "?"
        val end = endedAt?.let {
            val dt = java.time.LocalDateTime.parse(it)
            String.format("%02d:%02d", dt.hour, dt.minute)
        } ?: "?"
        "$start - $end"
    } catch (e: Exception) {
        ""
    }
}

/**
 * 根据达成率返回颜色
 */
private fun getAchievementColor(achievement: Double): Color {
    return when {
        achievement >= 100 -> Color(0xFF10B981)
        achievement >= 80 -> VitalOrange
        achievement >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}
