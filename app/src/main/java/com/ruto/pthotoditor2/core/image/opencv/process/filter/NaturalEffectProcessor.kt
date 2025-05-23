package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object NaturalEffectProcessor {

    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        val bgr = Mat()
        val smoothed = Mat()
        val result = Mat()
        var gammaCorrected: Mat? = null

        try {
            Utils.bitmapToMat(bitmap, src)

            // 1. RGBA → BGR 변환
            Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

            // 2. 스무딩 계수 설정 (톤 날림 방지)
            val qualityLevel = CommonUtils.getImageQualityLevel(bitmap)
            val (d, sigmaColor, sigmaSpace, gamma) = when (qualityLevel) {
                "HIGH" -> Quadruple(5, 50.0, 30.0, 1.05)
                "MEDIUM" -> Quadruple(5, 40.0, 20.0, 1.0)
                else -> Quadruple(3, 30.0, 15.0, 0.95)
            }

            // 3. Bilateral Filter 적용
            Imgproc.bilateralFilter(bgr, smoothed, d, sigmaColor, sigmaSpace)

            // 4. Gamma Correction
            gammaCorrected = CommonUtils.applyGammaCorrection(smoothed, gamma)

            // 5. 알파 채널 복원
            val rgbaChannels = ArrayList<Mat>()
            Core.split(src, rgbaChannels)
            val alpha = rgbaChannels[3].clone()

            val resultChannels = ArrayList<Mat>()
            Core.split(gammaCorrected, resultChannels)

            val reorderedChannels = listOf(
                resultChannels[2], // R
                resultChannels[1], // G
                resultChannels[0], // B
                alpha              // A
            )
            Core.merge(reorderedChannels, result)

            // 6. 알파 0인 영역 RGB 클리핑
            for (y in 0 until result.rows()) {
                for (x in 0 until result.cols()) {
                    val pixel = result.get(y, x)
                    if (pixel[3] == 0.0) {
                        result.put(y, x, 0.0, 0.0, 0.0, 0.0)
                    }
                }
            }

            // 7. Bitmap 결과 반환
            val output = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(result, output)
            return output

        } finally {
            src.release()
            bgr.release()
            smoothed.release()
            gammaCorrected?.release()
            result.release()
        }
    }

    // Kotlin엔 Quadruple이 없으니 내부에 정의
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
