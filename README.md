# LifeHub - 智能生活服务工具

## 项目简介

LifeHub（慧生活）是一款基于AI的智能生活服务工具，提供**餐饮营养分析**和**餐后运动规划**两大核心功能。通过AI技术，帮助用户科学管理饮食健康，智能规划餐后运动，实现健康生活的闭环管理。

### 核心功能

- 🍽️ **餐饮营养分析**：菜品营养查询、菜单图片识别、饮食记录管理
- 🏃 **餐后运动规划**：AI生成个性化运动计划、卡路里消耗规划、运动计划管理
- 👤 **用户中心**：个人偏好设置、健康目标管理

## 技术栈

### Android前端
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM
- **网络请求**: Retrofit2 + OkHttp3
- **异步处理**: Kotlin Coroutines + Flow
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 36

### 后端
- **开发语言**: Python 3.10+
- **Web框架**: FastAPI
- **AI服务**: 阿里云通义千问API (DashScope)

## 项目结构

```
LifeHub/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/lifehub/
│   │   │   ├── data/                    # 数据层
│   │   │   │   ├── FoodData.kt         # 菜品数据模型
│   │   │   │   ├── FoodRequest.kt      # 菜品请求模型
│   │   │   │   ├── FoodResponse.kt     # API响应模型
│   │   │   │   ├── TripData.kt         # 行程数据模型
│   │   │   │   ├── UserData.kt         # 用户数据模型
│   │   │   │   └── UserSession.kt      # 用户会话管理
│   │   │   ├── network/                 # 网络层
│   │   │   │   ├── ApiService.kt       # Retrofit接口定义
│   │   │   │   └── RetrofitClient.kt   # Retrofit客户端配置
│   │   │   ├── viewmodel/               # ViewModel层
│   │   │   │   ├── FoodViewModel.kt    # 菜品查询ViewModel
│   │   │   │   ├── TripViewModel.kt    # 行程管理ViewModel
│   │   │   │   └── UserViewModel.kt    # 用户管理ViewModel
│   │   │   ├── navigation/              # 导航层
│   │   │   │   ├── MainNavigation.kt   # 主导航配置
│   │   │   │   └── Screen.kt           # 路由定义
│   │   │   ├── ui/                      # UI层
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomePage.kt            # 首页
│   │   │   │   │   ├── NutritionDetailPage.kt # 营养详情页
│   │   │   │   │   ├── CameraPage.kt          # 相机拍照页
│   │   │   │   │   ├── TodayDietRecordsPage.kt    # 今日饮食记录
│   │   │   │   │   ├── AllDietRecordsPage.kt      # 所有饮食记录
│   │   │   │   │   ├── TripListPage.kt        # 行程列表页
│   │   │   │   │   ├── TripPlanningPage.kt    # 行程规划页
│   │   │   │   │   ├── TripDetailPage.kt     # 行程详情页
│   │   │   │   │   ├── ProfilePage.kt         # 个人资料页
│   │   │   │   │   └── LoginPage.kt           # 登录页
│   │   │   │   └── theme/              # 主题配置
│   │   │   │       ├── Color.kt        # 颜色定义
│   │   │   │       ├── Theme.kt        # 主题设置
│   │   │   │       └── Type.kt         # 字体设置
│   │   │   └── MainActivity.kt          # 主活动
│   │   ├── AndroidManifest.xml          # 应用配置文件
│   │   └── res/                         # 资源文件
│   └── build.gradle.kts                 # 应用级Gradle配置
└── build.gradle.kts                     # 项目级Gradle配置
```

## 功能特性

### 1. 餐饮营养分析 🍽️

#### 1.1 菜品营养查询
- ✅ 输入菜品名称，AI智能分析营养成分
- ✅ 显示热量、蛋白质、脂肪、碳水化合物等详细数据
- ✅ 基于用户健康目标的个性化推荐
- ✅ 美观的营养成分卡片展示

#### 1.2 菜单图片识别
- ✅ 拍照或从相册选择菜单图片
- ✅ AI识别菜单中的多个菜品
- ✅ 自动分析每个菜品的营养成分
- ✅ 根据用户健康目标提供推荐建议
- ✅ 显示推荐理由和营养数据

#### 1.3 饮食记录管理
- ✅ 记录每日饮食（早餐、午餐、晚餐、加餐）
- ✅ 查看今日饮食记录
- ✅ 查看历史饮食记录（按日期分组）
- ✅ 记录营养成分数据
- ✅ 支持手动添加饮食记录

### 2. 餐后运动规划 🏃

