# PaddleOCR ONNX 模型文件

本目录存放 PaddleOCR 端侧 OCR 所需的 ONNX 模型文件和字符字典。

## 文件清单

| 文件名 | 说明 | 大小 |
|--------|------|------|
| `ch_PP-OCRv4_det_infer.onnx` | 文本检测模型 (DBNet) | 4.5 MB |
| `ch_PP-OCRv4_rec_infer.onnx` | 文本识别模型 (CRNN) | 10.4 MB |
| `ch_ppocr_mobile_v2.0_cls_infer.onnx` | 方向分类模型 | 0.6 MB |
| `ppocr_keys_v1.txt` | 中文字符字典 (6623字符) | ~60 KB |

## 获取方式

运行项目根目录下的模型下载脚本：

```bash
conda activate software_contest
cd Frontend/lifehub-frontend
python scripts/download_onnx_models.py
```

脚本从 HuggingFace (Ooredoo-Group/rapidocr-models) 直接下载预转换的 ONNX 模型，
无需安装 PaddlePaddle 或 paddle2onnx。

## 模型来源

- **原始模型**: PaddleOCR PP-OCRv4 (百度飞桨开源 OCR)
- **ONNX 转换**: RapidAI 社区预转换，托管于 HuggingFace

## 注意事项

- 模型文件较大（共约 15.4 MB），不应提交到 Git 仓库
- `.gitignore` 已配置忽略 `*.onnx` 文件
- 首次运行 APP 前必须确保模型文件已放置到此目录
