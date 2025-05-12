package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await


/**
 * retrun 값은 입술,눈 부위에 대한 정보이다.
 */
object FacialPartMaskUtil {


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

        val eyeMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val mouthMask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

        val eyeCanvas = Canvas(eyeMask)
        val mouthCanvas = Canvas(mouthMask)
        val paint = Paint().apply { color = Color.WHITE }

        val face = faces.first()

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

        drawContour(eyeCanvas, face.getContour(FaceContour.LEFT_EYE)?.points)
        drawContour(eyeCanvas, face.getContour(FaceContour.RIGHT_EYE)?.points)

        drawContour(mouthCanvas, face.getContour(FaceContour.UPPER_LIP_TOP)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.LOWER_LIP_TOP)?.points)
        drawContour(mouthCanvas, face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points)

        return Pair(eyeMask, mouthMask)
    }
}
