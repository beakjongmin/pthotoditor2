package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar

object ColorUtils {

    fun matchToneByMean(reference: Bitmap, target: Bitmap): Bitmap {
        val srcMat = Mat()
        val refMat = Mat()
        Utils.bitmapToMat(target, srcMat)
        Utils.bitmapToMat(reference, refMat)

        val meanRef = Core.mean(refMat)
        val meanSrc = Core.mean(srcMat)

        val diff = Scalar(
            meanRef.`val`[0] - meanSrc.`val`[0],
            meanRef.`val`[1] - meanSrc.`val`[1],
            meanRef.`val`[2] - meanSrc.`val`[2]
        )

        Core.add(srcMat, diff, srcMat)

        val result = Bitmap.createBitmap(srcMat.cols(), srcMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(srcMat, result)
        return result
    }
}