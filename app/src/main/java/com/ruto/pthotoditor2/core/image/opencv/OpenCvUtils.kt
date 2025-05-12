package com.ruto.pthotoditor2.core.image.opencv

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mlkit.vision.segmentation.SegmentationMask
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar

object OpenCvUtils {


    /**
     * mask 에 값을 바탕으로 인물영역을 분리해주는 유틸함수
     */
    fun extractMaskedRegion(bitmap: Bitmap, mask: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val origPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val output = IntArray(width * height)

        for (i in origPixels.indices) {
            val alpha = (maskPixels[i] shr 24) and 0xFF
            if (alpha > 0) {
                output[i] = origPixels[i]
            } else {
                output[i] = Color.TRANSPARENT
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }



    fun blendCroppedRegionBack(
        original: Bitmap,
        upscaledPerson: Bitmap,
        mask: Bitmap,
        offsetX: Int,
        offsetY: Int
    ): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)

        val resizedMask = Bitmap.createScaledBitmap(mask, upscaledPerson.width, upscaledPerson.height, true)
        val personPixels = IntArray(upscaledPerson.width * upscaledPerson.height)
        val maskPixels = IntArray(upscaledPerson.width * upscaledPerson.height)

        upscaledPerson.getPixels(personPixels, 0, upscaledPerson.width, 0, 0, upscaledPerson.width, upscaledPerson.height)
        resizedMask.getPixels(maskPixels, 0, upscaledPerson.width, 0, 0, upscaledPerson.width, upscaledPerson.height)

        // ✅ 블렌딩에 실제 사용될 마스크 픽셀 중 알파가 의미 있는 픽셀 수 계산
        var changedPixels = 0
        for (i in maskPixels.indices) {
            val alpha = (maskPixels[i] shr 24) and 0xFF
            if (alpha > 30) {
                changedPixels++
            }
        }
        Log.d("BlendDebug", "💡 블렌딩 영역 알파 > 30 픽셀 수: $changedPixels / ${maskPixels.size}")


        for (y in 0 until upscaledPerson.height) {
            for (x in 0 until upscaledPerson.width) {
                val i = y * upscaledPerson.width + x
                val alpha = (maskPixels[i] shr 24) and 0xFF
                if (alpha > 0) {
                    val pixel = personPixels[i]
                    val targetX = offsetX + x
                    val targetY = offsetY + y

                    if (targetX in 0 until result.width && targetY in 0 until result.height) {
                        result.setPixel(targetX, targetY, pixel)
                    }
                }
            }
        }

        return result
    }
    /**
     * SegmentationMask를 이진 마스크 Bitmap으로 변환
     * toBinaryMask(...) → Confidence 기반 이진 마스크 (흑/백) 생성
     * ML Kit의 SegmentationMask는 알파 기반
     *
     *
     * @param threshold 인물 판단 기준 confidence (0.0 ~ 1.0)
     */


    fun toSoftAlphaMask(mask: SegmentationMask, width: Int, height: Int): Bitmap {
        val buffer = mask.buffer
        buffer.rewind()
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val confidence = buffer.float
            val alpha = (confidence * 255).toInt().coerceIn(0, 255)
            pixels[i] = (alpha shl 24) or 0xFFFFFF
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun matchToneByMean(reference: Bitmap, target: Bitmap): Bitmap {
        val srcMat = Mat()
        val refMat = Mat()
        Utils.bitmapToMat(target, srcMat)
        Utils.bitmapToMat(reference, refMat)

        val meanRef = Core.mean(refMat)
        val meanSrc = Core.mean(srcMat)

        val diff = Scalar(
            meanRef.`val`[0] - meanSrc.`val`[0],
            meanRef.`val`[1] - meanSrc.`val`[1],
            meanRef.`val`[2] - meanSrc.`val`[2]
        )

        Core.add(srcMat, diff, srcMat)

        val result = Bitmap.createBitmap(srcMat.cols(), srcMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(srcMat, result)
        return result
    }






}
