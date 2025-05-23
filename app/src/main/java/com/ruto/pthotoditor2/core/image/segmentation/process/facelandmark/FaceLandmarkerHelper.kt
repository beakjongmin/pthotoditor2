package com.ruto.pthotoditor2.core.image.segmentation.process.facelandmark

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

object FaceLandmarkerHelper {

    private var landmarker: FaceLandmarker? = null

    fun init(context: Context) {
        if (landmarker != null) return

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .build()
        Log.d("FaceLandmarkerHelper","FaceLandmarkerHelper init 성공")
        landmarker = FaceLandmarker.createFromOptions(context, options)
    }

    fun detectLandmarks(bitmap: Bitmap): FaceLandmarkerResult? {
        if (landmarker == null) {
            Log.e("FaceLandmarkerHelper", "❌ FaceLandmarker is not initialized.")
            throw IllegalStateException("FaceLandmarker not initialized.")
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker!!.detect(mpImage)
        Log.d("FaceLandmarkerHelper", "✅ Landmark detected: ${result.faceLandmarks().size}")
        return result
    }

    fun getFirstFaceLandmarks(bitmap: Bitmap): List<NormalizedLandmark>? {
        val result = detectLandmarks(bitmap)
        return result?.faceLandmarks()?.firstOrNull()
    }





    fun release() {
        landmarker?.close()
        landmarker = null
    }
}
