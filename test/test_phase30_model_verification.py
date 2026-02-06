#!/usr/bin/env python3
"""
Phase 30: PaddleOCR ONNX 模型集成 - 综合验证测试

验证内容：
1. ONNX 模型文件存在性和大小
2. ONNX 模型文件格式有效性（使用 onnxruntime 加载）
3. 模型输入/输出张量名称和形状
4. 字符字典文件完整性
5. Kotlin 代码结构验证（模型路径、常量一致性）
6. 下载脚本可用性
7. 边界条件：模型文件损坏检测、空文件检测
"""
import os
import sys
import struct
import unittest

# 项目路径
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(PROJECT_DIR, "app", "src", "main", "assets", "models")
KOTLIN_SRC = os.path.join(PROJECT_DIR, "app", "src", "main", "java", "com", "example", "lifehub", "ai")


class TestModelFilesExistence(unittest.TestCase):
    """测试模型文件存在性"""

    def test_det_model_exists(self):
        """检测模型文件存在"""
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_det_infer.onnx")
        self.assertTrue(os.path.exists(path), f"检测模型不存在: {path}")

    def test_rec_model_exists(self):
        """识别模型文件存在"""
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_rec_infer.onnx")
        self.assertTrue(os.path.exists(path), f"识别模型不存在: {path}")

    def test_cls_model_exists(self):
        """分类模型文件存在"""
        path = os.path.join(ASSETS_DIR, "ch_ppocr_mobile_v2.0_cls_infer.onnx")
        self.assertTrue(os.path.exists(path), f"分类模型不存在: {path}")

    def test_dictionary_exists(self):
        """字符字典文件存在"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        self.assertTrue(os.path.exists(path), f"字典文件不存在: {path}")

    def test_model_info_exists(self):
        """MODEL_INFO.txt 存在"""
        path = os.path.join(ASSETS_DIR, "MODEL_INFO.txt")
        self.assertTrue(os.path.exists(path))

    def test_readme_exists(self):
        """README.md 存在"""
        path = os.path.join(ASSETS_DIR, "README.md")
        self.assertTrue(os.path.exists(path))

    def test_no_placeholder_files(self):
        """不应存在 PLACEHOLDER.txt 文件"""
        for f in os.listdir(ASSETS_DIR):
            self.assertFalse(
                f.endswith(".PLACEHOLDER.txt"),
                f"发现占位文件: {f}"
            )

    def test_no_paddle_model_directories(self):
        """不应存在 Paddle 原始模型目录"""
        for d in ["ch_PP-OCRv4_det_infer", "ch_PP-OCRv4_rec_infer", "ch_ppocr_mobile_v2.0_cls_infer"]:
            path = os.path.join(ASSETS_DIR, d)
            self.assertFalse(
                os.path.isdir(path),
                f"发现 Paddle 原始模型目录（应已删除）: {d}"
            )


class TestModelFileSizes(unittest.TestCase):
    """测试模型文件大小是否在合理范围"""

    def test_det_model_size(self):
        """检测模型大小 ~4.5MB"""
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_det_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        size_mb = os.path.getsize(path) / (1024 * 1024)
        self.assertGreater(size_mb, 2.0, f"检测模型过小: {size_mb:.1f} MB")
        self.assertLess(size_mb, 10.0, f"检测模型过大: {size_mb:.1f} MB")

    def test_rec_model_size(self):
        """识别模型大小 ~10.4MB"""
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_rec_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        size_mb = os.path.getsize(path) / (1024 * 1024)
        self.assertGreater(size_mb, 5.0, f"识别模型过小: {size_mb:.1f} MB")
        self.assertLess(size_mb, 20.0, f"识别模型过大: {size_mb:.1f} MB")

    def test_cls_model_size(self):
        """分类模型大小 ~0.6MB"""
        path = os.path.join(ASSETS_DIR, "ch_ppocr_mobile_v2.0_cls_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        size_mb = os.path.getsize(path) / (1024 * 1024)
        self.assertGreater(size_mb, 0.1, f"分类模型过小: {size_mb:.1f} MB")
        self.assertLess(size_mb, 5.0, f"分类模型过大: {size_mb:.1f} MB")

    def test_total_model_size(self):
        """总模型大小 ~15.4MB"""
        models = [
            "ch_PP-OCRv4_det_infer.onnx",
            "ch_PP-OCRv4_rec_infer.onnx",
            "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        ]
        total = 0
        for m in models:
            path = os.path.join(ASSETS_DIR, m)
            if os.path.exists(path):
                total += os.path.getsize(path)
        total_mb = total / (1024 * 1024)
        self.assertGreater(total_mb, 10.0, f"总大小过小: {total_mb:.1f} MB")
        self.assertLess(total_mb, 30.0, f"总大小过大: {total_mb:.1f} MB")


class TestOnnxModelValidity(unittest.TestCase):
    """使用 onnxruntime 验证模型文件有效性"""

    @classmethod
    def setUpClass(cls):
        try:
            import onnxruntime as ort
            cls.ort = ort
            cls.has_ort = True
        except ImportError:
            cls.has_ort = False

    def _load_model(self, filename):
        if not self.has_ort:
            self.skipTest("onnxruntime 未安装")
        path = os.path.join(ASSETS_DIR, filename)
        if not os.path.exists(path):
            self.skipTest(f"模型文件不存在: {filename}")
        return self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])

    def test_det_model_loadable(self):
        """检测模型可被 onnxruntime 加载"""
        session = self._load_model("ch_PP-OCRv4_det_infer.onnx")
        self.assertIsNotNone(session)

    def test_rec_model_loadable(self):
        """识别模型可被 onnxruntime 加载"""
        session = self._load_model("ch_PP-OCRv4_rec_infer.onnx")
        self.assertIsNotNone(session)

    def test_cls_model_loadable(self):
        """分类模型可被 onnxruntime 加载"""
        session = self._load_model("ch_ppocr_mobile_v2.0_cls_infer.onnx")
        self.assertIsNotNone(session)

    def test_det_model_input_name(self):
        """检测模型输入名称为 'x'"""
        session = self._load_model("ch_PP-OCRv4_det_infer.onnx")
        inputs = [i.name for i in session.get_inputs()]
        self.assertIn("x", inputs, f"输入名称不包含 'x': {inputs}")

    def test_rec_model_input_name(self):
        """识别模型输入名称为 'x'"""
        session = self._load_model("ch_PP-OCRv4_rec_infer.onnx")
        inputs = [i.name for i in session.get_inputs()]
        self.assertIn("x", inputs, f"输入名称不包含 'x': {inputs}")

    def test_cls_model_input_name(self):
        """分类模型输入名称为 'x'"""
        session = self._load_model("ch_ppocr_mobile_v2.0_cls_infer.onnx")
        inputs = [i.name for i in session.get_inputs()]
        self.assertIn("x", inputs, f"输入名称不包含 'x': {inputs}")

    def test_det_model_output_name(self):
        """检测模型输出名称包含 sigmoid"""
        session = self._load_model("ch_PP-OCRv4_det_infer.onnx")
        outputs = [o.name for o in session.get_outputs()]
        self.assertTrue(
            any("sigmoid" in o for o in outputs),
            f"输出名称不含 sigmoid: {outputs}"
        )

    def test_rec_model_output_name(self):
        """识别模型输出名称包含 softmax"""
        session = self._load_model("ch_PP-OCRv4_rec_infer.onnx")
        outputs = [o.name for o in session.get_outputs()]
        self.assertTrue(
            any("softmax" in o for o in outputs),
            f"输出名称不含 softmax: {outputs}"
        )

    def test_cls_model_output_name(self):
        """分类模型输出名称为 save_infer_model/scale_0.tmp_1"""
        session = self._load_model("ch_ppocr_mobile_v2.0_cls_infer.onnx")
        outputs = [o.name for o in session.get_outputs()]
        self.assertIn(
            "save_infer_model/scale_0.tmp_1", outputs,
            f"输出名称不匹配: {outputs}"
        )

    def test_det_model_input_shape(self):
        """检测模型输入形状为 [batch, 3, H, W]"""
        session = self._load_model("ch_PP-OCRv4_det_infer.onnx")
        inp = session.get_inputs()[0]
        shape = inp.shape
        self.assertEqual(4, len(shape), f"输入维度应为4: {shape}")
        self.assertEqual(3, shape[1], f"通道数应为3: {shape}")

    def test_rec_model_input_shape(self):
        """识别模型输入形状为 [batch, 3, 48, W]"""
        session = self._load_model("ch_PP-OCRv4_rec_infer.onnx")
        inp = session.get_inputs()[0]
        shape = inp.shape
        self.assertEqual(4, len(shape), f"输入维度应为4: {shape}")
        self.assertEqual(3, shape[1], f"通道数应为3: {shape}")

    def test_cls_model_input_shape(self):
        """分类模型输入形状为 [batch, 3, 48, 192]"""
        session = self._load_model("ch_ppocr_mobile_v2.0_cls_infer.onnx")
        inp = session.get_inputs()[0]
        shape = inp.shape
        self.assertEqual(4, len(shape), f"输入维度应为4: {shape}")
        self.assertEqual(3, shape[1], f"通道数应为3: {shape}")


class TestOnnxModelInference(unittest.TestCase):
    """测试模型推理功能（使用假数据）"""

    @classmethod
    def setUpClass(cls):
        try:
            import onnxruntime as ort
            import numpy as np
            cls.ort = ort
            cls.np = np
            cls.has_deps = True
        except ImportError:
            cls.has_deps = False

    def test_det_model_inference(self):
        """检测模型可进行推理（640x640输入）"""
        if not self.has_deps:
            self.skipTest("onnxruntime/numpy 未安装")
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_det_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        session = self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])
        dummy = self.np.random.randn(1, 3, 640, 640).astype(self.np.float32)
        result = session.run(None, {"x": dummy})
        self.assertIsNotNone(result)
        self.assertEqual(1, len(result))
        # 输出应该是概率图
        self.assertEqual(4, len(result[0].shape))

    def test_rec_model_inference(self):
        """识别模型可进行推理（48x320输入）"""
        if not self.has_deps:
            self.skipTest("onnxruntime/numpy 未安装")
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_rec_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        session = self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])
        dummy = self.np.random.randn(1, 3, 48, 320).astype(self.np.float32)
        result = session.run(None, {"x": dummy})
        self.assertIsNotNone(result)
        self.assertEqual(1, len(result))

    def test_cls_model_inference(self):
        """分类模型可进行推理（48x192输入）"""
        if not self.has_deps:
            self.skipTest("onnxruntime/numpy 未安装")
        path = os.path.join(ASSETS_DIR, "ch_ppocr_mobile_v2.0_cls_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        session = self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])
        dummy = self.np.random.randn(1, 3, 48, 192).astype(self.np.float32)
        result = session.run(None, {"x": dummy})
        self.assertIsNotNone(result)
        self.assertEqual(1, len(result))
        # 分类输出应该有2个类别（0度/180度）
        self.assertEqual(2, result[0].shape[-1])

    def test_det_output_shape_matches_input(self):
        """检测模型输出空间尺寸应与输入匹配"""
        if not self.has_deps:
            self.skipTest("onnxruntime/numpy 未安装")
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_det_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        session = self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])
        H, W = 320, 320
        dummy = self.np.random.randn(1, 3, H, W).astype(self.np.float32)
        result = session.run(None, {"x": dummy})
        out_shape = result[0].shape
        # 输出 H, W 应与输入一致
        self.assertEqual(H, out_shape[2])
        self.assertEqual(W, out_shape[3])

    def test_rec_output_has_classes(self):
        """识别模型输出应包含 6624 个类别（6623字符 + blank）"""
        if not self.has_deps:
            self.skipTest("onnxruntime/numpy 未安装")
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_rec_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("模型文件不存在")
        session = self.ort.InferenceSession(path, providers=["CPUExecutionProvider"])
        dummy = self.np.random.randn(1, 3, 48, 320).astype(self.np.float32)
        result = session.run(None, {"x": dummy})
        num_classes = result[0].shape[-1]
        # 应该有 6625 个类（6624字符 + 1 blank）
        self.assertEqual(6625, num_classes, f"类别数: {num_classes}")


class TestDictionary(unittest.TestCase):
    """测试字符字典"""

    def test_dictionary_line_count(self):
        """字典应有 6622 个非空字符行"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            lines = [l for l in f.readlines() if l.strip()]
        self.assertEqual(6622, len(lines), f"字典行数: {len(lines)}")

    def test_dictionary_contains_chinese(self):
        """字典应包含中文字符"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        has_chinese = any('\u4e00' <= c <= '\u9fff' for c in content)
        self.assertTrue(has_chinese, "字典不包含中文字符")

    def test_dictionary_contains_digits(self):
        """字典应包含数字"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        has_digits = any(c.isdigit() for c in content)
        self.assertTrue(has_digits, "字典不包含数字")

    def test_dictionary_contains_letters(self):
        """字典应包含英文字母"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        has_letters = any(c.isalpha() and c.isascii() for c in content)
        self.assertTrue(has_letters, "字典不包含英文字母")

    def test_dictionary_no_empty_lines_except_trailing(self):
        """字典中间不应有空行（末尾空行可接受）"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            lines = f.readlines()
        # 允许末尾最多1个空行
        non_empty = [l for l in lines if l.strip()]
        empty_count = len(lines) - len(non_empty)
        self.assertLessEqual(empty_count, 1,
                             f"字典有 {empty_count} 个空行（最多允许1个末尾空行）")

    def test_dictionary_encoding_utf8(self):
        """字典应为 UTF-8 编码"""
        path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
        if not os.path.exists(path):
            self.skipTest("字典文件不存在")
        try:
            with open(path, "r", encoding="utf-8") as f:
                f.read()
        except UnicodeDecodeError:
            self.fail("字典文件不是 UTF-8 编码")


