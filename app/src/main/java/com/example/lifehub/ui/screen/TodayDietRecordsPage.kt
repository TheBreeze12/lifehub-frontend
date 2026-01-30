package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.FoodViewModel

/** 今日饮食记录页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayDietRecordsPage(navController: NavController, foodViewModel: FoodViewModel = viewModel()) {
    val userId =
            try {
                if (UserSession.isLoggedIn()) UserSession.getUserId() else null
            } catch (e: Exception) {
                null
            }

    val todayDietRecordsState by foodViewModel.todayDietRecordsState.collectAsState()

    // 加载今日饮食记录
    LaunchedEffect(userId) { userId?.let { foodViewModel.getTodayDietRecords(it) } }

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(
                            brush = Brush.verticalGradient(
                                    colors = listOf(
                                            Color.White,
                                            BackgroundGradientStart,
                                            BackgroundBeige
                                    )
                            )
                    )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部工具栏
            TopAppBar(
                    title = {
                        Text(
                                "今日饮食记录",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Box(
                                    modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ForestGreenLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "返回",
                                        tint = ForestGreenDark
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                    )
            )

            when (val state = todayDietRecordsState) {
                is com.example.lifehub.viewmodel.DietRecordsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
                is com.example.lifehub.viewmodel.DietRecordsState.Success -> {
                    val todayRecords = state.records.values.flatten()

                    if (todayRecords.isEmpty()) {
                        // 空状态
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                        modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors = listOf(
                                                                        ForestGreenLight.copy(alpha = 0.4f),
                                                                        ForestGreen.copy(alpha = 0.3f)
                                                                )
                                                        )
                                                ),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                            Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = ForestGreen,
                                            modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                        text = "今天还没有饮食记录",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "快去添加你的第一餐吧！",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                )
                            }
                        }
                    } else {
                        // 显示今日营养统计
                        Card(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = ForestGreen.copy(alpha = 0.1f)
                                        ),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                            modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(ForestGreenLight.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                                Icons.Default.Restaurant,
                                                contentDescription = null,
                                                tint = ForestGreenDark,
                                                modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                            text = "今日营养统计",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val totalCalories =
                                        todayRecords
                                                .fold(0.0) { acc, record -> acc + record.calories }
                                                .toInt()
                                val totalProtein =
                                        todayRecords.fold(0.0) { acc, record ->
                                            acc + record.protein
                                        }
                                val totalFat =
                                        todayRecords.fold(0.0) { acc, record -> acc + record.fat }
                                val totalCarbs =
                                        todayRecords.fold(0.0) { acc, record -> acc + record.carbs }

                                NutritionStatItem("🔥 总热量", "${totalCalories} 千卡", CaloriesColor)
                                Spacer(modifier = Modifier.height(10.dp))
                                NutritionStatItem(
                                        "💪 蛋白质",
                                        "${String.format("%.1f", totalProtein)} g",
                                        ProteinColor
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NutritionStatItem(
                                        "🥑 脂肪",
                                        "${String.format("%.1f", totalFat)} g",
                                        FatColor
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NutritionStatItem(
                                        "🍞 碳水",
                                        "${String.format("%.1f", totalCarbs)} g",
                                        CarbsColor
                                )
                            }
                        }

                        // 显示饮食记录列表
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) { items(todayRecords) { record -> DietRecordCard(record = record) } }
                    }
                }
                is com.example.lifehub.viewmodel.DietRecordsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                    text = "加载失败",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ErrorRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    text = state.message,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForestGreen)
                    }
                }
            }
        }
    }
}

/** 营养统计项 */
@Composable
fun NutritionStatItem(label: String, value: String, color: Color) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = label,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
        )
        Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
        )
    }
}

/** 饮食记录卡片 */
@Composable
fun DietRecordCard(record: DietRecord) {
    Card(
            modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.05f)
                    ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = record.foodName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )
                MealTypeChip(record.mealType)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionInfoItem("热量", "${record.calories.toInt()} 千卡", CaloriesColor)
                NutritionInfoItem("蛋白质", "${String.format("%.1f", record.protein)} g", ProteinColor)
                NutritionInfoItem("脂肪", "${String.format("%.1f", record.fat)} g", FatColor)
                NutritionInfoItem("碳水", "${String.format("%.1f", record.carbs)} g", CarbsColor)
            }
        }
    }
}

/** 餐次标签 */
@Composable
fun MealTypeChip(mealType: String) {
    val (text, bgColor, textColor) =
            when (mealType.lowercase()) {
                "breakfast", "早餐" -> Triple("🌅 早餐", VitalOrangeLight.copy(alpha = 0.2f), VitalOrange)
                "lunch", "午餐" -> Triple("☀️ 午餐", SkyBlueLight.copy(alpha = 0.2f), SkyBlue)
                "dinner", "晚餐" -> Triple("🌙 晚餐", LavenderPurple.copy(alpha = 0.2f), LavenderPurple)
                "snack", "加餐" -> Triple("🍎 加餐", ForestGreenLight.copy(alpha = 0.2f), ForestGreen)
                else -> Triple(mealType, TextTertiary.copy(alpha = 0.2f), TextSecondary)
            }

    Surface(
            shape = RoundedCornerShape(10.dp),
            color = bgColor
    ) {
        Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** 营养信息项 */
@Composable
fun NutritionInfoItem(label: String, value: String, color: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
        )
    }
}
