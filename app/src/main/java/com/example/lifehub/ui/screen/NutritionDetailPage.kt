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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.data.CookingMethodItem
import com.example.lifehub.ui.components.AllergenHighlightSection
import com.example.lifehub.ui.components.AllergenWarningDialog
import com.example.lifehub.ui.components.CookingMethodComparisonSection
import com.example.lifehub.ui.components.matchUserAllergens
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.MenuRecognitionState
import com.example.lifehub.viewmodel.UserPreferencesState
import com.example.lifehub.viewmodel.UserViewModel

/** 营养详情页面 展示单个菜品的详细营养成分和AI推荐理由 */
@Composable
fun NutritionDetailPage(dishName: String, navController: NavController) {
        val foodViewModel: FoodViewModel = viewModel()
        val userViewModel: UserViewModel = viewModel()
        val recognitionState by foodViewModel.recognitionState.collectAsState()
        val userPreferencesState by userViewModel.userPreferencesState.collectAsState()

        // 过敏原预警弹窗状态
        var showAllergenWarning by remember { mutableStateOf(false) }
        var pendingAddRecord by remember { mutableStateOf(false) }

        // 获取用户ID
        val userId =
                try {
                        if (UserSession.isLoggedIn()) UserSession.getUserId() else null
                } catch (e: Exception) {
                        null
                }

        // 加载最新识别结果和用户偏好
        LaunchedEffect(Unit) {
                foodViewModel.getLatestRecognition(userId)
                userId?.let { userViewModel.getUserPreferences(it) }
        }

        // 获取用户过敏原列表
        val userAllergens = when (val state = userPreferencesState) {
                is UserPreferencesState.Success -> state.data.allergens ?: emptyList()
                else -> emptyList()
        }

        // 从识别结果中查找对应的菜品
        val dishItem =
                when (val state = recognitionState) {
                        is MenuRecognitionState.Success -> {
                                state.dishes.find { it.name == dishName }
                        }
                        else -> null
                }

        // 构建营养数据（使用后端数据或默认值）
        val nutritionData =
                if (dishItem != null) {
                        // 根据菜品名称生成emoji
                        val emoji =
                                when {
                                        dishItem.name.contains("牛") ||
                                                dishItem.name.contains("肉") -> "🥩"
                                        dishItem.name.contains("鸡") ||
                                                dishItem.name.contains("蛋") -> "🍳"
                                        dishItem.name.contains("鱼") -> "🐟"
                                        dishItem.name.contains("菜") ||
                                                dishItem.name.contains("白菜") ||
                                                dishItem.name.contains("豆芽") -> "🥬"
                                        dishItem.name.contains("豆腐") -> "🧈"
                                        else -> "🍽️"
                                }

                        // 生成标签
                        val tags = mutableListOf<String>()
                        if (dishItem.isRecommended) {
                                tags.add("推荐")
                        }
                        if (dishItem.protein > 20) tags.add("高蛋白")
                        if (dishItem.fat < 10) tags.add("低脂")
                        if (dishItem.calories < 200) tags.add("低卡")

                        // 基于菜名检测过敏原（8大类标准过敏原 + 用户自定义过敏原）
                        val detectedAllergens = detectAllergensFromDishName(
                                dishItem.name, userAllergens
                        )
                        val allergenReasoning = if (detectedAllergens.isNotEmpty()) {
                                "菜品“${dishItem.name}”可能含有过敏原成分：${detectedAllergens.joinToString("、") { com.example.lifehub.ui.components.getAllergenDisplayName(it) }}。请根据个人情况谨慎食用。"
                        } else null

                        NutritionData(
                                name = dishItem.name,
                                emoji = emoji,
                                calories = dishItem.calories.toInt(),
                                protein = dishItem.protein.toFloat(),
                                fat = dishItem.fat.toFloat(),
                                carbs = dishItem.carbs.toFloat(),
                                isRecommended = dishItem.isRecommended,
                                tags = tags,
                                aiReason = dishItem.reason ?: "暂无推荐理由",
                                allergens = detectedAllergens,
                                allergenReasoning = allergenReasoning,
                                cookingMethodComparisons = dishItem.cookingMethodComparisons
                        )
                } else {
                        // 如果找不到，使用默认数据
                        NutritionData(
                                name = dishName,
                                emoji = "🍽️",
                                calories = 0,
                                protein = 0f,
                                fat = 0f,
                                carbs = 0f,
                                isRecommended = false,
                                tags = emptyList(),
                                aiReason = "正在加载数据...",
                                allergens = emptyList(),
                                allergenReasoning = null
                        )
                }

        // 计算匹配的过敏原
        val matchedAllergens = matchUserAllergens(nutritionData.allergens, userAllergens)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // 顶部展示区域
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(256.dp)
                                        .background(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(0xFFFFE5D9),
                                                                        Color(0xFFF8F5F0)
                                                                )
                                                )
                                        )
                ) {
                        // 返回按钮
                        IconButton(
                                onClick = { navController.navigateUp() },
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(24.dp)
                                                .background(
                                                        Color.White.copy(alpha = 0.5f),
                                                        CircleShape
                                                )
                        ) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "返回",
                                        tint = Color(0xFF1F2937)
                                )
                        }

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
                                                TagChip(
                                                        "✓ 推荐",
                                                        Color(0xFFD1FAE5),
                                                        Color(0xFF059669)
                                                )
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
                                        Text(
                                                text = "能量值",
                                                fontSize = 12.sp,
                                                color = Color(0xFF9CA3AF)
                                        )

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
                                                NutritionItem(
                                                        "蛋白质",
                                                        "${nutritionData.protein}g",
                                                        Color(0xFF10B981)
                                                )
                                                NutritionItem(
                                                        "脂肪",
                                                        "${nutritionData.fat}g",
                                                        Color(0xFFF59E0B)
                                                )
                                                NutritionItem(
                                                        "碳水",
                                                        "${nutritionData.carbs}g",
                                                        Color(0xFF3B82F6)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI推荐理由卡片
                        Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFD4EDDA),
                                border =
                                        androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Color(0xFFC3E6CB)
                                        )
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

                        // 过敏原高亮展示区域
                        if (nutritionData.allergens.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                AllergenHighlightSection(
                                        detectedAllergens = nutritionData.allergens,
                                        matchedAllergens = matchedAllergens,
                                        allergenReasoning = nutritionData.allergenReasoning
                                )
                        }

                        // Phase 50: 烹饪方式热量差异对比
                        if (!nutritionData.cookingMethodComparisons.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CookingMethodComparisonSection(
                                        cookingMethods = nutritionData.cookingMethodComparisons
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 添加到饮食记录按钮
                        val addRecordState by foodViewModel.addDietRecordState.collectAsState()

                        // 监听添加记录状态
                        LaunchedEffect(addRecordState) {
                                when (addRecordState) {
                                        is com.example.lifehub.viewmodel.AddDietRecordState.Success -> {
                                                // 添加成功，返回上一页
                                                navController.navigateUp()
                                        }
                                        is com.example.lifehub.viewmodel.AddDietRecordState.Error -> {
                                                // 错误已在按钮中显示
                                        }
                                        else -> {}
                                }
                        }

                        // 实际添加记录的逻辑
                        fun doAddRecord() {
                                if (userId != null && dishItem != null) {
                                        val currentHour =
                                                java.util.Calendar.getInstance()
                                                        .get(java.util.Calendar.HOUR_OF_DAY)
                                        val mealType =
                                                when {
                                                        currentHour < 10 -> "breakfast"
                                                        currentHour < 15 -> "lunch"
                                                        currentHour < 20 -> "dinner"
                                                        else -> "snack"
                                                }

                                        foodViewModel.addDietRecord(
                                                userId = userId,
                                                foodName = dishItem.name,
                                                calories = dishItem.calories.toDouble(),
                                                protein = dishItem.protein.toDouble(),
                                                fat = dishItem.fat.toDouble(),
                                                carbs = dishItem.carbs.toDouble(),
                                                mealType = mealType
                                        )
                                }
                        }

                        Button(
                                onClick = {
                                        // 如果有匹配的过敏原，显示预警弹窗
                                        if (matchedAllergens.isNotEmpty()) {
                                                showAllergenWarning = true
                                        } else {
                                                doAddRecord()
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                enabled =
                                        userId != null &&
                                                dishItem != null &&
                                                addRecordState !is
                                                        com.example.lifehub.viewmodel.AddDietRecordState.Loading
                        ) {
                                when (addRecordState) {
                                        is com.example.lifehub.viewmodel.AddDietRecordState.Loading -> {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color.White
                                                )
                                        }
                                        is com.example.lifehub.viewmodel.AddDietRecordState.Error -> {
                                                Text(
                                                        text = "添加失败，请重试",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                        else -> {
                                                Text(
                                                        text = "✓ 添加到今日饮食记录",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                        }

                        // 显示错误提示
                        if (addRecordState is com.example.lifehub.viewmodel.AddDietRecordState.Error
                        ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text =
                                                (addRecordState as
                                                                com.example.lifehub.viewmodel.AddDietRecordState.Error)
                                                        .message,
                                        fontSize = 12.sp,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                )
                        }
                }

                // 过敏原预警弹窗
                if (showAllergenWarning) {
                        AllergenWarningDialog(
                                matchedAllergens = matchedAllergens.map { 
                                        com.example.lifehub.ui.components.getAllergenDisplayName(it) 
                                },
                                allergenReasoning = nutritionData.allergenReasoning,
                                onDismiss = { showAllergenWarning = false },
                                onConfirm = {
                                        showAllergenWarning = false
                                        // 用户确认后添加记录
                                        if (userId != null && dishItem != null) {
                                                val currentHour =
                                                        java.util.Calendar.getInstance()
                                                                .get(java.util.Calendar.HOUR_OF_DAY)
                                                val mealType =
                                                        when {
                                                                currentHour < 10 -> "breakfast"
                                                                currentHour < 15 -> "lunch"
                                                                currentHour < 20 -> "dinner"
                                                                else -> "snack"
                                                        }

                                                foodViewModel.addDietRecord(
                                                        userId = userId,
                                                        foodName = dishItem.name,
                                                        calories = dishItem.calories.toDouble(),
                                                        protein = dishItem.protein.toDouble(),
                                                        fat = dishItem.fat.toDouble(),
                                                        carbs = dishItem.carbs.toDouble(),
                                                        mealType = mealType
                                                )
                                        }
                                }
                        )
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

/**
 * 基于菜品名称检测潜在过敏原
 * 匹配8大类标准过敏原关键词 + 用户自定义过敏原
 * @param dishName 菜品名称
 * @param userAllergens 用户设置的过敏原列表
 * @return 检测到的过敏原代码列表
 */
fun detectAllergensFromDishName(dishName: String, userAllergens: List<String>): List<String> {
        val detected = mutableSetOf<String>()

        // 8大类标准过敏原关键词映射
        val standardAllergenKeywords = mapOf(
                "milk" to listOf("牛奶", "奶酪", "芝士", "黄油", "奶油", "乳", "酸奶", "奶"),
                "egg" to listOf("鸡蛋", "蛋", "蛋糕", "蛋挞"),
                "fish" to listOf("鱼", "三文鱼", "鳕鱼", "鲈鱼", "鲫鱼", "鲤鱼", "鳗", "带鱼"),
                "shellfish" to listOf("虾", "蟹", "螃蟹", "龙虾", "贝", "蛤", "蚝", "牡蛎", "扇贝", "海鲜"),
                "peanut" to listOf("花生"),
                "tree_nut" to listOf("杏仁", "核桃", "腰果", "坚果", "榛子", "开心果", "松子"),
                "wheat" to listOf("面", "馒头", "饺子", "面条", "馄饨", "包子", "饼", "面包", "馍", "麦"),
                "soy" to listOf("豆腐", "豆浆", "大豆", "毛豆", "黄豆", "豆干", "豆皮")
        )

        // 检测标准过敏原
        for ((code, keywords) in standardAllergenKeywords) {
                if (keywords.any { dishName.contains(it) }) {
                        detected.add(code)
                }
        }

        // 检测用户自定义过敏原（直接匹配菜品名称中是否包含用户过敏原关键词）
        for (allergen in userAllergens) {
                if (allergen.isNotBlank() && dishName.contains(allergen)) {
                        // 尝试将用户过敏原映射到标准代码
                        val code = com.example.lifehub.ui.components.normalizeAllergenForMatching(allergen)
                        detected.add(code)
                }
        }

        return detected.toList()
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
        val aiReason: String,
        val allergens: List<String> = emptyList(),
        val allergenReasoning: String? = null,
        val cookingMethodComparisons: List<CookingMethodItem>? = null
)
