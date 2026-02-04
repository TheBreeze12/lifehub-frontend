package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.DeleteDietRecordState
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.UpdateDietRecordState

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
    val updateDietRecordState by foodViewModel.updateDietRecordState.collectAsState()
    val deleteDietRecordState by foodViewModel.deleteDietRecordState.collectAsState()

    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DietRecord?>(null) }

    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingRecord by remember { mutableStateOf<DietRecord?>(null) }

    // 加载今日饮食记录
    LaunchedEffect(userId) { userId?.let { foodViewModel.getTodayDietRecords(it) } }

    // 监听更新/删除状态，刷新列表
    LaunchedEffect(updateDietRecordState) {
        if (updateDietRecordState is UpdateDietRecordState.Success) {
            showEditDialog = false
            editingRecord = null
            userId?.let { foodViewModel.getTodayDietRecords(it) }
            foodViewModel.resetUpdateDietRecordState()
        }
    }

    LaunchedEffect(deleteDietRecordState) {
        if (deleteDietRecordState is DeleteDietRecordState.Success) {
            showDeleteDialog = false
            deletingRecord = null
            userId?.let { foodViewModel.getTodayDietRecords(it) }
            foodViewModel.resetDeleteDietRecordState()
        }
    }

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
                        ) {
                            items(todayRecords) { record ->
                                DietRecordCard(
                                        record = record,
                                        onEditClick = {
                                            editingRecord = record
                                            showEditDialog = true
                                        },
                                        onDeleteClick = {
                                            deletingRecord = record
                                            showDeleteDialog = true
                                        }
                                )
                            }
                        }
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

        // 编辑对话框
        if (showEditDialog && editingRecord != null) {
            EditDietRecordDialog(
                    record = editingRecord!!,
                    isLoading = updateDietRecordState is UpdateDietRecordState.Loading,
                    errorMessage = (updateDietRecordState as? UpdateDietRecordState.Error)?.message,
                    onDismiss = {
                        showEditDialog = false
                        editingRecord = null
                        foodViewModel.resetUpdateDietRecordState()
                    },
                    onConfirm = { foodName, calories, protein, fat, carbs, mealType ->
                        userId?.let { uid ->
                            foodViewModel.updateDietRecord(
                                    recordId = editingRecord!!.id,
                                    userId = uid,
                                    foodName = foodName,
                                    calories = calories,
                                    protein = protein,
                                    fat = fat,
                                    carbs = carbs,
                                    mealType = mealType
                            )
                        }
                    }
            )
        }

        // 删除确认对话框
        if (showDeleteDialog && deletingRecord != null) {
            DeleteDietRecordDialog(
                    record = deletingRecord!!,
                    isLoading = deleteDietRecordState is DeleteDietRecordState.Loading,
                    errorMessage = (deleteDietRecordState as? DeleteDietRecordState.Error)?.message,
                    onDismiss = {
                        showDeleteDialog = false
                        deletingRecord = null
                        foodViewModel.resetDeleteDietRecordState()
                    },
                    onConfirm = {
                        userId?.let { uid ->
                            foodViewModel.deleteDietRecord(deletingRecord!!.id, uid)
                        }
                    }
            )
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
fun DietRecordCard(
        record: DietRecord,
        onEditClick: (() -> Unit)? = null,
        onDeleteClick: (() -> Unit)? = null
) {
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
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                )
                Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditClick != null) {
                        IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "编辑",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    MealTypeChip(record.mealType)
                }
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

/** 编辑饮食记录对话框 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDietRecordDialog(
        record: DietRecord,
        isLoading: Boolean,
        errorMessage: String?,
        onDismiss: () -> Unit,
        onConfirm: (String, Double, Double, Double, Double, String) -> Unit
) {
    var foodName by remember { mutableStateOf(record.foodName) }
    var calories by remember { mutableStateOf(record.calories.toString()) }
    var protein by remember { mutableStateOf(record.protein.toString()) }
    var fat by remember { mutableStateOf(record.fat.toString()) }
    var carbs by remember { mutableStateOf(record.carbs.toString()) }
    var selectedMealType by remember { mutableStateOf(record.mealType) }

    val mealTypes = listOf(
            "breakfast" to "早餐",
            "lunch" to "午餐",
            "dinner" to "晚餐",
            "snack" to "加餐"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                    modifier = Modifier.padding(20.dp)
            ) {
                Text(
                        text = "编辑饮食记录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("菜品名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text("热量(千卡)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            label = { Text("蛋白质(g)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            label = { Text("脂肪(g)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            label = { Text("碳水(g)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                        text = "餐次",
                        fontSize = 14.sp,
                        color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { (value, label) ->
                        FilterChip(
                                selected = selectedMealType == value ||
                                        selectedMealType == label,
                                onClick = { selectedMealType = value },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = ErrorRed
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                    ) {
                        Text("取消")
                    }
                    Button(
                            onClick = {
                                val caloriesValue = calories.toDoubleOrNull() ?: record.calories
                                val proteinValue = protein.toDoubleOrNull() ?: record.protein
                                val fatValue = fat.toDoubleOrNull() ?: record.fat
                                val carbsValue = carbs.toDoubleOrNull() ?: record.carbs
                                onConfirm(foodName, caloriesValue, proteinValue, fatValue, carbsValue, selectedMealType)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && foodName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

/** 删除饮食记录确认对话框 */
@Composable
fun DeleteDietRecordDialog(
        record: DietRecord,
        isLoading: Boolean,
        errorMessage: String?,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text = "确认删除",
                        fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("确定要删除「${record.foodName}」这条饮食记录吗？")
                    Text(
                            text = "此操作不可撤销",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                    )
                    if (errorMessage != null) {
                        Text(
                                text = errorMessage,
                                fontSize = 13.sp,
                                color = ErrorRed,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text("删除")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                ) {
                    Text("取消")
                }
            }
    )
}
