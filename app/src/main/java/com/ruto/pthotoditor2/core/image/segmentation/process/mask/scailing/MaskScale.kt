package com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object MaskScale {

    fun scaleMaskWithCanvas(mask: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scaledMask = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledMask)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val scaleX = targetWidth / mask.width.toFloat()
        val scaleY = targetHeight / mask.height.toFloat()

        canvas.scale(scaleX, scaleY)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        return scaledMask
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