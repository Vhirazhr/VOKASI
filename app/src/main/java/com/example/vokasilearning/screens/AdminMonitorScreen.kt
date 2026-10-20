package com.example.vokasilearning.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.DailyExcelReport
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun AdminMonitorScreen(students: List<Student>, attempts: List<Attempt>) {
    val context = LocalContext.current
    val classes = students.map { it.classroom.label }.distinct().sorted()
    var classroom by remember { mutableStateOf(classes.firstOrNull().orEmpty()) }
    var date by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var classMenu by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(10) }
    var reviewing by remember { mutableStateOf<Attempt?>(null) }
    var pendingExport by remember { mutableStateOf<Pair<String, String>?>(null) }

    val selectedMembers = students.filter { it.classroom.label == classroom }
    val summary = AdminAnalytics.classSummary(classroom, students, attempts, date)
    val todayResults = AdminAnalytics.latestCompulsoryByStudent(attempts, date)
    val sorted = selectedMembers.sortedWith(
        compareBy<Student> { if (todayResults.containsKey(it.username)) 1 else 0 }
            .thenBy { it.name }
    )
    val completed = summary.completedCount

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val selection = pendingExport
        pendingExport = null
        if (uri != null && selection != null) {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Tidak dapat membuat berkas Excel")
                stream.use {
                    DailyExcelReport.write(it, selection.first, selection.second, students, attempts)
                }
                Toast.makeText(context, "Excel berhasil disimpan", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(context, "Ekspor gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { VokasiTitle("Nilai Harian", "Rekap latihan wajib • data di perangkat ini") }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Kelas", color = Subtle, fontSize = 11.sp)
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { classMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(classroom.ifBlank { "Pilih kelas" }, maxLines = 1,
                                fontSize = 12.sp, color = Ink) }
                            DropdownMenu(
                                expanded = classMenu,
                                onDismissRequest = { classMenu = false }
                            ) {
                                classes.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            classroom = value; classMenu = false; visibleCount = 10
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Tanggal", color = Subtle, fontSize = 11.sp)
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                                val parts = date.split("-").mapNotNull { it.toIntOrNull() }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        date = String.format(Locale.US, "%04d-%02d-%02d",
                                            year, month + 1, day)
                                        visibleCount = 10
                                    },
                                    parts.getOrNull(0) ?: cal.get(Calendar.YEAR),
                                    (parts.getOrNull(1) ?: cal.get(Calendar.MONTH) + 1) - 1,
                                    parts.getOrNull(2) ?: cal.get(Calendar.DAY_OF_MONTH)
                                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(date, fontSize = 12.sp, color = Ink, maxLines = 1) }
                    }
                }
                VokasiButton(
                    text = "Unduh Excel",
                    enabled = classroom.isNotBlank(),
                    onClick = {
                        pendingExport = classroom to date
                        exporter.launch(
                            "VOKASI_Nilai_${classroom.replace(' ', '_')}_$date.xlsx"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Sudah", color = Good, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$completed", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Belum", color = Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${summary.missingCount}", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Rata-rata", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(summary.averageScore?.toString() ?: "—",
                        color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daftar Siswa", color = Ink, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("${summary.studentCount} siswa", color = Subtle, fontSize = 12.sp)
            }
        }
        if (sorted.isEmpty()) {
            item { WhiteCard(Modifier.fillMaxWidth()) { Text("Tidak ada siswa untuk kelas ini.", color = Subtle) } }
        } else {
            items(sorted.take(visibleCount), key = { it.username }) { student ->
                val result = todayResults[student.username]
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(student, 42.dp)
                        Column(Modifier.weight(1f)) {
                            Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = Ink, maxLines = 2)
                            Text(if (result == null) "Belum mengerjakan" else "Latihan wajib selesai",
                                fontSize = 11.sp, color = if (result == null) Warning else Good)
                        }
                        if (result == null) Pill("Belum", PaleGold, Warning)
                        else Pill("${result.score}/100", PaleBlue, Navy)
                    }
                    if (result != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${result.correct}/${result.questionCount} benar",
                                fontSize = 11.sp, color = Subtle, modifier = Modifier.weight(1f))
                            TextButton(onClick = { reviewing = result }) {
                                Text("Pembahasan", fontSize = 12.sp, color = Navy)
                            }
                        }
                    }
                }
            }
            if (sorted.size > visibleCount) {
                item {
                    TextButton(onClick = { visibleCount += 10 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Muat 10 lagi", color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    reviewing?.let { item ->
        AdminAnswerReviewDialog(attempt = item, onClose = { reviewing = null })
    }
}
