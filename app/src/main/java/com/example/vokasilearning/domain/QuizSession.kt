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

/**
 * Mesin sesi quiz prototipe: soal acak, jawaban, timer dan penilaian.
 * Belum terhubung ke penyimpanan/server.
 */
internal class QuizSession {
    var questions by mutableStateOf<List<Question>>(emptyList())
        private set
    val answers = mutableStateMapOf<Int, Int>()
    var index by mutableIntStateOf(0)
    var remainingSeconds by mutableIntStateOf(0)
    var sessionId by mutableIntStateOf(0)
    private var startedAt by mutableLongStateOf(0L)

    fun start(bank: List<Question>): Boolean {
        if (bank.isEmpty()) return false
        questions = bank.shuffled().take(QUESTIONS_PER_SESSION)
        answers.clear()
        index = 0
        remainingSeconds = questions.size * SECONDS_PER_QUESTION
        startedAt = System.currentTimeMillis()
        sessionId++
        return true
    }

    fun finish(student: Student, previous: List<Attempt>): Attempt {
        val correct = questions.count { answers[it.id] == it.answerIndex }
        val score = if (questions.isEmpty()) 0 else correct * 100 / questions.size
        val signature = questions.map { it.id }.sorted().joinToString("-")
        val now = System.currentTimeMillis()
        val repeat = previous.any {
            it.studentId == student.username && it.signature == signature && it.earnedXp > 0
        }
        val xpToday = previous.filter {
            it.studentId == student.username && dayKey(it.timeMillis) == dayKey(now)
        }.sumOf { it.earnedXp }
        val xp = if (answers.isNotEmpty() && !repeat && xpToday < DAILY_XP_CAP) {
            minOf(SESSION_XP, DAILY_XP_CAP - xpToday)
        } else 0
        return Attempt(
            student.username, now, signature, correct, answers.size,
            questions.size, score, xp, ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
        )
    }
}

@Composable
internal fun rememberQuizSession(): QuizSession = remember { QuizSession() }
