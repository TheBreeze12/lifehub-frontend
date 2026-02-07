package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.components.AllergenSelectorDialog
import com.example.lifehub.ui.components.getAllergenDisplayText
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.TripViewModel
import com.example.lifehub.viewmodel.UserViewModel

/** 个人中心页 - MVP版本 用户偏好设置、饮食和行程统计 */
@Composable
fun ProfilePage(
        navController: NavController,
        viewModel: UserViewModel = viewModel(),
        tripViewModel: TripViewModel = viewModel(),
        foodViewModel: FoodViewModel = viewModel()
) {
    val context = LocalContext.current
    UserSession.init(context)

    val isLoggedIn = remember { mutableStateOf(UserSession.isLoggedIn()) }
    val userId = remember { mutableStateOf(UserSession.getUserId()) }
    val nickname = remember { mutableStateOf(UserSession.getNickname()) }

    // 观察用户偏好状态
    val preferencesState by viewModel.userPreferencesState.collectAsState()
    val updateState by viewModel.updatePreferencesState.collectAsState()

    // 观察行程列表状态，用于计算行程数量
    val tripListState by tripViewModel.tripListState.collectAsState()

    // 观察饮食记录状态，用于计算用餐记录数量
    val dietRecordsState by foodViewModel.dietRecordsState.collectAsState()

    // 如果已登录，获取用户偏好、行程列表和饮食记录
    LaunchedEffect(userId.value) {
        userId.value?.let {
            viewModel.getUserPreferences(it)
            tripViewModel.getTripList(it)
            foodViewModel.getDietRecords(it)
        }
    }

    // 处理更新成功
    LaunchedEffect(updateState) {
        if (updateState is com.example.lifehub.viewmodel.UpdatePreferencesState.Success) {
            userId.value?.let { viewModel.getUserPreferences(it) }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundBeige)) {
        item {
            // 用户信息区域
            ProfileHeader(
                    isLoggedIn = isLoggedIn.value,
                    nickname = nickname.value ?: "健康达人",
                    onClickAvatar = {
                        if (!isLoggedIn.value) {
                            navController.navigate(Screen.Login.route)
                        }
                    }
            )
        }

        if (isLoggedIn.value && userId.value != null) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // 统计卡片
                // 计算行程数量
                val tripCount =
                        when (val state = tripListState) {
                            is com.example.lifehub.viewmodel.TripListState.Success ->
                                    state.trips.size
                            else -> 0
                        }

                // 计算用餐记录数量
                val dietRecordCount =
                        when (val state = dietRecordsState) {
                            is com.example.lifehub.viewmodel.DietRecordsState.Success -> {
                                // 统计所有日期的记录总数
                                state.records.values.sumOf { it.size }
                            }
                            else -> 0
                        }

                StatsCards(
                        navController = navController,
                        tripCount = tripCount,
                        dietRecordCount = dietRecordCount
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // 饮食偏好设置
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                            text = "饮食偏好",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = preferencesState) {
                        is com.example.lifehub.viewmodel.UserPreferencesState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                        is com.example.lifehub.viewmodel.UserPreferencesState.Success -> {
                            DietPreferenceSection(
                                    healthGoal = state.data.healthGoal,
                                    allergens = state.data.allergens ?: emptyList(),
                                    onUpdate = { newHealthGoal, newAllergens ->
                                        userId.value?.let {
                                            viewModel.updateUserPreferences(
                                                    userId = it,
                                                    healthGoal = newHealthGoal,
                                                    allergens = newAllergens
                                            )
                                        }
                                    }
                            )
                        }
                        is com.example.lifehub.viewmodel.UserPreferencesState.Error -> {
                            Text(
                                    text = "加载失败: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                            )
                        }
                        else -> {
                            // Idle状态，显示默认值
                            DietPreferenceSection(
                                    healthGoal = null,
                                    allergens = emptyList(),
                                    onUpdate = { newHealthGoal, newAllergens ->
                                        userId.value?.let {
                                            viewModel.updateUserPreferences(
                                                    userId = it,
                                                    healthGoal = newHealthGoal,
                                                    allergens = newAllergens
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // 出行偏好设置
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                            text = "出行偏好",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = preferencesState) {
                        is com.example.lifehub.viewmodel.UserPreferencesState.Success -> {
                            TravelPreferenceSection(
                                    travelPreference = state.data.travelPreference,
                                    dailyBudget = state.data.dailyBudget,
                                    onUpdate = { newTravelPreference, newDailyBudget ->
                                        userId.value?.let {
                                            viewModel.updateUserPreferences(
                                                    userId = it,
                                                    travelPreference = newTravelPreference,
                                                    dailyBudget = newDailyBudget
                                            )
                                        }
                                    }
                            )
                        }
                        else -> {
                            TravelPreferenceSection(
                                    travelPreference = null,
                                    dailyBudget = null,
                                    onUpdate = { newTravelPreference, newDailyBudget ->
                                        userId.value?.let {
                                            viewModel.updateUserPreferences(
                                                    userId = it,
                                                    travelPreference = newTravelPreference,
                                                    dailyBudget = newDailyBudget
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // 身体参数设置
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                            text = "身体参数",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = preferencesState) {
                        is com.example.lifehub.viewmodel.UserPreferencesState.Success -> {
                            BodyParamsSection(
                                    weight = state.data.weight,
                                    height = state.data.height,
                                    age = state.data.age,
                                    gender = state.data.gender,
                                    onUpdate = { newWeight, newHeight, newAge, newGender ->
                                        userId.value?.let {
                                            viewModel.updateBodyParams(
                                                    userId = it,
                                                    weight = newWeight,
                                                    height = newHeight,
                                                    age = newAge,
                                                    gender = newGender
                                            )
                                        }
                                    }
                            )
                        }
                        else -> {
                            BodyParamsSection(
                                    weight = null,
                                    height = null,
                                    age = null,
                                    gender = null,
                                    onUpdate = { newWeight, newHeight, newAge, newGender ->
                                        userId.value?.let {
                                            viewModel.updateBodyParams(
                                                    userId = it,
                                                    weight = newWeight,
                                                    height = newHeight,
                                                    age = newAge,
                                                    gender = newGender
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // 退出登录按钮
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Button(
                            onClick = {
                                UserSession.logout()
                                isLoggedIn.value = false
                                userId.value = null
                                nickname.value = "健康达人"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(16.dp)
                    ) { Text("退出登录", color = Color.White) }
                }
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                        text = "请点击头像登录",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun ProfileHeader(isLoggedIn: Boolean, nickname: String, onClickAvatar: () -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    BackgroundGradientStart,
                                                                    BackgroundBeige
                                                            )
                                            )
                            )
                            .padding(24.dp)
                            .padding(top = 32.dp)
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 头像（可点击）- 带阴影效果
            Box(
                    modifier =
                            Modifier.size(88.dp)
                                    .clip(CircleShape)
                                    .background(
                                            brush =
                                                    Brush.linearGradient(
                                                            colors =
                                                                    listOf(
                                                                            ForestGreen,
                                                                            ForestGreenDark
                                                                    )
                                                    )
                                    )
                                    .border(4.dp, Color.White, CircleShape)
                                    .clickable(onClick = onClickAvatar),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "用户头像",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                )
            }

            Column {
                Text(
                        text = nickname,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                        shape = RoundedCornerShape(8.dp),
                        color =
                                if (isLoggedIn) ForestGreenLight.copy(alpha = 0.3f)
                                else LavenderPurple.copy(alpha = 0.2f)
                ) {
                    Text(
                            text = if (isLoggedIn) "✓ 已登录" else "点击头像登录",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLoggedIn) ForestGreenDark else LavenderPurple,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCards(navController: NavController, tripCount: Int, dietRecordCount: Int) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
                modifier = Modifier.weight(1f),
                value = dietRecordCount.toString(),
                label = "用餐记录",
                gradientColors =
                        listOf(CoralPink.copy(alpha = 0.15f), VitalOrangeLight.copy(alpha = 0.1f)),
                iconTint = CoralPink,
                onClick = { navController.navigate(Screen.AllDietRecords.route) }
        )
        StatCard(
                modifier = Modifier.weight(1f),
                value = tripCount.toString(),
                label = "运动规划",
                gradientColors =
                        listOf(ForestGreenLight.copy(alpha = 0.2f), ForestGreen.copy(alpha = 0.1f)),
                iconTint = ForestGreen,
                onClick = { navController.navigate(Screen.TripList.route) }
        )
        // Phase 17: 热量收支统计入口
        StatCard(
                modifier = Modifier.weight(1f),
                value = "📊",
                label = "热量统计",
                gradientColors =
                        listOf(SkyBlueLight.copy(alpha = 0.2f), SkyBlue.copy(alpha = 0.1f)),
                iconTint = SkyBlue,
                onClick = { navController.navigate(Screen.Stats.route) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Phase 48: 健康目标达成入口
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
                modifier = Modifier.weight(1f),
                value = "🎯",
                label = "健康目标",
                gradientColors =
                        listOf(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF22C55E).copy(alpha = 0.1f)),
                iconTint = Color(0xFF10B981),
                onClick = { navController.navigate(Screen.GoalProgress.route) }
        )
        // Phase 49: 运动历史记录入口
        StatCard(
                modifier = Modifier.weight(1f),
                value = "🏃",
                label = "运动历史",
                gradientColors =
                        listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFF818CF8).copy(alpha = 0.1f)),
                iconTint = Color(0xFF6366F1),
                onClick = { navController.navigate(Screen.ExerciseHistory.route) }
        )
        // 占位保持对齐
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
        modifier: Modifier = Modifier,
        value: String,
        label: String,
        gradientColors: List<Color> = listOf(Color.White, Color.White),
        iconTint: Color = ForestGreen,
        onClick: (() -> Unit)? = null
) {
    Card(
            modifier =
                    modifier.then(
                            if (onClick != null) {
                                Modifier.clickable(onClick = onClick)
                            } else {
                                Modifier
                            }
                    ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(brush = Brush.verticalGradient(gradientColors))
                                .background(Color.White.copy(alpha = 0.7f))
                                .padding(14.dp),
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = iconTint)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = label,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DietPreferenceSection(
        healthGoal: String?,
        allergens: List<String>,
        onUpdate: (String?, List<String>) -> Unit
) {
    var showHealthGoalDialog by remember { mutableStateOf(false) }
    var showAllergenDialog by remember { mutableStateOf(false) }
    var selectedHealthGoal by remember { mutableStateOf(healthGoal ?: "balanced") }
    var selectedAllergens by remember { mutableStateOf(allergens) }

    // 同步外部数据
    LaunchedEffect(healthGoal, allergens) {
        selectedHealthGoal = healthGoal ?: "balanced"
        selectedAllergens = allergens
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            SettingItem(
                    icon = Icons.Default.TravelExplore,
                    iconTint = ForestGreen,
                    title = "健康目标",
                    value = getHealthGoalDisplayName(selectedHealthGoal),
                    onClick = { showHealthGoalDialog = true }
            )

            CustomDivider(color = Color(0xFFF9FAFB))

            SettingItem(
                    icon = Icons.Default.Warning,
                    iconTint = Color(0xFFEF4444),
                    title = "过敏原档案",
                    value = getAllergenDisplayText(selectedAllergens),
                    onClick = { showAllergenDialog = true }
            )
        }
    }

    // 健康目标选择对话框
    if (showHealthGoalDialog) {
        HealthGoalDialog(
                currentGoal = selectedHealthGoal,
                onDismiss = { showHealthGoalDialog = false },
                onConfirm = { goal ->
                    selectedHealthGoal = goal
                    showHealthGoalDialog = false
                    onUpdate(goal, selectedAllergens)
                }
        )
    }

    // 过敏原档案配置对话框（增强版：支持八大类+自定义输入）
    if (showAllergenDialog) {
        AllergenSelectorDialog(
                currentAllergens = selectedAllergens,
                onDismiss = { showAllergenDialog = false },
                onConfirm = { newAllergens ->
                    selectedAllergens = newAllergens
                    showAllergenDialog = false
                    onUpdate(selectedHealthGoal, newAllergens)
                }
        )
    }
}

@Composable
private fun TravelPreferenceSection(
        travelPreference: String?,
        dailyBudget: Int?,
        onUpdate: (String?, Int?) -> Unit
) {
    var showTravelModeDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedTravelMode by remember { mutableStateOf(travelPreference ?: "self_driving") }
    var dailyBudgetText by remember { mutableStateOf((dailyBudget ?: 500).toString()) }

    // 同步外部数据
    LaunchedEffect(travelPreference, dailyBudget) {
        selectedTravelMode = travelPreference ?: "self_driving"
        dailyBudgetText = (dailyBudget ?: 500).toString()
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            SettingItem(
                    icon = Icons.Default.DirectionsCar,
                    iconTint = Color(0xFF3B82F6),
                    title = "出行方式",
                    value = getTravelPreferenceDisplayName(selectedTravelMode),
                    onClick = { showTravelModeDialog = true }
            )

            CustomDivider(color = Color(0xFFF9FAFB))

            SettingItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = VitalOrange,
                    title = "每日预算",
                    value = "¥$dailyBudgetText",
                    onClick = { showBudgetDialog = true }
            )
        }
    }

    // 出行方式选择对话框
    if (showTravelModeDialog) {
        TravelModeDialog(
                currentMode = selectedTravelMode,
                onDismiss = { showTravelModeDialog = false },
                onConfirm = { mode ->
                    selectedTravelMode = mode
                    showTravelModeDialog = false
                    onUpdate(mode, dailyBudgetText.toIntOrNull())
                }
        )
    }

    // 预算设置对话框
    if (showBudgetDialog) {
        BudgetDialog(
                currentBudget = dailyBudgetText,
                onDismiss = { showBudgetDialog = false },
                onConfirm = { budget ->
                    dailyBudgetText = budget
                    showBudgetDialog = false
                    onUpdate(selectedTravelMode, budget.toIntOrNull())
                }
        )
    }
}

@Composable
private fun SettingItem(
        icon: ImageVector,
        iconTint: Color,
        title: String,
        value: String,
        onClick: () -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
            )
            Text(text = title, fontSize = 14.sp, color = TextPrimary)
        }

        Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = value, fontSize = 12.sp, color = TextSecondary)
            Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "编辑",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CustomDivider(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color))
}

// 对话框组件
@Composable
private fun HealthGoalDialog(
        currentGoal: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    val goals =
            listOf(
                    "balanced" to "均衡模式",
                    "reduce_fat" to "减脂模式",
                    "gain_muscle" to "增肌模式",
                    "control_sugar" to "控糖模式"
            )

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择健康目标") },
            text = {
                Column {
                    goals.forEach { (value, label) ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable { onConfirm(value) }
                                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                    selected = currentGoal == value,
                                    onClick = { onConfirm(value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}


@Composable
private fun TravelModeDialog(
        currentMode: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    val modes = listOf("self_driving" to "自驾", "public_transport" to "公共交通", "walking" to "步行")

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择出行方式") },
            text = {
                Column {
                    modes.forEach { (value, label) ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable { onConfirm(value) }
                                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                    selected = currentMode == value,
                                    onClick = { onConfirm(value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun BudgetDialog(
        currentBudget: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置每日预算") },
            text = {
                OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("预算（元）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = { TextButton(onClick = { onConfirm(budgetText) }) { Text("确定") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// 辅助函数
private fun getHealthGoalDisplayName(goal: String?): String {
    return when (goal) {
        "reduce_fat" -> "减脂模式"
        "gain_muscle" -> "增肌模式"
        "control_sugar" -> "控糖模式"
        "balanced" -> "均衡模式"
        else -> "未设置"
    }
}

private fun getTravelPreferenceDisplayName(preference: String?): String {
    return when (preference) {
        "self_driving" -> "自驾"
        "public_transport" -> "公共交通"
        "walking" -> "步行"
        else -> "未设置"
    }
}

@Composable
private fun BodyParamsSection(
        weight: Double?,
        height: Double?,
        age: Int?,
        gender: String?,
        onUpdate: (Double?, Double?, Int?, String?) -> Unit
) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showAgeDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }

    var currentWeight by remember { mutableStateOf(weight) }
    var currentHeight by remember { mutableStateOf(height) }
    var currentAge by remember { mutableStateOf(age) }
    var currentGender by remember { mutableStateOf(gender) }

    // 同步外部数据
    LaunchedEffect(weight, height, age, gender) {
        currentWeight = weight
        currentHeight = height
        currentAge = age
        currentGender = gender
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            SettingItem(
                    icon = Icons.Default.FitnessCenter,
                    iconTint = Color(0xFF10B981),
                    title = "体重",
                    value = if (currentWeight != null) "${currentWeight}kg" else "未设置",
                    onClick = { showWeightDialog = true }
            )

            CustomDivider(color = Color(0xFFF9FAFB))

            SettingItem(
                    icon = Icons.Default.Straighten,
                    iconTint = Color(0xFF6366F1),
                    title = "身高",
                    value = if (currentHeight != null) "${currentHeight}cm" else "未设置",
                    onClick = { showHeightDialog = true }
            )

            CustomDivider(color = Color(0xFFF9FAFB))

            SettingItem(
                    icon = Icons.Default.DateRange,
                    iconTint = Color(0xFFF59E0B),
                    title = "年龄",
                    value = if (currentAge != null) "${currentAge}岁" else "未设置",
                    onClick = { showAgeDialog = true }
            )

            CustomDivider(color = Color(0xFFF9FAFB))

            SettingItem(
                    icon = Icons.Default.Face,
                    iconTint = Color(0xFFEC4899),
                    title = "性别",
                    value = getGenderDisplayName(currentGender),
                    onClick = { showGenderDialog = true }
            )
        }
    }

    // 体重设置对话框
    if (showWeightDialog) {
        WeightDialog(
                currentWeight = currentWeight,
                onDismiss = { showWeightDialog = false },
                onConfirm = { newWeight ->
                    currentWeight = newWeight
                    showWeightDialog = false
                    onUpdate(newWeight, currentHeight, currentAge, currentGender)
                }
        )
    }

    // 身高设置对话框
    if (showHeightDialog) {
        HeightDialog(
                currentHeight = currentHeight,
                onDismiss = { showHeightDialog = false },
                onConfirm = { newHeight ->
                    currentHeight = newHeight
                    showHeightDialog = false
                    onUpdate(currentWeight, newHeight, currentAge, currentGender)
                }
        )
    }

    // 年龄设置对话框
    if (showAgeDialog) {
        AgeDialog(
                currentAge = currentAge,
                onDismiss = { showAgeDialog = false },
                onConfirm = { newAge ->
                    currentAge = newAge
                    showAgeDialog = false
                    onUpdate(currentWeight, currentHeight, newAge, currentGender)
                }
        )
    }

    // 性别设置对话框
    if (showGenderDialog) {
        GenderDialog(
                currentGender = currentGender,
                onDismiss = { showGenderDialog = false },
                onConfirm = { newGender ->
                    currentGender = newGender
                    showGenderDialog = false
                    onUpdate(currentWeight, currentHeight, currentAge, newGender)
                }
        )
    }
}

@Composable
private fun WeightDialog(
        currentWeight: Double?,
        onDismiss: () -> Unit,
        onConfirm: (Double?) -> Unit
) {
    var weightText by remember { mutableStateOf(currentWeight?.toString() ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置体重") },
            text = {
                Column {
                    OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("体重（kg）") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "请输入有效的体重值（0.1-500kg）",
                            fontSize = 12.sp,
                            color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            val weight = weightText.toDoubleOrNull()
                            if (weight != null && weight > 0 && weight <= 500) {
                                onConfirm(weight)
                            }
                        }
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun HeightDialog(
        currentHeight: Double?,
        onDismiss: () -> Unit,
        onConfirm: (Double?) -> Unit
) {
    var heightText by remember { mutableStateOf(currentHeight?.toString() ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置身高") },
            text = {
                Column {
                    OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("身高（cm）") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "请输入有效的身高值（50-300cm）",
                            fontSize = 12.sp,
                            color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            val height = heightText.toDoubleOrNull()
                            if (height != null && height > 0 && height <= 300) {
                                onConfirm(height)
                            }
                        }
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AgeDialog(
        currentAge: Int?,
        onDismiss: () -> Unit,
        onConfirm: (Int?) -> Unit
) {
    var ageText by remember { mutableStateOf(currentAge?.toString() ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置年龄") },
            text = {
                Column {
                    OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("年龄") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "请输入有效的年龄值（1-150岁）",
                            fontSize = 12.sp,
                            color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            val age = ageText.toIntOrNull()
                            if (age != null && age > 0 && age <= 150) {
                                onConfirm(age)
                            }
                        }
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun GenderDialog(
        currentGender: String?,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    val genders = listOf(
            "male" to "男",
            "female" to "女",
            "other" to "其他"
    )

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择性别") },
            text = {
                Column {
                    genders.forEach { (value, label) ->
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onConfirm(value) }
                                        .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                    selected = currentGender == value,
                                    onClick = { onConfirm(value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun getGenderDisplayName(gender: String?): String {
    return when (gender) {
        "male" -> "男"
        "female" -> "女"
        "other" -> "其他"
        else -> "未设置"
    }
}
