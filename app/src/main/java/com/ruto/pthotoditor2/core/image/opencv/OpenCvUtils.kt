package com.ruto.pthotoditor2.core.image.opencv

import android.graphics.Bitmap
import com.ruto.pthotoditor2.core.image.opencv.utils.BlendUtils
import com.ruto.pthotoditor2.core.image.opencv.utils.ColorUtils
import com.ruto.pthotoditor2.core.image.opencv.utils.MaskUtils

object OpenCvUtils {


    /**
     * MLKit SegmentationMask를 하드 알파 마스크로 변환합니다.
     */
    fun toHardAlphaMask(mask: com.google.mlkit.vision.segmentation.SegmentationMask, width: Int, height: Int): Bitmap {
        return MaskUtils.toHardAlphaMask(mask, width, height)
    }

    /**
     * base 이미지에 overlay 이미지를 alphaMask 기준으로 블렌딩합니다.
     */
    fun blend(base: Bitmap, overlay: Bitmap, alphaMask: Bitmap) {
        BlendUtils.blend(base, overlay, alphaMask)
    }

    /**
     * cropped 영역을 원본 이미지에 다시 합성합니다.
     */
    fun blendCroppedRegionBack(
        original: Bitmap,
        upscaledPerson: Bitmap,
        mask: Bitmap,
        offsetX: Int,
        offsetY: Int
    ): Bitmap {
        return BlendUtils.blendCroppedRegionBack(original, upscaledPerson, mask, offsetX, offsetY)
    }

    /**
     * 타겟 이미지를 기준 이미지의 평균 색상 톤에 맞춥니다.
     */
    fun matchToneByMean(reference: Bitmap, target: Bitmap): Bitmap {
        return ColorUtils.matchToneByMean(reference, target)
    }

    /**
     * 이미지 마스크 기반으로 인물 영역만 추출합니다.
     */
    fun extractMaskedRegion(bitmap: Bitmap, mask: Bitmap): Bitmap {
        return MaskUtils.extractMaskedRegion(bitmap, mask)
    }

    /**
     * 이미지 마스크 기반으로 두 이미지를 합성합니다. ( Orignal은 원본 , filterd는 전체 필터 적용된 rawimage,mask 는 마스크영역만 적용하고 나머지는 롤백)
     */
    fun applyFilterWithAlphaMask(original: Bitmap, filtered: Bitmap, mask: Bitmap) : Bitmap {
        return  MaskUtils.applyFilterWithAlphaMask(original,filtered,mask)
    }
}
