package com.example.vokasilearning.screens

import androidx.activity.compose.BackHandler
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

/**
 * A single path through the school hierarchy: Jurusan -> Kelas -> Siswa -> Detail.
 * Only one level is rendered at once. Searching works from every level.
 */
@Composable
internal fun AdminStudentsScreen(
    students: List<Student>,
    attempts: List<Attempt>,
    initialMajor: String = ""
) {
    var major by remember(initialMajor) { mutableStateOf(initialMajor.takeIf { requested ->
        students.any { it.classroom.major == requested }
    }) }
    var grade by remember(initialMajor) { mutableStateOf<String?>(null) }
    var classroom by remember(initialMajor) { mutableStateOf<String?>(null) }
    var selectedStudent by remember(initialMajor) { mutableStateOf<Student?>(null) }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var pagingActivated by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf<Attempt?>(null) }
    val lazyState = rememberLazyListState()
    val latestCompulsory = remember(attempts) {
        attempts.asSequence().filter { it.practiceCategory == null }
            .groupBy { it.studentId }
            .mapValues { (_, list) -> list.maxByOrNull { it.timeMillis }!! }
    }
    val majors = students.groupBy { it.classroom.major }.toSortedMap()
    val classes = students.filter { it.classroom.major == major && (grade == null || schoolGrade(it.classroom.label) == grade) }
        .groupBy { it.classroom.label }.toSortedMap()
    val matches = if (query.isBlank()) emptyList() else students.filter {
        it.name.contains(query.trim(), ignoreCase = true) ||
            it.username.contains(query.trim(), ignoreCase = true) ||
            it.classroom.label.contains(query.trim(), ignoreCase = true) ||
            it.classroom.major.contains(query.trim(), ignoreCase = true)
    }.sortedWith(compareBy<Student> { it.classroom.major }.thenBy { it.classroom.label }.thenBy { it.name })
    val classmates = students.filter {
        it.classroom.major == major && it.classroom.label == classroom
    }.sortedBy { it.name }
    val level = when {
        query.isNotBlank() -> "Pencarian"
        selectedStudent != null -> "Detail Siswa"
        classroom != null -> "Pilih Siswa"
        grade != null -> "Pilih Kelas"
        major != null -> "Pilih Tingkat"
        else -> "Pilih Jurusan"
    }
    fun back() {
        when {
            selectedStudent != null -> { selectedStudent = null }
            query.isNotBlank() -> query = ""
            classroom != null -> classroom = null
            grade != null -> grade = null
            major != null -> major = null
        }
        page = 0
    }
    fun selectStudent(student: Student) {
        major = student.classroom.major
        grade = schoolGrade(student.classroom.label)
        classroom = student.classroom.label
        selectedStudent = student
        query = ""
        page = 0
    }
    BackHandler(enabled = selectedStudent != null || classroom != null || grade != null ||
        major != null || query.isNotBlank()) { back() }
    val scopeResetKey = listOf(major.orEmpty(), grade.orEmpty(), classroom.orEmpty(), selectedStudent?.username.orEmpty(), query)
    LaunchedEffect(scopeResetKey) { pagingActivated = false; lazyState.scrollToItem(0) }
    LaunchedEffect(page, pagingActivated) {
        if (pagingActivated) lazyState.scrollToItem(2)
    }

    val history = selectedStudent?.let { student ->
        attempts.filter { it.studentId == student.username && it.practiceCategory == null }
            .sortedWith(compareByDescending<Attempt> { it.timeMillis }.thenByDescending { it.sessionKey })
    }.orEmpty()
    val listingSize = when {
        selectedStudent != null && query.isBlank() -> history.size
        query.isNotBlank() -> matches.size
        classroom != null -> classmates.size
        grade != null -> classes.size
        major != null -> listOf("X", "XI", "XII").count { level -> students.any { it.classroom.major == major && schoolGrade(it.classroom.label) == level } }
        else -> majors.size
    }
    val pageSize = AdminPagination.PAGE_SIZE
    val pageCount = AdminPagination.count(listingSize, pageSize)
    val activePage = AdminPagination.active(page, listingSize, pageSize)
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        state = lazyState,
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Data Siswa", "Jurusan • Tingkat • Kelas • Siswa", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                        .padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("CARI SISWA", color = NavyLight, fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it; page = 0 },
                            placeholder = { Text("Nama, username, kelas, jurusan") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(13.dp)
                        )
                        AdminSelect(
                            label = "Filter jurusan",
                            value = major.orEmpty(),
                            options = listOf("") + majors.keys.toList(),
                            onChange = { selected ->
                                major = selected.ifBlank { null }
                                grade = null
                                classroom = null
                                selectedStudent = null
                                query = ""
                                page = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${students.size} siswa • ${majors.size} jurusan • Data Supabase",
                            color = Subtle, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    AdminSectionTitle("Direktori sekolah", level)
                    Text(
                        when {
                            query.isNotBlank() -> "${matches.size} siswa ditemukan"
                            selectedStudent != null -> "${selectedStudent!!.classroom.major} / ${selectedStudent!!.classroom.label}"
                            classroom != null -> "${major.orEmpty()} / ${grade.orEmpty()} / ${classroom.orEmpty()}"
                            grade != null -> "${major.orEmpty()} / ${grade.orEmpty()}"
                            major != null -> major.orEmpty()
                            else -> "Pilih jurusan untuk melanjutkan"
                        },
                        color = Subtle, fontSize = 11.sp, maxLines = 2
                    )
                }
                if (major != null || grade != null || classroom != null || selectedStudent != null || query.isNotBlank()) {
                    TextButton(onClick = { back() }) {
                        Text("‹ Kembali", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (query.isNotBlank()) {
            val visible = matches.drop(activePage * pageSize).take(pageSize)
            if (visible.isEmpty()) {
                item {
                    WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Text("Tidak ada siswa yang cocok.", color = Subtle)
                    }
                }
            } else {
                items(visible, key = { "search_${it.username}" }) { student ->
                    AdminDirectoryStudentRow(
                        student = student, grade = latestCompulsory[student.username],
                        onClick = { selectStudent(student) }
                    )
                }
            }
        } else if (selectedStudent != null) {
            val student = selectedStudent!!
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(student, 52.dp)
                        Column(Modifier.weight(1f)) {
                            Text(student.name, color = Ink, fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold, maxLines = 2)
                            Text(student.classroom.major, color = Subtle, fontSize = 11.sp)
                            Text(student.classroom.label + " • @" + student.username,
                                color = Subtle, fontSize = 11.sp)
                            if (student.admissionYear > 0) {
                                Text("Angkatan ${student.admissionYear}",
                                    color = Subtle, fontSize = 11.sp)
                            }
                        }
                    }
                    val latest = latestCompulsory[student.username]
                    HorizontalDivider(color = Border)
                    Text("Nilai wajib terakhir", color = Subtle, fontSize = 11.sp)
                    Text(latest?.let { "${it.score}/100" } ?: "Belum ada hasil",
                        color = when {
                            latest == null -> Subtle
                            latest.score < 60 -> Bad
                            else -> Navy
                        }, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${history.size} sesi wajib tersimpan", color = Subtle, fontSize = 11.sp)
                }
            }
            if (history.isEmpty()) {
                item {
                    WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Text("Belum ada riwayat latihan wajib.", color = Subtle)
                    }
                }
            } else {
                items(history.drop(activePage * pageSize).take(pageSize),
                    key = { it.sessionKey.ifBlank { it.timeMillis.toString() } }) { attempt ->
                    WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(dateFormat.format(Date(attempt.timeMillis)) + " WIB",
                                    color = Subtle, fontSize = 11.sp)
                                Text("${attempt.score}/100 • ${attempt.correct}/${attempt.questionCount} benar",
                                    color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { reviewing = attempt }) {
                                Text("Jawaban", color = Navy, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else if (classroom != null) {
            if (classmates.isEmpty()) {
                item { WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Belum ada siswa di kelas ini.", color = Subtle)
                } }
            } else {
                items(classmates.drop(activePage * pageSize).take(pageSize), key = { it.username }) { student ->
                    AdminDirectoryStudentRow(
                        student = student, grade = latestCompulsory[student.username],
                        onClick = { selectStudent(student) }
                    )
                }
            }
        } else if (grade != null) {
            val visibleClasses = classes.entries.drop(activePage * pageSize).take(pageSize)
            items(visibleClasses, key = { it.key }) { entry ->
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.key, color = Ink, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                            Text("${entry.value.size} siswa",
                                color = Subtle, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { classroom = entry.key; page = 0 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy)
                        ) { Text("Pilih Kelas", fontSize = 12.sp) }
                    }
                }
            }
        } else if (major != null) {
            val grades = listOf("X", "XI", "XII").filter { level ->
                students.any { it.classroom.major == major && schoolGrade(it.classroom.label) == level }
            }
            items(grades, key = { it }) { level ->
                val matching = students.filter {
                    it.classroom.major == major && schoolGrade(it.classroom.label) == level
                }
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tingkat $level", color = Ink, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                            Text("${matching.map { it.classroom.label }.distinct().size} kelas • ${matching.size} siswa",
                                color = Subtle, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { grade = level; classroom = null; page = 0 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy)
                        ) { Text("Lihat Kelas", fontSize = 12.sp) }
                    }
                }
            }
        } else {
            val visibleMajors = majors.entries.drop(activePage * pageSize).take(pageSize)
            items(visibleMajors, key = { it.key }) { entry ->
                val numClasses = entry.value.map { it.classroom.label }.distinct().size
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.key, color = Ink, fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold, maxLines = 2)
                            Text("$numClasses kelas • ${entry.value.size} siswa",
                                color = Subtle, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { major = entry.key; grade = null; page = 0 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy)
                        ) { Text("Pilih Tingkat", fontSize = 12.sp) }
                    }
                }
            }
        }

        item {
            AdminPaginationFooter(
                label = when {
                    selectedStudent != null && query.isBlank() -> "Riwayat"
                    query.isNotBlank() || classroom != null -> "Siswa"
                    grade != null -> "Kelas"
                    major != null -> "Tingkat"
                    else -> "Jurusan"
                },
                total = listingSize,
                page = activePage,
                onPrevious = { page = activePage - 1; pagingActivated = true },
                onNext = { page = activePage + 1; pagingActivated = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
    }

    reviewing?.let { attempt ->
        val owner = students.firstOrNull { it.username == attempt.studentId }
        AdminAnswerReviewDialog(
            attempt = attempt,
            studentName = owner?.name ?: attempt.studentNameSnapshot,
            classroom = owner?.classroom?.label ?: attempt.classSnapshot,
            onClose = { reviewing = null }
        )
    }
}

@Composable
private fun AdminDirectoryStudentRow(
    student: Student,
    grade: Attempt?,
    onClick: () -> Unit
) {
    WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(student, 43.dp)
            Column(Modifier.weight(1f)) {
                Text(student.name, color = Ink, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${student.classroom.major} • ${student.classroom.label}",
                    color = Subtle, fontSize = 10.sp, maxLines = 2)
                Text(grade?.let { "Nilai wajib: ${it.score}/100" } ?: "Belum ada nilai wajib",
                    color = grade?.let { if (it.score < 60) Bad else Navy } ?: Subtle,
                    fontSize = 11.sp)
            }
            TextButton(onClick = onClick) {
                Text("Detail", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
            }
        }
    }
}
