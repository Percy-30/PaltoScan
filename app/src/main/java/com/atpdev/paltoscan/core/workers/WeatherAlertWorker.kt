package com.atpdev.paltoscan.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.core.utils.LocationHelper
import com.atpdev.paltoscan.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class WeatherAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository,
    private val locationHelper: LocationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Obtain current location for weather alert
                val location = locationHelper.getCurrentLocation() ?: return@withContext Result.success()
                
                val riskData = weatherRepository.getRiskOfLateBlight(location.latitude, location.longitude)
                val isRisk = riskData.first
                val message = riskData.second

                if (isRisk == true) {
                    showNotification("Alerta de Riesgo Agrícola", message)
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "weather_alerts_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas Climáticas"
            val descriptionText = "Notificaciones sobre riesgos para tus cultivos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_potato)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(context)) {
                notify(1001, builder.build())
            }
        }
    }
}
