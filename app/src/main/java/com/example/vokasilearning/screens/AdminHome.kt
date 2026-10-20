package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

/** Grade-first home: exactly one selected major/class/date at a time. */
@Composable
internal fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    onStudents: () -> Unit,
    onMonitoring: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val majors = students.map { it.classroom.major }.distinct().sorted()
    var major by remember { mutableStateOf(majors.firstOrNull().orEmpty()) }
    val classOptions = students.filter { it.classroom.major == major }
        .map { it.classroom.label }.distinct().sorted()
    var classroom by remember(major) { mutableStateOf(classOptions.firstOrNull().orEmpty()) }
    var date by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var expandedMajor by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    val summary = AdminAnalytics.classSummary(classroom, students, attempts, date, major)
    val byStudent = AdminAnalytics.latestCompulsoryByStudent(attempts, date)
    val classMembers = students.filter {
        it.classroom.major == major && it.classroom.label == classroom
    }
    val ordered = classMembers.sortedWith(
        compareBy<Student> { if (byStudent[it.username] == null) 1 else 0 }.thenBy { it.name }
    )
    val allClasses = AdminAnalytics.allClasses(students, attempts, date)
    val byMajor = allClasses.groupBy { it.major }.toSortedMap()

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero(
                    title = "Halo, Admin!",
                    subtitle = "Pantau hasil latihan siswa • SMKN 1 Singosari"
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 198.dp)
                        .shadow(14.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HASIL LATIHAN WAJIB",
                                modifier = Modifier.weight(1f), color = NavyLight,
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Pill("Data lokal", PaleGold, Warning)
                        }
                        Text("Cek nilai siswa", color = Ink, fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Pilih jurusan, kelas, dan tanggal.",
                            color = Subtle, fontSize = 12.sp)
                        AdminSelect("Jurusan", major, majors, { major = it },
                            Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Kelas", classroom, classOptions, { classroom = it },
                                Modifier.weight(1f))
                            AdminDateSelect(date, { date = it }, Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Border)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminStatChip("Sudah", "${summary.completedCount}", Good,
                                Modifier.weight(1f))
                            AdminStatChip("Belum", "${summary.missingCount}", Warning,
                                Modifier.weight(1f))
                            AdminStatChip("Rata-rata",
                                summary.averageScore?.toString() ?: "—", Navy,
                                Modifier.weight(1f))
                        }
                        Text(
                            "${summary.studentCount} siswa di $classroom • Nilai wajib terbaru per siswa",
                            fontSize = 11.sp, color = Subtle
                        )
                        VokasiButton(
                            text = "Lihat Hasil Siswa",
                            onClick = { onMonitoring(major, classroom) },
                            enabled = classroom.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AdminExcelAction(
                            students, attempts, major, classroom, date,
                            modifier = Modifier.fillMaxWidth(),
                            text = "Unduh Excel Kelas"
                        )
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                AdminSectionTitle("Pratinjau", "Nilai terbaru kelas")
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(classroom.ifBlank { "Belum ada kelas" },
                            modifier = Modifier.weight(1f),
                            color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(date, color = Subtle, fontSize = 11.sp)
                    }
                    if (ordered.isEmpty()) {
                        Text("Belum ada siswa di kelas ini.", color = Subtle, fontSize = 12.sp)
                    } else {
                        ordered.take(5).forEachIndexed { i, student ->
                            if (i > 0) HorizontalDivider(color = Border)
                            val score = byStudent[student.username]
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Avatar(student, 36.dp)
                                Text(student.name, modifier = Modifier.weight(1f),
                                    color = Ink, fontSize = 12.sp,
                                    maxLines = 1)
                                Text(score?.let { "${it.score}/100" } ?: "Belum",
                                    color = if (score == null) Warning else
                                        if (score.score < 60) Bad else Navy,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(
                        onClick = { onMonitoring(major, classroom) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Lihat semua nilai", color = Navy, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                AdminSectionTitle("Struktur sekolah", "Rekap per jurusan")
                Text("Pilih jurusan untuk melihat kelasnya.",
                    color = Subtle, fontSize = 12.sp)
            }
        }
        byMajor.forEach { (majorName, classes) ->
            item(key = majorName) {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    val done = classes.sumOf { it.completedCount }
                    val total = classes.sumOf { it.studentCount }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(majorName, color = Ink, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp)
                            Text("${classes.size} kelas • $done/$total siswa sudah latihan",
                                color = Subtle, fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                expandedMajor = if (expandedMajor == majorName) null else majorName
                            }
                        ) {
                            Text(if (expandedMajor == majorName) "Tutup" else "Lihat kelas",
                                color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = Good, trackColor = PaleGreen
                    )
                    if (expandedMajor == majorName) {
                        classes.forEach { cls ->
                            HorizontalDivider(color = Border)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(cls.classroom, color = Ink, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("${cls.completedCount}/${cls.studentCount} selesai • Rata-rata ${cls.averageScore?.toString() ?: "—"}",
                                        color = Subtle, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { onMonitoring(majorName, cls.classroom) }
                                ) { Text("Nilai", color = Navy, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = onStudents,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) { Text("Buka Data Siswa", color = Navy, fontWeight = FontWeight.Bold) }
        }
        item {
            TextButton(onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth()) {
                Text("Keluar Akun", color = Subtle)
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar akun admin?") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Keluar", color = Bad)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun AdminStatChip(
    label: String, value: String, accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.background(Canvas, RoundedCornerShape(14.dp))
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
    }
}
