package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.AdminPagination
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.ui.*
import java.util.Calendar

/** School master data with local persistence, scoped to the admin's device. */
@Composable
internal fun AdminSchoolScreen(
    directory: SchoolDirectory,
    onUpdate: (SchoolDirectory) -> Boolean,
    onBack: () -> Unit,
    onRosterExport: (Int) -> Unit
) {
    var tab by remember { mutableStateOf("Siswa") }
    var notice by remember { mutableStateOf("") }
    var newMajor by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("X") }
    var classSuffix by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var cohort by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var filterMajor by remember { mutableStateOf("") }
    var filterClass by remember { mutableStateOf("") }
    var filterYear by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("Semua") }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var exportNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val rosterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) exportScope.launch {
            exportNotice = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        SchoolRosterExcel.write(stream, directory.roster)
                    } ?: throw IllegalArgumentException("Gagal membuka lokasi file.")
                }
                onRosterExport(directory.roster.size)
                "Data siswa berhasil disimpan ke Excel."
            } catch (_: Exception) {
                "Ekspor Excel gagal. Periksa izin penyimpanan."
            }
        }
    }

    val classOptions = directory.classes.filter { it.major == major }.map { it.label }.sorted()
    val filteredClasses = directory.classes.filter { filterMajor.isBlank() || it.major == filterMajor }
        .map { it.label }.distinct().sorted()
    val results = directory.roster.filter {
        (filterMajor.isBlank() || it.major == filterMajor) &&
            (filterClass.isBlank() || it.classLabel == filterClass) &&
            (filterYear.isBlank() || it.admissionYear.toString() == filterYear) &&
            (filterStatus == "Semua" ||
                (filterStatus == "Terdaftar") == it.registered) &&
            (query.isBlank() || it.fullName.contains(query.trim(), true) ||
                it.username.contains(query.trim(), true))
    }.sortedWith(compareBy<SchoolRosterEntry> { it.major }
        .thenBy { it.classLabel }.thenBy { it.fullName })
    val active = AdminPagination.active(page, results.size, AdminPagination.PAGE_SIZE)
    fun commit(next: SchoolDirectory, label: String) {
        notice = if (onUpdate(next)) label else "Gagal menyimpan data sekolah."
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box {
                AdminHero("Data Sekolah", "Manajemen jurusan, kelas, angkatan, dan siswa", compact = true)
                Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 198.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(18.dp)) {
                        TextButton(onClick = onBack) { Text("‹ Ringkasan", color = Navy) }
                        Text(directory.majors.size.toString() + " jurusan • " +
                            directory.classes.size + " kelas • " + directory.roster.size + " siswa",
                            color = Ink, fontWeight = FontWeight.Bold)
                        Text(directory.registeredCount.toString() + " terdaftar • " +
                            directory.pendingCount + " belum terdaftar",
                            color = Subtle, fontSize = 12.sp)
                        Text("Penyimpanan lokal. Sinkronisasi antar HP memerlukan server sekolah.",
                            color = Subtle, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Arsip Data Sekolah", color = Ink, fontWeight = FontWeight.Bold)
                Text("Unduh daftar siswa beserta jurusan, kelas, angkatan dan status pendaftaran.",
                    color = Subtle, fontSize = 11.sp)
                OutlinedButton(
                    onClick = { rosterLauncher.launch("VOKASI_Data_Sekolah.xlsx") },
                    enabled = directory.roster.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unduh Data Siswa (Excel)", color = Navy, fontSize = 12.sp)
                }
                if (exportNotice.isNotBlank()) Text(exportNotice, color = Subtle, fontSize = 11.sp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Jurusan", "Kelas", "Siswa").forEach { value ->
                    FilterChip(selected = tab == value, onClick = {
                        tab = value; page = 0; notice = ""
                    }, label = { Text(value) })
                }
            }
        }
        if (tab == "Jurusan") {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tambah jurusan", color = Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = newMajor, onValueChange = { newMajor = it },
                        label = { Text("Nama jurusan") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp))
                    VokasiButton("Simpan jurusan", enabled = newMajor.trim().length >= 3,
                        onClick = {
                            val name = newMajor.trim()
                            if (directory.majors.any { it.equals(name, true) }) notice = "Jurusan sudah ada."
                            else {
                                commit(directory.copy(majors = directory.majors + name), "Jurusan ditambahkan.")
                                newMajor = ""
                            }
                        }, modifier = Modifier.fillMaxWidth())
                }
            }
            items(directory.majors.sorted()) { value ->
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(value, color = Ink, fontWeight = FontWeight.Bold)
                    Text(directory.classes.count { it.major == value }.toString() + " kelas",
                        color = Subtle, fontSize = 11.sp)
                    if (directory.classes.none { it.major == value }) {
                        TextButton(onClick = {
                            commit(directory.copy(majors = directory.majors - value),
                                "Jurusan kosong dihapus.")
                        }) { Text("Hapus jurusan kosong", color = Bad) }
                    }
                }
            }
        } else if (tab == "Kelas") {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tambah kelas", color = Ink, fontWeight = FontWeight.Bold)
                    AdminSelect("Jurusan", major, directory.majors.sorted(), { major = it },
                        Modifier.fillMaxWidth())
                    AdminSelect("Tingkat", grade, listOf("X", "XI", "XII"), { grade = it },
                        Modifier.fillMaxWidth())
                    OutlinedTextField(value = classSuffix, onValueChange = { classSuffix = it },
                        label = { Text("Nama kelas, contoh: TPM 1") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp))
                    VokasiButton("Simpan kelas", enabled = major.isNotBlank() &&
                        classSuffix.trim().length >= 2, onClick = {
                            val label = grade + " " + classSuffix.trim()
                            if (directory.classes.any {
                                    it.major == major && it.label.equals(label, true)
                                }) notice = "Kelas sudah ada."
                            else {
                                commit(directory.copy(classes = directory.classes +
                                    Classroom(label, major)), "Kelas ditambahkan.")
                                classSuffix = ""
                            }
                        }, modifier = Modifier.fillMaxWidth())
                }
            }
            items(directory.classes.sortedWith(compareBy<Classroom> { it.major }.thenBy { it.label })) { c ->
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(c.label, color = Ink, fontWeight = FontWeight.Bold)
                    Text(c.major, color = Subtle, fontSize = 12.sp)
                    if (directory.roster.none { it.classLabel == c.label && it.major == c.major }) {
                        TextButton(onClick = {
                            commit(directory.copy(classes = directory.classes - c),
                                "Kelas kosong dihapus.")
                        }) { Text("Hapus kelas kosong", color = Bad) }
                    }
                }
            }
        } else {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Daftar nama siswa resmi", color = Ink, fontWeight = FontWeight.Bold)
                    AdminSelect("Jurusan", major, directory.majors.sorted(), {
                        major = it; selectedClass = ""
                    }, Modifier.fillMaxWidth())
                    AdminSelect("Kelas", selectedClass, classOptions, { selectedClass = it },
                        Modifier.fillMaxWidth())
                    val now = Calendar.getInstance().get(Calendar.YEAR)
                    AdminSelect("Angkatan", cohort, (now downTo now - 8).map { it.toString() },
                        { cohort = it }, Modifier.fillMaxWidth())
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it },
                        label = { Text("Nama lengkap sesuai data sekolah") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp))
                    VokasiButton("Tambah siswa", enabled = major.isNotBlank() &&
                        selectedClass in classOptions && fullName.trim().contains(" ") &&
                        cohort.toIntOrNull() != null, onClick = {
                            val name = fullName.trim().replace(Regex("\\s+"), " ")
                            if (directory.roster.any {
                                it.fullName.equals(name, true) &&
                                    it.classLabel == selectedClass && it.major == major &&
                                    it.admissionYear == cohort.toInt()
                            }) notice = "Siswa sudah tercatat di kelas dan angkatan tersebut."
                            else {
                                val id = (directory.roster.maxOfOrNull { it.id } ?: 0L) + 1L
                                commit(directory.copy(roster = directory.roster +
                                    SchoolRosterEntry(id, name, selectedClass, major, cohort.toInt())),
                                    "Siswa ditambahkan.")
                                fullName = ""
                            }
                        }, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Status pendaftaran siswa", color = Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = query, onValueChange = { query = it; page = 0 },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Cari nama / username") }, shape = RoundedCornerShape(12.dp))
                    AdminSelect("Jurusan", filterMajor, listOf("") + directory.majors.sorted(), {
                        filterMajor = it; filterClass = ""; page = 0
                    }, Modifier.fillMaxWidth())
                    AdminSelect("Kelas", filterClass, listOf("") + filteredClasses, {
                        filterClass = it; page = 0
                    }, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminSelect("Angkatan", filterYear, listOf("") +
                            directory.roster.map { it.admissionYear.toString() }.distinct().sortedDescending(),
                            { filterYear = it; page = 0 }, Modifier.weight(1f))
                        AdminSelect("Status", filterStatus,
                            listOf("Semua", "Terdaftar", "Belum Terdaftar"),
                            { filterStatus = it; page = 0 }, Modifier.weight(1f))
                    }
                }
            }
            if (results.isEmpty()) {
                item { WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Belum ada data siswa sesuai filter.", color = Subtle)
                } }
            } else {
                items(AdminPagination.slice(results, active, AdminPagination.PAGE_SIZE),
                    key = { it.id }) { e ->
                    WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Text(e.fullName, color = Ink, fontWeight = FontWeight.Bold)
                        Text(e.major + " • " + e.classLabel + " • Angkatan " + e.admissionYear,
                            color = Subtle, fontSize = 11.sp)
                        Text(if (e.registered) "● Terdaftar" else "● Belum terdaftar",
                            color = if (e.registered) Good else Bad, fontSize = 12.sp)
                        if (e.registered) {
                            Text("Username: " + e.username, color = Navy, fontSize = 11.sp)
                            Text("Password sudah dibuat (disimpan sebagai hash, tidak ditampilkan).",
                                color = Subtle, fontSize = 10.sp)
                        }
                        else TextButton(onClick = {
                            commit(directory.copy(roster = directory.roster - e),
                                "Data siswa belum terdaftar dihapus.")
                        }) { Text("Hapus data belum terdaftar", color = Bad, fontSize = 11.sp) }
                    }
                }
            }
            item {
                AdminPaginationFooter("Siswa", results.size, active,
                    onPrevious = { page = active - 1 },
                    onNext = { page = active + 1 },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
            }
        }
        if (notice.isNotBlank()) {
            item {
                Text(notice, Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    color = if (notice.contains("ditambahkan") || notice.contains("dihapus")) Good else Bad,
                    fontSize = 12.sp)
            }
        }
    }
}
