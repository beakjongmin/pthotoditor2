package com.ruto.pthotoditor2.feature.editor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.photoeditor2.core.image.ml.SuperResolutionHelper
import com.ruto.pthotoditor2.core.image.opencv.OpenCvFilters
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils.toSoftAlphaMask
import com.ruto.pthotoditor2.core.image.segmentation.process.dslr.ensureSoftwareConfig
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor.detectFaceWithHairRegion
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.FacialPartMaskUtil.createEyeAndMouthMasks
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing.MaskScale.featherAlphaMask
import com.ruto.pthotoditor2.feature.editor.model.UpScaletype
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class EnhancementViewModel @Inject constructor() : ViewModel() {


    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun getEffectTypes(): List<UpScaletype> = UpScaletype.entries


    fun applyPortraitEffect(
        context: Context,
        original: Bitmap?,
        type: UpScaletype,
        onResult: (Bitmap) -> Unit
    ) {
        if (original == null) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Main) { _isProcessing.value = true }

                val safeOriginal = original.ensureSoftwareConfig()
                val faceRect = detectFaceWithHairRegion(safeOriginal) ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "얼굴을 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                val croppedHead = Bitmap.createBitmap(
                    safeOriginal,
                    faceRect.left,
                    faceRect.top,
                    faceRect.width(),
                    faceRect.height()
                )

                // 마스크 생성 (전체 face mask)
                val fullPersonMask = SelfieSegmentor.segment(croppedHead)
                val faceAlphaMask =
                    toSoftAlphaMask(fullPersonMask, croppedHead.width, croppedHead.height)

                // Eye mask 생성
                val (eyeMaskRaw, _) = createEyeAndMouthMasks(croppedHead)
                if (eyeMaskRaw == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "얼굴 인식이 불가능합니다.", Toast.LENGTH_SHORT).show()
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                // 필터 처리

                val filteredFace = OpenCvFilters.applyFilter(croppedHead, type) //전체 필터 적용

                val filteredEyes = OpenCvFilters.applyEyesFilter(croppedHead,type) //눈만적용

                // Feather 마스크 처리
                val faceFeather = featherAlphaMask(faceAlphaMask, 3.0)

                val eyeFeather = featherAlphaMask(eyeMaskRaw, 5.0)


                // 결과 이미지 베이스
                val resultBitmap = Bitmap.createBitmap(
                    croppedHead.width,
                    croppedHead.height,
                    Bitmap.Config.ARGB_8888
                )
                Canvas(resultBitmap).drawBitmap(croppedHead, 0f, 0f, null)

                // 부위별 블렌딩

                //(얼굴 영역)
                OpenCvUtils.blend(resultBitmap, filteredFace, faceFeather)

//                saveImageToGallery(context,resultBitmap,"resultBitmap")
                //(눈 영역)
                OpenCvUtils.blend(resultBitmap, filteredEyes, eyeFeather)
                // 톤 매칭
                val toneMatched = OpenCvUtils.matchToneByMean(croppedHead, resultBitmap)
//                saveImageToGallery(context,toneMatched,"matchToneByMean")
                // 업스케일 (마지막 단계)
                val upscaledFinal = SuperResolutionHelper.upscale(context, toneMatched)
//                saveImageToGallery(context,upscaledFinal,"upscaledFinal")
                // 원래 faceRect 크기로 리사이즈 (원본 합성용)
                val restoredSize = Bitmap.createScaledBitmap(
                    upscaledFinal,
                    faceRect.width(),
                    faceRect.height(),
                    true
                )

                // 원본 마스크도 faceRect 기준으로 맞춤
                val maskRestored = Bitmap.createScaledBitmap(
                    faceAlphaMask,
                    faceRect.width(),
                    faceRect.height(),
                    true
                )

                // 원본 이미지에 합성
                val final = OpenCvUtils.blendCroppedRegionBack(
                    original = safeOriginal,
                    upscaledPerson = restoredSize,
                    mask = maskRestored,
                    offsetX = faceRect.left,
                    offsetY = faceRect.top
                )

                withContext(Dispatchers.Main) {
                    onResult(final)
                    _isProcessing.value = false
                }

            } catch (e: Exception) {
                Log.e("EnhancementViewModel", "❌ 오류: ${e.message}", e)
                withContext(Dispatchers.Main) { _isProcessing.value = false }
            }
        }
    }






}
