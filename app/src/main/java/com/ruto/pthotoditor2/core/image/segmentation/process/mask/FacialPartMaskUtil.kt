package com.ruto.pthotoditor2.core.image.segmentation.process.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


/**
 * retrun 값은 입술,눈 부위에 대한 정보이다.
 */
object FacialPartMaskUtil {

    fun subtractMaskOpenCV(baseMask: Bitmap, eyeMask: Bitmap): Bitmap {
        val baseMat = Mat()
        val eyeMat = Mat()
        val result = Mat()

        Utils.bitmapToMat(baseMask, baseMat)
        Utils.bitmapToMat(eyeMask, eyeMat)

        // 단일 채널 알파 값만 비교한다면 아래처럼 gray 변환 후 subtract
        Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(eyeMat, eyeMat, Imgproc.COLOR_RGBA2GRAY)

        Core.subtract(baseMat, eyeMat, result)

        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2RGBA)
        Utils.matToBitmap(result, resultBitmap)
        return resultBitmap
    }
    //OpenCV subtract는 대체로 dense한 마스크(예: 전체 인물 세그먼트)에 유리
    fun subtractMaskOpenCVFixed(baseMask: Bitmap, eyeMask: Bitmap): Bitmap {
        val baseMat = Mat()
        val eyeMat = Mat()
        val resultMat = Mat()

        Utils.bitmapToMat(baseMask, baseMat)
        Utils.bitmapToMat(eyeMask, eyeMat)

        val baseChannels = ArrayList<Mat>()
        val eyeChannels = ArrayList<Mat>()

        Core.split(baseMat, baseChannels)
        Core.split(eyeMat, eyeChannels)

        val baseAlpha = baseChannels[3]
        val eyeAlpha = eyeChannels[3]
        val resultAlpha = Mat()

        // ✅ 눈 마스크 알파를 threshold 처리
        Imgproc.threshold(eyeAlpha, eyeAlpha, 10.0, 255.0, Imgproc.THRESH_BINARY)

        Core.subtract(baseAlpha, eyeAlpha, resultAlpha)

        val white = Mat.ones(baseAlpha.size(), CvType.CV_8UC1).apply {
            convertTo(this, CvType.CV_8UC1, 255.0)
        }

        val merged = arrayListOf(white.clone(), white.clone(), white.clone(), resultAlpha)
        Core.merge(merged, resultMat)

        val output = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, output)

        Log.d("SubtractDebug", "baseAlpha mean=${Core.mean(baseAlpha)}")
        Log.d("SubtractDebug", "eyeAlpha mean (after threshold)=${Core.mean(eyeAlpha)}")

        // 💥 release 처리
        baseMat.release()
        eyeMat.release()
        resultMat.release()
        baseAlpha.release()
        eyeAlpha.release()
        resultAlpha.release()
        white.release()
        baseChannels.forEach { it.release() }
        eyeChannels.forEach { it.release() }
        merged.forEach { it.release() }

        return output
    }

//    이 함수는 성능이 약간 느릴 수 있어 (픽셀 루프)
//
//    하지만 정확성이 중요할 때는 이 방식이 더 신뢰 가능
//
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
        val paint = Paint().apply { color = Color.WHITE }

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
        val output = Bitmap.createBitmap(mask.width, mask.height,Bitmap.Config.ALPHA_8)
        val canvas = Canvas(output)
        val matrix = Matrix().apply {
            postRotate(angle, mask.width / 2f, mask.height / 2f)
        }
        canvas.drawBitmap(mask, matrix, null)
        return output
    }
    private fun drawAccurateEyeMask(
        canvas: Canvas,
        top: List<PointF>?,
        bottom: List<PointF>?,
        paint: Paint
    ) {
        if (top.isNullOrEmpty() || bottom.isNullOrEmpty()) return

        val path = Path().apply {
            moveTo(top.first().x, top.first().y)
            top.drop(1).forEach { lineTo(it.x, it.y) }
            bottom.reversed().forEach { lineTo(it.x, it.y) }
            close()
        }
        canvas.drawPath(path, paint)
    }


}
