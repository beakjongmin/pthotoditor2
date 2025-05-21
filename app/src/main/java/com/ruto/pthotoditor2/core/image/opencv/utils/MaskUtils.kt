package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.segmentation.SegmentationMask
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


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

    fun andMasks(mask1: Bitmap, mask2: Bitmap): Mat {
        val m1 = Mat()
        val m2 = Mat()
        val gray1 = Mat()
        val gray2 = Mat()
        val result = Mat()
        try {
            Utils.bitmapToMat(mask1, m1)
            Utils.bitmapToMat(mask2, m2)

            Imgproc.cvtColor(m1, gray1, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(m2, gray2, Imgproc.COLOR_RGBA2GRAY)

            Core.bitwise_and(gray1, gray2, result)
            return result.clone() // 호출자에서 해제해야 하므로 clone()
        } finally {
            m1.release()
            m2.release()
            gray1.release()
            gray2.release()
            // result는 호출자에서 사용하는 값이므로 여기서는 release X
        }
    }
}