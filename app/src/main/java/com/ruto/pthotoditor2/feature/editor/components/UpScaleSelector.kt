package com.ruto.pthotoditor2.feature.editor.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.ruto.pthotoditor2.feature.editor.model.UpScaletype
import com.ruto.pthotoditor2.feature.editor.viewmodel.EnhancementViewModel


@Composable
fun UpScaleSelector(
    context: Context,
    enhancementViewModel: EnhancementViewModel,
    original: Bitmap?,
    onResult: (Bitmap) -> Unit
) {
    val effects = remember { enhancementViewModel.getEffectTypes() }
    var selected by remember { mutableStateOf<UpScaletype?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "인물 효과 선택",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items(effects) { effect ->
                val isSelected = selected == effect
                val bgColor = if (isSelected) Color(0xFF4CAF50) else Color.DarkGray

                Box(
                    modifier = Modifier
                        .background(bgColor, shape = MaterialTheme.shapes.small)
                        .clickable {
                            selected = effect
                            enhancementViewModel.applyPortraitEffect(
                                context = context,
                                original = original,
                                type = effect,
                                onResult = onResult
                            )
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = effect.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
