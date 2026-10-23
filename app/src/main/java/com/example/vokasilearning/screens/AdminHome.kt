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
import java.util.Calendar
import java.util.Locale
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

/** School-wide admin overview; grade export defaults to all majors/classes. */
@Composable
internal fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    roster: List<SchoolRosterEntry>,
    scoresReady: Boolean = true,
    scoresNotice: String = "",
    onStudents: (String) -> Unit,
    onMonitoring: (String, String, String) -> Unit,
    onExcelExport: (String, String, String) -> Unit,
    adminName: String,
    schoolName: String
) {
    val majors = (students.map { it.classroom.major } + roster.map { it.major })
        .distinct().sorted()
    var major by remember { mutableStateOf("") }
    val classOptions = (students.filter { major.isBlank() || it.classroom.major == major }
        .map { it.classroom.label } + roster.filter { major.isBlank() || it.major == major }
        .map { it.classLabel }).distinct().sorted()
    var classroom by remember(major) { mutableStateOf("") }
    var date by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var statsMajor by remember { mutableStateOf("") }
    var statsGrade by remember { mutableStateOf("") }
    var statsClass by remember { mutableStateOf("") }
    val currentYear = wibCalendar().get(Calendar.YEAR)
    val currentMonth = wibCalendar().get(Calendar.MONTH) + 1
    var weeklyStats by remember { mutableStateOf(true) }
    var statsWeekAnchor by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }
    var statsMonth by remember { mutableIntStateOf(currentMonth) }
    var statsYear by remember { mutableIntStateOf(currentYear) }
    var showScopeFilters by remember { mutableStateOf(false) }
    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val years = (attempts.mapNotNull { dayKey(it.timeMillis).take(4).toIntOrNull() } +
        (currentYear - 4..currentYear).toList()).distinct()
        .filter { it in 2000..currentYear }.sortedDescending().map { it.toString() }
    val scopedClasses = students.filter { statsMajor.isNotBlank() &&
        it.classroom.major == statsMajor &&
        (statsGrade.isBlank() || schoolGrade(it.classroom.label) == statsGrade) }
        .map { it.classroom.label }.distinct().sorted()
    val stats = remember(students, attempts, statsMajor, statsGrade, statsClass,
        statsMonth, statsYear, statsWeekAnchor, weeklyStats) {
        val scope = SchoolScope(statsMajor, statsGrade, statsClass)
        if (weeklyStats) SchoolAnalytics.calculateWeekly(students, attempts, scope, statsWeekAnchor)
        else SchoolAnalytics.calculateMonthly(students, attempts, scope, statsYear, statsMonth)
    }
    // No shared student results across both months means growth is unavailable,
    // not that every pupil received a zero or that the calculation failed.
    val comparableStudents = stats.improved + stats.stable + stats.declined
    val previousMonthTitle = monthNames[if (statsMonth == 1) 11 else statsMonth - 2]
    val weekRange = SchoolAnalytics.weekRange(statsWeekAnchor)
    val selectedPeriod = if (weeklyStats) "pekan ${weekRange.first} – ${weekRange.second}"
        else "${monthNames[statsMonth - 1]} ${statsYear}"
    val previousPeriod = if (weeklyStats) "pekan sebelumnya" else previousMonthTitle


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
                        .padding(top = 225.dp)
                        .shadow(14.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(17.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HASIL LATIHAN WAJIB",
                                modifier = Modifier.weight(1f), color = NavyLight,
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Pill(if (scoresReady) "Supabase" else "Menunggu data",
                                if (scoresReady) PaleGreen else PaleGold,
                                if (scoresReady) Good else Warning)
                        }
                        Text("Unduh Hasil Siswa", color = Ink, fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Pilih tanggal dan filter untuk laporan nilai wajib.",
                            color = Subtle, fontSize = 11.sp)
                        AdminSelect("Jurusan", major, listOf("") + majors, { major = it },
                            Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Kelas", classroom, listOf("") + classOptions, { classroom = it },
                                Modifier.weight(1f))
                            AdminDateSelect(date, { date = it }, Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal cepat:", color = Subtle, fontSize = 11.sp)
                            TextButton(
                                onClick = { date = dayKey(System.currentTimeMillis()) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                            ) { Text("Hari ini", fontSize = 11.sp) }
                            TextButton(
                                onClick = {
                                    val yesterday = wibCalendar().apply {
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    date = dayKey(yesterday.timeInMillis)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                            ) { Text("Kemarin", fontSize = 11.sp) }
                        }
                        VokasiButton(
                            text = "Lihat Hasil Siswa",
                            onClick = { onMonitoring(major, classroom, date) },
                            enabled = students.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AdminBulkExcelAction(
                            roster = roster, students = students, attempts = attempts,
                            major = major, classroom = classroom, date = date,
                            modifier = Modifier.fillMaxWidth(), onExport = onExcelExport,
                            scoresReady = scoresReady
                        )
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminSectionTitle("Data sekolah", "Statistik & perkembangan")
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = weeklyStats,
                            onClick = { weeklyStats = true },
                            label = { Text("Mingguan") }
                        )
                        FilterChip(
                            selected = !weeklyStats,
                            onClick = { weeklyStats = false },
                            label = { Text("Bulanan") }
                        )
                    }
                    if (weeklyStats) {
                        AdminDateSelect(statsWeekAnchor, { statsWeekAnchor = it }, Modifier.fillMaxWidth())
                        Text("Pekan Senin–Minggu: ${weekRange.first} s.d. ${weekRange.second}",
                            color = NavyLight, fontSize = 11.sp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect(
                                "Bulan", monthNames[statsMonth - 1], monthNames,
                                { statsMonth = monthNames.indexOf(it) + 1 },
                                Modifier.weight(1f)
                            )
                            AdminSelect("Tahun", statsYear.toString(), years,
                                { statsYear = it.toInt() }, Modifier.weight(1f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (statsMajor.isBlank() && statsGrade.isBlank() && statsClass.isBlank())
                                    "Seluruh sekolah"
                                else listOf(statsMajor, statsGrade, statsClass)
                                    .filter { it.isNotBlank() }.joinToString(" • "),
                                color = Navy, fontWeight = FontWeight.Bold,
                                fontSize = 12.sp, maxLines = 2
                            )
                            Text(if (weeklyStats) "Rekap latihan wajib mingguan" else "Rekap latihan wajib bulanan",
                                color = Subtle, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showScopeFilters = !showScopeFilters },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (showScopeFilters) "Tutup filter" else "Filter jurusan",
                                color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(if (showScopeFilters) "⌃" else "⌄", color = Navy, fontSize = 14.sp)
                        }
                    }
                    if (showScopeFilters) {
                        AdminSelect("Jurusan", statsMajor, listOf("") + majors,
                            { statsMajor = it; statsClass = "" }, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Tingkat", statsGrade, listOf("", "X", "XI", "XII"),
                                { statsGrade = it; statsClass = "" }, Modifier.weight(1f))
                            AdminSelect("Kelas", statsClass, listOf("") + scopedClasses,
                                { statsClass = it }, Modifier.weight(1f))
                        }
                        if (statsMajor.isBlank()) {
                            Text("Pilih jurusan sebelum memfilter kelas.",
                                color = Subtle, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            statsMajor = ""; statsGrade = ""; statsClass = ""
                            showScopeFilters = false
                        }) {
                            Text("Tampilkan semua", color = Navy, fontSize = 12.sp)
                        }
                    }
                    if (scoresNotice.isNotBlank()) Text(scoresNotice,
                        color = Warning, fontSize = 11.sp)
                    HorizontalDivider(color = Border)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Siswa", color = Subtle, fontSize = 11.sp)
                            Text("${stats.students}", color = Ink, fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sudah latihan", color = Subtle, fontSize = 11.sp)
                            Text(if (scoresReady) "${stats.completed}" else "—",
                                color = Good, fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rata-rata", color = Subtle, fontSize = 11.sp)
                            Text(if (scoresReady) stats.average?.toString() ?: "—" else "—",
                                color = Navy, fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(if (scoresReady)
                        "${stats.majors} jurusan • ${stats.classes} kelas • ${stats.missing} belum tercatat di server"
                        else "${stats.majors} jurusan • ${stats.classes} kelas • Status latihan menunggu sinkronisasi",
                        color = Subtle, fontSize = 11.sp)
                }

                SchoolScoreDonut(stats, dataAvailable = scoresReady, weekly = weeklyStats)
                SchoolMonthlyTrend(stats, dataAvailable = scoresReady, weekly = weeklyStats)

                WhiteCard(Modifier.fillMaxWidth()) {
                    Text("Perubahan nilai siswa", color = Ink, fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text(if (!scoresReady) "Menunggu data dari Supabase."
                        else if (weeklyStats) "Pekan terpilih dibanding pekan sebelumnya."
                        else "Bulan terpilih dibanding bulan sebelumnya.",
                        color = Subtle, fontSize = 11.sp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (scoresReady && comparableStudents > 0) "${stats.improved}" else "—", color = Good,
                                fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Naik", color = Subtle, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (scoresReady && comparableStudents > 0) "${stats.stable}" else "—", color = Navy,
                                fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Tetap", color = Subtle, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (scoresReady && comparableStudents > 0) "${stats.declined}" else "—", color = Bad,
                                fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Turun", color = Subtle, fontSize = 12.sp)
                        }
                    }
                    Text(
                        if (!scoresReady) "Rata-rata menunggu sinkronisasi"
                        else if (comparableStudents == 0)
                            if (weeklyStats) "Belum ada siswa dengan nilai wajib di kedua pekan."
                            else "Belum ada siswa dengan nilai wajib di kedua bulan."
                        else "${comparableStudents} siswa punya nilai pada kedua periode. " +
                            "Rata-rata sebelumnya: ${stats.previousAverage ?: "—"}",
                        color = Subtle, fontSize = 11.sp
                    )
                }
            }
        }
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
