package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object NaturalEffectProcessor {

    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

        val qualityLevel = CommonUtils.getImageQualityLevel(bitmap)
        val (d, sigma, gamma) = when (qualityLevel) {
            "HIGH" -> Triple(9, 90.0, 1.4)
            "MEDIUM" -> Triple(7, 60.0, 1.3)
            else -> Triple(5, 30.0, 1.2)
        }

        val smoothed = Mat()
        Imgproc.bilateralFilter(bgr, smoothed, d, sigma, sigma)

        val gammaCorrected = CommonUtils.applyGammaCorrection(smoothed, gamma)
        val rgba = Mat()
        Imgproc.cvtColor(gammaCorrected, rgba, Imgproc.COLOR_BGR2RGBA)
        val result = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, result)
        return result
    }

}