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
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.vokasilearning.data.DailyExcelReport
@Composable
internal fun AdminMonitorScreen(students: List<Student>, attempts: List<Attempt>) {
    val context = LocalContext.current
    var selectedClass by remember { mutableStateOf("Semua kelas") }
    var selectedDate by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var filterOpen by remember { mutableStateOf(false) }
    var detailAttempt by remember { mutableStateOf<Attempt?>(null) }
    var focusedStudent by remember { mutableStateOf<String?>(null) }
    val classes = listOf("Semua kelas") + students.map { it.classroom.label }.distinct().sorted()
    val filtered = students.filter { selectedClass == "Semua kelas" || it.classroom.label == selectedClass }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null && selectedClass != "Semua kelas") {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Tidak dapat membuka file")
                DailyExcelReport.write(stream, selectedClass, selectedDate, students, attempts)
                Toast.makeText(context, "Excel berhasil dibuat", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VokasiTitle("Monitoring Siswa", "Histori latihan tersimpan lokal pada perangkat ini") }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Rekap Nilai Harian", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("Pilih satu kelas dan satu tanggal. Excel hanya berisi nilai per siswa, satu sheet.",
                    color = Subtle, fontSize = 12.sp)
                Box {
                    OutlinedButton(
                        onClick = { filterOpen = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(selectedClass, color = Ink) }
                    DropdownMenu(
                        expanded = filterOpen,
                        onDismissRequest = { filterOpen = false }
                    ) {
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = { selectedClass = cls; filterOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tanggal (YYYY-MM-DD)") },
                    singleLine = true
                )
                val isValid = Regex("\\d{4}-\\d{2}-\\d{2}").matches(selectedDate)
                VokasiButton(
                    text = "Download Excel (.xlsx)",
                    onClick = {
                        val filename = "VOKASI_Nilai_${selectedClass.replace(' ', '_')}_$selectedDate.xlsx"
                        fileLauncher.launch(filename)
                    },
                    enabled = selectedClass != "Semua kelas" && isValid,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ekspor ini menggunakan data yang ada di perangkat, bukan nilai terverifikasi server.",
                    color = Subtle, fontSize = 10.sp)
            }
        }
        item { Text("${filtered.size} siswa terdaftar pada filter ini", color = Subtle, fontSize = 12.sp) }
        items(filtered, key = { it.username }) { student ->
            val history = attempts.filter { it.studentId == student.username }.sortedByDescending { it.timeMillis }
            val last = history.firstOrNull()
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Avatar(student)
                    Column(Modifier.weight(1f)) {
                        Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("${student.classroom.label} • ${student.classroom.major}",
                            fontSize = 11.sp, color = Subtle)
                    }
                    Pill(last?.let { "${it.score}%" } ?: "Belum", PaleBlue, Navy)
                }
                Text("${history.size} sesi selesai • ${studentSeasonXp(student, attempts)} XP",
                    color = Subtle, fontSize = 12.sp)
                if (history.isNotEmpty()) {
                    TextButton(onClick = {
                        focusedStudent = if (focusedStudent == student.username) null else student.username
                    }) {
                        Text(if (focusedStudent == student.username) "Tutup riwayat" else "Lihat riwayat & jawaban",
                            color = Navy)
                    }
                    if (focusedStudent == student.username) {
                        history.forEach { session ->
                            HorizontalDivider(color = Border)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${dayKey(session.timeMillis)} • Nilai ${session.score}% • ${session.correct}/${session.questionCount} benar",
                                    color = Ink, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { detailAttempt = session }) { Text("Detail", color = Navy) }
                            }
                        }
                    }
                }
            }
        }
    }

    detailAttempt?.let { session ->
        AlertDialog(
            onDismissRequest = { detailAttempt = null },
            title = { Text("Detail ${session.review.size} Soal", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(session.review) { i, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${i + 1}. ${item.question.question}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Status: ${item.status.name}", fontSize = 11.sp)
                            Text("Jawaban siswa: ${item.selectedIndex?.let { item.question.options[it] } ?: "Tidak dijawab"}",
                                fontSize = 11.sp)
                            Text("Kunci: ${item.question.options[item.question.answerIndex]}",
                                fontSize = 11.sp, color = Good)
                            if (item.question.explanation.isNotBlank()) {
                                Text(item.question.explanation, fontSize = 11.sp)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailAttempt = null }) { Text("Tutup") }
            }
        )
    }
}
