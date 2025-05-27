package com.ruto.pthotoditor2.feature.editor.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruto.pthotoditor2.commonui.ImageDisplay
import com.ruto.pthotoditor2.commonui.LoadingOverlay
import com.ruto.pthotoditor2.feature.editor.components.common.BottomSelector
import com.ruto.pthotoditor2.feature.editor.components.common.ConfirmReplaceImageDialog
import com.ruto.pthotoditor2.feature.editor.components.dslr.BlurControl
import com.ruto.pthotoditor2.feature.editor.components.dslr.ThresholdControl
import com.ruto.pthotoditor2.feature.editor.components.upscale.UpScaleSelector
import com.ruto.pthotoditor2.feature.editor.model.EditorMode
import com.ruto.pthotoditor2.feature.editor.viewmodel.DSLRViewModel
import com.ruto.pthotoditor2.feature.editor.viewmodel.EditorStateViewModel
import com.ruto.pthotoditor2.feature.editor.viewmodel.EnhancementViewModel
import com.ruto.pthotoditor2.feature.filter.components.FilterSelectorScreen
import com.ruto.pthotoditor2.feature.filter.viewmodel.FilterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editorStateViewModel: EditorStateViewModel = hiltViewModel(),
    dslrViewModel: DSLRViewModel = hiltViewModel(),
    enhancementViewModel: EnhancementViewModel = hiltViewModel(),
    filterViewModel: FilterViewModel,
    context: Context,
    onRequestImagePick: () -> Unit,
    onSaveImage: () -> Unit
) {
    //editorstate
    val originalBitmap by editorStateViewModel.originalImage.collectAsState()
    val editedBitmap by editorStateViewModel.editedImage.collectAsState()
    val editorMode by editorStateViewModel.editorMode.collectAsState()

    //dslr
    val blurStrength by dslrViewModel.blurStrength.collectAsState()
    val threshold by dslrViewModel.threshold.collectAsState()

    //filter
    val showLoading by filterViewModel.isGeneratingPreview.collectAsState()

    //enhancement
    val isprocessingjob by enhancementViewModel.isProcessing.collectAsState()
    val isUpscaleEnabled by enhancementViewModel.isUpscalEnabled.collectAsState()

    //screen 관련 함수
    var showDialog by remember { mutableStateOf(false) }
    //터치 불가능 하게 하는 로직
    val blockTouchModifier = Modifier.pointerInput(showLoading) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
            }
        }
    }


    //DSLR 로 전환될시 효과 적용
    LaunchedEffect(editorMode) {
        when {
            editorMode == EditorMode.DSLR && originalBitmap != null -> {

                dslrViewModel.applyBokehEffect(context,originalBitmap, onResult = editorStateViewModel::setEditedBitmap)
            }
            editorMode == EditorMode.FILTER && originalBitmap != null -> {
                // 필터 적용
                //filterViewModel.
            }

            editorMode == EditorMode.UPSCALE && originalBitmap != null -> {
                //enhancementViewModel.applySuperResolution(context, original = originalBitmap, onResult = editorStateViewModel::setEditedBitmap)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    // ✅ 업스케일 모드일 경우에만 스위치 노출
                    if (editorMode == EditorMode.UPSCALE && editedBitmap != null ) {
                        Text(
                            text = if (isUpscaleEnabled) "고해상도 변환 적용" else "고해상도 변환 안함",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isUpscaleEnabled,
                            onCheckedChange = {
                                enhancementViewModel.SetUpscalEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                uncheckedThumbColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "업스케일 설명 보기",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { {} }
                        )
                    }

                    if (editedBitmap != null) {
                        TextButton(onClick = { onSaveImage() }) {
                            Text(
                                text = "저장",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF121212)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImageDisplay(
                originalBitmap = originalBitmap,
                editedBitmap = editedBitmap,
                onRequestImagePick = onRequestImagePick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                changeImage = {
                    showDialog = true
                }
            )

            BottomSelector(
                currentMode = editorMode,
                onModeSelected = { editorStateViewModel.setEditorMode(it) }
            )

            when (editorMode) {
                EditorMode.FILTER -> {
                    FilterSelectorScreen(
                        context = context,
                        editorStateViewModel = editorStateViewModel,
                        filterViewModel = filterViewModel
                    )
                }
                EditorMode.DSLR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "흐림 강도",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        BlurControl(
                            blurValue = blurStrength,
                            onBlurValueChange = {
                                dslrViewModel.setBlurStrength(
                                    context = context,
                                    value = it,
                                    original = originalBitmap,
                                    onResult = editorStateViewModel::setEditedBitmap
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "적용 범위",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        ThresholdControl(
                            thresholdValue = threshold,
                            onThresholdChange = {
                                dslrViewModel.setThreshold(
                                    context = context,
                                    value = it,
                                    original = originalBitmap,
                                    onResult = editorStateViewModel::setEditedBitmap
                                )
                            }
                        )
                    }
                }

                EditorMode.UPSCALE -> {
                    UpScaleSelector(
                        context = context,
                        enhancementViewModel = enhancementViewModel,
                        original = originalBitmap,
                        onResult = editorStateViewModel::setEditedBitmap
                    )
                }
            }
        }

        // ✅ 여기서 전체 오버레이
        LoadingOverlay(
            visible = isprocessingjob,
            message = " 고화질 변환을 진행 중입니다...",
            modifier = blockTouchModifier
        )

        LoadingOverlay(
            visible = showLoading,
            message = "필터 미리보기를 불러오는 중입니다...",
            modifier = blockTouchModifier
        )
        
        if (showDialog) {
            ConfirmReplaceImageDialog(
                onDismiss = { showDialog = false },
                onConfirm = {
                    showDialog = false
                    onRequestImagePick()
                }
            )
        }
    }
}
