package com.ruto.pthotoditor2.core.image.segmentation.process.dslr

import android.content.Context
import android.graphics.Bitmap
import com.ruto.pthotoditor2.core.image.commonutil.HardwareBitmapConvert.ensureSoftwareConfig
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc



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
