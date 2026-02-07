# LifeHub 后端API接口文档

## 基础信息

**Base URL**: `http://localhost:8000`（开发环境）  
**Base URL**: `http://10.0.2.2:8000`（Android模拟器）  
**Base URL**: `http://你的电脑IP:8000`（真机调试）

**Content-Type**: `application/json`（除文件上传接口外）

**响应格式**: 所有API返回统一的JSON格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**注意**: 详细的API文档请参考 `../backend/API文档.md`，本文档主要面向Android前端开发者。

## 1. 餐饮识别服务

### 1.1 上传菜单图片识别

**接口**: `POST /api/food/recognize`

**说明**: 上传菜单图片，识别菜品并返回营养成分，根据用户健康目标提供推荐

**请求头**:
```
Content-Type: multipart/form-data
```

**请求参数**:
| 参数名 | 类型   | 必填 | 说明                                 |
| ------ | ------ | ---- | ------------------------------------ |
| image  | File   | 是   | 菜单图片文件（支持常见图片格式）     |
| userId | String | 否   | 用户ID（可选，用于根据健康目标推荐） |

**响应示例**:
```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "dishes": [
      {
        "name": "凯撒沙拉",
        "calories": 180.0,
        "protein": 12.0,
        "fat": 8.0,
        "carbs": 15.0,
        "isRecommended": true,
        "reason": "蛋白质丰富，适合您的减脂目标"
      },
      {
        "name": "奶油培根意面",
        "calories": 680.0,
        "protein": 22.0,
        "fat": 35.0,
        "carbs": 65.0,
        "isRecommended": false,
        "reason": "热量较高，建议减少摄入"
      }
    ]
  }
}
```

**实现要点**:
1. 使用OCR识别菜单文字（推荐：PaddleOCR或通义千问Qwen-VL）
2. 提取菜名列表
3. 对每个菜名调用大模型获取营养数据
4. 根据用户健康目标（从请求Header或数据库获取）判断是否推荐
5. 使用RAG检索《中国食物成分表》提高准确性

---

### 1.2 分析单个菜品营养成分

**接口**: `POST /api/food/analyze`

**说明**: 根据菜品名称分析营养成分（用于文本查询）

**请求体**:
```json
{
  "food_name": "宫保鸡丁"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "分析成功",
  "data": {
    "name": "宫保鸡丁",
    "calories": 320.0,
    "protein": 28.0,
    "fat": 18.0,
    "carbs": 15.0,
    "recommendation": "蛋白质含量高，适合健身人群"
  }
}
```

**注意**: 请求参数为 `food_name`，不是 `foodName`

---

### 1.3 添加饮食记录

**接口**: `POST /api/food/record`

**说明**: 用户将菜品添加到今日饮食记录

**请求体**:
```json
{
  "userId": 123,
  "foodName": "宫保鸡丁",
  "calories": 320.0,
  "protein": 28.0,
  "fat": 18.0,
  "carbs": 15.0,
  "mealType": "lunch",
  "recordDate": "2026-01-23"
}
```

**mealType 枚举值**:
- `breakfast` - 早餐
- `lunch` - 午餐
- `dinner` - 晚餐
- `snack` - 加餐

**响应示例**:
```json
{
  "code": 200,
  "message": "记录成功",
  "data": null
}
```

---

### 1.4 获取最新菜单识别结果

**接口**: `GET /api/food/latest-recognition`

**说明**: 获取用户最新的菜单识别结果

**请求参数**:
| 参数名 | 类型 | 必填 | 说明           |
| ------ | ---- | ---- | -------------- |
| userId | Int  | 否   | 用户ID（可选） |

**响应示例**: 与1.1相同

---

### 1.5 获取饮食记录

**接口**: `GET /api/food/records`

**说明**: 获取用户所有饮食记录，按日期分组

