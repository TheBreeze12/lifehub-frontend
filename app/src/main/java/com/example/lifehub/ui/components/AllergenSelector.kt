package com.example.lifehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog

/**
 * 八大类过敏原数据类
 * @param code 过敏原代码（与后端一致）
 * @param name 中文名称
 * @param nameEn 英文名称
 * @param description 描述
 * @param icon 表情图标
 */
data class AllergenCategory(
        val code: String,
        val name: String,
        val nameEn: String,
        val description: String,
        val icon: String
)

/**
 * 八大类过敏原常量
 * 与后端allergen_service.py保持一致
 */
object AllergenCategories {
    val EIGHT_MAJOR_ALLERGENS =
            listOf(
                    AllergenCategory(
                            code = "milk",
                            name = "乳制品",
                            nameEn = "Milk",
                            description = "牛奶、奶酪、黄油、酸奶、奶油等",
                            icon = "🥛"
                    ),
                    AllergenCategory(
                            code = "egg",
                            name = "鸡蛋",
                            nameEn = "Egg",
                            description = "各种蛋类及其制品",
                            icon = "🥚"
                    ),
                    AllergenCategory(
                            code = "fish",
                            name = "鱼类",
                            nameEn = "Fish",
                            description = "各种鱼类及鱼制品",
                            icon = "🐟"
                    ),
                    AllergenCategory(
                            code = "shellfish",
                            name = "甲壳类",
                            nameEn = "Shellfish",
                            description = "虾、蟹、贝类等海鲜",
                            icon = "🦐"
                    ),
                    AllergenCategory(
                            code = "peanut",
                            name = "花生",
                            nameEn = "Peanut",
                            description = "花生及花生制品",
                            icon = "🥜"
                    ),
                    AllergenCategory(
                            code = "tree_nut",
                            name = "树坚果",
                            nameEn = "Tree Nuts",
                            description = "杏仁、核桃、腰果、榛子等",
                            icon = "🌰"
                    ),
                    AllergenCategory(
                            code = "wheat",
                            name = "小麦",
                            nameEn = "Wheat",
                            description = "小麦及含麸质食品",
                            icon = "🌾"
                    ),
                    AllergenCategory(
                            code = "soy",
                            name = "大豆",
                            nameEn = "Soy",
                            description = "豆腐、豆浆、酱油等豆制品",
                            icon = "🫘"
                    )
            )

    /**
     * 根据代码获取过敏原类别
     */
    fun getByCode(code: String): AllergenCategory? {
        return EIGHT_MAJOR_ALLERGENS.find { it.code == code.lowercase() }
    }

    /**
     * 根据中文名称获取代码
     */
    fun getCodeByName(name: String): String? {
        return EIGHT_MAJOR_ALLERGENS.find { it.name == name }?.code
    }

    /**
     * 判断是否为八大类过敏原
     */
    fun isMajorAllergen(allergen: String): Boolean {
        val lowerAllergen = allergen.lowercase()
        return EIGHT_MAJOR_ALLERGENS.any { 
            it.code == lowerAllergen || it.name == allergen 
        }
    }
}

