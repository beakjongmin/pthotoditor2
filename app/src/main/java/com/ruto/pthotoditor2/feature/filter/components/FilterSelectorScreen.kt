package com.ruto.pthotoditor2.feature.filter.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.ruto.pthotoditor2.feature.editor.viewmodel.EditorStateViewModel
import com.ruto.pthotoditor2.feature.filter.manager.LutProvider
import com.ruto.pthotoditor2.feature.filter.viewmodel.FilterViewModel
import kotlinx.coroutines.launch

@Composable
fun FilterSelectorScreen(
    context: Context,
    editorStateViewModel: EditorStateViewModel,
    filterViewModel: FilterViewModel
) {
    val lutList = remember { LutProvider.getLutList(context) }
    val previews by filterViewModel.filterPreviews.collectAsState()
    val intensity by filterViewModel.intensity.collectAsState()
    val originalBitmap by editorStateViewModel.originalImage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val loadingPreviews by filterViewModel.loadingPreviews.collectAsState()


    // 🔥 최초 미리보기 생성
    LaunchedEffect(originalBitmap) {
        originalBitmap?.let { bitmap ->
            filterViewModel.generatePreviewThumbnailsPaged(
                context = context,
                originalBitmap = bitmap,
                lutList = lutList,

            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (originalBitmap == null) {
            // ✅ 이미지 없을 때 안내 메시지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("이미지를 업로드하면 필터 미리보기가 표시됩니다.")
            }
        } else {

            // ✅ LazyRow – 필터 썸네일 미리보기
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                items(lutList) { lutModel ->
                    val preview = previews[lutModel.name]

                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(72.dp)
                            .clickable {
                                filterViewModel.selectFilter(lutModel)
                                originalBitmap?.let { bitmap ->
                                    coroutineScope.launch {
                                        filterViewModel.applySelectedFilterAsync(context, bitmap) {
                                            editorStateViewModel.setEditedBitmap(it)
                                        }
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = lutModel.name,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            val isLoading = lutModel.name in loadingPreviews

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (preview != null) {
                                    Image(
                                        bitmap = preview.asImageBitmap(),
                                        contentDescription = lutModel.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = lutModel.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 🔧 필터 강도 조절 슬라이더
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "필터 강도: ${(intensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = intensity,
                onValueChange = {
                    filterViewModel.setIntensity(it)
                    editorStateViewModel.originalImage.value?.let { bitmap ->
                        coroutineScope.launch {
                            filterViewModel.applySelectedFilterAsync(
                                context,
                                bitmap
                            ) { filteredBitmap ->
                                editorStateViewModel.setEditedBitmap(filteredBitmap)
                            }
                        }
                    }
                                },
                valueRange = 0f..1f
            )
        }
    }
}
