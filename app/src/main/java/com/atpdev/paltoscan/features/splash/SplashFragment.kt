package com.atpdev.app.ui.splash

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val splashDelay = 4000L // 4 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // return inflater.inflate(R.layout.fragment_splash, container, false)
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Android 12 (S): API nivel 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requireActivity().splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
                startAnimationsAndNavigate()
            }
        } else {
            startAnimationsAndNavigate()
        }
    }

    private fun startAnimationsAndNavigate() {
        // 1. Animación Lottie
        binding.lottieAnimationfragmentSplash.playAnimation()

        // 2. Animación del texto (sube desde abajo)
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.textFragmentSplash.startAnimation(slideUp)

        // Navegar después de 3 segundos
        handler.postDelayed({
            navigateToMain()
        }, splashDelay)
    }

    private fun navigateToMain() {
        if (isAdded && findNavController().currentDestination?.id == R.id.splashFragment) {
            findNavController().navigate(R.id.action_splashFragment_to_recognitionMain)
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        binding.lottieAnimationfragmentSplash.cancelAnimation()
        _binding = null
        // super.onDestroyView()
        // view?.findViewById<LottieAnimationView>(R.id.lottieAnimationfragment_splash)?.cancelAnimation()
        super.onDestroyView()
    }
}

// Simulación de carga con Handler
        /*Handler(Looper.getMainLooper()).postDelayed({
            // Navegar al fragmento principal
            findNavController().navigate(R.id.action_splashFragment_to_recognitionMain)
        }, 3000) // 3 segundos de espera*/
