package com.ruto.pthotoditor2.core.image.segmentation.process.dslr

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

// ✅ HARDWARE 비트맵을 안전하게 소프트웨어 비트맵으로 변환
//fun Bitmap.ensureSoftwareConfig(): Bitmap {
//    return if (this.config == Bitmap.Config.HARDWARE) {
//        this.copy(Bitmap.Config.ARGB_8888, false)
//    } else {
//        this
//    }
//}

fun Bitmap.ensureSoftwareConfig(): Bitmap {

    return if (this.config == Bitmap.Config.HARDWARE || !this.isMutable) {
        this.copy(Bitmap.Config.ARGB_8888, true)
    } else {
        this
    }
}

// ✅ OpenCV Gaussian Blur 함수
fun Bitmap.blur(context: Context, radius: Float = 20f): Bitmap? {
    return try {
        val safeBitmap = this.ensureSoftwareConfig()

        val src = Mat()
        val dst = Mat()

        Utils.bitmapToMat(safeBitmap, src)

        val kernelSize = if (radius % 2 == 1f) radius else radius + 1
        Imgproc.GaussianBlur(src, dst, Size(kernelSize.toDouble(), kernelSize.toDouble()), 0.0)

        val result = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, result)

        src.release()
        dst.release()
        result
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
