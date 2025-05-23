package com.ruto.pthotoditor2.feature.filter.manager

import android.content.Context
import com.ruto.pthotoditor2.feature.filter.model.LutModel
import java.util.UUID

object LutProvider {
    fun getLutList(context: Context): List<LutModel> {
        val assetManager = context.assets
        val lutFileNames = assetManager.list("luts") ?: return emptyList()

        return lutFileNames.filter { it.endsWith(".cube") }.map { fileName ->
            LutModel(
                id = UUID.randomUUID().toString(), // UUID로 고유 ID 생성
                name = fileName.removeSuffix(".cube"),
                previewImagePath = "previewimage/sample.png", // assets 경로로
                lutFileName = "luts/$fileName"
            )
        }
    }
}