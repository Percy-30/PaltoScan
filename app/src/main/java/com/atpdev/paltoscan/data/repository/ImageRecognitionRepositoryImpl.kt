package com.atpdev.paltoscan.data.repository

import android.content.Context
import timber.log.Timber
import android.graphics.Bitmap
import android.graphics.Color
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
        private var detectionThreshold = 0.55f

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

        private fun isLikelyAvocadoLeaf(bitmap: Bitmap): Boolean {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 10 || height < 10) return false

            val startX = (width * 0.15).toInt()
            val startY = (height * 0.15).toInt()
            val cropW = (width * 0.70).toInt().coerceAtLeast(1)
            val cropH = (height * 0.70).toInt().coerceAtLeast(1)

            val centerCrop = Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
            val scaled = Bitmap.createScaledBitmap(centerCrop, 64, 64, true)

            var foliagePixels = 0
            val totalPixels = 64 * 64

            for (y in 0 until 64) {
                for (x in 0 until 64) {
                    val pixel = scaled.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    val maxC = Math.max(r, Math.max(g, b))
                    val minC = Math.min(r, Math.min(g, b))
                    val chroma = maxC - minC

                    // 1. Verde vegetal foliar: verde superior a azul y rojo con saturación
                    val isGreen = (g >= r * 0.90f) && (g >= b * 1.10f) && (chroma > 14) && (g > 35)
                    // 2. Tejido necrótico / daño foliar (manchas bronceadas o pardas características del palto):
                    val isBrownLeaf = (r >= b * 1.20f) && (g >= b * 1.05f) && (chroma > 18) && (maxC < 225) && (r > 40)

                    if (isGreen || isBrownLeaf) {
                        foliagePixels++
                    }
                }
            }

            val foliageRatio = foliagePixels.toFloat() / totalPixels
            Timber.tag("ImageRecognitionRepository").d("Foliage ratio detectado: ${foliageRatio * 100}%")
            return foliageRatio >= 0.10f // Al menos 10% de características foliares en el centro
        }

        override suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult {
            return withContext(Dispatchers.IO) {
                try {
                    // Validar si la imagen contiene tejido foliar vegetal
                    if (!isLikelyAvocadoLeaf(bitmap)) {
                        Timber.tag("ImageRecognitionRepository").w("La muestra no parece ser una hoja de palto")
                        return@withContext RecognitionResult(
                            "No detectado",
                            0.0f,
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE,
                        )
                    }

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
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
                    val secondDisease = classificationResult.secondLabel
                    val secondProb = classificationResult.secondConfidence

                    // Si la confianza es menor al umbral base (0.55) o no se reconoce
                    if (isUnknown || maxProbability < detectionThreshold) {
                        Timber.tag("ImageRecognitionRepository").d("Resultado poco concluyente (< $detectionThreshold)")
                        return@withContext RecognitionResult(
                            "No detectado",
                            maxProbability,
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE,
                            secondDiseaseName = secondDisease,
                            secondProbability = secondProb,
                            isLowConfidence = true,
                        ).apply { heatmapBitmap = classificationResult.heatmap }
                    }

                    // Caso especial: Hoja clasificada como "Healthy" pero con confianza < 0.65
                    // Para evitar falsos negativos en hojas con daño evidente (ej. necrosis o quemaduras)
                    val isHealthyWithLowConfidence = (diseaseName == "Healthy" && maxProbability < 0.65f)
                    val isLowConfidence = maxProbability < 0.65f

                    if (isHealthyWithLowConfidence) {
                        Timber.tag("ImageRecognitionRepository").w("Hoja clasificada como Healthy con baja confianza (${maxProbability * 100}%), marcando como Diagnóstico Incierto")
                        return@withContext RecognitionResult(
                            diseaseName = "Diagnóstico Incierto",
                            probability = maxProbability,
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE,
                            secondDiseaseName = secondDisease,
                            secondProbability = secondProb,
                            isLowConfidence = true,
                        ).apply { heatmapBitmap = classificationResult.heatmap }
                    }

                    Timber.tag("ImageRecognitionRepository").d("Probabilidad: $maxProbability, Resultado final: $diseaseName, Segunda opción: $secondDisease ($secondProb)")
                    return@withContext RecognitionResult(
                        diseaseName = diseaseName,
                        probability = maxProbability,
                        status = com.atpdev.paltoscan.domain.model.RecognitionStatus.SUCCESS,
                        secondDiseaseName = secondDisease,
                        secondProbability = secondProb,
                        isLowConfidence = isLowConfidence,
                    ).apply { heatmapBitmap = classificationResult.heatmap }
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
