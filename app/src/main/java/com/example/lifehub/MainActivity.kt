package com.example.lifehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.MainNavigation
import com.example.lifehub.ui.theme.BackgroundBeige
import com.example.lifehub.ui.theme.LifeHubTheme

/** LifeHub主活动 智能生活服务工具 - MVP版本 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化UserSession
        UserSession.init(this)

        enableEdgeToEdge()
        setContent {
            LifeHubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundBeige) {
                    MainNavigation()
                }
            }
        }
    }
}
