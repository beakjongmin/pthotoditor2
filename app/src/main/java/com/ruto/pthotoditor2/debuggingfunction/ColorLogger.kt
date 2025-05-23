package com.ruto.pthotoditor2.debuggingfunction


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * 디버깅 로직
 */
object ColorLogger {

    private const val TAG = "ColorDebug"

    fun logAlphaHistogram(mask: Bitmap,maskname: String) {
        val width = mask.width
        val height = mask.height
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)

        // 알파 값 분포를 구간별로 나눠서 카운팅
        val histogram = IntArray(7) // 0, 1~50, 51~100, 101~150, 151~200, 201~255 201~254 255

        for (pixel in pixels) {
            val alpha = (pixel shr 24) and 0xFF
            when (alpha) {
                0 -> histogram[0]++
                in 1..50 -> histogram[1]++
                in 51..100 -> histogram[2]++
                in 101..150 -> histogram[3]++
                in 151..200 -> histogram[4]++
                in 201..254 -> histogram[5]++
                in 255..255 -> histogram[6]++
            }
        }

        Log.d("AlphaHistogram", "🧪 알파 히스토그램 ${maskname}")
        Log.d("AlphaHistogram", "alpha = 0        : ${histogram[0]}")
        Log.d("AlphaHistogram", "alpha 1~50       : ${histogram[1]}")
        Log.d("AlphaHistogram", "alpha 51~100     : ${histogram[2]}")
        Log.d("AlphaHistogram", "alpha 101~150    : ${histogram[3]}")
        Log.d("AlphaHistogram", "alpha 151~200    : ${histogram[4]}")
        Log.d("AlphaHistogram", "alpha 201~254    : ${histogram[5]}")
        Log.d("AlphaHistogram", "alpha 255    : ${histogram[6]}")
    }

    fun logAlphaStats(tag: String, bitmap: Bitmap) {
        var minAlpha = 255
        var maxAlpha = 0
        var nonZeroCount = 0
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val alpha = (pixel shr 24) and 0xff
            if (alpha > 0) nonZeroCount++
            if (alpha < minAlpha) minAlpha = alpha
            if (alpha > maxAlpha) maxAlpha = alpha
        }

        Log.d(tag, "🧪 Alpha Stats: min=$minAlpha, max=$maxAlpha, nonZeroPixels=$nonZeroCount / total=${pixels.size}")
    }
    /**
     * 전체 이미지의 평균 BGR + A 값 로그 출력
     */
    fun logMean(tag: String, bitmap: Bitmap) {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val mean = Core.mean(mat)
        val formatted = String.format(
            "B=%.1f, G=%.1f, R=%.1f, A=%.1f",
            mean.`val`[0], mean.`val`[1], mean.`val`[2], mean.`val`.getOrElse(3) { -1.0 }
        )
        Log.d(TAG, "[$tag] 평균 색상: $formatted")
    }

    /**
     * 특정 좌표 (x, y)의 BGR + A 픽셀 값 출력
     */
    fun logPixel(tag: String, bitmap: Bitmap, x: Int, y: Int) {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val pixel = mat.get(y, x)
        val b = pixel?.getOrNull(0) ?: -1.0
        val g = pixel?.getOrNull(1) ?: -1.0
        val r = pixel?.getOrNull(2) ?: -1.0
        val a = pixel?.getOrNull(3) ?: -1.0
        Log.d(TAG, "[$tag] 픽셀 ($x,$y): B=$b, G=$g, R=$r, A=$a")
    }

    fun debugDrawMaskOverlay(mask: Bitmap, base: Bitmap): Bitmap {
        val canvas = Canvas(base)
        val paint = Paint().apply {
            color = Color.RED
            alpha = 120
        }
        canvas.drawBitmap(mask, 0f, 0f, paint)
        return base
    }

    fun visualizeAlphaMask(mask: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                val alpha = Color.alpha(mask.getPixel(x, y))
                // alpha 값을 RGB로 뿌려서 흑백 마스크로 표현
                val color = Color.argb(255, alpha, alpha, alpha)
                result.setPixel(x, y, color)
            }
        }
        return result
    }
    fun logLowAlphaStats(bitmap: Bitmap, threshold: Int = 30) {
        var lowAlphaCount = 0
        var totalCount = 0
        var maxAlpha = 0
        var minAlpha = 255

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val alpha = Color.alpha(bitmap.getPixel(x, y))
                if (alpha in 1..threshold) {
                    lowAlphaCount++
                }
                if (alpha > 0) totalCount++
                maxAlpha = maxOf(maxAlpha, alpha)
                minAlpha = minOf(minAlpha, alpha)
            }
        }

        val percentage = if (totalCount > 0) lowAlphaCount * 100.0 / totalCount else 0.0

        Log.d("AlphaDebug", "🔍 경계 알파값 통계 (α ≤ $threshold): $lowAlphaCount / $totalCount (${String.format("%.2f", percentage)}%)")
        Log.d("AlphaDebug", "🧪 알파 범위: min=$minAlpha, max=$maxAlpha")
    }
    fun removeLowAlpha(mask: Bitmap, minAlpha: Int = 30): Bitmap {
        val result = mask.copy(Bitmap.Config.ARGB_8888, true)
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val pixel = result.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha <= minAlpha) {
                    result.setPixel(x, y, Color.argb(0, 0, 0, 0))
                }
            }
        }
        return result
    }

    fun dilateMask(mask: Bitmap, radius: Double): Bitmap {
        val src = Mat()
        val dst = Mat()

        // Bitmap → Mat 변환
        Utils.bitmapToMat(mask, src)

        // radius → kernel size (홀수)
        val kSize = ((radius * 2).toInt()).let { if (it % 2 == 0) it + 1 else it }

        // 커널 생성
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(kSize.toDouble(), kSize.toDouble()))

        // dilation
        Imgproc.dilate(src, dst, kernel)

        // Mat → Bitmap 변환
        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)

        // 메모리 해제
        src.release()
        dst.release()
        kernel.release()

        return result
    }


    fun applyGaussianBlur(bitmap: Bitmap, radius: Double): Bitmap {
        val src = Mat()
        val dst = Mat()

        // Bitmap → Mat 변환
        Utils.bitmapToMat(bitmap, src)

        // radius → 홀수 커널 사이즈로 보정
        val kSize = ((radius * 2).toInt()).let { if (it % 2 == 0) it + 1 else it }

        // Gaussian Blur 적용
        Imgproc.GaussianBlur(src, dst, Size(kSize.toDouble(), kSize.toDouble()), 0.0)

        // Mat → Bitmap 변환
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)

        // 메모리 해제
        src.release()
        dst.release()

        return result
    }

    fun gammaCompressAlpha(bitmap: Bitmap, gamma: Double): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val color = result.getPixel(x, y)
                val alpha = Color.alpha(color)
                val compressed = (255.0 * Math.pow(alpha / 255.0, gamma)).toInt().coerceIn(0, 255)
                result.setPixel(x, y, Color.argb(compressed, 255, 255, 255))
            }
        }
        return result
    }


    fun matchToneByMean(reference: Bitmap, target: Bitmap): Bitmap {
        val refMat = Mat()
        val tgtMat = Mat()
        Utils.bitmapToMat(reference, refMat)
        Utils.bitmapToMat(target, tgtMat)

        val refMean = Core.mean(refMat)
        val tgtMean = Core.mean(tgtMat)

        val scaleB = refMean.`val`[0] / tgtMean.`val`[0]
        val scaleG = refMean.`val`[1] / tgtMean.`val`[1]
        val scaleR = refMean.`val`[2] / tgtMean.`val`[2]

        val resultMat = Mat()
        val channels = ArrayList<Mat>()
        Core.split(tgtMat, channels)

        channels[0].convertTo(channels[0], -1, scaleB)
        channels[1].convertTo(channels[1], -1, scaleG)
        channels[2].convertTo(channels[2], -1, scaleR)

        Core.merge(channels, resultMat)
        val result = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, result)

        // 메모리 해제
        refMat.release()
        tgtMat.release()
        resultMat.release()
        channels.forEach { it.release() }

        return result
    }

}
