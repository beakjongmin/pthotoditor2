package com.ruto.pthotoditor2.core.image.segmentation.process.facedetection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.tasks.await

// 이 객체는 MLKit의 Selfie Segmentation 기능을 캡슐화한 유틸리티 싱글턴이다.
// object 키워드를 사용하면 앱 전체에서 단 하나의 인스턴스만 만들어짐 (싱글톤)
object SelfieSegmentor {

    // segmenter 는 lazy 로 초기화됨. 즉, 처음 호출될 때 한 번만 생성됨.
    // MLKit의 SelfieSegmenter를 생성하는 코드이며, 내부적으로 Context는 자동 주입됨.
    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE) // 단일 이미지 처리 모드
                .build()
        )
    }

    // 실제로 segmentation을 수행하는 suspend 함수 (비동기 + 코루틴 기반)
    // 입력은 Bitmap, 출력은 MLKit이 제공하는 SegmentationMask
    suspend fun segment(bitmap: Bitmap): SegmentationMask {
        // MLKit이 요구하는 InputImage 객체로 변환 (bitmap + 회전 0도)
        val input = InputImage.fromBitmap(bitmap, 0)
        Log.d("배경 분리 로직", "Starting segment...")
        // segmenter 로부터 결과를 비동기로 얻어옴. await()는 코루틴에서 suspend 처리
        return segmenter.process(input).await()
    }


    suspend fun detectFaceWithHairRegion(bitmap: Bitmap): Rect? {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val faces = detector.process(inputImage).await()
        if (faces.isEmpty()) return null

        val face = faces.first().boundingBox

        // 헤어를 포함하도록 위로 확장
//        val extendedTop = (face.top - face.height() * 0.3f).toInt().coerceAtLeast(0)
//        val extendedBottom = (face.bottom + face.height() * 0.05f).toInt().coerceAtMost(bitmap.height)
//        val paddingRatio = 0.01f // 좌우로 얼굴 너비의 10%씩 확장
//        val extendedLeft = (face.left - face.width() * paddingRatio).toInt().coerceAtLeast(0)
//        val extendedRight = (face.right + face.width() * paddingRatio).toInt().coerceAtMost(bitmap.width)
        val extendedTop = (face.top - face.height() * 0.3f).toInt().coerceAtLeast(0)
        val extendedBottom = (face.bottom + face.height() * 0.05f).toInt().coerceAtMost(bitmap.height)
        val extendedLeft = face.left
        val extendedRight = face.right
        return Rect(extendedLeft, extendedTop, extendedRight, extendedBottom)
    }


}