package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.RecommendedFood
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.RecommendationState

/** Phase 42: 个性化菜品推荐页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationPage(
    navController: NavController,
    foodViewModel: FoodViewModel = viewModel()
) {
    val recommendationState by foodViewModel.recommendationState.collectAsState()

    // 当前选中的餐次
    var selectedMealType by remember { mutableStateOf("lunch") }
    val mealTypes = listOf(
        "breakfast" to "早餐",
        "lunch" to "午餐",
        "dinner" to "晚餐",
        "snack" to "加餐"
    )

    // 获取用户ID
    val userId = try {
        UserSession.getUserId()
    } catch (e: Exception) {
        null
    }

    // 页面加载时获取推荐
    LaunchedEffect(userId, selectedMealType) {
        if (userId != null) {
            foodViewModel.getFoodRecommendations(
                userId = userId,
                mealType = selectedMealType,
                limit = 6
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "智能推荐",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundGradientStart,
                            BackgroundBeige,
                            BackgroundGradientEnd.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            if (userId == null) {
                // 未登录提示
                NotLoggedInHint()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 餐次选择器
                    item {
                        MealTypeSelector(
                            mealTypes = mealTypes,
                            selectedMealType = selectedMealType,
                            onMealTypeSelected = { selectedMealType = it }
                        )
                    }

                    // 推荐概要信息
                    item {
                        when (val state = recommendationState) {
                            is RecommendationState.Success -> {
                                RecommendationSummaryCard(
                                    healthGoalLabel = state.data.healthGoalLabel,
                                    remainingCalories = state.data.remainingCalories,
                                    dailyCalorieTarget = state.data.dailyCalorieTarget,
                                    recommendationCount = state.data.recommendations.size
                                )
                            }
                            else -> { /* 不显示 */ }
                        }
                    }

                    // 推荐内容
                    when (val state = recommendationState) {
                        is RecommendationState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(color = ForestGreen)
                                        Text(
                                            "正在为您智能推荐...",
                                            color = TextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        is RecommendationState.Success -> {
                            if (state.data.recommendations.isEmpty()) {
                                item {
                                    EmptyRecommendationCard()
                                }
                            } else {
                                items(state.data.recommendations) { food ->
                                    RecommendationFoodCard(food = food)
                                }
                            }
                        }
                        is RecommendationState.Error -> {
                            item {
                                ErrorCard(
                                    message = state.message,
                                    onRetry = {
                                        foodViewModel.getFoodRecommendations(
                                            userId = userId,
                                            mealType = selectedMealType,
                                            limit = 6
                                        )
                                    }
                                )
                            }
                        }
                        is RecommendationState.Idle -> { /* 不显示 */ }
                    }
                }
            }
        }
    }
}

/** 餐次选择器 */
@Composable
private fun MealTypeSelector(
    mealTypes: List<Pair<String, String>>,
    selectedMealType: String,
    onMealTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mealTypes.forEach { (type, label) ->
            val isSelected = type == selectedMealType
            FilterChip(
                selected = isSelected,
                onClick = { onMealTypeSelected(type) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ForestGreenLight.copy(alpha = 0.5f),
                    selectedBorderColor = ForestGreen,
                    enabled = true,
                    selected = isSelected
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 推荐概要信息卡片 */
@Composable
private fun RecommendationSummaryCard(
    healthGoalLabel: String,
    remainingCalories: Double,
    dailyCalorieTarget: Double,
    recommendationCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ForestGreen.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 健康目标
            Column {
                Text(
                    text = "当前目标",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ForestGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = healthGoalLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 剩余热量
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "剩余热量",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${remainingCalories.toInt()} kcal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalOrange
                )
            }

            // 推荐数量
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "已推荐",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recommendationCount}道菜",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
            }
        }
    }
}

/** 推荐菜品卡片 */
@Composable
private fun RecommendationFoodCard(food: RecommendedFood) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ForestGreen.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 菜品名称 + 评分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = food.foodName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 评分徽章
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        food.score >= 85 -> ForestGreen.copy(alpha = 0.15f)
                        food.score >= 70 -> VitalOrange.copy(alpha = 0.15f)
                        else -> TextSecondary.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = "${food.score.toInt()}分",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            food.score >= 85 -> ForestGreenDark
                            food.score >= 70 -> VitalOrange
                            else -> TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标签
            if (food.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    food.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tagColor(tag).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = tagColor(tag)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 营养数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NutrientChip(
                    label = "热量",
                    value = "${food.calories.toInt()}",
                    unit = "kcal",
                    color = CaloriesColor
                )
                NutrientChip(
                    label = "蛋白质",
                    value = String.format("%.1f", food.protein),
                    unit = "g",
                    color = ProteinColor
                )
                NutrientChip(
                    label = "脂肪",
                    value = String.format("%.1f", food.fat),
                    unit = "g",
                    color = FatColor
                )
                NutrientChip(
                    label = "碳水",
                    value = String.format("%.1f", food.carbs),
                    unit = "g",
                    color = CarbsColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 推荐理由
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ForestGreenLight.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = ForestGreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = food.reason,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/** 营养素数值展示 */
@Composable
private fun NutrientChip(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = unit,
            fontSize = 10.sp,
            color = TextTertiary
        )
    }
}

/** 根据标签返回对应颜色 */
private fun tagColor(tag: String): Color {
    return when {
        tag.contains("蛋白") -> ProteinColor
        tag.contains("低脂") || tag.contains("脂肪") -> FatColor
        tag.contains("低碳") || tag.contains("碳水") -> CarbsColor
        tag.contains("低热量") || tag.contains("热量") -> CaloriesColor
        tag.contains("高纤") || tag.contains("纤维") -> ForestGreenDark
        else -> LavenderPurple
    }
}

/** 空推荐卡片 */
@Composable
private fun EmptyRecommendationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无推荐菜品",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "请先完善您的健康目标和偏好设置",
                fontSize = 13.sp,
                color = TextTertiary
            )
        }
    }
}

/** 错误卡片 */
@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("重试")
            }
        }
    }
}

/** 未登录提示 */
@Composable
private fun NotLoggedInHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "请先登录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "登录后即可获取个性化推荐",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}
