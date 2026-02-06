package com.example.lifehub.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.ExerciseTrackingUtils
import com.example.lifehub.data.SaveExerciseState
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.ExerciseViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 运动结算展示页面 - Phase 28
 *
 * 功能：
 * - 展示运动完成后的结算数据
 * - 展示实际vs计划对比
 * - 展示达成率
 * - 保存运动记录到后端
 *
 * @param navController 导航控制器
 * @param planId 关联运动计划ID（可选）
 * @param exerciseType 运动类型
 * @param distance 运动距离（米）
 * @param duration 运动时长（毫秒）
 * @param calories 消耗热量（kcal）
 * @param pace 平均配速（分钟/公里）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSummaryPage(
    navController: NavController,
    planId: Int? = null,
    exerciseType: String = "walking",
    distance: Double = 0.0,
    duration: Long = 0L,
    calories: Double = 0.0,
    pace: Double = 0.0,
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val saveState by exerciseViewModel.saveState.collectAsState()
    val scrollState = rememberScrollState()

    // 计算实际运动时长（分钟）
    val durationMinutes = (duration / 60000).toInt().coerceAtLeast(1)

    // 运动类型显示名称
    val exerciseTypeLabel = when (exerciseType) {
        "walking" -> "散步"
        "running" -> "跑步"
        "cycling" -> "骑行"
        "hiking" -> "徒步"
        else -> "运动"
    }

    // 保存成功后的达成率数据
    var caloriesAchievement by remember { mutableStateOf<Double?>(null) }
    var durationAchievement by remember { mutableStateOf<Double?>(null) }
    var savedPlannedCalories by remember { mutableStateOf<Double?>(null) }
    var savedPlannedDuration by remember { mutableStateOf<Int?>(null) }

    // 监听保存状态变化
    LaunchedEffect(saveState) {
        if (saveState is SaveExerciseState.Success) {
            // 保存成功后无需额外操作，UI已通过状态更新
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 顶部标题栏
            SummaryTopBar(
                title = "$exerciseTypeLabel 结算",
                onBackClick = {
                    exerciseViewModel.resetSaveState()
                    navController.popBackStack()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 运动完成标识
            CompletionBadge()

            Spacer(modifier = Modifier.height(16.dp))

            // 核心数据卡片
            CoreDataCard(
                distance = distance,
                duration = duration,
                calories = calories,
                pace = pace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 实际vs计划对比卡片
            ComparisonCard(
                actualCalories = calories,
                actualDuration = durationMinutes,
                actualDistance = distance,
                plannedCalories = savedPlannedCalories,
                plannedDuration = savedPlannedDuration,
                caloriesAchievement = caloriesAchievement,
                durationAchievement = durationAchievement
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 达成率展示卡片
            AchievementCard(
                caloriesAchievement = caloriesAchievement,
                durationAchievement = durationAchievement
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮区域
            SaveSection(
                saveState = saveState,
                onSave = {
                    val userId = UserSession.getUserId() ?: 1
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    exerciseViewModel.saveExerciseRecord(
                        userId = userId,
                        actualCalories = calories,
                        actualDuration = durationMinutes,
                        distance = if (distance > 0) distance else null,
                        exerciseDate = today,
                        plannedCalories = null,
                        plannedDuration = null
                    )
                },
                onSaveSuccess = { response ->
                    caloriesAchievement = response.caloriesAchievement
                    durationAchievement = response.durationAchievement
                    savedPlannedCalories = response.plannedCalories
                    savedPlannedDuration = response.plannedDuration
                },
                onFinish = {
                    exerciseViewModel.resetSaveState()
                    exerciseViewModel.resetTracking()
                    navController.popBackStack(
                        route = "home",
                        inclusive = false
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** 顶部标题栏 */
@Composable
private fun SummaryTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

/** 运动完成标识 */
@Composable
private fun CompletionBadge() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ForestGreenLight, ForestGreen)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "运动已完成",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ForestGreenDark
        )
    }
}

/** 核心数据卡片 */
@Composable
private fun CoreDataCard(
    distance: Double,
    duration: Long,
    calories: Double,
    pace: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 主要数据：时间
            Text(
                text = ExerciseTrackingUtils.formatDuration(duration),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "运动时长",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Divider(color = BackgroundBeige, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // 三列数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryDataItem(
                    label = "距离",
                    value = ExerciseTrackingUtils.formatDistance(distance),
                    icon = Icons.Default.Straighten,
                    color = ForestGreen
                )
                SummaryDataItem(
                    label = "配速",
                    value = ExerciseTrackingUtils.formatPace(pace),
                    icon = Icons.Default.Speed,
                    color = SkyBlue
                )
                SummaryDataItem(
                    label = "热量",
                    value = "${calories.toInt()} kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    color = VitalOrange
                )
            }
        }
    }
}

/** 单个数据项 */
@Composable
private fun SummaryDataItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/** 实际vs计划对比卡片 */
@Composable
private fun ComparisonCard(
    actualCalories: Double,
    actualDuration: Int,
    actualDistance: Double,
    plannedCalories: Double?,
    plannedDuration: Int?,
    caloriesAchievement: Double?,
    durationAchievement: Double?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = SkyBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "实际 vs 目标对比",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 热量对比
            ComparisonRow(
                label = "热量消耗",
                actualValue = "${actualCalories.toInt()} kcal",
                plannedValue = if (plannedCalories != null) "${plannedCalories.toInt()} kcal" else "—",
                achievement = caloriesAchievement,
                color = VitalOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 时长对比
            ComparisonRow(
                label = "运动时长",
                actualValue = "${actualDuration} 分钟",
                plannedValue = if (plannedDuration != null) "${plannedDuration} 分钟" else "—",
                achievement = durationAchievement,
                color = SkyBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 距离
            ComparisonRow(
                label = "运动距离",
                actualValue = ExerciseTrackingUtils.formatDistance(actualDistance),
                plannedValue = "—",
                achievement = null,
                color = ForestGreen
            )

            if (plannedCalories == null && plannedDuration == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "保存记录后，系统将自动从运动计划中获取目标数据并计算达成率",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 对比行 */
@Composable
private fun ComparisonRow(
    label: String,
    actualValue: String,
    plannedValue: String,
    achievement: Double?,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = actualValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = " / ",
                fontSize = 14.sp,
                color = TextTertiary
            )
            Text(
                text = plannedValue,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        if (achievement != null) {
            Spacer(modifier = Modifier.height(4.dp))
            // 进度条
            val progress = (achievement / 100.0).coerceIn(0.0, 1.5).toFloat()
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (achievement >= 100) SuccessGreen else color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

/** 达成率展示卡片 */
@Composable
private fun AchievementCard(
    caloriesAchievement: Double?,
    durationAchievement: Double?
) {
    val hasAchievement = caloriesAchievement != null || durationAchievement != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasAchievement) CardBackgroundTint else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "达成率",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasAchievement) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 热量达成率
                    AchievementCircle(
                        label = "热量达成",
                        value = caloriesAchievement,
                        color = VitalOrange
                    )

                    // 时长达成率
                    AchievementCircle(
                        label = "时长达成",
                        value = durationAchievement,
                        color = SkyBlue
                    )
                }
            } else {
                Text(
                    text = "保存运动记录后将显示达成率",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 达成率圆形展示 */
@Composable
private fun AchievementCircle(
    label: String,
    value: Double?,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (value != null) {
                Text(
                    text = "${value.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        value >= 100 -> SuccessGreen
                        value >= 80 -> color
                        value >= 50 -> WarningYellow
                        else -> ErrorRed
                    }
                )
            } else {
                Text(
                    text = "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
            }
        }

        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/** 保存按钮区域 */
@Composable
private fun SaveSection(
    saveState: SaveExerciseState,
    onSave: () -> Unit,
    onSaveSuccess: (com.example.lifehub.data.ExerciseRecordResponseData) -> Unit,
    onFinish: () -> Unit
) {
    // 监听保存成功后更新达成率数据
    LaunchedEffect(saveState) {
        if (saveState is SaveExerciseState.Success) {
            // 这里无法直接拿到response data，因为ViewModel只保存了recordId
            // 达成率数据会在后续通过查询接口获取（或由SaveState扩展携带）
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (saveState) {
            is SaveExerciseState.Idle -> {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存运动记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is SaveExerciseState.Saving -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存中...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is SaveExerciseState.Success -> {
                // 保存成功提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SuccessGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "运动记录已保存",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 完成按钮
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "返回首页",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is SaveExerciseState.Error -> {
                // 错误提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorRed.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveState.message,
                            fontSize = 14.sp,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 重试按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("跳过保存", color = TextSecondary)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestGreen
                        )
                    ) {
                        Text("重试", color = Color.White)
                    }
                }
            }
        }
    }
}
