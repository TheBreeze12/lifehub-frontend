"""
Phase 29: ONNX Runtime集成 - 代码结构验证测试

验证内容：
1. 文件存在性检查
2. 依赖声明检查
3. 类/接口结构验证
4. 测试文件完整性
5. 代码质量检查
"""

import os
import re
import sys

# 项目路径
FRONTEND_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP_SRC = os.path.join(FRONTEND_ROOT, "app", "src")
MAIN_JAVA = os.path.join(APP_SRC, "main", "java", "com", "example", "lifehub")
TEST_JAVA = os.path.join(APP_SRC, "test", "java", "com", "example", "lifehub")

passed = 0
failed = 0
total = 0


def check(description, condition):
    global passed, failed, total
    total += 1
    if condition:
        passed += 1
        print(f"  ✅ {description}")
    else:
        failed += 1
        print(f"  ❌ {description}")


def read_file(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


print("=" * 60)
print("Phase 29: ONNX Runtime集成 - 代码结构验证")
print("=" * 60)

# ==================== 1. 文件存在性检查 ====================
print("\n--- 1. 文件存在性检查 ---")

onnx_inference_path = os.path.join(MAIN_JAVA, "ai", "OnnxInference.kt")
check("OnnxInference.kt 文件存在", os.path.isfile(onnx_inference_path))

build_gradle_path = os.path.join(FRONTEND_ROOT, "app", "build.gradle.kts")
check("build.gradle.kts 文件存在", os.path.isfile(build_gradle_path))

test_file_path = os.path.join(TEST_JAVA, "Phase29OnnxInferenceTest.kt")
check("Phase29OnnxInferenceTest.kt 测试文件存在", os.path.isfile(test_file_path))

ai_dir = os.path.join(MAIN_JAVA, "ai")
check("ai/ 目录已创建", os.path.isdir(ai_dir))

# ==================== 2. 依赖声明检查 ====================
print("\n--- 2. 依赖声明检查 ---")

build_gradle = read_file(build_gradle_path)
check(
    "ONNX Runtime Android 依赖已声明",
    "com.microsoft.onnxruntime:onnxruntime-android" in build_gradle,
)
check(
    "ONNX Runtime 版本号已指定",
    re.search(r"onnxruntime-android:\d+\.\d+\.\d+", build_gradle) is not None,
)
# 确保版本号合理（1.x.x）
version_match = re.search(r"onnxruntime-android:(\d+\.\d+\.\d+)", build_gradle)
if version_match:
    version = version_match.group(1)
    major = int(version.split(".")[0])
    check(f"ONNX Runtime 版本合理 (v{version})", major >= 1)
else:
    check("ONNX Runtime 版本号解析", False)

# ==================== 3. 类/接口结构验证 ====================
print("\n--- 3. 类/接口结构验证 ---")

onnx_code = read_file(onnx_inference_path)

check("OnnxInference 类定义存在", "class OnnxInference" in onnx_code)
check("OnnxModelConfig 数据类定义存在", "data class OnnxModelConfig" in onnx_code)
check("ModelLoadState 密封类定义存在", "sealed class ModelLoadState" in onnx_code)
check("InferenceResult 密封类定义存在", "sealed class InferenceResult" in onnx_code)

# companion object 方法
check("normalizeThreadCount 方法存在", "fun normalizeThreadCount" in onnx_code)
check("isValidModelPath 方法存在", "fun isValidModelPath" in onnx_code)
check("normalizePixelValue 方法存在", "fun normalizePixelValue" in onnx_code)
check(
    "normalizePixelValueWithMeanStd 方法存在",
    "fun normalizePixelValueWithMeanStd" in onnx_code,
)
check(
    "calculateResizedDimensions 方法存在",
    "fun calculateResizedDimensions" in onnx_code,
)
check("argmax 方法存在", "fun argmax" in onnx_code)
check("topKIndices 方法存在", "fun topKIndices" in onnx_code)

# 实例方法
check("loadModel 方法存在", "suspend fun loadModel" in onnx_code)
check("loadModelFromFile 方法存在", "suspend fun loadModelFromFile" in onnx_code)
check("runInference 方法存在", "suspend fun runInference" in onnx_code)
check(
    "runInferenceMultiInput 方法存在",
    "suspend fun runInferenceMultiInput" in onnx_code,
)
check("close 方法存在", "fun close()" in onnx_code)
check("unloadModel 方法存在", "fun unloadModel" in onnx_code)
check("isModelLoaded 方法存在", "fun isModelLoaded" in onnx_code)
check("getModelInputInfo 方法存在", "fun getModelInputInfo" in onnx_code)
check("getModelOutputInfo 方法存在", "fun getModelOutputInfo" in onnx_code)
check("initEnvironment 方法存在", "fun initEnvironment" in onnx_code)

# 导入检查
check("导入 OnnxTensor", "import ai.onnxruntime.OnnxTensor" in onnx_code)
check("导入 OrtEnvironment", "import ai.onnxruntime.OrtEnvironment" in onnx_code)
check("导入 OrtSession", "import ai.onnxruntime.OrtSession" in onnx_code)
check("导入 Dispatchers", "import kotlinx.coroutines.Dispatchers" in onnx_code)

# ModelLoadState 子类
check(
    "ModelLoadState.NotLoaded 存在",
    "object NotLoaded" in onnx_code or "NotLoaded" in onnx_code,
)
check(
    "ModelLoadState.Loading 存在",
    "object Loading" in onnx_code or "Loading" in onnx_code,
)
check(
    "ModelLoadState.Loaded 存在",
    "data class Loaded" in onnx_code or "Loaded(" in onnx_code,
)
check("ModelLoadState.Error 存在", "data class Error" in onnx_code)

# InferenceResult 子类
check(
    "InferenceResult.Success 存在",
    "data class Success" in onnx_code and "inferenceTimeMs" in onnx_code,
)
check(
    "InferenceResult.Error 存在",
    "InferenceResult.Error" in onnx_code or 'message: String' in onnx_code,
)

# OnnxModelConfig 字段
check("modelName 字段存在", "val modelName: String" in onnx_code)
check("modelPath 字段存在", "val modelPath: String" in onnx_code)
check("inputNames 字段存在", "val inputNames: List<String>" in onnx_code)
check("outputNames 字段存在", "val outputNames: List<String>" in onnx_code)
check("numThreads 字段存在", "val numThreads: Int" in onnx_code)
check("useGpu 字段存在", "val useGpu: Boolean" in onnx_code)
check("inputShape 字段存在", "val inputShape: LongArray?" in onnx_code)

# ==================== 4. 测试文件完整性 ====================
print("\n--- 4. 测试文件完整性 ---")

test_code = read_file(test_file_path)

# 统计测试方法数量
test_methods = re.findall(r"@Test\s+fun\s+`[^`]+`", test_code)
check(f"测试方法数量充足 (找到 {len(test_methods)} 个)", len(test_methods) >= 30)

# 检查测试覆盖的关键区域
check("测试 OnnxModelConfig 创建", "OnnxModelConfig" in test_code)
check("测试 InferenceResult.Success", "InferenceResult.Success" in test_code)
check("测试 InferenceResult.Error", "InferenceResult.Error" in test_code)
check("测试 ModelLoadState", "ModelLoadState" in test_code)
check("测试 normalizeThreadCount", "normalizeThreadCount" in test_code)
check("测试 isValidModelPath", "isValidModelPath" in test_code)
check("测试 normalizePixelValue", "normalizePixelValue" in test_code)
check("测试 calculateResizedDimensions", "calculateResizedDimensions" in test_code)
check("测试 argmax", "argmax" in test_code)
check("测试 topKIndices", "topKIndices" in test_code)
check("测试边界条件", "边界条件" in test_code or "empty" in test_code.lower())

# ==================== 5. 代码质量检查 ====================
print("\n--- 5. 代码质量检查 ---")

check("包声明正确", "package com.example.lifehub.ai" in onnx_code)
check("类有文档注释", "/**" in onnx_code and "*/" in onnx_code)
check("使用 withContext(Dispatchers.IO)", "withContext(Dispatchers.IO)" in onnx_code)
check("资源释放 - close方法", "sessions.clear()" in onnx_code)
check("线程数限制 - MAX_THREADS", "MAX_THREADS" in onnx_code)
check(
    "错误处理 - try/catch", onnx_code.count("try {") >= 2
)
check("常量定义 - DEFAULT_NUM_THREADS", "DEFAULT_NUM_THREADS" in onnx_code)
check(
    "支持的扩展名 - SUPPORTED_EXTENSIONS", "SUPPORTED_EXTENSIONS" in onnx_code
)
check("中文注释存在", any(c >= "\u4e00" and c <= "\u9fff" for c in onnx_code))
check("没有硬编码API Key", "api_key" not in onnx_code.lower() and "apikey" not in onnx_code.lower())

# ==================== 结果汇总 ====================
print("\n" + "=" * 60)
print(f"验证结果: {passed}/{total} 通过, {failed} 失败")
print("=" * 60)

if failed > 0:
    print(f"\n⚠️ 有 {failed} 项验证未通过，请检查上述❌项")
    sys.exit(1)
else:
    print("\n🎉 所有验证项全部通过！Phase 29 代码结构完整。")
    sys.exit(0)
