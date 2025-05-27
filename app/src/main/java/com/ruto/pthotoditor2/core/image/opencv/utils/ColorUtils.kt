package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object ColorUtils {


    fun matchToneByMean(reference: Bitmap, target: Bitmap, alphaMask: Bitmap): Bitmap {
        val width = target.width
        val height = target.height

        val refMat = Mat()
        val tgtMat = Mat()
        val maskMat = Mat()
        Utils.bitmapToMat(reference, refMat)
        Utils.bitmapToMat(target, tgtMat)
        Utils.bitmapToMat(alphaMask, maskMat)

        val refBGR = Mat()
        val tgtBGR = Mat()
        Imgproc.cvtColor(refMat, refBGR, Imgproc.COLOR_RGBA2BGR)
        Imgproc.cvtColor(tgtMat, tgtBGR, Imgproc.COLOR_RGBA2BGR)

        // 알파 채널에서 0이 아닌 부분만 추출
        val maskAlpha = Mat()
        val channels = mutableListOf<Mat>()
        Core.split(maskMat, channels)
        channels[3].copyTo(maskAlpha)

        val meanRef = Core.mean(refBGR, maskAlpha)
        val meanTgt = Core.mean(tgtBGR, maskAlpha)

        val diff = Scalar(
            meanRef.`val`[0] - meanTgt.`val`[0],
            meanRef.`val`[1] - meanTgt.`val`[1],
            meanRef.`val`[2] - meanTgt.`val`[2]
        )

        Core.add(tgtBGR, diff, tgtBGR, maskAlpha)

        val resultRGBA = Mat()
        Imgproc.cvtColor(tgtBGR, resultRGBA, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultRGBA, result)
        return result
    }


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