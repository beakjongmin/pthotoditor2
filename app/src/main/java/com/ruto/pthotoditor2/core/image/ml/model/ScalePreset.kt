package com.ruto.pthotoditor2.core.image.ml.model

data class ScalePreset(
    val inputScaleRatio: Float,
    val outputScaleFactor: Int,
    val finalOutputWidth: Int,
    val finalOutputHeight: Int
)
