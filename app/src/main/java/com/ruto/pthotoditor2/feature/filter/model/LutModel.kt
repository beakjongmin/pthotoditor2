package com.ruto.pthotoditor2.feature.filter.model


data class LutModel(
    val id: String,             // 고유 식별자 추가 (UUID나 고유한 값)
    val name: String,
    val previewImagePath: String, // 새로 추가: assets 경로
    val lutFileName: String
)

data class LutData(
    val size: Int,
    val table: List<FloatArray>
)