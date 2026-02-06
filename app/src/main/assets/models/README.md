# PaddleOCR ONNX 模型文件

本目录存放 PaddleOCR 端侧 OCR 所需的 ONNX 模型文件和字符字典。

## 所需文件

| 文件名 | 说明 | 大小(约) |
|--------|------|----------|
| `ch_PP-OCRv4_det_infer.onnx` | 文本检测模型 (DBNet) | ~4.7MB |
| `ch_PP-OCRv4_rec_infer.onnx` | 文本识别模型 (CRNN) | ~10MB |
| `ch_ppocr_mobile_v2.0_cls_infer.onnx` | 方向分类模型 | ~1.4MB |
| `ppocr_keys_v1.txt` | 中文字符字典 (6623字符) | ~60KB |

## 获取方式

运行项目根目录下的模型下载脚本：

```bash
# 在 software_contest conda 环境中执行
conda activate software_contest
cd Frontend/lifehub-frontend
python scripts/download_ocr_models.py
```

脚本会自动：
1. 下载 PaddlePaddle 推理模型
2. 使用 paddle2onnx 转换为 ONNX 格式
3. 下载字符字典
4. 将文件放置到本目录

## 手动获取

如果自动脚本失败，可手动操作：

1. 安装依赖：`pip install paddlepaddle paddle2onnx`
2. 从 PaddleOCR 下载推理模型并用 paddle2onnx 转换
3. 字典文件：https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.7/ppocr/utils/ppocr_keys_v1.txt

## 注意事项

- 模型文件较大，不应提交到 Git 仓库
- `.gitignore` 已配置忽略 `*.onnx` 文件
- 首次运行 APP 前必须确保模型文件已放置到此目录
