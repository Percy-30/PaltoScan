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
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.Dispatchers
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
            // Inicialmente, mostrar solo la vista previa y el botón de captura
            cameraPreview.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE

            // Asegurarse de que la guía sea visible cuando la cámara está activa
            overlayGuide.visibility = View.VISIBLE

            btnSaveCancelGone()
            btnTakePhoto.setOnClickListener { captureImage() }
            btnSavePhoto.setOnClickListener { saveImage() }
            btnCancelPhoto.setOnClickListener { retakePhoto() }
        }
    }

    private fun btnSaveCancelGone() {
        with(binding) {
            btnSavePhoto.visibility = View.GONE
            btnCancelPhoto.visibility = View.GONE
        }
    }

    private fun btnSaveCancelVisibible() {
        with(binding) {
            btnSavePhoto.visibility = View.VISIBLE
            btnCancelPhoto.visibility = View.VISIBLE
        }
    }

    private fun startCamera() {
        cameraRepository.startCamera(binding.cameraPreview, viewLifecycleOwner)
    }

    private fun captureImage() {
        cameraRepository.captureImage(
            onImageCaptured = { uri ->
                showPreview(uri)
            },
            onError = { error ->
                showError(error)
            },
        )
    }

    private fun showToastError(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastError).show()
    }

    private fun saveImage() {
        // binding.lottieAnimationView.playAnimation()
        cameraRepository.saveImage(
            onImageSaved = { uri ->
                // Navegar de vuelta con el URI de la imagen
                findNavController().previousBackStackEntry?.savedStateHandle?.set("image_uri", uri.toString())
                findNavController().popBackStack()
            },
            onError = { error ->
                showError(error)
            },
        )
    }

    private fun showPreview(uri: Uri) {
        with(binding) {
            // Ocultar la vista previa de la cámara y mostrar la imagen capturada
            cameraPreview.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            overlayGuide.visibility = View.GONE // Ocultar la guía cuando se muestra la preview
            imagePreview.setImageURI(uri)

            // Cambiar los botones visibles
            btnTakePhoto.visibility = View.GONE
            btnSaveCancelVisibible()
        }
        isImageCaptured = true
    }

    private fun retakePhoto() {
        with(binding) {
            // Volver a mostrar la vista previa de la cámara
            cameraPreview.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            overlayGuide.visibility = View.VISIBLE // Mostrar la guía nuevamente

            // Restaurar los botones originales
            btnTakePhoto.visibility = View.VISIBLE
            btnSaveCancelGone()
        }
        isImageCaptured = false
        startCamera()
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
