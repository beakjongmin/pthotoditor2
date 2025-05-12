package com.ruto.pthotoditor2.core.image.ml.debuggingfunction


import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat

object ColorLogger {

    private const val TAG = "ColorDebug"

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
}
