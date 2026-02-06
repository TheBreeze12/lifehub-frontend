package com.example.lifehub

import com.example.lifehub.data.*
import com.example.lifehub.navigation.Screen
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 1-24 综合测试
 * 覆盖所有数据模型、Gson序列化、导航路由、业务逻辑、边界条件
 */
class ComprehensivePhase1to24Test {

    private val gson = Gson()

    // =====================================================================
    // Phase 1: JWT认证 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase1 - LoginRequest construction`() {
        val req = LoginRequest(username = "testuser", password = "pass123")
        assertEquals("testuser", req.username)
        assertEquals("pass123", req.password)
    }

    @Test
    fun `Phase1 - LoginResponse success`() {
        val data = LoginData(userId = 1, username = "test", nickname = "昵称")
        val resp = LoginResponse(code = 200, message = "登录成功", loginData = data)
        assertEquals(200, resp.code)
        assertNotNull(resp.loginData)
        assertEquals(1, resp.loginData!!.userId)
    }

    @Test
    fun `Phase1 - LoginResponse failure`() {
        val resp = LoginResponse(code = 401, message = "密码错误", loginData = null)
        assertEquals(401, resp.code)
        assertNull(resp.loginData)
    }

    @Test
    fun `Phase1 - UserRegistrationRequest construction`() {
        val req = UserRegistrationRequest(nickname = "新用户", password = "secure123")
        assertEquals("新用户", req.nickname)
        assertEquals("secure123", req.password)
    }

    @Test
    fun `Phase1 - UserRegistrationResponse success`() {
        val resp = UserRegistrationResponse(code = 200, message = "注册成功", userId = 42)
        assertEquals(200, resp.code)
        assertEquals(42, resp.userId)
    }

    @Test
    fun `Phase1 - UserRegistrationResponse duplicate user`() {
        val resp = UserRegistrationResponse(code = 400, message = "用户已存在", userId = null)
        assertEquals(400, resp.code)
        assertNull(resp.userId)
    }

    @Test
    fun `Phase1 - LoginRequest Gson serialization`() {
        val req = LoginRequest(username = "admin", password = "abc")
        val json = gson.toJson(req)
        assertTrue(json.contains("\"username\""))
        assertTrue(json.contains("\"password\""))
        val deserialized = gson.fromJson(json, LoginRequest::class.java)
        assertEquals(req, deserialized)
    }

    @Test
    fun `Phase1 - UserRegistrationRequest Gson serialization`() {
        val req = UserRegistrationRequest(nickname = "测试", password = "pw")
        val json = gson.toJson(req)
        val deserialized = gson.fromJson(json, UserRegistrationRequest::class.java)
        assertEquals(req.nickname, deserialized.nickname)
        assertEquals(req.password, deserialized.password)
    }

    // =====================================================================
    // Phase 2-3: 饮食记录CRUD - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase2 - DietRecord full construction`() {
        val record = DietRecord(
            id = 10, userId = 1, foodName = "宫保鸡丁",
            calories = 320.0, protein = 28.0, fat = 18.0, carbs = 15.0,
            mealType = "lunch", recordDate = "2026-02-06", createdAt = "2026-02-06T12:00:00"
        )
        assertEquals(10, record.id)
        assertEquals("宫保鸡丁", record.foodName)
        assertEquals(320.0, record.calories, 0.01)
        assertEquals("lunch", record.mealType)
    }

    @Test
    fun `Phase2 - AddDietRecordRequest construction`() {
        val req = AddDietRecordRequest(
            userId = 1, foodName = "番茄炒蛋", calories = 150.0,
            protein = 10.5, fat = 8.2, carbs = 6.3,
            mealType = "dinner", recordDate = "2026-02-06"
        )
        assertEquals(1, req.userId)
        assertEquals("番茄炒蛋", req.foodName)
        assertEquals("dinner", req.mealType)
    }

    @Test
    fun `Phase3 - UpdateDietRecordRequest partial update`() {
        val req = UpdateDietRecordRequest(userId = 1, foodName = "新名称")
        assertEquals(1, req.userId)
        assertEquals("新名称", req.foodName)
        assertNull(req.calories)
        assertNull(req.protein)
        assertNull(req.fat)
        assertNull(req.carbs)
        assertNull(req.mealType)
        assertNull(req.recordDate)
    }

    @Test
    fun `Phase3 - UpdateDietRecordRequest full update`() {
        val req = UpdateDietRecordRequest(
            userId = 1, foodName = "更新菜名", calories = 400.0,
            protein = 30.0, fat = 20.0, carbs = 25.0,
            mealType = "breakfast", recordDate = "2026-03-01"
        )
        assertEquals(400.0, req.calories!!, 0.01)
        assertEquals("breakfast", req.mealType)
    }

    @Test
    fun `Phase3 - UpdateDietRecordResponse success with data`() {
        val data = DietRecordData(
            id = 5, foodName = "Updated", calories = 300.0,
            protein = 20.0, fat = 10.0, carbs = 40.0,
            mealType = "lunch", recordDate = "2026-02-06"
        )
        val resp = UpdateDietRecordResponse(code = 200, message = "更新成功", data = data)
        assertEquals(200, resp.code)
        assertNotNull(resp.data)
        assertEquals(5, resp.data!!.id)
    }

    @Test
    fun `Phase3 - DietRecordsByDateResponse Gson deserialization`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "2026-02-06": [
                    {
                        "id": 1, "userId": 1, "foodName": "米饭",
                        "calories": 230.0, "protein": 4.0, "fat": 0.5, "carbs": 51.0,
                        "mealType": "lunch", "recordDate": "2026-02-06",
                        "createdAt": "2026-02-06T12:00:00"
                    }
                ]
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, DietRecordsByDateResponse::class.java)
        assertEquals(200, resp.code)
        assertNotNull(resp.data)
        assertTrue(resp.data!!.containsKey("2026-02-06"))
        assertEquals(1, resp.data!!["2026-02-06"]!!.size)
        assertEquals("米饭", resp.data!!["2026-02-06"]!![0].foodName)
    }

    @Test
    fun `Phase3 - DietRecordsByDateResponse empty data`() {
        val json = """{"code": 200, "message": "获取成功", "data": {}}"""
        val resp = gson.fromJson(json, DietRecordsByDateResponse::class.java)
        assertEquals(200, resp.code)
        assertTrue(resp.data!!.isEmpty())
    }

    @Test
    fun `Phase3 - all meal types English`() {
        val types = listOf("breakfast", "lunch", "dinner", "snack")
        types.forEach { type ->
            val req = AddDietRecordRequest(1, "食物", 100.0, 5.0, 3.0, 10.0, type, "2026-02-06")
            assertEquals(type, req.mealType)
        }
    }

    @Test
    fun `Phase3 - all meal types Chinese`() {
        val types = listOf("早餐", "午餐", "晚餐", "加餐")
        types.forEach { type ->
            val req = AddDietRecordRequest(1, "食物", 100.0, 5.0, 3.0, 10.0, type, "2026-02-06")
            assertEquals(type, req.mealType)
        }
    }

    @Test
    fun `Phase3 - ApiResponse delete success`() {
        val resp = ApiResponse(code = 200, message = "删除成功", data = null)
        assertEquals(200, resp.code)
        assertNull(resp.data)
    }

    @Test
    fun `Phase3 - DietRecord equals and hashCode`() {
        val r1 = DietRecord(1, 1, "菜", 100.0, 5.0, 3.0, 10.0, "lunch", "2026-02-06", "2026-02-06T12:00:00")
        val r2 = DietRecord(1, 1, "菜", 100.0, 5.0, 3.0, 10.0, "lunch", "2026-02-06", "2026-02-06T12:00:00")
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `Phase3 - DietRecord not equals different id`() {
        val r1 = DietRecord(1, 1, "菜", 100.0, 5.0, 3.0, 10.0, "lunch", "2026-02-06", "t")
        val r2 = DietRecord(2, 1, "菜", 100.0, 5.0, 3.0, 10.0, "lunch", "2026-02-06", "t")
        assertNotEquals(r1, r2)
    }

    @Test
    fun `Phase3 - boundary - zero nutrition values`() {
        val req = AddDietRecordRequest(1, "水", 0.0, 0.0, 0.0, 0.0, "snack", "2026-02-06")
        assertEquals(0.0, req.calories, 0.01)
    }

    @Test
    fun `Phase3 - boundary - very large nutrition values`() {
        val req = AddDietRecordRequest(1, "大餐", 9999.99, 500.0, 400.0, 800.0, "dinner", "2026-02-06")
        assertEquals(9999.99, req.calories, 0.01)
    }

    @Test
    fun `Phase3 - boundary - special characters in food name`() {
        val names = listOf("麻辣火锅（特辣）", "Sushi 🍣", "Café au lait", "Pizza \"Margherita\"")
        names.forEach { name ->
            val req = UpdateDietRecordRequest(userId = 1, foodName = name)
            assertEquals(name, req.foodName)
        }
    }

    // =====================================================================
    // Phase 4-5: 身体参数 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase4 - UpdatePreferencesRequest with body params`() {
        val req = UpdatePreferencesRequest(
            userId = 1, healthGoal = "reduce_fat", allergens = listOf("花生"),
            travelPreference = "walking", dailyBudget = 200,
            weight = 70.5, height = 175.0, age = 25, gender = "male"
        )
        assertEquals(70.5, req.weight!!, 0.01)
        assertEquals(175.0, req.height!!, 0.01)
        assertEquals(25, req.age)
        assertEquals("male", req.gender)
    }

    @Test
    fun `Phase4 - UpdatePreferencesRequest without body params`() {
        val req = UpdatePreferencesRequest(
            userId = 1, healthGoal = "balanced",
            allergens = null, travelPreference = null, dailyBudget = null
        )
        assertNull(req.weight)
        assertNull(req.height)
        assertNull(req.age)
        assertNull(req.gender)
    }

    @Test
    fun `Phase5 - UserPreferencesData with body params`() {
        val data = UserPreferencesData(
            userId = 1, nickname = "测试用户", healthGoal = "gain_muscle",
            allergens = listOf("牛奶", "鸡蛋"), travelPreference = "self_driving",
            dailyBudget = 300, weight = 80.0, height = 180.0, age = 30, gender = "male"
        )
        assertEquals(80.0, data.weight!!, 0.01)
        assertEquals(180.0, data.height!!, 0.01)
        assertEquals(30, data.age)
        assertEquals("male", data.gender)
    }

    @Test
    fun `Phase5 - UserPreferencesResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "获取成功",
            "data": {
                "userId": 1, "nickname": "user1", "healthGoal": "reduce_fat",
                "allergens": ["花生"], "travelPreference": "walking",
                "dailyBudget": 100, "weight": 65.0, "height": 170.0, "age": 28, "gender": "female"
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, UserPreferencesResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(65.0, resp.data!!.weight!!, 0.01)
        assertEquals("female", resp.data!!.gender)
    }

    @Test
    fun `Phase5 - all health goal values`() {
        val goals = listOf("reduce_fat", "gain_muscle", "control_sugar", "balanced")
        goals.forEach { goal ->
            val req = UpdatePreferencesRequest(userId = 1, healthGoal = goal,
                allergens = null, travelPreference = null, dailyBudget = null)
            assertEquals(goal, req.healthGoal)
        }
    }

    @Test
    fun `Phase5 - all gender values`() {
        val genders = listOf("male", "female", "other")
        genders.forEach { g ->
            val req = UpdatePreferencesRequest(userId = 1, healthGoal = null,
                allergens = null, travelPreference = null, dailyBudget = null, gender = g)
            assertEquals(g, req.gender)
        }
    }

    @Test
    fun `Phase5 - boundary - weight edge values`() {
        val req1 = UpdatePreferencesRequest(userId = 1, healthGoal = null,
            allergens = null, travelPreference = null, dailyBudget = null, weight = 0.1)
        assertEquals(0.1, req1.weight!!, 0.01)

        val req2 = UpdatePreferencesRequest(userId = 1, healthGoal = null,
            allergens = null, travelPreference = null, dailyBudget = null, weight = 500.0)
        assertEquals(500.0, req2.weight!!, 0.01)
    }

    // =====================================================================
    // Phase 6-9: 过敏原 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase6 - FoodData with allergens`() {
        val food = FoodData(
            name = "宫保鸡丁", calories = 320.0, protein = 28.0,
            fat = 18.0, carbs = 15.0, recommendation = "适合增肌",
            allergens = listOf("peanut"), allergenReasoning = "含花生"
        )
        assertNotNull(food.allergens)
        assertEquals(1, food.allergens!!.size)
        assertEquals("peanut", food.allergens!![0])
        assertEquals("含花生", food.allergenReasoning)
    }

    @Test
    fun `Phase6 - FoodData without allergens (backward compatible)`() {
        val food = FoodData(
            name = "米饭", calories = 230.0, protein = 4.0,
            fat = 0.5, carbs = 51.0, recommendation = "主食"
        )
        assertNull(food.allergens)
        assertNull(food.allergenReasoning)
    }

    @Test
    fun `Phase7 - FoodData Gson with allergens serialization`() {
        val food = FoodData(
            name = "鱼香肉丝", calories = 250.0, protein = 20.0,
            fat = 15.0, carbs = 10.0, recommendation = "经典川菜",
            allergens = listOf("soy", "wheat"), allergenReasoning = "酱油含大豆和小麦"
        )
        val json = gson.toJson(food)
        assertTrue(json.contains("\"allergens\""))
        assertTrue(json.contains("\"allergen_reasoning\""))
        val deserialized = gson.fromJson(json, FoodData::class.java)
        assertEquals(2, deserialized.allergens!!.size)
        assertTrue(deserialized.allergens!!.contains("soy"))
    }

    @Test
    fun `Phase7 - FoodResponse with allergens Gson deserialization`() {
        val json = """
        {
            "success": true, "message": "分析成功",
            "data": {
                "name": "虾饺", "calories": 200.0, "protein": 15.0,
                "fat": 8.0, "carbs": 18.0, "recommendation": "蒸菜",
                "allergens": ["shellfish", "wheat"],
                "allergen_reasoning": "虾饺含虾和面皮"
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, FoodResponse::class.java)
        assertTrue(resp.success)
        assertEquals(2, resp.data!!.allergens!!.size)
        assertTrue(resp.data!!.allergens!!.contains("shellfish"))
    }

    @Test
    fun `Phase8 - eight major allergen codes`() {
        val allergenCodes = listOf("milk", "egg", "fish", "shellfish", "peanut", "tree_nut", "wheat", "soy")
        assertEquals(8, allergenCodes.size)
        allergenCodes.forEach { code ->
            val food = FoodData("测试", 100.0, 5.0, 3.0, 10.0, "测试", listOf(code), null)
            assertTrue(food.allergens!!.contains(code))
        }
    }

    @Test
    fun `Phase9 - UserPreferences with allergen list`() {
        val prefs = UpdatePreferencesRequest(
            userId = 1, healthGoal = null,
            allergens = listOf("花生", "牛奶", "鸡蛋", "海鲜", "自定义过敏原"),
            travelPreference = null, dailyBudget = null
        )
        assertEquals(5, prefs.allergens!!.size)
        assertTrue(prefs.allergens!!.contains("自定义过敏原"))
    }

    @Test
    fun `Phase9 - UserPreferences empty allergen list`() {
        val prefs = UpdatePreferencesRequest(
            userId = 1, healthGoal = null,
            allergens = emptyList(), travelPreference = null, dailyBudget = null
        )
        assertTrue(prefs.allergens!!.isEmpty())
    }

    // =====================================================================
    // Phase 10-14: 餐前餐后对比 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase10 - MealComparisonRecord initial state`() {
        val record = MealComparisonRecord(
            comparisonId = 1, beforeImageUrl = "/images/before1.jpg"
        )
        assertEquals(1, record.comparisonId)
        assertNotNull(record.beforeImageUrl)
        assertNull(record.afterImageUrl)
        assertNull(record.consumptionRatio)
        assertNull(record.netCalories)
        assertEquals("pending_after", record.status)
    }

    @Test
    fun `Phase10 - MealComparisonRecord completed state`() {
        val record = MealComparisonRecord(
            comparisonId = 1,
            beforeImageUrl = "/images/before1.jpg",
            afterImageUrl = "/images/after1.jpg",
            consumptionRatio = 0.7,
            originalCalories = 500.0,
            netCalories = 350.0,
            status = "completed"
        )
        assertEquals("completed", record.status)
        assertEquals(0.7, record.consumptionRatio!!, 0.01)
        assertEquals(350.0, record.netCalories!!, 0.01)
    }

    @Test
    fun `Phase11 - BeforeMealResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "上传成功",
            "data": {
                "comparison_id": 42,
                "before_image_url": "/uploads/before_42.jpg",
                "before_features": {
                    "dishes": [
                        {"name": "红烧肉", "estimated_weight": 200, "estimated_calories": 500.0,
                         "estimated_protein": 25.0, "estimated_fat": 35.0, "estimated_carbs": 10.0}
                    ],
                    "total_estimated_calories": 500.0,
                    "total_estimated_protein": 25.0,
                    "total_estimated_fat": 35.0,
                    "total_estimated_carbs": 10.0
                },
                "status": "pending_after"
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, BeforeMealResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(42, resp.data!!.comparisonId)
        assertNotNull(resp.data!!.beforeFeatures)
        assertEquals(1, resp.data!!.beforeFeatures!!.dishes!!.size)
        assertEquals("红烧肉", resp.data!!.beforeFeatures!!.dishes!![0].name)
        assertEquals(500.0, resp.data!!.beforeFeatures!!.totalEstimatedCalories!!, 0.01)
    }

    @Test
    fun `Phase12 - AfterMealResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "对比完成",
            "data": {
                "comparison_id": 42,
                "before_image_url": "/uploads/before_42.jpg",
                "after_image_url": "/uploads/after_42.jpg",
                "consumption_ratio": 0.75,
                "original_calories": 500.0,
                "net_calories": 375.0,
                "original_protein": 25.0,
                "original_fat": 35.0,
                "original_carbs": 10.0,
                "net_protein": 18.75,
                "net_fat": 26.25,
                "net_carbs": 7.5,
                "comparison_analysis": "吃了约75%",
                "status": "completed"
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, AfterMealResponse::class.java)
        assertEquals(200, resp.code)
        val data = resp.data!!
        assertEquals(42, data.comparisonId)
        assertEquals(0.75, data.consumptionRatio, 0.01)
        assertEquals(500.0, data.originalCalories, 0.01)
        assertEquals(375.0, data.netCalories, 0.01)
        assertEquals(18.75, data.netProtein, 0.01)
        assertEquals(26.25, data.netFat, 0.01)
        assertEquals(7.5, data.netCarbs, 0.01)
        assertEquals("completed", data.status)
    }

    @Test
    fun `Phase12 - net calories calculation logic`() {
        val originalCalories = 600.0
        val consumptionRatio = 0.8
        val expectedNet = originalCalories * consumptionRatio
        assertEquals(480.0, expectedNet, 0.01)
    }

    @Test
    fun `Phase12 - consumption ratio edge cases`() {
        // 全部吃完
        val full = 1.0
        assertEquals(500.0, 500.0 * full, 0.01)
        // 没吃
        val none = 0.0
        assertEquals(0.0, 500.0 * none, 0.01)
        // 一半
        val half = 0.5
        assertEquals(250.0, 500.0 * half, 0.01)
    }

    @Test
    fun `Phase13 - DishFeature construction`() {
        val dish = DishFeature(
            name = "青椒炒肉", estimatedWeight = 150,
            estimatedCalories = 280.0, estimatedProtein = 20.0,
            estimatedFat = 15.0, estimatedCarbs = 12.0
        )
        assertEquals("青椒炒肉", dish.name)
        assertEquals(150, dish.estimatedWeight)
        assertEquals(280.0, dish.estimatedCalories!!, 0.01)
    }

    @Test
    fun `Phase13 - MealFeatures with multiple dishes`() {
        val dishes = listOf(
            DishFeature("菜1", 100, 200.0, 10.0, 8.0, 20.0),
            DishFeature("菜2", 150, 300.0, 15.0, 12.0, 25.0)
        )
        val features = MealFeatures(
            dishes = dishes,
            totalEstimatedCalories = 500.0,
            totalEstimatedProtein = 25.0,
            totalEstimatedFat = 20.0,
            totalEstimatedCarbs = 45.0
        )
        assertEquals(2, features.dishes!!.size)
        assertEquals(500.0, features.totalEstimatedCalories!!, 0.01)
    }

    @Test
    fun `Phase14 - AfterMealData nutritional proportions consistency`() {
        val data = AfterMealData(
            comparisonId = 1,
            beforeImageUrl = "b.jpg", afterImageUrl = "a.jpg",
            consumptionRatio = 0.6,
            originalCalories = 400.0, netCalories = 240.0,
            originalProtein = 30.0, originalFat = 20.0, originalCarbs = 35.0,
            netProtein = 18.0, netFat = 12.0, netCarbs = 21.0,
            comparisonAnalysis = "约60%", status = "completed"
        )
        // 验证净摄入 = 原始 * 消耗比例
        assertEquals(data.originalCalories * data.consumptionRatio, data.netCalories, 0.01)
        assertEquals(data.originalProtein * data.consumptionRatio, data.netProtein, 0.01)
        assertEquals(data.originalFat * data.consumptionRatio, data.netFat, 0.01)
        assertEquals(data.originalCarbs * data.consumptionRatio, data.netCarbs, 0.01)
    }

    // =====================================================================
    // Phase 15-18: 数据统计 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase15 - DailyCalorieStats construction`() {
        val stats = DailyCalorieStats(
            date = "2026-02-06", userId = 1,
            intakeCalories = 2000.0, mealCount = 3,
            burnCalories = 500.0, exerciseCount = 1,
            exerciseDuration = 45, netCalories = 1500.0
        )
        assertEquals("2026-02-06", stats.date)
        assertEquals(2000.0, stats.intakeCalories, 0.01)
        assertEquals(500.0, stats.burnCalories, 0.01)
        assertEquals(1500.0, stats.netCalories, 0.01)
    }

    @Test
    fun `Phase15 - DailyCalorieStats default values`() {
        val stats = DailyCalorieStats(date = "2026-02-06", userId = 1)
        assertEquals(0.0, stats.intakeCalories, 0.01)
        assertEquals(0, stats.mealCount)
        assertEquals(0.0, stats.burnCalories, 0.01)
        assertEquals(0, stats.exerciseCount)
        assertEquals(0, stats.exerciseDuration)
        assertEquals(0.0, stats.netCalories, 0.01)
        assertNull(stats.mealBreakdown)
    }

    @Test
    fun `Phase15 - WeeklyCalorieStats construction`() {
        val daily = listOf(
            DailyBreakdown("2026-02-03", 1800.0, 400.0, 1400.0),
            DailyBreakdown("2026-02-04", 2200.0, 600.0, 1600.0)
        )
        val weekly = WeeklyCalorieStats(
            weekStart = "2026-02-03", weekEnd = "2026-02-09", userId = 1,
            totalIntake = 4000.0, totalBurn = 1000.0, totalNet = 3000.0,
            avgIntake = 2000.0, avgBurn = 500.0, avgNet = 1500.0,
            totalMeals = 6, totalExercises = 2, activeDays = 2,
            dailyBreakdown = daily
        )
        assertEquals(2, weekly.dailyBreakdown.size)
        assertEquals(4000.0, weekly.totalIntake, 0.01)
        assertEquals(2000.0, weekly.avgIntake, 0.01)
    }

    @Test
    fun `Phase15 - DailyCalorieStatsResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "获取成功",
            "data": {
                "date": "2026-02-06", "user_id": 1,
                "intake_calories": 1800.0, "meal_count": 3,
                "burn_calories": 300.0, "exercise_count": 1,
                "exercise_duration": 30, "net_calories": 1500.0,
                "meal_breakdown": {"breakfast": 400.0, "lunch": 800.0, "dinner": 600.0}
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, DailyCalorieStatsResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(1800.0, resp.data!!.intakeCalories, 0.01)
        assertNotNull(resp.data!!.mealBreakdown)
        assertEquals(3, resp.data!!.mealBreakdown!!.size)
    }

    @Test
    fun `Phase16 - DailyNutrientStats construction`() {
        val stats = DailyNutrientStats(
            date = "2026-02-06", userId = 1,
            totalProtein = 80.0, totalFat = 60.0, totalCarbs = 250.0,
            totalCalories = 1860.0,
            proteinCalories = 320.0, fatCalories = 540.0, carbsCalories = 1000.0,
            proteinRatio = 17.2, fatRatio = 29.0, carbsRatio = 53.8,
            mealCount = 3
        )
        assertEquals(80.0, stats.totalProtein, 0.01)
        assertEquals(17.2, stats.proteinRatio, 0.1)
        // 比例之和应约等于100
        val totalRatio = stats.proteinRatio + stats.fatRatio + stats.carbsRatio
        assertEquals(100.0, totalRatio, 1.0)
    }

    @Test
    fun `Phase16 - NutrientComparison statuses`() {
        val low = NutrientComparison(actualRatio = 8.0, recommendedMin = 10.0, recommendedMax = 15.0, status = "low", message = "偏低")
        val normal = NutrientComparison(actualRatio = 12.0, recommendedMin = 10.0, recommendedMax = 15.0, status = "normal", message = "正常")
        val high = NutrientComparison(actualRatio = 18.0, recommendedMin = 10.0, recommendedMax = 15.0, status = "high", message = "偏高")

        assertEquals("low", low.status)
        assertTrue(low.actualRatio < low.recommendedMin)
        assertEquals("normal", normal.status)
        assertTrue(normal.actualRatio in normal.recommendedMin..normal.recommendedMax)
        assertEquals("high", high.status)
        assertTrue(high.actualRatio > high.recommendedMax)
    }

    @Test
    fun `Phase16 - GuidelinesComparison full construction`() {
        val comp = GuidelinesComparison(
            protein = NutrientComparison(12.0, 10.0, 15.0, "normal", "正常"),
            fat = NutrientComparison(28.0, 20.0, 30.0, "normal", "正常"),
            carbs = NutrientComparison(58.0, 50.0, 65.0, "normal", "正常")
        )
        assertNotNull(comp.protein)
        assertNotNull(comp.fat)
        assertNotNull(comp.carbs)
    }

    @Test
    fun `Phase17 - ChartDataPoint net calculation`() {
        val point = ChartDataPoint(label = "周一", intake = 2000f, burn = 500f)
        assertEquals(1500f, point.net)
    }

    @Test
    fun `Phase17 - ChartDataPoint zero burn`() {
        val point = ChartDataPoint(label = "休息日", intake = 1800f, burn = 0f)
        assertEquals(1800f, point.net)
    }

    @Test
    fun `Phase17 - ChartDataPoint negative net when burn exceeds intake`() {
        val point = ChartDataPoint(label = "减脂日", intake = 500f, burn = 800f)
        assertEquals(-300f, point.net)
    }

    @Test
    fun `Phase17 - StatsViewMode enum values`() {
        assertEquals(2, StatsViewMode.entries.size)
        assertTrue(StatsViewMode.entries.contains(StatsViewMode.DAILY))
        assertTrue(StatsViewMode.entries.contains(StatsViewMode.WEEKLY))
    }

    @Test
    fun `Phase18 - RadarChartDataPoint normalizedValue normal case`() {
        val point = RadarChartDataPoint(
            label = "蛋白质", value = 12.0,
            recommendedMin = 10.0, recommendedMax = 15.0, status = "normal"
        )
        // normalizedValue = 12.0 / 15.0 = 0.8
        assertEquals(0.8f, point.normalizedValue, 0.01f)
    }

    @Test
    fun `Phase18 - RadarChartDataPoint normalizedValue clamped high`() {
        val point = RadarChartDataPoint(
            label = "脂肪", value = 50.0,
            recommendedMin = 20.0, recommendedMax = 30.0, status = "high"
        )
        // 50/30 = 1.667, clamped to 1.5
        assertEquals(1.5f, point.normalizedValue, 0.01f)
    }

    @Test
    fun `Phase18 - RadarChartDataPoint normalizedValue clamped low`() {
        val point = RadarChartDataPoint(
            label = "碳水", value = 0.0,
            recommendedMin = 50.0, recommendedMax = 65.0, status = "low"
        )
        assertEquals(0.0f, point.normalizedValue, 0.01f)
    }

    @Test
    fun `Phase18 - DailyNutrientStatsResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "获取成功",
            "data": {
                "date": "2026-02-06", "user_id": 1,
                "total_protein": 80.0, "total_fat": 60.0, "total_carbs": 250.0,
                "total_calories": 1860.0,
                "protein_calories": 320.0, "fat_calories": 540.0, "carbs_calories": 1000.0,
                "protein_ratio": 17.2, "fat_ratio": 29.0, "carbs_ratio": 53.8,
                "meal_count": 3,
                "guidelines_comparison": {
                    "protein": {"actual_ratio": 17.2, "recommended_min": 10.0, "recommended_max": 15.0, "status": "high", "message": "偏高"},
                    "fat": {"actual_ratio": 29.0, "recommended_min": 20.0, "recommended_max": 30.0, "status": "normal", "message": "正常"},
                    "carbs": {"actual_ratio": 53.8, "recommended_min": 50.0, "recommended_max": 65.0, "status": "normal", "message": "正常"}
                }
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, DailyNutrientStatsResponse::class.java)
        assertEquals(200, resp.code)
        val data = resp.data!!
        assertEquals(17.2, data.proteinRatio, 0.1)
        assertNotNull(data.guidelinesComparison)
        assertEquals("high", data.guidelinesComparison!!.protein!!.status)
        assertEquals("normal", data.guidelinesComparison!!.fat!!.status)
    }

    // =====================================================================
    // Phase 19-22: 运动路径规划 (后端) - 前端对应数据模型测试
    // =====================================================================

    @Test
    fun `Phase19 - TripItem with METs data Gson roundtrip`() {
        val item = TripItem(
            dayIndex = 1, startTime = "19:00", placeName = "公园",
            placeType = "walking", duration = 30, cost = 150.0, notes = "散步30分钟"
        )
        val json = gson.toJson(item)
        val deserialized = gson.fromJson(json, TripItem::class.java)
        assertEquals(item.placeName, deserialized.placeName)
        assertEquals(item.duration, deserialized.duration)
        assertEquals(item.cost, deserialized.cost)
    }

    @Test
    fun `Phase22 - GenerateRoutesRequest construction`() {
        val req = GenerateRoutesRequest(
            startLat = 39.9, startLng = 116.4,
            targetCalories = 300.0, maxTimeMinutes = 60,
            exerciseType = "walking", weightKg = 70.0
        )
        assertEquals(39.9, req.startLat, 0.01)
        assertEquals(116.4, req.startLng, 0.01)
        assertEquals(300.0, req.targetCalories, 0.01)
        assertEquals(60, req.maxTimeMinutes)
    }

    @Test
    fun `Phase22 - GenerateRoutesRequest default values`() {
        val req = GenerateRoutesRequest(startLat = 31.2, startLng = 121.5, targetCalories = 200.0)
        assertEquals(60, req.maxTimeMinutes)
        assertEquals("walking", req.exerciseType)
        assertEquals(70.0, req.weightKg!!, 0.01)
    }

    @Test
    fun `Phase22 - GenerateRoutesRequest Gson serialization snake_case`() {
        val req = GenerateRoutesRequest(startLat = 39.9, startLng = 116.4, targetCalories = 300.0)
        val json = gson.toJson(req)
        assertTrue(json.contains("\"start_lat\""))
        assertTrue(json.contains("\"start_lng\""))
        assertTrue(json.contains("\"target_calories\""))
        assertTrue(json.contains("\"max_time_minutes\""))
    }

    // =====================================================================
    // Phase 23-24: 地图与路线 - 数据模型测试
    // =====================================================================

    @Test
    fun `Phase24 - RouteWaypoint construction`() {
        val wp = RouteWaypoint(lat = 39.9, lng = 116.4, order = 0, type = "start")
        assertEquals(39.9, wp.lat, 0.01)
        assertEquals(116.4, wp.lng, 0.01)
        assertEquals(0, wp.order)
        assertEquals("start", wp.type)
    }

    @Test
    fun `Phase24 - RouteWaypoint defaults`() {
        val wp = RouteWaypoint(lat = 39.9, lng = 116.4)
        assertEquals(0, wp.order)
        assertEquals("waypoint", wp.type)
    }

    @Test
    fun `Phase24 - ParetoRoute construction`() {
        val waypoints = listOf(
            RouteWaypoint(39.90, 116.40, 0, "start"),
            RouteWaypoint(39.91, 116.41, 1, "waypoint"),
            RouteWaypoint(39.92, 116.42, 2, "end")
        )
        val route = ParetoRoute(
            routeId = 1, routeName = "最短时间路线",
            timeMinutes = 25.0, caloriesBurn = 180.0,
            greeneryScore = 7.5, distanceMeters = 2500.0,
            waypoints = waypoints, exerciseType = "walking", intensity = 3.5
        )
        assertEquals(1, route.routeId)
        assertEquals("最短时间路线", route.routeName)
        assertEquals(25.0, route.timeMinutes, 0.01)
        assertEquals(180.0, route.caloriesBurn, 0.01)
        assertEquals(3, route.waypoints.size)
        assertEquals("walking", route.exerciseType)
    }

    @Test
    fun `Phase24 - ParetoRoute empty waypoints`() {
        val route = ParetoRoute(
            routeId = 1, routeName = "空路线",
            timeMinutes = 0.0, caloriesBurn = 0.0,
            greeneryScore = 0.0, distanceMeters = 0.0
        )
        assertTrue(route.waypoints.isEmpty())
        assertNull(route.exerciseType)
        assertNull(route.intensity)
    }

    @Test
    fun `Phase24 - RoutesResponseData construction`() {
        val routes = listOf(
            ParetoRoute(1, "路线A", 20.0, 150.0, 8.0, 2000.0),
            ParetoRoute(2, "路线B", 30.0, 250.0, 9.0, 3000.0)
        )
        val data = RoutesResponseData(
            routes = routes,
            startPoint = RouteWaypoint(39.9, 116.4, 0, "start"),
            targetCalories = 200.0, maxTimeMinutes = 60,
            exerciseType = "walking", weightKg = 70.0, nRoutes = 2
        )
        assertEquals(2, data.routes.size)
        assertEquals(2, data.nRoutes)
        assertEquals(200.0, data.targetCalories, 0.01)
    }

    @Test
    fun `Phase24 - GenerateRoutesResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "路径生成成功",
            "data": {
                "routes": [
                    {
                        "route_id": 1, "route_name": "最快路线",
                        "time_minutes": 20.0, "calories_burn": 180.0,
                        "greenery_score": 6.5, "distance_meters": 2000.0,
                        "waypoints": [
                            {"lat": 39.9, "lng": 116.4, "order": 0, "type": "start"},
                            {"lat": 39.91, "lng": 116.41, "order": 1, "type": "end"}
                        ],
                        "exercise_type": "walking", "intensity": 3.5
                    },
                    {
                        "route_id": 2, "route_name": "最佳绿化",
                        "time_minutes": 35.0, "calories_burn": 250.0,
                        "greenery_score": 9.0, "distance_meters": 3500.0,
                        "waypoints": [],
                        "exercise_type": "walking", "intensity": 4.0
                    }
                ],
                "start_point": {"lat": 39.9, "lng": 116.4, "order": 0, "type": "start"},
                "target_calories": 200.0,
                "max_time_minutes": 60,
                "exercise_type": "walking",
                "weight_kg": 70.0,
                "n_routes": 2
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, GenerateRoutesResponse::class.java)
        assertEquals(200, resp.code)
        val data = resp.data!!
        assertEquals(2, data.routes.size)
        assertEquals("最快路线", data.routes[0].routeName)
        assertEquals(2, data.routes[0].waypoints.size)
        assertEquals("最佳绿化", data.routes[1].routeName)
        assertEquals(0, data.routes[1].waypoints.size)
        assertEquals(2, data.nRoutes)
    }

    @Test
    fun `Phase24 - multiple Pareto routes have different trade-offs`() {
        val fastest = ParetoRoute(1, "最快", 15.0, 120.0, 5.0, 1500.0)
        val greenest = ParetoRoute(2, "最绿", 40.0, 300.0, 9.5, 4000.0)
        val balanced = ParetoRoute(3, "均衡", 25.0, 200.0, 7.5, 2500.0)

        assertTrue(fastest.timeMinutes < greenest.timeMinutes)
        assertTrue(greenest.greeneryScore > fastest.greeneryScore)
        assertTrue(balanced.timeMinutes > fastest.timeMinutes && balanced.timeMinutes < greenest.timeMinutes)
    }

    // =====================================================================
    // 导航路由测试
    // =====================================================================

    @Test
    fun `Navigation - all Screen routes are unique`() {
        val routes = listOf(
            Screen.Home.route, Screen.Camera.route, Screen.NutritionDetail.route,
            Screen.TripPlanning.route, Screen.TripDetail.route, Screen.TripList.route,
            Screen.Profile.route, Screen.Login.route, Screen.Register.route,
            Screen.TodayDietRecords.route, Screen.AllDietRecords.route,
            Screen.MealComparison.route, Screen.BeforeMealCamera.route,
            Screen.AfterMealCamera.route, Screen.Stats.route
        )
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `Navigation - NutritionDetail createRoute`() {
        val route = Screen.NutritionDetail.createRoute("番茄炒蛋")
        assertEquals("nutrition_detail/番茄炒蛋", route)
    }

    @Test
    fun `Navigation - TripDetail createRoute`() {
        val route = Screen.TripDetail.createRoute("42")
        assertEquals("trip_detail/42", route)
    }

    @Test
    fun `Navigation - AfterMealCamera createRoute`() {
        val route = Screen.AfterMealCamera.createRoute(99)
        assertEquals("after_meal_camera/99", route)
    }

    @Test
    fun `Navigation - Screen route patterns`() {
        assertTrue(Screen.NutritionDetail.route.contains("{dishName}"))
        assertTrue(Screen.TripDetail.route.contains("{tripId}"))
        assertTrue(Screen.AfterMealCamera.route.contains("{comparisonId}"))
    }

    // =====================================================================
    // 通用数据模型测试
    // =====================================================================

    @Test
    fun `General - FoodRequest Gson serialization uses food_name`() {
        val req = FoodRequest(foodName = "番茄炒蛋")
        val json = gson.toJson(req)
        assertTrue("FoodRequest should serialize to food_name", json.contains("\"food_name\""))
        assertFalse("FoodRequest should NOT serialize to foodName", json.contains("\"foodName\""))
    }

    @Test
    fun `General - FoodRequest Gson deserialization from food_name`() {
        val json = """{"food_name": "红烧肉"}"""
        val req = gson.fromJson(json, FoodRequest::class.java)
        assertEquals("红烧肉", req.foodName)
    }

    @Test
    fun `General - FoodResponse success Gson roundtrip`() {
        val resp = FoodResponse(
            success = true, message = "分析成功",
            data = FoodData("鱼", 200.0, 30.0, 8.0, 0.0, "健康", listOf("fish"), "含鱼")
        )
        val json = gson.toJson(resp)
        val deserialized = gson.fromJson(json, FoodResponse::class.java)
        assertTrue(deserialized.success)
        assertEquals("鱼", deserialized.data!!.name)
        assertEquals(listOf("fish"), deserialized.data!!.allergens)
    }

    @Test
    fun `General - FoodResponse failure`() {
        val resp = FoodResponse(success = false, message = "AI服务不可用", data = null)
        assertFalse(resp.success)
        assertNull(resp.data)
    }

    @Test
    fun `General - RecognizeMenuResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "识别成功",
            "data": {
                "dishes": [
                    {"name": "宫保鸡丁", "calories": 320.0, "protein": 28.0, "fat": 18.0, "carbs": 15.0, "isRecommended": true, "reason": "蛋白质丰富"},
                    {"name": "糖醋里脊", "calories": 280.0, "protein": 20.0, "fat": 12.0, "carbs": 25.0, "isRecommended": false, "reason": "糖分较高"}
                ]
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, RecognizeMenuResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(2, resp.data!!.dishes.size)
        assertTrue(resp.data!!.dishes[0].isRecommended)
        assertFalse(resp.data!!.dishes[1].isRecommended)
    }

    @Test
    fun `General - TripPlan full construction`() {
        val items = listOf(
            TripItem(1, "19:00", "公园", "walking", 30, 150.0, "散步"),
            TripItem(1, "19:30", "操场", "running", 20, 200.0, "跑步")
        )
        val plan = TripPlan(
            tripId = 1, title = "餐后运动", destination = "附近公园",
            startDate = "2026-02-06", endDate = "2026-02-06", items = items
        )
        assertEquals(1, plan.tripId)
        assertEquals(2, plan.items.size)
        assertEquals("walking", plan.items[0].placeType)
    }

    @Test
    fun `General - TripSummary construction`() {
        val summary = TripSummary(
            tripId = 1, title = "运动计划", destination = "公园",
            startDate = "2026-02-06", endDate = "2026-02-06",
            status = "planning", itemCount = 3
        )
        assertEquals("planning", summary.status)
        assertEquals(3, summary.itemCount)
    }

    @Test
    fun `General - GenerateTripRequest with location`() {
        val req = GenerateTripRequest(
            userId = 1, query = "消耗300卡",
            preferences = UserPreferences("reduce_fat", listOf("花生")),
            latitude = 39.9, longitude = 116.4
        )
        assertEquals(39.9, req.latitude!!, 0.01)
        assertEquals(116.4, req.longitude!!, 0.01)
    }

    @Test
    fun `General - GenerateTripRequest without location`() {
        val req = GenerateTripRequest(userId = 1, query = "散步", preferences = null)
        assertNull(req.latitude)
        assertNull(req.longitude)
    }

    @Test
    fun `General - WeatherResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "获取成功",
            "data": {
                "address": "北京市", "latitude": 39.9, "longitude": 116.4,
                "temperature": 5.0, "windspeed": 3.0, "winddirection": 180,
                "weathercode": 1, "time": "2026-02-06T10:00",
                "hourly": {
                    "time": ["2026-02-06T10:00", "2026-02-06T11:00"],
                    "temperature_2m": [5.0, 6.0],
                    "precipitation": [0.0, 0.0]
                }
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, WeatherResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(5.0, resp.data!!.temperature!!, 0.01)
        assertEquals(2, resp.data!!.hourly!!.time!!.size)
    }

    @Test
    fun `General - WeatherData null fields`() {
        val data = WeatherData(null, null, null, null, null, null, null, null, null)
        assertNull(data.address)
        assertNull(data.temperature)
        assertNull(data.hourly)
    }

    @Test
    fun `General - DietHistoryResponse Gson deserialization`() {
        val json = """
        {
            "code": 200, "message": "获取成功",
            "data": {
                "totalCalories": 2500.0,
                "targetCalories": 2000.0,
                "records": [
                    {"id": 1, "userId": 1, "foodName": "米饭", "calories": 230.0,
                     "protein": 4.0, "fat": 0.5, "carbs": 51.0,
                     "mealType": "lunch", "recordDate": "2026-02-06",
                     "createdAt": "2026-02-06T12:00:00"}
                ]
            }
        }
        """.trimIndent()
        val resp = gson.fromJson(json, DietHistoryResponse::class.java)
        assertEquals(200, resp.code)
        assertEquals(2500.0, resp.data!!.totalCalories, 0.01)
        assertEquals(1, resp.data!!.records.size)
    }

    // =====================================================================
    // 边界条件与异常场景测试
    // =====================================================================

    @Test
    fun `Edge - very long food name`() {
        val longName = "超级无敌豪华至尊版".repeat(10) // 90 chars
        val req = FoodRequest(foodName = longName)
        assertEquals(longName, req.foodName)
    }

    @Test
    fun `Edge - empty dishes list in recognition`() {
        val data = RecognizeMenuData(dishes = emptyList())
        assertTrue(data.dishes.isEmpty())
    }

    @Test
    fun `Edge - DietRecord with very precise nutrition`() {
        val record = DietRecord(
            id = 1, userId = 1, foodName = "精确",
            calories = 123.456789, protein = 10.123456, fat = 5.654321, carbs = 20.111111,
            mealType = "lunch", recordDate = "2026-02-06", createdAt = "t"
        )
        assertEquals(123.456789, record.calories, 0.000001)
    }

    @Test
    fun `Edge - empty UserPreferences allergens`() {
        val prefs = UserPreferences(healthGoal = "balanced", allergens = emptyList())
        assertTrue(prefs.allergens!!.isEmpty())
    }

    @Test
    fun `Edge - null UserPreferences`() {
        val prefs = UserPreferences(healthGoal = null, allergens = null)
        assertNull(prefs.healthGoal)
        assertNull(prefs.allergens)
    }

    @Test
    fun `Edge - TripPlan empty items`() {
        val plan = TripPlan(1, "空计划", null, "2026-02-06", "2026-02-06")
        assertTrue(plan.items.isEmpty())
        assertNull(plan.destination)
    }

    @Test
    fun `Edge - WeeklyCalorieStats empty breakdown`() {
        val weekly = WeeklyCalorieStats(
            weekStart = "2026-02-03", weekEnd = "2026-02-09", userId = 1
        )
        assertTrue(weekly.dailyBreakdown.isEmpty())
        assertEquals(0.0, weekly.totalIntake, 0.01)
        assertEquals(0, weekly.activeDays)
    }

    @Test
    fun `Edge - coordinates at equator and prime meridian`() {
        val wp = RouteWaypoint(lat = 0.0, lng = 0.0)
        assertEquals(0.0, wp.lat, 0.01)
        assertEquals(0.0, wp.lng, 0.01)
    }

    @Test
    fun `Edge - negative coordinates`() {
        val wp = RouteWaypoint(lat = -33.87, lng = -151.21) // Sydney area
        assertTrue(wp.lat < 0)
        assertTrue(wp.lng < 0)
    }

    @Test
    fun `Edge - ParetoRoute with maximum practical values`() {
        val route = ParetoRoute(
            routeId = 1, routeName = "马拉松",
            timeMinutes = 300.0, caloriesBurn = 3000.0,
            greeneryScore = 10.0, distanceMeters = 42195.0
        )
        assertEquals(42195.0, route.distanceMeters, 0.1)
    }

    @Test
    fun `Edge - Gson null handling for optional fields`() {
        val json = """
        {
            "code": 200, "message": null,
            "data": null
        }
        """.trimIndent()
        val resp = gson.fromJson(json, GenerateRoutesResponse::class.java)
        assertEquals(200, resp.code)
        assertNull(resp.message)
        assertNull(resp.data)
    }
}
