package com.example.vokasilearning.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

// --- DESIGN TOKENS ---
internal val Navy = Color(0xFF233F68)
internal val NavyLight = Color(0xFF426AA0)
internal val DeepNavy = Color(0xFF162D4F)
internal val Canvas = Color(0xFFF5F7FB)
internal val Ink = Color(0xFF1C2738)
internal val Subtle = Color(0xFF748195)
internal val Border = Color(0xFFE3EAF1)
internal val Good = Color(0xFF23A381)
internal val Bad = Color(0xFFE65C66)
internal val Warning = Color(0xFFF0AC43)

internal val PaleBlue = Color(0xFFE8F0FA)
internal val PaleGreen = Color(0xFFE5F4ED)
internal val PalePurple = Color(0xFFF1EBFB)
internal val PaleGold = Color(0xFFFFF2DB)


@Composable
internal fun VokasiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy, onPrimary = Color.White, secondary = NavyLight,
            background = Canvas, surface = Color.White,
            onBackground = Ink, onSurface = Ink
        ),
        content = content
    )
}
