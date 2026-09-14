import android.animation.Animator
import timber.log.Timber
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import android.graphics.Color
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.airbnb.lottie.LottieAnimationView
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.core.common.MenuToolbar
import com.atpdev.paltoscan.core.ui.FragmentAlertDialogExit
import com.atpdev.paltoscan.core.utils.sharePaltoScanApp
import com.atpdev.paltoscan.databinding.FragmentHistoryBinding
import com.atpdev.paltoscan.features.history.HistoryViewModel
import com.atpdev.paltoscan.features.history.adapter.HistoryAdapter
import com.atpdev.paltoscan.features.history.adapter.HistoryItem
import com.atpdev.paltoscan.features.result.SharedViewModel
import com.bumptech.glide.Glide
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var menuHandler: MenuToolbar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        EnabledRetroceso()
        setupClearAllButton() // Agrega esta línea
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

    private fun setupClearAllButton() {
        val btnClearAll = binding.btndeleteallhistory
        val lottieAnimation = binding.btnDeleteallhistorylottieAnimationView

        btnClearAll.setOnClickListener {
            // Mostrar diálogo de confirmación
            showClearHistoryDialog(lottieAnimation)
        }
    }

    private fun showToastCorrect(message: String) {
        StyleableToast.makeText(requireContext(), message, R.style.exampleToastCorrect).show()
    }

    private fun showClearHistoryDialog(lottieAnimation: LottieAnimationView) {
        FragmentAlertDialogExit.newInstance(
            title = "Vaciar historial",
            message = "¿Estás seguro de que quieres eliminar todo el historial?",
            positiveText = "Eliminar todo",
            negativeText = "Cancelar",
            onPositive = {
                // Haptic feedback
                com.atpdev.paltoscan.core.utils.HapticHelper.vibrateSuccess(requireContext())
                // Animación al confirmar
                lottieAnimation.speed = 1.5f
                lottieAnimation.playAnimation()

                lottieAnimation.addAnimatorListener(
                    object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            lifecycleScope.launch {
                                try {
                                    // Vaciar el historial
                                    historyViewModel.clearAllHistory()
                                    // Actualizar UI
                                    binding.recyclerViewHistory.visibility = View.GONE
                                    binding.noHistoryTextView.visibility = View.VISIBLE
                                    // Mostrar confirmación
                                    showToastCorrect("Historial vaciado")
                                } catch (e: Exception) {
                                    Timber.tag("HistoryFragment").e(e, "Error al vaciar historial")
                                    Toast.makeText(
                                        requireContext(),
                                        "Error al vaciar el historial",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } finally {
                                    lottieAnimation.frame = 0
                                }
                            }
                        }
                    },
                )
            },
            onNegative = {
                // No se necesita acción específica al cancelar
            },
        ).show(parentFragmentManager, "ClearHistoryDialog")
    }

    // Método para obtener los detalles de la enfermedad
    /*private suspend fun getDiseaseInfo(diseaseName: String): DiseaseInfo {
        // Aquí deberías hacer alguna consulta a la base de datos o repositorio
        return diseaseDatabase[diseaseName] ?: DiseaseInfo(
            name = diseaseName,
            description = "Descripción no disponible",
            prevention = "Prevención no disponible",
            causes = "Causas no disponible",
            treatment = "Tratamiento no disponible"
        )
    }*/

    private fun setupRecyclerView() {
        historyAdapter =
            HistoryAdapter(
                onDeleteClick = { historyItem ->
                    historyViewModel.removeFromHistoryById(historyItem.id)
                    Timber.tag("HistoryFragment").d("Eliminando historial con id: ${historyItem.id}")
                },
                onItemClick = { historyItem ->
                    lifecycleScope.launch {
                        val updatedHistory = historyViewModel.getHistoryById(historyItem.id)
                        Timber.tag("HistoryFragment").d("ACTUALIZED $updatedHistory")
                        updatedHistory?.let { history ->
                            // ✅ 1. GUARDAR en SharedViewModel
                            sharedViewModel.setSelectedHistoryItem(history)

                            // ✅ 2. CREAR Bundle con TODOS los parámetros necesarios
                            val bundle =
                                Bundle().apply {
                                    putString("diseaseName", history.diseaseName) // ✅ CLAVE: Pasar diseaseName
                                    putString("detectedDisease", history.diseaseName) // ✅ Para compatibilidad
                                    putString("section", "Enfermedad") // ✅ Sección por defecto
                                    putInt("position", 0)
                                    putBoolean("fromHistory", true) // ✅ Indicar que viene del historial
                                }

                            Timber.tag("HistoryFragment").d("✅ Navegando a DiseaseInfoFragment con: ${history.diseaseName}")

                            // ✅ 3. Navegar CON bundle
                            findNavController().navigate(
                                R.id.action_historyFragment_to_diseaseInfoFragment,
                                bundle,
                            )
                        }
                    }
                },
            )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            // Remove setHasFixedSize(true) because it is inside a NestedScrollView
        }
    }

    /*private fun setupRecyclerView() {
            historyAdapter = HistoryAdapter(
                onDeleteClick = { historyItem ->
                    //historyViewModel.removeFromHistory(historyItem.diseaseName)
                    //Timber.tag("HistoryFragment").d("Eliminando historial con nombre de enfermedad: ${historyItem.diseaseName}")
                    // Eliminar por id
                    historyViewModel.removeFromHistoryById(historyItem.id)  // Llama a un método que elimina por id
                    Timber.tag("HistoryFragment").d("Eliminando historial con id: ${historyItem.id}")

                },
                onItemClick = { historyItem ->
                    lifecycleScope.launch {
                        // Obtener el historial más actualizado directamente de la base de datos
                        val updatedHistory = historyViewModel.getHistoryById(historyItem.id)
                        Timber.tag("HistoryFragment").d("ACTUALIZED ${updatedHistory}")
                        updatedHistory?.let { //history ->
                            // Clear previous data first
                            // En lugar de pasar `null`, pasamos una instancia con valores por defecto
                            //sharedViewModel.clearSelectedHistory()
                            sharedViewModel.setSelectedHistoryItem(it)
                            // Forzar una recarga del historial
                            //historyViewModel.getAllHistory()
                            findNavController().navigate(R.id.action_historyFragment_to_diseaseInfoFragment)
                        }
                       }
                }
            )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }*/

    /*private fun setupRecyclerView() {
        // Inicializar el adapter con la función de eliminar
        historyAdapter = HistoryAdapter { historyItem ->
            // Llamar al método removeFromHistory del ViewModel
            historyViewModel.removeFromHistory(historyItem.diseaseName, historyItem.section)
        }
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }*/

    /*private fun observeViewModel() {
        historyViewModel.historyList.observe(viewLifecycleOwner) { historyItems ->
            historyAdapter.submitList(historyItems)
        }
    }*/
    // En el Fragment
    private fun observeViewModel() {
        historyViewModel.historyList.observe(viewLifecycleOwner) { histories ->
            Timber.tag("HistoryFragment").d("Actualizando lista: ${histories.size} elementos")

            val historyItems =
                histories.map { history ->
                    HistoryItem(
                        id = history.id,
                        diseaseName = history.diseaseName,
                        section = history.section,
                        timestamp = history.timestamp,
                        imagePath = history.imagePath ?: "",
                    )
                }

            historyAdapter.submitList(historyItems.toList())
            // binding.recyclerViewHistory.visibility = if (histories.isNotEmpty()) View.VISIBLE else View.GONE

            // Mostrar/ocultar RecyclerView y texto de historial vacío
            val hasHistory = histories.isNotEmpty()
            binding.recyclerViewHistory.visibility = if (hasHistory) View.VISIBLE else View.GONE
            binding.noHistoryTextView.visibility = if (hasHistory) View.GONE else View.VISIBLE

            // Habilitar/deshabilitar botón según si hay historial
            binding.btndeleteallhistory.isEnabled = hasHistory
            binding.btnDeleteallhistorylottieAnimationView.isEnabled = hasHistory

            // Cambiar opacidad para indicar estado deshabilitado
            val alphaValue = if (hasHistory) 1.0f else 0.5f
            binding.btndeleteallhistory.alpha = alphaValue
            binding.btnDeleteallhistorylottieAnimationView.alpha = alphaValue

            // Configurar PieChart y BarChart
            if (hasHistory) {
                setupPieChart(histories)
                setupBarChart(histories)
            } else {
                binding.pieChart.visibility = View.GONE
                binding.barChart.visibility = View.GONE
            }
        }
    }

    private fun setupPieChart(histories: List<com.atpdev.paltoscan.data.local.entity.History>) {
        binding.pieChart.visibility = View.VISIBLE
        
        // Agrupar por nombre de enfermedad y contar
        val diseaseCounts = histories.groupingBy { it.diseaseName }.eachCount()
        
        val entries = ArrayList<PieEntry>()
        for ((disease, count) in diseaseCounts) {
            entries.add(PieEntry(count.toFloat(), disease))
        }

        val dataSet = PieDataSet(entries, "Enfermedades")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f

        val textColor = getChartTextColor()
        dataSet.valueTextColor = textColor

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = "Historial"
        binding.pieChart.setCenterTextColor(textColor)
        binding.pieChart.legend.textColor = textColor
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate() // refresh
    }

    private fun setupBarChart(histories: List<com.atpdev.paltoscan.data.local.entity.History>) {
        binding.barChart.visibility = View.VISIBLE
        
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        
        // 1. Obtener todas las enfermedades únicas para mantener el mismo orden de colores en cada barra
        val uniqueDiseases = histories.map { it.diseaseName }.distinct().sorted()
        
        // 2. Agrupar historiales por fecha
        val historiesByDate = histories.groupBy { dateFormat.format(Date(it.timestamp)) }
        
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f
        
        // 3. Crear una entrada (BarEntry) apilada para cada fecha
        for ((dateStr, dayHistories) in historiesByDate.toSortedMap()) {
            val diseaseCountsForDay = dayHistories.groupingBy { it.diseaseName }.eachCount()
            val floatArray = FloatArray(uniqueDiseases.size)
            
            // Llenar el arreglo respetando el orden de uniqueDiseases
            for (i in uniqueDiseases.indices) {
                floatArray[i] = (diseaseCountsForDay[uniqueDiseases[i]] ?: 0).toFloat()
            }
            
            entries.add(BarEntry(index, floatArray))
            labels.add(dateStr)
            index++
        }

        val dataSet = BarDataSet(entries, "Análisis por Día")
        // Combinar paletas de colores para tener suficientes opciones
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.PASTEL_COLORS.toList()
        dataSet.stackLabels = uniqueDiseases.toTypedArray()
        dataSet.valueTextSize = 12f

        val textColor = getChartTextColor()
        dataSet.valueTextColor = textColor

        val data = BarData(dataSet)
        binding.barChart.data = data
        binding.barChart.description.isEnabled = false
        binding.barChart.legend.textColor = textColor
        
        val xAxis = binding.barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = textColor
        
        binding.barChart.axisLeft.granularity = 1f
        binding.barChart.axisLeft.textColor = textColor
        binding.barChart.axisRight.isEnabled = false
        
        // Habilitar scroll horizontal si hay muchas barras
        binding.barChart.isDragEnabled = true
        binding.barChart.setScaleEnabled(false) // Deshabilitar zoom vertical/horizontal por pellizco
        
        binding.barChart.animateY(1000)
        binding.barChart.invalidate() // refresh
        
        // Mostrar máximo 5 días a la vez para que se puedan scrollear
        binding.barChart.setVisibleXRangeMaximum(5f)
        // Mover la vista al final (días más recientes)
        if (labels.isNotEmpty()) {
            binding.barChart.moveViewToX(labels.size - 1f)
        }
    }

    private fun getChartTextColor(): Int {
        val nightModeFlags = requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

        /*historyViewModel.historyList.observe(viewLifecycleOwner) { histories ->
            Timber.tag("UI").d("Se actualizó la lista del historial: ${histories.size} elementos -> Datos: $histories")
            val historyItems = histories.map { history ->
                HistoryItem(
                    diseaseName = history.diseaseName,
                    section = "main",
                    timestamp = history.timestamp,
                    imagePath = history.imagePath ?: ""
                )
            }
            historyAdapter.submitList(historyItems)
            Timber.tag("UI").d("Lista enviada al adaptador: ${historyItems.size} elementos")
            binding.recyclerViewHistory.visibility =
                if (histories.isNotEmpty()) View.VISIBLE else View.GONE
        }*/

    private fun setupToolbar() {
        menuHandler =
            MenuToolbar(
                context = requireContext(),
                onHistoryClick = { /* Ya estamos en History */ },
                onAboutClick = { navigateToAlertDialog() },
                onShareClick = { sharePaltoScanApp() },
                onExitClick = { showExitConfirmationDialog() },
            )

        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.appBarMenu.toolbar)
            supportActionBar?.apply {
                title = "Historial"
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
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> menuHandler.onOptionsItemSelected(item)
        }
    }
    /*private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }*/

    private fun navigateToAlertDialog() {
        findNavController().navigate(R.id.action_historyFragment_to_fragmentAlertDialog)
    }

    override fun onDestroyView() {
        cleanupResources()
        super.onDestroyView()
        // sharedViewModel.clearSelectedHistory()
    }

    private fun cleanupResources() {
        try {
            // 1. Limpiar el adaptador del RecyclerView
            binding.recyclerViewHistory.adapter = null

            // 2. Cancelar corrutinas pendientes
            lifecycleScope.launch {
                coroutineContext.cancelChildren()
            }

            // 3. Limpiar selección en el ViewModel compartido
            sharedViewModel.clearSelectedHistory()

            // 4. Detener y limpiar animaciones Lottie
            binding.btnDeleteallhistorylottieAnimationView.apply {
                cancelAnimation()
                setImageDrawable(null)
            }

            // 5. Remover listeners
            binding.btndeleteallhistory.setOnClickListener(null)

            // 6. Limpiar Glide (si estás usando imágenes)
            Glide.with(this).clear(binding.root)

            // 7. Liberar binding
            _binding = null

            Timber.tag("HistoryFragment").d("All resources cleaned up")
        } catch (e: Exception) {
            Timber.tag("HistoryFragment").e(e, "Error during cleanup")
        }
    }
}
