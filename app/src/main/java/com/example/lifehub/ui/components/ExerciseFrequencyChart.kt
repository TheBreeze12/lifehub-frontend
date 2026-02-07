package com.example.lifehub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifehub.data.ExerciseFrequencyData
import com.example.lifehub.data.ExerciseTypeDistribution
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.ui.theme.VitalOrange

/**
 * Phase 51: 运动频率分析展示组件
 * 展示运动频率柱状图、类型分布饼图、评级和建议
 */

@Composable
fun ExerciseFrequencyChart(
    data: ExerciseFrequencyData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 频率概览卡片
        FrequencyOverviewCard(data = data)

        Spacer(modifier = Modifier.height(16.dp))

        // 每日运动频率柱状图
        DailyFrequencyBarChart(data = data)

        Spacer(modifier = Modifier.height(16.dp))

        // 运动类型分布
        if (data.typeDistribution.isNotEmpty()) {
            TypeDistributionSection(distribution = data.typeDistribution)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 评级与建议
        FrequencyRatingCard(
            rating = data.frequencyRating,
            suggestion = data.frequencySuggestion
        )
    }
}

/**
 * 频率概览卡片 - 展示关键统计指标
 */
@Composable
private fun FrequencyOverviewCard(data: ExerciseFrequencyData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "运动频率概览（${data.periodLabel}）",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${data.activeDays}/${data.totalDays}",
                    label = "运动天数",
                    valueColor = ForestGreen
                )
                StatItem(
                    value = "${data.totalExerciseCount}",
                    label = "运动次数",
                    valueColor = VitalOrange
                )
                StatItem(
                    value = String.format("%.1f", data.avgFrequency),
                    label = "周均次数",
                    valueColor = ForestGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${data.totalDuration}",
                    label = "总时长(分钟)",
                    valueColor = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = String.format("%.0f", data.totalCalories),
                    label = "总消耗(kcal)",
                    valueColor = VitalOrange
                )
                StatItem(
                    value = String.format("%.0f", data.avgCaloriesPerSession),
                    label = "次均消耗",
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 每日运动频率柱状图（简易Canvas实现，不依赖第三方图表库）
 */
@Composable
private fun DailyFrequencyBarChart(data: ExerciseFrequencyData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "每日运动次数",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val dailyData = data.dailyData
            if (dailyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            val maxCount = remember(dailyData) {
                (dailyData.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
            }
            val barColor = ForestGreen
            val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barWidth = size.width / dailyData.size * 0.6f
                val gapWidth = size.width / dailyData.size * 0.4f
                val totalBarSpace = barWidth + gapWidth

                dailyData.forEachIndexed { index, day ->
                    val barHeight = if (maxCount > 0 && day.count > 0) {
                        (day.count.toFloat() / maxCount) * (size.height - 20f)
                    } else {
                        4f
                    }
                    val x = index * totalBarSpace + gapWidth / 2
                    val y = size.height - barHeight

                    drawRect(
                        color = if (day.count > 0) barColor else emptyBarColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            // 日期标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val labels = remember(dailyData) {
                    if (dailyData.size <= 7) {
                        dailyData.map { it.date.takeLast(5) }
                    } else {
                        // 月模式只显示部分标签避免拥挤
                        dailyData.mapIndexed { i, d ->
                            if (i % 5 == 0 || i == dailyData.size - 1) d.date.takeLast(5)
                            else ""
                        }
                    }
                }
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

/**
 * 运动类型分布区域
 */
@Composable
private fun TypeDistributionSection(distribution: List<ExerciseTypeDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "运动类型分布",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 简易饼图
            val pieColors = listOf(
                ForestGreen,
                VitalOrange,
                Color(0xFF42A5F5),
                Color(0xFFAB47BC),
                Color(0xFF26A69A),
                Color(0xFFEF5350),
                Color(0xFFFFA726),
                Color(0xFF66BB6A),
                Color(0xFF78909C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 饼图
                Canvas(
                    modifier = Modifier.size(100.dp)
                ) {
                    var startAngle = -90f
                    distribution.forEachIndexed { index, item ->
                        val sweepAngle = (item.percentage / 100.0 * 360.0).toFloat()
                        drawArc(
                            color = pieColors[index % pieColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(size.width, size.height)
                        )
                        startAngle += sweepAngle
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 图例
                Column(modifier = Modifier.weight(1f)) {
                    distribution.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(10.dp),
                                color = pieColors[index % pieColors.size],
                                shape = RoundedCornerShape(2.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.label} ${String.format("%.1f", item.percentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${item.count}次",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 频率评级与建议卡片
 */
@Composable
private fun FrequencyRatingCard(
    rating: String,
    suggestion: String
) {
    val (ratingLabel, ratingColor) = when (rating) {
        "excellent" -> "优秀" to ForestGreen
        "good" -> "良好" to Color(0xFF42A5F5)
        "fair" -> "一般" to VitalOrange
        else -> "不足" to Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ratingColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 评级标签
            Surface(
                color = ratingColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = ratingLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 建议文本
            Text(
                text = suggestion.ifEmpty { "开始记录运动数据，获取个性化建议" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
