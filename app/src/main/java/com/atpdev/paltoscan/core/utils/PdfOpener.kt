package com.atpdev.paltoscan.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfOpener {
    fun openPdfFromAssets(
        context: Context,
        assetFileName: String = "manual_palto_scan.pdf",
        fallbackUrl: String = "https://drive.google.com/file/d/1VwJXsqxt-jSnAUdNaj6FUzEVkut9kzXD/view?usp=sharing",
    ) {
        try {
            val inputStream = context.assets.open(assetFileName)
            val outFile = File.createTempFile("manual_", ".pdf", context.cacheDir)
            FileOutputStream(outFile).use { output ->
                inputStream.copyTo(output)
            }

            val pdfUri: Uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    outFile,
                )

            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(pdfUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                }

            context.startActivity(Intent.createChooser(intent, "Abrir manual con..."))
        } catch (e: ActivityNotFoundException) {
            // No hay visor de PDF instalado
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el manual", Toast.LENGTH_SHORT).show()
        }
    }
}
