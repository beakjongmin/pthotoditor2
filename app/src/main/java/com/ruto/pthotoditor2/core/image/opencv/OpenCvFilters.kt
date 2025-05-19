package com.ruto.pthotoditor2.core.image.opencv

import android.graphics.Bitmap
import com.ruto.pthotoditor2.core.image.opencv.process.filter.ClearEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.NaturalEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.SharpEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.SoftEffectProcessor
import com.ruto.pthotoditor2.core.image.opencv.process.filter.facialfacepart.EyesFilter
import com.ruto.pthotoditor2.feature.editor.model.UpScaletype

object OpenCvFilters {
    fun applyFilter(source: Bitmap, type: UpScaletype): Bitmap {
        return when (type) {
            UpScaletype.SHARP -> SharpEffectProcessor.apply(source)
            UpScaletype.SOFT -> SoftEffectProcessor.apply(source)
            UpScaletype.CLEAR -> ClearEffectProcessor.apply(source)
            UpScaletype.NATURAL -> NaturalEffectProcessor.apply(source)
            UpScaletype.UPSCALEONLY -> source
        }

    }

    fun applyEyesFilter(source: Bitmap, type: UpScaletype): Bitmap {
        return when (type) {
            UpScaletype.SHARP -> EyesFilter.applySnowStyleSharp(source)
            UpScaletype.SOFT -> EyesFilter.applySoftEyes(source)
            UpScaletype.CLEAR -> EyesFilter.applyClearEyes(source)
            UpScaletype.NATURAL -> EyesFilter.applyNaturalEyes(source)
            UpScaletype.UPSCALEONLY -> source // 처리 없음
        }
    }


//    fun applySharp(bitmap: Bitmap): Bitmap = SharpEffectProcessor.apply(bitmap)
//
//    fun applySoft(bitmap: Bitmap): Bitmap = SoftEffectProcessor.apply(bitmap)
//
//    fun applyClear(bitmap: Bitmap): Bitmap = ClearEffectProcessor.apply(bitmap)
//
//    fun applyNatural(bitmap: Bitmap): Bitmap = NaturalEffectProcessor.apply(bitmap)
//
//    //눈부분
//    fun applySharpEyes(bitmap: Bitmap): Bitmap = EyesFilter.applySnowStyleSharp(bitmap)

}
