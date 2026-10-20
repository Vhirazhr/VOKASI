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
internal fun StudentHome(
    me: Student, students: List<Student>, attempts: List<Attempt>, countQuestions: Int,
    onStart: () -> Unit, onRanking: () -> Unit
) {
    val today = dayKey(System.currentTimeMillis())
    val todayCount = attempts.count { it.studentId == me.username && dayKey(it.timeMillis) == today }
    val seasonXp = studentSeasonXp(me, attempts)
    val streak = calculateStreak(me, attempts)
    val numberOfQuestions = minOf(QUESTIONS_PER_SESSION, countQuestions)
    val topStudents = rankList(students, attempts, me, RankingScope.SCHOOL, RankingMetric.XP).take(3)

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().height(285.dp)
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF294D7A))))
                        .padding(22.dp)
                ) {
                    Box(
                        Modifier.size(220.dp).align(Alignment.TopEnd)
                            .offset(x = 125.dp, y = 30.dp)
                            .border(40.dp, Color.White.copy(alpha = 0.035f), CircleShape)
                    )
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) { Text("▣", color = Color.White, fontSize = 24.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("VOKASI", color = Color.White, fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                            Text("LEARNING", color = Color.White.copy(alpha = 0.72f),
                                fontSize = 8.sp, letterSpacing = 2.sp)
                        }
                        Text(seasonTitle(), color = Color(0xFFCBD7E5), fontSize = 10.sp,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 9.dp, vertical = 8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.CenterStart).padding(bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Selamat datang,", color = Color(0xFFC7D5E6), fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Halo, ${me.name.substringBefore(' ')}!",
                                color = Color.White, fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(7.dp))
                            Text("${me.classroom.label} • SMKN 1 Singosari",
                                color = Color(0xFFD7E1ED), fontSize = 11.sp)
                        }
                        Avatar(me, 57.dp)
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 209.dp)
                        .shadow(15.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦  LATIHAN WAJIB HARI INI",
                                modifier = Modifier.weight(1f), color = NavyLight,
                                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Pill(
                                if (todayCount > 0) "Sudah latihan" else "Siap berlatih",
                                PaleGold, Color(0xFFAA7928)
                            )
                        }
                        Text("Siap tingkatkan\nkemampuanmu?", color = Ink,
                            fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
                            lineHeight = 29.sp)
                        Text(
                            "Soal penjumlahan, pengurangan, perkalian, dan pembagian dipilih otomatis oleh sistem.",
                            color = Subtle, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        HorizontalDivider(color = Border)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HomeQuickStat("✦", "$numberOfQuestions", "soal acak",
                                PaleBlue, NavyLight, Modifier.weight(1f))
                            HomeQuickStat("◷", "${numberOfQuestions * SECONDS_PER_QUESTION / 60} mnt",
                                "waktu sesi", PaleGreen, Good, Modifier.weight(1f))
                            HomeQuickStat("★", "+$SESSION_XP XP", "sesi valid",
                                PaleGold, Warning, Modifier.weight(1f))
                        }
                        VokasiButton(
                            text = "Mulai Mengerjakan  →", onClick = onStart,
                            enabled = countQuestions > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("♢  Sistem menentukan soal dan batas waktu secara otomatis",
                            color = Subtle, fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
        item {
            StaggeredSlideIn(0) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("PROGRES HARI INI", fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold, color = NavyLight, letterSpacing = 1.sp)
                    Text("Satu langkah lebih baik", color = Ink,
                        fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(17.dp),
                            horizontalArrangement = Arrangement.spacedBy(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { if (todayCount > 0) 1f else 0f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Good, trackColor = PaleGreen, strokeWidth = 7.dp
                                )
                                Text(if (todayCount > 0) "100%" else "0%",
                                    color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    if (todayCount > 0) "Latihan harian tercatat!" else "Yuk, mulai latihan pertamamu!",
                                    color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                                Text("$todayCount sesi hari ini • $streak hari streak",
                                    color = Subtle, fontSize = 11.sp)
                                LinearProgressIndicator(
                                    progress = { if (todayCount > 0) 1f else 0f },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = Good, trackColor = PaleGreen
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            StaggeredSlideIn(1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhiteCard(Modifier.weight(1f)) {
                        Text("XP Musim Ini", color = Subtle, fontSize = 11.sp)
                        AnimatedCounter(
                            target = seasonXp,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Navy
                        )
                    }
                    WhiteCard(Modifier.weight(1f)) {
                        Text("Streak Harian", color = Subtle, fontSize = 11.sp)
                        Text("🔥 $streak hari", color = Ink, fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp)
                    }
                }
            }
        }
        item {
            StaggeredSlideIn(2) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PAPAN PERINGKAT", color = NavyLight,
                                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Teratas musim ini", color = Ink,
                                fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = onRanking) {
                            Text("Lihat semua →", color = Navy, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(21.dp)),
                        shape = RoundedCornerShape(21.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            topStudents.forEachIndexed { index, student ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (student.username == me.username) PaleBlue else Color.White)
                                        .padding(horizontal = 9.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text("${index + 1}", modifier = Modifier.width(17.dp),
                                        color = if (index == 0) Warning else Subtle,
                                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    Avatar(student, 38.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text(student.name, color = Ink, fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(student.classroom.label, color = Subtle, fontSize = 10.sp)
                                    }
                                    Text("${studentSeasonXp(student, attempts)} XP",
                                        color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (index < topStudents.lastIndex) HorizontalDivider(color = Border)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickStat(
    symbol: String, value: String, label: String, bg: Color, fg: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(5.dp))
        Column {
            Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Subtle, fontSize = 9.sp)
        }
    }
}
