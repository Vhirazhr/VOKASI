package com.example.vokasilearning.screens

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
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*
import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.vokasilearning.data.StudentResultPdf
import kotlinx.coroutines.launch



@Composable
internal fun ResultPage(
    student: Student,
    attempt: Attempt,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRanking: () -> Unit,
    onPractice: (String) -> Unit,
    openReviewOnEntry: Boolean = false
) {
    val context = LocalContext.current
    val wrong = attempt.questionCount - attempt.correct
    val directWrong = attempt.review.count { it.status == AnswerStatus.SALAH }
    val timedOut = attempt.review.count { it.status == AnswerStatus.TIMEOUT }
    val missing = attempt.review.count { it.status == AnswerStatus.TIDAK_DIJAWAB }
    var showReview by remember(attempt.sessionKey) { mutableStateOf(openReviewOnEntry) }
    var filter by remember(attempt.sessionKey) { mutableStateOf("Semua") }
    var page by remember(attempt.sessionKey) { mutableIntStateOf(0) }
    val pageSize = 10
    val filtered = attempt.review.withIndex().filter { (_, review) ->
        when (filter) {
            "Salah" -> review.status != AnswerStatus.BENAR
            "Benar" -> review.status == AnswerStatus.BENAR
            "Timeout" -> review.status == AnswerStatus.TIMEOUT
            else -> true
        }
    }
    val pageCount = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val currentPage = page.coerceIn(0, pageCount - 1)
    val displayed = filtered.drop(currentPage * pageSize).take(pageSize)
    val grade = scoreGrade(attempt.score)
    val gradeColor = when {
        attempt.score >= 90 -> Good
        attempt.score >= 75 -> NavyLight
        attempt.score >= 60 -> Color(0xFFA06C13)
        else -> Bad
    }
    val gradeBackground = when {
        attempt.score >= 90 -> PaleGreen
        attempt.score >= 75 -> PaleBlue
        attempt.score >= 60 -> PaleGold
        else -> Color(0xFFFFE7EA)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Berkas PDF tidak dapat dibuka")
                stream.use { StudentResultPdf.write(it, student, attempt) }
                Toast.makeText(context, "Laporan PDF tersimpan", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(context, "Gagal menyimpan PDF: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    fun exportPdf() {
        val id = (attempt.studentNameSnapshot.ifBlank { student.name })
            .replace(Regex("[^A-Za-z0-9_-]"), "_").take(36)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date(attempt.timeMillis))
        val category = attempt.practiceCategory?.replace(Regex("[^A-Za-z0-9_-]"), "_") ?: "Wajib"
        val filename = "VOKASI_${id}_${category}_${timestamp}.pdf"
        exportLauncher.launch(filename)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(showReview, attempt.sessionKey) {
        if (showReview) listState.animateScrollToItem(2)
    }
    Column(Modifier.fillMaxSize().background(Canvas)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy, fontWeight = FontWeight.Bold) }
            VokasiBrandMark(size = 28.dp, cornerRadius = 8.dp)
            Spacer(Modifier.weight(1f))
            Text("Hasil Latihan", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onHome) { Text("Beranda", color = Navy) }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(17.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                WhiteCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (attempt.practiceCategory == null) "LATIHAN WAJIB SELESAI" else "LATIHAN ${attempt.practiceCategory.uppercase()}",
                            color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold
                        )
                        AnimatedCounter(
                            target = attempt.score, suffix = "/100",
                            color = Ink,
                            textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(gradeBackground).padding(13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GRADE / PENCAPAIAN", color = gradeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(grade, color = gradeColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            "Benar ${attempt.correct} dari ${attempt.questionCount} soal",
                            color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultNumber("Benar", attempt.correct, modifier = Modifier.weight(1f), color = Good)
                            ResultNumber("Salah", wrong, modifier = Modifier.weight(1f), color = Bad)
                        }
                        Text(
                            "Salah dijawab $directWrong • Timeout $timedOut • Kosong $missing",
                            color = Subtle, fontSize = 11.sp, textAlign = TextAlign.Center
                        )
                        Text(
                            "${clockText(attempt.secondsSpent)} • +${attempt.earnedXp} XP",
                            color = Subtle, fontSize = 12.sp
                        )
                    }
                }
            }
            item {
                WhiteCard(Modifier.fillMaxWidth()) {
                    Text("LANJUTKAN", color = NavyLight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    VokasiButton(
                        text = "Pembahasan",
                        onClick = { showReview = true }, modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VokasiButton(
                            "Unduh PDF", onClick = { exportPdf() },
                            isSecondary = true, modifier = Modifier.weight(1f)
                        )
                        VokasiButton(
                            "Peringkat", onClick = onRanking,
                            isSecondary = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (showReview) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pembahasan Jawaban", color = Ink, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)
                            )
                            Text("${attempt.review.size} soal", color = Subtle, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Semua", "Salah", "Benar", "Timeout").forEach { value ->
                                val active = filter == value
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (active) Navy else Color.White)
                                        .clickable { filter = value; page = 0 }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        value, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Subtle
                                    )
                                }
                            }
                        }
                        Text(
                            if (filtered.isEmpty()) "Tidak ada soal pada filter ini"
                            else "Soal ${currentPage * pageSize + 1}–${currentPage * pageSize + displayed.size} dari ${filtered.size}",
                            color = Subtle, fontSize = 12.sp
                        )
                    }
                }
                items(displayed, key = { it.value.question.id }) { indexed ->
                    val review = indexed.value
                    val correct = review.status == AnswerStatus.BENAR
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Soal ${indexed.index + 1} • ${review.question.category}",
                                fontWeight = FontWeight.Bold, color = Ink, fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Pill(
                                when (review.status) {
                                    AnswerStatus.BENAR -> "Benar"
                                    AnswerStatus.SALAH -> "Salah"
                                    AnswerStatus.TIMEOUT -> "Timeout"
                                    AnswerStatus.TIDAK_DIJAWAB -> "Kosong"
                                },
                                if (correct) PaleGreen else Color(0xFFFFE7EA),
                                if (correct) Good else Bad
                            )
                        }
                        Text(review.question.question, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Jawabanmu: ${review.selectedIndex?.let { review.question.options.getOrNull(it) } ?: "Tidak dijawab"}",
                            color = if (correct) Good else Bad, fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            "Kunci: ${review.question.options.getOrElse(review.question.answerIndex) { "-" }}",
                            color = Good, fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                        if (review.question.explanation.isNotBlank()) {
                            Text(review.question.explanation, color = Subtle, fontSize = 12.sp)
                        }
                    }
                }
                if (filtered.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    page = (page - 1).coerceAtLeast(0)
                                    scope.launch { listState.animateScrollToItem(2) }
                                },
                                enabled = currentPage > 0
                            ) { Text("‹ Sebelumnya") }
                            Text("${currentPage + 1} / $pageCount", color = Ink, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    page = (page + 1).coerceAtMost(pageCount - 1)
                                    scope.launch { listState.animateScrollToItem(2) }
                                },
                                enabled = currentPage < pageCount - 1
                            ) { Text("Berikutnya ›") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompletionDialog(
    attempt: Attempt,
    streak: Int,
    syncStatus: String = "",
    highlightStreak: Boolean = false,
    onViewResults: () -> Unit
) {
    val gradeColor = when {
        attempt.score >= 90 -> Good
        attempt.score >= 75 -> Navy
        attempt.score >= 60 -> Color(0xFFA06C13)
        else -> Bad
    }
    val gradeBackground = when {
        attempt.score >= 90 -> PaleGreen
        attempt.score >= 75 -> PaleBlue
        attempt.score >= 60 -> PaleGold
        else -> Color(0xFFFFE7EA)
    }
    val wrong = (attempt.questionCount - attempt.correct).coerceAtLeast(0)

    Dialog(
        onDismissRequest = onViewResults,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(PaleBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                }
                Text(
                    if (attempt.practiceCategory == null) "LATIHAN WAJIB SELESAI"
                    else "LATIHAN MATERI SELESAI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyLight,
                    letterSpacing = 0.8.sp
                )
                Text(
                    completionHeadline(attempt.score),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    lineHeight = 29.sp
                )
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(gradeBackground)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("NILAI KAMU", color = gradeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${attempt.score}",
                        color = gradeColor,
                        fontSize = 53.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 57.sp
                    )
                    Text(
                        scoreGrade(attempt.score),
                        color = gradeColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                            .background(PaleGreen).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Benar", color = Good, fontSize = 11.sp)
                        Text("${attempt.correct}/${attempt.questionCount}",
                            color = Good, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFFFFE7EA)).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Salah", color = Bad, fontSize = 11.sp)
                        Text("$wrong", color = Bad, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                HorizontalDivider(color = Border)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("XP Diperoleh", fontSize = 11.sp, color = Subtle)
                        Text(
                            "+${attempt.earnedXp} XP",
                            fontSize = 18.sp,
                            color = if (attempt.earnedXp > 0) Good else Subtle,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Streak", fontSize = 11.sp, color = Subtle)
                        Text(
                            if (streak > 0) "$streak hari" else "Belum mulai",
                            fontSize = if (streak > 0) 18.sp else 15.sp,
                            color = Navy,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (highlightStreak && streak in setOf(3, 7, 14, 30, 60, 100)) {
                    Text(
                        "Streak $streak hari! Pertahankan kebiasaan belajarmu.",
                        color = Good, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                if (syncStatus.isNotBlank()) {
                    Text(
                        syncStatus,
                        color = if (syncStatus.startsWith("Tersimpan di Supabase")) Good else Subtle,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
                VokasiButton(
                    text = "Lihat Hasil",
                    onClick = onViewResults,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun ResultNumber(label: String, amount: Int, prefix: String = "", suffix: String = "", modifier: Modifier, color: Color = Navy) {
    Column(modifier.background(Canvas, RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Subtle, fontWeight = FontWeight.Medium)
        AnimatedCounter(
            target = amount, prefix = prefix, suffix = suffix,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = color
        )
    }
}
