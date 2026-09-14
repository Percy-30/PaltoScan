package com.atpdev.paltoscan.domain.model

import java.io.Serializable

enum class RecognitionStatus {
    SUCCESS,
    INCONCLUSIVE,
    ERROR,
}

data class RecognitionResult(
    val diseaseName: String,
    val probability: Float,
    val status: RecognitionStatus = RecognitionStatus.SUCCESS,
    val imageUrl: String? = null,
    val confidenceLevel: String? = null,
) : Serializable {
    @Transient
    var heatmapBitmap: android.graphics.Bitmap? = null

    fun getProbabilityString(): String {
        return String.format("%.2f%%", probability * 100)
    }

    override fun toString(): String {
        return "Enfermedad: $diseaseName, Probabilidad: ${getProbabilityString()}"
    }
}
