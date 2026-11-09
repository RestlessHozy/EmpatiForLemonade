package com.example.empati

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.empati.ui.theme.EmpatiTheme

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Ubah ke `true` bila di masa depan ingin kembali memblokir screenshot / screen record.
         * Kode `FLAG_SECURE` sengaja dipertahankan agar mudah diaktifkan kembali kapan saja.
         */
        const val ENABLE_SCREENSHOT_PROTECTION = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ENABLE_SCREENSHOT_PROTECTION) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        enableEdgeToEdge()
        setContent {
            EmpatiTheme {
                EmpatiApp()
            }
        }
    }
}
