package com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object MaskScale {

    //마스크 영역을 희미하게 만들어서 ( alpha 분포를 좀더 퍼트림)
    fun featherAlphaMask(mask: Bitmap, radius: Double = 3.0): Bitmap {
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

    /**
     * 마스크 영역을 확장시킨다 -> 마스크에서 추출시 영역이 희미하게 남을경우 사용
     */
    fun dilateAlphaMask(mask: Bitmap, radius: Double = 3.0): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(mask, srcMat)

        val channels = ArrayList<Mat>()
        Core.split(srcMat, channels)

        val alphaChannel = channels.getOrNull(3)
            ?: throw IllegalArgumentException("Alpha channel not found")

        val processedAlpha = Mat()
        alphaChannel.convertTo(processedAlpha, CvType.CV_8UC1)

        // 팽창 커널 생성 (radius에 따라)
        val kernelSize = (radius * 2 + 1).toInt()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(kernelSize.toDouble(), kernelSize.toDouble()))

        // Dilation
        Imgproc.dilate(processedAlpha, processedAlpha, kernel)

        // 결과 병합
        channels[3] = processedAlpha
        Core.merge(channels, srcMat)

        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(srcMat, result)
        return result
    }
}