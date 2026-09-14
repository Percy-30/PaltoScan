package com.atpdev.paltoscan.core.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.atpdev.paltoscan.databinding.FragmentAlertDialogBinding

class FragmentAlertDialog : DialogFragment() {
    private var _binding: FragmentAlertDialogBinding? = null
    private val binding get() = _binding!!

    var onActionClicked: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        binding.infoVersion.text = "Version $version"

        binding.tvPrivacyPolicy.setOnClickListener {
            val url = "https://sites.google.com/view/privacypolicy-paltoscan/inicio" // Coloca aquí el enlace real
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        initViews()
    }

    private fun initViews() {
        // binding.infoTitle.text = title ?: "Título por defecto"
        // binding.infoDescription.text = description ?: "Descripción por defecto"
        setupButton()
    }

    private fun setupButton() {
        binding.btnInfoClose.setOnClickListener {
            onActionClicked?.invoke()
            dismiss() // Cierra el diálogo automáticamente
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FragmentAlertDialog"
    }
}
