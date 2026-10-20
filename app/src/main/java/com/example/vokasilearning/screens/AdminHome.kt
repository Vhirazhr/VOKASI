package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    onStudents: () -> Unit, onQuestions: () -> Unit,
    onMonitoring: () -> Unit, onLogout: () -> Unit
) {
    val today = dayKey(System.currentTimeMillis())
    val classes = AdminAnalytics.allClasses(students, attempts, today)
    val completed = classes.sumOf { it.completedCount }
    val pending = (students.size - completed).coerceAtLeast(0)
    val todaySessions = AdminAnalytics.compulsoryOnDate(attempts, today)
    var showLogout by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Navy, DeepNavy)),
                    RoundedCornerShape(22.dp)
                ).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("VOKASI / ADMIN", color = Color(0xFFCFDEEF),
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Dashboard Sekolah", color = Color.White, fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("Ringkasan latihan wajib hari ini • $today",
                    color = Color(0xFFE1EAF3), fontSize = 12.sp)
                Surface(color = Color.White.copy(alpha = 0.13f), shape = RoundedCornerShape(11.dp)) {
                    Text("Data lokal / demo", color = Color.White,
                        fontSize = 11.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp))
                }
            }
        }
        item {
            Text("Aktivitas Hari Ini", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Sudah latihan", color = Good, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$completed", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("dari ${students.size} siswa", color = Subtle, fontSize = 11.sp)
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Belum latihan", color = Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$pending", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("siswa terdaftar", color = Subtle, fontSize = 11.sp)
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kehadiran latihan wajib", color = Ink, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        Text("${todaySessions.size} sesi tercatat hari ini",
                            fontSize = 11.sp, color = Subtle)
                    }
                    Text(
                        if (students.isEmpty()) "—" else "${completed * 100 / students.size}%",
                        color = Navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                LinearProgressIndicator(
                    progress = { if (students.isEmpty()) 0f else completed.toFloat() / students.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Good, trackColor = PaleGreen
                )
            }
        }
        item {
            Text("Kelola", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Data Siswa", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${students.size} akun lokal", color = Subtle, fontSize = 11.sp)
                    VokasiButton("Buka Siswa", onClick = onStudents,
                        modifier = Modifier.fillMaxWidth())
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Nilai Harian", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Per kelas & tanggal", color = Subtle, fontSize = 11.sp)
                    VokasiButton("Buka Nilai", onClick = onMonitoring,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bank Soal", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${questions.size} soal di perangkat", color = Subtle, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = onQuestions, shape = RoundedCornerShape(12.dp)) {
                        Text("Kelola", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ringkasan Per Kelas", color = Ink, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("${classes.size} kelas", color = Subtle, fontSize = 11.sp)
            }
        }
        if (classes.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth()) {
                    Text("Belum ada data kelas.", color = Subtle)
                }
            }
        } else {
            classes.forEach { summary ->
                item(key = summary.classroom) {
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(summary.classroom, color = Ink, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                                Text("${summary.completedCount} dari ${summary.studentCount} sudah latihan",
                                    color = Subtle, fontSize = 11.sp)
                            }
                            Text(
                                summary.averageScore?.let { "Rata-rata $it" } ?: "Belum ada nilai",
                                color = if (summary.averageScore == null) Subtle else Navy,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (summary.studentCount == 0) 0f
                                else summary.completedCount.toFloat() / summary.studentCount
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Good, trackColor = PaleGreen
                        )
                    }
                }
            }
        }
        item {
            TextButton(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Keluar Akun", color = Subtle, fontWeight = FontWeight.Medium)
            }
        }
    }
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Keluar dari admin?", fontWeight = FontWeight.Bold) },
            text = { Text("Kamu perlu masuk kembali untuk membuka panel admin.") },
            confirmButton = {
                TextButton(onClick = { showLogout = false; onLogout() }) {
                    Text("Keluar", color = Bad)
                }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Batal") } }
        )
    }
}
