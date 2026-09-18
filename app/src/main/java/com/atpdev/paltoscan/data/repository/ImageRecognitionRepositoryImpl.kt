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
            var bluePixels = 0
            val totalPixels = sampleSize * sampleSize
            val isFoliage = BooleanArray(totalPixels)
            val luminance = FloatArray(totalPixels)

            val greenHues = ArrayList<Float>()
            val greenSats = ArrayList<Float>()
            val hsv = FloatArray(3)

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
                    val exG = (2 * g) - r - b

                    // 0. Detección de objetos azules sintéticos no agrícolas (colchón, ropa, alfombra)
                    if (b >= g * 1.15f && b >= r * 1.15f && b >= 50) {
                        bluePixels++
                    }

                    // 1. Verde vegetal foliar (clorofila viva o clorótica, compatible con luz natural y de interior):
                    val isGreenLeaf = (g >= b * 1.08f) && (g >= r * 0.92f) && (exG > 5) && (chroma >= 8) && (g in 25..245)

                    // 2. Tejido necrótico / daño foliar (manchas pardas, tizón o necrosis de palto):
                    val isBrownLeaf = (r >= b * 1.12f) && (r >= g * 0.75f) && (g >= b * 0.95f) &&
                                      (r in 28..165) && (r - g <= 50) && (chroma >= 8)

                    if (isGreenLeaf || isBrownLeaf) {
                        foliagePixels++
                        isFoliage[idx] = true

                        if (isGreenLeaf) {
                            Color.colorToHSV(pixel, hsv)
                            greenHues.add(hsv[0]) // Hue 0..360
                            greenSats.add(hsv[1] * 100f) // Sat 0..100
                        }
                    }
                }
            }

            val foliageRatio = foliagePixels.toFloat() / totalPixels
            val blueRatio = bluePixels.toFloat() / totalPixels
            Timber.tag("ImageRecognitionRepository").d("Foliage ratio: ${foliageRatio * 100}%, Blue ratio: ${blueRatio * 100}%")

            // Requisito 1: La muestra vegetal debe ocupar al menos el 12% del área central (permite hojas aisladas sobre pisos/mesas)
            if (foliageRatio < 0.12f) {
                Timber.tag("ImageRecognitionRepository").w("Rechazado: Cobertura foliar insuficiente (${foliageRatio * 100}%)")
                return false
            }

            // Requisito 2: No debe haber elementos azules sintéticos prominentes (> 5% del encuadre)
            if (blueRatio > 0.05f) {
                Timber.tag("ImageRecognitionRepository").w("Rechazado: Contiene objetos azules sintéticos (${blueRatio * 100}%)")
                return false
            }

            // Requisito 3: Detección de telas / mantas teñidas sintéticas (fleece, ropa verde, paredes)
            // Las telas sintéticas poseen una saturación artificialmente alta (>55%) combinada con dispersión de tono casi nula (hueStd < 5°).
            if (greenSats.isNotEmpty()) {
                val meanSat = greenSats.average().toFloat()
                val meanHue = greenHues.average().toFloat()
                var sumSqDiffHue = 0.0
                for (h in greenHues) {
                    val diff = (h - meanHue).toDouble()
                    sumSqDiffHue += diff * diff
                }
                val hueStdDev = Math.sqrt(sumSqDiffHue / greenHues.size).toFloat()

                Timber.tag("ImageRecognitionRepository").d("Green Sat: $meanSat%, Hue stdDev: $hueStdDev deg")

                val isSyntheticCloth = (meanSat > 55.0f && hueStdDev < 5.0f) ||
                                       (meanSat > 58.0f && foliageRatio > 0.70f && hueStdDev < 8.0f)

                if (isSyntheticCloth) {
                    Timber.tag("ImageRecognitionRepository").w("Rechazado: Superficie sintética teñida (Sat: $meanSat%, Hue std: $hueStdDev°)")
                    return false
                }
            }

            // Requisito 4: Variación de textura para evitar gráficos digitales o superficies 100% planas
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

                        if (gradMagnitude > 40f) {
                            edgeCount++
                        }
                    }
                }
            }

            if (foliageTested > 0) {
                val edgeDensity = edgeCount.toFloat() / foliageTested
                val meanLum = sumLum / foliageTested
                val varianceLum = (sumSqLum / foliageTested) - (meanLum * meanLum)
                val stdDevLum = Math.sqrt(Math.max(0.0, varianceLum))

                if (edgeDensity < 0.010f && stdDevLum < 8.0) {
                    Timber.tag("ImageRecognitionRepository").w("Rechazado: Superficie completamente plana sin textura foliar (edges: ${edgeDensity * 100}%, stdDev: $stdDevLum)")
                    return false
                }
            }

            return true
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
