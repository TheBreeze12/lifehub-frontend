package com.example.lifehub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.data.DailyNutrientStats
import com.example.lifehub.data.RadarChartDataPoint
import com.example.lifehub.ui.theme.ForestGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase 18: 营养素雷达图组件
 * 展示三大营养素（蛋白质、脂肪、碳水化合物）的摄入比例与膳食指南建议值的对比
 */

// 膳食指南常量（与后端保持一致）
object DietaryGuidelines {
    const val PROTEIN_MIN = 10.0
    const val PROTEIN_MAX = 15.0
    const val FAT_MIN = 20.0
    const val FAT_MAX = 30.0
    const val CARBS_MIN = 50.0
    const val CARBS_MAX = 65.0
}

// 状态对应的颜色
@Composable
fun getStatusColor(status: String): Color {
    return when (status) {
        "normal" -> ForestGreen
        "low" -> Color(0xFFFF9800)  // Orange
        "high" -> Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * 营养素雷达图
 * @param stats 每日营养素统计数据
 * @param modifier Modifier
 */
@Composable
fun NutrientRadarChart(
    stats: DailyNutrientStats,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // 构建雷达图数据点
    val dataPoints = buildRadarDataPoints(stats)
    
    // 判断是否有数据
    val hasData = stats.totalCalories > 0
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 雷达图
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = min(size.width, size.height) / 2 * 0.7f
                
                // 绘制背景网格
                drawRadarBackground(centerX, centerY, radius)
                
                // 绘制建议范围区域
                drawRecommendedRange(centerX, centerY, radius, dataPoints)
                
                if (hasData) {
                    // 绘制实际数据多边形
                    drawActualData(centerX, centerY, radius, dataPoints)
                }
                
                // 绘制轴线
                drawAxes(centerX, centerY, radius)
            }
            
            // 绘制标签（使用Compose Text）
            RadarLabels(dataPoints = dataPoints, hasData = hasData)
        }
        
        // 图例
        if (hasData) {
            NutrientLegend(dataPoints = dataPoints)
        } else {
            Text(
                text = "暂无营养数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 构建雷达图数据点
 */
private fun buildRadarDataPoints(stats: DailyNutrientStats): List<RadarChartDataPoint> {
    val comparison = stats.guidelinesComparison
    
    return listOf(
        RadarChartDataPoint(
            label = "蛋白质",
            value = stats.proteinRatio,
            recommendedMin = comparison?.protein?.recommendedMin ?: DietaryGuidelines.PROTEIN_MIN,
            recommendedMax = comparison?.protein?.recommendedMax ?: DietaryGuidelines.PROTEIN_MAX,
            status = comparison?.protein?.status ?: determineStatus(
                stats.proteinRatio, 
                DietaryGuidelines.PROTEIN_MIN, 
                DietaryGuidelines.PROTEIN_MAX
            )
        ),
        RadarChartDataPoint(
            label = "脂肪",
            value = stats.fatRatio,
            recommendedMin = comparison?.fat?.recommendedMin ?: DietaryGuidelines.FAT_MIN,
            recommendedMax = comparison?.fat?.recommendedMax ?: DietaryGuidelines.FAT_MAX,
            status = comparison?.fat?.status ?: determineStatus(
                stats.fatRatio,
                DietaryGuidelines.FAT_MIN,
                DietaryGuidelines.FAT_MAX
            )
        ),
        RadarChartDataPoint(
            label = "碳水",
            value = stats.carbsRatio,
            recommendedMin = comparison?.carbs?.recommendedMin ?: DietaryGuidelines.CARBS_MIN,
            recommendedMax = comparison?.carbs?.recommendedMax ?: DietaryGuidelines.CARBS_MAX,
            status = comparison?.carbs?.status ?: determineStatus(
                stats.carbsRatio,
                DietaryGuidelines.CARBS_MIN,
                DietaryGuidelines.CARBS_MAX
            )
        )
    )
}

/**
 * 判断营养素状态
 */
private fun determineStatus(actual: Double, min: Double, max: Double): String {
    return when {
        actual < min -> "low"
        actual > max -> "high"
        else -> "normal"
    }
}

/**
 * 绘制雷达图背景网格
 */
private fun DrawScope.drawRadarBackground(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.2f)
    val levels = 5
    
    // 绘制同心圆/多边形
    for (i in 1..levels) {
        val levelRadius = radius * i / levels
        val path = Path()
        
        for (j in 0 until 3) {
            val angle = -PI / 2 + j * 2 * PI / 3
            val x = centerX + levelRadius * cos(angle).toFloat()
            val y = centerY + levelRadius * sin(angle).toFloat()
            
            if (j == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        drawPath(
            path = path,
            color = gridColor,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * 绘制建议范围区域（浅绿色半透明区域）
 */
private fun DrawScope.drawRecommendedRange(
    centerX: Float,
    centerY: Float,
    radius: Float,
    dataPoints: List<RadarChartDataPoint>
) {
    // 建议最大值区域
    val maxPath = Path()
    val minPath = Path()
    
    dataPoints.forEachIndexed { index, point ->
        val angle = -PI / 2 + index * 2 * PI / 3
        
        // 最大值边界（相对于65%作为100%基准）
        val maxNorm = (point.recommendedMax / 65.0).coerceIn(0.0, 1.0)
        val maxR = radius * maxNorm.toFloat()
        val maxX = centerX + maxR * cos(angle).toFloat()
        val maxY = centerY + maxR * sin(angle).toFloat()
        
        // 最小值边界
        val minNorm = (point.recommendedMin / 65.0).coerceIn(0.0, 1.0)
        val minR = radius * minNorm.toFloat()
        val minX = centerX + minR * cos(angle).toFloat()
        val minY = centerY + minR * sin(angle).toFloat()
        
        if (index == 0) {
            maxPath.moveTo(maxX, maxY)
            minPath.moveTo(minX, minY)
        } else {
            maxPath.lineTo(maxX, maxY)
            minPath.lineTo(minX, minY)
        }
    }
    maxPath.close()
    minPath.close()
    
    // 绘制建议范围区域
    drawPath(
        path = maxPath,
        color = ForestGreen.copy(alpha = 0.15f)
    )
}

/**
 * 绘制实际数据多边形
 */
private fun DrawScope.drawActualData(
    centerX: Float,
    centerY: Float,
    radius: Float,
    dataPoints: List<RadarChartDataPoint>
) {
    val path = Path()
    
    dataPoints.forEachIndexed { index, point ->
        val angle = -PI / 2 + index * 2 * PI / 3
        // 使用65%作为100%基准进行归一化
        val norm = (point.value / 65.0).coerceIn(0.0, 1.5)
        val r = radius * norm.toFloat()
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    
    // 填充
    drawPath(
        path = path,
        color = ForestGreen.copy(alpha = 0.3f)
    )
    
    // 边框
    drawPath(
        path = path,
        color = ForestGreen,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // 绘制数据点
    dataPoints.forEachIndexed { index, point ->
        val angle = -PI / 2 + index * 2 * PI / 3
        val norm = (point.value / 65.0).coerceIn(0.0, 1.5)
        val r = radius * norm.toFloat()
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        
        drawCircle(
            color = when (point.status) {
                "normal" -> ForestGreen
                "low" -> Color(0xFFFF9800)
                "high" -> Color(0xFFF44336)
                else -> ForestGreen
            },
            radius = 6.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

/**
 * 绘制轴线
 */
private fun DrawScope.drawAxes(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    val axisColor = Color.Gray.copy(alpha = 0.5f)
    
    for (i in 0 until 3) {
        val angle = -PI / 2 + i * 2 * PI / 3
        val endX = centerX + radius * cos(angle).toFloat()
        val endY = centerY + radius * sin(angle).toFloat()
        
        drawLine(
            color = axisColor,
            start = Offset(centerX, centerY),
            end = Offset(endX, endY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * 雷达图标签
 */
@Composable
private fun BoxScope.RadarLabels(
    dataPoints: List<RadarChartDataPoint>,
    hasData: Boolean
) {
    // 蛋白质标签（顶部）
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "蛋白质",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (hasData && dataPoints.isNotEmpty()) {
            Text(
                text = "${String.format("%.1f", dataPoints[0].value)}%",
                style = MaterialTheme.typography.bodySmall,
                color = getStatusColor(dataPoints[0].status)
            )
        }
    }
    
    // 脂肪标签（右下）
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "脂肪",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (hasData && dataPoints.size > 1) {
            Text(
                text = "${String.format("%.1f", dataPoints[1].value)}%",
                style = MaterialTheme.typography.bodySmall,
                color = getStatusColor(dataPoints[1].status)
            )
        }
    }
    
    // 碳水标签（左下）
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "碳水",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (hasData && dataPoints.size > 2) {
            Text(
                text = "${String.format("%.1f", dataPoints[2].value)}%",
                style = MaterialTheme.typography.bodySmall,
                color = getStatusColor(dataPoints[2].status)
            )
        }
    }
}

/**
 * 营养素图例
 */
@Composable
private fun NutrientLegend(
    dataPoints: List<RadarChartDataPoint>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dataPoints.forEach { point ->
            NutrientLegendItem(point = point)
        }
        
        // 建议范围说明
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(2.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = ForestGreen.copy(alpha = 0.15f))
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "建议范围",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 单个营养素图例项
 */
@Composable
private fun NutrientLegendItem(
    point: RadarChartDataPoint
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态指示点
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(
                    color = when (point.status) {
                        "normal" -> ForestGreen
                        "low" -> Color(0xFFFF9800)
                        "high" -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = point.label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${String.format("%.1f", point.value)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = getStatusColor(point.status)
            )
            Text(
                text = "建议: ${point.recommendedMin.toInt()}-${point.recommendedMax.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 营养素摘要卡片
 * 展示总热量和三大营养素的克数
 */
@Composable
fun NutrientSummaryCard(
    stats: DailyNutrientStats,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "今日营养摄入",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 总热量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "总热量",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.0f", stats.totalCalories)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForestGreen
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 三大营养素克数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientGramItem(
                    label = "蛋白质",
                    grams = stats.totalProtein,
                    color = ForestGreen
                )
                NutrientGramItem(
                    label = "脂肪",
                    grams = stats.totalFat,
                    color = Color(0xFFFF9800)
                )
                NutrientGramItem(
                    label = "碳水",
                    grams = stats.totalCarbs,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun NutrientGramItem(
    label: String,
    grams: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${String.format("%.1f", grams)}g",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
