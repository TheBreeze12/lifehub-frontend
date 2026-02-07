package com.example.lifehub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.ui.theme.*

/**
 * Phase 58: 营养素自定义展示组件
 *
 * 支持用户自选营养素展示维度（膳食纤维、钠、维生素等），
 * 基于菜品基础营养数据估算扩展营养素含量。
 */

/** 营养素类型枚举 */
enum class NutrientType(
    val label: String,
    val unit: String,
    val color: Color
) {
    // 核心营养素（默认展示）
    PROTEIN("蛋白质", "g", Color(0xFF10B981)),
    FAT("脂肪", "g", Color(0xFFF59E0B)),
    CARBS("碳水化合物", "g", Color(0xFF3B82F6)),

    // 扩展营养素（用户可选）
    DIETARY_FIBER("膳食纤维", "g", Color(0xFF8D6E63)),
    SODIUM("钠", "mg", Color(0xFFEF4444)),
    VITAMIN_A("维生素A", "μg", Color(0xFFFF9800)),
    VITAMIN_C("维生素C", "mg", Color(0xFFFFC107)),
    CALCIUM("钙", "mg", Color(0xFF9C27B0)),
    IRON("铁", "mg", Color(0xFF795548)),
    POTASSIUM("钾", "mg", Color(0xFF00BCD4))
}

/** 营养素展示项 */
data class NutrientDisplayItem(
    val type: NutrientType,
    val value: Float,
    val dailyRecommended: Float  // 每日推荐摄入量
)

/** 获取默认展示的核心营养素 */
fun getDefaultNutrientTypes(): List<NutrientType> {
    return listOf(NutrientType.PROTEIN, NutrientType.FAT, NutrientType.CARBS)
}

/** 获取扩展营养素（用户可自选） */
fun getExtendedNutrientTypes(): List<NutrientType> {
    return listOf(
        NutrientType.DIETARY_FIBER,
        NutrientType.SODIUM,
        NutrientType.VITAMIN_A,
        NutrientType.VITAMIN_C,
        NutrientType.CALCIUM,
        NutrientType.IRON,
        NutrientType.POTASSIUM
    )
}

/** 获取所有营养素类型 */
fun getAllNutrientTypes(): List<NutrientType> {
    return getDefaultNutrientTypes() + getExtendedNutrientTypes()
}

/**
 * 基于菜品基础营养数据估算扩展营养素含量
 *
 * 真实场景应由后端RAG检索《中国食物成分表》返回精确数据，
 * 此处基于食物类型关键词和基础营养素比例进行合理估算。
 *
 * @param foodName 菜品名称（用于食物类型推断）
 * @param calories 热量（千卡）
 * @param protein 蛋白质（g）
 * @param fat 脂肪（g）
 * @param carbs 碳水化合物（g）
 * @param selectedTypes 用户选择展示的营养素类型
 * @return 估算的营养素展示项列表
 */
fun estimateExtendedNutrients(
    foodName: String,
    calories: Int,
    protein: Float,
    fat: Float,
    carbs: Float,
    selectedTypes: List<NutrientType>
): List<NutrientDisplayItem> {
    if (selectedTypes.isEmpty()) return emptyList()

    // 食物类型推断
    val isVegetable = foodName.contains("菜") || foodName.contains("瓜") ||
            foodName.contains("茄") || foodName.contains("豆芽") ||
            foodName.contains("白菜") || foodName.contains("萝卜") ||
            foodName.contains("沙拉") || foodName.contains("凉拌")
    val isMeat = foodName.contains("肉") || foodName.contains("牛") ||
            foodName.contains("猪") || foodName.contains("羊") ||
            foodName.contains("排骨") || foodName.contains("鸡")
    val isFish = foodName.contains("鱼") || foodName.contains("虾") ||
            foodName.contains("蟹") || foodName.contains("海鲜")
    val isDairy = foodName.contains("奶") || foodName.contains("乳") ||
            foodName.contains("酸奶") || foodName.contains("芝士")
    val isGrain = foodName.contains("米") || foodName.contains("面") ||
            foodName.contains("粥") || foodName.contains("饭") ||
            foodName.contains("馒头") || foodName.contains("饼")
    val isFruit = foodName.contains("果") || foodName.contains("苹果") ||
            foodName.contains("橙") || foodName.contains("柠檬")

    // 基础系数（基于热量的比例）
    val calorieFactor = (calories / 100.0f).coerceAtLeast(0.1f)

    return selectedTypes.map { type ->
        val value = when (type) {
            NutrientType.DIETARY_FIBER -> {
                when {
                    isVegetable -> 2.5f * calorieFactor
                    isGrain -> 1.8f * calorieFactor
                    isFruit -> 2.0f * calorieFactor
                    isMeat || isFish -> 0.1f * calorieFactor
                    else -> 1.0f * calorieFactor
                }
            }
            NutrientType.SODIUM -> {
                // 大多数中式菜品含钠较高
                when {
                    foodName.contains("咸") || foodName.contains("酱") -> 800f * calorieFactor
                    foodName.contains("清蒸") || foodName.contains("白") -> 200f * calorieFactor
                    isMeat -> 500f * calorieFactor
                    isVegetable -> 300f * calorieFactor
                    else -> 400f * calorieFactor
                }
            }
            NutrientType.VITAMIN_A -> {
                when {
                    foodName.contains("胡萝卜") || foodName.contains("南瓜") -> 300f * calorieFactor
                    isVegetable -> 80f * calorieFactor
                    isMeat -> 20f * calorieFactor
                    isDairy -> 50f * calorieFactor
                    else -> 30f * calorieFactor
                }
            }
            NutrientType.VITAMIN_C -> {
                when {
                    isFruit -> 30f * calorieFactor
                    isVegetable -> 20f * calorieFactor
                    foodName.contains("番茄") || foodName.contains("西红柿") -> 25f * calorieFactor
                    foodName.contains("辣椒") -> 35f * calorieFactor
                    else -> 5f * calorieFactor
                }
            }
            NutrientType.CALCIUM -> {
                when {
                    isDairy -> 120f * calorieFactor
                    foodName.contains("豆腐") || foodName.contains("豆") -> 80f * calorieFactor
                    isVegetable -> 40f * calorieFactor
                    isFish -> 50f * calorieFactor
                    else -> 25f * calorieFactor
                }
            }
            NutrientType.IRON -> {
                when {
                    isMeat -> 2.5f * calorieFactor
                    foodName.contains("菠菜") || foodName.contains("木耳") -> 3.0f * calorieFactor
                    isVegetable -> 1.2f * calorieFactor
                    isFish -> 1.0f * calorieFactor
                    else -> 0.8f * calorieFactor
                }
            }
            NutrientType.POTASSIUM -> {
                when {
                    isFruit -> 180f * calorieFactor
                    isVegetable -> 150f * calorieFactor
                    isMeat -> 200f * calorieFactor
                    else -> 120f * calorieFactor
                }
            }
            // 核心营养素不在此处估算
            else -> 0f
        }

        val dailyRecommended = when (type) {
            NutrientType.DIETARY_FIBER -> 25.0f
            NutrientType.SODIUM -> 2000.0f
            NutrientType.VITAMIN_A -> 800.0f
            NutrientType.VITAMIN_C -> 100.0f
            NutrientType.CALCIUM -> 800.0f
            NutrientType.IRON -> 15.0f
            NutrientType.POTASSIUM -> 2000.0f
            NutrientType.PROTEIN -> 60.0f
            NutrientType.FAT -> 60.0f
            NutrientType.CARBS -> 300.0f
        }

        NutrientDisplayItem(
            type = type,
            value = value.coerceAtLeast(0f),
            dailyRecommended = dailyRecommended
        )
    }
}

