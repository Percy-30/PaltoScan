package com.atpdev.paltoscan.domain.repository

import android.graphics.Bitmap
import com.atpdev.paltoscan.domain.model.RecognitionResult

interface ImageRecognitionRepository {
    suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult

    fun setDetectionThreshold(threshold: Float)
}
