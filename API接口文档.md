# LifeHub 后端API接口文档 - MVP版本

## 基础信息

**Base URL**: `http://your-server-domain.com`

**Content-Type**: `application/json`

**响应格式**: 所有API返回统一的JSON格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 1. 餐饮识别服务

### 1.1 上传菜单图片识别

**接口**: `POST /api/food/recognize`

**说明**: 上传菜单图片，识别菜品并返回营养成分

**请求头**:
```
Content-Type: multipart/form-data
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明         |
| ------ | ---- | ---- | ------------ |
| image  | File | 是   | 菜单图片文件 |

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
  "foodName": "宫保鸡丁"
}
```

**响应示例**:
```json
{
  "code": 200,
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

## 2. 行程规划服务

### 2.1 生成行程

**接口**: `POST /api/trip/generate`

**说明**: 根据用户输入的自然语言生成完整行程

**请求体**:
```json
{
  "userId": 123,
  "query": "规划周末带娃去杭州玩",
  "preferences": {
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"]
  }
}
```

**healthGoal 枚举值**:
- `reduce_fat` - 减脂
- `gain_muscle` - 增肌
- `control_sugar` - 控糖
- `balanced` - 均衡

**响应示例**:
```json
{
  "code": 200,
  "message": "生成成功",
  "data": {
    "id": 456,
    "title": "杭州2日亲子游",
    "destination": "杭州",
    "startDate": "2026-01-25",
    "endDate": "2026-01-26",
    "status": "planning",
    "items": [
      {
        "id": 1001,
        "tripId": 456,
        "dayIndex": 1,
        "startTime": "09:00",
        "placeName": "西湖风景区",
        "placeType": "attraction",
        "duration": 180,
        "cost": 0.0,
        "notes": "建议游玩3小时"
      },
      {
        "id": 1002,
        "tripId": 456,
        "dayIndex": 1,
        "startTime": "12:30",
        "placeName": "楼外楼",
        "placeType": "dining",
        "duration": 90,
        "cost": 150.0,
        "notes": null
      }
    ]
  }
}
```

**placeType 枚举值**:
- `attraction` - 景点
- `dining` - 餐饮
- `transport` - 交通
- `accommodation` - 住宿

**实现要点**:
1. 使用LLM（推荐：通义千问Qwen-Max）解析用户输入
2. 提取关键信息：时间、目的地、人群、偏好
3. 调用LLM生成行程结构化JSON
4. 可选：集成高德地图API获取景点信息
5. 可选：集成和风天气API获取天气预报
6. 存储到数据库（`trip_plan` 和 `trip_item` 表）

---

### 2.2 获取行程详情

**接口**: `GET /api/trip/{tripId}`

**说明**: 获取指定行程的完整信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| tripId | Int  | 是   | 行程ID |

**响应示例**: 与"生成行程"的`data`字段相同

---

### 2.3 获取用户行程列表

**接口**: `GET /api/trip/list`

**说明**: 获取用户的所有行程（用于"最近规划"）

**查询参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 456,
      "title": "杭州2日亲子游",
      "destination": "杭州",
      "startDate": "2026-01-25",
      "endDate": "2026-01-26",
      "status": "planning"
    },
    {
      "id": 457,
      "title": "上海周末游",
      "destination": "上海",
      "startDate": "2026-02-01",
      "endDate": "2026-02-02",
      "status": "done"
    }
  ]
}
```

---

## 3. 用户中心服务

### 3.1 更新用户偏好设置

**接口**: `PUT /api/user/preferences`

**说明**: 更新用户的健康目标、过敏原、出行偏好等

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

**travelPreference 枚举值**:
- `self_driving` - 自驾
- `public_transport` - 公共交通
- `walking` - 步行优先

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

### 3.2 获取用户信息

**接口**: `GET /api/user/{userId}`

**说明**: 获取用户基本信息和偏好设置

**路径参数**:
| 参数名 | 类型 | 必填 | 说明   |
| ------ | ---- | ---- | ------ |
| userId | Int  | 是   | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "nickname": "健康达人",
    "healthGoal": "reduce_fat",
    "allergens": ["海鲜", "花生"],
    "travelPreference": "self_driving",
    "dailyBudget": 500
  }
}
```

---

### 3.3 获取饮食历史记录

**接口**: `GET /api/user/diet-history`

**说明**: 获取用户的饮食记录（用于"今日饮食"展示）

**查询参数**:
| 参数名 | 类型   | 必填 | 说明                               |
| ------ | ------ | ---- | ---------------------------------- |
| userId | Int    | 是   | 用户ID                             |
| date   | String | 否   | 日期（YYYY-MM-DD），不传则返回今日 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCalories": 1280.0,
    "targetCalories": 1800.0,
    "records": [
      {
        "id": 1,
        "userId": 123,
        "foodName": "宫保鸡丁",
        "calories": 320.0,
        "protein": 28.0,
        "fat": 18.0,
        "carbs": 15.0,
        "mealType": "lunch",
        "recordDate": "2026-01-23",
        "createdAt": "2026-01-23 12:30:00"
      }
    ]
  }
}
```

---

## 错误码说明

| 错误码 | 说明             |
| ------ | ---------------- |
| 200    | 成功             |
| 400    | 请求参数错误     |
| 401    | 未授权（未登录） |
| 404    | 资源不存在       |
| 500    | 服务器内部错误   |

---

## 技术实现建议

### Python + FastAPI 实现示例

```python
from fastapi import FastAPI, File, UploadFile
from pydantic import BaseModel

app = FastAPI()

class FoodRequest(BaseModel):
    foodName: str

@app.post("/api/food/recognize")
async def recognize_menu(image: UploadFile = File(...)):
    # 1. 保存图片到临时文件
    # 2. 调用通义千问Qwen-VL识别菜名
    # 3. 对每个菜名查询营养数据（RAG + LLM）
    # 4. 返回结果
    return {
        "code": 200,
        "message": "识别成功",
        "data": {"dishes": [...]}
    }

@app.post("/api/food/analyze")
async def analyze_food(request: FoodRequest):
    # 1. 查询营养数据库或调用LLM
    # 2. 返回营养成分
    return {
        "code": 200,
        "data": {...}
    }
```

### 数据库表设计参考

见项目计划书 `2.4.2 数据库设计` 章节。

---

## 前端调用示例

```kotlin
// 在ViewModel中调用
viewModelScope.launch {
    try {
        val response = apiService.generateTrip(
            GenerateTripRequest(
                userId = 123,
                query = "规划周末杭州亲子游",
                preferences = null
            )
        )
        
        if (response.code == 200 && response.data != null) {
            // 处理成功
            _tripState.value = TripState.Success(response.data)
        } else {
            // 处理错误
            _tripState.value = TripState.Error(response.message ?: "请求失败")
        }
    } catch (e: Exception) {
        _tripState.value = TripState.Error(e.message ?: "网络异常")
    }
}
```

---

## 测试建议

1. 使用 **Postman** 或 **Apifox** 进行接口测试
2. 创建 **Mock Server** 用于前端开发（可使用Apifox的Mock功能）
3. 编写单元测试覆盖核心业务逻辑
4. 进行压力测试确保并发性能

---

## 下一步开发计划

MVP版本完成后，可逐步添加：
- 餐前餐后对比功能
- 动态Plan B推送
- 离线行程包下载
- 热量联动行程调节
- WebSocket实时通知

---

**最后更新**: 2026-01-23
**文档版本**: v1.0 MVP

