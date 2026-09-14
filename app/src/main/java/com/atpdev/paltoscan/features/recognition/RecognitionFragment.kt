package com.atpdev.paltoscan.features.recognition

import android.app.Activity
import timber.log.Timber
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieListener
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.core.common.MenuToolbar
import com.atpdev.paltoscan.core.ml.TensorFlowHelper
import com.atpdev.paltoscan.core.ui.FragmentAlertDialogExit
import com.atpdev.paltoscan.core.utils.sharePaltoScanApp
import com.atpdev.paltoscan.databinding.FragmentRecognitionBinding
import com.atpdev.paltoscan.domain.model.RecognitionResult
import com.atpdev.paltoscan.domain.repository.CameraRepository
import com.atpdev.paltoscan.features.result.SharedViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class RecognitionFragment : Fragment() {
    private var _binding: FragmentRecognitionBinding? = null
    private val binding get() = _binding!!

    // private lateinit var cameraRepository: CameraRepository
    private lateinit var menuHandler: MenuToolbar
    private val viewModel: RecognitionViewModel by viewModels()
    private var cargeImage = false

    private val animationPool = mutableListOf<LottieComposition>()
    private var isAnimationsPreloaded = false

    // private lateinit var viewModel: RecognitionViewModel
    lateinit var tensorFlowHelper: TensorFlowHelper

    // Crear una instancia de SharedViewModel
    val sharedViewModel: SharedViewModel by activityViewModels()

    @Inject
    lateinit var cameraRepository: CameraRepository

    // Animaciones
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.to_botton_anim) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecognitionBinding.inflate(inflater, container, false)
        // Estado inicial del botón
        binding.btnProcessImage.isEnabled = false
        binding.btnProcessImage.alpha = 0.5f
        return binding.root
    }

    private val job = Job()
    private lateinit var viewModelScope: CoroutineScope
    private val customScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Princiapal Funciones
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModelScope = customScope
        try {
            tensorFlowHelper = TensorFlowHelper(requireContext())
        } catch (e: Exception) {
            Timber.tag("RecognitionFragment").e(e, "Error al inicializar TensorFlowHelper")
            android.widget.Toast.makeText(requireContext(), "Crash TF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
        startMenu()
        // setupCameraRepository()
        setupObservers()
        setupClickListenersCameraGalery()
        observeNavigationResult()
        // ProcesarImagen()

        BotonProcesar()

        preloadAnimations()

        // Agrega el código aquí
        /*viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }*/

        EnabledRetroceso()
    }

    private fun EnabledRetroceso() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = true
                }
            },
        )
    }

    private fun requestCameraPermission() {
        /*ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CameraRepository.CAMERA_PERMISSION_REQUEST_CODE
        )*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, actualizar LiveData
                viewModel.updateCameraPermissionStatus(true)
            } else {
                // Permiso denegado, actualizar LiveData
                viewModel.updateCameraPermissionStatus(false)
                showToastError("Se requiere permiso de cámara para esta función")
            }
        }
    }

    // ********MENU
    private fun startMenu() {
        menuHandler =
            MenuToolbar(
                context = requireContext(),
                onHistoryClick = { navigateToHistoryFragment() },
                onAboutClick = { navigateToAlertDialog() },
                onShareClick = { sharePaltoScanApp() },
                onExitClick = { showExitConfirmationDialog() },
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
                navigateToRecognitionFragmentMain()
                true
            }
            else -> menuHandler.onOptionsItemSelected(item)
        }
    }

    // Navegar hacia el diálogo "History"
    private fun navigateToHistoryFragment() {
        try {
            findNavController().navigate(R.id.action_recognitionFragment_to_historyFragment)
        } catch (e: Exception) {
            Timber.tag("Navigation").e("Error al navegar al diálogo: ${e.message}")
        }
    }

    // *********ALERT DIALOG
    private fun navigateToAlertDialog() {
        try {
            findNavController().navigate(R.id.action_recognitionFragment_to_fragmentAlertDialog)
        } catch (e: Exception) {
            Timber.tag("Navigation").e("Error navigating to AlertDialog: ${e.message}")
            showToastError("Error al mostrar el diálogo")
            // Toast.makeText(requireContext(), "Error al mostrar el diálogo", Toast.LENGTH_SHORT).show()
        }
    }

    // Pasar de fragment en fragment
    private fun navigateToRecognitionFragmentMain() {
        findNavController().navigate(R.id.action_recognitionFragment_to_recognitionMain)
    }

    private fun navigateToProcessImagenFragment() {
        findNavController().navigate(R.id.action_recognitionFragment_to_resultFragment)
    }

    private fun navigateToResultFragment(
        result: RecognitionResult,
        bitmap: Bitmap,
    ) {
        Timber.tag("RecognitionFragment").d("Navegando a ResultFragment")
        // Haptic Feedback de éxito
        com.atpdev.paltoscan.core.utils.HapticHelper.vibrateSuccess(requireContext())
        // Pasar el heatmap al sharedViewModel
        sharedViewModel.setHeatmapBitmap(result.heatmapBitmap)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val bundle =
                Bundle().apply {
                    putSerializable("recognitionResult", result) // Usar Serializable en lugar de Parcelable
                }
            findNavController().navigate(R.id.action_recognitionFragment_to_resultFragment, bundle)
        }, 500)
    }

    // Inicializar CameraRepository
    private fun setupCameraRepository() {
        // cameraRepository = CameraRepository(requireActivity())
    }

    private fun setupClickListenersCameraGalery() {
        // btnCamera.setOnClickListener { OpenCameraClick() }
        // btnUploadPhoto.setOnClickListener { OpenUploadClick() }
        setupMenuFlotant()
    }

    private fun CloseMenuFlotant() {
        viewModel.resetFabMenuState()
        binding.icPhotoAlternative.visibility = View.GONE
        binding.icAddAPhoto.visibility = View.GONE
    }

    private fun checkPermissionAndExecute(action: () -> Unit) {
        if (!cameraRepository.isCameraPermissionGranted()) {
            requestCameraPermission()
        } else {
            action()
        }
    }

    private fun setupMenuFlotant() {
        with(binding) {
            iccAdd.setOnClickListener { viewModel.btnAddMenu() }

            icPhotoAlternative.setOnClickListener {
                if (!cameraRepository.isCameraPermissionGranted()) {
                    viewModel.setOpenCameraAfterPermission(true)
                    requestCameraPermission()
                } else {
                    viewModel.btnUploadImage()
                    showToast("Cargar Foto")
                    OpenUploadClick()
                }
            }

            icAddAPhoto.setOnClickListener {
                if (!cameraRepository.isCameraPermissionGranted()) {
                    requestCameraPermission()
                } else {
                    viewModel.btnOpenCamera()
                    showToast("Tomar foto")
                    navigateToCameraFragment()
                }
            }

            imgPhotoPreview.setOnClickListener {
                if (cargeImage) {
                    showToastProccessImagevalidation("Ya se cargo la imagen ahora debe procesar.... O Tomar otra foto")
                    // CloseMenuFlotant()
                    // binding.imgPhotoPreview.setImageBitmap(null)
                    // cargeImage = false
                } else {
                    if (!cameraRepository.isCameraPermissionGranted()) {
                        viewModel.setOpenCameraAfterPermission(true)
                        requestCameraPermission()
                    } else {
                        showToast("Tomar foto")
                        navigateToCameraFragment()
                       /*viewModel.btnOpenCamera()
                       showToast("Tomar foto")
                       navigateToCameraFragment()*/
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        var isFirstObservation = true // Variable para evitar la animación inicial
        viewModel.isFabMenuOpen.observe(viewLifecycleOwner) { isOpen ->
            if (isFirstObservation) {
                isFirstObservation = false
            } else {
                setVisibility(isOpen)
                setAnimation(isOpen)
                setClickeable(isOpen)
            }
        }
        
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is com.atpdev.paltoscan.features.recognition.RecognitionUiState.Loading -> {
                    // La animación de carga ya se inicia al hacer click en procesar
                }
                is com.atpdev.paltoscan.features.recognition.RecognitionUiState.Error -> {
                    showToastError(state.message)
                    // Detener la animación si hay error
                    _binding?.progressBarlottieAnimationView?.apply {
                        cancelAnimation()
                        visibility = View.GONE
                    }
                    if (state.isCameraError) {
                        Timber.tag("CameraError").e(state.message)
                    }
                }
                is com.atpdev.paltoscan.features.recognition.RecognitionUiState.Success -> {
                    // La navegación ocurre manualmente tras processImage()
                    _binding?.progressBarlottieAnimationView?.apply {
                        cancelAnimation()
                        visibility = View.GONE
                    }
                }
                else -> {}
            }
        }
    }

    private fun BotonProcesar() {
        binding.btnProcessImage.setOnClickListener {
            _binding ?: return@setOnClickListener

            if (cargeImage) {
                // Iniciar animación en bucle infinito
                showRandomAnimation(loop = true) // <-- Ahora se repite
                lifecycleScope.launch {
                    delay(3000) // Espera 3 segundos
                    try {
                        // ProcesarImagen()
                        // ⏱️ Medir inicio
                        val startTime = System.currentTimeMillis()

                        ProcesarImagen() // tu función que procesa la imagen

                        // ⏱️ Medir fin
                        val endTime = System.currentTimeMillis()
                        val elapsedTime = endTime - startTime // en ms

                        // Mostrar en Log
                        Timber.tag("Performance").d("Velocidad de reconocimiento: $elapsedTime ms")

                        // Mostrar en Toast
                        // Toast.makeText(requireContext(), "Reconocimiento en $elapsedTime ms", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Timber.tag("Process").e(e, "Error")
                    }
                }
            }
        }
    }

    // Lista de nombres de archivos en assets
    private val animationFiles =
        listOf(
            "animation_loading_1.json",
            "animation_loading_2.json",
            "animation_loading_3.json",
            "animation_loading_4.json",
            "animation_loading_5.json",
            "animation_loading_6.json",
            "animation_loading_7.json",
        )

    private fun preloadAnimations() {
        if (isAnimationsPreloaded) return

        animationFiles.forEach { fileName ->
            try {
                val inputStream = requireContext().assets.open(fileName)
                LottieCompositionFactory.fromJsonInputStream(inputStream, fileName)
                    .addListener(
                        object : LottieListener<LottieComposition> {
                            override fun onResult(composition: LottieComposition?) {
                                composition?.let {
                                    animationPool.add(it)
                                }
                                inputStream.close()
                            }

                            fun onFailure(error: Throwable) {
                                Timber.tag("Lottie").e(error, "Error cargando $fileName")
                                inputStream.close()
                            }
                        },
                    )
            } catch (e: IOException) {
                Timber.tag("Lottie").e(e, "Error abriendo archivo $fileName")
            }
        }

        isAnimationsPreloaded = true
    }

    private fun getRandomAnimation(callback: (LottieComposition?) -> Unit) {
        if (animationPool.isNotEmpty()) {
            callback(animationPool.random())
        } else {
            loadFallbackAnimationAsync(callback)
        }
    }

    private fun loadFallbackAnimationAsync(callback: (LottieComposition?) -> Unit) {
        try {
            val inputStream = requireContext().assets.open("animation_loading_1.json")
            LottieCompositionFactory.fromJsonInputStream(inputStream, "fallback")
                .addListener(
                    object : LottieListener<LottieComposition> {
                        override fun onResult(composition: LottieComposition?) {
                            callback(composition)
                            inputStream.close()
                        }

                        fun onFailure(error: Throwable) {
                            callback(null)
                            inputStream.close()
                            Timber.tag("Lottie").e(error, "Error fallback")
                        }
                    },
                )
        } catch (e: IOException) {
            callback(null)
            Timber.tag("Lottie").e(e, "Error abriendo fallback")
        }
    }

    private fun showRandomAnimation(loop: Boolean = false) {
        getRandomAnimation { composition ->
            _binding?.progressBarlottieAnimationView?.apply {
                composition?.let {
                    setComposition(it)
                    repeatCount = if (loop) LottieDrawable.INFINITE else 0 // <- Loop infinito o no
                    playAnimation()
                    visibility = View.VISIBLE
                } ?: run {
                    visibility = View.GONE
                    showToastError("Error cargando animación")
                }
            }
        }
    }

    // Variable para controlar el estado
    private var isProcessing = false

    private fun handleImageState(hasImage: Boolean) {
        val lottieAnimationView = binding.btnProcessImageslottieAnimationView

        if (isProcessing) return // No cambiar estado si ya se está procesando

        cargeImage = hasImage

        if (hasImage) {
            // Animación de "esperando acción del usuario"
            lottieAnimationView.apply {
                // setAnimation(R.raw.waiting_animation) // Animación de espera
                visibility = View.VISIBLE
                speed = 1.0f
                repeatCount = LottieDrawable.INFINITE
                playAnimation()

                binding.btnProcessImage.isEnabled = true
                binding.btnProcessImage.alpha = 1.0f
            }
        } else {
            lottieAnimationView.cancelAnimation()
            lottieAnimationView.visibility = View.GONE
            binding.btnProcessImage.isEnabled = false
            binding.btnProcessImage.alpha = 0.5f
        }
    }

    private fun ProcesarImagen() {
        Timber.tag("RecognitionFragment").d("Botón procesar imagen presionado")
        if (cargeImage) {
            Timber.tag("RecognitionFragment").d("Imagen cargada")

            // Obtener el bitmap de la vista previa
            val imageUri = (binding.imgPhotoPreview.drawable as? BitmapDrawable)?.bitmap
            imageUri?.let { bitmap ->
                Timber.tag("RecognitionFragment").d("Imagen convertida a bitmap")

                // Guardar el bitmap en un archivo temporal
                val file = saveBitmapToFile(bitmap)
                file?.let { file ->
                    Timber.tag("RecognitionFragment").d("Imagen guardada en archivo")

                    // Lanzar una corrutina para procesar la imagen
                    lifecycleScope.launch {
                        sharedViewModel.setCompressedImage(file)
                        Timber.tag("RecognitionFragment").d("Corrutina lanzada")
                        try {
                            // Llamar al ViewModel para procesar la imagen
                            val result = viewModel.processImage(file)
                            // Ajustar dinámicamente el umbral (por ejemplo, desde un Slider/SeekBar)

                            // Mostrar el resultado en la interfaz de usuario
                            if (result.diseaseName == "No reconocido") {
                                Timber.tag("RecognitionFragment").d("Imagen no reconocida")
                                Toast.makeText(requireContext(), "No reconocido", Toast.LENGTH_SHORT).show()
                            } else {
                                Timber.tag("RecognitionFragment").d("Resultado del procesamiento: $result")
                                navigateToResultFragment(result, bitmap)
                            }
                        } catch (e: Exception) {
                            Timber.tag("RecognitionFragment").e("Error al procesar la imagen: ${e.message}")
                            Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Timber.tag("RecognitionFragment").e("Error al procesar la imagen: ${e.message}")
                            Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Timber.tag("RecognitionFragment").e("Error al guardar la imagen en un archivo")
                    Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Timber.tag("RecognitionFragment").e("Error al convertir la imagen a bitmap")
                Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Debe abrir el menú para realizar la acción", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper method to save bitmap to a temporary file
    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val tempFile = File.createTempFile("image", ".jpg", requireContext().cacheDir)
            val outputStream = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            Timber.tag("ImageSave").e(e, "Error saving bitmap to file")
            null
        }
    }

    private fun OpenUploadClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            selectImageLauncher.launch("image/*")
        } else {
            // Android 8 - Android 10 (API 26 - 29)
            OpenGaleryUploadImage()
        }
    }

    private fun OpenGaleryUploadImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, CAMERA_PERMISSION_REQUEST_CODE)
    }

    // Pasar de fragment en fragment
    private fun navigateToCameraFragment() {
        CloseMenuFlotant()
        findNavController().navigate(R.id.action_recognitionFragment_to_fragmentCamera)
    }

    // Mostrar resultrando tayedo de otro activity
    private fun observeNavigationResult() {
        // Observar el resultado de la navega
        // ción desde FragmentCamera
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("image_uri")?.observe(
            viewLifecycleOwner,
        ) { uri ->
            uri?.let {
                // Mostrar la imagen capturada
                binding.imgPhotoPreview.setImageURI(Uri.parse(it))
                binding.imgPhotoPreview.visibility = View.VISIBLE
                handleImageState(true) // <-- Aquí actualizamos el estado
                // cargeImage = true
                // Toast.makeText(requireContext(), "Imagen cargada de la camara", Toast.LENGTH_SHORT).show()
                showToastCorrect("Imagen cargada con éxito")
                // Limpiar el savedStateHandle para evitar mostrar la misma imagen múltiples veces
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("image_uri")
            }
        }
    }

    private fun setVisibility(clicked: Boolean) {
        with(binding) {
            // icPhotoAlternative.visibility = if (clicked) View.VISIBLE else View.GONE
            // icAddAPhoto.visibility = if (clicked) View.VISIBLE else View.GONE

            if (clicked) {
                icPhotoAlternative.visibility = View.GONE
                icAddAPhoto.visibility = View.GONE
            } else {
                // Esperar al final de la animación para ocultarlos
                icPhotoAlternative.postDelayed({
                    icPhotoAlternative.visibility = View.GONE
                }, toBottom.duration)

                icAddAPhoto.postDelayed({ icAddAPhoto.visibility }, toBottom.duration)
            }
        }
    }

    private fun setAnimation(clicked: Boolean) {
        with(binding) {
            if (clicked) {
                icPhotoAlternative.startAnimation(fromBottom)
                icAddAPhoto.startAnimation(fromBottom)
                iccAdd.startAnimation(rotateOpen)
            } else {
                icPhotoAlternative.startAnimation(toBottom)
                icAddAPhoto.startAnimation(toBottom)
                iccAdd.startAnimation(rotateClose)
            }
        }
    }

    private fun setClickeable(clicked: Boolean) {
        with(binding) {
            icPhotoAlternative.isClickable = clicked
            icAddAPhoto.isClickable = clicked
        }
    }

    private fun showToast(message: String) {
        // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        // StyleableToast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        StyleableToast.makeText(requireContext(), message, R.style.exampleToast).show()
    }

    private fun showToastCorrect(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastCorrect).show()
    }

    private fun showToastError(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastError).show()
    }

    private fun showToastProccessImagevalidation(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastProcessImage).show()
    }

    // Maneja el resultado de la selección de imagen (Android 8 - Android 10)
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            handleSelectedImage(data?.data)
            /*val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                binding.imgPhotoPreview.setImageURI(selectedImageUri)
                binding.imgPhotoPreview.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Imagen cargada con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }*/
        }
    }

    private fun handleSelectedImage(uri: Uri?) {
        uri?.let {
            with(binding.imgPhotoPreview) {
                setImageURI(it)
                visibility = View.VISIBLE
                CloseMenuFlotant()
                // cargeImage = true
                handleImageState(true) // <-- Aquí actualizamos el estado
            }
            showToastCorrect("Imagen cargada con éxito")
        } ?: showToastError("No se pudo cargar la imagen")
    }

    // Maneja   Selección de imágenes (Android 11+)
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Cargar la imagen con escalado antes de asignarla al ImageView
                loadImageEfficiently(it)
                CloseMenuFlotant()
                // cargeImage = true
                handleImageState(true) // <-- Aquí actualizamos el estad
                showToastCorrect("Imagen cargada con éxito")
                // Toast.makeText(requireContext(), "Imagen cargada con éxito", Toast.LENGTH_SHORT).show()
            } ?: run {
                handleImageState(false) // <-- Aquí actualizamos el estad
                // Toast.makeText(requireContext(), "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                showToastError("No se pudo cargar la imagen")
            }
        }

    private fun loadImageEfficiently(uri: Uri) {
        try {
            // Cargar la imagen de manera eficiente, evitando OOM (OutOfMemoryError)
            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

            // Obtener las dimensiones de la imagen sin cargarla completamente en memoria
            requireContext().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Definir un tamaño máximo para las imágenes (por ejemplo, 1024px de ancho)
            val targetWidth = 1024
            val scale =
                max(
                    1,
                    min(
                        options.outWidth / targetWidth,
                        options.outHeight / targetWidth,
                    ),
                )

            // Configurar las opciones para cargar la imagen de forma eficiente
            val loadOptions =
                BitmapFactory.Options().apply {
                    inSampleSize = scale
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

            // Cargar la imagen escalada
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream, null, loadOptions)
                bitmap?.let {
                    binding.imgPhotoPreview.setImageBitmap(it) // Asignar la imagen escalada
                    binding.imgPhotoPreview.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Timber.tag("ImageLoading").e("Error loading image: ${e.message}")
            Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        cleanupResources()
        _binding = null
        super.onDestroyView()
    }

    private fun cleanupResources() {
        try {
            // 1. Cancelar todas las corrutinas pendientes
            job.cancel()
            viewModelScope.coroutineContext.cancelChildren()

            // 2. Liberar recursos de TensorFlow
            if (::tensorFlowHelper.isInitialized) {
                tensorFlowHelper.close()
            }

            // 3. Limpiar binding recursos si el binding aún existe
            _binding?.let {
                // Limpiar lottie animation
                it.progressBarlottieAnimationView.apply {
                    cancelAnimation()
                }

                // Liberar animaciones
                it.iccAdd.clearAnimation()
                it.icPhotoAlternative.clearAnimation()
                it.icAddAPhoto.clearAnimation()

                // Remover listeners de botones
                it.iccAdd.setOnClickListener(null)
                it.icPhotoAlternative.setOnClickListener(null)
                it.icAddAPhoto.setOnClickListener(null)
                it.btnProcessImage.setOnClickListener(null)

                // Limpiar recursos de imágenes
                it.imgPhotoPreview.setImageDrawable(null)
                if (isAdded) {
                    Glide.with(requireContext()).clear(it.imgPhotoPreview)
                }
            }

            // 3. Limpiar recursos de imágenes
            // clearImageResources()

            // 4. Liberar listeners y observadores
            clearObservers()

            // 5. Limpiar archivos temporales
            // clearTempFiles()

            // 8. Limpiar ViewModel compartido
            sharedViewModel.clearSelectedHistory()

            Timber.tag("RecognitionFragment").d("All resources cleaned up successfully")
        } catch (e: Exception) {
            Timber.tag("RecognitionFragment").e(e, "Error during cleanup")
        }
    }

    private fun clearImageResources() {
        binding.imgPhotoPreview.setImageDrawable(null)
        Glide.with(this).clear(binding.imgPhotoPreview)
    }

    private fun clearObservers() {
        viewModel.isFabMenuOpen.removeObservers(viewLifecycleOwner)
        viewModel.uiState.removeObservers(viewLifecycleOwner)
        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("image_uri")
    }

    private fun clearTempFiles() {
        try {
            requireContext().cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("image") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.tag("RecognitionFragment").e(e, "Error clearing temp files")
        }
    }

    // Define el código de solicitud para la selección de imagen
    companion object {
        // private const val REQUEST_IMAGE_SELECT_CODE = 1002
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
