package com.ruto.pthotoditor2.feature.editor.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruto.pthotoditor2.feature.editor.model.EditorMode

@Composable
fun BottomSelector(
    currentMode: EditorMode,
    onModeSelected: (EditorMode) -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "필터",
            color = if (currentMode == EditorMode.FILTER) selectedColor else unselectedColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable { onModeSelected(EditorMode.FILTER) }
        )
        Text(
            text = "DSLR",
            color = if (currentMode == EditorMode.DSLR) selectedColor else unselectedColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable { onModeSelected(EditorMode.DSLR) }
        )
        Text(
            text = "고화질 변환",
            color = if (currentMode == EditorMode.UPSCALE) selectedColor else unselectedColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable { onModeSelected(EditorMode.UPSCALE) }
        )
    }
}
