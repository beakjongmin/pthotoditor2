package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object MasktoAlpha {

    fun toAlphaDrawableMask(originalAlpha: Bitmap): Bitmap {
        val argb = Bitmap.createBitmap(originalAlpha.width, originalAlpha.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(argb)
        val paint = Paint().apply { color = Color.WHITE } // 흰색으로 불투명 처리
        canvas.drawBitmap(originalAlpha, 0f, 0f, paint)
        return argb
    }
}