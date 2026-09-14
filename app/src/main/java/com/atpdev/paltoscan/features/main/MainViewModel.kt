package com.atpdev.paltoscan.features.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atpdev.paltoscan.core.utils.LocationHelper
import com.atpdev.paltoscan.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        private val weatherRepository: WeatherRepository,
        private val locationHelper: LocationHelper
    ) : AndroidViewModel(application) {
        
        private val _weatherRisk = MutableLiveData<Pair<Boolean?, String>>()
        val weatherRisk: LiveData<Pair<Boolean?, String>> get() = _weatherRisk

        fun fetchWeatherRisk() {
            viewModelScope.launch {
                try {
                    val location = locationHelper.getCurrentLocation()
                    if (location != null) {
                        val risk = weatherRepository.getRiskOfLateBlight(location.latitude, location.longitude)
                        _weatherRisk.postValue(Pair(risk.first, risk.second))
                    } else {
                        _weatherRisk.postValue(Pair(null, "Activa tu GPS (Ubicación) para recibir alertas climáticas."))
                    }
                } catch (e: Exception) {
                    _weatherRisk.postValue(Pair(null, "Sin conexión y sin ubicación cacheada."))
                }
            }
        }
        // LiveData para los resultados de reconocimiento de la imagen
    /*private val _recognitionResult = MutableLiveData<RecognitionResult>()
    val recognitionResult: LiveData<RecognitionResult> get() = _recognitionResult

    private val _bitmapFlow = MutableStateFlow<Bitmap?>(null)
    val bitmapFlow = _bitmapFlow.asStateFlow()

    private val _imageProxyFlow = MutableStateFlow<ImageProxy?>(null)
    val imageProxyFlow = _imageProxyFlow.asStateFlow()

    // - 1 = inicial   0 = error,  1 = exito
    private val _operacionExitosa = MutableLiveData<Int>()
    val operacionExitosa: LiveData<Int> get() = _operacionExitosa

    fun iniciar(){
        _operacionExitosa.value = -1
    }
    // LiveData para el estado de carga
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    // LiveData para manejar errores
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    // Función para procesar la imagen y obtener el resultado
    /*fun processImage(imageFile: File) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = imageRecognitionRepository.getRecognitionResult(imageFile)
                _recognitionResult.postValue(result)
                _loading.postValue(false)
            } catch (e: Exception) {
                _errorMessage.postValue("Error: ${e.message}")
                _loading.postValue(false)
            }
        }
    }*/*/
    }
