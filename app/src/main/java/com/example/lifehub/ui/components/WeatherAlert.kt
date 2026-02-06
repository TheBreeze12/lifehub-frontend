package com.example.lifehub.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.data.PlanBAlternative
import com.example.lifehub.data.PlanBData
import com.example.lifehub.data.WeatherAssessment
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.PlanBState

/**
 * Phase 33: 天气预警横幅组件
 * 根据天气评估严重程度展示不同级别的预警信息
 */
@Composable
fun WeatherAlertBanner(
        weatherAssessment: WeatherAssessment,
        modifier: Modifier = Modifier
) {
    val alertLevel = getAlertLevelFromSeverity(weatherAssessment.severity)
    if (alertLevel == AlertLevel.NONE) return

    val (bgGradient, iconTint, icon) = getAlertStyle(alertLevel, weatherAssessment.weathercode)

    Card(
            modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = iconTint.copy(alpha = 0.2f),
                            spotColor = iconTint.copy(alpha = 0.3f)
                    ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .background(bgGradient)
                        .padding(16.dp)
        ) {
            Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 天气图标
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                )

                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 标题
                    Text(
                            text = when (alertLevel) {
                                AlertLevel.INFO -> "天气提示"
                                AlertLevel.WARNING -> "⚠ 天气预警"
                                AlertLevel.DANGER -> "🚨 恶劣天气警告"
                                else -> ""
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                    )

                    // 天气描述
                    Text(
                            text = weatherAssessment.description,
                            fontSize = 13.sp,
                            color = TextPrimary
                    )

                    // 建议
                    Text(
                            text = weatherAssessment.recommendation,
                            fontSize = 12.sp,
                            color = TextSecondary
                    )

                    // 温度/风速警告
                    weatherAssessment.warnings?.forEach { warning ->
                        Text(
                                text = warning,
                                fontSize = 11.sp,
                                color = ErrorRed,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phase 33: Plan B 室内替代方案展示卡片
 * 展示完整的Plan B方案，包含原计划热量对比和替代运动列表
 */
@Composable
fun PlanBCard(
        planBData: PlanBData,
        modifier: Modifier = Modifier
) {
    if (!planBData.need_plan_b) return

    Card(
            modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = VitalOrange.copy(alpha = 0.1f),
                            spotColor = VitalOrange.copy(alpha = 0.15f)
                    ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(24.dp)
                )
                Text(
                        text = "室内替代方案 (Plan B)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )
            }

            // 原因说明
            Text(
                    text = planBData.reason,
                    fontSize = 13.sp,
                    color = TextSecondary
            )

            // 热量对比卡片
            CalorieComparisonRow(
                    originalCalories = planBData.original_calories,
                    planBCalories = planBData.plan_b_total_calories
            )

            // 替代运动列表
            planBData.alternatives.forEach { alt ->
                PlanBAlternativeItem(alternative = alt)
            }
        }
    }
}

/**
 * 热量对比行：原计划 vs Plan B
 */
@Composable
private fun CalorieComparisonRow(
        originalCalories: Double,
        planBCalories: Double
) {
    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackgroundTint)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
    ) {
        // 原计划热量
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                    text = "原计划",
                    fontSize = 11.sp,
                    color = TextTertiary
            )
            Text(
                    text = "${originalCalories.toInt()} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
            )
        }

        // 箭头
        Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = VitalOrange,
                modifier = Modifier.size(20.dp)
        )

        // Plan B 热量
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                    text = "Plan B",
                    fontSize = 11.sp,
                    color = TextTertiary
            )
            Text(
                    text = "${planBCalories.toInt()} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalOrange
            )
        }
    }
}

/**
 * Plan B 替代运动项
 */
