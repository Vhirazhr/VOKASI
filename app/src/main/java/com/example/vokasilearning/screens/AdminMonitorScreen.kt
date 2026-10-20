package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

/** Class/day compulsory grades; latest compulsory attempt per student. */
@Composable
internal fun AdminMonitorScreen(
    students: List<Student>,
    attempts: List<Attempt>,
    initialMajor: String = "",
    initialClass: String = "",
    initialDate: String = ""
) {
    val majors = students.map { it.classroom.major }.distinct().sorted()
    var major by remember(initialMajor) {
        mutableStateOf(initialMajor.takeIf { it in majors } ?: majors.firstOrNull().orEmpty())
    }
    val classes = students.filter { it.classroom.major == major }
        .map { it.classroom.label }.distinct().sorted()
    var classroom by remember(initialMajor, initialClass, major) {
        mutableStateOf(initialClass.takeIf { it in classes } ?: classes.firstOrNull().orEmpty())
    }
    var date by remember(initialDate) {
        mutableStateOf(initialDate.ifBlank { dayKey(System.currentTimeMillis()) })
    }
    var search by remember { mutableStateOf("") }
    var onlyMissing by remember { mutableStateOf(false) }
    var page by remember(major, classroom, date, search, onlyMissing) {
        mutableIntStateOf(0)
    }
    var pagingActivated by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var reviewing by remember { mutableStateOf<Attempt?>(null) }

    val summary = AdminAnalytics.classSummary(classroom, students, attempts, date, major)
    val latest = AdminAnalytics.latestCompulsoryByStudent(attempts, date)
    val members = students.filter {
        it.classroom.major == major && it.classroom.label == classroom &&
            (search.isBlank() || it.name.contains(search.trim(), true) ||
                it.username.contains(search.trim(), true))
    }.filter { !onlyMissing || latest[it.username] == null }
        .sortedWith(
            compareBy<Student> { if (latest[it.username] == null) 1 else 0 }.thenBy { it.name }
        )
    val pageSize = 15
    val pageCount = AdminPagination.count(members.size, pageSize)
    val activePage = AdminPagination.active(page, members.size, pageSize)
    val visibleMembers = AdminPagination.slice(members, activePage, pageSize)
    LaunchedEffect(activePage, pagingActivated) {
        if (pagingActivated) listState.scrollToItem(1)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        state = listState,
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Nilai Siswa", "Rekap latihan wajib per kelas dan tanggal", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("LAPORAN KELAS", color = NavyLight, fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Pill("Lokal / demo", PaleGold, Warning)
                        }
                        AdminSelect("Jurusan", major, majors, { major = it },
                            Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Kelas", classroom, classes, { classroom = it },
                                Modifier.weight(1f))
                            AdminDateSelect(date, { date = it }, Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Border)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSummaryMetric("Selesai", "${summary.completedCount}", Good, Modifier.weight(1f))
                            AdminSummaryMetric("Belum", "${summary.missingCount}", Warning, Modifier.weight(1f))
                            AdminSummaryMetric("Rata-rata", summary.averageScore?.toString() ?: "—",
                                Navy, Modifier.weight(1f))
                        }
                        AdminExcelAction(
                            students, attempts, major, classroom, date,
                            modifier = Modifier.fillMaxWidth(), text = "Unduh Excel Kelas"
                        )
                        Text("Nilai wajib terakhir pada tanggal terpilih • ${summary.studentCount} siswa",
                            color = Subtle, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                AdminSectionTitle("Hasil terbaru", "Daftar nilai siswa")
                OutlinedTextField(
                    value = search, onValueChange = { search = it; pagingActivated = false },
                    placeholder = { Text("Cari nama atau username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !onlyMissing,
                        onClick = { onlyMissing = false; pagingActivated = false },
                        label = { Text("Semua", fontSize = 12.sp) }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = onlyMissing,
                        onClick = { onlyMissing = true; pagingActivated = false },
                        label = { Text("Belum mengerjakan", fontSize = 12.sp) }
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${members.size} siswa", color = Subtle, fontSize = 11.sp)
                }
            }
        }
        if (members.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tidak ada hasil yang cocok.", color = Subtle, fontSize = 12.sp)
                }
            }
        } else {
            items(visibleMembers, key = { it.username }) { student ->
                val result = latest[student.username]
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Avatar(student, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(student.name, color = Ink, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2)
                            Text(student.classroom.label, color = Subtle, fontSize = 11.sp)
                        }
                        Pill(
                            result?.let { "${it.score}/100" } ?: "Belum",
                            when {
                                result == null -> PaleGold
                                result.score < 60 -> Color(0xFFFFE7EA)
                                result.score >= 90 -> PaleGreen
                                else -> PaleBlue
                            },
                            when {
                                result == null -> Warning
                                result.score < 60 -> Bad
                                result.score >= 90 -> Good
                                else -> Navy
                            }
                        )
                    }
                    if (result != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${result.correct}/${result.questionCount} benar",
                                color = Subtle, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { reviewing = result }) {
                                Text("Lihat Jawaban", color = Navy, fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Siswa ${activePage * pageSize + 1}–${activePage * pageSize + visibleMembers.size} dari ${members.size}",
                        fontSize = 11.sp, color = Subtle
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            enabled = activePage > 0,
                            onClick = { page = activePage - 1; pagingActivated = true },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Sebelumnya", fontSize = 12.sp) }
                        Text("${activePage + 1}/$pageCount",
                            color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            enabled = activePage < pageCount - 1,
                            onClick = { page = activePage + 1; pagingActivated = true },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Berikutnya", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
    reviewing?.let { result ->
        val owner = students.firstOrNull { it.username == result.studentId }
        AdminAnswerReviewDialog(
            attempt = result,
            studentName = owner?.name ?: result.studentNameSnapshot,
            classroom = owner?.classroom?.label ?: result.classSnapshot,
            onClose = { reviewing = null }
        )
    }
}

@Composable
private fun AdminSummaryMetric(
    label: String, value: String, accent: Color, modifier: Modifier = Modifier
) {
    Column(
        modifier.background(Canvas, RoundedCornerShape(14.dp))
            .padding(horizontal = 3.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 21.sp, color = Ink, fontWeight = FontWeight.ExtraBold)
    }
}
