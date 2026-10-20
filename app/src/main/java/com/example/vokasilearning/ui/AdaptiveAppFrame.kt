package com.example.vokasilearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

/**
 * Compact Android memakai lebar penuh, landscape dan tablet mendapat ukuran
 * maksimal 840 dp supaya teks dan soal tetap nyaman dibaca.
 */
@Composable
internal fun AdaptiveAppFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        val contentWidth = maxWidth.coerceAtMost(840.dp)
        Box(Modifier.width(contentWidth).fillMaxHeight()) {
            content()
        }
    }
}
