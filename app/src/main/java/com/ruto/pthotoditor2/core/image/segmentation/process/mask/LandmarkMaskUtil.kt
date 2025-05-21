
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


object LandmarkMaskUtil {
        fun createClosedFaceContourMask(
            landmarks: List<NormalizedLandmark>,
            width: Int,
            height: Int
        ): Bitmap {
            Log.d("FaceMaskDebug", "🎯 Mask size: ${width} x $height")
            Log.d("FaceMaskDebug", "✅ landmarks.size = ${landmarks.size}")

            // MediaPipe FaceMesh 기반 얼굴 외곽선 인덱스 (가장 신뢰받는 순서)
            val outlineIndices = listOf(
                10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361,
                288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149,
                150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54,
                103, 67, 109
            )

            // 포인트 계산
            val outlinePoints = outlineIndices.mapIndexed { i, idx ->
                val x = (landmarks[idx].x() * width).toInt().toDouble()
                val y = (landmarks[idx].y() * height).toInt().toDouble()
                Log.d("FaceMaskDebug", "▶ Point[$i] = (x=$x, y=$y)")
                Point(x, y)
            }.toMutableList()

            outlinePoints.add(outlinePoints.first()) // 닫기

            // 마스크 생성
            val maskMat = Mat.zeros(height, width, CvType.CV_8UC1)
            val matOfPoint = MatOfPoint()
            matOfPoint.fromList(outlinePoints)
            Imgproc.fillPoly(maskMat, listOf(matOfPoint), Scalar(255.0))

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(maskMat, result)
            maskMat.release()

            Log.d("FaceMaskDebug", "✅ Mask generation complete")
            return result
        }
    }
