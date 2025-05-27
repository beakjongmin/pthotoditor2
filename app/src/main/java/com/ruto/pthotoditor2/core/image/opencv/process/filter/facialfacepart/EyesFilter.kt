package com.ruto.pthotoditor2.core.image.opencv.process.filter.facialfacepart

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object EyesFilter {

    fun applySnowStyleSharp(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // 1. Soft한 Unsharp Mask
        val blurred = Mat()
        Imgproc.GaussianBlur(mat, blurred, Size(0.0, 0.0), 2.5) // sigma 줄임

        val sharpened = Mat()
        Core.addWeighted(mat, 1.8, blurred, -0.8, 0.0, sharpened) // // ✅ 자연스러운 눈 디테일 강조

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(sharpened, result)
        return result
    }

    // SOFT: Bilateral + 약간의 밝기 감소로 눈 주변을 부드럽게
    fun applySoftEyes(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val softened = Mat()
        Imgproc.bilateralFilter(mat, softened, 9, 75.0, 75.0) // bilateral soft blur

        // 밝기 약간 낮추기
        Core.add(softened, Scalar(-10.0, -10.0, -10.0), softened)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(softened, result)
        return result
    }

    // CLEAR: 대비와 채도 향상 → 눈을 또렷하게
    fun applyClearEyes(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val contrasted = Mat()
        mat.convertTo(contrasted, -1, 1.15, 2.0) // ✅ 더 자연스러운 톤

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(contrasted, result)
        return result
    }

    // NATURAL: 감마 보정 기반의 자연스러운 밝기 조정
    fun applyNaturalEyes(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // 감마 LUT 생성
        val gamma = 1.05
        val lut = Mat(1, 256, org.opencv.core.CvType.CV_8UC1)
        val gammaLUT = ByteArray(256) { i ->
            ((255.0 * Math.pow(i / 255.0, 1.0 / gamma)).toInt()).coerceIn(0, 255).toByte()
        }
        lut.put(0, 0, gammaLUT)

        val resultMat = Mat()
        Core.LUT(mat, lut, resultMat)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, result)
        return result
    }



}