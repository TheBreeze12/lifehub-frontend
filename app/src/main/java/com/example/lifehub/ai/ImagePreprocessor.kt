package com.example.lifehub.ai

import android.graphics.Bitmap

/**
 * PaddleOCR 图像预处理器
 *
 * 支持检测(det)、分类(cls)、识别(rec)三种模型的图像预处理：
 * - 检测：缩放到最长边960，pad到32的倍数，ImageNet归一化
 * - 分类：缩放到48×192，ImageNet归一化
 * - 识别：高度48，宽度按比例缩放（最大320），ImageNet归一化
 *
 * 归一化参数使用ImageNet标准值：
 * - mean = [0.485, 0.456, 0.406] (RGB)
 * - std  = [0.229, 0.224, 0.225] (RGB)
 */
object ImagePreprocessor {

    // ===== PaddleOCR 标准归一化参数 (RGB通道) =====
    const val MEAN_R = 0.485f
    const val MEAN_G = 0.456f
    const val MEAN_B = 0.406f
    const val STD_R = 0.229f
    const val STD_G = 0.224f
    const val STD_B = 0.225f

    // ===== 模型输入尺寸常量 =====
    /** 检测模型最长边上限 */
    const val DET_MAX_SIDE_LEN = 960
    /** 识别模型输入高度 */
    const val REC_IMG_HEIGHT = 48
    /** 识别模型最大输入宽度 */
    const val REC_MAX_WIDTH = 320
    /** 分类模型输入高度 */
    const val CLS_IMG_HEIGHT = 48
    /** 分类模型输入宽度 */
    const val CLS_IMG_WIDTH = 192

    /**
     * 计算检测模型的输入尺寸
     * 规则：最长边不超过 maxSideLen，结果 pad 到 32 的倍数
     *
     * @param width 原始宽度
     * @param height 原始高度
     * @param maxSideLen 最长边上限，默认960
     * @return Pair(目标宽度, 目标高度)
     */
    fun calculateDetSize(
        width: Int,
        height: Int,
        maxSideLen: Int = DET_MAX_SIDE_LEN
    ): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return Pair(32, 32)

        val ratio = if (maxOf(width, height) > maxSideLen) {
            maxSideLen.toFloat() / maxOf(width, height)
        } else {
            1.0f
        }

        var newW = (width * ratio).toInt()
        var newH = (height * ratio).toInt()

        // Pad到32的倍数，至少32
        newW = ((newW + 31) / 32 * 32).coerceAtLeast(32)
        newH = ((newH + 31) / 32 * 32).coerceAtLeast(32)

