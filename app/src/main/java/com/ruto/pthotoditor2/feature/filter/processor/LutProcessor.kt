package com.ruto.pthotoditor2.feature.filter.processor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ruto.pthotoditor2.feature.filter.model.LutData
import java.io.BufferedReader
import java.io.InputStreamReader

object LutProcessor {

    private const val TAG = "LutProcessor"

    /**
     * 🔄 LUT 캐시 저장소
     * - key: LUT 파일 경로 (예: "filters/warm.cube")
     * - value: LutData (파싱된 LUT 사이즈와 RGB 변환 테이블)
     * - 장점: LUT를 한 번만 읽고, 다시 쓸 때 재파싱하지 않아 성능 향상
     */
    private val lutCache = mutableMapOf<String, LutData>()
    /**
     *     lutCache = {
     *         "filters/warm.cube" to LutData(32, List[32768] of FloatArray[3]),
     *         "filters/cool.cube" to LutData(33, List[35937] of FloatArray[3]),
     *         "filters/vintage.cube" to LutData(25, List[15625] of FloatArray[3])
     *     }
     */

    /**
     * 📥 Assets 폴더에서 LUT 파일을 읽고 파싱하여 LutData로 반환
     * - 이미 캐시에 있다면 재사용 (성능 최적화)
     */
    fun loadLutFromAssets(context: Context, lutPath: String): LutData {
        // 💾 캐시가 존재하면 바로 반환 (중복 로딩 방지)
        lutCache[lutPath]?.let {
            Log.i(TAG, "LUT [$lutPath] loaded from cache.")
            return it
        }

        // 📂 파일 오픈 및 라인별 읽기
        val inputStream = context.assets.open(lutPath)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val tempTable = mutableListOf<FloatArray>()  // LUT 색상 테이블 저장소
        var lutSize = 0  // LUT 3D 사이즈 (예: 32)

        // 📖 파일 라인 반복
        reader.forEachLine { line ->
            val trimmed = line.trim()

            // 🔍 주석, 메타데이터 무시
            if (
                trimmed.isEmpty() || trimmed.startsWith("#") ||
                trimmed.startsWith("TITLE") || trimmed.startsWith("DOMAIN") ||
                trimmed.startsWith("LUT_1D_SIZE")
            ) return@forEachLine

            // 📏 LUT 크기 정보 파싱 (예: LUT_3D_SIZE 32)
            if (trimmed.startsWith("LUT_3D_SIZE")) {
                lutSize = trimmed.split(" ").last().toInt()
                Log.i(TAG, "Loaded LUT 3D size: $lutSize")
                return@forEachLine
            }

            // 🎨 RGB 색상 데이터 파싱 (예: 0.56 0.42 0.39)
            val parts = trimmed.split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }
            if (parts.size == 3) {
                tempTable.add(floatArrayOf(parts[0], parts[1], parts[2]))
            }
        }

        reader.close()

        // 📦 LUT 데이터 객체 생성 후 캐시에 저장
        val lutData = LutData(size = lutSize, table = tempTable)
        lutCache[lutPath] = lutData

        Log.i(TAG, "LUT [$lutPath] loaded. Total colors: ${tempTable.size}")
        return lutData
    }

    /**
     * 🎨 LUT 필터를 적용하여 색상 변경된 Bitmap 반환
     * - 각 픽셀을 LUT 색상으로 보정 (선형 보간 없이 인덱싱 방식)
     * - intensity로 보정 강도 조절 가능
     */
    fun applyLut(source: Bitmap, lut: LutData, intensity: Float = 0.3f): Bitmap {
        if (lut.table.isEmpty() || lut.size == 0) return source

        val width = source.width
        val height = source.height
        val lutSize = lut.size
        val lutTable = lut.table

        // ⚠️ Hardware Bitmap은 mutable 복사 필요
        val workingBitmap = if (source.config == Bitmap.Config.HARDWARE || !source.isMutable) {
            source.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            source
        }

        val pixels = IntArray(width * height)
        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 🔁 모든 픽셀에 LUT 적용
        for (i in pixels.indices) {
            val pixel = pixels[i]

            // RGB 정규화 (0~1)
            val rOriginal = (pixel shr 16 and 0xFF) / 255.0f
            val gOriginal = (pixel shr 8 and 0xFF) / 255.0f
            val bOriginal = (pixel and 0xFF) / 255.0f

            // LUT 인덱스 계산
            val rIndex = (rOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)
            val gIndex = (gOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)
            val bIndex = (bOriginal * (lutSize - 1)).toInt().coerceIn(0, lutSize - 1)

            val lutIndex = rIndex * lutSize * lutSize + gIndex * lutSize + bIndex
            val mapped = lutTable.getOrNull(lutIndex) ?: floatArrayOf(rOriginal, gOriginal, bOriginal)

            // 원본 색상과 LUT 색상을 섞음 (intensity에 따라)
            val rFinal = ((rOriginal * (1f - intensity)) + (mapped[0] * intensity)).coerceIn(0f, 1f)
            val gFinal = ((gOriginal * (1f - intensity)) + (mapped[1] * intensity)).coerceIn(0f, 1f)
            val bFinal = ((bOriginal * (1f - intensity)) + (mapped[2] * intensity)).coerceIn(0f, 1f)

            // 최종 색상으로 픽셀 값 갱신
            pixels[i] = (0xFF shl 24) or
                    ((rFinal * 255).toInt().coerceIn(0, 255) shl 16) or
                    ((gFinal * 255).toInt().coerceIn(0, 255) shl 8) or
                    (bFinal * 255).toInt().coerceIn(0, 255)
        }

        // 결과 비트맵 생성 및 적용
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * 🔍 썸네일용 Bitmap 축소 함수
     * - 필터 미리보기 속도 향상을 위해 사용
     */
    fun Bitmap.toThumbnail(width: Int = 80): Bitmap {
        val ratio = width.toFloat() / this.width
        val height = (this.height * ratio).toInt()
        return Bitmap.createScaledBitmap(this, width, height, true)
    }
}
