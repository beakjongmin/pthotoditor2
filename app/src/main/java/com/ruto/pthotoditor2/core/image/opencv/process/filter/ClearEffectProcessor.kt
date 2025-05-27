package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object ClearEffectProcessor {

    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val resultMat = applyNaturalClearEnhancement(src)

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)

        return resultBitmap
    }

    fun applyNaturalClearEnhancement(src: Mat): Mat {
        val bgr = Mat()
        val ycrcb = Mat()
        val merged = Mat()
        val balanced = Mat()
        val finalResult = Mat()

        try {
            // 1. RGBA → BGR
            Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

            // 2. YCrCb 변환 + 약한 CLAHE
            Imgproc.cvtColor(bgr, ycrcb, Imgproc.COLOR_BGR2YCrCb)
            val channels = ArrayList<Mat>()
            Core.split(ycrcb, channels)

            val clahe = Imgproc.createCLAHE()
            clahe.clipLimit = 0.15
            clahe.setTilesGridSize(Size(8.0, 8.0))
            clahe.apply(channels[0], channels[0]) // Y 채널에만 적용

            Core.merge(channels, ycrcb)
            Imgproc.cvtColor(ycrcb, merged, Imgproc.COLOR_YCrCb2BGR)
            channels.forEach { it.release() }

            // 3. 얼굴 중심 영역 기준 화이트 밸런스
            val faceRect = Rect(
                bgr.cols() / 4, bgr.rows() / 4,
                bgr.cols() / 2, bgr.rows() / 2
            )
            val roi = Mat(merged, faceRect)
            val avgColor = Core.mean(roi)



            val rgbChannels = ArrayList<Mat>()
            Core.split(merged, rgbChannels)

            Core.merge(rgbChannels, balanced)
            rgbChannels.forEach { it.release() }

            // 4. 소프트 톤 압축 (하이라이트 살짝 억제)
            val toneCompressed = Mat()
            Core.multiply(balanced, Scalar(0.98, 0.98, 0.98), toneCompressed)

            // 5. 소프트 글로우 추가
            val blurred = Mat()
            Imgproc.GaussianBlur(toneCompressed, blurred, Size(0.0, 0.0), 15.0)
            Core.addWeighted(toneCompressed, 1.0, blurred, 0.05, 0.0, finalResult)

            // 6. 알파 채널 복원
            val result = Mat()
            val rgbaChannels = ArrayList<Mat>()
            Core.split(src, rgbaChannels)
            val alpha = rgbaChannels[3].clone()

            val resultChannels = ArrayList<Mat>()
            Core.split(finalResult, resultChannels)

            val reordered = listOf(
                resultChannels[2], // R
                resultChannels[1], // G
                resultChannels[0], // B
                alpha              // A
            )
            Core.merge(reordered, result)

            // alpha=0 픽셀 RGB 클리핑
            for (y in 0 until result.rows()) {
                for (x in 0 until result.cols()) {
                    val pixel = result.get(y, x)
                    if (pixel[3] == 0.0) result.put(y, x, 0.0, 0.0, 0.0, 0.0)
                }
            }

            // 메모리 정리
            alpha.release()
            rgbaChannels.forEach { it.release() }
            resultChannels.forEach { it.release() }

            return result
        } finally {
            bgr.release()
            ycrcb.release()
            merged.release()
            balanced.release()
            finalResult.release()
        }
    }
}