package com.ruto.pthotoditor2.core.image.opencv.process.filter

import android.graphics.Bitmap
import android.util.Log
import com.ruto.pthotoditor2.debuggingfunction.ColorLogger
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
        val tag = "SharpEffectProcessor"
        Log.d(tag, "apply() 시작 - Bitmap size: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
        ColorLogger.logPixel("apply픽셀값", bitmap, bitmap.width / 2, bitmap.height / 2)

        val src = Mat()
        Utils.bitmapToMat(bitmap, src) // src는 RGBA 순서
        Log.d(tag, "🔄 Bitmap → Mat 변환 완료 - size=${src.size()}, channels=${src.channels()}")

        val resultMat = applyDetailSharpening2(src, sharpnessLevel = 0.4)

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)
        Log.d(tag, "🔄 Mat → Bitmap 복원 완료 - 결과 size: ${resultBitmap.width}x${resultBitmap.height}")
        ColorLogger.logPixel("apply이후픽셀값", resultBitmap, resultBitmap.width / 2, resultBitmap.height / 2)

        return resultBitmap
    }

    /**
     * @param src "이것은 RGBA 형식입니다."
     *
     *      *
     *      * src: 원래 Bitmap을 Mat으로 변환한 것 → RGBA
     *      *
     *      * clipped: 샤프닝 후 나온 결과 → BGR (알파 없음)
     *      *
     *      * 우리는 최종적으로 다시 RGBA 형태의 Bitmap 으로 되돌리고 싶음
     */

    fun applyDetailSharpening(src: Mat, sharpnessLevel: Double = 0.5): Mat {
        val tag = "SharpEffectProcessor"

        // 🔹 중간 결과 저장용 Mat들 선언
        val bgr = Mat()           // RGBA → BGR 변환된 이미지 (알파 제외)
        val blurred = Mat()       // 블러 처리된 BGR 이미지 (디테일 마스크 계산용)
        val detailMask = Mat()    // 원본과 블러 차이 → 디테일 강조 마스크
        val sharpened = Mat()     // 샤프닝 처리 결과 (BGR)
        val clipped: Mat          // RGB 값 클리핑된 결과 (샤프닝된 BGR → RGB 범위 보정)
        val result = Mat()        // 최종 RGBA 병합 이미지

        try {
            // 1. RGBA → BGR 변환 (OpenCV는 내부적으로 BGR 기준 처리)
            Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

            // 2. 부드럽게 블러 처리 (Unsharp Masking 기법의 기본)
            Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), 0.8)
            // Median Blur로 좀 더 노이즈 억제
//            Imgproc.medianBlur(bgr, blurred,3)

            // 3. 원본과 블러 이미지의 차이를 계산 → 디테일 강조 부분 추출
            Core.absdiff(bgr, blurred, detailMask)

            // 4. 원본 + 디테일 마스크를 가중합 → 샤프닝 처리

//            Core.addWeighted(bgr, 1.0, detailMask, sharpnessLevel, 0.0, sharpened)
            Core.addWeighted(bgr, 1.0, detailMask, 0.5, 0.0, sharpened)
            // 5. 샤프닝 결과의 RGB 값을 0~250 범위로 보정
//            clipped = clipRGBRange(sharpened, min = 10.0, max = 250.0)

            // 6. 원본 src에서 R, G, B, A 분리 → 알파 채널은 따로 유지
            val rgbaChannels = ArrayList<Mat>()
            Core.split(src, rgbaChannels)
            val alpha = rgbaChannels[3].clone() // 투명도 정보 따로 저장

            // 7. clipped에서 B, G, R 채널 추출
            val resultChannels = ArrayList<Mat>()
            Core.split(sharpened, resultChannels)

            // 8. R, G, B + A 순으로 병합할 준비 (RGBA 순서)
            val reorderedChannels = listOf(
                resultChannels[2], // R
                resultChannels[1], // G
                resultChannels[0], // B
                alpha              // A
            )

            // 9. RGB + A 병합 → 최종 RGBA 이미지 구성
            Core.merge(reorderedChannels, result)

            // 🔹 더 이상 사용하지 않는 Mat 객체 해제
            alpha.release()
            rgbaChannels.forEach { it.release() }
            resultChannels.forEach { it.release() }

            return result
        } finally {
            // 🔹 예외 발생 여부와 관계없이 해제할 Mat
            bgr.release()
            blurred.release()
            detailMask.release()
            sharpened.release()

            // clipped는 위에서 복사한 값이고 반환에 포함되지 않으므로 release 생략 가능
        }
    }


    fun applyDetailSharpening2(src: Mat, sharpnessLevel: Double = 1.2 ): Mat {
        val tag = "SharpEffectProcessor"

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
