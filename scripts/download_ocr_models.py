#!/usr/bin/env python3
"""
PaddleOCR ONNX 模型下载与转换脚本

功能：
1. 下载 PaddleOCR PP-OCRv4 推理模型（det + rec + cls）
2. 使用 paddle2onnx 转换为 ONNX 格式
3. 下载中文字符字典 ppocr_keys_v1.txt
4. 将文件放置到 app/src/main/assets/models/ 目录

使用方法：
    conda activate software_contest
    python scripts/download_ocr_models.py

依赖：
    pip install paddlepaddle paddle2onnx requests
"""

import os
import sys
import tarfile
import shutil
import subprocess
import tempfile
import urllib.request

# 模型下载URL（PaddleOCR官方）
MODELS = {
    "det": {
        "url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.tar",
        "dir_name": "ch_PP-OCRv4_det_infer",
        "output_name": "ch_PP-OCRv4_det_infer.onnx",
    },
    "rec": {
        "url": "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.tar",
        "dir_name": "ch_PP-OCRv4_rec_infer",
        "output_name": "ch_PP-OCRv4_rec_infer.onnx",
    },
    "cls": {
        "url": "https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar",
        "dir_name": "ch_ppocr_mobile_v2.0_cls_infer",
        "output_name": "ch_ppocr_mobile_v2.0_cls_infer.onnx",
    },
}

DICT_URL = "https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.7/ppocr/utils/ppocr_keys_v1.txt"
DICT_NAME = "ppocr_keys_v1.txt"

# 输出目录
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(PROJECT_DIR, "app", "src", "main", "assets", "models")


def download_file(url: str, dest: str, desc: str = ""):
    """下载文件，显示进度"""
    print(f"  下载 {desc or url} ...")
    try:
        urllib.request.urlretrieve(url, dest)
        size_mb = os.path.getsize(dest) / (1024 * 1024)
        print(f"  完成 ({size_mb:.1f} MB)")
    except Exception as e:
        print(f"  下载失败: {e}")
        raise


def extract_tar(tar_path: str, extract_dir: str):
    """解压tar文件"""
    print(f"  解压 {os.path.basename(tar_path)} ...")
    with tarfile.open(tar_path, "r") as tar:
        tar.extractall(path=extract_dir)
    print("  解压完成")


def convert_to_onnx(model_dir: str, output_path: str):
    """使用paddle2onnx将PaddlePaddle模型转换为ONNX"""
    pdmodel = os.path.join(model_dir, "inference.pdmodel")
    pdiparams = os.path.join(model_dir, "inference.pdiparams")

    if not os.path.exists(pdmodel) or not os.path.exists(pdiparams):
        raise FileNotFoundError(f"模型文件不存在: {model_dir}")

    print(f"  转换为ONNX: {os.path.basename(output_path)} ...")
    cmd = [
        sys.executable, "-m", "paddle2onnx",
        "--model_dir", model_dir,
        "--model_filename", "inference.pdmodel",
        "--params_filename", "inference.pdiparams",
        "--save_file", output_path,
        "--opset_version", "11",
        "--enable_onnx_checker", "True",
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  转换失败: {result.stderr}")
        raise RuntimeError(f"paddle2onnx 转换失败: {result.stderr}")

    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"  转换完成 ({size_mb:.1f} MB)")


def check_dependencies():
    """检查必要依赖"""
    missing = []
    try:
        import paddle  # noqa: F401
    except ImportError:
        missing.append("paddlepaddle")

    try:
        import paddle2onnx  # noqa: F401
    except ImportError:
        missing.append("paddle2onnx")

    if missing:
        print(f"缺少依赖: {', '.join(missing)}")
        print(f"请运行: pip install {' '.join(missing)}")
        return False
    return True


def main():
    print("=" * 60)
    print("PaddleOCR ONNX 模型下载与转换")
    print("=" * 60)

    # 检查依赖
    if not check_dependencies():
        sys.exit(1)

    # 创建输出目录
    os.makedirs(ASSETS_DIR, exist_ok=True)
    print(f"输出目录: {ASSETS_DIR}\n")

    # 使用临时目录进行下载和转换
    with tempfile.TemporaryDirectory() as tmp_dir:
        # 下载并转换每个模型
        for model_type, info in MODELS.items():
            print(f"\n[{model_type.upper()}] {info['dir_name']}")
            output_path = os.path.join(ASSETS_DIR, info["output_name"])

            if os.path.exists(output_path):
                print(f"  已存在，跳过: {info['output_name']}")
                continue

            # 下载
            tar_path = os.path.join(tmp_dir, f"{model_type}.tar")
            download_file(info["url"], tar_path, info["dir_name"])

            # 解压
            extract_tar(tar_path, tmp_dir)

            # 转换
            model_dir = os.path.join(tmp_dir, info["dir_name"])
            convert_to_onnx(model_dir, output_path)

            # 清理tar文件节省空间
            os.remove(tar_path)

        # 下载字典
        print(f"\n[DICT] {DICT_NAME}")
        dict_path = os.path.join(ASSETS_DIR, DICT_NAME)
        if os.path.exists(dict_path):
            print(f"  已存在，跳过: {DICT_NAME}")
        else:
            download_file(DICT_URL, dict_path, DICT_NAME)

    # 验证
    print("\n" + "=" * 60)
    print("验证模型文件:")
    all_ok = True
    for info in MODELS.values():
        path = os.path.join(ASSETS_DIR, info["output_name"])
        exists = os.path.exists(path)
        size = os.path.getsize(path) / (1024 * 1024) if exists else 0
        status = f"OK ({size:.1f} MB)" if exists else "缺失"
        print(f"  {info['output_name']}: {status}")
        if not exists:
            all_ok = False

    dict_path = os.path.join(ASSETS_DIR, DICT_NAME)
    dict_exists = os.path.exists(dict_path)
    if dict_exists:
        with open(dict_path, "r", encoding="utf-8") as f:
            line_count = sum(1 for _ in f)
        print(f"  {DICT_NAME}: OK ({line_count} 字符)")
    else:
        print(f"  {DICT_NAME}: 缺失")
        all_ok = False

    if all_ok:
        print("\n所有模型文件准备就绪！")
    else:
        print("\n部分文件缺失，请检查网络连接后重试。")
        sys.exit(1)


if __name__ == "__main__":
    main()