#### 2.1 运动计划生成
- ✅ 自然语言描述运动需求（如"规划餐后运动，消耗300卡路里"）
- ✅ AI智能生成个性化运动计划
- ✅ 根据今日饮食记录自动计算卡路里消耗目标
- ✅ 考虑用户健康目标和运动偏好
- ✅ 自动规划运动类型、地点、时长等节点
- ✅ 显示预计消耗卡路里和运动时长

#### 2.2 运动计划管理
- ✅ 查看所有运动计划列表
- ✅ 查看最近运动计划（快速访问）
- ✅ 查看运动计划详情（包含所有运动节点）
- ✅ 运动计划状态管理（规划中/进行中/已完成）

### 3. 用户中心 👤

#### 3.1 个人偏好设置
- ✅ 设置健康目标（减脂/增肌/控糖/均衡）
- ✅ 设置过敏原列表
- ✅ 设置运动偏好（散步/跑步/骑行等）
- ✅ 健康目标与运动计划联动

#### 3.2 用户信息管理
- ✅ 查看和编辑个人资料
- ✅ 用户登录和会话管理

### 4. UI/UX特性 ✨

- ✅ Material Design 3 设计规范
- ✅ 流畅的页面导航和转场动画
- ✅ 完整的加载状态和错误处理
- ✅ 响应式布局，适配不同屏幕尺寸
- ✅ 统一的配色方案和视觉风格

## 配色方案

| 颜色名称                 | 色值      | 用途                     |
| ------------------------ | --------- | ------------------------ |
| 森林绿 (ForestGreen)     | `#2D5A27` | 主色调，导航栏、强调元素 |
| 活力橙 (VitalOrange)     | `#FF6B35` | 强调色，按钮、数值显示   |
| 米白色 (BackgroundBeige) | `#F8F5F0` | 背景色                   |
| 纯白 (CardBackground)    | `#FFFFFF` | 卡片背景                 |
| 主文字 (TextPrimary)     | `#1C1C1E` | 主要文字颜色             |
| 次要文字 (TextSecondary) | `#8E8E93` | 次要文字颜色             |

## 环境配置

### 前置要求
1. **Android Studio**: Koala | 2024.1.1 或更高版本
2. **JDK**: 11 或更高版本
3. **Android SDK**: API 26+ (compileSdk 36)
4. **Python后端**: 需要先启动后端服务（见后端文档）

### 依赖安装

项目使用Gradle自动管理依赖，主要依赖包括：

```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.activity:activity-compose:1.8.2")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// CameraX (用于拍照功能)
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

Gradle会自动下载这些依赖，首次同步可能需要几分钟时间。

## 快速开始

### 1. 启动后端服务

进入后端目录并启动服务：

```bash
cd ../backend
python -m venv venv
venv\Scripts\activate          # Windows
source venv/bin/activate       # Linux/Mac

pip install -r requirements_simple.txt

# 配置环境变量（创建.env文件）
DASHSCOPE_API_KEY=your_api_key_here
HOST=0.0.0.0
PORT=8000

# 启动服务
uvicorn app.main:app --reload
```

后端服务启动后会运行在 `http://localhost:8000`

### 2. 配置Android项目

#### 方式A：使用Android模拟器（推荐）

模拟器会自动将 `10.0.2.2` 映射到主机的 `localhost`，无需修改配置。

#### 方式B：使用真机调试

1. 确保手机和电脑在同一WiFi网络
2. 查看电脑的局域网IP地址（如：`192.168.1.100`）
3. 修改 `RetrofitClient.kt` 中的 `BASE_URL`：

```kotlin
// 修改前
private const val BASE_URL = "http://10.0.2.2:8000"

// 修改后（替换为你的电脑IP）
private const val BASE_URL = "http://192.168.1.100:8000"
```

### 3. 运行应用

1. 用Android Studio打开项目
2. 等待Gradle同步完成
3. 点击运行按钮或按 `Shift + F10`
4. 选择模拟器或真机设备

## 使用说明

### 餐饮营养分析

#### 查询菜品营养

1. 在首页或营养详情页输入菜品名称（例如："番茄炒蛋"）
2. 点击"查询"按钮
3. 等待AI分析结果（通常3-5秒）
4. 查看营养成分和AI推荐建议

#### 识别菜单图片

1. 进入相机页面
2. 点击拍照按钮或从相册选择菜单图片
3. 等待AI识别（通常5-10秒）
4. 查看识别出的菜品列表和营养分析
5. 根据推荐建议选择合适的菜品

#### 记录饮食

1. 在营养详情页查看菜品信息后，可以添加到饮食记录
2. 或手动在饮食记录页面添加记录
3. 选择餐次（早餐/午餐/晚餐/加餐）
4. 选择记录日期
5. 查看今日或历史饮食记录

