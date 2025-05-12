package com.ruto.pthotoditor2.core.image.opencv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import com.ruto.pthotoditor2.core.image.ml.debuggingfunction.ColorLogger
import com.ruto.pthotoditor2.core.image.opencv.process.filter.ClearEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.NaturalEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.SharpEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.SoftEffectProcessor

object OpenCvFilters {
    //context는 디버깅용임
    fun applyFilterWithMask(
        context: Context, // 디버깅용
        source: Bitmap,
        mask: Bitmap,
        filter: (Bitmap) -> Bitmap,
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 1. 인물만 추출 (마스크의 알파를 그대로 사용)
        val personOnly = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        personOnly.eraseColor(Color.TRANSPARENT)


        Canvas(personOnly).apply {
            drawBitmap(source, 0f, 0f, null)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            drawBitmap(mask, 0f, 0f, paint)
        }
        ColorLogger.logMean("인물추출후",personOnly)
        // 2. 필터 적용
        val filtered = filter(personOnly)

        Log.d("ApplyFilter", "🎨 필터 적용 완료")
        ColorLogger.logMean("필터 적용 후,인물추출와 비교해야할것. ",filtered)
        ColorLogger.logPixel("필터 적용전 중앙", personOnly, personOnly.width / 2, personOnly.height / 2)
        ColorLogger.logPixel("필터 적용후 중앙", filtered, filtered.width / 2, filtered.height / 2)
        // 3. 결과 이미지 위에 부드럽게 합성
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)
        resultCanvas.drawBitmap(source, 0f, 0f, null)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER) // Soft mask가 적용된 filtered를 자연스럽게 blend
        }

        resultCanvas.drawBitmap(filtered, 0f, 0f, paint)
        Log.d("ApplyFilter", "✅ 최종 결과 비트맵 생성 완료")

        return result
    }

    fun applySharp(bitmap: Bitmap): Bitmap = SharpEffectProcessor.apply(bitmap)

    fun applySoft(bitmap: Bitmap): Bitmap = SoftEffectProcessor.apply(bitmap)

    fun applyClear(bitmap: Bitmap): Bitmap = ClearEffectProcessor.apply(bitmap)

    fun applyNatural(bitmap: Bitmap): Bitmap = NaturalEffectProcessor.apply(bitmap)



}
