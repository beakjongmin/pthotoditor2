package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

object CommonUtils {

    fun getImageQualityLevel(bitmap: Bitmap): String {
        val pixelCount = bitmap.width * bitmap.height
        return when {
            pixelCount >= 1920 * 1080 -> "HIGH"
            pixelCount >= 1280 * 720 -> "MEDIUM"
            else -> "LOW"
        }
    }

    fun clipRGBRange(mat: Mat, min: Double = 10.0, max: Double = 240.0): Mat {
        val channels = ArrayList<Mat>()
        val result = Mat()
        try {
            // 채널 분리
            Core.split(mat, channels)

            // 각 채널 클리핑
            for (i in channels.indices) {
                Core.min(channels[i], Scalar(max), channels[i])
                Core.max(channels[i], Scalar(min), channels[i])
            }

            // 병합 결과 생성
            Core.merge(channels, result)

            return result
        } finally {
            // 채널 Mat 해제
            channels.forEach { it.release() }
            // result는 리턴했으므로, 호출한 쪽에서 release 책임
        }
    }

    fun clipRGBRangePreserveAlpha(mat: Mat, min: Double = 10.0, max: Double = 240.0): Mat {
        val channels = ArrayList<Mat>()
        Core.split(mat, channels)

        val numChannels = mat.channels()
        val clippedChannels = ArrayList<Mat>()

        for (i in 0 until numChannels) {
            if (i < 3) {
                // R, G, B 채널에만 clip 적용
                val clipped = Mat()
                Core.min(channels[i], Scalar(max), clipped)
                Core.max(clipped, Scalar(min), clipped)
                clippedChannels.add(clipped)
            } else {
                // Alpha 채널은 그대로 유지
                clippedChannels.add(channels[i])
            }
        }

        val result = Mat()
        Core.merge(clippedChannels, result)
        return result
    }

    fun applyGammaCorrection(mat: Mat, gamma: Double): Mat {
        val lut = Mat(1, 256, CvType.CV_8UC1)
        for (i in 0..255) {
            val corrected = (i / 255.0).pow(1.0 / gamma) * 255.0
            lut.put(0, i, corrected)
        }
        val channels = ArrayList<Mat>()
        Core.split(mat, channels)
        for (i in channels.indices) {
            Core.LUT(channels[i], lut, channels[i])
        }
        val result = Mat()
        Core.merge(channels, result)
        return result
    }


    fun applySoftSmoothingWithTone(src: Mat, ksize: Int, sigma: Double, tone: Scalar): Mat {
        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)
        val smoothed = Mat()
        Imgproc.bilateralFilter(bgr, smoothed, ksize, sigma, sigma)
        val warmed = Mat()
        Core.add(smoothed, tone, warmed)
        val rgba = Mat()
        Imgproc.cvtColor(warmed, rgba, Imgproc.COLOR_BGR2RGBA)
        return rgba
    }
}