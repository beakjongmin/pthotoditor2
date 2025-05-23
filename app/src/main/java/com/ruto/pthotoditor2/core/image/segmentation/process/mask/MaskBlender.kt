package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object MaskBlender {

    fun toGrayMat(bitmap: Bitmap): Mat {
        val rgba = Mat()
        val gray = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        return gray
    }

    fun toAlphaMat(bitmap: Bitmap): Mat {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)

        val channels = mutableListOf<Mat>()
        Core.split(rgba, channels)
        val alpha = channels[3] // A 채널만 추출

        // 나머지 해제
        channels[0].release()
        channels[1].release()
        channels[2].release()
        rgba.release()

        return alpha
    }
//        fun createFaceAndHairMask(
//        segmentMask: Bitmap,
//        jawlineMask: Bitmap,
//        width: Int,
//        height: Int
//    ): Bitmap {
//        //얼굴,머리, 목 영역 gray scale
//        val personMat = toGrayMat(segmentMask)
//        //얼굴영역 gray scale
//        val faceMat = toGrayMat(jawlineMask)
//
//        // segment - face = 머리 + 목 + 어깨 후보 (얼굴 제외)
//        val hairCandidate = Mat()
//        Core.subtract(personMat, faceMat, hairCandidate)
//
//        // 윗부분 마스크 생성 (상단 55%)
//        val topMask = Mat.zeros(height, width, CvType.CV_8UC1)
//        Imgproc.rectangle(topMask, Rect(0, 0, width, (height * 0.65).toInt()), Scalar(255.0), -1)
//
//        // 머리카락만 추출
//        val hairOnlyMat = Mat()
//        Core.bitwise_and(hairCandidate, topMask, hairOnlyMat)
//
//        // 얼굴 + 머리카락 결합
//        val finalMat = Mat()
//        Core.bitwise_or(faceMat, hairOnlyMat, finalMat)
//
//        // 결과 비트맵으로 변환
//        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(finalMat, result)
//
//        // 메모리 해제
//        personMat.release()
//        faceMat.release()
//        hairCandidate.release()
//        topMask.release()
//        hairOnlyMat.release()
//        finalMat.release()
//
//        Log.d("MaskUtils", "✅ 얼굴 + 머리카락 마스크 생성 완료")
//        return result
//    }


    fun createFaceAndHairMask(
        segmentMask: Bitmap,
        jawlineMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        // 1. 얼굴, 머리, 목 전체 영역 → 실제  alpha 기반)
        val personMat = toAlphaMat(segmentMask)

        // 2. 얼굴 윤곽 마스크 (jawline 기반 전체 얼굴 영역) (alpha 기반)
        val faceMat = toAlphaMat(jawlineMask)

        // 3. 얼굴 제외한 나머지 (머리/목/어깨 후보)
        val hairCandidate = Mat()
        Core.subtract(personMat, faceMat, hairCandidate)

        // 4. 상단 65% 영역만 남기기 → 머리만 남김
        val topMask = Mat.zeros(height, width, CvType.CV_8UC1)
        Imgproc.rectangle(topMask, Rect(0, 0, width, (height * 0.85).toInt()), Scalar(255.0), -1)

        val hairOnlyMat = Mat()
        Core.bitwise_and(hairCandidate, topMask, hairOnlyMat)

        // 5. 얼굴 + 머리카락 결합
        val finalAlpha = Mat()
        Core.bitwise_or(faceMat, hairOnlyMat, finalAlpha)

        // 6. RGB는 전부 0, alpha 채널만 설정된 ARGB 비트맵 생성
        val zero = Mat.zeros(height, width, CvType.CV_8UC1)
        val r = zero.clone()
        val g = zero.clone()
        val b = zero.clone()
        val a = finalAlpha.clone()

        val bgraMat = Mat()
        Core.merge(listOf(b, g, r, a), bgraMat)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(bgraMat, result)

        // 7. 메모리 해제
        personMat.release()
        faceMat.release()
        hairCandidate.release()
        topMask.release()
        hairOnlyMat.release()
        finalAlpha.release()
        bgraMat.release()
        zero.release()
        r.release()
        g.release()
        b.release()
        a.release()

        Log.d("MaskUtils", "✅ 얼굴 + 머리카락 마스크 생성 완료 (알파 기반)")
        return result
    }
}