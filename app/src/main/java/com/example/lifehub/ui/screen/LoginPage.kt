package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier.fillMaxSize().background(BackgroundBeige),
            contentAlignment = Alignment.Center
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo/标题
            Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = ForestGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "欢迎回来", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Text(text = "登录以继续使用", fontSize = 14.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(32.dp))

            // 用户名输入框
            OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("用户ID") },
                    placeholder = { Text("请输入用户ID（数字）") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    focusedLabelColor = ForestGreen
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
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                    imageVector =
                                            if (passwordVisible) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    visualTransformation =
                            if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    focusedLabelColor = ForestGreen
                            )
            )

            // 错误提示
            if (errorMessage != null) {
                Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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

                        // 验证密码
                        if (password != "123") {
                            errorMessage = "密码错误，请输入123"
                            return@Button
                        }

                        // 开始登录（通过获取用户偏好验证用户是否存在）
                        isLoading = true
                        errorMessage = null
                        viewModel.login(userId)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                            text = "登录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示信息
            Text(text = "提示：用户ID为数字，密码固定为123", fontSize = 12.sp, color = TextSecondary)
        }
    }
}
