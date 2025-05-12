package com.ruto.pthotoditor2.feature.filter.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.pthotoditor2.feature.filter.model.LutData
import com.ruto.pthotoditor2.feature.filter.model.LutModel
import com.ruto.pthotoditor2.feature.filter.processor.LutProcessor
import com.ruto.pthotoditor2.feature.filter.processor.LutProcessor.toThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow<LutModel?>(null)
    val selectedFilter: StateFlow<LutModel?> = _selectedFilter

    private val _intensity = MutableStateFlow(0.17f)
    val intensity: StateFlow<Float> = _intensity

    private val _isGeneratingPreview = MutableStateFlow(false)
    val isGeneratingPreview: StateFlow<Boolean> = _isGeneratingPreview

    private var filterJob: Job? = null

    private val _filterPreviews = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val filterPreviews: StateFlow<Map<String, Bitmap>> = _filterPreviews

    private val _loadingPreviews = MutableStateFlow<Set<String>>(emptySet())
    val loadingPreviews: StateFlow<Set<String>> = _loadingPreviews

    fun selectFilter(filter: LutModel) {
        _selectedFilter.value = filter
    }

    fun setIntensity(value: Float) {
        _intensity.value = value.coerceIn(0f, 1f)
    }

    /**
     * 사용자 요청에 따른 필터 적용: 미리보기 생성 중일 때는 무시
     */
    fun applySelectedFilterAsync(
        context: Context,
        originalBitmap: Bitmap,
        onResult: (Bitmap) -> Unit
    ) {
        if (_isGeneratingPreview.value) return // 🔒 중복 적용 방지

        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val filter = _selectedFilter.value ?: return@launch onResult(originalBitmap)
            val currentIntensity = _intensity.value

            val lutData = withContext(Dispatchers.IO) {
                LutProcessor.loadLutFromAssets(context, filter.lutFileName)
            }

            val filteredBitmap = withContext(Dispatchers.Default) {
                LutProcessor.applyLut(originalBitmap, lutData, intensity = currentIntensity)
            }

            onResult(filteredBitmap)
        }
    }

    /**
     * LazyRow용 썸네일 필터 미리보기: Top10 우선 + 나머지는 유휴 상태에서 순차 로딩
     */
    fun generatePreviewThumbnailsPaged(
        context: Context,
        originalBitmap: Bitmap,
        lutList: List<LutModel>,
        intensity: Float = 0.32f
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val thumb = originalBitmap.toThumbnail()
            Log.d("FilterPreview", "Generated thumb size: ${thumb.width} x ${thumb.height}")
            val topList = lutList.take(10)
            val remainingList = lutList.drop(10)

            withContext(Dispatchers.Main) {
                _isGeneratingPreview.value = true
                _loadingPreviews.value = lutList.map { it.name }.toSet()
            }

            // 🔹 Top 10 먼저 처리
            topList.forEach { lut ->
                processSingleLut(context, lut, thumb, intensity)
            }

            // 🔹 나머지 순차 로딩 (5개씩)
            remainingList.chunked(5).forEach { chunk ->
                chunk.forEach { lut ->
                    processSingleLut(context, lut, thumb, intensity)
                }
                delay(200L)
            }

            withContext(Dispatchers.Main) {
                _loadingPreviews.value = emptySet()
                _isGeneratingPreview.value = false
            }
        }
    }

    /**
     * LUT 필터 하나 적용 후 결과 UI에 반영
     */
    private suspend fun processSingleLut(
        context: Context,
        lut: LutModel,
        thumb: Bitmap,
        intensity: Float
    ) {
        try {
            val lutData: LutData = withContext(Dispatchers.IO) {
                LutProcessor.loadLutFromAssets(context, lut.lutFileName)
            }

            val filtered = withContext(Dispatchers.Default) {
                LutProcessor.applyLut(thumb, lutData, intensity)
            }

            withContext(Dispatchers.Main) {
                _filterPreviews.value = _filterPreviews.value + (lut.name to filtered)
            }
        } catch (e: Exception) {
            Log.e("FilterViewModel", "Failed to process LUT ${lut.name}", e)
        }
    }
}