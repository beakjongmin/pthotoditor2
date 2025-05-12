package com.ruto.pthotoditor2.feature.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ThresholdControl(
    thresholdValue: Float,
    onThresholdChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 20
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
                                onThresholdChange(newValue)
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
                    start = Offset(x, size.height / 2 - 6),
                    end = Offset(x, size.height / 2 + 6),
                    strokeWidth = 1.2f
                )
            }

            // Thumb
            val thumbX = (thresholdValue - valueRange.start) / (valueRange.endInclusive - valueRange.start) * size.width
            val thumbHeight = size.height * 0.5f
            val centerY = size.height / 2
            drawLine(
                color = Color.Cyan,
                start = Offset(thumbX, centerY - thumbHeight / 2),
                end = Offset(thumbX, centerY + thumbHeight / 2),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ThresholdControlPreview() {
    var thresholdValue by remember { mutableStateOf(0.3f) }
    ThresholdControl(
        thresholdValue = thresholdValue,
        onThresholdChange = { thresholdValue = it }
    )
}
