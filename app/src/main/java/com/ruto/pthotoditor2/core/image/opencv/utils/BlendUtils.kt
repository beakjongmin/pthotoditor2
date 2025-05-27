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
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) //결과 = 기존 maskedOverlay 이미지 * alphaMask의 알파값 (0.0 ~ 1.0)
        }

        val maskedOverlay = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        Canvas(maskedOverlay).apply {
            drawBitmap(overlay, 0f, 0f, null)
            drawBitmap(alphaMask, 0f, 0f, paint)
        }

        // 최종 base 위에 합성
        Canvas(base).drawBitmap(maskedOverlay, 0f, 0f, null)
    }
    fun blendopencv(base: Bitmap, overlay: Bitmap, alphaMask: Bitmap) {
        val width = base.width
        val height = base.height

        require(overlay.width == width && overlay.height == height) { "overlay 크기 불일치" }
        require(alphaMask.width == width && alphaMask.height == height) { "alphaMask 크기 불일치" }

        val basePixels = IntArray(width * height)
        val overlayPixels = IntArray(width * height)
        val alphaPixels = IntArray(width * height)

        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)
        alphaMask.getPixels(alphaPixels, 0, width, 0, 0, width, height)

        for (i in basePixels.indices) {
            val baseColor = basePixels[i]
            val overlayColor = overlayPixels[i]
            val alphaColor = alphaPixels[i]

            val alpha = ((alphaColor shr 24) and 0xFF) / 255f // soft mask 알파값 추출 (0.0 ~ 1.0)

            // 픽셀이 완전히 투명하면 패스
            if (alpha == 0f) continue
            if (alpha == 1f) {
                basePixels[i] = overlayColor
                continue
            }

            val r1 = (baseColor shr 16) and 0xFF
            val g1 = (baseColor shr 8) and 0xFF
            val b1 = baseColor and 0xFF

            val r2 = (overlayColor shr 16) and 0xFF
            val g2 = (overlayColor shr 8) and 0xFF
            val b2 = overlayColor and 0xFF

            val r = (r1 * (1 - alpha) + r2 * alpha).toInt().coerceIn(0, 255)
            val g = (g1 * (1 - alpha) + g2 * alpha).toInt().coerceIn(0, 255)
            val b = (b1 * (1 - alpha) + b2 * alpha).toInt().coerceIn(0, 255)

            basePixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        base.setPixels(basePixels, 0, width, 0, 0, width, height)
    }

    fun blendCroppedRegionBack(
        original: Bitmap,
        ProcessedPerson: Bitmap,
        mask: Bitmap,
        offsetX: Int,
        offsetY: Int
    ): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)

        val resizedMask = Bitmap.createScaledBitmap(mask, ProcessedPerson.width, ProcessedPerson.height, true)
        val personPixels = IntArray(ProcessedPerson.width * ProcessedPerson.height)
        val maskPixels = IntArray(ProcessedPerson.width * ProcessedPerson.height)

        ProcessedPerson.getPixels(personPixels, 0, ProcessedPerson.width, 0, 0, ProcessedPerson.width, ProcessedPerson.height)
        resizedMask.getPixels(maskPixels, 0, ProcessedPerson.width, 0, 0, ProcessedPerson.width, ProcessedPerson.height)

        // ✅ 블렌딩에 실제 사용될 마스크 픽셀 중 알파가 의미 있는 픽셀 수 계산
        var changedPixels = 0
        for (i in maskPixels.indices) {
            val alpha = (maskPixels[i] shr 24) and 0xFF
            if (alpha > 30) {
                changedPixels++
            }
        }
        Log.d("BlendDebug", "💡 블렌딩 영역 알파 > 30 픽셀 수: $changedPixels / ${maskPixels.size}")


        for (y in 0 until ProcessedPerson.height) {
            for (x in 0 until ProcessedPerson.width) {
                val i = y * ProcessedPerson.width + x
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