package com.ruto.pthotoditor2.feature.editor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.photoeditor2.core.image.ml.SuperResolutionHelper
import com.ruto.pthotoditor2.core.image.opencv.OpenCvFilters
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils.toSoftAlphaMask
import com.ruto.pthotoditor2.core.image.opencv.process.filter.facialfacepart.EyesFilter.applySnowStyleSharp
import com.ruto.pthotoditor2.core.image.segmentation.process.dslr.ensureSoftwareConfig
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor.detectFaceWithHairRegion
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.FacialPartMaskUtil.createEyeAndMouthMasks
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing.MaskScale.featherAlphaMask
import com.ruto.pthotoditor2.debuggingfunction.ColorLogger.visualizeAlphaMask
import com.ruto.pthotoditor2.debuggingfunction.saveImageToGallery
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

    fun getEffectTypes(): List<UpScaletype> = UpScaletype.values().toList()



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

                val croppedHead = Bitmap.createBitmap(safeOriginal, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())

                // 마스크 생성 (전체 face mask)
                val fullPersonMask = SelfieSegmentor.segment(croppedHead)
                val faceAlphaMask = toSoftAlphaMask(fullPersonMask, croppedHead.width, croppedHead.height)

                // Eye mask 생성
                val (eyeMaskRaw, _) = createEyeAndMouthMasks(croppedHead)
                if (eyeMaskRaw == null) {
                    Log.w("EnhancementViewModel", "❌ Eye Mask 생성 실패. 종료.")
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                // 필터 처리

                val filteredFace = applyFilter(croppedHead, type) //전체 필터 적용

                val filteredEyes = applySnowStyleSharp(croppedHead) //눈만적용
                Log.d("enhance","${filteredEyes.config}")
                Log.d("enhance","faceAlphaMask ${faceAlphaMask.config}")
                Log.d("enhance","eyeMaskRaw ${eyeMaskRaw.config}")
                // Feather 마스크 처리
                val faceFeather = featherAlphaMask(faceAlphaMask, 5.0)
                Log.d("enhance","faceFeather 완료 ${faceFeather.config}")
                val eyeFeather = featherAlphaMask(eyeMaskRaw, 5.0)
                Log.d("enhance","faceFeather 완료 ${eyeFeather.config}")



                // 결과 이미지 베이스
                val resultBitmap = Bitmap.createBitmap(croppedHead.width, croppedHead.height, Bitmap.Config.ARGB_8888)
                Canvas(resultBitmap).drawBitmap(croppedHead, 0f, 0f, null)

                // 부위별 블렌딩
                blend(resultBitmap, filteredFace, faceFeather)

                blend(resultBitmap, filteredEyes, eyeFeather)
                val debugmask = visualizeAlphaMask(eyeMaskRaw)
                saveImageToGallery(context, debugmask, "debug_eyeMaskRaw")

                // 톤 매칭
                val toneMatched = OpenCvUtils.matchToneByMean(croppedHead, resultBitmap)
                saveImageToGallery(context, toneMatched, "debug_toneMatched")
                // 업스케일 (마지막 단계)
                val upscaledFinal = SuperResolutionHelper.upscale(context, toneMatched)
                saveImageToGallery(context, upscaledFinal, "debug_upscaledFinal")
                // 원래 faceRect 크기로 리사이즈 (원본 합성용)
                val restoredSize = Bitmap.createScaledBitmap(upscaledFinal, faceRect.width(), faceRect.height(), true)

                // 원본 마스크도 faceRect 기준으로 맞춤
                val maskRestored = Bitmap.createScaledBitmap(faceAlphaMask, faceRect.width(), faceRect.height(), true)

                // 원본 이미지에 합성
                val final = OpenCvUtils.blendCroppedRegionBack(
                    original = safeOriginal,
                    upscaledPerson = restoredSize,
                    mask = maskRestored,
                    offsetX = faceRect.left,
                    offsetY = faceRect.top
                )
                saveImageToGallery(context, final, "debug_finalOutput")
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

    private fun applyFilter(source: Bitmap, type: UpScaletype): Bitmap {
        return when (type) {
            UpScaletype.SHARP -> {
                OpenCvFilters.applySharp(source)
            }
            UpScaletype.SOFT -> {
                OpenCvFilters.applySoft(source)
            }
            UpScaletype.CLEAR -> {
                OpenCvFilters.applyClear(source)
            }
            UpScaletype.NATURAL -> {
                OpenCvFilters.applyNatural(source)
            }
            UpScaletype.UPSCALEONLY -> {
                source
            }
        }
    }

    private fun blend(base: Bitmap, overlay: Bitmap, alphaMask: Bitmap) {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Masked overlay 준비
        /**
         * overlay를 maskedOverlay 위에 그대로 그림
         * 예: 눈 필터링 된 sharp 이미지를 덮어씀.
         * RGB 값이 maskedOverlay에 먼저 들어감.
         */

        val maskedOverlay = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        Canvas(maskedOverlay).apply {
            drawBitmap(overlay, 0f, 0f, null)
            drawBitmap(alphaMask, 0f, 0f, paint)
        }

        // 최종 base 위에 합성
        Canvas(base).drawBitmap(maskedOverlay, 0f, 0f, null)
    }

}
