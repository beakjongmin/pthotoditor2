//package com.ruto.pthotoditor2.feature.filter.processor
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import java.io.BufferedReader
//import java.io.InputStreamReader
//
//object LutProcessorbackup {
//
//    private const val TAG = "LutProcessor" // 🔥 로그 태그 추가
//
//    private var lutTable: List<FloatArray> = emptyList()
//    private var lutSize = 0
//    private val lutCache = mutableMapOf<String, List<FloatArray>>()
//
//    fun loadLutFromAssets(context: Context, lutPath: String) {
//        val inputStream = context.assets.open(lutPath)
//        val reader = BufferedReader(InputStreamReader(inputStream))
//        val tempTable = mutableListOf<FloatArray>()
//
//        reader.forEachLine { line ->
//            val trimmed = line.trim()
//            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("TITLE") || trimmed.startsWith("DOMAIN") || trimmed.startsWith("LUT_1D_SIZE")) {
//                return@forEachLine
//            }
//            if (trimmed.startsWith("LUT_3D_SIZE")) {
//                lutSize = trimmed.split(" ").last().toInt()
//                Log.i(TAG, "Loaded LUT 3D size: $lutSize") // 🔥 LUT 사이즈 로드 로그
//                return@forEachLine
//            }
//
//            val parts = trimmed.split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }
//            if (parts.size == 3) {
//                tempTable.add(floatArrayOf(parts[0], parts[1], parts[2]))
//            }
//        }
//
//        lutTable = tempTable
//        reader.close()
//
//        Log.i(TAG, "LUT table loaded successfully. Total colors: ${lutTable.size}")
//    }
//
//    fun applyLut(source: Bitmap, intensity: Float = 0.3f): Bitmap {
//        if (lutTable.isEmpty() || lutSize == 0) return source
//
//        val width = source.width
//        val height = source.height
//
//        val workingBitmap = if (source.config == Bitmap.Config.HARDWARE || !source.isMutable) {
//            source.copy(Bitmap.Config.ARGB_8888, true)
//        } else {
//            source
//        }
//
//        val pixels = IntArray(width * height)
//        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
//
//        for (i in pixels.indices) {
//            val pixel = pixels[i]
//            val rOriginal = (pixel shr 16 and 0xFF) / 255.0f
//            val gOriginal = (pixel shr 8 and 0xFF) / 255.0f
//            val bOriginal = (pixel and 0xFF) / 255.0f
//
//            val rIndex = (rOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)
//            val gIndex = (gOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)
//            val bIndex = (bOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)
//
//            val lutIndex = rIndex * lutSize * lutSize + gIndex * lutSize + bIndex
//            val mapped = lutTable.getOrNull(lutIndex) ?: floatArrayOf(rOriginal, gOriginal, bOriginal)
//
//            // 🔥 원본색과 LUT 색을 섞는다
//            val rFinal = ((rOriginal * (1f - intensity)) + (mapped[0] * intensity)).coerceIn(0f, 1f)
//            val gFinal = ((gOriginal * (1f - intensity)) + (mapped[1] * intensity)).coerceIn(0f, 1f)
//            val bFinal = ((bOriginal * (1f - intensity)) + (mapped[2] * intensity)).coerceIn(0f, 1f)
//
//            pixels[i] = (0xFF shl 24) or
//                    ((rFinal * 255).toInt().coerceIn(0, 255) shl 16) or
//                    ((gFinal * 255).toInt().coerceIn(0, 255) shl 8) or
//                    (bFinal * 255).toInt().coerceIn(0, 255)
//        }
//
//        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        output.setPixels(pixels, 0, width, 0, 0, width, height)
//
//        return output
//    }
//
//    fun Bitmap.toThumbnail(width: Int = 80): Bitmap {
//        val ratio = width.toFloat() / this.width
//        val height = (this.height * ratio).toInt()
//        return Bitmap.createScaledBitmap(this, width, height, true)
//    }
//
//}