**请求参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "2026-01-23": [
      {
        "id": 1,
        "userId": 123,
        "foodName": "宫保鸡丁",
        "calories": 320.0,
        "protein": 28.0,
        "fat": 18.0,
        "carbs": 15.0,
        "mealType": "午餐",
        "recordDate": "2026-01-23",
        "createdAt": "2026-01-23T10:30:00"
      }
    ]
  }
}
```

---

### 1.6 获取今日饮食记录

**接口**: `GET /api/food/records/today`

**说明**: 获取用户今天的饮食记录

**请求参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**: 与1.5相同，但只包含今天的记录

---

## 2. 运动规划服务（餐后运动规划）

### 2.1 生成运动计划

**接口**: `POST /api/trip/generate`

**说明**: 根据用户查询和偏好，AI生成个性化餐后运动计划

**请求体**:
```json
{
  "userId": 123,
  "query": "规划餐后运动，消耗300卡路里",
  "preferences": {
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"]
  }
}
```

**注意**: 
- `query` 支持自然语言描述，如："规划餐后运动，消耗300卡路里"、"餐后散步30分钟"等
- `placeType` 为运动类型：`walking`(散步)、`running`(跑步)、`cycling`(骑行)、`park`(公园)、`gym`(健身房)、`indoor`(室内)、`outdoor`(户外)
- `cost` 字段表示预计消耗卡路里（kcal），不是费用

**healthGoal 枚举值**:
- `reduce_fat` - 减脂
- `gain_muscle` - 增肌
- `control_sugar` - 控糖
- `balanced` - 均衡

**响应示例**:
```json
{
  "code": 200,
  "message": "行程生成成功",
  "data": {
    "tripId": 456,
    "title": "餐后运动计划",
    "destination": "附近公园",
    "startDate": "2026-01-27",
    "endDate": "2026-01-27",
    "items": [
      {
        "dayIndex": 1,
        "startTime": "19:00",
        "placeName": "附近公园",
        "placeType": "walking",
        "duration": 30,
        "cost": 150,
        "notes": "餐后散步，建议慢走"
      }
    ]
  }
}
```

**实现要点**:
1. 使用LLM（推荐：通义千问Qwen-Max）解析用户输入
2. 提取关键信息：时间、目的地、人群、偏好
3. 调用LLM生成行程结构化JSON
4. 可选：集成高德地图API获取景点信息
5. 可选：集成和风天气API获取天气预报
6. 存储到数据库（`trip_plan` 和 `trip_item` 表）

---

### 2.2 获取运动计划详情

**接口**: `GET /api/trip/{tripId}`

**说明**: 获取指定运动计划的完整信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| tripId | Int  | 是   | 计划ID |

**响应示例**: 与"生成运动计划"的`data`字段相同

---

### 2.3 获取用户运动计划列表

**接口**: `GET /api/trip/list`

**说明**: 获取用户的所有运动计划

**查询参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "tripId": 456,
      "title": "餐后运动计划",
      "destination": "附近公园",
      "startDate": "2026-01-27",
      "endDate": "2026-01-27",
      "status": "planning",
      "itemCount": 1
    }
  ]
}
```

---

### 2.4 获取最近运动计划

**接口**: `GET /api/trip/recent`

**说明**: 获取用户最近运动计划（用于快速访问）

**查询参数**:
| 参数名 | 类型 | 必填 | 说明                  |
| ------ | ---- | ---- | --------------------- |
| userId | Int  | 是   | 用户ID                |
| limit  | Int  | 否   | 返回数量限制，默认5条 |

**响应示例**: 与2.3相同

---

### 2.5 获取首页运动计划

**接口**: `GET /api/trip/home`

**说明**: 获取首页展示的运动计划（最近的几个）

**查询参数**:
| 参数名 | 类型 | 必填 | 说明                  |
| ------ | ---- | ---- | --------------------- |
| userId | Int  | 是   | 用户ID                |
| limit  | Int  | 否   | 返回数量限制，默认3条 |

**响应示例**: 与2.3相同

---

## 3. 用户中心服务

### 3.1 用户注册 ⭐

**接口**: `POST /api/user/register`

**说明**: 注册新用户

**请求体**:
```json
{
  "nickname": "健康达人",
  "password": "securepassword123"
}
```

