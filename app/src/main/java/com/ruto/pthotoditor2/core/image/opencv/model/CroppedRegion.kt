package com.ruto.pthotoditor2.core.image.opencv.model

import android.graphics.Bitmap

data class CroppedRegion(
    val bitmap: Bitmap,
    val offsetX: Int,
    val offsetY: Int,
    val width: Int,
    val height: Int
)

data class CroppedHeadRegion(
    val bitmap: Bitmap,
    val offsetX: Int,
    val offsetY: Int,
    val width: Int,
    val height: Int
)