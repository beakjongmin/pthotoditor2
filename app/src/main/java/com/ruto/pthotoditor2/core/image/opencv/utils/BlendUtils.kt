package com.ruto.pthotoditor2.core.image.opencv.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log

object BlendUtils {


    // Masked overlay 준비
    /**
     * overlay를 maskedOverlay 위에 그대로 그림
     * 예: 눈 필터링 된 sharp 이미지를 덮어씀.
     * RGB 값이 maskedOverlay에 먼저 들어감.
     * PorterDuffXfermode는 내부적으로 blending 연산할 때 0~1.0 로 "정규화(normalized)" 해서 계산.
     * → alphaMask 가 0인 부분은 overlay가 사라지고,
     *
     * → alphaMask 가 255인 부분은 overlay가 그대로 남고,
     *
     * → 중간 값(Feathered 부분)은 부드럽게 blending 됨.
     */

    fun blend(base: Bitmap, overlay: Bitmap, alphaMask: Bitmap) {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode =
                PorterDuffXfermode(PorterDuff.Mode.DST_IN) //결과 = 기존 maskedOverlay 이미지 * alphaMask의 알파값 (0.0 ~ 1.0)
        }

        val maskedOverlay = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        Canvas(maskedOverlay).apply {
            drawBitmap(overlay, 0f, 0f, null)
            drawBitmap(alphaMask, 0f, 0f, paint)
        }

        // 최종 base 위에 합성
        Canvas(base).drawBitmap(maskedOverlay, 0f, 0f, null)
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

}