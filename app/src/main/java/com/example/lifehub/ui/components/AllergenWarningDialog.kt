package com.example.lifehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 过敏原预警弹窗组件
 * @param matchedAllergens 匹配到的用户过敏原列表
 * @param allergenReasoning AI推理的过敏原说明
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认（仍要添加）回调
 */
@Composable
fun AllergenWarningDialog(
        matchedAllergens: List<String>,
        allergenReasoning: String?,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                    ) {
                        Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                // 警告图标
                Box(
                        modifier =
                                Modifier.size(72.dp)
                                        .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.Warning,
                            contentDescription = "警告",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                        text = "⚠️ 过敏原预警",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 警告内容
                Text(
                        text = "检测到该菜品可能含有您的过敏原：",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 过敏原标签
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    matchedAllergens.forEach { allergen ->
                        AllergenTag(allergen = allergen)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // AI推理说明
                if (!allergenReasoning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                    text = "💡 AI分析",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    text = allergenReasoning,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF6B7280)
                                    )
                    ) {
                        Text(text = "返回修改", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // 确认按钮
                    Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDC2626)
                                    )
                    ) {
                        Text(text = "仍要添加", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * 过敏原标签组件
 * @param allergen 过敏原名称
 */
@Composable
fun AllergenTag(allergen: String) {
    Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEE2E2)
    ) {
        Text(
                text = allergen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 过敏原高亮展示区域组件
 * @param detectedAllergens 检测到的所有过敏原
 * @param matchedAllergens 匹配用户过敏原档案的过敏原
 * @param allergenReasoning AI推理说明
 */
@Composable
fun AllergenHighlightSection(
        detectedAllergens: List<String>,
        matchedAllergens: List<String>,
        allergenReasoning: String?
) {
    if (detectedAllergens.isEmpty()) return

    val hasWarning = matchedAllergens.isNotEmpty()

    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (hasWarning) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
            border =
                    androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasWarning) Color(0xFFFCA5A5) else Color(0xFFFCD34D)
                    )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.Warning,
                        contentDescription = "过敏原",
                        tint = if (hasWarning) Color(0xFFDC2626) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = if (hasWarning) "⚠️ 过敏原警告" else "🔍 检测到过敏原",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasWarning) Color(0xFFDC2626) else Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 过敏原标签展示
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detectedAllergens.forEach { allergen ->
                    val isMatched = matchedAllergens.contains(allergen)
                    Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMatched) Color(0xFFDC2626) else Color(0xFFD97706)
                    ) {
                        Text(
                                text = getAllergenDisplayName(allergen),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // AI推理说明
            if (!allergenReasoning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                        text = allergenReasoning,
                        fontSize = 12.sp,
                        color = if (hasWarning) Color(0xFF991B1B) else Color(0xFF92400E),
                        lineHeight = 16.sp
                )
            }

            // 用户匹配警告
            if (hasWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = "⚠️ 您对以下过敏原过敏：${matchedAllergens.joinToString("、") { getAllergenDisplayName(it) }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                )
            }
        }
    }
}

/**
 * 获取过敏原的中文显示名称
 * @param allergenCode 过敏原代码
 * @return 中文名称
 */
fun getAllergenDisplayName(allergenCode: String): String {
    return when (allergenCode.lowercase()) {
        "milk" -> "乳制品"
        "egg" -> "鸡蛋"
        "fish" -> "鱼类"
        "shellfish" -> "甲壳类"
        "peanut" -> "花生"
        "tree_nut", "treenut" -> "树坚果"
        "wheat" -> "小麦"
        "soy" -> "大豆"
        else -> allergenCode
    }
}

/**
 * 将用户过敏原中文名称转换为代码进行匹配
 * @param userAllergen 用户设置的过敏原（可能是中文）
 * @return 过敏原代码
 */
fun normalizeAllergenForMatching(userAllergen: String): String {
    return when {
        userAllergen.contains("乳") || userAllergen.contains("奶") -> "milk"
        userAllergen.contains("蛋") -> "egg"
        userAllergen.contains("鱼") -> "fish"
        userAllergen.contains("虾") || userAllergen.contains("蟹") || 
                userAllergen.contains("贝") || userAllergen.contains("海鲜") || 
                userAllergen.contains("甲壳") -> "shellfish"
        userAllergen.contains("花生") -> "peanut"
        userAllergen.contains("坚果") || userAllergen.contains("杏仁") || 
                userAllergen.contains("核桃") || userAllergen.contains("腰果") -> "tree_nut"
        userAllergen.contains("麦") || userAllergen.contains("面") || 
                userAllergen.contains("麸") -> "wheat"
        userAllergen.contains("豆") -> "soy"
        else -> userAllergen.lowercase()
    }
}

/**
 * 检查过敏原是否匹配
 * @param detectedAllergens AI检测到的过敏原列表（代码形式）
 * @param userAllergens 用户设置的过敏原列表（可能是中文）
 * @return 匹配到的过敏原列表
 */
fun matchUserAllergens(
        detectedAllergens: List<String>,
        userAllergens: List<String>?
): List<String> {
    if (userAllergens.isNullOrEmpty() || detectedAllergens.isEmpty()) {
        return emptyList()
    }

    val normalizedUserAllergens = userAllergens.map { normalizeAllergenForMatching(it) }
    
    return detectedAllergens.filter { detected ->
        val detectedLower = detected.lowercase()
        normalizedUserAllergens.any { userAllergen ->
            detectedLower == userAllergen || 
            detectedLower.contains(userAllergen) || 
            userAllergen.contains(detectedLower)
        }
    }
}
