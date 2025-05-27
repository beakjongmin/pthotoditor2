package com.ruto.pthotoditor2.feature.editor.viewmodel

import LandmarkMaskUtil
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruto.photoeditor2.core.image.ml.SuperResolutionHelper
import com.ruto.pthotoditor2.core.image.commonutil.HardwareBitmapConvert.ensureSoftwareConfig
import com.ruto.pthotoditor2.core.image.opencv.OpenCvFilters
import com.ruto.pthotoditor2.core.image.opencv.OpenCvUtils
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor
import com.ruto.pthotoditor2.core.image.segmentation.process.facedetection.SelfieSegmentor.detectFaceWithHairRegion
import com.ruto.pthotoditor2.core.image.segmentation.process.facelandmark.FaceLandmarkerHelper
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.FacialPartMaskUtil
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.MaskBlender
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.partofface.HairMask
import com.ruto.pthotoditor2.core.image.segmentation.process.mask.scailing.MaskScale
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

    private val _isUpscalEnabled = MutableStateFlow(true)
    val isUpscalEnabled = _isUpscalEnabled.asStateFlow()

    fun SetUpscalEnabled(enabled: Boolean) {
        _isUpscalEnabled.value = enabled
    }

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

                //1.필터링할 영역 추출
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


                //2.마스킹 영역 생성 (전체 -> 부분 방향으로 진행)

                // 전체 마스크 생성 (croppedhead 기준 face mask)
                val fullPersonMask = SelfieSegmentor.segment(croppedHead)
                //알파 처리 ( croppedhead 크기의 맞춤)

                /**
                 * ( 사람의 헤어 영역부터 목까지 있는 0,255 이진화된 마스크)
                 */

                val hardfullPersonMask = OpenCvUtils.toHardAlphaMask(fullPersonMask, croppedHead.width, croppedHead.height)



                //얼굴 랜드마크 포인트 가져오기 (윤곽 마스크 생성)
                FaceLandmarkerHelper.init(context)
                val landmarks = FaceLandmarkerHelper.getFirstFaceLandmarks(croppedHead)

                if (landmarks == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "얼굴 랜드마크를 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                //랜드마크 기반 윤곽 마스크 생성

                /**
                 * 얼굴 윤곽 마스크
                 */
                val jawlineMask = LandmarkMaskUtil.createClosedFaceContourMask(landmarks, croppedHead.width, croppedHead.height)

                // 얼굴 + 머리카락 영역 결합된 마스크 생성 ( 마스크  결합)
                val faceHairMask = MaskBlender.createFaceAndHairMask(
                    segmentMask = hardfullPersonMask,
                    jawlineMask = jawlineMask,
                    width = croppedHead.width,
                    height = croppedHead.height
                )


                // 눈 마스크 생성
                // Eye mask 생성 ( parital 영역)
                val (eyeMaskRaw, _) = FacialPartMaskUtil.createEyeAndMouthMasks(croppedHead)

                if (eyeMaskRaw == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "얼굴 인식이 불가능합니다.", Toast.LENGTH_SHORT).show()
                        _isProcessing.value = false
                        onResult(original)
                    }
                    return@launch
                }

                // 마스크 확장
                val expandedEyemask = MaskScale.dilateAlphaMask(eyeMaskRaw, radius = 3.0)

                //헤어 마스크 생성 ( RGB 처리 -> 랜드마크 기반 생성된 expandedEyemask 추출)
                val hairMaskRaw = HairMask.extractHairMaskFromSegmentWithTone(croppedHead, hardfullPersonMask)

                // expandedEyemask 추출 (헤어만 남음)
                val HairMask = FacialPartMaskUtil.subtractMask(hairMaskRaw, expandedEyemask  , threshold = 50)


                //이진화 마스크를 filter 적용을 할떄 좀더 자연스럽게 적용시키기위해 사용됨
                val faceFeather = MaskScale.featherAlphaMask(faceHairMask, 5.0)
                val eyeFeather = MaskScale.featherAlphaMask(eyeMaskRaw, 5.0)



                //3.필터 효과 적용 영역

                //3.1 전체 영역에 필터 처리 진행
                val rawFiltered = OpenCvFilters.applyFilter(croppedHead, type)

                val filteredFace = OpenCvUtils.applyFilterWithAlphaMask(
                    original = croppedHead,
                    filtered = rawFiltered,
                    mask = faceHairMask
                )

                //3.2 부위 ( 눈 ) 필터 처리 진행
                val rawFilteredEyes = OpenCvFilters.applyEyesFilter(croppedHead,type) //눈만적용

                val filteredEyes = OpenCvUtils.applyFilterWithAlphaMask(
                    original = croppedHead,
                    filtered = rawFilteredEyes,
                    mask = eyeMaskRaw
                )
                
                
                //3.3 헤어 필터 처리
                //개발예정
                
                //4.부위별 이미지 결합

                // 결과 이미지 베이스
                val resultBitmap = Bitmap.createBitmap(
                    croppedHead.width,
                    croppedHead.height,
                    Bitmap.Config.ARGB_8888
                )

                Canvas(resultBitmap).drawBitmap(croppedHead, 0f, 0f, null)

                //(얼굴 영역 결합)
                OpenCvUtils.BlendOpencv(resultBitmap, filteredFace, faceFeather)
                //(눈 영역 결합)
                OpenCvUtils.BlendOpencv(resultBitmap, filteredEyes, eyeFeather)
                // 톤 매칭
                val toneMatched = OpenCvUtils.matchToneByMean(croppedHead, resultBitmap,faceFeather)

                //이미지 업스케일링 및 이미지 사이즈 조절 ( 화질 향상)

                //업스케일 (마지막 단계)

                // ✅ 조건 분기 처리
                val resultForBlend = if (_isUpscalEnabled.value) {
                    Log.d("업스케일진행 로그","업스케일 진행합니다 ${_isUpscalEnabled.value}")
                    val upscaled = SuperResolutionHelper.upscale(context, toneMatched)
                    upscaled
                } else {
                    toneMatched
                }


                // 원래 faceRect 크기로 리사이즈 (원본 합성용)
                val restoredSize = Bitmap.createScaledBitmap(
                    resultForBlend,
                    faceRect.width(),
                    faceRect.height(),
                    true
                )

                // 원본 이미지에 합성
                val final = OpenCvUtils.blendCroppedRegionBack(
                    original = safeOriginal,
                    ProcessedPerson = restoredSize,
                    mask = hardfullPersonMask,
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
