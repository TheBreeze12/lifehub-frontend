package com.example.lifehub.navigation

/** 应用页面路由定义 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object NutritionDetail : Screen("nutrition_detail/{dishName}") {
        fun createRoute(dishName: String) = "nutrition_detail/$dishName"
    }
    object TripPlanning : Screen("trip_planning")
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object TripList : Screen("trip_list")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Register : Screen("register")

    object TodayDietRecords : Screen("today_diet_records")
    object AllDietRecords : Screen("all_diet_records")

    // Phase 13: 餐前餐后对比功能
    object MealComparison : Screen("meal_comparison")
    object BeforeMealCamera : Screen("before_meal_camera")

    // Phase 14: 餐后拍摄功能
    object AfterMealCamera : Screen("after_meal_camera/{comparisonId}") {
        fun createRoute(comparisonId: Int) = "after_meal_camera/$comparisonId"
    }

    // Phase 17: 热量收支统计页面
    object Stats : Screen("stats")

    // Phase 27: 运动轨迹追踪
    object ExerciseTracking : Screen("exercise_tracking?planId={planId}&exerciseType={exerciseType}") {
        fun createRoute(planId: Int? = null, exerciseType: String = "walking"): String {
            val pid = planId?.toString() ?: ""
            return "exercise_tracking?planId=$pid&exerciseType=$exerciseType"
        }
    }
}
