package com.atpdev.paltoscan.core.ml

import android.content.Context
import timber.log.Timber
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val heatmap: Bitmap? = null,
    val secondLabel: String? = null,
    val secondConfidence: Float = 0f,
)

/**
 * Clase que ayuda a utilizar el modelo de TensorFlow Lite.
 */
class TensorFlowHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private lateinit var interpreter: Interpreter
        private lateinit var inputTensor: Tensor
        private lateinit var outputTensor: Tensor
        private val model: Interpreter
        private val inputSize = 224 // MobileNetV3Large - entrada 224x224 (confirmado por el modelo exportado)

        private var error: String? = null

        private val classNames: List<String> by lazy { loadLabels() }

        companion object {
            private const val NUM_CLASSES = 11 // PaltoScan: 11 clases de enfermedades del palto (MobileNetV3Large)
            private const val THRESHOLD = 0.5f // Umbral de confianza mínima para aceptar una predicción
        }

        init {
            // Modelo principal de PaltoScan: MobileNetV3Large o MobileNetV2 entrenado con 11 clases
            var modelFile: ByteBuffer? = null
            val candidates = listOf(
                "ml/paltoscan_MobileNetV3Large_Inicial.tflite",
            )
            for (candidate in candidates) {
                try {
                    modelFile = FileUtil.loadMappedFile(context, candidate)
                    Timber.tag("TensorFlowHelper").d("Cargado modelo: $candidate (11 clases)")
                    break
                } catch (e: Exception) {
                    Timber.tag("TensorFlowHelper").w("No se pudo cargar $candidate, intentando siguiente...")
                }
            }
            if (modelFile == null) {
                throw IllegalStateException("No se pudo cargar ningún modelo TFLite de los assets.")
            }
            model = Interpreter(modelFile)
        }

        fun getInputShape(): IntArray {
            return inputTensor.shape()
        }

        private fun loadLabels(): List<String> {
            return context.assets.open("ml/labels.txt").bufferedReader().useLines { lines ->
                lines.toList()
            }
        }

        private fun loadModel() {
            if (!::interpreter.isInitialized) {
                val model = loadModelFile(context, "model_mobilenet.tflite")
                interpreter = Interpreter(model)

                inputTensor = interpreter.getInputTensor(0)
                outputTensor = interpreter.getOutputTensor(0)

                val outputShape = outputTensor.shape()
                if (outputShape.size != 2 || outputShape[1] != NUM_CLASSES) {
                    throw RuntimeException("La forma del tensor de salida no es la esperada. Forma: ${outputShape.joinToString()}")
                }
            }
        }

        private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val byteBuffer =
                ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
                    order(ByteOrder.nativeOrder())
                    rewind()
                }

            for (y in 0 until inputSize) {
                for (x in 0 until inputSize) {
                    val pixel = scaledBitmap.getPixel(x, y)
                    byteBuffer.putFloat(Color.red(pixel).toFloat())
                    byteBuffer.putFloat(Color.green(pixel).toFloat())
                    byteBuffer.putFloat(Color.blue(pixel).toFloat())
                }
            }
            return byteBuffer
        }

        fun runInference(bitmap: Bitmap): FloatArray {
            // Preprocesar la imagen
            val inputImage = preprocessBitmap(bitmap)

            // Salida del modelo — 11 clases (PaltoScan MobileNetV3Large)
            val outputBuffer =
                TensorBuffer.createFixedSize(
                    intArrayOf(1, NUM_CLASSES),
                    DataType.FLOAT32,
                ) // [1, 11] — coincide con output TensorSpec shape=(None, 11)
            // val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)  // Asegúrate de que la forma coincida con el modelo

            // Ejecutar la inferencia
            model.run(inputImage, outputBuffer.buffer.rewind())

            // Obtener el arreglo de resultados
            return outputBuffer.floatArray
        }

        suspend fun runInferenceWithUnknownDetection(bitmap: Bitmap): Pair<Boolean, ClassificationResult> {
            val inputBuffer = preprocessBitmap(bitmap)
            
            val outputCount = model.outputTensorCount
            
            var predictionBuffer: TensorBuffer? = null
            var featureMapBuffer: TensorBuffer? = null
            var predictionIndex = 0
            var featureMapIndex = -1
            
            // Determinar qué salida es cuál basándose en la forma
            for (i in 0 until outputCount) {
                val shape = model.getOutputTensor(i).shape()
                if (shape.size == 2) { // [1, 4]
                    predictionIndex = i
                    predictionBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
                } else if (shape.size == 4) { // [1, 8, 8, 1664]
                    featureMapIndex = i
                    featureMapBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
                }
            }
            
            if (predictionBuffer == null) {
                 return Pair(true, ClassificationResult("Error de modelo", 0f))
            }
            
            // Crear el mapa de salidas
            val outputs: MutableMap<Int, Any> = HashMap()
            outputs[predictionIndex] = predictionBuffer.buffer.rewind()
            if (featureMapBuffer != null && featureMapIndex != -1) {
                outputs[featureMapIndex] = featureMapBuffer.buffer.rewind()
            }
            
            // Ejecutar el modelo
            val inputs = arrayOf<Any>(inputBuffer.rewind())
            model.runForMultipleInputsOutputs(inputs, outputs)
            
            val results = predictionBuffer.floatArray
            val sortedIndices = results.indices.sortedByDescending { results[it] }
            val maxIndex = sortedIndices.getOrNull(0) ?: 0
            val secondIndex = sortedIndices.getOrNull(1) ?: -1
            
            var heatmapBitmap: Bitmap? = null
            if (featureMapBuffer != null) {
                // Procesar Feature Map a Heatmap (CAM Simulado)
                val featureArray = featureMapBuffer.floatArray
                heatmapBitmap = generateHeatmap(featureArray, 8, 8, 1664, bitmap)
            }

            return if (results[maxIndex] > THRESHOLD) {
                Pair(false, mapResultToRecognition(results, maxIndex, secondIndex, heatmapBitmap))
            } else {
                Pair(true, ClassificationResult("No identificado", 0f, heatmapBitmap))
            }
        }
        
        private fun generateHeatmap(features: FloatArray, width: Int, height: Int, channels: Int, originalBitmap: Bitmap): Bitmap {
            val heatmap = FloatArray(width * height)
            var maxVal = Float.MIN_VALUE
            var minVal = Float.MAX_VALUE
            
            // Promediar los canales para obtener activación espacial global (CAM Simplificado)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        val index = (y * width * channels) + (x * channels) + c
                        sum += features[index]
                    }
                    val avg = sum / channels
                    heatmap[y * width + x] = avg
                    if (avg > maxVal) maxVal = avg
                    if (avg < minVal) minVal = avg
                }
            }
            
            // Crear Bitmap 8x8 con colores (Rojo = calor alto, Azul = frío)
            val heatmapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val range = maxVal - minVal
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val normalized = if (range > 0) (heatmap[y * width + x] - minVal) / range else 0f
                    // Convertir normalizado (0-1) a color (0=Azul, 1=Rojo) simplificado
                    // Un heatmap simple: de Azul -> Verde -> Amarillo -> Rojo
                    val color = calculateHeatmapColor(normalized)
                    heatmapBitmap.setPixel(x, y, color)
                }
            }
            
            // Redimensionar al tamaño original de la imagen
            val scaledHeatmap = Bitmap.createScaledBitmap(heatmapBitmap, originalBitmap.width, originalBitmap.height, true)
            
            // Combinar con la imagen original
            val combinedBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(combinedBitmap)
            canvas.drawBitmap(scaledHeatmap, 0f, 0f, null)
            
            return combinedBitmap
        }
        
        private fun calculateHeatmapColor(value: Float): Int {
            val a = 128 // 50% de opacidad para mezclar con la imagen original
            val r = (Math.max(0.0, Math.min(1.0, (1.5 - Math.abs(1.0 - 4.0 * (value - 0.5))))) * 255).toInt()
            val g = (Math.max(0.0, Math.min(1.0, (1.5 - Math.abs(1.0 - 4.0 * (value - 0.25))))) * 255).toInt()
            val b = (Math.max(0.0, Math.min(1.0, (1.5 - Math.abs(1.0 - 4.0 * value)))) * 255).toInt()
            return Color.argb(a, r, g, b)
        }

        private fun mapResultToRecognition(
            results: FloatArray,
            classIndex: Int,
            secondIndex: Int,
            heatmap: Bitmap?
        ): ClassificationResult {
            val firstLabel = if (classIndex in results.indices) classNames[classIndex] else "No identificado"
            val firstConfidence = if (classIndex in results.indices) results[classIndex] else 0f
            val secondLabel = if (secondIndex in results.indices && results[secondIndex] > 0.05f) classNames[secondIndex] else null
            val secondConfidence = if (secondIndex in results.indices) results[secondIndex] else 0f
            return ClassificationResult(
                label = firstLabel,
                confidence = firstConfidence,
                heatmap = heatmap,
                secondLabel = secondLabel,
                secondConfidence = secondConfidence
            )
        }

        private fun getOutputSize(): Int {
            // PaltoScan MobileNetV3Large — 11 clases de enfermedades del palto
            return NUM_CLASSES
        }

        fun getError(): String? {
            return error
        }

    /*private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        return try {
            context.assets.open(filename).use { inputStream ->
                val buffer = ByteBuffer.allocateDirect(inputStream.available())
                buffer.order(ByteOrder.nativeOrder())
                inputStream.read(buffer.array())
                //buffer.rewind() // Prepare the buffer for reading
                buffer
            }
            /*val fileDescriptor = context.assets.openFd(filename)
            val inputStream = fileDescriptor.createInputStream()
            val fileChannel = inputStream.channel
            val size = fileChannel.size()
            val buffer = ByteBuffer.allocateDirect(size.toInt())
            buffer.order(ByteOrder.nativeOrder())
            fileChannel.read(buffer)
            fileDescriptor.close()
            buffer.rewind()
            buffer*/
        } catch (e: IOException) {
            throw RuntimeException("Error al cargar el modelo $filename: ${e.message}", e)
        }
    }*/

        private fun loadModelFile(
            context: Context,
            filename: String,
        ): ByteBuffer {
            try {
                val fileDescriptor = context.assets.openFd(filename)
                val inputStream = fileDescriptor.createInputStream()
                val fileChannel = inputStream.channel
                val size = fileChannel.size()
                val buffer = ByteBuffer.allocateDirect(size.toInt())
                buffer.order(ByteOrder.nativeOrder())
                fileChannel.read(buffer)
                fileDescriptor.close()
                buffer.rewind()
                return buffer
            } catch (e: Exception) {
                throw RuntimeException("Error al cargar el modelo $filename: ${e.message}", e)
            }
        }

        fun close() {
            if (::interpreter.isInitialized) {
                interpreter.close()
            } else {
                Timber.tag("TensorFlowHelper").w("Interpreter no inicializado antes de cerrar.")
            }
        }

    /*fun close() {
        interpreter.close()
    }*/
    }
