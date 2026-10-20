package com.example.vokasilearning.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*
import java.util.Calendar
import kotlin.math.max

@Composable
internal fun ProfileScreen(me: Student, attempts: List<Attempt>, onLogout: () -> Unit, onReview: (Attempt) -> Unit) {
    val mine = attempts.filter { it.studentId == me.username }.sortedByDescending { it.timeMillis }
    val wibToday = wibCalendar()
    val initialHistorySelection = remember(me.username) {
        HistorySelection(
            date = dayKey(System.currentTimeMillis()),
            month = wibToday.get(Calendar.MONTH) + 1,
            year = wibToday.get(Calendar.YEAR)
        )
    }
    var historySelection by remember(me.username) { mutableStateOf(initialHistorySelection) }
    var historyShown by remember(me.username) { mutableIntStateOf(5) }
    val filteredHistory = StudentHistoryFilter.filter(mine, historySelection)
    val shownHistory = filteredHistory.take(historyShown)
    val historyDateFormat = remember {
        java.text.SimpleDateFormat("dd MMM yyyy • HH:mm", java.util.Locale("id", "ID")).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
        }
    }
    val totalXp = lifetimeXp(me, attempts)
    val seasonXp = studentSeasonXp(me, attempts)
    val level = levelFor(totalXp)
    val totalQuestions = mine.sumOf { it.questionCount }
    val accuracy = if (totalQuestions > 0) mine.sumOf { it.correct } * 100 / totalQuestions else me.seedScore
    val streak = calculateStreak(me, attempts)
    val validDays = mine.filter { it.answered > 0 }.map { dayKey(it.timeMillis) }.toSet()
    val lastWeek = (6 downTo 0).map { offset ->
        val cal = wibCalendar().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        Pair(
            listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")[cal.get(Calendar.DAY_OF_WEEK) - 1],
            dayKey(cal.timeInMillis) in validDays
        )
    }
    var showLogout by remember { mutableStateOf(false) }

    val levelProgress = remember { Animatable(0f) }
    LaunchedEffect(totalXp) {
        val pointsRequired = (level.nextXp - level.startXp).coerceAtLeast(1)
        val target = ((totalXp - level.startXp).toFloat() / pointsRequired).coerceIn(0f, 1f)
        levelProgress.animateTo(target, animationSpec = tween(850, easing = FastOutSlowInEasing))
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StudentScreenHeader("Profil Siswa", "Semua perkembangan belajarmu ada di sini.")
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Avatar(me, 72.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            me.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("@${me.username}", color = Subtle, fontSize = 12.sp)
                        Text(me.classroom.label, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(me.classroom.major, color = Subtle, fontSize = 11.sp, maxLines = 2)
                    }
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentSectionHeading("PERKEMBANGAN", "Level & pencapaian")
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("LEVEL SAAT INI", color = NavyLight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Level ${level.level}", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                        }
                        Pill("$totalXp XP", PaleGold, Navy)
                    }
                    LinearProgressIndicator(
                        progress = { levelProgress.value },
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                        color = Good, trackColor = PaleGreen
                    )
                    Text(
                        "${max(0, level.nextXp - totalXp)} XP lagi menuju level ${level.level + 1}",
                        color = Subtle, fontSize = 11.sp
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileStatCard(
                        "Lifetime XP", totalXp.toString(), Navy,
                        Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        "Akurasi", "$accuracy%", Good,
                        Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        "Streak Aktif", "$streak hari", Warning,
                        Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentSectionHeading("KONSISTENSI", "Streak mingguan")
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$streak hari berturut-turut", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Aktivitas latihan 7 hari terakhir", color = Subtle, fontSize = 11.sp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lastWeek.forEach { (day, active) ->
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(33.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (active) Good else PaleBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (active) "✓" else "•",
                                        color = if (active) Color.White else Subtle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(day, color = Subtle, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentSectionHeading("RANGKUMAN", "Musim berjalan")
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(seasonTitle(), color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Pill("Aktif", PaleGreen, Good)
                    }
                    HorizontalDivider(color = Border)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("XP Season", color = Subtle, fontSize = 11.sp)
                            Text("$seasonXp XP", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Sesi selesai", color = Subtle, fontSize = 11.sp)
                            Text("${mine.size}", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        item {
            StudentHistoryFilterControls(
                applied = historySelection,
                onApply = { selection ->
                    historySelection = selection
                    historyShown = 5
                }
            )
        }
        item {
            Text(
                "${filteredHistory.size} sesi • Terbaru lebih dulu",
                color = Subtle, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        if (shownHistory.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(
                        if (mine.isEmpty()) "Belum ada riwayat latihan."
                        else "Tidak ada riwayat untuk filter ini.",
                        color = Subtle, fontSize = 13.sp
                    )
                }
            }
        } else {
            itemsIndexed(shownHistory, key = { position, attempt ->
                attempt.sessionKey.ifBlank { "${attempt.signature}_${attempt.timeMillis}_$position" }
            }) { _, attempt ->
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                attempt.practiceCategory?.let { "Latihan $it" } ?: "Latihan Wajib",
                                color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${historyDateFormat.format(java.util.Date(attempt.timeMillis))} WIB • ${attempt.correct}/${attempt.questionCount} benar",
                                color = Subtle, fontSize = 11.sp
                            )
                        }
                        Pill(
                            "${attempt.score}%",
                            when {
                                attempt.score >= 90 -> PaleGreen
                                attempt.score >= 75 -> PaleBlue
                                attempt.score >= 60 -> PaleGold
                                else -> Color(0xFFFFE7EA)
                            },
                            when {
                                attempt.score >= 90 -> Good
                                attempt.score >= 75 -> NavyLight
                                attempt.score >= 60 -> Color(0xFFA06C13)
                                else -> Bad
                            }
                        )
                    }
                    TextButton(onClick = { onReview(attempt) }) {
                        Text("Lihat Hasil →", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            if (historyShown < filteredHistory.size) {
                item {
                    TextButton(
                        onClick = { historyShown += 5 },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    ) {
                        Text(
                            "Muat 5 lagi (${filteredHistory.size - shownHistory.size} tersisa)",
                            color = Navy, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            VokasiButton(
                text = "Keluar dari Akun",
                onClick = { showLogout = true },
                isSecondary = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Keluar akun?", fontWeight = FontWeight.Bold) },
            text = { Text("Kamu perlu login kembali untuk mengakses aplikasi.") },
            confirmButton = {
                TextButton(onClick = { showLogout = false; onLogout() }) {
                    Text("Keluar", color = Bad)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(label, color = Subtle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
        }
    }
}
