package com.atpdev.paltoscan.features.result

import android.content.Context
import timber.log.Timber
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.core.common.MenuToolbar
import com.atpdev.paltoscan.core.ui.FragmentAlertDialogExit
import com.atpdev.paltoscan.core.utils.sharePaltoScanApp
import com.atpdev.paltoscan.databinding.FragmentResultBinding
import com.atpdev.paltoscan.domain.model.RecognitionResult
import com.atpdev.paltoscan.features.diseaseInfo.DiseaseRepository
import com.atpdev.paltoscan.features.recognition.RecognitionViewModel
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@AndroidEntryPoint
class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    // Remove the by viewModels() delegation
    // En el ResultFragment
    private lateinit var viewModel: RecognitionViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bitmap: Bitmap

    private lateinit var menuHandler: MenuToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        startMenu()
        viewModel = ViewModelProvider(requireActivity())[RecognitionViewModel::class.java]
        // viewModel = ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))[RecognitionViewModel::class.java]
        Timber.tag("Resultado").d("ViewModelFinal ")

        // Recupera el RecognitionResult del Bundle
        // mostrarResultUI()
        final()
        btnDiseaseInfo()
        EnabledRetroceso()

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is com.atpdev.paltoscan.features.recognition.RecognitionUiState.Error) {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun final() {
        // Obtener los datos enviados desde RecognitionFragment
        val recognitionResult = arguments?.getSerializable("recognitionResult") as? RecognitionResult
        recognitionResult?.let { result ->
            // Mostrar nombre legible y en español de la enfermedad
            binding.tvDiseaseName.text = DiseaseRepository.getDisplayName(result.diseaseName)
            val resultconfidence = result.getProbabilityString().replace("%", "").toFloat()

            val isNotDetected = result.diseaseName == "No detectado" || result.diseaseName == "No reconocido" || result.diseaseName == "No identificado"
            val isInconclusive = result.diseaseName == "Diagnóstico Incierto" || result.status == com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE
            val isLowConfidence = result.isLowConfidence || (resultconfidence < 65f && !isNotDetected)

            binding.progresoCircular.apply {
                progress = if (isNotDetected) 100f else resultconfidence
                setProgressWithAnimation(if (isNotDetected) 100f else resultconfidence, 1500)

                // Cambia el color según nivel de acierto y estado
                progressBarColor =
                    when {
                        isNotDetected -> Color.parseColor("#F44336") // Rojo
                        isInconclusive -> Color.parseColor("#FF9800") // Naranja (Incierto / Duda)
                        resultconfidence >= 75 -> Color.parseColor("#4CAF50") // Verde
                        resultconfidence >= 50 -> Color.parseColor("#FFC107") // Amarillo
                        else -> Color.parseColor("#F44336") // Rojo
                    }
            }
            binding.tvConfidence.text = if (isNotDetected) "0%" else "${resultconfidence.toInt()}%"
            binding.btnDiseaseInfo.visibility = if (isNotDetected) View.GONE else View.VISIBLE

            // Mostrar segunda opción si el modelo detectó otra sospecha
            if (!result.secondDiseaseName.isNullOrBlank() && result.secondProbability > 0.05f) {
                val secondName = DiseaseRepository.getDisplayName(result.secondDiseaseName)
                val secondPercent = (result.secondProbability * 100).toInt()
                binding.tvSecondPrediction.text = "Segunda sospecha: $secondName ($secondPercent%)"
                binding.tvSecondPrediction.visibility = View.VISIBLE
            } else {
                binding.tvSecondPrediction.visibility = View.GONE
            }

            // Mostrar advertencia si la certeza es baja o inconclusa
            if (isInconclusive) {
                binding.tvWarning.text = "⚠️ Baja certeza diagnóstica. Si la hoja presenta daño o manchas, se sugiere repetir la foto con luz natural enfocando el haz (cara superior)."
                binding.tvWarning.visibility = View.VISIBLE
            } else if (isLowConfidence) {
                binding.tvWarning.text = "⚠️ Confianza moderada. Asegúrate de enfocar bien la superficie de la hoja sin sombras."
                binding.tvWarning.visibility = View.VISIBLE
            } else {
                binding.tvWarning.visibility = View.GONE
            }

            Timber.tag("Performance").d("Probabilidad: ${result.probability}, Resultado: ${result.diseaseName}, 2da: ${result.secondDiseaseName} (${result.secondProbability})")
        }

        // Usa los datos
        sharedViewModel.heatmapBitmap.observe(viewLifecycleOwner) { heatmap ->
            if (heatmap != null) {
                // Si hay mapa de calor, mostrarlo (ya viene fusionado con la imagen original)
                binding.imageViewResult.setImageBitmap(heatmap)
            } else {
                // Si no hay mapa de calor, mostrar la imagen normal
                sharedViewModel.compressedImage?.observe(viewLifecycleOwner) { file ->
                    binding.imageViewResult.setImageURI(file)
                }
            }
        }

        /*val bitmap = arguments?.getParcelable<Bitmap>("bitmap")
        bitmap?.let {
            val cacheFile = File(requireContext().cacheDir, "temp_data")
            val fos = FileOutputStream(cacheFile)
            it.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()

            // Pasa la ruta del archivo temporal al SharedViewModel
            sharedViewModel.setBitmapPath(cacheFile.absolutePath)
            //sharedViewModel.setBitmap(it)
        }*/
    }

    // Función para guardar el Bitmap en un archivo
    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val contextWrapper = ContextWrapper(requireContext())
        val directory = contextWrapper.getDir("images", Context.MODE_PRIVATE)
        val file = File(directory, "image_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    private fun mostrarResultUI() {
        val diseaseName = arguments?.getString("diseaseName")
        val probability = arguments?.getFloat("probability")
        val imageUrl = arguments?.getString("imageUrl")
        val confidenceLevel = arguments?.getString("confidenceLevel")
        val bitmap = arguments?.getParcelable<Bitmap>("bitmap")
        // Reconstruye RecognitionResult

        val recognitionResult =
            RecognitionResult(
                diseaseName = diseaseName ?: "Desconocido",
                probability = probability ?: 0.0f,
                imageUrl = imageUrl,
                confidenceLevel = confidenceLevel,
            )
        // Usa los datos
        binding.tvDiseaseName.text = DiseaseRepository.getDisplayName(recognitionResult.diseaseName)
        binding.tvConfidence.text = recognitionResult.getProbabilityString()
        bitmap?.let { binding.imageViewResult.setImageBitmap(it) }
    }

    private fun btnDiseaseInfo() {
        binding.btnDiseaseInfo.setOnClickListener {
            val recognitionResult = arguments?.getSerializable("recognitionResult") as? RecognitionResult
            val bitmap = arguments?.getParcelable<Bitmap>("bitmap")

            recognitionResult?.let { result ->
                val bundle =
                    Bundle().apply {
                        putString("detectedDisease", result.diseaseName)
                        putString("diseaseName", result.diseaseName)
                        putParcelable("bitmap", bitmap) // Pasar el Bitmap
                    }
                // Resetear el estado antes de navegar
                sharedViewModel.reset()
                // sharedViewModel.clearSelectedHistory()
                findNavController().navigate(R.id.action_resultFragment_to_diseaseInfoFragment, bundle)
            }

            /*val recognitionResult = arguments?.getSerializable("recognitionResult") as? RecognitionResult
            recognitionResult?.let { result ->
                val action = ResultFragmentDirections.actionResultFragmentToDiseaseInfoFragment()
                findNavController().navigate(action)
            }*/
        }
    }

    private fun navigateToFragmentDiseaseInfo() {
        findNavController().navigate(R.id.action_resultFragment_to_diseaseInfoFragment)
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
                showToastProccessImagevalidation("No se guardo en el historial")
                navigateToRecognitionFragment()
                true
            }
            else -> menuHandler.onOptionsItemSelected(item)
        }
    }

    // Navegar hacia el diálogo "History"
    private fun navigateToHistoryFragment() {
        try {
            findNavController().navigate(R.id.action_diseaseInfoFragment_to_historyFragment)
        } catch (e: Exception) {
            Timber.tag("Navigation").e("Error al navegar al diálogo: ${e.message}")
        }
    }

    // *********ALERT DIALOG
    private fun navigateToAlertDialog() {
        try {
            findNavController().navigate(R.id.action_resultFragment_to_fragmentAlertDialog)
        } catch (e: Exception) {
            Timber.tag("Navigation").e("Error navigating to AlertDialog: ${e.message}")
            Toast.makeText(requireContext(), "Error al mostrar el diálogo", Toast.LENGTH_SHORT).show()
        }
    }

    // Pasar de fragment en fragment
    private fun navigateToRecognitionFragment() {
        findNavController().navigate(R.id.action_resultFragment_to_recognitionFragment)
    }

    override fun onDestroyView() {
        cleanupResources()
        super.onDestroyView()
    }

    private fun showToastProccessImagevalidation(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastProcessImage).show()
    }

    private fun cleanupResources() {
        try {
            // 1. Limpiar recursos de imágenes
            clearImageResources()

            // 2. Limpiar ViewModel compartido
            sharedViewModel.reset()

            // 3. Remover observadores
            clearObservers()

            // 4. Limpiar referencias de menú
            menuHandler.let {
                // Si MenuToolbar tiene algún recurso que limpiar
            }

            // 5. Liberar binding (siempre al final)
            _binding = null

            Timber.tag("ResultFragment").d("All resources cleaned up successfully")
        } catch (e: Exception) {
            Timber.tag("ResultFragment").e(e, "Error during cleanup")
        }
    }

    private fun clearImageResources() {
        try {
            // Limpiar ImageView
            binding.imageViewResult.setImageDrawable(null)

            // Limpiar Glide (si se usó)
            Glide.with(this).clear(binding.imageViewResult)

            // Limpiar Picasso (si se usó)
            Picasso.get().cancelRequest(binding.imageViewResult)

            // Liberar bitmap si existe
            if (::bitmap.isInitialized) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Timber.tag("ResultFragment").e(e, "Error clearing image resources")
        }
    }

    private fun clearObservers() {
        try {
            // Remover observadores del ViewModel compartido
            sharedViewModel.compressedImage?.removeObservers(viewLifecycleOwner)
            viewModel.uiState.removeObservers(viewLifecycleOwner)
        } catch (e: Exception) {
            Timber.tag("ResultFragment").e(e, "Error clearing observers")
        }
    }
}
