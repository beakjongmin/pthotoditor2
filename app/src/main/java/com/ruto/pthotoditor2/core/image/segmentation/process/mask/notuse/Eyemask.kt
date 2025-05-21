package com.ruto.pthotoditor2.core.image.segmentation.process.mask.notuse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

object EyeMaskUtil {

    suspend fun createEyeMaskFromMLKit(
        bitmap: Bitmap
    ): Bitmap? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(detectorOptions)

        val faces = detector.process(image).await()  // 코루틴 사용 (Task.await 필요)
        if (faces.isEmpty()) {
            Log.w("EyeMaskUtil", "얼굴을 찾지 못함")
            return null
        }

        val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(mask)
        val paint = Paint().apply { color = Color.WHITE }

        for (face in faces) {
            drawEyeContourMask(face, canvas, paint)
        }

        return mask
    }

    private fun drawEyeContourMask(face: Face, canvas: Canvas, paint: Paint) {
        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points
        val rightEye = face.getContour(FaceContour.RIGHT_EYE)?.points

        fun draw(points: List<PointF>?) {
            points?.let {
                val path = Path().apply {
                    moveTo(it.first().x, it.first().y)
                    for (point in it.drop(1)) lineTo(point.x, point.y)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }

        draw(leftEye)
        draw(rightEye)
    }
}