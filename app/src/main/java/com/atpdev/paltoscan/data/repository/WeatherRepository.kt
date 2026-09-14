package com.atpdev.paltoscan.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.atpdev.paltoscan.data.remote.OpenMeteoService
import com.atpdev.paltoscan.data.remote.WeatherResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface WeatherRepository {
    suspend fun getRiskOfLateBlight(latitude: Double, longitude: Double): Pair<Boolean?, String>
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val openMeteoService: OpenMeteoService,
    @ApplicationContext private val context: Context
) : WeatherRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    override suspend fun getRiskOfLateBlight(latitude: Double, longitude: Double): Pair<Boolean?, String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = openMeteoService.getCurrentWeather(latitude, longitude)
                val temp = response.current.temperature
                val humidity = response.current.relativeHumidity

                // Guardar en cache offline
                prefs.edit().apply {
                    putFloat("last_temp", temp.toFloat())
                    putInt("last_humidity", humidity)
                    putLong("last_time", System.currentTimeMillis())
                }.apply()

                evaluateRisk(temp, humidity)
            } catch (e: Exception) {
                // Fallback a cache offline
                val lastTemp = prefs.getFloat("last_temp", -999f)
                val lastHumidity = prefs.getInt("last_humidity", -1)
                if (lastTemp != -999f && lastHumidity != -1) {
                    evaluateRisk(lastTemp.toDouble(), lastHumidity)
                } else {
                    Pair(null, "Sin conexión y sin datos recientes.")
                }
            }
        }
    }

    private fun evaluateRisk(temp: Double, humidity: Int): Pair<Boolean, String> {
        val isRisk = (temp in 10.0..25.0) && (humidity >= 90)
        val message = if (isRisk) {
            "Alerta Riesgo: Alta humedad ($humidity%) y temp. ideal ($temp°C) para Tizón Tardío (Rancha)."
        } else {
            "Bajo riesgo de Rancha actual. Humedad: $humidity%, Temp: $temp°C."
        }
        return Pair(isRisk, message)
    }
}
