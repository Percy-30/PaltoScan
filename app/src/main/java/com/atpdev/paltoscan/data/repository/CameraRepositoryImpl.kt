package com.atpdev.paltoscan.data.repository

import android.Manifest
import timber.log.Timber
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.atpdev.paltoscan.domain.repository.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale

class CameraRepositoryImpl(
    private val activity: FragmentActivity,
) : CameraRepository {
    companion object {
        private const val TAG = "CameraRepository"
    }

    private var imageCapture: ImageCapture? = null
    private var lastCapturedImageUri: Uri? = null

    private val _cameraState = MutableStateFlow<CameraRepository.CameraState>(CameraRepository.CameraState.Idle)
    override val cameraState: StateFlow<CameraRepository.CameraState> = _cameraState

    override fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    override fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        if (!isCameraPermissionGranted()) {
            _cameraState.value = CameraRepository.CameraState.Error("Permiso de cámara denegado")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview =
                    Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )

                _cameraState.value = CameraRepository.CameraState.Preview
            } catch (e: Exception) {
                _cameraState.value =
                    CameraRepository.CameraState.Error("Error al iniciar la cámara: ${e.message}")
                Timber.tag(TAG).e(e, "Error al vincular casos de uso de la cámara")
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    override fun captureImage(
        onImageCaptured: (Uri) -> Unit,
        onError: (String) -> Unit,
    ) {
        val imageCapture =
            imageCapture ?: run {
                onError("La captura de imagen no está configurada")
                return
            }

        val fileName =
            "PREVIEW_" +
                SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US,
                ).format(System.currentTimeMillis()) + ".jpg"

        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraApp/Preview")
            }

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                activity.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        lastCapturedImageUri = uri
                        _cameraState.value = CameraRepository.CameraState.ImageCaptured(uri)
                        onImageCaptured(uri)
                    } ?: onError("Error: URI vacía")
                }

                override fun onError(exception: ImageCaptureException) {
                    _cameraState.value =
                        CameraRepository.CameraState.Error("Error al capturar: ${exception.message}")
                    onError("Error al capturar: ${exception.message}")
                }
            },
        )
    }

    override fun saveImage(
        onImageSaved: (Uri) -> Unit,
        onError: (String) -> Unit,
    ) {
        val capturedUri =
            lastCapturedImageUri ?: run {
                onError("No hay imagen capturada para guardar")
                return
            }

        val fileName =
            "IMG_" +
                SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US,
                ).format(System.currentTimeMillis()) + ".jpg"

        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            }

        try {
            val inputStream = activity.contentResolver.openInputStream(capturedUri)
            val outputUri =
                activity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                )

            outputUri?.let { uri ->
                activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream?.use { input -> input.copyTo(outputStream) }
                }
                activity.contentResolver.delete(capturedUri, null, null)
                lastCapturedImageUri = null
                _cameraState.value = CameraRepository.CameraState.ImageSaved(uri)
                onImageSaved(uri)
            } ?: onError("Error al guardar la imagen")
        } catch (e: Exception) {
            onError("Error al guardar la imagen: ${e.message}")
        }
    }
}