**请求参数**:
| 参数名   | 类型   | 必填 | 说明                   |
| -------- | ------ | ---- | ---------------------- |
| nickname | string | 是   | 用户昵称，最大50个字符 |
| password | string | 是   | 用户密码，6-128个字符  |

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "userId": 123
}
```

**错误响应**:
- 用户已存在（HTTP 400）: `{"detail": "用户已存在，nickname: 健康达人"}`
- 密码长度不符合要求（HTTP 422）: 验证错误详情

---

### 3.2 用户登录

**接口**: `GET /api/user/data`

**说明**: 通过昵称和密码登录，获取用户信息

**请求参数**:
| 参数名   | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| nickname | string | 是   | 用户昵称 |
| password | string | 是   | 用户密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 123,
    "nickname": "健康达人",
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"],
    "travelPreference": "self_driving",
    "dailyBudget": 500
  }
}
```

**错误响应**:
- 用户不存在（HTTP 404）: `{"detail": "用户不存在，nickname: 健康达人"}`
- 密码错误（HTTP 401）: `{"detail": "密码错误"}`

---

### 3.3 获取用户偏好

**接口**: `GET /api/user/preferences`

**说明**: 获取用户的偏好设置（健康目标、过敏原、出行偏好等）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 123,
    "nickname": "健康达人",
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"],
    "travelPreference": "self_driving",
    "dailyBudget": 500
  }
}
```

---

### 3.4 更新用户偏好设置

**接口**: `PUT /api/user/preferences`

**说明**: 更新用户的健康目标、过敏原、出行偏好等（支持部分更新）

**请求体**:
```json
{
  "userId": 123,
  "healthGoal": "reduce_fat",
  "allergens": ["海鲜", "花生"],
  "travelPreference": "self_driving",
  "dailyBudget": 500
}
```

**请求参数**:
| 参数名           | 类型         | 必填 | 说明                                                    |
| ---------------- | ------------ | ---- | ------------------------------------------------------- |
| userId           | int          | 是   | 用户ID，>0                                              |
| healthGoal       | string\|null | 否   | 健康目标：reduce_fat/gain_muscle/control_sugar/balanced |
| allergens        | array\|null  | 否   | 过敏原列表，如：["海鲜", "花生"]                        |
| travelPreference | string\|null | 否   | 出行偏好：self_driving/public_transport/walking         |
| dailyBudget      | int\|null    | 否   | 出行日预算（元），≥0                                    |

**travelPreference 枚举值**:
- `self_driving` - 自驾
- `public_transport` - 公共交通
- `walking` - 步行优先

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 123,
    "nickname": "健康达人",
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"],
    "travelPreference": "self_driving",
    "dailyBudget": 500
  }
}
```

---

## 4. 天气服务

### 4.1 根据计划ID查询天气 ⭐

**接口**: `GET /api/weather/by-plan`

**说明**: 通过运动计划ID查询当前天气
- 若计划包含 `latitude/longitude`，按坐标查询
- 否则按计划的 `destination` 地址查询