/**
 * 过敏原档案配置对话框
 * @param currentAllergens 当前已选择的过敏原列表（可包含代码或中文名称）
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认回调，返回选中的过敏原列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllergenSelectorDialog(
        currentAllergens: List<String>,
        onDismiss: () -> Unit,
        onConfirm: (List<String>) -> Unit
) {
    // 将当前过敏原转换为统一的格式（中文名称）
    val normalizedCurrentAllergens = currentAllergens.map { allergen ->
        // 如果是代码，转换为中文名称
        AllergenCategories.getByCode(allergen)?.name ?: allergen
    }.toSet()

    // 选中的八大类过敏原（使用中文名称）
    var selectedMajorAllergens by remember { mutableStateOf(normalizedCurrentAllergens) }

    // 自定义过敏原输入
    var customAllergenInput by remember { mutableStateOf("") }

    // 自定义过敏原列表（排除八大类）
    var customAllergens by remember {
        mutableStateOf(
                normalizedCurrentAllergens.filter { allergen ->
                    !AllergenCategories.EIGHT_MAJOR_ALLERGENS.any { it.name == allergen }
                }.toSet()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                    modifier =
                            Modifier.padding(20.dp)
                                    .verticalScroll(rememberScrollState())
            ) {
                // 标题栏
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "过敏原档案配置",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 说明文字
                Text(
                        text = "选择您的过敏原，我们会在营养分析时为您预警",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 八大类过敏原标题
                Text(
                        text = "八大类过敏原",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 八大类过敏原网格
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 每行显示2个
                    AllergenCategories.EIGHT_MAJOR_ALLERGENS.chunked(2).forEach { rowItems ->
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { allergen ->
                                val isSelected = selectedMajorAllergens.contains(allergen.name)
                                AllergenChip(
                                        allergen = allergen,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedMajorAllergens =
                                                    if (isSelected) {
                                                        selectedMajorAllergens - allergen.name
                                                    } else {
                                                        selectedMajorAllergens + allergen.name
                                                    }
                                        },
                                        modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果是奇数个，添加占位
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 自定义过敏原标题
                Text(
                        text = "自定义过敏原",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "如有其他过敏原，可在下方添加",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 自定义输入框
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                            value = customAllergenInput,
                            onValueChange = { customAllergenInput = it },
                            placeholder = { Text("输入过敏原名称", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                    )
                    Button(
                            onClick = {
                                val trimmed = customAllergenInput.trim()
                                if (trimmed.isNotEmpty() && !customAllergens.contains(trimmed)) {
                                    customAllergens = customAllergens + trimmed
                                    customAllergenInput = ""
                                }
                            },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981)
                                    ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(20.dp))
                    }
                }

                // 已添加的自定义过敏原
                if (customAllergens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customAllergens.toList()) { allergen ->
                            CustomAllergenTag(
                                    name = allergen,
                                    onRemove = { customAllergens = customAllergens - allergen }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 已选摘要
                val totalSelected =
                        selectedMajorAllergens.filter { name ->
                            AllergenCategories.EIGHT_MAJOR_ALLERGENS.any { it.name == name }
                        }.size + customAllergens.size

                if (totalSelected > 0) {
                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    text = "已选择 $totalSelected 种过敏原",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 按钮区域
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF6B7280)
                                    )
                    ) { Text("取消", fontSize = 14.sp, fontWeight = FontWeight.Medium) }

                    Button(
                            onClick = {
                                // 合并八大类和自定义过敏原
                                val selectedMajorNames = selectedMajorAllergens.filter { name ->
                                    AllergenCategories.EIGHT_MAJOR_ALLERGENS.any { it.name == name }
                                }
                                val allAllergens =
                                        (selectedMajorNames + customAllergens).toList()
                                onConfirm(allAllergens)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981)
                                    )
                    ) { Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

/**
 * 八大类过敏原选择芯片
 */
@Composable
fun AllergenChip(
        allergen: AllergenCategory,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    val backgroundColor =
            if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF9FAFB)
    val borderColor =
            if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB)
    val textColor =
            if (isSelected) Color(0xFF059669) else Color(0xFF6B7280)

    Surface(
            modifier =
                    modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(onClick = onClick),
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = allergen.icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                            text = allergen.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                    )
                    Text(
                            text = allergen.nameEn,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                    )
                }
            }
            if (isSelected) {
                Box(
                        modifier =
                                Modifier.size(20.dp)
                                        .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.Check,
                            contentDescription = "已选",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 自定义过敏原标签（可删除）
 */
@Composable
fun CustomAllergenTag(name: String, onRemove: () -> Unit) {
    Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEE2E2)
    ) {
        Row(
                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFDC2626)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
            ) {
                Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 过敏原档案展示组件（用于ProfilePage展示已选过敏原）
 * @param allergens 过敏原列表
 * @param onEdit 编辑回调
 */
@Composable
fun AllergenProfileDisplay(
        allergens: List<String>,
        onEdit: () -> Unit
) {
    if (allergens.isEmpty()) {
        Text(
                text = "未设置",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
        )
    } else {
        LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onEdit)
        ) {
            items(allergens.take(3)) { allergen ->
                // 尝试匹配八大类
                val category = AllergenCategories.EIGHT_MAJOR_ALLERGENS.find { it.name == allergen }
                val displayText = if (category != null) {
                    "${category.icon} ${category.name}"
                } else {
                    allergen
                }
                
                Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                ) {
                    Text(
                            text = displayText,
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // 如果超过3个，显示更多
            if (allergens.size > 3) {
                item {
                    Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6)
                    ) {
                        Text(
                                text = "+${allergens.size - 3}",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取过敏原的展示文本
 * @param allergens 过敏原列表
 * @return 展示文本
 */
fun getAllergenDisplayText(allergens: List<String>): String {
    if (allergens.isEmpty()) return "未设置"
    
    val displayList = allergens.take(3).map { allergen ->
        AllergenCategories.EIGHT_MAJOR_ALLERGENS.find { it.name == allergen }?.name ?: allergen
    }
    
    return if (allergens.size > 3) {
        displayList.joinToString("、") + " 等${allergens.size}项"
    } else {
        displayList.joinToString("、")
    }
}
