package com.example.lifehub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifehub.data.ChartDataPoint
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.ui.theme.VitalOrange
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

/**
 * Phase 17: 热量收支柱状图组件
 * 使用Vico图表库展示热量摄入和消耗数据
 */

@Composable
fun CalorieChart(
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    showBurn: Boolean = true
) {
    if (dataPoints.isEmpty()) {
        EmptyChartPlaceholder(modifier)
        return
    }

    val intakeColor = VitalOrange
    val burnColor = ForestGreen

    // 创建图表数据
    val chartEntryModelProducer = remember(dataPoints) {
        val intakeEntries = dataPoints.mapIndexed { index, point ->
            entryOf(index.toFloat(), point.intake)
        }
        val burnEntries = if (showBurn) {
            dataPoints.mapIndexed { index, point ->
                entryOf(index.toFloat(), point.burn)
            }
        } else {
            emptyList()
        }
        
        ChartEntryModelProducer(
            if (showBurn && burnEntries.isNotEmpty()) {
                listOf(intakeEntries, burnEntries)
            } else {
                listOf(intakeEntries)
            }
        )
    }

    // X轴标签格式化
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        dataPoints.getOrNull(value.toInt())?.label ?: ""
    }

    // Y轴标签格式化
    val startAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${value.toInt()}"
    }

    Column(modifier = modifier) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = intakeColor, label = "摄入")
            if (showBurn) {
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(color = burnColor, label = "消耗")
            }
        }

        // 图表
        Chart(
            chart = columnChart(
                columns = if (showBurn) {
                    listOf(
                        lineComponent(
                            color = intakeColor,
                            thickness = 16.dp
                        ),
                        lineComponent(
                            color = burnColor,
                            thickness = 16.dp
                        )
                    )
                } else {
                    listOf(
                        lineComponent(
                            color = intakeColor,
                            thickness = 24.dp
                        )
                    )
                },
                spacing = 8.dp
            ),
            chartModelProducer = chartEntryModelProducer,
            startAxis = rememberStartAxis(
                valueFormatter = startAxisValueFormatter,
                label = textComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = bottomAxisValueFormatter,
                label = textComponent(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = MaterialTheme.shapes.small
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "开始记录饮食和运动吧",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 热量收支摘要卡片
 */
@Composable
fun CalorieSummaryCard(
    intakeCalories: Double,
    burnCalories: Double,
    netCalories: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CalorieStat(
                value = intakeCalories,
                label = "摄入",
                color = VitalOrange
            )
            CalorieStat(
                value = burnCalories,
                label = "消耗",
                color = ForestGreen
            )
            CalorieStat(
                value = netCalories,
                label = "净热量",
                color = if (netCalories >= 0) VitalOrange else ForestGreen
            )
        }
    }
}

@Composable
private fun CalorieStat(
    value: Double,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%.0f".format(value),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "kcal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 周统计摘要卡片
 */
@Composable
fun WeeklySummaryCard(
    totalIntake: Double,
    totalBurn: Double,
    avgIntake: Double,
    avgBurn: Double,
    activeDays: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "本周统计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "总摄入", value = "%.0f kcal".format(totalIntake))
                StatItem(label = "总消耗", value = "%.0f kcal".format(totalBurn))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "日均摄入", value = "%.0f kcal".format(avgIntake))
                StatItem(label = "日均消耗", value = "%.0f kcal".format(avgBurn))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "活跃天数", value = "$activeDays 天")
                StatItem(label = "净热量", value = "%.0f kcal".format(totalIntake - totalBurn))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
