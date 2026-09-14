package com.atpdev.paltoscan.features.result

import android.graphics.Bitmap
import timber.log.Timber
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atpdev.paltoscan.data.local.entity.History
import com.atpdev.paltoscan.features.history.adapter.HistoryItem
import java.io.File
import java.lang.ref.WeakReference

/*class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val _bitmap = MutableLiveData<Bitmap>()
    val bitmap: LiveData<Bitmap> = _bitmap

    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
    }
}*/
class SharedViewModel : ViewModel() {
    // class SharedViewModel(application: Application) : AndroidViewModel(application) {
    // MutableLiveData para el Bitmap
    private val _bitmap = MutableLiveData<Bitmap>()
    val bitmap: LiveData<Bitmap> = _bitmap
    private val _bitmapPath = MutableLiveData<String>()
    val bitmapPath: LiveData<String> = _bitmapPath

    private val _heatmapBitmap = MutableLiveData<Bitmap?>()
    val heatmapBitmap: LiveData<Bitmap?> = _heatmapBitmap

    private var weakBitmap: WeakReference<Bitmap>? = null

    // MutableLiveData para el historial seleccionado
    private val _selectedHistoryItem = MutableLiveData<History?>()
    val selectedHistoryItem: LiveData<History?> get() = _selectedHistoryItem

    private val _compressedImage = MutableLiveData<File?>()
    val compressedImage: LiveData<Uri?> = MutableLiveData()

    private val _historyList = MutableLiveData<List<HistoryItem>>()
    val historyList: LiveData<List<HistoryItem>> = _historyList

    // val compressedImage: LiveData<File> = _compressedImage

    // Flag para controlar el procesamiento de imágenes
    private val _isImageProcessed = MutableLiveData<Boolean>()
    val isImageProcessed: LiveData<Boolean> = _isImageProcessed

    /*fun setCompressedImage(file: File) {
        _compressedImage.value = file
    }*/
    fun setCompressedImage(file: File) {
        // val uri = Uri.fromFile(file) // Convertir archivo a Uri
        // (compressedImage as MutableLiveData).value = uri
        file?.let {
            val uri = Uri.fromFile(it)
            (compressedImage as MutableLiveData).value = uri
        }
        /*val uri = Uri.fromFile(file)
        if (uri != null) {
            // loadImageEfficiently(uri)
            (compressedImage as MutableLiveData).value = uri
        } else {
            Timber.tag("ImageLoading").e("Invalid file or URI.")
        }*/
    }

    // Método para actualizar el Bitmap
    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
    }

    fun setBitmapPath(path: String) {
        _bitmapPath.value = path
    }

    fun setHeatmapBitmap(bitmap: Bitmap?) {
        _heatmapBitmap.value = bitmap
    }

    // Método para actualizar el HistoryItem seleccionado
    fun setSelectedHistoryItem(history: History?) {
        if (history != null && history.diseaseName.isNotEmpty()) {
            _selectedHistoryItem.value = history
            Timber.tag("SharedViewModel").d("Selected History Updated: $history")
        } else {
            _selectedHistoryItem.value = null
            Timber.tag("SharedViewModel").d("Selected History Cleared")
        }

        /*if (history.diseaseName.isNotEmpty()) {
            //_selectedHistoryItem.value = history
            _selectedHistoryItem.postValue(history)
            Timber.tag("SharedViewModel").d("Selected History Updated: $history")
        }*/
    }

    // Clear method to reset state if needed
    fun updateHistoryList(historyList: List<HistoryItem>) {
        _historyList.value = historyList
    }

    fun clearSelectedHistory() {
        _selectedHistoryItem.value = null
    }

    fun setImageProcessed(processed: Boolean) {
        _isImageProcessed.value = processed
    }

    // Función para resetear el estado
    fun reset() {
        _isImageProcessed.value = false
        _compressedImage.value = null
        _selectedHistoryItem.value = null
        _heatmapBitmap.value = null
    }

    /*private fun loadImageEfficiently(uri: Uri) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val targetWidth = 1024
            val scale = kotlin.math.max(1, kotlin.math.min(
                options.outWidth / targetWidth,
                options.outHeight / targetWidth
            ))

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, loadOptions)?.let { bitmap ->
                    weakBitmap = WeakReference(bitmap)
                    setBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            Timber.tag("ImageLoading").e("Error loading image: ${e.message}")
        }
    }*/
}
