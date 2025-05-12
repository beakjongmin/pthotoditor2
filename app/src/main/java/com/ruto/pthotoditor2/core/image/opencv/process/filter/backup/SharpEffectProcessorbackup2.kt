package com.ruto.pthotoditor2.core.image.opencv.process.filter.backup

import android.graphics.Bitmap
import android.util.Log
import com.ruto.pthotoditor2.core.image.ml.debuggingfunction.ColorLogger
import com.ruto.pthotoditor2.core.image.opencv.process.filter.CommonUtils.clipRGBRange
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object SharpEffectProcessorbackup2 {

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

        val resultMat = applyDetailSharpening(src, sharpnessLevel = 0.8)

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

    fun applyDetailSharpening(src: Mat, sharpnessLevel: Double = 1.2): Mat {
        val tag = "SharpEffectProcessor"

        // 🔹 중간 결과 저장용 Mat들 선언
        val bgr = Mat()           // RGBA → BGR 변환된 이미지 (알파 제외)
        val blurred = Mat()       // 블러 처리된 BGR 이미지 (디테일 마스크 계산용)
        val detailMask = Mat()    // 원본과 블러 차이 → 디테일 강조 마스크
        val sharpened = Mat()     // 샤프닝 처리 결과 (BGR)
        val clipped: Mat          // RGB 값 클리핑된 결과 (샤프닝된 BGR → RGB 범위 보정)
        val result = Mat()        // 최종 RGBA 병합 이미지
        //고주파
//        val laplacian = Mat()
//        val lapSharpened = Mat()

        try {
            // 1. RGBA → BGR 변환 (OpenCV는 내부적으로 BGR 기준 처리)
            Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

            // 2. 부드럽게 블러 처리 (Unsharp Masking 기법의 기본)
            Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), 0.5)

            // 3. 원본과 블러 이미지의 차이를 계산 → 디테일 강조 부분 추출
            Core.absdiff(bgr, blurred, detailMask)

            // 4. 원본 + 디테일 마스크를 가중합 → 샤프닝 처리
            Core.addWeighted(bgr, 1.0, detailMask, sharpnessLevel, 0.0, sharpened)


            //laplacian 디테일 추가
            val laplacian = Mat()
            Imgproc.Laplacian(bgr, laplacian, bgr.depth(), 3)

            val combined = Mat()
            Core.addWeighted(sharpened, 1.0, laplacian, 0.4, 0.0, combined)
            // 5. 샤프닝 결과의 RGB 값을 0~255 범위로 보정
            clipped = clipRGBRange(combined)

            // 6. 원본 src에서 R, G, B, A 분리 → 알파 채널은 따로 유지
            val rgbaChannels = ArrayList<Mat>()
            Core.split(src, rgbaChannels)
            val alpha = rgbaChannels[3].clone() // 투명도 정보 따로 저장

            // 7. clipped에서 B, G, R 채널 추출
            val resultChannels = ArrayList<Mat>()
            Core.split(clipped, resultChannels)

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
//    fun applyDetailSharpening(src: Mat, sharpnessLevel: Double = 0.8): Mat {
//        val tag = "SharpEffectProcessor"
//
//        val bgr = Mat()
//        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR) // RGBA → BGR 변환 (OpenCV 내부는 BGR 기준)
//
//
//        val blurred = Mat()
//        Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), 1.0)
//
//        val detailMask = Mat()
//        Core.absdiff(bgr, blurred, detailMask)
//
//        val sharpened = Mat()
//        Core.addWeighted(bgr, 1.0, detailMask, sharpnessLevel, 0.0, sharpened)
//        //clipped: 샤프닝 후 나온 결과 → BGR (알파 없음)
//        val clipped = clipRGBRange(sharpened)
//
//        // 채널 분리 후 알파 채널 복사
//        val rgbaChannels = ArrayList<Mat>()
//        Core.split(src, rgbaChannels) // src는 RGBA이므로 마지막 채널이 알파
//
//        val alpha = rgbaChannels[3].clone() //픽셀의 투명도(opacity)
//
//        // sharpened는 BGR 순서이므로, 병합 전 RGBA 순서를 맞추기 위해 R,G,B 순으로 정렬
//        val resultChannels = ArrayList<Mat>()
//
//        //// clipped.get(768, 768) 결과
//        //double[] = [80.0, 120.0, 200.0]  // B, G, R
//        Core.split(clipped, resultChannels)
//
//
//        // RGBA 순서로 재배열
//        val reorderedChannels = listOf(resultChannels[2], resultChannels[1], resultChannels[0], alpha)
//
//        Log.d(tag, "🧪 RGB + 알파 채널 병합 전: 채널 수=${reorderedChannels.size}, alpha 타입=${alpha.type()}")
//
//        val result = Mat()
//        Core.merge(reorderedChannels, result) // 병합 시 RGBA 순서로
//        //우리는 최종적으로 다시 RGBA 형태의 Bitmap 으로 되돌리고 싶음
//        Log.d(tag, "✅ RGBA 병합 완료 - size=${result.size()}, type=${result.type()}")
//        return result
//    }

    fun applyUnsharpMask(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

        val blurred = Mat()
        Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), 1.0)

        val mask = Mat()
        Core.subtract(bgr, blurred, mask)

        val result = Mat()
        Core.addWeighted(bgr, 1.0, mask, 1.5, 0.0, result)

        val clipped = clipRGBRange(result)
        val rgba = Mat()
        Imgproc.cvtColor(clipped, rgba, Imgproc.COLOR_BGR2RGBA)

        val output = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, output)
        return output
    }
}