@Composable
private fun PlanBAlternativeItem(alternative: PlanBAlternative) {
    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF7ED))
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 室内运动图标
        Box(
                modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VitalOrangeLight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = getExerciseIcon(alternative.exercise_type),
                    contentDescription = null,
                    tint = VitalOrange,
                    modifier = Modifier.size(22.dp)
            )
        }

        Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                    text = alternative.exercise_name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
            )
            Text(
                    text = alternative.description,
                    fontSize = 11.sp,
                    color = TextSecondary
            )
        }

        // 时长和热量
        Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                    text = "${alternative.duration}分钟",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
            )
            Text(
                    text = "${alternative.calories.toInt()} kcal",
                    fontSize = 11.sp,
                    color = VitalOrange,
                    fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Plan B 加载状态区域
 * 在TripDetailPage中使用，展示Plan B的各种加载状态
 */
@Composable
fun PlanBSection(
        planBState: PlanBState,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
) {
    when (planBState) {
        is PlanBState.Loading -> {
            Box(
                    modifier = modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    contentAlignment = Alignment.Center
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                            color = VitalOrange,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                    )
                    Text(
                            text = "正在检查天气状况...",
                            fontSize = 13.sp,
                            color = TextSecondary
                    )
                }
            }
        }
        is PlanBState.Success -> {
            val data = planBState.data

            Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 天气预警横幅
                WeatherAlertBanner(weatherAssessment = data.weather)

                // Plan B 替代方案（仅在需要时展示）
                if (data.need_plan_b) {
                    PlanBCard(planBData = data)
                }
            }
        }
        is PlanBState.Error -> {
            Row(
                    modifier = modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorRed.copy(alpha = 0.1f))
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                )
                Text(
                        text = "天气检查失败",
                        fontSize = 13.sp,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRetry) {
                    Text("重试", fontSize = 12.sp, color = ErrorRed)
                }
            }
        }
        is PlanBState.Idle -> {
            // 空闲状态不展示内容
        }
    }
}

// ==================== 辅助函数 ====================

/** 天气预警级别 */
enum class AlertLevel { NONE, INFO, WARNING, DANGER }

/** 根据严重程度获取预警级别 */
fun getAlertLevelFromSeverity(severity: String): AlertLevel {
    return when (severity) {
        "good" -> AlertLevel.NONE
        "mild" -> AlertLevel.INFO
        "moderate" -> AlertLevel.WARNING
        "severe" -> AlertLevel.DANGER
        else -> AlertLevel.NONE
    }
}

/** 获取预警样式（背景渐变、图标颜色、图标） */
private fun getAlertStyle(
        level: AlertLevel,
        weathercode: Int?
): Triple<Brush, Color, ImageVector> {
    return when (level) {
        AlertLevel.INFO -> Triple(
                Brush.linearGradient(listOf(SkyBlueLight.copy(alpha = 0.3f), SkyBlue.copy(alpha = 0.1f))),
                SkyBlue,
                getWeatherIcon(weathercode)
        )
        AlertLevel.WARNING -> Triple(
                Brush.linearGradient(listOf(WarningYellow.copy(alpha = 0.3f), VitalOrangeLight.copy(alpha = 0.15f))),
                VitalOrange,
                getWeatherIcon(weathercode)
        )
        AlertLevel.DANGER -> Triple(
                Brush.linearGradient(listOf(ErrorRed.copy(alpha = 0.2f), CoralPink.copy(alpha = 0.1f))),
                ErrorRed,
                getWeatherIcon(weathercode)
        )
        else -> Triple(
                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                TextSecondary,
                Icons.Default.WbSunny
        )
    }
}

/** 根据WMO天气代码获取图标 */
fun getWeatherIcon(code: Int?): ImageVector {
    return when (code) {
        null -> Icons.Default.WbSunny
        0, 1 -> Icons.Default.WbSunny
        2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Cloud
        in 51..57 -> Icons.Default.Grain
        in 61..67 -> Icons.Default.WaterDrop
        in 71..77 -> Icons.Default.AcUnit
        in 80..82 -> Icons.Default.WaterDrop
        in 85..86 -> Icons.Default.AcUnit
        in 95..99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }
}

/** 根据运动类型获取图标 */
private fun getExerciseIcon(exerciseType: String): ImageVector {
    return when (exerciseType) {
        "jumping_rope" -> Icons.Default.FitnessCenter
        "aerobics" -> Icons.Default.DirectionsRun
        "yoga" -> Icons.Default.SelfImprovement
        "weight_training" -> Icons.Default.FitnessCenter
        "cycling" -> Icons.Default.DirectionsBike
        "stair_climbing" -> Icons.Default.Stairs
        "gym" -> Icons.Default.FitnessCenter
        "stretching" -> Icons.Default.Accessibility
        "running" -> Icons.Default.DirectionsRun
        "tai_chi" -> Icons.Default.SelfImprovement
        "dancing" -> Icons.Default.MusicNote
        "table_tennis" -> Icons.Default.SportsTennis
        else -> Icons.Default.FitnessCenter
    }
}