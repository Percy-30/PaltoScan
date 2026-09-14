package com.atpdev.paltoscan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        } else {
            timber.log.Timber.plant(CrashReportingTree())
        }
        
        // Manejo global de excepciones para evitar cierres abruptos
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(com.atpdev.paltoscan.core.utils.GlobalExceptionHandler(this, defaultHandler))
    }

    private class CrashReportingTree : timber.log.Timber.Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(message)
            if (t != null) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}
