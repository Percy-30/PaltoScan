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
    val secondDiseaseName: String? = null,
    val secondProbability: Float = 0f,
    val isLowConfidence: Boolean = false,
) : Serializable {
    @Transient
    var heatmapBitmap: android.graphics.Bitmap? = null

    fun getProbabilityString(): String {
        return String.format("%.2f%%", probability * 100)
    }

    fun getSecondProbabilityString(): String {
        return String.format("%.2f%%", secondProbability * 100)
    }

    override fun toString(): String {
        val base = "Enfermedad: $diseaseName, Probabilidad: ${getProbabilityString()}"
        return if (!secondDiseaseName.isNullOrBlank()) {
            "$base (Segunda opción: $secondDiseaseName con ${getSecondProbabilityString()})"
        } else {
            base
        }
    }
}
