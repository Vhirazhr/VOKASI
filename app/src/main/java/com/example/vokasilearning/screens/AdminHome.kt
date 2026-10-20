package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onStudents: (String) -> Unit,
    onMonitoring: (String, String, String) -> Unit,
    onExcelExport: (String, String, String) -> Unit,
    onLog: () -> Unit,
    onProfile: () -> Unit,
    adminName: String,
    schoolName: String,
    onLogout: () -> Unit
) {
    val majors = students.map { it.classroom.major }.distinct().sorted()
    var major by remember { mutableStateOf(majors.firstOrNull().orEmpty()) }
    val classOptions = students.filter { it.classroom.major == major }
        .map { it.classroom.label }.distinct().sorted()
    var classroom by remember(major) { mutableStateOf(classOptions.firstOrNull().orEmpty()) }
    var date by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var confirmLogout by remember { mutableStateOf(false) }
    var statsMajor by remember { mutableStateOf("") }
    var statsGrade by remember { mutableStateOf("") }
    var statsClass by remember { mutableStateOf("") }
    var statsDate by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    val scopedClasses = students.filter { (statsMajor.isBlank() || it.classroom.major == statsMajor) &&
        (statsGrade.isBlank() || schoolGrade(it.classroom.label) == statsGrade) }
        .map { it.classroom.label }.distinct().sorted()
    val stats = remember(students, attempts, statsMajor, statsGrade, statsClass, statsDate) {
        SchoolAnalytics.calculate(students, attempts,
            SchoolScope(statsMajor, statsGrade, statsClass), statsDate)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero(
                    title = "Halo, ${adminName.take(20)}!",
                    subtitle = "Pantau hasil latihan siswa • $schoolName"
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 245.dp)
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
                        VokasiButton(
                            text = "Lihat Hasil Siswa",
                            onClick = { onMonitoring(major, classroom, date) },
                            enabled = classroom.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AdminExcelAction(
                            students, attempts, major, classroom, date,
                            modifier = Modifier.fillMaxWidth(),
                            text = "Unduh Excel Kelas",
                            onExport = onExcelExport
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionTitle("Data sekolah", "Statistik & perkembangan")
                Text("Keseluruhan sekolah ditampilkan secara otomatis. Filter hanya jika diperlukan.",
                    color = Subtle, fontSize = 12.sp)
                WhiteCard(Modifier.fillMaxWidth()) {
                    AdminSelect("Jurusan", statsMajor,
                        listOf("") + majors,
                        { statsMajor = it; statsClass = "" }, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminSelect("Tingkat", statsGrade, listOf("", "X", "XI", "XII"),
                            { statsGrade = it; statsClass = "" }, Modifier.weight(1f))
                        AdminSelect("Kelas", statsClass, listOf("") + scopedClasses,
                            { statsClass = it }, Modifier.weight(1f))
                    }
                    AdminDateSelect(statsDate, { statsDate = it }, Modifier.fillMaxWidth())
                    Text(
                        if (statsMajor.isBlank() && statsGrade.isBlank() && statsClass.isBlank())
                            "Seluruh sekolah" else
                            listOf(statsMajor, statsGrade, statsClass).filter { it.isNotBlank() }.joinToString(" • "),
                        color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("Siswa", "${stats.students}", Navy, Modifier.weight(1f))
                        AdminStatChip("Selesai", "${stats.completed}", Good, Modifier.weight(1f))
                        AdminStatChip("Belum", "${stats.missing}", Warning, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("Jurusan", "${stats.majors}", Navy, Modifier.weight(1f))
                        AdminStatChip("Kelas", "${stats.classes}", Navy, Modifier.weight(1f))
                        AdminStatChip("Rata-rata", stats.average?.toString() ?: "—", Navy, Modifier.weight(1f))
                    }
                    Text("Partisipasi latihan wajib: ${stats.participationPercent}%",
                        color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { stats.participationPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(7.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = Good, trackColor = PaleGreen
                    )
                    HorizontalDivider(color = Border)
                    Text("Distribusi nilai siswa yang selesai", color = Ink,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("75–100", "${stats.high}", Good, Modifier.weight(1f))
                        AdminStatChip("60–74", "${stats.middle}", Warning, Modifier.weight(1f))
                        AdminStatChip("0–59", "${stats.low}", Bad, Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Border)
                    Text("Perkembangan siswa", color = Ink, fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Bandingkan nilai wajib terbaru dengan pengerjaan sebelumnya.",
                        color = Subtle, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("Naik", "${stats.improved}", Good, Modifier.weight(1f))
                        AdminStatChip("Tetap", "${stats.stable}", Navy, Modifier.weight(1f))
                        AdminStatChip("Turun", "${stats.declined}", Bad, Modifier.weight(1f))
                    }
                    Text("Tren rata-rata per hari (maksimal 7 hari dengan data)",
                        color = Subtle, fontSize = 11.sp)
                    if (stats.trend.isEmpty()) {
                        Text("Belum ada riwayat untuk ditampilkan.", color = Subtle, fontSize = 12.sp)
                    } else {
                        stats.trend.forEach { (day, average) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(day, color = Subtle, fontSize = 11.sp,
                                    modifier = Modifier.width(100.dp))
                                LinearProgressIndicator(
                                    progress = { (average ?: 0) / 100f },
                                    modifier = Modifier.weight(1f).height(7.dp),
                                    color = Navy, trackColor = PaleBlue
                                )
                                Text(average?.toString() ?: "—", color = Ink, fontSize = 12.sp,
                                    modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }
                    }
                    Text("Persentase dan rata-rata hanya berdasarkan nilai wajib di tanggal terpilih.",
                        color = Subtle, fontSize = 11.sp)
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                AdminSectionTitle("Administrasi", "Pengaturan Panel")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onLog, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Log Aktivitas", fontSize = 12.sp, color = Navy) }
                    OutlinedButton(
                        onClick = onProfile, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Profil Admin", fontSize = 12.sp, color = Navy) }
                }
            }
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
