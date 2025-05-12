package com.ruto.pthotoditor2.feature.editor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.pthotoditor2.core.image.segmentation.process.dslr.BokehProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DSLRViewModel @Inject constructor() : ViewModel() {

    private var bokehJob: Job? = null

    private val _blurStrength = MutableStateFlow(21f)
    val blurStrength = _blurStrength.asStateFlow()

    private val _threshold = MutableStateFlow(0.63f)
    val threshold = _threshold.asStateFlow()

    // 🎯 외부에서 원본과 setter를 받아 처리
    fun applyBokehEffect(
        context: Context,
        original: Bitmap?,
        onResult: (Bitmap) -> Unit
    ) {
        bokehJob?.cancel()
        bokehJob = viewModelScope.launch(Dispatchers.Default) {
            original?.let {
                val result = BokehProcessor.applyBokeh(
                    context = context,
                    original = it,
                    blurRadius = _blurStrength.value,
                    threshold = _threshold.value
                )
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            }
        }
    }

    fun setBlurStrength(value: Float, context: Context, original: Bitmap?, onResult: (Bitmap) -> Unit) {
        Log.d("DSLRViewModel", "BlurStrength 변경: $value")
        _blurStrength.value = value
        applyBokehEffect(context, original, onResult)
    }

    fun setThreshold(value: Float, context: Context, original: Bitmap?, onResult: (Bitmap) -> Unit) {
        Log.d("DSLRViewModel", "Threshold 변경: $value")
        _threshold.value = value
        applyBokehEffect(context, original, onResult)
    }
}
