package com.ruto.pthotoditor2.debuggingfunction

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.util.UUID

fun saveImageToGallery(context: Context, bitmap: Bitmap,filename: String): Uri? {

    val filename = "${filename}_${UUID.randomUUID()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoEditor2")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    imageUri?.let { uri ->
        resolver.openOutputStream(uri)?.use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 파일 완성
        resolver.update(uri, contentValues, null, null)

        Log.d("SuperResolution", "📸 갤러리에 저장됨: $uri")
        return uri
    }

    Log.e("SuperResolution", "❌ 이미지 저장 실패")
    return null
}