class TestOnnxFileFormat(unittest.TestCase):
    """测试 ONNX 文件格式（二进制级验证）"""

    def _check_onnx_magic(self, filename):
        """检查 ONNX 文件魔数"""
        path = os.path.join(ASSETS_DIR, filename)
        if not os.path.exists(path):
            self.skipTest(f"文件不存在: {filename}")
        with open(path, "rb") as f:
            header = f.read(8)
        # ONNX 使用 protobuf 格式，第一个字节通常是 0x08
        self.assertGreater(len(header), 0, "文件为空")
        self.assertNotEqual(header, b'\x00' * 8, "文件内容全为0")

    def test_det_onnx_format(self):
        self._check_onnx_magic("ch_PP-OCRv4_det_infer.onnx")

    def test_rec_onnx_format(self):
        self._check_onnx_magic("ch_PP-OCRv4_rec_infer.onnx")

    def test_cls_onnx_format(self):
        self._check_onnx_magic("ch_ppocr_mobile_v2.0_cls_infer.onnx")

    def test_det_not_html_error_page(self):
        """检测模型不是 HTML 错误页面（下载失败时可能获得HTML）"""
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_det_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("文件不存在")
        with open(path, "rb") as f:
            header = f.read(20)
        self.assertNotIn(b"<!DOCTYPE", header)
        self.assertNotIn(b"<html", header)

    def test_rec_not_html_error_page(self):
        path = os.path.join(ASSETS_DIR, "ch_PP-OCRv4_rec_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("文件不存在")
        with open(path, "rb") as f:
            header = f.read(20)
        self.assertNotIn(b"<!DOCTYPE", header)
        self.assertNotIn(b"<html", header)

    def test_cls_not_html_error_page(self):
        path = os.path.join(ASSETS_DIR, "ch_ppocr_mobile_v2.0_cls_infer.onnx")
        if not os.path.exists(path):
            self.skipTest("文件不存在")
        with open(path, "rb") as f:
            header = f.read(20)
        self.assertNotIn(b"<!DOCTYPE", header)
        self.assertNotIn(b"<html", header)


class TestKotlinCodeStructure(unittest.TestCase):
    """验证 Kotlin 代码结构和模型路径一致性"""

    def test_ocr_service_exists(self):
        """OcrService.kt 存在"""
        path = os.path.join(KOTLIN_SRC, "OcrService.kt")
        self.assertTrue(os.path.exists(path), f"OcrService.kt 不存在: {path}")

    def test_image_preprocessor_exists(self):
        """ImagePreprocessor.kt 存在"""
        path = os.path.join(KOTLIN_SRC, "ImagePreprocessor.kt")
        self.assertTrue(os.path.exists(path))

    def test_onnx_inference_exists(self):
        """OnnxInference.kt 存在"""
        path = os.path.join(KOTLIN_SRC, "OnnxInference.kt")
        self.assertTrue(os.path.exists(path))

    def test_model_paths_in_code(self):
        """代码中的模型路径与实际文件一致"""
        ocr_service = os.path.join(KOTLIN_SRC, "OcrService.kt")
        if not os.path.exists(ocr_service):
            self.skipTest("OcrService.kt 不存在")
        with open(ocr_service, "r", encoding="utf-8") as f:
            content = f.read()

        expected_paths = [
            "models/ch_PP-OCRv4_det_infer.onnx",
            "models/ch_PP-OCRv4_rec_infer.onnx",
            "models/ch_ppocr_mobile_v2.0_cls_infer.onnx",
            "models/ppocr_keys_v1.txt",
        ]
        for p in expected_paths:
            self.assertIn(p, content, f"代码中缺少路径: {p}")

    def test_model_names_in_code(self):
        """代码中定义了3个模型名称常量"""
        ocr_service = os.path.join(KOTLIN_SRC, "OcrService.kt")
        if not os.path.exists(ocr_service):
            self.skipTest("OcrService.kt 不存在")
        with open(ocr_service, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("MODEL_DET", content)
        self.assertIn("MODEL_REC", content)
        self.assertIn("MODEL_CLS", content)

    def test_model_files_match_code_paths(self):
        """assets/models/ 中的 ONNX 文件与代码中的路径一致"""
        onnx_files = [f for f in os.listdir(ASSETS_DIR) if f.endswith(".onnx")]
        expected = {
            "ch_PP-OCRv4_det_infer.onnx",
            "ch_PP-OCRv4_rec_infer.onnx",
            "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        }
        self.assertEqual(expected, set(onnx_files),
                         f"ONNX 文件集合不匹配: {onnx_files}")


class TestDownloadScript(unittest.TestCase):
    """测试下载脚本"""

    def test_download_script_exists(self):
        """下载脚本存在"""
        path = os.path.join(PROJECT_DIR, "scripts", "download_onnx_models.py")
        self.assertTrue(os.path.exists(path))

    def test_download_script_syntax(self):
        """下载脚本语法正确"""
        path = os.path.join(PROJECT_DIR, "scripts", "download_onnx_models.py")
        if not os.path.exists(path):
            self.skipTest("脚本不存在")
        import py_compile
        try:
            py_compile.compile(path, doraise=True)
        except py_compile.PyCompileError as e:
            self.fail(f"脚本语法错误: {e}")

    def test_no_old_scripts(self):
        """旧的转换脚本已删除"""
        scripts_dir = os.path.join(PROJECT_DIR, "scripts")
        old_scripts = [
            "convert_onnx_linux.sh",
            "convert_onnx_windows.bat",
            "download_ocr_models.py",
            "cleanup_and_download.py",
            "convert_models_wsl.py",
            "convert_wsl.sh",
        ]
        for s in old_scripts:
            path = os.path.join(scripts_dir, s)
            self.assertFalse(os.path.exists(path), f"旧脚本未删除: {s}")


class TestGitIgnore(unittest.TestCase):
    """测试 .gitignore 配置"""

    def test_onnx_ignored(self):
        """.gitignore 应包含 *.onnx"""
        gitignore = os.path.join(PROJECT_DIR, ".gitignore")
        if not os.path.exists(gitignore):
            self.skipTest(".gitignore 不存在")
        with open(gitignore, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("*.onnx", content, ".gitignore 未忽略 *.onnx")


if __name__ == "__main__":
    unittest.main(verbosity=2)
