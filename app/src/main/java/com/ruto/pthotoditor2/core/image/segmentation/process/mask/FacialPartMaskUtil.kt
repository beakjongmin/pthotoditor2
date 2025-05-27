package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ruto.pthotoditor2.core.image.segmentation.process.facelandmark.FaceMeshIndices
import kotlinx.coroutines.tasks.await


/**
 * retrun 값은 입술,눈 부위에 대한 정보이다.
 */
object FacialPartMaskUtil {


//    특히 눈처럼 좁고 예민한 마스크 영역에서는 픽셀 단위가 더 확실
    fun subtractMask(full: Bitmap, subtract: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(full.width, full.height, Bitmap.Config.ARGB_8888)

        for (y in 0 until full.height) {
            for (x in 0 until full.width) {
                val fullAlpha = Color.alpha(full.getPixel(x, y))
                val subAlpha = Color.alpha(subtract.getPixel(x, y))
                val newAlpha = (fullAlpha - subAlpha).coerceAtLeast(0)
                result.setPixel(x, y, Color.argb(newAlpha, 255, 255, 255))
            }
        }
        return result
    }

    fun subtractMask(full: Bitmap, subtract: Bitmap, threshold: Int = 5): Bitmap {
        val width = full.width
        val height = full.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val fullAlpha = Color.alpha(full.getPixel(x, y))
                val subAlpha = Color.alpha(subtract.getPixel(x, y))

                val newAlpha = if (subAlpha >= threshold) {
                    0 // 강제 제거
                } else {
                    fullAlpha
                }

                result.setPixel(x, y, Color.argb(newAlpha, 255, 255, 255))
            }
        }
        return result
    }

    suspend fun createEyeAndMouthMasks(croppedFace: Bitmap): Pair<Bitmap?, Bitmap?> {
        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(detectorOptions)
        val inputImage = InputImage.fromBitmap(croppedFace, 0)
        val faces = detector.process(inputImage).await()

        if (faces.isEmpty()) {
            Log.w("FacialPartMaskUtil", "No face found in cropped image.")
            return Pair(null, null)
        }

        val width = croppedFace.width
        val height = croppedFace.height

        // 🧼 초기 마스크
        val rawEyeMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val rawMouthMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val eyeCanvas = Canvas(rawEyeMask)
        val mouthCanvas = Canvas(rawMouthMask)
        val paint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = false
        }

        val face = faces.first()

        // 🧠 회전 각도
        val rotationAngle = face.headEulerAngleZ
        Log.d("MaskRotation", "Z 회전 각도: $rotationAngle")

        fun drawContour(canvas: Canvas, points: List<PointF>?) {
            points?.let {
                val path = Path().apply {
                    moveTo(it.first().x, it.first().y)
                    for (pt in it.drop(1)) lineTo(pt.x, pt.y)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }

        // 🎯 눈 그리기
        drawContour(eyeCanvas, face.getContour(FaceContour.LEFT_EYE)?.points)
        drawContour(eyeCanvas, face.getContour(FaceContour.RIGHT_EYE)?.points)

        // 🎯 입 그리기
        drawContour(mouthCanvas, face.getContour(FaceContour.UPPER_LIP_TOP)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.LOWER_LIP_TOP)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points)

        // 📌 회전된 마스크 반환
        val rotatedEyeMask = rotateBitmap(rawEyeMask, -rotationAngle) // 보정은 역방향
        val rotatedMouthMask = rotateBitmap(rawMouthMask, -rotationAngle)

        return Pair(rotatedEyeMask, rotatedMouthMask)


    }
    // 🔁 [NEW] 회전 보정 함수
    fun rotateBitmap(mask: Bitmap, angle: Float): Bitmap {
        val output = Bitmap.createBitmap(mask.width, mask.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = Matrix().apply {
            postRotate(angle, mask.width / 2f, mask.height / 2f)
        }
        canvas.drawBitmap(mask, matrix, null)
        return output
    }


    fun createEyeMaskFromLandmarks(
        landmarks: List<NormalizedLandmark>,
        width: Int,
        height: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = false // <-- 이걸 false 로 설정
        }

        fun drawPolygon(indices: List<Int>) {
            val path = Path().apply {
                val first = landmarks[indices.first()]
                moveTo(first.x() * width, first.y() * height)
                for (i in 1 until indices.size) {
                    val pt = landmarks[indices[i]]
                    lineTo(pt.x() * width, pt.y() * height)
                }
                close()
            }
            canvas.drawPath(path, paint)
        }

        drawPolygon(FaceMeshIndices.LEFT_EYE)
        drawPolygon(FaceMeshIndices.RIGHT_EYE)

        return result
    }

    fun createEyeBrowFromLandmarks(
        landmarks: List<NormalizedLandmark>,
        width: Int,
        height: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        fun drawPolygon(indices: List<Int>) {
            val path = Path().apply {
                val first = landmarks[indices.first()]
                moveTo(first.x() * width, first.y() * height)
                for (i in 1 until indices.size) {
                    val pt = landmarks[indices[i]]
                    lineTo(pt.x() * width, pt.y() * height)
                }
                close()
            }
            canvas.drawPath(path, paint)
        }

        drawPolygon(FaceMeshIndices.LEFT_EYEBROW)
        drawPolygon(FaceMeshIndices.RIGHT_EYEBROW)

        return result
    }
//    private fun drawAccurateEyeMask(
//        canvas: Canvas,
//        top: List<PointF>?,
//        bottom: List<PointF>?,
//        paint: Paint
//    ) {
//        if (top.isNullOrEmpty() || bottom.isNullOrEmpty()) return
//
//        val path = Path().apply {
//            moveTo(top.first().x, top.first().y)
//            top.drop(1).forEach { lineTo(it.x, it.y) }
//            bottom.reversed().forEach { lineTo(it.x, it.y) }
//            close()
//        }
//        canvas.drawPath(path, paint)
//    }


}
