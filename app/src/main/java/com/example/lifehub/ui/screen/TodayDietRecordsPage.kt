package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.BackgroundBeige
import com.example.lifehub.ui.theme.ForestGreen
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

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("今日饮食记录", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
    ) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().background(BackgroundBeige).padding(paddingValues)
        ) {
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
                                Text(
                                        text = "今天还没有饮食记录",
                                        fontSize = 16.sp,
                                        color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "快去添加你的第一餐吧！",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    } else {
                        // 显示今日营养统计
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                        text = "今日营养统计",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

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

                                NutritionStatItem("总热量", "${totalCalories} 千卡", Color(0xFFEF4444))
                                Spacer(modifier = Modifier.height(8.dp))
                                NutritionStatItem(
                                        "蛋白质",
                                        "${String.format("%.1f", totalProtein)} g",
                                        Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NutritionStatItem(
                                        "脂肪",
                                        "${String.format("%.1f", totalFat)} g",
                                        Color(0xFFF59E0B)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NutritionStatItem(
                                        "碳水",
                                        "${String.format("%.1f", totalCarbs)} g",
                                        Color(0xFF3B82F6)
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
                            Text(text = "加载失败", fontSize = 16.sp, color = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, fontSize = 12.sp, color = Color(0xFF6B7280))
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
        Text(text = label, fontSize = 12.sp, color = Color(0xFF6B7280))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** 饮食记录卡片 */
@Composable
fun DietRecordCard(record: DietRecord) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        color = Color(0xFF1F2937)
                )
                MealTypeChip(record.mealType)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NutritionInfoItem("热量", "${record.calories.toInt()} 千卡")
                NutritionInfoItem("蛋白质", "${String.format("%.1f", record.protein)} g")
                NutritionInfoItem("脂肪", "${String.format("%.1f", record.fat)} g")
                NutritionInfoItem("碳水", "${String.format("%.1f", record.carbs)} g")
            }
        }
    }
}

/** 餐次标签 */
@Composable
fun MealTypeChip(mealType: String) {
    val (text, color) =
            when (mealType.lowercase()) {
                "breakfast", "早餐" -> "早餐" to Color(0xFFFFF4E6)
                "lunch", "午餐" -> "午餐" to Color(0xFFE6F4FF)
                "dinner", "晚餐" -> "晚餐" to Color(0xFFF0E6FF)
                "snack", "加餐" -> "加餐" to Color(0xFFE6FFE6)
                else -> mealType to Color(0xFFF3F4F6)
            }

    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Text(
                text = text,
                fontSize = 10.sp,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** 营养信息项 */
@Composable
fun NutritionInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF9CA3AF))
    }
}
