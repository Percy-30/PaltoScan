package com.atpdev.paltoscan.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.atpdev.paltoscan.databinding.FragmentAlertDialogExitBinding

class FragmentAlertDialogExit : DialogFragment() {
    private var _binding: FragmentAlertDialogExitBinding? = null
    private val binding get() = _binding!!

    // Propiedades configurables
    var title: String = "Título"
    var message: String = "Mensaje"
    var positiveButtonText: String = "Aceptar"
    var negativeButtonText: String = "Cancelar"
    var onPositiveClick: (() -> Unit)? = null
    var onNegativeClick: (() -> Unit)? = null

    // Propiedades OPCIONALES para colores
    var positiveButtonBackgroundColor: Int? = null
    var negativeButtonBackgroundColor: Int? = null
    var positiveButtonTextColor: Int? = null
    var negativeButtonTextColor: Int? = null

    // Nuevo: propiedad para el ícono
    var iconResId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertDialogExitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun setupViews() {
        with(binding) {
            // Configurar ícono (si se proporciona)
            iconResId?.let { resId ->
                iconHeader.setImageResource(resId)
                iconHeader.visibility = View.VISIBLE
            }

            infoTitle.text = title
            infoDescription.text = message

            // Configurar botón positivo (Aceptar/Salir)
            btnPositive.apply {
                text = positiveButtonText
                visibility = View.VISIBLE

                // Color de fondo (si se especificó)
                positiveButtonBackgroundColor?.let { colorRes ->
                    setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
                }
                // Color de texto (si se especificó)
                positiveButtonTextColor?.let { colorRes ->
                    setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                }

                setOnClickListener {
                    onPositiveClick?.invoke()
                    dismiss()
                }
            }

            // Configurar botón negativo (Cancelar)
            btnNegative.apply {
                text = negativeButtonText
                visibility = View.VISIBLE

                // Color de fondo (si se especificó)
                negativeButtonBackgroundColor?.let { colorRes ->
                    setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
                }
                // Color de texto (si se especificó)
                negativeButtonTextColor?.let { colorRes ->
                    setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                }

                setOnClickListener {
                    onNegativeClick?.invoke()
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        onPositiveClick = null
        onNegativeClick = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(
            title: String,
            message: String,
            positiveText: String,
            negativeText: String,
            onPositive: () -> Unit,
            onNegative: () -> Unit,
            // Parámetros opcionales (pueden ser nulos)
            positiveButtonBackgroundColor: Int? = null,
            negativeButtonBackgroundColor: Int? = null,
            positiveButtonTextColor: Int? = null,
            negativeButtonTextColor: Int? = null,
            iconResId: Int? = null, // Nuevo: ícono opcional
        ): FragmentAlertDialogExit {
            return FragmentAlertDialogExit().apply {
                this.title = title
                this.message = message
                this.positiveButtonText = positiveText
                this.negativeButtonText = negativeText
                this.onPositiveClick = onPositive
                this.onNegativeClick = onNegative
                // Asignar colores solo si se proporcionan
                this.positiveButtonBackgroundColor = positiveButtonBackgroundColor
                this.negativeButtonBackgroundColor = negativeButtonBackgroundColor
                this.positiveButtonTextColor = positiveButtonTextColor
                this.negativeButtonTextColor = negativeButtonTextColor
                this.iconResId = iconResId // Asignar ícono
            }
        }
    }
}
