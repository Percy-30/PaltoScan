package com.atpdev.paltoscan.data.repository

import android.content.Context
import timber.log.Timber
import android.graphics.Bitmap
import com.atpdev.paltoscan.core.ml.TensorFlowHelper
import com.atpdev.paltoscan.domain.model.RecognitionResult
import com.atpdev.paltoscan.domain.repository.ImageRecognitionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImageRecognitionRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ImageRecognitionRepository {
        private lateinit var tensorflowHelper: TensorFlowHelper
        private val classNames: List<String> = loadLabels()
        private var detectionThreshold = 0.70f // Umbral ajustado al 70% para marcar como INCONCLUSIVE
    /*# Probabilidades predichas por el modelo
    predicted_probabilities = [0.1, 0.4, 0.35, 0.8, 0.95, 0.2]
    # Etiquetas reales
    true_labels = [0, 0, 1, 1, 1, 0]*/

        init {
            tensorflowHelper = TensorFlowHelper(context)
        }

        override fun setDetectionThreshold(threshold: Float) {
            if (threshold in 0.0f..1.0f) {
                detectionThreshold = threshold
            } else {
                throw IllegalArgumentException("El umbral debe estar entre 0.0 y 1.0")
            }
        }

        private fun loadLabels(): List<String> {
            return context.assets.open("ml/labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.toList()
            }
        }

        override suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult {
            return withContext(Dispatchers.IO) {
                try {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true) // Cambia a 512 - 250
                    Timber.tag("ImageRecognitionRepository").d("Imagen escalada: ${scaledBitmap.width}x${scaledBitmap.height}")

                    // Ejecutar inferencia
                    val (isUnknown, classificationResult) = tensorflowHelper.runInferenceWithUnknownDetection(scaledBitmap)
                    Timber.tag("ImageRecognitionRepository").d("Resultado de la inferencia: ${classificationResult.label}")

                    if (isUnknown && classificationResult.label == "Error de modelo") {
                        Timber.tag("ImageRecognitionRepository").e("La inferencia no produjo resultados válidos")
                        throw ImageRecognitionException("La inferencia no produjo resultados")
                    }

                    // Obtener la probabilidad más alta
                    val maxProbability = classificationResult.confidence
                    val diseaseName = classificationResult.label

                    // Si la probabilidad más alta está por debajo del umbral de detección, se considera "Inconclusive"
                    return@withContext if (maxProbability < detectionThreshold) {
                        Timber.tag("ImageRecognitionRepository").d("Resultado poco concluyente")
                        RecognitionResult(
                            diseaseName,
                            maxProbability,
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE,
                        ).apply { heatmapBitmap = classificationResult.heatmap }
                    } else {
                        Timber.tag("ImageRecognitionRepository").d("Probabilidad: $maxProbability, Resultado final: $diseaseName")
                        RecognitionResult(
                            diseaseName, 
                            maxProbability, 
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.SUCCESS
                        ).apply { heatmapBitmap = classificationResult.heatmap }
                    }
                } catch (e: ImageRecognitionException) {
                    Timber.tag("ImageRecognitionRepository").e("Error al ejecutar la inferencia: ${e.message}")
                    return@withContext RecognitionResult("Error", 0f, status = com.atpdev.paltoscan.domain.model.RecognitionStatus.ERROR)
                } catch (e: Exception) {
                    Timber.tag("ImageRecognitionRepository").e("Error al ejecutar la inferencia: ${e.message}")
                    return@withContext RecognitionResult("Error", 0f, status = com.atpdev.paltoscan.domain.model.RecognitionStatus.ERROR)
                }
            }
        }

        class ImageRecognitionException(message: String?) : Exception(message)
    }
