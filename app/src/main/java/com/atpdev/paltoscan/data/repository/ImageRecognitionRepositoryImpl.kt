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
            if (width < 20 || height < 20) return false

            val startX = (width * 0.10).toInt()
            val startY = (height * 0.10).toInt()
            val cropW = (width * 0.80).toInt().coerceAtLeast(1)
            val cropH = (height * 0.80).toInt().coerceAtLeast(1)

            val centerCrop = Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
            val sampleSize = 128
            val scaled = Bitmap.createScaledBitmap(centerCrop, sampleSize, sampleSize, true)

            var foliagePixels = 0
            val totalPixels = sampleSize * sampleSize
            val isFoliage = BooleanArray(totalPixels)
            val luminance = FloatArray(totalPixels)

            for (y in 0 until sampleSize) {
                for (x in 0 until sampleSize) {
                    val idx = y * sampleSize + x
                    val pixel = scaled.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    // Luminancia estándar ITU-R BT.601
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    luminance[idx] = lum

                    val maxC = Math.max(r, Math.max(g, b))
                    val minC = Math.min(r, Math.min(g, b))
                    val chroma = maxC - minC

                    // 1. Exceso de Verde (Excess Green ExG = 2G - R - B)
                    val exG = (2 * g) - r - b
                    val ndviVis = if (g + r > 0) (g - r).toFloat() / (g + r) else 0f

                    // 2. Verde vegetal foliar real (Clorofila viva de palto):
                    val isGreenLeaf = (exG > 18) && (g >= r * 1.20f) && (g >= b * 1.15f) && (ndviVis >= 0.09f) && (chroma > 15) && (g in 35..235)

                    // 3. Tejido necrótico / daño foliar (manchas bronceadas, pardas o necróticas del palto):
                    val isBrownLeaf = (r >= b * 1.25f) && (r >= g * 0.90f) && (chroma > 16) && (r in 40..220) && (exG in -90..12)

                    if (isGreenLeaf || isBrownLeaf) {
                        foliagePixels++
                        isFoliage[idx] = true
                    }
                }
            }

            val foliageRatio = foliagePixels.toFloat() / totalPixels
            Timber.tag("ImageRecognitionRepository").d("Foliage ratio detectado: ${foliageRatio * 100}%")

            // Requisito 1: La muestra vegetal debe ocupar al menos el 20% del área central
            if (foliageRatio < 0.20f) {
                Timber.tag("ImageRecognitionRepository").w("Rechazado: Cobertura foliar insuficiente (${foliageRatio * 100}%)")
                return false
            }

            // Requisito 2: Análisis de estructura foliar y venación (Gradiente Sobel)
            // Las telas, mantas o superficies lisas tienen una textura plana sin nervaduras centrales ni secundarias.
            // Las hojas de palto reales presentan nervaduras con bordes lineales definidos.
            var edgeCount = 0
            var foliageTested = 0
            var sumLum = 0.0
            var sumSqLum = 0.0

            for (y in 1 until sampleSize - 1) {
                for (x in 1 until sampleSize - 1) {
                    val idx = y * sampleSize + x
                    if (isFoliage[idx]) {
                        foliageTested++
                        val lum = luminance[idx].toDouble()
                        sumLum += lum
                        sumSqLum += lum * lum

                        // Sobel horizontal (Gx) y vertical (Gy)
                        val gx = (luminance[(y - 1) * sampleSize + (x + 1)] + 2 * luminance[y * sampleSize + (x + 1)] + luminance[(y + 1) * sampleSize + (x + 1)]) -
                                 (luminance[(y - 1) * sampleSize + (x - 1)] + 2 * luminance[y * sampleSize + (x - 1)] + luminance[(y + 1) * sampleSize + (x - 1)])

                        val gy = (luminance[(y + 1) * sampleSize + (x - 1)] + 2 * luminance[(y + 1) * sampleSize + x] + luminance[(y + 1) * sampleSize + (x + 1)]) -
                                 (luminance[(y - 1) * sampleSize + (x - 1)] + 2 * luminance[(y - 1) * sampleSize + x] + luminance[(y - 1) * sampleSize + (x + 1)])

                        val gradMagnitude = Math.abs(gx) + Math.abs(gy)

                        // Gradiente significativo que indica nervadura o borde celular/foliar
                        if (gradMagnitude > 45f) {
                            edgeCount++
                        }
                    }
                }
            }

            if (foliageTested == 0) return false

            val edgeDensity = edgeCount.toFloat() / foliageTested
            val meanLum = sumLum / foliageTested
            val varianceLum = (sumSqLum / foliageTested) - (meanLum * meanLum)
            val stdDevLum = Math.sqrt(Math.max(0.0, varianceLum))

            Timber.tag("ImageRecognitionRepository").d("Foliage edgeDensity: ${edgeDensity * 100}%, stdDevLum: $stdDevLum")

            // Una tela/manta lisa sintética o pared verde tiene muy poca variación estructural interna
            // (edgeDensity muy baja < 0.015 o stdDevLum < 12.0)
            if (edgeDensity < 0.015f && stdDevLum < 12.0) {
                Timber.tag("ImageRecognitionRepository").w("Rechazado: Superficie sintética o plana sin nervaduras foliares (edgeDensity: ${edgeDensity * 100}%, stdDev: $stdDevLum)")
                return false
            }

            return true
        }

        private fun hasAuthenticHealthyChlorophyll(bitmap: Bitmap): Boolean {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 20 || height < 20) return false

            val startX = (width * 0.15).toInt()
            val startY = (height * 0.15).toInt()
            val cropW = (width * 0.70).toInt().coerceAtLeast(1)
            val cropH = (height * 0.70).toInt().coerceAtLeast(1)

            val centerCrop = Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
            val sampleSize = 64
            val scaled = Bitmap.createScaledBitmap(centerCrop, sampleSize, sampleSize, true)

            var chlorophyllPixels = 0
            val totalPixels = sampleSize * sampleSize

            for (y in 0 until sampleSize) {
                for (x in 0 until sampleSize) {
                    val pixel = scaled.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    val exG = (2 * g) - r - b
                    val ndviVis = if (g + r > 0) (g - r).toFloat() / (g + r) else 0f

                    // Clorofila auténtica de hoja sana de palto (Persea americana):
                    // El verde vivo supera al rojo en al menos 22% y al azul en 18%
                    if (g >= r * 1.22f && g >= b * 1.18f && exG > 22 && ndviVis >= 0.10f && g in 35..240) {
                        chlorophyllPixels++
                    }
                }
            }

            val ratio = chlorophyllPixels.toFloat() / totalPixels
            Timber.tag("ImageRecognitionRepository").d("Chlorophyll ratio para Healthy: ${ratio * 100}%")
            return ratio >= 0.25f // Una hoja sana real debe tener al menos 25% de clorofila viva en el centro
        }

        override suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult {
            return withContext(Dispatchers.IO) {
                try {
                    // Validar si la imagen contiene tejido foliar vegetal
                    if (!isLikelyAvocadoLeaf(bitmap)) {
                        Timber.tag("ImageRecognitionRepository").w("La muestra no parece ser una hoja de palto")
                        return@withContext RecognitionResult(
                            "No es una hoja",
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

                    // Validación cruzada para "Healthy":
                    // Si el modelo predijo "Healthy" (incluso con 90%+), exigimos que contenga clorofila de hoja sana real.
                    // Si es una manta polar verde, tela, ropa o pared, se descarta como "No es una hoja".
                    if (diseaseName == "Healthy" && !hasAuthenticHealthyChlorophyll(bitmap)) {
                        Timber.tag("ImageRecognitionRepository").w("Rechazo cruzado: Red predijo Healthy pero no posee clorofila auténtica de palto")
                        return@withContext RecognitionResult(
                            diseaseName = "No es una hoja",
                            probability = 0.0f,
                            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE,
                        )
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
