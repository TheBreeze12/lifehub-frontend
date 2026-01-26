package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.lifehub.ui.theme.ForestGreen

/** 营养详情页面 展示单个菜品的详细营养成分和AI推荐理由 */
@Composable
fun NutritionDetailPage(dishName: String, navController: NavController) {
    // 模拟营养数据（实际应该从API获取）
    val nutritionData =
            NutritionData(
                    name = dishName,
                    emoji =
                            when (dishName) {
                                "凯撒沙拉" -> "🥗"
                                "烤三文鱼" -> "🐟"
                                "奶油培根意面" -> "🍝"
                                else -> "🍽️"
                            },
                    calories = 180,
                    protein = 12f,
                    fat = 8f,
                    carbs = 15f,
                    isRecommended = true,
                    tags = listOf("低脂", "高蛋白"),
                    aiReason = "根据您的「减脂」目标，这道菜热量适中、蛋白质丰富，非常适合作为午餐选择。"
            )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 顶部展示区域
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(256.dp)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(Color(0xFFFFE5D9), Color(0xFFF8F5F0))
                                        )
                                )
        ) {
            // 返回按钮
            IconButton(
                    onClick = { navController.navigateUp() },
                    modifier =
                            Modifier.align(Alignment.TopStart)
                                    .padding(24.dp)
                                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color(0xFF1F2937)) }

            Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = nutritionData.emoji, fontSize = 72.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = nutritionData.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nutritionData.isRecommended) {
                        TagChip("✓ 推荐", Color(0xFFD1FAE5), Color(0xFF059669))
                    }
                    nutritionData.tags.forEach { tag ->
                        TagChip(tag, Color(0xFFE5E7EB), Color(0xFF6B7280))
                    }
                }
            }
        }

        // 营养信息卡片
        Column(modifier = Modifier.padding(24.dp)) {
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "能量值", fontSize = 12.sp, color = Color(0xFF9CA3AF))

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                                text = "${nutritionData.calories}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = "千卡",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutritionItem("蛋白质", "${nutritionData.protein}g", Color(0xFF10B981))
                        NutritionItem("脂肪", "${nutritionData.fat}g", Color(0xFFF59E0B))
                        NutritionItem("碳水", "${nutritionData.carbs}g", Color(0xFF3B82F6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI推荐理由卡片
            Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD4EDDA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3E6CB))
            ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "推荐理由",
                            tint = Color(0xFF155724),
                            modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                                text = "AI 推荐理由",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF155724)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = nutritionData.aiReason,
                                fontSize = 11.sp,
                                color = Color(0xFF155724),
                                lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 添加到饮食记录按钮
            Button(
                    onClick = {
                        // TODO: 添加到饮食记录
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) { Text(text = "✓ 添加到今日饮食记录", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** 标签芯片组件 */
@Composable
fun TagChip(text: String, backgroundColor: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = backgroundColor) {
        Text(
                text = text,
                fontSize = 12.sp,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** 营养素展示组件 */
@Composable
fun NutritionItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF9CA3AF))
    }
}

/** 营养数据类 */
data class NutritionData(
        val name: String,
        val emoji: String,
        val calories: Int,
        val protein: Float,
        val fat: Float,
        val carbs: Float,
        val isRecommended: Boolean,
        val tags: List<String>,
        val aiReason: String
)
