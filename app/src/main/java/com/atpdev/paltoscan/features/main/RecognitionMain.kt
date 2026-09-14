package com.atpdev.paltoscan.features.main

import android.Manifest
import timber.log.Timber
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.core.common.MenuToolbar
import com.atpdev.paltoscan.core.ml.TensorFlowHelper
import com.atpdev.paltoscan.core.ui.FragmentAlertDialogExit
import com.atpdev.paltoscan.core.utils.PdfOpener
import com.atpdev.paltoscan.databinding.FragmentRecognitionMainBinding
import com.atpdev.paltoscan.domain.repository.CameraRepository
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import javax.inject.Inject

@AndroidEntryPoint
class RecognitionMain : Fragment() {
    private lateinit var binding: FragmentRecognitionMainBinding
    private lateinit var menuHandler: MenuToolbar
    private val viewModel: MainViewModel by activityViewModels()

    lateinit var buttonWithAnimation: ConstraintLayout
    lateinit var lottieAnimationView: LottieAnimationView
    lateinit var buttonText: TextView

    // private lateinit var viewModel: RecognitionViewModel
    lateinit var tensorFlowHelper: TensorFlowHelper

    // Camera repository
    // private lateinit var cameraRepository: CameraRepository
    // ✅ Hilt inyectará el repositorio automáticamente
    @Inject
    lateinit var cameraRepository: CameraRepository

