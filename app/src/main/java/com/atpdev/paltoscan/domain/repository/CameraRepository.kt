package com.atpdev.paltoscan.domain.repository

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

interface CameraRepository {
    val cameraState: StateFlow<CameraState>

    fun isCameraPermissionGranted(): Boolean

    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    )

    fun captureImage(
        onImageCaptured: (android.net.Uri) -> Unit,
        onError: (String) -> Unit,
    )

    fun saveImage(
        onImageSaved: (android.net.Uri) -> Unit,
        onError: (String) -> Unit,
    )

    sealed class CameraState {
        object Idle : CameraState()

        object Preview : CameraState()

        data class Error(val message: String) : CameraState()

        data class ImageCaptured(val uri: android.net.Uri) : CameraState()

        data class ImageSaved(val uri: android.net.Uri) : CameraState()
    }
}
