package com.example.lifehub.navigation


import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lifehub.ui.screen.*

/** 底部导航栏项目 */
sealed class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home, "首页", Icons.Filled.Home)
    object Food : BottomNavItem(Screen.Camera, "餐饮", Icons.Filled.Restaurant)
    object Trip : BottomNavItem(Screen.TripPlanning, "运动", Icons.Filled.Place)
    object Profile : BottomNavItem(Screen.Profile, "我的", Icons.Filled.Person)
}

/** 主导航组件 管理应用内所有页面的导航逻辑 */
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems =
            listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Food,
                    BottomNavItem.Trip,
                    BottomNavItem.Profile
            )

    Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // 只在主要页面显示底部导航栏
                val shouldShowBottomBar =
                        currentDestination?.route in
                                listOf(
                                        Screen.Home.route,
                                        Screen.Camera.route,
                                        Screen.TripPlanning.route,
                                        Screen.Profile.route
                                )

                if (shouldShowBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected =
                                    currentDestination?.hierarchy?.any {
                                        it.route == item.screen.route
                                    } == true

                            NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = selected,
                                    onClick = {
                                        // 始终允许导航，即使已选中也重新导航以确保正确返回
                                        navController.navigate(item.screen.route) {
                                            // 清除回退栈到首页
                                            popUpTo(Screen.Home.route) {
                                                inclusive = item.screen.route == Screen.Home.route
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                            )
                        }
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomePage(navController = navController) }

            composable(Screen.Camera.route) { CameraPage(navController = navController) }

            composable(Screen.NutritionDetail.route) { backStackEntry ->
                val dishName = backStackEntry.arguments?.getString("dishName") ?: ""
                NutritionDetailPage(dishName = dishName, navController = navController)
            }

            composable(Screen.TripPlanning.route) {
                TripPlanningPage(navController = navController)
            }

            composable(Screen.TripDetail.route) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                TripDetailPage(tripId = tripId, navController = navController)
            }

            composable(Screen.TripList.route) { TripListPage(navController = navController) }

            composable(Screen.Profile.route) { ProfilePage(navController = navController) }

            composable(Screen.TodayDietRecords.route) {
                TodayDietRecordsPage(navController = navController)
            }

            composable(Screen.AllDietRecords.route) {
                AllDietRecordsPage(navController = navController)
            }

            composable(Screen.Login.route) {
                LoginPage(
                        navController = navController,
                        onLoginSuccess = { userId ->
                            // 登录成功后返回个人中心页面
                            navController.popBackStack()
                            navController.navigate(Screen.Profile.route)
                        }
                )
            }

            composable(Screen.Register.route) {
                RegisterPage(
                        navController = navController,
                        onRegisterSuccess = { userId ->
                            // 注册成功后返回登录页面或直接登录
                            navController.popBackStack()
                            // 或者直接登录
                        }
                )
            }

            // Phase 13: 餐前餐后对比功能
            composable(Screen.MealComparison.route) {
                MealComparisonPage(navController = navController)
            }

            composable(Screen.BeforeMealCamera.route) {
                BeforeMealCameraPage(navController = navController)
            }

            // Phase 14: 餐后拍摄功能
            composable(Screen.AfterMealCamera.route) { backStackEntry ->
                val comparisonId = backStackEntry.arguments?.getString("comparisonId")?.toIntOrNull() ?: 0
                AfterMealCameraPage(navController = navController, comparisonId = comparisonId)
            }

            // Phase 17: 热量收支统计页面
            composable(Screen.Stats.route) {
                StatsPage(navController = navController)
            }

            // Phase 27: 运动轨迹追踪
            composable(
                route = Screen.ExerciseTracking.route,
                arguments = listOf(
                    androidx.navigation.navArgument("planId") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    androidx.navigation.navArgument("exerciseType") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "walking"
                    }
                )
            ) { backStackEntry ->
                val planIdStr = backStackEntry.arguments?.getString("planId")
                val planId = planIdStr?.toIntOrNull()
                val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "walking"
                ExerciseTrackingPage(
                    navController = navController,
                    planId = planId,
                    exerciseType = exerciseType
                )
            }

            // Phase 28: 运动结算展示
            composable(
                route = Screen.ExerciseSummary.route,
                arguments = listOf(
                    androidx.navigation.navArgument("planId") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    androidx.navigation.navArgument("exerciseType") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "walking"
                    },
                    androidx.navigation.navArgument("distance") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "0.0"
                    },
                    androidx.navigation.navArgument("duration") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "0"
                    },
                    androidx.navigation.navArgument("calories") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "0.0"
                    },
                    androidx.navigation.navArgument("pace") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "0.0"
                    }
                )
            ) { backStackEntry ->
                val planIdStr = backStackEntry.arguments?.getString("planId")
                val planId = planIdStr?.toIntOrNull()
                val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "walking"
                val distance = backStackEntry.arguments?.getString("distance")?.toDoubleOrNull() ?: 0.0
                val duration = backStackEntry.arguments?.getString("duration")?.toLongOrNull() ?: 0L
                val calories = backStackEntry.arguments?.getString("calories")?.toDoubleOrNull() ?: 0.0
                val pace = backStackEntry.arguments?.getString("pace")?.toDoubleOrNull() ?: 0.0
                ExerciseSummaryPage(
                    navController = navController,
                    planId = planId,
                    exerciseType = exerciseType,
                    distance = distance,
                    duration = duration,
                    calories = calories,
                    pace = pace
                )
            }

            // Phase 42: 个性化菜品推荐
            composable(Screen.Recommendation.route) {
                RecommendationPage(navController = navController)
            }

            // Phase 48: 健康目标达成情况
            composable(Screen.GoalProgress.route) {
                GoalProgressPage(navController = navController)
            }

            // Phase 49: 运动历史记录
            composable(Screen.ExerciseHistory.route) {
                ExerciseHistoryPage(navController = navController)
            }

            // Phase 47: 离线运动包管理
            composable(
                route = Screen.OfflinePackage.route,
                arguments = listOf(
                    androidx.navigation.navArgument("planId") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val planIdStr = backStackEntry.arguments?.getString("planId")
                val planId = planIdStr?.toIntOrNull()
                OfflinePackagePage(
                    navController = navController,
                    planId = planId
                )
            }
        }
    }
}
