package com.atpdev.paltoscan.features.camera

import android.graphics.drawable.BitmapDrawable
import timber.log.Timber
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
// import com.example.imagerecognitionapp.Manifest
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.domain.repository.CameraRepository
import com.atpdev.paltoscan.databinding.FragmentCameraBinding
import com.atpdev.paltoscan.core.common.MenuToolbar
import com.atpdev.paltoscan.core.ui.FragmentAlertDialog
import com.atpdev.paltoscan.core.ui.FragmentAlertDialogExit
import com.atpdev.paltoscan.features.recognition.RecognitionViewModel
import com.atpdev.paltoscan.core.utils.sharePaltoScanApp
import android.graphics.Bitmap
import android.graphics.Color
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class FragmentCamera : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private val aboutDialog: FragmentAlertDialog? = null

    private lateinit var cameraExecutor: ExecutorService

    // private lateinit var cameraRepository: CameraRepository
    private var isCleaningUp = false
    private var camera: Camera? = null
    private var isImageCaptured = false
    private var isCapturing = false
    private var leafDetectionJob: Job? = null
    private var consecutiveLeafDetections = 0
    private val REQUIRED_CONSECUTIVE_DETECTIONS = 4 // ~1 segundo de estabilidad (4 x 250ms)
    private val viewModel: RecognitionViewModel by viewModels()

    private lateinit var menuHandler: MenuToolbar
    private lateinit var info: FragmentAlertDialog

    lateinit var lottieAnimation: LottieAnimationView

    @Inject
    lateinit var cameraRepository: CameraRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar Toolbar
        startMenu()
        setupCamera()
        setupUI()
        observeCameraState()

        EnabledRetroceso()
    }

    private fun EnabledRetroceso() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = true
                    CloseMenuFlotant()
                }
            },
        )
    }

    // ********MENU
    private fun startMenu() {
        menuHandler =
            MenuToolbar(
                context = requireContext(),
                onHistoryClick = { handleHistoryClick() },
                onAboutClick = { navigateToAlertDialog() },
                onShareClick = { sharePaltoScanApp() },
                onExitClick = { showExitConfirmationDialog() },
                // onExitClick = { requireActivity().finish() }
            )

        // Configurar la Toolbar
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.appBarMenu.toolbar)
            supportActionBar?.apply {
                title = "Reconocimiento"
                setDisplayHomeAsUpEnabled(true)
            }
        }
        setHasOptionsMenu(true)
    }

    private fun showExitConfirmationDialog() {
        FragmentAlertDialogExit.newInstance(
            title = getString(R.string.exit_app_title),
            message = getString(R.string.exit_app_message),
            positiveText = getString(R.string.exit),
            negativeText = getString(R.string.cancel),
            onPositive = {
                cleanupResources()
                requireActivity().finishAffinity()
            },
            onNegative = {
                // No hacer nada o puedes agregar lógica adicional
            },
        ).show(parentFragmentManager, "ExitDialog")
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        menuHandler.onCreateOptionsMenu(menu, inflater)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Solo manejamos el botón de navegación hacia atrás aquí
        // Todo lo demás se delega al menuHandler
        return when (item.itemId) {
            android.R.id.home -> {
                navigateToRecognitionFragment()
                true
            }
            else -> menuHandler.onOptionsItemSelected(item)
        }
    }

    private fun handleHistoryClick() {
        // Implementa aquí la lógica para el historial
        // Toast.makeText(requireContext(), "Historial seleccionado", Toast.LENGTH_SHORT).show()
        showToastError("No se puede navegar al Historial")
        // findNavController().navigate(R.id.action_fragmentCamera_to_historyFragment)
    }

    // *********ALERT DIALOG
    private fun navigateToAlertDialog() {
        try {
            findNavController().navigate(R.id.action_fragmentCamera_to_fragmentAlertDialog)
        } catch (e: Exception) {
            Timber.tag("Navigation").e("Error navigating to AlertDialog: ${e.message}")
            Toast.makeText(requireContext(), "Error al mostrar el diálogo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun CloseMenuFlotant() {
        // viewModel.btnAddMenu()
        viewModel.resetFabMenuState() // Reiniciar el estado del menú flotante
        // viewModel.btnOpenCamera()
    }

    // Pasar de fragment en fragment
    private fun navigateToRecognitionFragment() {
        if (isCleaningUp) return // Evitar múltiples llamadas
        isCleaningUp = true
        CloseMenuFlotant()
        showToastError("No se guardo la foto")
        lifecycleScope.launch {
            try {
                // 1. Liberar recursos de la cámara primero
                cleanupResources()

                // 2. Esperar un frame para asegurar la liberación
                withContext(Dispatchers.Main) {
                    // 3. Navegar después de limpiar
                    findNavController().navigate(R.id.action_fragmentCamera_to_recognitionFragment)
                }
            } catch (e: Exception) {
                Timber.tag("Navigation").e(e, "Error navigating")
            } finally {
                isCleaningUp = false
            }
        }
    }

    // *****CAMERA
    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        // cameraRepository = CameraRepository(requireActivity())
        startCamera()
    }

    private fun setupUI() {
        with(binding) {
            cameraPreview.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            overlayGuide.visibility = View.VISIBLE
            linearEdit.visibility = View.GONE

            btnTakePhoto.setOnClickListener { captureImage() }
        }
    }

    private fun startCamera() {
        cameraRepository.startCamera(binding.cameraPreview, viewLifecycleOwner)
        startLeafDetection()
    }

    private fun startLeafDetection() {
        leafDetectionJob?.cancel()
        leafDetectionJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(250)
                    if (isCapturing) continue

                    val previewView = _binding?.cameraPreview ?: continue
                    val bitmap = withContext(Dispatchers.Main) {
                        try {
                            previewView.bitmap
                        } catch (e: Exception) {
                            null
                        }
                    } ?: continue

                    val hasLeaf = checkLeafInGuide(bitmap)

                    withContext(Dispatchers.Main) {
                        if (_binding == null || isCapturing) return@withContext

                        if (hasLeaf) {
                            consecutiveLeafDetections++
                            // Teñir silueta guía en verde esmeralda para indicar detección exitosa
                            binding.overlayGuide.setColorFilter(Color.parseColor("#4CAF50"))

                            if (consecutiveLeafDetections >= REQUIRED_CONSECUTIVE_DETECTIONS) {
                                consecutiveLeafDetections = 0
                                Timber.tag("FragmentCamera").d("¡Hoja detectada de forma estable! Disparo automático iniciado")
                                captureImage()
                            }
                        } else {
                            consecutiveLeafDetections = 0
                            // Restablecer color blanco original de la silueta
                            binding.overlayGuide.clearColorFilter()
                        }
                    }
                }
            }
        }
    }

    private fun checkLeafInGuide(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 20 || height < 20) return false

        val startX = (width * 0.22).toInt()
        val startY = (height * 0.20).toInt()
        val cropW = (width * 0.56).toInt().coerceAtLeast(1)
        val cropH = (height * 0.60).toInt().coerceAtLeast(1)

        val centerCrop = Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(centerCrop, 48, 48, false)
        if (centerCrop != bitmap) {
            centerCrop.recycle()
        }

        var foliagePixels = 0
        val totalPixels = 48 * 48

        for (y in 0 until 48) {
            for (x in 0 until 48) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val maxC = Math.max(r, Math.max(g, b))
                val minC = Math.min(r, Math.min(g, b))
                val chroma = maxC - minC

                val isGreen = (g >= r * 0.90f) && (g >= b * 1.10f) && (chroma > 14) && (g > 35)
                val isBrownLeaf = (r >= b * 1.20f) && (g >= b * 1.05f) && (chroma > 18) && (maxC < 225) && (r > 40)

                if (isGreen || isBrownLeaf) {
                    foliagePixels++
                }
            }
        }
        scaled.recycle()

        val foliageRatio = foliagePixels.toFloat() / totalPixels
        return foliageRatio >= 0.25f
    }

    private fun captureImage() {
        if (isCapturing) return
        isCapturing = true

        try {
            com.atpdev.paltoscan.core.utils.HapticHelper.vibrateSuccess(requireContext())
        } catch (e: Exception) {
            Timber.tag("Camera").e(e, "Error al activar feedback háptico")
        }

        cameraRepository.captureImage(
            onImageCaptured = { uri ->
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        leafDetectionJob?.cancel()
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("image_uri", uri.toString())
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("auto_process", true)
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Timber.tag("FragmentCamera").e(e, "Error al regresar de cámara")
                        isCapturing = false
                    }
                }
            },
            onError = { error ->
                isCapturing = false
                showError(error)
            },
        )
    }

    private fun showToastError(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastError).show()
    }

    private fun showError(message: String) {
        viewModel.setCameraError(message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun observeCameraState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cameraRepository.cameraState.collect { state ->
                    when (state) {
                        is CameraRepository.CameraState.Error -> showError(state.message)
                        is CameraRepository.CameraState.ImageCaptured -> {
                            // La imagen se ha capturado, mostrar la vista previa
                        }
                        is CameraRepository.CameraState.ImageSaved -> {
                            // La imagen se ha guardado, navegar de vuelta
                        }
                        else -> { /* No action needed */ }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CloseMenuFlotant()
        cleanupResources()
        // cameraExecutor.shutdown()
        _binding = null
    }

    private fun cleanupResources() {
        try {
            // 0. Cancelar detección automática de hoja
            leafDetectionJob?.cancel()
            leafDetectionJob = null
            isCapturing = false

            // 1. Liberar recursos de CameraRepository
            // 2. Liberar recursos de CameraRepository (sin shutdown)
            if (::cameraRepository.isInitialized) {
                // Alternativa para Meerkat 24.3.1
                try {
                    // Usamos reflexión como último recurso para acceder a campos internos
                    val providerField = cameraRepository::class.java.getDeclaredField("cameraProvider")
                    providerField.isAccessible = true
                    val provider = providerField.get(cameraRepository) as? ProcessCameraProvider
                    provider?.unbindAll()
                    Timber.tag("Camera").d("CameraProvider liberado")

                    val imageCaptureField = cameraRepository::class.java.getDeclaredField("imageCapture")
                    imageCaptureField.isAccessible = true
                    imageCaptureField.set(cameraRepository, null)
                    Timber.tag("Camera").d("ImageCapture liberado")
                } catch (e: NoSuchFieldException) {
                    Timber.tag("Camera").e(e, "Campo no encontrado en CameraRepository")
                } catch (e: IllegalAccessException) {
                    Timber.tag("Camera").e(e, "Acceso ilegal al campo")
                }
            }

            // 2. Detener y liberar el ExecutorService
            if (::cameraExecutor.isInitialized) {
                cameraExecutor.shutdownNow()
                Timber.tag("Camera").d("CameraExecutor shut down")
            }

            // 3. Liberar recursos de la vista previa de la cámara
            // binding.cameraPreview.surfaceProvider = null
            binding.cameraPreview.controller = null

            // 4. Liberar recursos de la imagen de vista previa
            binding.imagePreview.setImageURI(null)
            (binding.imagePreview.drawable as? BitmapDrawable)?.bitmap?.recycle()

            // 5. Liberar animaciones Lottie si existen
            if (::lottieAnimation.isInitialized) {
                lottieAnimation.cancelAnimation()
                lottieAnimation.setImageDrawable(null)
            }

            // 6. Liberar binding
            _binding = null

            // 7. Resetear estado
            isImageCaptured = false
            camera = null

            Timber.tag("Camera").d("All resources cleaned up")
        } catch (e: Exception) {
            Timber.tag("Camera").e(e, "Error during cleanup")
        }
    }
}
