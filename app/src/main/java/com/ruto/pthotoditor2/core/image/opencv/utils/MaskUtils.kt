package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mlkit.vision.segmentation.SegmentationMask


/*
 @object mask 관련 유틸 (extractMaskedRegion, toSoftAlphaMask 등)
 */
object MaskUtils {

    fun toSoftAlphaMask(mask: SegmentationMask, width: Int, height: Int): Bitmap {
        val buffer = mask.buffer
        buffer.rewind()
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val confidence = buffer.float
            val alpha = (confidence * 255).toInt().coerceIn(0, 255)
            pixels[i] = (alpha shl 24) or 0xFFFFFF
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }


    /**
     * mask 에 값을 바탕으로 인물영역을 분리해주는 유틸함수
     */
    fun extractMaskedRegion(bitmap: Bitmap, mask: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val origPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val output = IntArray(width * height)

        for (i in origPixels.indices) {
            val alpha = (maskPixels[i] shr 24) and 0xFF
            if (alpha > 0) {
                output[i] = origPixels[i]
            } else {
                output[i] = Color.TRANSPARENT
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    fun toHardAlphaMask(mask: SegmentationMask, width: Int, height: Int, threshold: Float = 0.6f): Bitmap {
        val buffer = mask.buffer
        buffer.rewind()
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val confidence = buffer.float
            val alpha = if (confidence >= threshold) 255 else 0
            pixels[i] = (alpha shl 24) or 0xFFFFFF // 흰색 RGB + 알파
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }


    fun applyFilterWithAlphaMask(
        original: Bitmap,
        filtered: Bitmap,
        mask: Bitmap
    ): Bitmap {
        require(original.width == filtered.width && filtered.width == mask.width)
        require(original.height == filtered.height && filtered.height == mask.height)

        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)

        val originalPixels = IntArray(original.width * original.height)
        val filteredPixels = IntArray(filtered.width * filtered.height)
        val maskPixels = IntArray(mask.width * mask.height)

        original.getPixels(originalPixels, 0, original.width, 0, 0, original.width, original.height)

        filtered.getPixels(filteredPixels, 0, filtered.width, 0, 0, filtered.width, filtered.height)

        mask.getPixels(maskPixels,0,mask.width,0,0,mask.width,mask.height)

        var countTotal = maskPixels.size
        var countApplied = 0
        var countSkipped = 0
        var maxAlpha = 0
        var minAlpha = 255

        for (i in maskPixels.indices) {
            val alpha = (maskPixels[i] shr 24) and 0xFF

            if (alpha > 0) {
                result.setPixel(i % original.width, i / original.width, filteredPixels[i])
                countApplied++
            } else {
                result.setPixel(i % original.width, i / original.width, originalPixels[i])
                countSkipped++
            }

            if (alpha > maxAlpha) maxAlpha = alpha
            if (alpha < minAlpha) minAlpha = alpha
        }

        Log.d("AlphaMaskDebug", "📊 전체 픽셀 수: $countTotal")
        Log.d("AlphaMaskDebug", "✅ 필터 적용 픽셀 수 (alpha > 0): $countApplied")
        Log.d("AlphaMaskDebug", "⛔ 필터 미적용 픽셀 수 (alpha = 0): $countSkipped")
        Log.d("AlphaMaskDebug", "🔍 알파 값 범위: $minAlpha ~ $maxAlpha")

        return result
    }
}