# LifeHub - 智能生活服务工具

## 项目简介

LifeHub（慧生活）是一款基于AI的智能生活服务工具，提供**餐饮营养分析**和**智能出行规划**两大核心功能。通过AI技术，帮助用户科学管理饮食健康，智能规划出行行程。

### 核心功能

- 🍽️ **餐饮营养分析**：菜品营养查询、菜单图片识别、饮食记录管理
- 🗺️ **智能出行规划**：AI生成个性化行程、行程管理、行程详情查看
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

### 2. 智能出行规划 🗺️

#### 2.1 行程生成
- ✅ 自然语言描述出行需求
- ✅ AI智能生成个性化行程计划
- ✅ 考虑用户健康目标和过敏原
- ✅ 自动规划景点、餐饮、交通等节点
- ✅ 显示预计时间和费用

#### 2.2 行程管理
- ✅ 查看所有行程列表
- ✅ 查看最近行程（快速访问）
- ✅ 查看行程详情（包含所有节点）
- ✅ 行程状态管理（规划中/进行中/已完成）

### 3. 用户中心 👤

#### 3.1 个人偏好设置
- ✅ 设置健康目标（减脂/增肌/控糖/均衡）
- ✅ 设置过敏原列表
- ✅ 设置出行偏好（自驾/公共交通/步行）
- ✅ 设置出行日预算

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

### 智能出行规划

#### 生成行程

1. 进入行程规划页面
2. 输入出行需求（例如："规划周末带娃去杭州玩"）
3. 系统会根据用户偏好自动生成行程
4. 查看生成的行程详情，包括：
   - 行程标题和目的地
   - 开始和结束日期
   - 每日的行程节点（景点、餐饮、交通等）
   - 每个节点的预计时间和费用

#### 查看行程

1. 在行程列表页查看所有行程
2. 点击某个行程查看详细信息
3. 查看每个节点的详细说明和备注

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

#### 行程规划查询

| 查询示例                 | 说明             |
| ------------------------ | ---------------- |
| 规划周末带娃去杭州玩     | 亲子游行程       |
| 北京3日游，预算每天500元 | 带预算的行程规划 |
| 上海美食之旅             | 美食主题行程     |

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

### 行程规划接口

#### 7. 生成行程计划
- **接口**: `POST /api/trip/generate`
- **功能**: 根据用户查询和偏好，AI生成个性化行程

#### 8. 获取行程列表
- **接口**: `GET /api/trip/list`
- **功能**: 获取用户全部行程规划列表

#### 9. 获取最近行程
- **接口**: `GET /api/trip/recent`
- **功能**: 获取用户最近行程规划

#### 10. 获取首页行程
- **接口**: `GET /api/trip/home`
- **功能**: 获取首页展示的行程（最近的几个）

#### 11. 获取行程详情
- **接口**: `GET /api/trip/{tripId}`
- **功能**: 获取某个行程的具体信息

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

#### 生成行程
```kotlin
// 请求
POST /api/trip/generate
{
  "userId": 123,
  "query": "规划周末带娃去杭州玩",
  "preferences": {
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"]
  }
}

// 响应
{
  "code": 200,
  "message": "行程生成成功",
  "data": {
    "tripId": 456,
    "title": "杭州2日亲子游",
    "destination": "杭州",
    "startDate": "2026-01-25",
    "endDate": "2026-01-26",
    "items": [...]
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

### Q7: 行程生成失败
**A**: 
- 检查后端AI服务配置是否正确
- 确认查询文本是否清晰明确
- 查看后端日志了解详细错误信息

## 开发计划

### 当前版本 v1.0.0
- ✅ 菜品营养查询功能
- ✅ 菜单图片识别功能
- ✅ 饮食记录管理（今日/历史）
- ✅ 智能行程规划生成
- ✅ 行程列表和详情查看
- ✅ 用户偏好设置
- ✅ Material Design 3 UI
- ✅ MVVM架构
- ✅ Navigation导航系统
- ✅ 完整的错误处理和加载状态
- ✅ 相机拍照功能

### 未来规划 v1.1.0+
- [ ] 语音输入查询
- [ ] 饮食记录统计分析（热量趋势、营养均衡度）
- [ ] 行程分享功能
- [ ] 行程编辑和自定义
- [ ] 收藏功能（收藏菜品、行程）
- [ ] 推送通知（饮食提醒、行程提醒）
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

