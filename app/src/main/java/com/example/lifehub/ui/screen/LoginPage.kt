package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.LoginState
import com.example.lifehub.viewmodel.UserViewModel

/** 登录页面 */
@Composable
fun LoginPage(
        navController: NavController,
        onLoginSuccess: (Int) -> Unit,
        viewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    UserSession.init(context)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 观察登录状态
    val loginState by viewModel.loginState.collectAsState()

    // 处理登录成功
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                // 保存登录信息
                UserSession.saveLogin(
                        userId = state.userId,
                        username = username,
                        nickname = state.nickname
                )
                isLoading = false
                onLoginSuccess(state.userId)
            }
            is LoginState.Error -> {
                errorMessage = state.message
                isLoading = false
            }
            is LoginState.Loading -> {
                isLoading = true
            }
            else -> {}
        }
    }

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(
                            brush = Brush.verticalGradient(
                                    colors = listOf(
                                            BackgroundGradientStart,
                                            BackgroundBeige,
                                            BackgroundGradientEnd.copy(alpha = 0.5f)
                                    )
                            )
                    ),
            contentAlignment = Alignment.Center
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo图标
            Box(
                    modifier = Modifier
                            .size(100.dp)
                            .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    ambientColor = ForestGreen.copy(alpha = 0.3f),
                                    spotColor = ForestGreen.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(
                                    brush = Brush.linearGradient(
                                            colors = listOf(ForestGreen, ForestGreenDark)
                                    )
                            ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                    text = "欢迎回来",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "登录开启健康生活",
                    fontSize = 15.sp,
                    color = TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 输入卡片
            Card(
                    modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = ForestGreen.copy(alpha = 0.1f)
                            ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 用户名输入框
                    OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = null
                            },
                            label = { Text("用户ID") },
                            placeholder = { Text("请输入用户ID（数字）") },
                            leadingIcon = {
                                Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ForestGreen
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    focusedLabelColor = ForestGreen,
                                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.5f),
                                    cursorColor = ForestGreen
                            )
                    )

                    // 密码输入框
                    OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("密码") },
                            placeholder = { Text("请输入密码") },
                            leadingIcon = {
                                Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = ForestGreen
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility
                                                          else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                            tint = TextTertiary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    focusedLabelColor = ForestGreen,
                                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.5f),
                                    cursorColor = ForestGreen
                            )
                    )

                    // 错误提示
                    if (errorMessage != null) {
                        Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorRed.copy(alpha = 0.1f)
                        ) {
                            Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = errorMessage!!,
                                        color = ErrorRed,
                                        fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 登录按钮
            Button(
                    onClick = {
                        if (username.isBlank()) {
                            errorMessage = "请输入用户ID"
                            return@Button
                        }
                        if (password.isBlank()) {
                            errorMessage = "请输入密码"
                            return@Button
                        }

                        val userId = username.toIntOrNull()
                        if (userId == null) {
                            errorMessage = "用户ID必须是数字"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        viewModel.login(userId, password)
                    },
                    modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = ForestGreen.copy(alpha = 0.3f)
                            ),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = TextTertiary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                        modifier = Modifier
                                .fillMaxSize()
                                .background(
                                        brush = Brush.horizontalGradient(
                                                colors = listOf(ForestGreen, ForestGreenDark)
                                        )
                                ),
                        contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                                text = "登录",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部装饰
            Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                        modifier = Modifier
                                .width(30.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(TextTertiary.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = "LifeHub",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                        modifier = Modifier
                                .width(30.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(TextTertiary.copy(alpha = 0.3f))
                )
            }
        }
    }
}
