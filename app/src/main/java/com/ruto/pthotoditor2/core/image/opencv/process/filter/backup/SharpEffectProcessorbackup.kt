package com.ruto.pthotoditor2.core.image.opencv.process.filter.backup

import android.graphics.Bitmap
import android.util.Log
import com.ruto.pthotoditor2.core.image.opencv.process.filter.CommonUtils
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object SharpEffectProcessorbackup {

    fun apply(bitmap: Bitmap): Bitmap {
        val tag = "SharpEffectProcessor"
        Log.d(tag, "apply() 시작 - Bitmap size: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        Log.d(tag, "🔄 Bitmap → Mat 변환 완료 - size=${src.size()}, channels=${src.channels()}")

        val resultMat = applyLaplacianSharpeningWithAlphaSafe(src)

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)
        Log.d(tag, "🔄 Mat → Bitmap 복원 완료 - 결과 size: ${resultBitmap.width}x${resultBitmap.height}")

        return resultBitmap
    }

    fun applyLaplacianSharpeningWithAlphaSafe(src: Mat): Mat {
        val tag = "SharpEffectProcessor"

        val rgbaChannels = ArrayList<Mat>()
        Core.split(src, rgbaChannels)
        val alpha = rgbaChannels[3].clone()

        Log.d(tag, "🔍 알파 채널 분리 완료 - size=${alpha.size()}, type=${alpha.type()}")

        // RGB 영역만 추출 (BGR)
        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

        // 마스크: 알파 값이 30 이상인 픽셀만 필터 적용 대상으로 설정
        val mask = Mat()
        Imgproc.threshold(alpha, mask, 30.0, 255.0, Imgproc.THRESH_BINARY)

//        srcWeight	원본 이미지에 얼마나 가중치를 둘지 (값이 클수록 원래 밝기가 유지됨)
//        blurWeight	블러 이미지에 음수를 주면 차이를 강조해서 날카롭게 만듦
//        sigma	블러의 강도. 커질수록 더 부드럽고 큰 영역이 흐려짐 (디테일 덜 살고, 부드러워짐)

        val pixelCount = bgr.cols() * bgr.rows()
        val (srcWeight, blurWeight, sigma) = when {
            pixelCount >= 1920 * 1080 -> Triple(1.3, -0.3, 0.8)
            pixelCount >= 1280 * 720 -> Triple(2.0, -1.0, 0.9)
            else -> Triple(1.8, -0.8, 0.8)
        }

        Log.d(tag, "📏 필터 파라미터: srcWeight=$srcWeight, blurWeight=$blurWeight, sigma=$sigma")

        val blurred = Mat()
        Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), sigma)

        val sharpened = Mat()
        Core.addWeighted(bgr, srcWeight, blurred, blurWeight, 0.0, sharpened)

        // 톤 복구 (지나치게 어두운 부분 완화)
        Core.add(sharpened, Scalar(10.0, 10.0, 10.0), sharpened)

        // 마스크 영역만 유지 (알파 > 30인 부분만 sharpened, 나머지는 원본 bgr 유지)
        val safeBGR = Mat()
        bgr.copyTo(safeBGR)
        sharpened.copyTo(safeBGR, mask)


        val clipped = CommonUtils.clipRGBRange(safeBGR, min = 10.0, max = 240.0)

        val clippedRGBA = Mat()
        Imgproc.cvtColor(clipped, clippedRGBA, Imgproc.COLOR_BGR2RGBA)
        Core.insertChannel(alpha, clippedRGBA, 3)
        return clippedRGBA

    }

    fun applyUnsharpMask(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

        val blurred = Mat()
        Imgproc.GaussianBlur(bgr, blurred, Size(0.0, 0.0), 1.0) // sigma 조절

        val mask = Mat()
        Core.subtract(bgr, blurred, mask)

        val result = Mat()
        Core.addWeighted(bgr, 1.0, mask, 1.5, 0.0, result)

        val clipped = CommonUtils.clipRGBRange(result)
        val rgba = Mat()
        Imgproc.cvtColor(clipped, rgba, Imgproc.COLOR_BGR2RGBA)

        val output = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, output)
        return output
    }
}