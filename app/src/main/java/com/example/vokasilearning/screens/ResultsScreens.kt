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

@Composable
internal fun ResultPage(attempt: Attempt, onHome: () -> Unit, onRanking: () -> Unit) {
    val timedOut = attempt.review.count { it.status == AnswerStatus.TIMEOUT }
    val incorrect = attempt.review.count { it.status == AnswerStatus.SALAH }
    val missing = attempt.review.count { it.status == AnswerStatus.TIDAK_DIJAWAB }
    var showDetails by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { VokasiTitle("Hasil Latihan", "100 soal • Skor dihitung otomatis setelah sesi selesai") }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedCounter(
                        target = attempt.score,
                        suffix = "%",
                        textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Navy
                    )
                    Text("${attempt.correct} benar dari ${attempt.questionCount} soal", color = Subtle, fontSize = 14.sp)
                    Pill("Grade: ${scoreGrade(attempt.score)}", PaleGreen, Good)
                    Text(
                        "Benar ${attempt.correct} • Salah $incorrect • Timeout $timedOut • Kosong $missing",
                        color = Ink, fontSize = 12.sp
                    )
                    Text("Durasi: ${clockText(attempt.secondsSpent)} • XP +${attempt.earnedXp}", color = Subtle, fontSize = 12.sp)
                    Text("Laporan saat ini tersimpan pada perangkat, belum tersinkron ke server sekolah.", color = Subtle, fontSize = 11.sp)
                }
            }
        }
        item {
            VokasiButton(
                text = if (showDetails) "Tutup Pembahasan" else "Lihat Pembahasan 100 Soal",
                onClick = { showDetails = !showDetails },
                modifier = Modifier.fillMaxWidth(),
                isSecondary = true
            )
        }
        if (showDetails) {
            itemsIndexed(attempt.review) { i, item ->
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Soal ${i + 1} • ${item.question.category} (${item.question.difficultyCode})",
                            color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.weight(1f))
                        Pill(
                            item.status.name.replace('_',' '),
                            if (item.status == AnswerStatus.BENAR) PaleGreen else PaleGold,
                            if (item.status == AnswerStatus.BENAR) Good else Warning
                        )
                    }
                    Text(item.question.question, color = Ink, fontSize = 13.sp)
                    Text(
                        "Jawabanmu: ${item.selectedIndex?.let { item.question.options[it] } ?: "Tidak dijawab / timeout"}",
                        color = Subtle, fontSize = 12.sp
                    )
                    Text("Jawaban benar: ${item.question.options[item.question.answerIndex]}",
                        color = Good, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (item.question.explanation.isNotBlank()) {
                        Text("Pembahasan: ${item.question.explanation}", color = Subtle, fontSize = 12.sp)
                    }
                    Text("Waktu: ${item.responseSeconds}/${item.question.timeLimitSeconds} detik", color = Subtle, fontSize = 11.sp)
                }
            }
        }
        item {
            VokasiButton(text = "Kembali ke Beranda", onClick = onHome, modifier = Modifier.fillMaxWidth())
        }
        item {
            VokasiButton(text = "Lihat Papan Peringkat", onClick = onRanking, isSecondary = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ResultPopup(
    me: Student, users: List<Student>, attempts: List<Attempt>, attempt: Attempt,
    onClose: () -> Unit, onAllRanking: () -> Unit
) {
    val scores = users.filter { it.classroom.label == me.classroom.label }.map { user ->
        val last = attempts.lastOrNull { it.studentId == user.username }
        user to (last?.score ?: user.seedScore)
    }.sortedByDescending { it.second }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth(0.92f).heightIn(max = 700.dp).shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Latihan Selesai!", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Nilai dihitung otomatis", color = Subtle, fontSize = 13.sp)
                }

                // Animated Stats Row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultNumber(label = "Nilai", amount = attempt.score, suffix = "%", modifier = Modifier.weight(1f))
                    ResultNumber(label = "XP", amount = attempt.earnedXp, prefix = "+", modifier = Modifier.weight(1f), color = Good)
                    ResultNumber(label = "Streak", amount = calculateStreak(me, attempts), prefix = "🔥 ", modifier = Modifier.weight(1f), color = Warning)
                }

                Text("Peringkat Kelas • ${me.classroom.label}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(scores) { index, pair ->
                        Row(
                            Modifier.fillMaxWidth().background(if (pair.first.username == me.username) PaleBlue else Canvas, RoundedCornerShape(12.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Subtle, modifier = Modifier.width(24.dp))
                            Avatar(pair.first, 32.dp)
                            Text(pair.first.name, Modifier.weight(1f), fontSize = 13.sp, fontWeight = if (pair.first.username == me.username) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${pair.second}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                VokasiButton(text = "Tutup / Beranda", onClick = onClose, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onAllRanking, modifier = Modifier.fillMaxWidth()) {
                    Text("Buka papan peringkat lengkap", color = Navy, fontWeight = FontWeight.SemiBold)
                }
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
