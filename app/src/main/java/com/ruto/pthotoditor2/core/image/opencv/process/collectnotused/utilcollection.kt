package com.ruto.pthotoditor2.core.image.opencv.process.collectnotused

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils
import com.ruto.pthotoditor2.core.image.opencv.model.CroppedHeadRegion
import com.ruto.pthotoditor2.core.image.opencv.model.CroppedRegion
import java.nio.ByteBuffer

object utilcollection {

    fun extractMaskedRegionCropped(bitmap: Bitmap, mask: Bitmap): CroppedRegion {
        val width = bitmap.width
        val height = bitmap.height

        val origPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        var left = width
        var right = 0
        var top = height
        var bottom = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val alpha = (maskPixels[i] shr 24) and 0xFF
                if (alpha > 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        if (right <= left || bottom <= top) {
            return CroppedRegion(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), 0, 0, 1, 1)
        }

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
        return CroppedRegion(cropped, left, top, right - left + 1, bottom - top + 1)
    }

    fun extractHeadRegionCropped(
        bitmap: Bitmap,
        maskBitmap: Bitmap,
        top: Int,
        bottom: Int
    ): CroppedHeadRegion {
        val width = bitmap.width
        val height = bitmap.height

        val croppedHeight = (bottom - top).coerceAtLeast(1)

        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, top, width, croppedHeight)
        val croppedMask = Bitmap.createBitmap(maskBitmap, 0, top, width, croppedHeight)

        val personOnly = OpenCvUtils.extractMaskedRegion(croppedBitmap, croppedMask)
        return CroppedHeadRegion(
            bitmap = personOnly,
            offsetX = 0,
            offsetY = top,
            width = width,
            height = croppedHeight
        )
    }

    /**
     * 원본 이미지와 효과 이미지, 그리고 마스크를 이용해 영역별로 합성합니다.
     * @param src 원본 Bitmap
     * @param effect 효과 적용된 Bitmap
     * @param mask 인물 영역 마스크 (흑백 Bitmap)
     */
    fun blendEffectOnPersonOnly(
        original: Bitmap,
        effect: Bitmap,
        mask: Bitmap,
        maskThreshold: Int = 128
    ): Bitmap {
        val TAG = "BlendEffectDebug"

        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val originalPixels = IntArray(width * height)
        val effectPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        original.getPixels(originalPixels, 0, width, 0, 0, width, height)
        effect.getPixels(effectPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        for (i in originalPixels.indices) {
            if (i % (width * height / 10) == 0) { // 대략 10개만 출력
                Log.d(TAG, "original pixel[$i]: R=${Color.red(originalPixels[i])}, G=${Color.green(originalPixels[i])}, B=${Color.blue(originalPixels[i])}")
                Log.d(TAG, "effect pixel[$i]:   R=${Color.red(effectPixels[i])}, G=${Color.green(effectPixels[i])}, B=${Color.blue(effectPixels[i])}")
            }

            val alpha = (maskPixels[i] shr 24) and 0xFF
            result.setPixel(i % width, i / width, if (alpha >= maskThreshold) effectPixels[i] else originalPixels[i])
        }

        return result
    }

    /**
     * 인물(사람) 영역에만 효과 이미지를 적용하고, 나머지 영역은 원본을 유지함.
     * blendEffectOnPersonOnly()보다 품질은 좋지만 성능은 조금 더 무거움.
     */
    fun blendEffectOnPersonOnlySoft(
        original: Bitmap,
        effect: Bitmap,
        mask: Bitmap
    ): Bitmap {
        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val origPixels = IntArray(width * height)
        val effectPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        effect.getPixels(effectPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        //0 부터 픽샐 max 전까지
        for (i in origPixels.indices) {

            val alpha = (maskPixels[i] shr 24) and 0xFF

            val t = alpha / 255f // 0.0 ~ 1.0

            val orig = origPixels[i]
            val eff = effectPixels[i]

            val r = ((1 - t) * Color.red(orig) + t * Color.red(eff)).toInt().coerceIn(0, 255)
            val g = ((1 - t) * Color.green(orig) + t * Color.green(eff)).toInt().coerceIn(0, 255)
            val b = ((1 - t) * Color.blue(orig) + t * Color.blue(eff)).toInt().coerceIn(0, 255)


            result.setPixel(i % width, i / width, Color.argb(255, r, g, b))
        }

        return result
    }
    fun toBinaryMask(mask: SegmentationMask, width: Int, height: Int, threshold: Float = 0.6f): Bitmap {
        val buffer = mask.buffer
        Log.d("Debug", "Before rewind: position=${buffer.position()}")
        buffer.rewind()
        Log.d("Debug", "After rewind: position=${buffer.position()}")

        val alphaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val byteBuffer = ByteBuffer.allocate(width * height)

        for (i in 0 until width * height) {
            val confidence = buffer.float
            byteBuffer.put(if (confidence > threshold) 255.toByte() else 0.toByte())
        }
        //배경은 0 , 사람은 255 로 표현
        byteBuffer.rewind()
        alphaBitmap.copyPixelsFromBuffer(byteBuffer)

        return alphaBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun maskBitmap(src: Bitmap, mask: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // 1. 원본 먼저 그림
        canvas.drawBitmap(src, 0f, 0f, paint)

        // 2. 마스크를 알파로 적용
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        return result
    }
    fun toHardAlphaMask(mask: SegmentationMask, width: Int, height: Int, threshold: Float = 0.75f): Bitmap {
        val buffer = mask.buffer
        buffer.rewind()
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val confidence = buffer.float // 0.0 ~ 1.0


            val alpha = if (confidence > threshold) 255 else 0
            pixels[i] = (alpha shl 24) or 0xFFFFFF  // White with alpha
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}