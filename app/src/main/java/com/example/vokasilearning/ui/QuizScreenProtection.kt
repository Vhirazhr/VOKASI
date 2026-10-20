package com.example.vokasilearning.ui

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

/** Debug: izin rekaman khusus demo. Release: perlindungan FLAG_SECURE tetap aktif saat kuis. */
@Composable
internal fun QuizScreenProtection(activity: ComponentActivity, enabled: Boolean) {
    val isDebug = (activity.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val demoAllowed = DEMO_RECORDING && isDebug
    DisposableEffect(activity, enabled, demoAllowed) {
        if (enabled && !demoAllowed) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
