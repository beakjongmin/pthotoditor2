package com.ruto.pthotoditor2.commonui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ImageDisplay(
    originalBitmap: Bitmap?,
    editedBitmap: Bitmap?,
    onRequestImagePick: () -> Unit,
    changeImage: () -> Unit,
    modifier: Modifier
) {
    var showOriginal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable {
                if (editedBitmap == null) {
                    onRequestImagePick()
                } else {
                    changeImage()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 선택된 이미지 비트맵 결정 (null-safe)
        val displayBitmap = if (showOriginal) {
            originalBitmap ?: editedBitmap
        } else {
            editedBitmap ?: originalBitmap
        }

        displayBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Icon(
                imageVector = if (showOriginal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (showOriginal) "원본 숨기기" else "원본 보기",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                showOriginal = !showOriginal
                                tryAwaitRelease()
                                showOriginal = !showOriginal
                            }
                        )
                    },
                tint = Color.White
            )
        } ?: run {
            Text(
                text = "이미지를 선택해주세요.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
