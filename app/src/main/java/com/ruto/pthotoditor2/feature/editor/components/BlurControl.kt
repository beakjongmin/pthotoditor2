package com.ruto.pthotoditor2.feature.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BlurControl(
    blurValue: Float,
    onBlurValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..150f,
    steps: Int = 65 // 🔥 더 정밀하게 조절 (ex: 0.1 단위)
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        if (position != null && event.changes.first().pressed) {
                            val ratio = (position.x / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + (valueRange.endInclusive - valueRange.start) * ratio
                            coroutineScope.launch {
                                onBlurValueChange(newValue)
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickCount = steps + 1
            val tickSpacing = size.width / tickCount

            // 눈금 표시
            for (i in 0..steps) {
                val x = i * tickSpacing
                drawLine(
                    color = Color.White,
                    start = Offset(x, size.height / 2 - 8),
                    end = Offset(x, size.height / 2 + 8),
                    strokeWidth = 1.5f
                )
            }

            // Thumb
            val thumbX = (blurValue - valueRange.start) / (valueRange.endInclusive - valueRange.start) * size.width
            val thumbHeight = size.height * 0.6f
            val centerY = size.height / 2
            drawLine(
                color = Color.Yellow,
                start = Offset(thumbX, centerY - thumbHeight / 2),
                end = Offset(thumbX, centerY + thumbHeight / 2),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlurControlPreview() {
    var blurValue by remember { mutableStateOf(50f) }
    BlurControl(
        blurValue = blurValue,
        onBlurValueChange = { blurValue = it }
    )
}
