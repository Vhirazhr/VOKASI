package com.example.vokasilearning.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A local student directory. Deliberately no fake edit/sync controls before backend exists. */
@Composable
internal fun AdminStudentsScreen(students: List<Student>, attempts: List<Attempt>) {
    var query by remember { mutableStateOf("") }
    var selectedMajor by remember { mutableStateOf("Semua jurusan") }
    var selectedClass by remember { mutableStateOf("Semua kelas") }
    var majorMenu by remember { mutableStateOf(false) }
    var classMenu by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(10) }
    var selected by remember { mutableStateOf<Student?>(null) }
    var review by remember { mutableStateOf<Attempt?>(null) }

    val majors = listOf("Semua jurusan") + students.map { it.classroom.major }.distinct().sorted()
    val available = students.filter {
        selectedMajor == "Semua jurusan" || it.classroom.major == selectedMajor
    }
    val classes = listOf("Semua kelas") + available.map { it.classroom.label }.distinct().sorted()
    val found = available.filter { student ->
        (selectedClass == "Semua kelas" || student.classroom.label == selectedClass) &&
            (query.isBlank() || student.name.contains(query.trim(), ignoreCase = true) ||
                student.username.contains(query.trim(), ignoreCase = true) ||
                student.classroom.label.contains(query.trim(), ignoreCase = true))
    }.sortedWith(compareBy<Student> { it.classroom.label }.thenBy { it.name })

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            VokasiTitle("Data Siswa", "Cari siswa, kelas, dan perkembangan belajarnya.")
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; visibleCount = 10 },
                placeholder = { Text("Cari nama atau username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyLight,
                    unfocusedBorderColor = Border
                )
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { majorMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text(selectedMajor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    DropdownMenu(expanded = majorMenu, onDismissRequest = { majorMenu = false }) {
                        majors.forEach { major ->
                            DropdownMenuItem(text = { Text(major) }, onClick = {
                                selectedMajor = major
                                selectedClass = "Semua kelas"
                                majorMenu = false
                                visibleCount = 10
                            })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { classMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text(selectedClass, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        classes.forEach { classroom ->
                            DropdownMenuItem(text = { Text(classroom) }, onClick = {
                                selectedClass = classroom
                                classMenu = false
                                visibleCount = 10
                            })
                        }
                    }
                }
            }
        }
        item {
            Text("${found.size} siswa • Data di perangkat ini", color = Subtle, fontSize = 12.sp)
        }
        if (found.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth()) {
                    Text("Tidak ada siswa yang cocok.", color = Subtle, fontSize = 13.sp)
                }
            }
        } else {
            items(found.take(visibleCount), key = { it.username }) { student ->
                val myAttempts = attempts.filter { it.studentId == student.username }
                val latestCompulsory = myAttempts.filter { it.practiceCategory == null }.maxByOrNull { it.timeMillis }
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Avatar(student, 46.dp)
                        Column(Modifier.weight(1f)) {
                            Text(student.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${student.classroom.label} • ${student.classroom.major}",
                                color = Subtle, fontSize = 10.sp, maxLines = 2)
                        }
                        Pill(latestCompulsory?.let { "${it.score}/100" } ?: "Belum ada nilai",
                            if (latestCompulsory == null) PaleGold else PaleBlue,
                            if (latestCompulsory == null) Warning else Navy)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${myAttempts.count { it.practiceCategory == null }} sesi wajib",
                            fontSize = 11.sp, color = Subtle, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selected = student }) {
                            Text("Detail Siswa", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (found.size > visibleCount) {
                item {
                    TextButton(onClick = { visibleCount += 10 },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Muat 10 lagi", color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selected?.let { student ->
        val history = attempts.filter { it.studentId == student.username && it.practiceCategory == null }
            .sortedByDescending { it.timeMillis }
        val last = history.firstOrNull()
        var showCount by remember(student.username) { mutableIntStateOf(5) }
        val df = remember {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
        }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(student.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 410.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    item {
                        Text("${student.classroom.label} • ${student.classroom.major}", fontSize = 12.sp, color = Subtle)
                        Text("Username: ${student.username}", fontSize = 12.sp, color = Subtle)
                        Text("${history.size} sesi wajib • Nilai terakhir: ${last?.score?.toString() ?: "Belum ada"}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                    }
                    items(history.take(showCount), key = { it.sessionKey.ifBlank { it.timeMillis.toString() } }) { attempt ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            HorizontalDivider(color = Border)
                            Text(df.format(Date(attempt.timeMillis)) + " WIB",
                                fontSize = 11.sp, color = Subtle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Nilai ${attempt.score} • ${attempt.correct}/${attempt.questionCount} benar",
                                    color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { review = attempt }) {
                                    Text("Jawaban", fontSize = 11.sp, color = Navy)
                                }
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        item { Text("Belum mengerjakan latihan wajib.", color = Subtle, fontSize = 12.sp) }
                    } else if (showCount < history.size) {
                        item {
                            TextButton(onClick = { showCount += 5 }, modifier = Modifier.fillMaxWidth()) {
                                Text("Muat 5 lagi", color = Navy)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } }
        )
    }
    review?.let { attempt ->
        AdminAnswerReviewDialog(attempt = attempt, onClose = { review = null })
    }
}
