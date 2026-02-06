#!/usr/bin/env python3
"""
下载预转换的 PaddleOCR PP-OCRv4 ONNX 模型
从 HuggingFace Ooredoo-Group/rapidocr-models 仓库直接下载，
无需 paddle2onnx 转换。

用法:
    conda activate software_contest
    python scripts/download_onnx_models.py
"""
import os
import sys
import urllib.request
import ssl

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(PROJECT_DIR, "app", "src", "main", "assets", "models")

# HuggingFace 直链（Ooredoo-Group/rapidocr-models 仓库，预转换的 PP-OCRv4 ONNX）
HF_BASE = "https://huggingface.co/Ooredoo-Group/rapidocr-models/resolve/main"

MODELS = [
    {
        "filename": "ch_PP-OCRv4_det_infer.onnx",
        "url": f"{HF_BASE}/ch_PP-OCRv4_det_infer.onnx",
        "desc": "文本检测模型 (DBNet PP-OCRv4)",
        "expected_size_mb": 4.5,  # ~4.7MB
    },
    {
        "filename": "ch_PP-OCRv4_rec_infer.onnx",
        "url": f"{HF_BASE}/ch_PP-OCRv4_rec_infer.onnx",
        "desc": "文本识别模型 (CRNN PP-OCRv4)",
        "expected_size_mb": 10.3,  # ~10.9MB
    },
    {
        "filename": "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        "url": f"{HF_BASE}/ch_ppocr_mobile_v2.0_cls_infer.onnx",
        "desc": "方向分类模型 (PPOCRv2 Mobile)",
        "expected_size_mb": 0.5,  # ~585KB
    },
]


def download_file(url, dest, desc=""):
    """下载文件，支持 HTTPS"""
    print(f"  下载 {desc} ...")
    print(f"  URL: {url}")
    try:
        # 创建不验证SSL的context（某些环境下HuggingFace证书有问题）
        ctx = ssl.create_default_context()
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, context=ctx) as response:
            with open(dest, "wb") as f:
                while True:
                    chunk = response.read(8192)
                    if not chunk:
                        break
                    f.write(chunk)
        size_mb = os.path.getsize(dest) / (1024 * 1024)
        print(f"  完成 ({size_mb:.1f} MB)")
        return True
    except Exception as e:
        print(f"  下载失败: {e}")
        if os.path.exists(dest):
            os.remove(dest)
        return False


def main():
    print("=" * 55)
    print("PaddleOCR PP-OCRv4 ONNX 模型下载")
    print("来源: HuggingFace Ooredoo-Group/rapidocr-models")
    print("=" * 55)

    os.makedirs(ASSETS_DIR, exist_ok=True)
    print(f"目标目录: {ASSETS_DIR}\n")

    all_ok = True
    for m in MODELS:
        dest = os.path.join(ASSETS_DIR, m["filename"])

        if os.path.exists(dest):
            size_mb = os.path.getsize(dest) / (1024 * 1024)
            # 验证文件大小是否合理（允许20%偏差）
            if abs(size_mb - m["expected_size_mb"]) / m["expected_size_mb"] < 0.5:
                print(f"[{m['desc']}]")
                print(f"  已存在且大小合理: {m['filename']} ({size_mb:.1f} MB)")
                continue
            else:
                print(f"[{m['desc']}]")
                print(f"  文件大小异常 ({size_mb:.1f} MB, 期望 ~{m['expected_size_mb']:.1f} MB)，重新下载...")
                os.remove(dest)

        print(f"[{m['desc']}]")
        if not download_file(m["url"], dest, m["filename"]):
            all_ok = False

    # 验证
    print("\n" + "=" * 55)
    print("验证:")
    total_size = 0
    for m in MODELS:
        path = os.path.join(ASSETS_DIR, m["filename"])
        if os.path.exists(path):
            size_mb = os.path.getsize(path) / (1024 * 1024)
            total_size += size_mb
            print(f"  ✅ {m['filename']} ({size_mb:.1f} MB)")
        else:
            print(f"  ❌ {m['filename']} - 缺失")
            all_ok = False

    dict_path = os.path.join(ASSETS_DIR, "ppocr_keys_v1.txt")
    if os.path.exists(dict_path):
        with open(dict_path, "r", encoding="utf-8") as f:
            lines = sum(1 for _ in f)
        print(f"  ✅ ppocr_keys_v1.txt ({lines} 字符)")
    else:
        print(f"  ❌ ppocr_keys_v1.txt - 缺失")
        all_ok = False

    print(f"\n  模型总大小: {total_size:.1f} MB")
    if all_ok:
        print("  🎉 所有模型文件就绪！")
    else:
        print("  ⚠️ 部分文件缺失。")

    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
