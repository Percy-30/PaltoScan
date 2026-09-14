package com.atpdev.paltoscan.domain.usecase

class ClassifyImageUseCase {
    /*

En este caso, la clase ImageClassifier parece ser una capa adicional que no es estrictamente necesaria, ya que solo delega la llamada a la clase ImageRecognitionRepository.
Si solo tienes una única implementación de la clase ImageRecognitionRepository, entonces la clase ImageClassifier no aporta mucho valor y podrías eliminarla.
Sin embargo, si tienes varias implementaciones de la clase ImageRecognitionRepository (por ejemplo, una para reconocimiento de imágenes locales y otra para reconocimiento de imágenes en la nube), entonces la clase ImageClassifier podría ser útil para encapsular la lógica de selección de la implementación adecuada.


// ImageRecognitionRepository.kt
interface ImageRecognitionRepository {
    suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult
}

// LocalImageRecognitionRepository.kt
class LocalImageRecognitionRepository @Inject constructor(
    private val tensorflowHelper: TensorFlowHelper
) : ImageRecognitionRepository {
    override suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult {
        // Implementa la lógica para reconocimiento de imágenes locales
        val result = tensorflowHelper.runInference(bitmap)
        val diseaseName = getDiseaseName(result)
        val probability = getProbability(result)
        return RecognitionResult(diseaseName, probability)
    }

    // ...
}

// CloudImageRecognitionRepository.kt
class CloudImageRecognitionRepository @Inject constructor(
    private val apiService: ApiService
) : ImageRecognitionRepository {
    override suspend fun getRecognitionResult(bitmap: Bitmap): RecognitionResult {
        // Implementa la lógica para reconocimiento de imágenes en la nube
        val imageBytes = bitmap.toByteArray()
        val response = apiService.recognizeDisease(imageBytes)
        return RecognitionResult(response.diseaseName, response.probability)
    }

    // ...
}

// ImageClassifier.kt
class ImageClassifier @Inject constructor(
    private val localImageRecognitionRepository: LocalImageRecognitionRepository,
    private val cloudImageRecognitionRepository: CloudImageRecognitionRepository
) {
    suspend fun classifyImage(bitmap: Bitmap, useCloud: Boolean = false): RecognitionResult {
        return if (useCloud) {
            cloudImageRecognitionRepository.getRecognitionResult(bitmap)
        } else {
            localImageRecognitionRepository.getRecognitionResult(bitmap)
        }
    }
}
     */
}