### 餐后运动规划

#### 生成运动计划

1. 进入运动规划页面
2. 输入运动需求（例如："规划餐后运动，消耗300卡路里"）
3. 系统会根据用户今日饮食记录和偏好自动生成运动计划
4. 查看生成的运动计划详情，包括：
   - 运动计划标题和运动区域
   - 运动日期
   - 运动节点（散步、跑步、骑行等）
   - 每个节点的预计时间和消耗卡路里

#### 查看运动计划

1. 在运动计划列表页查看所有计划
2. 点击某个计划查看详细信息
3. 查看每个运动节点的详细说明和注意事项

### 用户设置

1. 进入个人资料页面
2. 设置健康目标（减脂/增肌/控糖/均衡）
3. 添加过敏原（如：海鲜、花生等）
4. 设置出行偏好和预算
5. 保存设置，系统会根据偏好提供个性化推荐

### 示例查询

#### 菜品营养查询

| 菜品名称 | 说明             |
| -------- | ---------------- |
| 番茄炒蛋 | 家常菜，营养均衡 |
| 红烧肉   | 高热量菜品       |
| 凉拌黄瓜 | 低热量蔬菜       |
| 鱼香肉丝 | 川菜代表         |

#### 运动规划查询

| 查询示例                    | 说明               |
| --------------------------- | ------------------ |
| 规划餐后运动，消耗300卡路里 | 根据卡路里目标规划 |
| 餐后散步30分钟              | 指定运动类型和时长 |
| 公园健走                    | 指定运动地点和类型 |

## API接口说明

LifeHub Android应用使用以下后端API接口。详细的API文档请参考 `backend/API文档.md`。

### 餐饮营养分析接口

#### 1. 分析菜品营养成分
- **接口**: `POST /api/food/analyze`
- **功能**: 输入菜品名称，返回AI分析的营养成分

#### 2. 菜单图片识别
- **接口**: `POST /api/food/recognize`
- **功能**: 上传菜单图片，识别菜品并分析营养

#### 3. 获取最新识别结果
- **接口**: `GET /api/food/latest-recognition`
- **功能**: 获取用户最新的菜单识别结果

#### 4. 添加饮食记录
- **接口**: `POST /api/food/record`
- **功能**: 添加用户的饮食记录

#### 5. 获取饮食记录
- **接口**: `GET /api/food/records`
- **功能**: 获取用户所有饮食记录（按日期分组）

#### 6. 获取今日饮食记录
- **接口**: `GET /api/food/records/today`
- **功能**: 获取用户今天的饮食记录

### 运动规划接口

#### 7. 生成运动计划
- **接口**: `POST /api/trip/generate`
- **功能**: 根据用户查询和偏好，AI生成个性化运动计划（餐后运动规划）

#### 8. 获取运动计划列表
- **接口**: `GET /api/trip/list`
- **功能**: 获取用户全部运动计划列表

#### 9. 获取最近运动计划
- **接口**: `GET /api/trip/recent`
- **功能**: 获取用户最近运动计划

#### 10. 获取首页运动计划
- **接口**: `GET /api/trip/home`
- **功能**: 获取首页展示的运动计划（最近的几个）

#### 11. 获取运动计划详情
- **接口**: `GET /api/trip/{tripId}`
- **功能**: 获取某个运动计划的具体信息

### 用户中心接口

#### 12. 获取用户偏好
- **接口**: `GET /api/user/preferences`
- **功能**: 获取用户的偏好设置

#### 13. 更新用户偏好
- **接口**: `PUT /api/user/preferences`
- **功能**: 更新用户的偏好设置（支持部分更新）

### 接口请求示例

#### 分析菜品营养
```kotlin
// 请求
POST /api/food/analyze
{
  "food_name": "番茄炒蛋"
}

// 响应
{
  "success": true,
  "message": "分析成功",
  "data": {
    "name": "番茄炒蛋",
    "calories": 150.0,
    "protein": 10.5,
    "fat": 8.2,
    "carbs": 6.3,
    "recommendation": "这道菜营养均衡，适合减脂期食用。"
  }
}
```

#### 生成运动计划
```kotlin
// 请求
POST /api/trip/generate
{
  "userId": 123,
  "query": "规划餐后运动，消耗300卡路里",
  "preferences": {
    "healthGoal": "reduce_fat",
    "allergens": []
  }
}

// 响应
{
  "code": 200,
  "message": "运动计划生成成功",
  "data": {
    "tripId": 456,
    "title": "餐后运动计划（消耗300卡路里）",
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
        "notes": "餐后散步"
      }
    ]
  }
}
```