    // Registrar el lanzador para solicitar permisos usando la API de ActivityResult
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, navegar al fragmento de reconocimiento
                Timber.tag("RecognitionMain").d("Permiso de cámara concedido")
                navigateToRecognitionFragment()
            } else {
                // Permiso denegado
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Mostrar explicación de por qué necesitamos el permiso
                    showPermissionRationaleDialog()
                } else {
                    // El usuario marcó "No volver a preguntar", mostrar diálogo para ir a configuración
                    showGoToSettingsDialog()
                }
            }
        }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndPromptLocation()
            viewModel.fetchWeatherRisk()
        } else {
            binding.txtWeatherStatus.text = "Clima no disponible"
            binding.txtWeatherDetails.text = "Se requiere permiso de ubicación."
        }
    }

    private fun checkAndPromptLocation() {
        val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            FragmentAlertDialogExit.newInstance(
                title = "GPS Desactivado",
                message = "Las alertas climáticas requieren que tu GPS esté encendido. ¿Deseas activarlo en los ajustes?",
                positiveText = "Ir a Ajustes",
                negativeText = "Ahora no",
                onPositive = {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onNegative = {},
                iconResId = R.drawable.ic_bar_info
            ).show(parentFragmentManager, "GpsPromptDialog")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize camera repository
        // cameraRepository = CameraRepository(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRecognitionMainBinding.inflate(inflater, container, false)
        return binding.root
        // return inflater.inflate(R.layout.fragment_recognition_main, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupMainButtons()
        setupWeatherWidget()
    }

    private fun setupWeatherWidget() {
        viewModel.weatherRisk.observe(viewLifecycleOwner) { riskData ->
            val isRisk = riskData.first
            val message = riskData.second

            if (isRisk == null) {
                // Error o Offline
                binding.txtWeatherStatus.text = "Clima No Disponible"
                binding.txtWeatherStatus.setTextColor(android.graphics.Color.GRAY)
                binding.imgWeatherIcon.setColorFilter(android.graphics.Color.GRAY)
            } else if (isRisk == true) {
                binding.txtWeatherStatus.text = "Alerta de Riesgo"
                binding.txtWeatherStatus.setTextColor(resources.getColor(R.color.colorRedBright, null))
                binding.imgWeatherIcon.setColorFilter(resources.getColor(R.color.colorRedBright, null))
            } else {
                binding.txtWeatherStatus.text = "Condiciones Favorables"
                binding.txtWeatherStatus.setTextColor(resources.getColor(R.color.colorPrimary, null))
                binding.imgWeatherIcon.setColorFilter(resources.getColor(R.color.colorPrimary, null))
            }
            binding.txtWeatherDetails.text = message
        }

        // Request location permission and fetch weather
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            checkAndPromptLocation()
            viewModel.fetchWeatherRisk()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar permisos de cámara
        if (cameraRepository.isCameraPermissionGranted()) {
            Timber.tag("RecognitionMain").d("Permiso de cámara ya concedido")
            binding.btnDetectPatofoli.isEnabled = true
        }

        // Refrescar clima si tiene permiso, ideal si vuelve de activar el GPS
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchWeatherRisk()
        }
    }

    private fun setupMainButtons() {
        // Botón para iniciar la detección - verifica permisos primero
        binding.btnDetectPatofoli.setOnClickListener {
            checkCameraPermission()
        }
        // Configurar otros botones
        infoApp()
        manualUserApp()
        exitApp()
    }

    private fun showToastError(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastError).show()
    }

    private fun showToastWarning(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastProcessImage).show()
    }

    private fun checkCameraPermission() {
        when {
            cameraRepository.isCameraPermissionGranted() -> {
                // Permiso ya concedido, navegar al fragmento de reconocimiento
                navigateToRecognitionFragment()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostrar explicación de por qué necesitamos el permiso
                showPermissionRationaleDialog()
            }
            else -> {
                // Solicitar permiso de cámara
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToRecognitionFragment() {
        findNavController().navigate(R.id.recognitionFragment)
    }

    private fun showPermissionRationaleDialog() {
        FragmentAlertDialogExit.newInstance(
            title = "Permiso de cámara necesario",
            message = "Esta aplicación necesita acceso a la cámara para detectar y analizar imágenes. Por favor, concede el permiso para continuar.",
            positiveText = "Conceder",
            negativeText = "Cancelar",
            onPositive = {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onNegative = {
                showToastWarning("El acceso a la cámara es necesario para usar esta función")
            },
            iconResId = R.drawable.ic_advertencia, // Ícono desde recursos
        ).show(parentFragmentManager, "CameraPermissionDialog")
    }

    private fun showGoToSettingsDialog() {
        FragmentAlertDialogExit.newInstance(
            title = "Permiso de cámara requerido",
            message = "Parece que denegaste el permiso de cámara. Para usar esta función, ve a Ajustes > Aplicaciones > PaltoScan > Permisos y habilítalo.",
            positiveText = "Ir a Ajustes",
            negativeText = "Cancelar",
            onPositive = {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                startActivity(intent)
            },
            onNegative = {
                showToastError("El acceso a la cámara es necesario para usar esta función")
            },
            iconResId = R.drawable.ic_bar_info, // Ícono desde recursos
            positiveButtonBackgroundColor = R.color.colorTeal,
            positiveButtonTextColor = R.color.black,
            negativeButtonBackgroundColor = R.color.colorOrangeSoft,
            negativeButtonTextColor = R.color.black,
        ).show(parentFragmentManager, "GoToSettingsDialog")
    }

    /*private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permiso de cámara necesario")
            .setMessage("Esta aplicación necesita acceso a la cámara para detectar y analizar imágenes. Por favor, concede el permiso para continuar.")
            .setPositiveButton("Conceder") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                showToastWarning("El acceso a la cámara es necesario para usar esta función",)
            }
            .show()
    }*/

    /*private fun showGoToSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permiso de cámara requerido")
            .setMessage("Parece que denegaste el permiso de cámara. Para usar esta función, ve a Ajustes > Aplicaciones > PaltoScan > Permisos y habilítalo.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar"){ dialog, _ ->
                dialog.dismiss()
                showToastError("El acceso a la cámara es necesario para usar esta función",)
            }
            .show()
    }*/

    private fun infoApp() {
        binding.btnAbout.setOnClickListener {
            findNavController().navigate(R.id.action_recognitionMain_to_fragmentAlertDialog)
        }
    }

    private fun manualUserApp() {
        binding.btnUserManual.setOnClickListener {
            PdfOpener.openPdfFromAssets(requireContext())
        }
    }

    private fun exitApp() {
        binding.btnExit.setOnClickListener {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
        if (::tensorFlowHelper.isInitialized) {
            tensorFlowHelper.close() // Llamar a close() solo si tensorflowHelper está inicializado
        } else {
            Timber.tag("RecognitionFragment").w("TensorFlowHelper no inicializado.")
        }
    }
}
