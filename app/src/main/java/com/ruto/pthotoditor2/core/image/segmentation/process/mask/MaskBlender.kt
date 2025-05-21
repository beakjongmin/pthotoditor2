package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
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
        return gray
    }

    fun createFaceAndHairMask(
        segmentMask: Bitmap,
        jawlineMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        //얼굴,머리, 목 영역 gray scale
        val personMat = toGrayMat(segmentMask)
        //얼굴영역 gray scale
        val faceMat = toGrayMat(jawlineMask)

        // segment - face = 머리 + 목 + 어깨 후보 (얼굴 제외)
        val hairCandidate = Mat()
        Core.subtract(personMat, faceMat, hairCandidate)

        // 윗부분 마스크 생성 (상단 55%)
        val topMask = Mat.zeros(height, width, CvType.CV_8UC1)
        Imgproc.rectangle(topMask, Rect(0, 0, width, (height * 0.65).toInt()), Scalar(255.0), -1)

        // 머리카락만 추출
        val hairOnlyMat = Mat()
        Core.bitwise_and(hairCandidate, topMask, hairOnlyMat)

        // 얼굴 + 머리카락 결합
        val finalMat = Mat()
        Core.bitwise_or(faceMat, hairOnlyMat, finalMat)

        // 결과 비트맵으로 변환
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(finalMat, result)

        // 메모리 해제
        personMat.release()
        faceMat.release()
        hairCandidate.release()
        topMask.release()
        hairOnlyMat.release()
        finalMat.release()

        Log.d("MaskUtils", "✅ 얼굴 + 머리카락 마스크 생성 완료")
        return result
    }

    fun createFaceAndHairMask1(
        segmentMask: Bitmap,
        jawlineMask: Bitmap,
        landmarks: List<NormalizedLandmark>,
        width: Int,
        height: Int
    ): Bitmap {
        val personMat = toGrayMat(segmentMask)
        val faceMat = toGrayMat(jawlineMask)

        // segment - face = 머리 + 목 + 어깨 후보
        val hairCandidate = Mat()
        Core.subtract(personMat, faceMat, hairCandidate)

        // ✅ 귀 landmark를 기준으로 머리 윗부분 추출
        val leftEarY = landmarks[127].y() * height
        val rightEarY = landmarks[356].y() * height
        val earMaxY = maxOf(leftEarY, rightEarY)

        val topMask = Mat.zeros(height, width, CvType.CV_8UC1)
        Imgproc.rectangle(
            topMask,
            Rect(0, 0, width, (earMaxY + 20).toInt()),  // +20 여유 포함
            Scalar(255.0),
            -1
        )

        // 머리카락만 추출
        val hairOnlyMat = Mat()
        Core.bitwise_and(hairCandidate, topMask, hairOnlyMat)

        // 얼굴 + 머리카락 결합
        val finalMat = Mat()
        Core.bitwise_or(faceMat, hairOnlyMat, finalMat)

        // 결과 비트맵으로 변환
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(finalMat, result)

        // 메모리 해제
        personMat.release()
        faceMat.release()
        hairCandidate.release()
        topMask.release()
        hairOnlyMat.release()
        finalMat.release()

        Log.d("MaskUtils", "✅ 얼굴 + 머리카락 마스크 생성 완료 (귀 포함)")
        return result
    }
}