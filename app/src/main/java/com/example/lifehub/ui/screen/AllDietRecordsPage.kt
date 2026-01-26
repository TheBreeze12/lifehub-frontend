package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.BackgroundBeige
import com.example.lifehub.ui.theme.ForestGreen
import com.example.lifehub.viewmodel.FoodViewModel
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

    // 加载所有饮食记录
    LaunchedEffect(userId) { userId?.let { foodViewModel.getDietRecords(it) } }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("我的用餐记录", fontWeight = FontWeight.Bold) },
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
            when (val state = dietRecordsState) {
                is com.example.lifehub.viewmodel.DietRecordsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForestGreen)
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
                                Text(text = "还没有饮食记录", fontSize = 16.sp, color = Color(0xFF6B7280))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "快去添加你的第一餐吧！",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    } else {
                        // 按日期分组的记录列表
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            recordsByDate.entries.forEach { entry ->
                                item {
                                    DateSectionHeader(
                                            dateStr = entry.key,
                                            recordCount = entry.value.size
                                    )
                                }
                                items(entry.value) { record -> DietRecordCard(record = record) }
                            }
                        }
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
                    date == today -> "今天"
                    date == yesterday -> "昨天"
                    date.year == today.year -> {
                        "${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)}"
                    }
                    else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
                }
            } else {
                dateStr
            }

    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = displayText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
        )
        Text(text = "$recordCount 条记录", fontSize = 12.sp, color = Color(0xFF9CA3AF))
    }
}
