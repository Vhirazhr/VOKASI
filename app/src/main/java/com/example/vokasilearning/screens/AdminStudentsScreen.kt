package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

/** Scalable local directory organized by department, then class. */
@Composable
internal fun AdminStudentsScreen(students: List<Student>, attempts: List<Attempt>) {
    var search by remember { mutableStateOf("") }
    var expandedMajor by remember { mutableStateOf<String?>(null) }
    var expandedClass by remember { mutableStateOf<String?>(null) }
    val visibleCounts = remember { mutableStateMapOf<String, Int>() }
    var selected by remember { mutableStateOf<Student?>(null) }
    var review by remember { mutableStateOf<Attempt?>(null) }

    val searchText = search.trim()
    val filtered = students.filter {
        searchText.isBlank() ||
            it.name.contains(searchText, ignoreCase = true) ||
            it.username.contains(searchText, ignoreCase = true) ||
            it.classroom.label.contains(searchText, ignoreCase = true) ||
            it.classroom.major.contains(searchText, ignoreCase = true)
    }
    val groups = filtered.groupBy { it.classroom.major }.toSortedMap()

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Data Siswa", "Direktori berdasarkan jurusan dan kelas", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp).padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("CARI SISWA", color = NavyLight, fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Nama, username, kelas, atau jurusan") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp)
                        )
                        Text("${filtered.size} siswa • ${groups.size} jurusan • Data lokal",
                            color = Subtle, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            AdminSectionTitle("Struktur sekolah", "Jurusan & kelas",
                Modifier.padding(horizontal = 18.dp))
        }
        if (groups.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tidak ada siswa yang cocok.", color = Subtle)
                }
            }
        }
        groups.forEach { (major, members) ->
            item(key = major) {
                val byClass = members.groupBy { it.classroom.label }.toSortedMap()
                val showMajor = expandedMajor == major || searchText.isNotBlank()
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(major, color = Ink, fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp)
                            Text("${byClass.size} kelas • ${members.size} siswa",
                                color = Subtle, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            expandedMajor = if (expandedMajor == major) null else major
                        }) {
                            Text(if (showMajor) "Tutup" else "Lihat kelas",
                                color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (showMajor) {
                        byClass.forEach { (classroom, classMembers) ->
                            val key = "$major/$classroom"
                            val showClass = expandedClass == key || searchText.isNotBlank()
                            HorizontalDivider(color = Border)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(classroom, color = Ink, fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp)
                                    Text("${classMembers.size} siswa", color = Subtle, fontSize = 11.sp)
                                }
                                TextButton(onClick = {
                                    expandedClass = if (expandedClass == key) null else key
                                }) {
                                    Text(if (showClass) "Tutup" else "Lihat siswa",
                                        color = Navy, fontSize = 12.sp)
                                }
                            }
                            if (showClass) {
                                val sorted = classMembers.sortedBy { it.name }
                                val count = visibleCounts[key] ?: 10
                                sorted.take(count).forEach { student ->
                                    val recent = attempts.filter {
                                        it.studentId == student.username && it.practiceCategory == null
                                    }.maxByOrNull { it.timeMillis }
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Avatar(student, 36.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text(student.name, fontSize = 12.sp, color = Ink,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                recent?.let { "Nilai wajib terakhir: ${it.score}/100" } ?: "Belum ada nilai wajib",
                                                color = Subtle, fontSize = 10.sp, maxLines = 1
                                            )
                                        }
                                        TextButton(
                                            onClick = { selected = student },
                                            contentPadding = PaddingValues(horizontal = 5.dp)
                                        ) { Text("Detail", fontSize = 12.sp, color = Navy) }
                                    }
                                }
                                if (sorted.size > count) {
                                    TextButton(onClick = { visibleCounts[key] = count + 10 },
                                        modifier = Modifier.fillMaxWidth()) {
                                        Text("Muat 10 lagi", fontSize = 12.sp, color = Navy)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { student ->
        val history = attempts.filter {
            it.studentId == student.username && it.practiceCategory == null
        }.sortedByDescending { it.timeMillis }
        var count by remember(student.username) { mutableIntStateOf(5) }
        val format = remember {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
        }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(student.name, fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("${student.classroom.major} • ${student.classroom.label}",
                            color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("@${student.username} • ${history.size} sesi wajib",
                            color = Subtle, fontSize = 11.sp)
                    }
                    history.take(count).forEach { attempt ->
                        item(key = attempt.sessionKey.ifBlank { "${attempt.timeMillis}" }) {
                            HorizontalDivider(color = Border)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(format.format(Date(attempt.timeMillis)) + " WIB",
                                        color = Subtle, fontSize = 11.sp)
                                    Text("${attempt.score}/100 • ${attempt.correct}/${attempt.questionCount} benar",
                                        color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                TextButton(onClick = { review = attempt }) {
                                    Text("Jawaban", color = Navy, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (history.isEmpty()) item { Text("Belum mengerjakan latihan wajib.", color = Subtle) }
                    if (history.size > count) item {
                        TextButton(onClick = { count += 5 }, modifier = Modifier.fillMaxWidth()) {
                            Text("Muat 5 lagi", color = Navy)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Tutup", color = Navy) }
            }
        )
    }
    review?.let { attempt ->
        AdminAnswerReviewDialog(attempt = attempt, onClose = { review = null })
    }
}