        return Pair(newW, newH)
    }

    /**
     * 计算识别模型的输入宽度
     * 规则：高度固定48，宽度按原始宽高比缩放，最大不超过 REC_MAX_WIDTH
     *
     * @param origWidth 原始宽度
     * @param origHeight 原始高度
     * @return 目标宽度
     */
    fun calculateRecWidth(origWidth: Int, origHeight: Int): Int {
        if (origWidth <= 0 || origHeight <= 0) return REC_MAX_WIDTH
        val ratio = REC_IMG_HEIGHT.toFloat() / origHeight
        return (origWidth * ratio).toInt().coerceIn(1, REC_MAX_WIDTH)
    }

    /**
     * 对单个像素值进行 ImageNet 标准归一化
     * 公式: (pixel / 255.0 - mean) / std
     *
     * @param pixel 像素值 [0, 255]
     * @param mean 通道均值
     * @param std 通道标准差
     * @return 归一化后的浮点值
     */
    fun normalizeChannel(pixel: Int, mean: Float, std: Float): Float {
        return (pixel / 255f - mean) / std
    }

    /**
     * 将 Bitmap 预处理为检测模型输入 (NCHW格式, batch=1)
     *
     * @param bitmap 输入图像
     * @param maxSideLen 最长边上限
     * @return Pair(归一化浮点数组, Pair(缩放后宽度, 缩放后高度))
     */
    fun preprocessForDetection(
        bitmap: Bitmap,
        maxSideLen: Int = DET_MAX_SIDE_LEN
    ): Pair<FloatArray, Pair<Int, Int>> {
        val (targetW, targetH) = calculateDetSize(bitmap.width, bitmap.height, maxSideLen)
        val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        val inputData = bitmapToNchwFloat(resized, targetW, targetH)

        if (resized !== bitmap) resized.recycle()
        return Pair(inputData, Pair(targetW, targetH))
    }

    /**
     * 将 Bitmap 预处理为识别模型输入 (NCHW格式, batch=1)
     * 高度固定48, 宽度按比例缩放并 pad 到 REC_MAX_WIDTH
     *
     * @param bitmap 输入文本行图像
     * @return 归一化浮点数组 [1, 3, 48, 320]
     */
    fun preprocessForRecognition(bitmap: Bitmap): FloatArray {
        val recW = calculateRecWidth(bitmap.width, bitmap.height)
        val resized = Bitmap.createScaledBitmap(bitmap, recW, REC_IMG_HEIGHT, true)

        // 创建固定大小的输入，超出部分填0（pad）
        val inputData = FloatArray(3 * REC_IMG_HEIGHT * REC_MAX_WIDTH)

        // 填充有效像素区域
        val pixels = IntArray(recW * REC_IMG_HEIGHT)
        resized.getPixels(pixels, 0, recW, 0, 0, recW, REC_IMG_HEIGHT)
        fillNchwDataFromPixels(pixels, inputData, recW, REC_IMG_HEIGHT, REC_MAX_WIDTH)

        if (resized !== bitmap) resized.recycle()
        return inputData
    }

    /**
     * 将 Bitmap 预处理为分类模型输入 (NCHW格式, batch=1)
     *
     * @param bitmap 输入文本行图像
     * @return 归一化浮点数组 [1, 3, 48, 192]
     */
    fun preprocessForClassification(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, CLS_IMG_WIDTH, CLS_IMG_HEIGHT, true)
        val inputData = bitmapToNchwFloat(resized, CLS_IMG_WIDTH, CLS_IMG_HEIGHT)
        if (resized !== bitmap) resized.recycle()
        return inputData
    }

    /**
     * 将 Bitmap 转换为 NCHW 格式的归一化 FloatArray
     * 通道顺序：RGB，使用 ImageNet 标准归一化
     *
     * @param bitmap 输入图像
     * @param width 图像宽度
     * @param height 图像高度
     * @return NCHW 格式的浮点数组
     */
    fun bitmapToNchwFloat(bitmap: Bitmap, width: Int, height: Int): FloatArray {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return pixelsToNchwFloat(pixels, width, height)
    }

    /**
     * 将 ARGB 像素数组转换为 NCHW 格式的归一化 FloatArray
     * 不依赖 Android Bitmap，可用于单元测试
     *
     * @param pixels ARGB 格式像素数组
     * @param width 图像宽度
     * @param height 图像高度
     * @return NCHW 格式的浮点数组
     */
    fun pixelsToNchwFloat(pixels: IntArray, width: Int, height: Int): FloatArray {
        val channelSize = width * height
        val inputData = FloatArray(3 * channelSize)

        for (i in 0 until channelSize) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // NCHW布局: [R通道全部像素, G通道全部像素, B通道全部像素]
            inputData[i] = normalizeChannel(r, MEAN_R, STD_R)
            inputData[channelSize + i] = normalizeChannel(g, MEAN_G, STD_G)
            inputData[2 * channelSize + i] = normalizeChannel(b, MEAN_B, STD_B)
        }

        return inputData
    }

    /**
     * 填充 NCHW 数据（支持实际宽度小于目标宽度时的 padding）
     * padding 区域保持为 0（FloatArray 初始化默认值）
     * 不依赖 Android Bitmap，可用于单元测试
     *
     * @param pixels ARGB 像素数组（actualW × height）
     * @param output 输出缓冲区（3 × height × totalW）
     * @param actualW 有效像素宽度
     * @param height 图像高度
     * @param totalW 输出总宽度（含 padding）
     */
    fun fillNchwDataFromPixels(
        pixels: IntArray,
        output: FloatArray,
        actualW: Int,
        height: Int,
        totalW: Int
    ) {
        val channelSize = height * totalW

        for (y in 0 until height) {
            for (x in 0 until actualW) {
                val pixelIdx = y * actualW + x
                val outputIdx = y * totalW + x
                val pixel = pixels[pixelIdx]

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                output[outputIdx] = normalizeChannel(r, MEAN_R, STD_R)
                output[channelSize + outputIdx] = normalizeChannel(g, MEAN_G, STD_G)
                output[2 * channelSize + outputIdx] = normalizeChannel(b, MEAN_B, STD_B)
            }
        }
        // padding 区域保持默认值 0
    }
}
