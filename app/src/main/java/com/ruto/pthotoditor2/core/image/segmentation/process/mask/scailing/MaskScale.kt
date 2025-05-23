package com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object MaskScale {

    fun featherAlphaMask2(mask: Bitmap, radius: Double = 3.0): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(mask, mat)

        val channels = ArrayList<Mat>()
        Core.split(mat, channels)

        val alpha = channels.getOrNull(3)
            ?: throw IllegalStateException("Alpha channel not found")

        // ❗ get()은 생략, 대신 안전하게 변환
        val safeAlpha = Mat()
        alpha.convertTo(safeAlpha, CvType.CV_8UC1)

        Imgproc.GaussianBlur(safeAlpha, safeAlpha, Size(radius, radius), 0.0)

        channels[3] = safeAlpha
        Core.merge(channels, mat)

        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)
        return result
    }

    fun featherAlphaMask(mask: Bitmap, radius: Double = 3.0): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(mask, mat)
        val channels = ArrayList<Mat>()
        Core.split(mat, channels)

        val alpha = channels[3]
        Imgproc.GaussianBlur(alpha, alpha, Size(radius, radius), 0.0)

        channels[3] = alpha
        Core.merge(channels, mat)

        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)
        return result
    }
}