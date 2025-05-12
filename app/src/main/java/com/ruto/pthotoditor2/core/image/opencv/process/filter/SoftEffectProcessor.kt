package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Scalar

object SoftEffectProcessor {

    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val qualityLevel = CommonUtils.getImageQualityLevel(bitmap)
        val (ksize, sigma) = when (qualityLevel) {
            "HIGH" -> 9 to 75.0
            "MEDIUM" -> 7 to 50.0
            else -> 5 to 30.0
        }
        val tone = when (qualityLevel) {
            "HIGH" -> Scalar(3.0, 1.5, 0.0)
            "MEDIUM" -> Scalar(2.0, 1.0, 0.0)
            else -> Scalar(1.0, 0.5, 0.0)
        }
        val resultMat = CommonUtils.applySoftSmoothingWithTone(src, ksize, sigma, tone)
        val result = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, result)
        return result
    }
}