**请求参数**:
| 参数名 | 类型 | 必填 | 说明       |
| ------ | ---- | ---- | ---------- |
| planId | int  | 是   | 运动计划ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "address": "附近公园",
    "latitude": 39.99,
    "longitude": 116.47,
    "temperature": 2.1,
    "windspeed": 5.0,
    "winddirection": 320,
    "weathercode": 3,
    "time": "2026-02-01T10:00",
    "hourly": {
      "time": ["2026-02-01T10:00", "2026-02-01T11:00"],
      "temperature_2m": [2.1, 2.3],
      "precipitation": [0.0, 0.0]
    }
  }
}
```

**注意**: 天气服务使用Open-Meteo，无需API Key

---

## 错误码说明

| HTTP状态码 | 说明               |
| ---------- | ------------------ |
| 200        | 请求成功           |
| 400        | 请求参数错误       |
| 401        | 未授权（密码错误） |
| 404        | 资源不存在         |
| 422        | 请求参数验证失败   |
| 500        | 服务器内部错误     |

## 接口总览

| 分类     | 接口                           | 方法 | 说明               |
| -------- | ------------------------------ | ---- | ------------------ |
| **餐饮** | `/api/food/analyze`            | POST | 分析菜品营养成分   |
| **餐饮** | `/api/food/recognize`          | POST | 菜单图片识别       |
| **餐饮** | `/api/food/latest-recognition` | GET  | 获取最新识别结果   |
| **餐饮** | `/api/food/record`             | POST | 添加饮食记录       |
| **餐饮** | `/api/food/records`            | GET  | 获取所有饮食记录   |
| **餐饮** | `/api/food/records/today`      | GET  | 获取今日饮食记录   |
| **用户** | `/api/user/register`           | POST | 用户注册           |
| **用户** | `/api/user/data`               | GET  | 用户登录           |
| **用户** | `/api/user/preferences`        | GET  | 获取用户偏好       |
| **用户** | `/api/user/preferences`        | PUT  | 更新用户偏好       |
| **运动** | `/api/trip/generate`           | POST | 生成运动计划       |
| **运动** | `/api/trip/list`               | GET  | 获取运动计划列表   |
| **运动** | `/api/trip/recent`             | GET  | 获取最近运动计划   |
| **运动** | `/api/trip/home`               | GET  | 获取首页运动计划   |
| **运动** | `/api/trip/{tripId}`           | GET  | 获取运动计划详情   |
| **天气** | `/api/weather/by-plan`         | GET  | 根据计划ID查询天气 |

---

## Android前端实现要点

### Retrofit接口定义

```kotlin
interface ApiService {
    // 用户注册
    @POST("/api/user/register")
    suspend fun registerUser(@Body request: UserRegistrationRequest): UserRegistrationResponse
    
    // 用户登录
    @GET("/api/user/data")
    suspend fun getUserData(
        @Query("nickname") nickname: String,
        @Query("password") password: String
    ): UserPreferencesResponse
    
    // 菜单识别
    @Multipart
    @POST("/api/food/recognize")
    suspend fun recognizeMenu(
        @Part image: MultipartBody.Part,
        @Part("userId") userId: okhttp3.RequestBody?
    ): RecognizeMenuResponse
    
    // 生成运动计划
    @POST("/api/trip/generate")
    suspend fun generateTrip(@Body request: GenerateTripRequest): GenerateTripResponse
    
    // 查询天气
    @GET("/api/weather/by-plan")
    suspend fun getWeatherByPlan(@Query("planId") planId: Int): WeatherResponse
}
```

### ViewModel状态管理

```kotlin
// 注册状态
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val userId: Int) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

// 登录状态
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: Int) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

### 数据模型

所有数据模型定义在 `data` 包下：
- `UserRegistrationRequest` - 注册请求
- `UserRegistrationResponse` - 注册响应
- `UserPreferencesResponse` - 用户偏好响应
- `GenerateTripRequest` - 生成运动计划请求
- `WeatherResponse` - 天气响应
- 等等...

---

## 前端调用示例

### 用户注册
```kotlin
viewModelScope.launch {
    try {
        val response = apiService.registerUser(
            UserRegistrationRequest(
                nickname = "健康达人",
                password = "password123"
            )
        )
        
        if (response.code == 200 && response.userId != null) {
            _registerState.value = RegisterState.Success(response.userId)
        } else {
            _registerState.value = RegisterState.Error(response.message ?: "注册失败")
        }
    } catch (e: Exception) {
        _registerState.value = RegisterState.Error(e.message ?: "网络请求失败")
    }
}
```

### 用户登录
```kotlin
viewModelScope.launch {
    try {
        val response = apiService.getUserData(
            nickname = "健康达人",
            password = "password123"
        )
        
        if (response.code == 200 && response.data != null) {
            // 保存登录信息
            UserSession.saveLogin(
                userId = response.data.userId,
                username = nickname,
                nickname = response.data.nickname
            )
            _loginState.value = LoginState.Success(response.data.userId)
        } else {
            _loginState.value = LoginState.Error(response.message ?: "登录失败")
        }
    } catch (e: Exception) {
        _loginState.value = LoginState.Error(e.message ?: "网络请求失败")
    }
}
```

