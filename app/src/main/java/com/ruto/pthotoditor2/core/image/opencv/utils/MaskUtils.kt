package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import android.graphics.Color
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

}