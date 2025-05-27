package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object SharpEffectProcessor {

    // OpenCV는 기본적으로 BGR 순서로 데이터를 처리하는데, RGBA ↔ BGR 변환 시 채널 순서를 착각하면 RGB가 역전된 것처럼 보일 수 있음
    //플랫폼	채널 순서
    //Android Bitmap (ARGB_8888)	R → G → B → A
    //OpenCV Mat (default)	B → G → R


    fun apply(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src) // src는 RGBA 순서

        val resultMat = applyDetailSharpening(src, sharpnessLevel = 0.4)

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)

        return resultBitmap
    }


    fun applyDetailSharpening(src: Mat, sharpnessLevel: Double = 1.2 ): Mat {


        val bgr = Mat()
        val lab = Mat()
        val claheResult = Mat()
        val blurred = Mat()
        val highBoost = Mat()
        val result = Mat()

        try {
            // 1. RGBA → BGR 변환
            Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

            // 2. CLAHE (Contrast Limited Adaptive Histogram Equalization)
            Imgproc.cvtColor(bgr, lab, Imgproc.COLOR_BGR2Lab)
            val labChannels = ArrayList<Mat>()
            Core.split(lab, labChannels)

            val clahe = Imgproc.createCLAHE()
            clahe.clipLimit = 	0.15
            clahe.apply(labChannels[0], labChannels[0]) // L 채널에만 적용

            Core.merge(labChannels, lab)
            Imgproc.cvtColor(lab, claheResult, Imgproc.COLOR_Lab2BGR)
            labChannels.forEach { it.release() }

            // 3. High-Boost Filtering: 원본 + (원본 - 블러) * 계수
            Imgproc.GaussianBlur(claheResult, blurred, Size(0.0, 0.0), 1.0) // ✅ 더 약하게
            Core.addWeighted(claheResult, 1.0 + sharpnessLevel, blurred, -sharpnessLevel, 0.0, highBoost)

            // 4. 알파 채널 복원 (원본 src에서)
            val rgbaChannels = ArrayList<Mat>()
            Core.split(src, rgbaChannels)
            val alpha = rgbaChannels[3].clone()

            val resultChannels = ArrayList<Mat>()
            Core.split(highBoost, resultChannels)

            val reorderedChannels = listOf(
                resultChannels[2], // R
                resultChannels[1], // G
                resultChannels[0], // B
                alpha              // A
            )
            Core.merge(reorderedChannels, result)

            // 알파 0인 픽셀의 RGB를 강제로 0으로 클리핑
            for (y in 0 until result.rows()) {
                for (x in 0 until result.cols()) {
                    val pixel = result.get(y, x)
                    val alpha = pixel[3]
                    if (alpha == 0.0) {
                        result.put(y, x, 0.0, 0.0, 0.0, 0.0)
                    }
                }
            }
            // 🔄 리소스 해제
            alpha.release()
            rgbaChannels.forEach { it.release() }
            resultChannels.forEach { it.release() }

            return result
        } finally {
            bgr.release()
            lab.release()
            claheResult.release()
            blurred.release()
            highBoost.release()
        }
    }


}