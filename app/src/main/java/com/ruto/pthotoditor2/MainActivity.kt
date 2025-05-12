package com.ruto.pthotoditor2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ruto.pthotoditor2.feature.commonui.ExitConfirmationDialog
import com.ruto.pthotoditor2.feature.editor.components.EditorScreen
import com.ruto.pthotoditor2.feature.editor.viewmodel.EditorStateViewModel
import com.ruto.pthotoditor2.feature.filter.viewmodel.FilterViewModel
import com.ruto.pthotoditor2.feature.gallery.SaveImageUseCase
import com.ruto.pthotoditor2.feature.gallery.rememberGalleryPicker
import com.ruto.pthotoditor2.ui.theme.Pthotoditor2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val editorStateViewModel: EditorStateViewModel by viewModels()
    private val filterViewModel: FilterViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showExitDialog by remember { mutableStateOf(false) }

            // 백버튼 핸들링
            BackHandler(enabled = true) {
                showExitDialog = true
            }

            if (showExitDialog) {
                ExitConfirmationDialog(
                    onDismiss = { showExitDialog = false },
                    onConfirmExit = { finish() }
                )
            }

            val context = LocalContext.current
            val galleryPicker = rememberGalleryPicker(context, editorStateViewModel)

            val saveImageUseCase = SaveImageUseCase()

            val saveImage = {
                saveImageUseCase(context, editorStateViewModel.editedImage.value)
            }

            Pthotoditor2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ✨ 상단: 편집 화면
                        EditorScreen(
                            context = context,
                            onRequestImagePick = { galleryPicker() },
                            onSaveImage = saveImage,
                            filterViewModel = filterViewModel
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // 약간의 여백


                    }
                }
            }
        }
    }
}
