package com.atpdev.paltoscan.core.utils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.features.main.MainActivity
import com.google.android.material.button.MaterialButton

class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        val btnRestart = findViewById<MaterialButton>(R.id.btnRestart)
        btnRestart.setOnClickListener {
            // Reiniciar la app volviendo a MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }
    }
}
