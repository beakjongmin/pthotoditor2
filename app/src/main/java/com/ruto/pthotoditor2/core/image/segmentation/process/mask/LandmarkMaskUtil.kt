

import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.opencv.android.Utils
import org.opencv.core.Core
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

        // 얼굴 외곽 인덱스 (MediaPipe 기준)
        val outlineIndices = listOf(
            10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361,
            288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149,
            150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54,
            103, 67, 109
        )

        val outlinePoints = outlineIndices.mapIndexed { i, idx ->
            val x = (landmarks[idx].x() * width).toInt().toDouble()
            val y = (landmarks[idx].y() * height).toInt().toDouble()
            Point(x, y)
        }.toMutableList()
        outlinePoints.add(outlinePoints.first()) // 윤곽 닫기

        // 1채널 마스크 (Gray → Alpha 용도)
        val maskMat = Mat.zeros(height, width, CvType.CV_8UC1)
        val matOfPoint = MatOfPoint()
        matOfPoint.fromList(outlinePoints)
        Imgproc.fillPoly(maskMat, listOf(matOfPoint), Scalar(255.0)) // 내부 = 255

        // BGRA 채널 구성: RGB = 0, Alpha = maskMat
        val bgraMat = Mat(height, width, CvType.CV_8UC4)
        val zero = Mat.zeros(height, width, CvType.CV_8UC1)
        val alpha = maskMat.clone()

        val channels = listOf(
            zero,       // B
            zero.clone(), // G
            zero.clone(), // R
            alpha        // A
        )

        Core.merge(channels, bgraMat)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(bgraMat, result)

        // 리소스 해제
        maskMat.release()
        matOfPoint.release()
        bgraMat.release()
        zero.release()
        channels[1].release()
        channels[2].release()
        alpha.release()

        Log.d("FaceMaskDebug", "✅ Mask generation complete with proper alpha")
        return result
    }

    }
