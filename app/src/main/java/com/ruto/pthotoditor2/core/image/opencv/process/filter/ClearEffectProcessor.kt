package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object ClearEffectProcessor {

    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val ycrcb = Mat()
        Imgproc.cvtColor(src, ycrcb, Imgproc.COLOR_BGR2YCrCb)
        val channels = ArrayList<Mat>()
        Core.split(ycrcb, channels)
        val clahe = Imgproc.createCLAHE().apply { clipLimit = 0.6 }
        clahe.apply(channels[0], channels[0])
        Core.merge(channels, ycrcb)
        Imgproc.cvtColor(ycrcb, ycrcb, Imgproc.COLOR_YCrCb2BGR)

        val clipped = CommonUtils.clipRGBRange(ycrcb, 10.0, 240.0)
        val result = Bitmap.createBitmap(clipped.cols(), clipped.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(clipped, result)
        return result
    }
}