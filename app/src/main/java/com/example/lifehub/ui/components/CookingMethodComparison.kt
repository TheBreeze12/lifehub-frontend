package com.example.lifehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.data.CookingMethodItem
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.ui.theme.ForestGreenLight
import com.example.lifehub.ui.theme.VitalOrange

/**
 * Phase 50: 烹饪方式热量差异对比组件
 * 展示同一食材在不同烹饪方式下的热量和脂肪差异
 */
@Composable
fun CookingMethodComparisonSection(
        cookingMethods: List<CookingMethodItem>,
        modifier: Modifier = Modifier
) {
        if (cookingMethods.isEmpty()) return

        val minCalories = cookingMethods.minOf { it.calories }
        val maxCalories = cookingMethods.maxOf { it.calories }
        val range = if (maxCalories > minCalories) (maxCalories - minCalories) else 1.0

        Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = "烹饪方式对比",
                                        tint = VitalOrange,
                                        modifier = Modifier.size(20.dp)
                                )
                                Text(
                                        text = "烹饪方式热量对比",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                                text = "同一食材，不同烹饪方式热量差异明显",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        cookingMethods.sortedBy { it.calories }.forEach { item ->
                                CookingMethodBar(
                                        item = item,
                                        minCalories = minCalories,
                                        range = range,
                                        isLowest = item.calories == minCalories
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                        }
                }
        }
}

@Composable
private fun CookingMethodBar(
        item: CookingMethodItem,
        minCalories: Double,
        range: Double,
        isLowest: Boolean
) {
        val fraction = ((item.calories - minCalories) / range).toFloat().coerceIn(0.15f, 1f)
        val barColor = if (isLowest) ForestGreen else {
                val ratio = ((item.calories - minCalories) / range).toFloat()
                when {
                        ratio < 0.4f -> ForestGreenLight
                        ratio < 0.7f -> VitalOrange
                        else -> Color(0xFFF87171)
                }
        }

        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                                Text(
                                        text = item.method,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF374151)
                                )
                                if (isLowest) {
                                        Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ForestGreen.copy(alpha = 0.15f)
                                        ) {
                                                Text(
                                                        text = "推荐",
                                                        fontSize = 10.sp,
                                                        color = ForestGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                        }
                                }
                        }

                        Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Text(
                                        text = "脂肪 ${"%.1f".format(item.fat)}g",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF)
                                )
                                Text(
                                        text = "${"%.0f".format(item.calories)} kcal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                )
                        }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                ) {
                        Box(
                                modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(8.dp)
                                        .background(barColor, RoundedCornerShape(4.dp))
                        )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                        text = item.description,
                        fontSize = 10.sp,
                        color = Color(0xFFB0B8C4)
                )
        }
}