更多接口详情请参考 `backend/API文档.md` 或访问 `http://localhost:8000/docs` 查看交互式API文档。

## 常见问题

### Q1: 网络请求失败
**A**: 检查以下几点：
- 后端服务是否正常运行（访问 `http://localhost:8000/docs`）
- 手机/模拟器是否能访问后端（检查IP配置）
- AndroidManifest.xml 是否已添加网络权限
- 是否设置了 `usesCleartextTraffic="true"`

### Q2: Gradle同步失败
**A**: 
- 检查网络连接
- 清理项目：Build → Clean Project
- 重新同步：File → Sync Project with Gradle Files

### Q3: AI返回格式错误
**A**: 
- 检查后端环境变量 `DASHSCOPE_API_KEY` 是否配置正确
- 查看后端日志，确认AI服务是否正常响应

### Q4: 模拟器无法连接后端
**A**: 
- 确认使用 `10.0.2.2` 而不是 `localhost` 或 `127.0.0.1`
- 检查防火墙是否阻止了8000端口

### Q5: 拍照功能无法使用
**A**: 
- 检查AndroidManifest.xml是否已添加相机权限
- 确认设备是否支持相机功能
- 检查是否授予了相机权限（Android 6.0+需要运行时权限）

### Q6: 图片上传失败
**A**: 
- 检查图片大小是否过大（建议小于5MB）
- 确认网络连接正常
- 检查后端服务是否正常运行

### Q7: 运动计划生成失败
**A**: 
- 检查后端AI服务配置是否正确
- 确认查询文本是否清晰明确（建议包含卡路里目标或运动类型）
- 查看后端日志了解详细错误信息

## 开发计划

### 当前版本 v1.0.0
- ✅ 菜品营养查询功能
- ✅ 菜单图片识别功能
- ✅ 饮食记录管理（今日/历史）
- ✅ 餐后运动规划生成（AI生成运动计划）
- ✅ 运动计划列表和详情查看
- ✅ 卡路里消耗计算和显示
- ✅ 用户偏好设置
- ✅ Material Design 3 UI
- ✅ MVVM架构
- ✅ Navigation导航系统
- ✅ 完整的错误处理和加载状态
- ✅ 相机拍照功能

### 未来规划 v1.1.0+
- [ ] 语音输入查询
- [ ] 饮食记录统计分析（热量趋势、营养均衡度）
- [ ] 运动计划分享功能
- [ ] 运动计划编辑和自定义
- [ ] 收藏功能（收藏菜品、运动计划）
- [ ] 推送通知（饮食提醒、运动提醒）
- [ ] 天气API集成（根据天气推荐室内/室外运动）
- [ ] POI查询（查询附近运动场所）
- [ ] 多语言支持
- [ ] 深色模式
- [ ] 数据导出功能
- [ ] 离线缓存支持

## 开发规范

### 代码风格
- 遵循Kotlin官方编码规范
- 使用有意义的变量和函数命名
- 添加必要的注释和文档
- 保持代码简洁和可读性

### 架构规范
- 严格遵循MVVM架构模式
- ViewModel负责业务逻辑，UI只负责展示
- 使用Kotlin Coroutines处理异步操作
- 网络请求统一通过Repository层

### Git提交规范
- 提交信息使用中文，清晰描述改动内容
- 格式：`[类型] 简短描述`
- 类型：`feat`(新功能)、`fix`(修复)、`docs`(文档)、`style`(格式)、`refactor`(重构)

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m '[feat] 添加新功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

## 许可证

本项目仅用于学习和软件创新大赛，请勿用于商业用途。

## 相关文档

- [后端API文档](../backend/API文档.md) - 完整的后端API接口说明
- [后端快速开始](../backend/快速开始.md) - 后端服务配置和启动指南
- [前端API接口文档](./API接口文档.md) - 前端API使用说明
- [快速开始指南](./快速开始指南.md) - 快速上手指南

## 技术架构

### 前端架构
```
UI层 (Compose)
    ↓
ViewModel层 (状态管理)
    ↓
Repository层 (数据仓库)
    ↓
Network层 (Retrofit)
    ↓
后端API
```

### 数据流
1. **用户操作** → UI层触发事件
2. **ViewModel** → 处理业务逻辑，调用Repository
3. **Repository** → 调用API服务
4. **API响应** → 返回数据给ViewModel
5. **状态更新** → ViewModel更新State
6. **UI刷新** → Compose自动重组


---

**LifeHub - 让生活更智慧 🌿**