### 生成运动计划
```kotlin
viewModelScope.launch {
    try {
        val response = apiService.generateTrip(
            GenerateTripRequest(
                userId = 123,
                query = "规划餐后运动，消耗300卡路里",
                preferences = TripPreferences(
                    healthGoal = "reduce_fat",
                    allergens = listOf("海鲜")
                )
            )
        )
        
        if (response.code == 200 && response.data != null) {
            _tripState.value = TripState.Success(response.data)
        } else {
            _tripState.value = TripState.Error(response.message ?: "请求失败")
        }
    } catch (e: Exception) {
        _tripState.value = TripState.Error(e.message ?: "网络异常")
    }
}
```

### 查询天气
```kotlin
viewModelScope.launch {
    try {
        val response = apiService.getWeatherByPlan(planId = 456)
        
        if (response.code == 200 && response.data != null) {
            // 处理天气数据
            val temperature = response.data.temperature
            val weathercode = response.data.weathercode
        }
    } catch (e: Exception) {
        // 处理错误
    }
}
```

---

## 测试建议

1. **使用Swagger UI测试**（推荐）
   - 访问 http://localhost:8000/docs
   - 可以直接测试所有接口
   - 查看请求/响应格式

2. **使用Postman或Apifox**
   - 导入OpenAPI规范（从Swagger UI下载）
   - 创建测试集合
   - 自动化测试流程

3. **Android端测试**
   - 使用Android Studio的Network Inspector查看网络请求
   - 使用Logcat查看日志
   - 测试各种错误场景

4. **单元测试**
   - 编写ViewModel单元测试
   - 测试数据模型序列化/反序列化
   - 测试错误处理逻辑

---

## 注意事项

1. **Base URL配置**: 
   - 模拟器使用：`http://10.0.2.2:8000`
   - 真机使用：`http://你的电脑IP:8000`
   - 在 `RetrofitClient.kt` 中配置

2. **网络权限**: 确保 `AndroidManifest.xml` 包含网络权限

3. **用户认证**: 当前版本使用简单的昵称+密码登录，生产环境建议使用Token认证

4. **错误处理**: 所有API调用都应包含try-catch错误处理

5. **数据格式**: 
   - 日期格式：`YYYY-MM-DD`
   - 时间格式：`HH:mm`
   - 餐次支持中文（早餐/午餐/晚餐/加餐）或英文（breakfast/lunch/dinner/snack）

---

### 运动频率分析（Phase 51）

**Retrofit接口定义**:
```kotlin
// 获取运动频率分析 GET /api/stats/exercise-frequency
@GET("/api/stats/exercise-frequency")
suspend fun getExerciseFrequency(
        @Query("user_id") userId: Int,
        @Query("period") period: String = "week"
): ExerciseFrequencyResponse
```

**请求参数**:
| 参数名  | 类型   | 必填 | 说明                                    |
| ------- | ------ | ---- | --------------------------------------- |
| user_id | int    | 是   | 用户ID                                  |
| period  | string | 否   | 统计周期：week（默认）或 month          |

**响应数据模型**: `ExerciseFrequencyResponse`
- `code`: 状态码
- `message`: 消息
- `data`: `ExerciseFrequencyData`
  - `userId`, `period`, `periodLabel`, `startDate`, `endDate`
  - `totalDays`, `activeDays`, `totalExerciseCount`, `totalDuration`, `totalCalories`
  - `avgFrequency`, `avgDurationPerSession`, `avgCaloriesPerSession`
  - `dailyData`: `List<DailyExerciseFrequency>` - 每日运动频率明细
  - `typeDistribution`: `List<ExerciseTypeDistribution>` - 运动类型分布
  - `frequencyRating`: 评级（excellent/good/fair/insufficient）
  - `frequencySuggestion`: 运动频率建议

---

**最后更新**: 2026-02-07  
**文档版本**: v1.1.0  
**对应后端版本**: v1.9.0

