package com.ruto.pthotoditor2

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoEditorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            System.loadLibrary("opencv_java4") // OpenCV 네이티브 라이브러리 로딩
            Log.d("OpenCV", "OpenCV native library loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("OpenCV", "Failed to load OpenCV native library", e)
        }
    }
}
