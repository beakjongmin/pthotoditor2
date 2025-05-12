package com.ruto.pthotoditor2.feature.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ruto.pthotoditor2.feature.editor.viewmodel.EditorStateViewModel


/**
 * 갤러리에서 이미지를 선택하고 처리하는 런처를 생성합니다.
 *
 * @param context 현재 Context (LocalContext.current)
 * @param viewModel EditorStateViewModel (이미지 저장 및 효과 적용용)
 * @return 갤러리 열기를 트리거하는 함수
 */

@Composable
fun rememberGalleryPicker(
    context: Context,
    viewModel: EditorStateViewModel
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            bitmap?.let { bmp ->
                viewModel.setOriginal(bmp)
//                viewModel.applyBokehEffect(context)
            }
        }
    }

    return remember { { launcher.launch("image/*") } }
}

//context: 앱의 컨텍스트. 리소스 접근이나 시스템 서비스 요청에 필요.
//
//uri: 사용자가 갤러리에서 고른 이미지의 경로 (URI).
//
//리턴: 변환된 Bitmap, 실패 시 null.

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}