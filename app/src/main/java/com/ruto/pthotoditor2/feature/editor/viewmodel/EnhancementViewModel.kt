package com.ruto.pthotoditor2.feature.editor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.photoeditor2.core.image.ml.SuperResolutionHelper
import com.ruto.pthotoditor2.core.image.opencv.OpenCvFilters
import com.ruto.pthotoditor2.core.image.opencv.OpenCvFilters.applyFilterWithMask
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils.toSoftAlphaMask
import com.ruto.pthotoditor2.core.image.segmentation.process.dslr.ensureSoftwareConfig
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor.detectFaceWithHairRegion
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.FacialPartMaskUtil.createEyeAndMouthMasks
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.MasktoAlpha
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

                val mask = SelfieSegmentor.segment(safeOriginal)
                val maskBitmap = toSoftAlphaMask(mask, safeOriginal.width, safeOriginal.height)

                // ✅ 머리 영역 좌표 계산
                val faceRect = detectFaceWithHairRegion(safeOriginal)

                if (faceRect == null) {

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "얼굴을 인식할수 없습니다.", Toast.LENGTH_SHORT).show()
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                val croppedHead = Bitmap.createBitmap(safeOriginal, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())
                Log.d("EnhancementViewModel", "📌 얼굴+헤어 영역 크롭 완료: ${croppedHead.width}x${croppedHead.height} at (${faceRect.left}, ${faceRect.top})")


                // ✅ 업스케일
                val upscaled = SuperResolutionHelper.upscale(context, croppedHead)
                Log.d("EnhancementViewModel", "🆙 업스케일 완료: ${upscaled.width}x${upscaled.height}")


                // 🧠 눈/입 마스크 생성
                val (eyeMask, mouthMask) = createEyeAndMouthMasks(croppedHead)
                if (eyeMask == null || mouthMask == null) {
                    Log.w("EnhancementViewModel", "눈 또는 입 마스크 생성 실패. 필터 적용 건너뜀")
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                val argbEyeMask = MasktoAlpha.toAlphaDrawableMask(eyeMask)
                val argbMouthMask = MasktoAlpha.toAlphaDrawableMask(mouthMask)

                //디버깅용 임시 코드
//                fun convertAlphaToARGB(mask: Bitmap): Bitmap {
//                    val argbBitmap = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
//                    val canvas = Canvas(argbBitmap)
//                    val paint = Paint().apply { color = Color.WHITE }
//                    canvas.drawBitmap(mask, 0f, 0f, paint) // 흰색으로 채운 알파를 그대로 그림
//                    return argbBitmap
//                }
//                val debugimage = convertAlphaToARGB(upscaledMouthMask)
//                saveUpscaledImageToGallery(context,debugimage)
//                Log.w("EnhancementViewModel", "upscaledEyeMask ${upscaledEyeMask.width} * ${upscaledEyeMask.height} ")
                //alpha mask
                val upscaledEyeMask = Bitmap.createScaledBitmap(argbEyeMask, upscaled.width, upscaled.height, false)
                val upscaledMouthMask = Bitmap.createScaledBitmap(argbMouthMask, upscaled.width, upscaled.height, false)

                val croppedMask = Bitmap.createBitmap(maskBitmap, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())
                val maskUpscaled = Bitmap.createScaledBitmap(croppedMask, upscaled.width, upscaled.height, false)

                // ✅ 필터 적용
                val filtered = when (type) {
                    UpScaletype.SHARP -> {
                        applyFilterWithMask(context,upscaled, maskUpscaled,) {
                            OpenCvFilters.applySharp(it)
                        }
                    }
                    UpScaletype.SOFT -> {
                        applyFilterWithMask(context,upscaled, maskUpscaled) {
                            OpenCvFilters.applySoft(it)
                        }
                    }
                    UpScaletype.CLEAR -> {
                        applyFilterWithMask(context,upscaled, maskUpscaled) {
                            OpenCvFilters.applyClear(it)
                        }
                    }
                    UpScaletype.NATURAL -> {
                        applyFilterWithMask(context,upscaled, maskUpscaled) {
                            OpenCvFilters.applyNatural(it)
                        }
                    }
                    UpScaletype.UPSCALEONLY -> {
                        upscaled
                    }
                }

                val toneCorrected = OpenCvUtils.matchToneByMean(upscaled, filtered)
//                saveUpscaledImageToGallery(context,filtered)
                // ✅ 다시 원래 크기로 다운스케일 (원래 크기: headRect)
                val restoredSize = Bitmap.createScaledBitmap(toneCorrected, faceRect.width(), faceRect.height(), false )

                // ✅ 해당 위치에 블렌딩
                val maskRegion = Bitmap.createBitmap(maskBitmap, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())

                val maskRestored = Bitmap.createScaledBitmap(maskRegion, faceRect.width(), faceRect.height(), false )

                val final = OpenCvUtils.blendCroppedRegionBack(
                    original = safeOriginal,
                    upscaledPerson = restoredSize,
                    mask = maskRestored,
                    offsetX = faceRect.left,
                    offsetY = faceRect.top
                )

                Log.d("EnhancementViewModel", "🧪  처리된 이미지 크기: ${final.width}x${final.height}")

                withContext(Dispatchers.Main) {
                    onResult(final)
                    _isProcessing.value = false
                }

            } catch (e: Exception) {
                Log.e("EnhancementViewModel", "❌ 인물 효과 실패: ${e.message}", e)
                withContext(Dispatchers.Main) { _isProcessing.value = false }
            }
        }
    }


}
