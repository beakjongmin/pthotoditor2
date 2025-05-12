package com.ruto.pthotoditor2.core.image.segmentation.process.dslr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

object BokehProcessor {

    private const val TAG = "보큰 프로세서"

    suspend fun applyBokeh(
        context: Context,
        original: Bitmap,
        blurRadius: Float = 25f,
        threshold: Float = 0.32f
    ): Bitmap = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting applyBokeh...")

        val bitmap = original.ensureSoftwareConfig()
        val mask = SelfieSegmentor.segment(bitmap)

        val width = bitmap.width
        val height = bitmap.height

        // 마스크를 floatBuffer로 변환
        val floatBuffer = mask.buffer.asFloatBuffer()
        floatBuffer.rewind()

        // ✅ 마스크를 부드럽게 처리
        val maskBitmap = segmentationMaskToBitmap(mask)
        val blurredMaskBitmap = maskBitmap.blur(context, radius = 15f) ?: maskBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val confidenceArray = bitmapToFloatArray(blurredMaskBitmap)

        val fixedBlurRadius = blurRadius.coerceIn(1f, 150f)
        val kernelSize = (fixedBlurRadius * 2).toInt() or 1
        Log.d(TAG, "변환된 커널 사이즈는 $kernelSize")

        val blurredBitmap = bitmap.blur(context, kernelSize.toFloat()) ?: bitmap

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width)

        for (y in 0 until height) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            val blurredRow = IntArray(width)
            blurredBitmap.getPixels(blurredRow, 0, width, 0, y, width, 1)

            for (x in 0 until width) {
                val idx = y * width + x
                val confidence = confidenceArray[idx]

                pixels[x] = if (confidence < threshold) blurredRow[x] else pixels[x]
            }

            result.setPixels(pixels, 0, width, 0, y, width, 1)
        }

        Log.d(TAG, "Bokeh processing complete.")
        return@withContext result
    }

    // SegmentationMask를 Bitmap으로 변환
    private fun segmentationMaskToBitmap(mask: SegmentationMask): Bitmap {
        val width = mask.width
        val height = mask.height
        val buffer = mask.buffer
        buffer.rewind()

        // 먼저 ALPHA_8로 생성
        val alphaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val byteBuffer = ByteBuffer.allocate(width * height)

        for (i in 0 until width * height) {
            val confidence = buffer.float
            byteBuffer.put((confidence * 255).toInt().toByte())
        }

        byteBuffer.rewind()
        alphaBitmap.copyPixelsFromBuffer(byteBuffer)

        // ✅ 여기 추가: ALPHA_8 → ARGB_8888 변환
        return alphaBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    // 부드럽게 처리된 Bitmap을 float 배열로 변환
    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height

        val intPixels = IntArray(width * height)
        bitmap.getPixels(intPixels, 0, width, 0, 0, width, height)

        return FloatArray(width * height) { idx ->
            val pixel = intPixels[idx]
            val alpha = (pixel shr 24) and 0xFF
            alpha / 255f
        }
    }
}
