package com.ruto.pthotoditor2.core.image.ml.policy

import com.ruto.pthotoditor2.core.image.ml.model.ScalePreset

object UpscalePolicy {
    private const val inputTileSize = 128
    private const val outputTileSize = 512
    private const val scaleFactor = outputTileSize / inputTileSize
    private const val maxOutputPixels = 2048 * 2048

    fun calculate(originalWidth: Int, originalHeight: Int): ScalePreset {
        val originalPixels = originalWidth * originalHeight
        val estimatedUpscaledPixels = originalPixels * scaleFactor * scaleFactor

        val inputScale = if (estimatedUpscaledPixels > maxOutputPixels) {
            kotlin.math.sqrt(maxOutputPixels.toFloat() / originalPixels)
        } else {
            1.0f
        }

        val resizedW = (originalWidth * inputScale).toInt()
        val resizedH = (originalHeight * inputScale).toInt()

        return ScalePreset(
            inputScaleRatio = inputScale,
            outputScaleFactor = scaleFactor,
            finalOutputWidth = resizedW * scaleFactor,
            finalOutputHeight = resizedH * scaleFactor
        )
    }
}