package com.example.vokasilearning.domain

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.vokasilearning.app.*
import com.example.vokasilearning.navigation.*
import com.example.vokasilearning.domain.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.features.auth.*
import com.example.vokasilearning.features.student.*
import com.example.vokasilearning.features.admin.*
import com.example.vokasilearning.ui.components.*
import com.example.vokasilearning.ui.theme.*
import com.example.vokasilearning.ui.adaptive.*
import com.example.vokasilearning.ui.security.*

internal fun clockText(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

internal fun scoreGrade(score: Int): String = when {
    score >= 90 -> "Sangat Baik"
    score >= 75 -> "Baik"
    score >= 60 -> "Cukup"
    else -> "Perlu Latihan"
}

internal fun wibCalendar() = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
internal fun dayKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
}.format(Date(millis))
internal fun seasonKey(timeMillis: Long): String {
    val cal = wibCalendar().apply { timeInMillis = timeMillis }
    return "${cal.get(Calendar.YEAR)}-S${cal.get(Calendar.MONTH) / 3 + 1}"
}
internal fun seasonTitle(): String {
    val c = wibCalendar()
    return "Season ${c.get(Calendar.MONTH) / 3 + 1} • ${c.get(Calendar.YEAR)}"
}
internal fun levelFor(totalXp: Int): LevelData {
    var level = 1; var threshold = 0; var cost = 100
    while (totalXp >= threshold + cost && level < 500) {
        threshold += cost; level++; cost += 50
    }
    return LevelData(level, threshold, threshold + cost)
}
internal fun calculateStreak(student: Student, attempts: List<Attempt>): Int {
    val validDays = attempts.filter { it.studentId == student.username && it.answered > 0 }
        .map { dayKey(it.timeMillis) }.toSet()
    val day = wibCalendar().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    if (dayKey(day.timeInMillis) !in validDays) day.add(Calendar.DAY_OF_YEAR, -1)
    var actual = 0
    while (dayKey(day.timeInMillis) in validDays) { actual++; day.add(Calendar.DAY_OF_YEAR, -1) }
    return max(actual, student.seedStreak)
}
internal fun studentSeasonXp(student: Student, attempts: List<Attempt>): Int {
    val currentSeason = seasonKey(System.currentTimeMillis())
    return student.seedXp + attempts.filter {
        it.studentId == student.username && seasonKey(it.timeMillis) == currentSeason
    }.sumOf { it.earnedXp }
}
internal fun lifetimeXp(student: Student, attempts: List<Attempt>): Int =
    student.seedXp + attempts.filter { it.studentId == student.username }.sumOf { it.earnedXp }

internal fun rankList(
    users: List<Student>, attempts: List<Attempt>, me: Student, scope: RankingScope, metric: RankingMetric,
    chosenMajor: String = me.classroom.major, chosenClass: String = me.classroom.label
): List<Student> {
    val filtered = users.filter { u -> when (scope) {
        RankingScope.SCHOOL -> true
        RankingScope.MAJOR -> u.classroom.major == chosenMajor
        RankingScope.CLASS -> u.classroom.label == chosenClass
    } }
    return if (metric == RankingMetric.XP) filtered.sortedWith(
        compareByDescending<Student> { studentSeasonXp(it, attempts) }.thenBy { it.name }
    ) else filtered.sortedWith(
        compareByDescending<Student> { calculateStreak(it, attempts) }.thenBy { it.name }
    )
}