/**
 * 营养素自定义展示区域 - 主组件
 *
 * 显示可选营养素芯片和选中营养素的详细数值
 */
@Composable
fun NutrientCustomDisplaySection(
    foodName: String,
    calories: Int,
    protein: Float,
    fat: Float,
    carbs: Float,
    modifier: Modifier = Modifier
) {
    var selectedTypes by remember {
        mutableStateOf(setOf<NutrientType>())
    }
    var isExpanded by remember { mutableStateOf(false) }

    val extendedItems = remember(foodName, calories, protein, fat, carbs, selectedTypes) {
        estimateExtendedNutrients(
            foodName = foodName,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            selectedTypes = selectedTypes.toList()
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行 + 展开/收起
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "自定义营养素",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 简要提示
            if (!isExpanded && selectedTypes.isEmpty()) {
                Text(
                    text = "点击展开，选择想查看的营养素（膳食纤维、钠、维生素等）",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            // 展开内容
            if (isExpanded) {
                // 营养素选择芯片
                Text(
                    text = "选择要展示的营养素：",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                NutrientTypeSelector(
                    selectedTypes = selectedTypes,
                    onTypeToggled = { type ->
                        selectedTypes = if (selectedTypes.contains(type)) {
                            selectedTypes - type
                        } else {
                            selectedTypes + type
                        }
                    }
                )
            }

            // 选中营养素的展示
            if (extendedItems.isNotEmpty()) {
                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp
                )

                extendedItems.forEach { item ->
                    NutrientDetailRow(item = item)
                }
            }
        }
    }
}

/**
 * 营养素类型选择器
 */
@Composable
private fun NutrientTypeSelector(
    selectedTypes: Set<NutrientType>,
    onTypeToggled: (NutrientType) -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedTypes = getExtendedNutrientTypes()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        extendedTypes.forEach { type ->
            val isSelected = selectedTypes.contains(type)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) type.color else Color(0xFFF5F5F5),
                label = "nutrientChipBg_${type.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                label = "nutrientChipText_${type.name}"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTypeToggled(type) },
                color = bgColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textColor
                        )
                    }
                    Text(
                        text = type.label,
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 单个营养素详细展示行
 */
@Composable
private fun NutrientDetailRow(
    item: NutrientDisplayItem,
    modifier: Modifier = Modifier
) {
    val percentage = if (item.dailyRecommended > 0f) {
        (item.value / item.dailyRecommended).coerceIn(0f, 1.5f)
    } else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 色点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(item.type.color)
        )

        // 名称
        Text(
            text = item.type.label,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.width(72.dp)
        )

        // 进度条
        Box(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = item.type.color,
                trackColor = item.type.color.copy(alpha = 0.12f)
            )
        }

        // 数值
        Text(
            text = formatNutrientValue(item.value, item.type.unit),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.width(64.dp)
        )

        // 占比
        Text(
            text = "${(percentage * 100).toInt()}%",
            fontSize = 11.sp,
            color = if (percentage > 1f) Color(0xFFEF4444) else TextTertiary,
            modifier = Modifier.width(36.dp)
        )
    }
}

/** 格式化营养素数值 */
private fun formatNutrientValue(value: Float, unit: String): String {
    return if (value >= 100) {
        "${value.toInt()}$unit"
    } else if (value >= 1) {
        String.format("%.1f$unit", value)
    } else {
        String.format("%.2f$unit", value)
    }
}
