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
import com.example.vokasilearning.data.SupabaseAttemptRepository
import kotlinx.coroutines.delay
import java.util.Locale
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

/** Class/day compulsory grades; latest compulsory attempt per student. */
@Composable
internal fun AdminMonitorScreen(
    students: List<Student>,
    questions: List<Question>,
    initialMajor: String = "",
    initialClass: String = "",
    initialDate: String = "",
    onExcelExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val majors = students.map { it.classroom.major }.distinct().sorted()
    var major by remember(initialMajor) {
        mutableStateOf(initialMajor.takeIf { it in majors }.orEmpty())
    }
    val classes = students.filter { major.isBlank() || it.classroom.major == major }
        .map { it.classroom.label }.distinct().sorted()
    var classroom by remember(initialMajor, initialClass, major) {
        mutableStateOf(initialClass.takeIf { it in classes }.orEmpty())
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
    var refreshKey by remember { mutableIntStateOf(0) }
    // Query authoritative results for the selected WIB date, not cached home data.
    var fetchedForDate by remember(date) { mutableStateOf<List<Attempt>?>(null) }
    var fetching by remember(date) { mutableStateOf(true) }
    var fetchError by remember(date) { mutableStateOf("") }
    var lastUpdated by remember(date) { mutableStateOf<String?>(null) }

    LaunchedEffect(date, refreshKey) {
        while (true) {
            fetching = true
            try {
                fetchedForDate = SupabaseAttemptRepository.loadForAdminDate(date, questions)
                fetchError = ""
                lastUpdated = java.text.SimpleDateFormat("HH:mm", Locale("id", "ID")).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                }.format(java.util.Date())
            } catch (error: Exception) {
                fetchError = error.message ?: "Koneksi Supabase gagal."
            } finally {
                fetching = false
            }
            delay(20_000L)
        }
    }
    val serverReady = fetchedForDate != null
    val displayedAttempts = fetchedForDate.orEmpty()
    val latest = AdminAnalytics.latestCompulsoryByStudent(displayedAttempts, date)
        .mapKeys { (username, _) -> username.trim().lowercase(Locale.ROOT) }
    fun attemptFor(username: String): Attempt? =
        latest[username.trim().lowercase(Locale.ROOT)]
    val scopeMembers = students.filter {
        (major.isBlank() || it.classroom.major == major) &&
            (classroom.isBlank() || it.classroom.label == classroom)
    }
    val scopeIds = scopeMembers.mapTo(HashSet()) { it.username.trim().lowercase(Locale.ROOT) }
    val scores = scopeMembers.mapNotNull { attemptFor(it.username)?.score }
    val summary = ClassDailySummary(
        classroom = classroom,
        studentCount = scopeMembers.size,
        completedCount = scores.size,
        averageScore = scores.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        sessions = AdminAnalytics.compulsoryOnDate(displayedAttempts, date)
            .count { it.studentId.trim().lowercase(Locale.ROOT) in scopeIds },
        major = major
    )
    val members = (if (serverReady) scopeMembers else emptyList()).filter {
        search.isBlank() || it.name.contains(search.trim(), true) ||
            it.username.contains(search.trim(), true)
    }.filter { !onlyMissing || attemptFor(it.username) == null }
        .sortedWith(
            compareBy<Student> { if (attemptFor(it.username) == null) 1 else 0 }.thenBy { it.name }
        )
    val pageSize = AdminPagination.PAGE_SIZE
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
                AdminHero("Nilai Siswa", "Rekap latihan wajib menurut jurusan, kelas, dan tanggal", compact = true)
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
                            Text("HASIL LATIHAN WAJIB", color = NavyLight, fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Pill(
                                when {
                                    !serverReady && fetching -> "Memuat..."
                                    fetchError.isNotBlank() -> "Gangguan"
                                    else -> "Supabase"
                                },
                                if (serverReady && fetchError.isBlank()) PaleGreen else PaleGold,
                                if (serverReady && fetchError.isBlank()) Good else Warning
                            )
                        }
                        AdminSelect("Jurusan", major, listOf("") + majors, { major = it },
                            Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Kelas", classroom, listOf("") + classes, { classroom = it },
                                Modifier.weight(1f))
                            AdminDateSelect(date, { date = it }, Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when {
                                    fetching && !serverReady -> "Mengambil nilai tanggal $date..."
                                    fetchError.isNotBlank() -> "Data terbaru belum berhasil dimuat."
                                    serverReady -> "Sinkron WIB: ${lastUpdated ?: "—"} • $date"
                                    else -> "Menunggu Supabase"
                                },
                                modifier = Modifier.weight(1f), fontSize = 11.sp,
                                color = if (fetchError.isNotBlank()) Warning else Subtle
                            )
                            TextButton(
                                onClick = { refreshKey++ },
                                enabled = !fetching,
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) { Text("Segarkan", fontSize = 11.sp, color = Navy) }
                        }
                        if (fetchError.isNotBlank()) {
                            Text("Sinkronisasi gagal: $fetchError",
                                color = Warning, fontSize = 11.sp)
                        }
                        HorizontalDivider(color = Border)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSummaryMetric("Selesai", if (serverReady) "${summary.completedCount}" else "—", Good, Modifier.weight(1f))
                            AdminSummaryMetric("Belum", if (serverReady) "${summary.missingCount}" else "—", Warning, Modifier.weight(1f))
                            AdminSummaryMetric("Rata-rata", if (serverReady) summary.averageScore?.toString() ?: "—" else "—",
                                Navy, Modifier.weight(1f))
                        }
                        if (serverReady && major.isNotBlank() && classroom.isNotBlank()) {
                            AdminExcelAction(
                                students, displayedAttempts, major, classroom, date,
                                modifier = Modifier.fillMaxWidth(), text = "Unduh Excel Kelas",
                                onExport = onExcelExport
                            )
                        } else {
                            Text("Ekspor semua kelas tersedia di Ringkasan.",
                                color = Subtle, fontSize = 11.sp)
                        }
                        Text(if (serverReady && fetchError.isBlank())
                                "Nilai server terverifikasi • ${summary.studentCount} siswa"
                            else "Belum bisa memastikan nilai terbaru • ${summary.studentCount} siswa",
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
                        label = { Text(if (serverReady) "Belum tercatat" else "Belum tersinkron", fontSize = 12.sp) }
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${members.size} siswa", color = Subtle, fontSize = 11.sp)
                }
            }
        }
        if (members.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(if (!serverReady) "Belum berhasil mengambil nilai dari Supabase."
                        else "Tidak ada siswa yang cocok dengan filter.",
                        color = Subtle, fontSize = 12.sp)
                }
            }
        } else {
            items(visibleMembers, key = { it.username }) { student ->
                val result = attemptFor(student.username)
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
                            if (!serverReady) "Memuat" else result?.let { "${it.score}/100" } ?: "Belum",
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
                AdminPaginationFooter(
                    label = "Siswa",
                    total = members.size,
                    page = activePage,
                    onPrevious = { page = activePage - 1; pagingActivated = true },
                    onNext = { page = activePage + 1; pagingActivated = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )
            }
        }
    }
    reviewing?.let { result ->
        val owner = students.firstOrNull { it.username.equals(result.studentId, ignoreCase = true) }
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
