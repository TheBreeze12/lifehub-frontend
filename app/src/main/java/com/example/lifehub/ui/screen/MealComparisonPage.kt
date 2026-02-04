package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.BeforeMealData
import com.example.lifehub.data.DishFeature
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.viewmodel.BeforeMealUploadState
import com.example.lifehub.viewmodel.FoodViewModel

private val ForestGreen = Color(0xFF2D5A27)
private val VitalOrange = Color(0xFFFF6B35)
private val BackgroundBeige = Color(0xFFF8F5F0)
private val CardBackground = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)

/**
 * 餐前餐后对比主页面
 * Phase 13: 餐前拍摄功能
 * 用户可以从此页面开始餐前拍摄流程，查看拍摄结果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealComparisonPage(navController: NavController) {
    val foodViewModel: FoodViewModel = viewModel()
    val beforeMealState by foodViewModel.beforeMealUploadState.collectAsState()
    val comparisonRecord by foodViewModel.currentComparisonRecord.collectAsState()

    // 检查用户登录状态
    val userId = try {
        if (UserSession.isLoggedIn()) UserSession.getUserId() else null
    } catch (e: Exception) {
        null
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    "餐前餐后对比",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ForestGreen
                        )
                )
            },
            containerColor = BackgroundBeige
    ) { innerPadding ->
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 未登录提示
            if (userId == null) {
                NotLoggedInContent(navController)
                return@Scaffold
            }

            // 功能介绍卡片
            FeatureIntroCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 根据状态显示不同内容
            when (val state = beforeMealState) {
                is BeforeMealUploadState.Idle -> {
                    // 显示开始拍摄按钮
                    StartCaptureSection(navController)
                }
                is BeforeMealUploadState.Loading -> {
                    // 显示加载中
                    LoadingSection()
                }
                is BeforeMealUploadState.Success -> {
                    // 显示餐前分析结果
                    BeforeMealResultSection(
                            data = state.data,
                            onContinueToAfter = {
                                // TODO: Phase 14 - 导航到餐后拍摄页面
                            },
                            onRetake = {
                                foodViewModel.resetBeforeMealUploadState()
                            }
                    )
                }
                is BeforeMealUploadState.Error -> {
                    // 显示错误
                    ErrorSection(
                            message = state.message,
                            onRetry = {
                                foodViewModel.resetBeforeMealUploadState()
                            }
                    )
                }
            }
        }
    }
}

/** 未登录提示内容 */
@Composable
private fun NotLoggedInContent(navController: NavController) {
    Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = "请先登录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = "登录后即可使用餐前餐后对比功能",
                fontSize = 14.sp,
                color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
                onClick = { navController.navigate(Screen.Login.route) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("去登录", color = Color.White)
        }
    }
}

/** 功能介绍卡片 */
@Composable
private fun FeatureIntroCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = "智能餐前餐后对比",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "通过AI分析餐前和餐后照片，精准计算您的实际摄入量",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 步骤说明
            StepItem(number = 1, title = "拍摄餐前照片", description = "AI识别菜品并估算热量")
            Spacer(modifier = Modifier.height(8.dp))
            StepItem(number = 2, title = "拍摄餐后照片", description = "AI对比计算实际摄入")
            Spacer(modifier = Modifier.height(8.dp))
            StepItem(number = 3, title = "查看净摄入", description = "精准记录您的饮食数据")
        }
    }
}

/** 步骤项 */
@Composable
private fun StepItem(number: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
                modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VitalOrange),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    text = number.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
            )
            Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
            )
        }
    }
}

/** 开始拍摄区域 */
@Composable
private fun StartCaptureSection(navController: NavController) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = ForestGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "开始餐前拍摄",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "拍摄您的餐前食物照片\nAI将自动识别菜品并估算热量",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = {
                        // 导航到餐前拍摄相机页面
                        navController.navigate(Screen.BeforeMealCamera.route)
                    },
                    modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VitalOrange)
            ) {
                Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "拍摄餐前照片",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 拍摄提示
    CaptureGuidelinesCard()
}

/** 拍摄引导提示卡片 */
@Composable
private fun CaptureGuidelinesCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = VitalOrange,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "拍摄小贴士",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GuidelineItem(icon = Icons.Default.CenterFocusStrong, text = "将食物居中放置在画面中央")
            Spacer(modifier = Modifier.height(6.dp))
            GuidelineItem(icon = Icons.Default.WbSunny, text = "确保光线充足，避免阴影遮挡")
            Spacer(modifier = Modifier.height(6.dp))
            GuidelineItem(icon = Icons.Default.Straighten, text = "垂直俯拍，保持适当距离")
        }
    }
}

/** 拍摄指南项 */
@Composable
private fun GuidelineItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
                icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text = text,
                fontSize = 13.sp,
                color = TextSecondary
        )
    }
}

/** 加载中区域 */
@Composable
private fun LoadingSection() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                    color = VitalOrange,
                    modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                    text = "AI正在分析餐前照片...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "正在识别菜品并估算热量，请稍候",
                    fontSize = 14.sp,
                    color = TextSecondary
            )
        }
    }
}

/** 餐前分析结果区域 */
@Composable
private fun BeforeMealResultSection(
        data: BeforeMealData,
        onContinueToAfter: () -> Unit,
        onRetake: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 成功标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "餐前分析完成",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 总热量
            data.beforeFeatures?.let { features ->
                TotalCaloriesCard(
                        calories = features.totalEstimatedCalories ?: 0.0,
                        protein = features.totalEstimatedProtein ?: 0.0,
                        fat = features.totalEstimatedFat ?: 0.0,
                        carbs = features.totalEstimatedCarbs ?: 0.0
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 菜品列表
                Text(
                        text = "识别到的菜品",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                features.dishes?.forEach { dish ->
                    DishFeatureItem(dish)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 操作按钮
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Text("重新拍摄", color = TextSecondary)
                }

                Button(
                        onClick = onContinueToAfter,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VitalOrange),
                        enabled = false // Phase 14才启用
                ) {
                    Text("用餐后继续", color = Color.White)
                }
            }

            // 提示
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    text = "* 餐后拍摄功能将在后续版本中开放",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
            )
        }
    }
}

/** 总热量卡片 */
@Composable
private fun TotalCaloriesCard(
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F5F5)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = "估算总热量",
                    fontSize = 14.sp,
                    color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                    verticalAlignment = Alignment.Bottom
            ) {
                Text(
                        text = String.format("%.0f", calories),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalOrange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                        text = "kcal",
                        fontSize = 16.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 营养素分布
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientItem(label = "蛋白质", value = protein, unit = "g")
                NutrientItem(label = "脂肪", value = fat, unit = "g")
                NutrientItem(label = "碳水", value = carbs, unit = "g")
            }
        }
    }
}

/** 营养素项 */
@Composable
private fun NutrientItem(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
        )
        Text(
                text = "${String.format("%.1f", value)}$unit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
        )
    }
}

/** 菜品特征项 */
@Composable
private fun DishFeatureItem(dish: DishFeature) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF9F9F9)
    ) {
        Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = dish.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                )
                dish.estimatedWeight?.let {
                    Text(
                            text = "约${it}g",
                            fontSize = 12.sp,
                            color = TextSecondary
                    )
                }
            }
            Text(
                    text = "${String.format("%.0f", dish.estimatedCalories ?: 0.0)} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = VitalOrange
            )
        }
    }
}

/** 错误区域 */
@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = "分析失败",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = VitalOrange),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text("重试", color = Color.White)
            }
        }
    }
}
