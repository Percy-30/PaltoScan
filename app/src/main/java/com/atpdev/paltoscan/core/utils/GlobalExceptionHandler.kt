package com.atpdev.paltoscan.core.utils

import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Timber.tag("GlobalExceptionHandler").e(exception, "Crash fatal interceptado")
        
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        val intent = Intent(applicationContext, ErrorActivity::class.java).apply {
            putExtra("EXTRA_ERROR_DETAILS", stackTrace)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        applicationContext.startActivity(intent)

        // Terminar el proceso actual
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }
}
