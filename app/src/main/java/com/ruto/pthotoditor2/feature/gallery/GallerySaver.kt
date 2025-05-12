package com.ruto.pthotoditor2.feature.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * SaveImageUseCase
 *
 * 앱의 편집된 이미지를 디바이스 갤러리에 저장하는 기능을 담당한다.
 *
 * - Context: 파일 접근 및 ContentResolver 사용을 위해 필요
 * - Bitmap: 저장할 이미지 데이터
 */
class SaveImageUseCase {

    /**
     * invoke 연산자 오버로딩
     *
     * 클래스 인스턴스를 함수처럼 사용할 수 있게 해준다.
     * 예시: saveImageUseCase(context, bitmap) 처럼 바로 호출 가능
     */
    operator fun invoke(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return // 저장할 이미지가 없으면 아무것도 하지 않음

        try {
            val filename = "EditedImage_${System.currentTimeMillis()}.jpg" // 파일 이름 생성 (타임스탬프 기반)

            val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PhotoEditor")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(imagesDir, filename)
                FileOutputStream(file)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            // ✅ 저장 성공 시 Toast 표시
            Toast.makeText(context, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            // ✅ 저장 실패 시 Toast 표시
            Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
