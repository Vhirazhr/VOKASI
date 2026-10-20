package com.example.vokasilearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.vokasilearning.app.VokasiApp
import com.example.vokasilearning.ui.theme.VokasiTheme

/** Android Activity sebagai pintu masuk, tidak berisi logika kuis atau tampilan fitur. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VokasiTheme { VokasiApp(this) }
        }
    }
}
