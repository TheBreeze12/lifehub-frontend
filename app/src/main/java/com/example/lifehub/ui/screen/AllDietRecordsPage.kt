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
import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.DeleteDietRecordState
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.UpdateDietRecordState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 所有饮食记录页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllDietRecordsPage(navController: NavController, foodViewModel: FoodViewModel = viewModel()) {
    val userId =
            try {
                if (UserSession.isLoggedIn()) UserSession.getUserId() else null
            } catch (e: Exception) {
                null
            }

    val dietRecordsState by foodViewModel.dietRecordsState.collectAsState()
    val updateDietRecordState by foodViewModel.updateDietRecordState.collectAsState()
    val deleteDietRecordState by foodViewModel.deleteDietRecordState.collectAsState()

    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DietRecord?>(null) }

    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingRecord by remember { mutableStateOf<DietRecord?>(null) }

    // 加载所有饮食记录
    LaunchedEffect(userId) { userId?.let { foodViewModel.getDietRecords(it) } }

    // 监听更新/删除状态，刷新列表
    LaunchedEffect(updateDietRecordState) {
        if (updateDietRecordState is UpdateDietRecordState.Success) {
            showEditDialog = false
            editingRecord = null
            userId?.let { foodViewModel.getDietRecords(it) }
            foodViewModel.resetUpdateDietRecordState()
        }
    }

    LaunchedEffect(deleteDietRecordState) {
        if (deleteDietRecordState is DeleteDietRecordState.Success) {
            showDeleteDialog = false
            deletingRecord = null
            userId?.let { foodViewModel.getDietRecords(it) }
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
                                "我的用餐记录",
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
                                            .background(CoralPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "返回",
                                        tint = CoralPink
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                    )
            )

            when (val state = dietRecordsState) {
                is com.example.lifehub.viewmodel.DietRecordsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoralPink)
                    }
                }
                is com.example.lifehub.viewmodel.DietRecordsState.Success -> {
                    val recordsByDate = state.records

                    if (recordsByDate.isEmpty()) {
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
                                                                        CoralPink.copy(alpha = 0.3f),
                                                                        VitalOrangeLight.copy(alpha = 0.2f)
                                                                )
                                                        )
                                                ),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                            Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = CoralPink,
                                            modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                        text = "还没有饮食记录",
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
                        // 按日期分组的记录列表
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            recordsByDate.entries.forEach { entry ->
                                item {
                                    DateSectionHeader(
                                            dateStr = entry.key,
                                            recordCount = entry.value.size
                                    )
                                }
                                items(entry.value) { record ->
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
                }
                is com.example.lifehub.viewmodel.DietRecordsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
                        CircularProgressIndicator(color = CoralPink)
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

/** 日期分组标题 */
@Composable
fun DateSectionHeader(dateStr: String, recordCount: Int) {
    val date =
            try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                null
            }

    val displayText =
            if (date != null) {
                val today = LocalDate.now()
                val yesterday = today.minusDays(1)

                when {
                    date == today -> "📅 今天"
                    date == yesterday -> "📅 昨天"
                    date.year == today.year -> {
                        "📅 ${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)}"
                    }
                    else -> "📅 ${date.year}年${date.monthValue}月${date.dayOfMonth}日"
                }
            } else {
                dateStr
            }

    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = displayText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
        )
        Surface(
                shape = RoundedCornerShape(10.dp),
                color = CoralPink.copy(alpha = 0.1f)
        ) {
            Text(
                    text = "$recordCount 条记录",
                    fontSize = 12.sp,
                    color = CoralPink,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
