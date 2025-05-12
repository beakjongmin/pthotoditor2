//package com.ruto.pthotoditor2.feature.filter.components
//
//import android.content.Context
//import android.graphics.BitmapFactory
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Slider
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.unit.dp
//import com.ruto.pthotoditor2.feature.editor.viewmodel.EditorStateViewModel
//import com.ruto.pthotoditor2.feature.filter.manager.LutProvider
//import com.ruto.pthotoditor2.feature.filter.model.LutModel
//import com.ruto.pthotoditor2.feature.filter.viewmodel.FilterViewModel
//import kotlinx.coroutines.launch // 🔥 추가
//
//@Composable
//fun FilterSelectorScreen(
//    context: Context,
//    editorViewModel: EditorStateViewModel,
//    filterViewModel: FilterViewModel
//) {
//    val lutList = remember { LutProvider.getLutList(context) }
//    val coroutineScope = rememberCoroutineScope()
//    val intensity by filterViewModel.intensity.collectAsState()
//
//    Column(
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        // 🔥 필터 썸네일 리스트
//        LazyRow(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp)
//        ) {
//            items(lutList) { lutModel ->
//                FilterItem(
//                    lutModel = lutModel,
//                    context = context,
//                    onClick = {
//                        filterViewModel.selectFilter(lutModel)
//                        editorViewModel.originalImage.value?.let { bitmap ->
//                            coroutineScope.launch {
//                                filterViewModel.applySelectedFilterAsync(context, bitmap) { filteredBitmap ->
//                                    editorViewModel.setEditedBitmap(filteredBitmap)
//                                }
//                            }
//                        }
//                    }
//                )
//            }
//        }
//
//        // 🔥 슬라이더 위에 필터 강도 텍스트
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 8.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "필터 강도: ${(intensity * 100).toInt()}%",
//                style = MaterialTheme.typography.labelMedium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            Slider(
//                value = intensity,
//                onValueChange = {
//                    filterViewModel.setIntensity(it)
//                    editorViewModel.originalImage.value?.let { bitmap ->
//                        coroutineScope.launch {
//                            filterViewModel.applySelectedFilterAsync(context, bitmap) { filteredBitmap ->
//                                editorViewModel.setEditedBitmap(filteredBitmap)
//                            }
//                        }
//                    }
//                },
//                valueRange = 0f..1f
//            )
//        }
//    }
//}
//
//@Composable
//fun FilterItem(
//    lutModel: LutModel,
//    context: Context,
//    onClick: () -> Unit
//) {
//    val assetManager = context.assets
//
//    val previewBitmap = remember(lutModel.previewImagePath) {
//        runCatching {
//            val inputStream = assetManager.open(lutModel.previewImagePath)
//            BitmapFactory.decodeStream(inputStream)
//        }.getOrNull()
//    }
//
//    Column(
//        modifier = Modifier
//            .padding(4.dp)
//            .width(72.dp) // 🔥 사진과 이름 정렬 공간 확보
//            .clickable { onClick() },
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        previewBitmap?.let {
//            Image(
//                bitmap = it.asImageBitmap(),
//                contentDescription = lutModel.name,
//                modifier = Modifier
//                    .size(64.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(4.dp))
//
//        Text(
//            text = lutModel.name,
//            style = MaterialTheme.typography.labelSmall, // 🔥 작은 폰트로
//            maxLines = 1
//        )
//    }
//}
//
