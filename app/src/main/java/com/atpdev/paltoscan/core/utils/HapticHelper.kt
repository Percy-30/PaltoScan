package com.atpdev.paltoscan.core.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {
    fun vibrateSuccess(context: Context) {
        vibratePattern(context, longArrayOf(0, 30, 60, 50))
    }

    fun vibrateError(context: Context) {
        vibratePattern(context, longArrayOf(0, 50, 100, 50, 100, 50))
    }

    fun vibrateClick(context: Context) {
        vibratePattern(context, longArrayOf(0, 20))
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Ignorar si el dispositivo no tiene vibrador o no hay permisos
        }
    }
}
