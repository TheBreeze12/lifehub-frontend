# LifeHub - 智能生活服务工具

## 项目简介

LifeHub（慧生活）是一款基于AI的智能生活服务工具，目前实现了**菜品营养查询**功能，用户输入菜品名称即可获得AI分析的营养成分和健康建议。

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
│   │   │   │   └── FoodResponse.kt     # API响应模型
│   │   │   ├── network/                 # 网络层
│   │   │   │   ├── ApiService.kt       # Retrofit接口定义
│   │   │   │   └── RetrofitClient.kt   # Retrofit客户端
│   │   │   ├── viewmodel/               # ViewModel层
│   │   │   │   └── FoodViewModel.kt    # 菜品查询ViewModel
│   │   │   ├── ui/                      # UI层
│   │   │   │   ├── screen/
│   │   │   │   │   └── FoodScreen.kt   # 菜品查询界面
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

### 1. 菜品营养查询
- ✅ 输入菜品名称
- ✅ AI智能分析营养成分（热量、蛋白质、脂肪、碳水化合物）
- ✅ 基于减脂目标的个性化推荐
- ✅ 美观的Material Design 3界面
- ✅ 完整的加载和错误状态处理

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
// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

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

### 查询菜品营养

1. 在输入框中输入菜品名称（例如："番茄炒蛋"）
2. 点击"查询"按钮
3. 等待AI分析结果
4. 查看营养成分和AI推荐建议

### 示例查询

| 菜品名称 | 说明             |
| -------- | ---------------- |
| 番茄炒蛋 | 家常菜，营养均衡 |
| 红烧肉   | 高热量菜品       |
| 凉拌黄瓜 | 低热量蔬菜       |
| 鱼香肉丝 | 川菜代表         |

## API接口说明

### POST /api/food/analyze

**请求体：**
```json
{
  "food_name": "番茄炒蛋"
}
```

**响应体：**
```json
{
  "success": true,
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

## 开发计划

### 当前版本 v0.1.0
- ✅ 菜品营养查询功能
- ✅ Material Design 3 UI
- ✅ MVVM架构
- ✅ 错误处理和加载状态

### 未来规划
- [ ] 拍照识别菜品
- [ ] 语音输入查询
- [ ] 历史记录功能
- [ ] 收藏功能
- [ ] 智能出行规划
- [ ] 多语言支持

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

## 许可证

本项目仅用于学习和软件创新大赛，请勿用于商业用途。

## 联系方式

项目开发者：软件创新团队  
项目地址：`C:\Users\86166\OneDrive\桌面\软件创新\LifeHub`

---

**LifeHub - 让生活更智慧 🌿**

