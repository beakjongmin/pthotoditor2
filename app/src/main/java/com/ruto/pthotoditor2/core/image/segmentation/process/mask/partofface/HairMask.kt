package com.ruto.pthotoditor2.core.image.segmentation.process.mask.partofface

import android.graphics.Bitmap
import android.graphics.Color

object HairMask {

    fun extractHairMaskFromSegmentWithTone(bitmap: Bitmap, segmentMask: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val segAlpha = Color.alpha(segmentMask.getPixel(x, y))
                val color = bitmap.getPixel(x, y)

                if (segAlpha > 128 && isHairTone(color, x, y, height)) {
                    result.setPixel(x, y, Color.argb(255, 255, 255, 255))
                } else {
                    result.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        return result
    }
    private fun isHairTone(color: Int, x: Int, y: Int, imageHeight: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val brightness = (r + g + b) / 3
        val maxDiff = maxOf(r, g, b) - minOf(r, g, b)
        val isDark = brightness in 20..110 // 좀 더 어둡게
        val isNeutral = maxDiff < 30       // 채도 더 낮은 것만
        val isUpperPart = y in (imageHeight * 0.15).toInt()..(imageHeight * 0.5).toInt()

        return isDark && isNeutral && isUpperPart
    }
}