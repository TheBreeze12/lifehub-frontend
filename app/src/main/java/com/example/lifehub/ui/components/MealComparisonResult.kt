package com.example.lifehub.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lifehub.data.AfterMealData

private val ForestGreen = Color(0xFF2D5A27)
private val VitalOrange = Color(0xFFFF6B35)
private val CardBackground = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)
private val SuccessGreen = Color(0xFF34C759)

/**
 * 餐前餐后对比结果组件
 * Phase 14: 双图对比展示
 * 展示餐前餐后照片、净摄入热量、消耗比例，支持手动调整比例
 */
@Composable
fun MealComparisonResult(
    afterMealData: AfterMealData,
    onRatioAdjusted: ((Double) -> Unit)? = null,
    onSaveRecord: (() -> Unit)? = null,
    onNewComparison: (() -> Unit)? = null
) {
    // 可调整的消耗比例（初始为AI识别值）
    var adjustedRatio by remember { mutableStateOf(afterMealData.consumptionRatio) }
    
    // 基于调整后比例计算的净摄入
    val adjustedNetCalories = afterMealData.originalCalories * adjustedRatio
    val adjustedNetProtein = afterMealData.originalProtein * adjustedRatio
    val adjustedNetFat = afterMealData.originalFat * adjustedRatio
    val adjustedNetCarbs = afterMealData.originalCarbs * adjustedRatio

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // 成功标题
        ComparisonSuccessHeader()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 双图对比展示
        DualImageComparison(
            beforeImageUrl = afterMealData.beforeImageUrl,
            afterImageUrl = afterMealData.afterImageUrl
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 净摄入热量展示
        NetCaloriesDisplay(
            originalCalories = afterMealData.originalCalories,
            netCalories = adjustedNetCalories,
            consumptionRatio = adjustedRatio
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 手动调整滑块
        RatioAdjustmentSlider(
            currentRatio = adjustedRatio,
            onRatioChange = { newRatio ->
                adjustedRatio = newRatio
                onRatioAdjusted?.invoke(newRatio)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 营养素对比
        NutrientComparisonCard(
            originalProtein = afterMealData.originalProtein,
            originalFat = afterMealData.originalFat,
            originalCarbs = afterMealData.originalCarbs,
            netProtein = adjustedNetProtein,
            netFat = adjustedNetFat,
            netCarbs = adjustedNetCarbs
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI分析说明
        afterMealData.comparisonAnalysis?.let { analysis ->
            if (analysis.isNotBlank()) {
                AnalysisCard(analysis = analysis)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 操作按钮
        ActionButtons(
            onSaveRecord = onSaveRecord,
            onNewComparison = onNewComparison
        )
    }
}

/** 成功标题 */
@Composable
private fun ComparisonSuccessHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "对比分析完成",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
        }
    }
}

/** 双图对比展示 */
@Composable
private fun DualImageComparison(
    beforeImageUrl: String?,
    afterImageUrl: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "餐前餐后对比",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 餐前图片
                ComparisonImageCard(
                    imageUrl = beforeImageUrl,
                    label = "餐前",
                    labelColor = VitalOrange,
                    modifier = Modifier.weight(1f)
                )
                
                // 对比箭头
                Icon(
                    Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
                
                // 餐后图片
                ComparisonImageCard(
                    imageUrl = afterImageUrl,
                    label = "餐后",
                    labelColor = ForestGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单张对比图片卡片 */
@Composable
private fun ComparisonImageCard(
    imageUrl: String?,
    label: String,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标签
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = labelColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 图片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/** 净摄入热量展示 */
@Composable
private fun NetCaloriesDisplay(
    originalCalories: Double,
    netCalories: Double,
    consumptionRatio: Double
) {
    val savedCalories = originalCalories - netCalories
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "净摄入热量",
                fontSize = 14.sp,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 大数字显示
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.0f", netCalories),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kcal",
                    fontSize = 18.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 对比信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 原始热量
                CaloriesInfoItem(
                    label = "原始热量",
                    value = String.format("%.0f", originalCalories),
                    color = VitalOrange
                )
                
                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE5E5E5))
                )
                
                // 消耗比例
                CaloriesInfoItem(
                    label = "实际摄入",
                    value = String.format("%.0f%%", consumptionRatio * 100),
                    color = ForestGreen
                )
                
                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE5E5E5))
                )
                
                // 节省热量
                CaloriesInfoItem(
                    label = "少摄入",
                    value = "+${String.format("%.0f", savedCalories)}",
                    color = SuccessGreen
                )
            }
        }
    }
}

/** 热量信息项 */
@Composable
private fun CaloriesInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** 手动调整消耗比例滑块 */
@Composable
private fun RatioAdjustmentSlider(
    currentRatio: Double,
    onRatioChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "手动调整比例",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VitalOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = String.format("%.0f%%", currentRatio * 100),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalOrange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 滑块
            Slider(
                value = currentRatio.toFloat(),
                onValueChange = { onRatioChange(it.toDouble()) },
                valueRange = 0f..1f,
                steps = 19, // 5%步进
                colors = SliderDefaults.colors(
                    thumbColor = VitalOrange,
                    activeTrackColor = VitalOrange,
                    inactiveTrackColor = VitalOrange.copy(alpha = 0.2f)
                )
            )
            
            // 刻度标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0%", fontSize = 11.sp, color = TextSecondary)
                Text(text = "25%", fontSize = 11.sp, color = TextSecondary)
                Text(text = "50%", fontSize = 11.sp, color = TextSecondary)
                Text(text = "75%", fontSize = 11.sp, color = TextSecondary)
                Text(text = "100%", fontSize = 11.sp, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "如果AI识别不准确，可以手动调整实际摄入比例",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 营养素对比卡片 */
@Composable
private fun NutrientComparisonCard(
    originalProtein: Double,
    originalFat: Double,
    originalCarbs: Double,
    netProtein: Double,
    netFat: Double,
    netCarbs: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "营养素摄入",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientComparisonItem(
                    name = "蛋白质",
                    original = originalProtein,
                    net = netProtein,
                    unit = "g",
                    color = Color(0xFF5AC8FA)
                )
                NutrientComparisonItem(
                    name = "脂肪",
                    original = originalFat,
                    net = netFat,
                    unit = "g",
                    color = Color(0xFFFFCC00)
                )
                NutrientComparisonItem(
                    name = "碳水",
                    original = originalCarbs,
                    net = netCarbs,
                    unit = "g",
                    color = Color(0xFFFF9500)
                )
            }
        }
    }
}

/** 营养素对比项 */
@Composable
private fun NutrientComparisonItem(
    name: String,
    original: Double,
    net: Double,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${String.format("%.1f", net)}$unit",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "原${String.format("%.1f", original)}$unit",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

/** AI分析说明卡片 */
@Composable
private fun AnalysisCard(analysis: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ForestGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = analysis,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/** 操作按钮 */
@Composable
private fun ActionButtons(
    onSaveRecord: (() -> Unit)?,
    onNewComparison: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onSaveRecord != null) {
            Button(
                onClick = onSaveRecord,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存记录")
            }
        }
        
        if (onNewComparison != null) {
            OutlinedButton(
                onClick = onNewComparison,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = ForestGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新的对比", color = ForestGreen)
            }
        }
    }
}
