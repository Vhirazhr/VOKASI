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
    var classPage by remember { mutableIntStateOf(0) }
    var classFilter by remember { mutableStateOf("") }
    var promotion by remember { mutableStateOf<SchoolRosterEntry?>(null) }
    var promotionTarget by remember { mutableStateOf("") }
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
    var advancedStudentFilters by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var exportNotice by remember { mutableStateOf("") }
    var importBusy by remember { mutableStateOf(false) }
    var importNotice by remember { mutableStateOf("") }
    var schoolImportBusy by remember { mutableStateOf(false) }
    var schoolImportNotice by remember { mutableStateOf("") }
    var schoolPreview by remember { mutableStateOf<SchoolDataExcel.Preview?>(null) }
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

    val schoolTemplateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) exportScope.launch {
            schoolImportNotice = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        SchoolDataExcel.template(it)
                    } ?: throw IllegalArgumentException("Lokasi tidak dapat dibuka.")
                }
                "Template Data Sekolah berhasil disimpan."
            } catch (_: Exception) { "Gagal menyimpan template sekolah." }
        }
    }
    val schoolImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !schoolImportBusy) exportScope.launch {
            schoolImportBusy = true
            schoolImportNotice = ""
            try {
                schoolPreview = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        SchoolDataExcel.read(it, directory)
                    } ?: throw IllegalArgumentException("File tidak dapat dibuka.")
                }
            } catch (e: IllegalArgumentException) {
                schoolImportNotice = e.message ?: "Data sekolah tidak valid."
            } catch (_: Exception) {
                schoolImportNotice = "Gagal membuka Excel. Gunakan Template Data Sekolah."
            } finally { schoolImportBusy = false }
        }
    }

    val classTemplateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) exportScope.launch {
            importNotice = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { SchoolClassExcel.template(it) }
                        ?: throw IllegalArgumentException("Gagal membuat template.")
                }
                "Template kelas Excel berhasil disimpan."
            } catch (_: Exception) { "Gagal menyimpan template Excel." }
        }
    }
    val classImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !importBusy) exportScope.launch {
            importBusy = true
            importNotice = try {
                val added = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        SchoolClassExcel.read(it, directory)
                    } ?: throw IllegalArgumentException("File tidak dapat dibuka.")
                }
                if (onUpdate(directory.copy(classes = directory.classes + added))) {
                    classPage = 0
                    "Berhasil menambahkan " + added.size + " kelas."
                } else "Gagal menyimpan data kelas."
            } catch (e: IllegalArgumentException) {
                e.message ?: "Excel tidak valid."
            } catch (_: Exception) {
                "Gagal impor kelas. Cek file Excel .xlsx."
            } finally { importBusy = false }
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
            AdminHero("Data Sekolah", "Jurusan, kelas, angkatan, dan siswa", compact = true)
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Impor Data Sekolah", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("Tambah data baru atau perbarui kelas siswa yang naik tingkat lewat satu file Excel.",
                    color = Subtle, fontSize = 12.sp)
                OutlinedButton(
                    onClick = { schoolTemplateLauncher.launch("Template_Data_Sekolah_VOKASI.xlsx") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Unduh Template Data Sekolah", color = Navy, fontSize = 12.sp) }
                VokasiButton(
                    text = if (schoolImportBusy) "Memeriksa Excel..." else "Impor Data Sekolah (Excel)",
                    enabled = !schoolImportBusy,
                    onClick = {
                        schoolImportLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Kolom A–E: Jurusan, Tingkat, Kelas, Angkatan, Nama Lengkap. " +
                    "Preview sebelum simpan. Akun, username, password dan angkatan tetap dijaga.",
                    color = Subtle, fontSize = 11.sp)
                if (schoolImportNotice.isNotBlank()) {
                    Text(schoolImportNotice, color = if (schoolImportNotice.startsWith("Berhasil") ||
                            schoolImportNotice.startsWith("Template")) Good else Bad,
                        fontSize = 12.sp)
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
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Kelola Data Sekolah", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("Pilih bagian yang ingin dikelola.", color = Subtle, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Jurusan", "Kelas", "Siswa").forEach { value ->
                        FilterChip(selected = tab == value, onClick = {
                            tab = value; page = 0; classPage = 0; notice = ""
                        }, label = { Text(value, fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                    }
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
                    Text("Impor Kelas Excel", color = Ink, fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp)
                    Text("Tambahkan banyak kelas sekaligus. Pastikan jurusannya dibuat lebih dulu.",
                        color = Subtle, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { classTemplateLauncher.launch("Template_Kelas_VOKASI.xlsx") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("Unduh Template Kelas", color = Navy) }
                    VokasiButton(
                        text = if (importBusy) "Memproses..." else "Impor Kelas dari Excel",
                        enabled = !importBusy,
                        onClick = { classImportLauncher.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        ) }, modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kolom: Jurusan • Tingkat (X/XI/XII) • Kelas. Duplikat dan data salah ditolak.",
                        color = Subtle, fontSize = 11.sp)
                    if (importNotice.isNotBlank()) Text(importNotice, color = Navy,
                        fontSize = 12.sp)
                }
            }
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
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Daftar Kelas", color = Ink, fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold)
                    AdminSelect("Filter jurusan", classFilter,
                        listOf("") + directory.majors.sorted(),
                        { classFilter = it; classPage = 0 }, Modifier.fillMaxWidth())
                }
            }
            val classResults = directory.classes.filter {
                classFilter.isBlank() || it.major == classFilter
            }.sortedWith(compareBy<Classroom> { it.major }.thenBy { it.label })
            val currentClassPage = AdminPagination.active(classPage, classResults.size, AdminPagination.PAGE_SIZE)
            items(AdminPagination.slice(classResults, currentClassPage, AdminPagination.PAGE_SIZE)) { c ->
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
            item {
                AdminPaginationFooter("Kelas", classResults.size, currentClassPage,
                    onPrevious = { classPage = currentClassPage - 1 },
                    onNext = { classPage = currentClassPage + 1 },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
            }
        } else {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daftar Siswa", color = Ink, fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp)
                            Text(results.size.toString() + " dari " + directory.roster.size +
                                " siswa • Maks. " + AdminPagination.PAGE_SIZE + " per halaman",
                                color = Subtle, fontSize = 11.sp)
                        }
                        TextButton(onClick = { advancedStudentFilters = !advancedStudentFilters }) {
                            Text(if (advancedStudentFilters) "Tutup Filter" else "Filter",
                                color = Navy, fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(value = query, onValueChange = { query = it; page = 0 },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Cari nama atau username siswa") },
                        shape = RoundedCornerShape(12.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Semua", "Terdaftar", "Belum Terdaftar").forEach { status ->
                            FilterChip(selected = filterStatus == status,
                                onClick = { filterStatus = status; page = 0 },
                                label = { Text(status, fontSize = 12.sp) })
                        }
                    }
                    if (advancedStudentFilters) {
                        AdminSelect("Jurusan", filterMajor, listOf("") + directory.majors.sorted(), {
                            filterMajor = it; filterClass = ""; page = 0
                        }, Modifier.fillMaxWidth())
                        AdminSelect("Kelas", filterClass, listOf("") + filteredClasses, {
                            filterClass = it; page = 0
                        }, Modifier.fillMaxWidth())
                        AdminSelect("Angkatan", filterYear, listOf("") +
                            directory.roster.map { it.admissionYear.toString() }.distinct().sortedDescending(),
                            { filterYear = it; page = 0 }, Modifier.fillMaxWidth())
                        TextButton(onClick = {
                            filterMajor = ""; filterClass = ""; filterYear = "";
                            filterStatus = "Semua"; query = ""; page = 0
                        }) { Text("Reset semua filter", color = Navy, fontSize = 12.sp) }
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
                            Text("Password tidak dapat dilihat admin.",
                                color = Subtle, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { promotion = e; promotionTarget = "" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pindah / Naik Kelas", color = Navy, fontSize = 12.sp) }
                        if (!e.registered) TextButton(onClick = {
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
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tambah Siswa", color = Ink, fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it },
                        label = { Text("Nama lengkap sesuai data sekolah") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp))
                    AdminSelect("Jurusan", major, directory.majors.sorted(), {
                        major = it; selectedClass = ""
                    }, Modifier.fillMaxWidth())
                    AdminSelect("Kelas", selectedClass, classOptions, { selectedClass = it },
                        Modifier.fillMaxWidth())
                    val now = Calendar.getInstance().get(Calendar.YEAR)
                    AdminSelect("Angkatan", cohort, (now downTo now - 8).map { it.toString() },
                        { cohort = it }, Modifier.fillMaxWidth())
                    VokasiButton("Tambah siswa", enabled = major.isNotBlank() &&
                        selectedClass in classOptions && fullName.trim().contains(" ") &&
                        cohort.toIntOrNull() != null, onClick = {
                            val name = fullName.trim().replace(Regex("\\s+"), " ")
                            if (directory.roster.any {
                                it.fullName.trim().equals(name, true) && it.major == major &&
                                    it.admissionYear == cohort.toInt()
                            }) notice = "Nama dan angkatan sudah tercatat di jurusan ini. " +
                                "Gunakan Pindah / Naik Kelas jika perlu."
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
        }
        if (notice.isNotBlank()) {
            item {
                Text(notice, Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    color = if (notice.contains("ditambahkan") || notice.contains("dihapus")) Good else Bad,
                    fontSize = 12.sp)
            }
        }
    }
    schoolPreview?.let { preview ->
        val changes = preview.newMajors + preview.newClasses +
            preview.newStudents + preview.movedStudents
        AlertDialog(
            onDismissRequest = { schoolPreview = null },
            title = { Text("Konfirmasi Impor Sekolah", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ringkasan perubahan pada data sekolah:", color = Ink, fontSize = 13.sp)
                    Text(preview.newMajors.toString() + " jurusan", color = Navy)
                    Text(preview.newClasses.toString() + " kelas", color = Navy)
                    Text(preview.newStudents.toString() + " siswa baru (Belum Terdaftar)", color = Navy)
                    Text(preview.movedStudents.toString() +
                        " siswa lama pindah/naik kelas (akun tetap)", color = Navy)
                    Text(preview.skipped.toString() + " data siswa duplikat dilewati",
                        color = Subtle, fontSize = 12.sp)
                    Text("Akun, username, dan password yang sudah ada tetap aman. " +
                        "Tidak ada data yang berubah sebelum menekan Simpan.",
                        color = Subtle, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    schoolImportNotice = when {
                        directory != preview.original ->
                            "Data sekolah berubah saat pratinjau. Buka Excel kembali."
                        changes == 0 -> "Tidak ada data baru. Semua siswa sudah tercatat."
                        onUpdate(preview.next) -> {
                            page = 0
                            classPage = 0
                            "Berhasil impor: " + preview.newMajors + " jurusan, " +
                                preview.newClasses + " kelas, " +
                                preview.newStudents + " siswa baru, " +
                                preview.movedStudents + " siswa pindah kelas."
                        }
                        else -> "Gagal menyimpan impor data sekolah."
                    }
                    schoolPreview = null
                }, enabled = changes > 0) {
                    Text("Simpan Data", color = Navy)
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolPreview = null }) {
                    Text("Batal", color = Subtle)
                }
            }
        )
    }

    promotion?.let { entry ->
        val validTargets = directory.classes.filter {
            it.major == entry.major && it.label != entry.classLabel
        }.sortedBy { it.label }
        AlertDialog(
            onDismissRequest = { promotion = null },
            title = { Text("Pindah / Naik Kelas", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(entry.fullName + " • " + entry.classLabel, color = Ink, fontSize = 13.sp)
                    Text("Admin memilih kelas tujuan. Tahun angkatan dan akun siswa tetap.",
                        color = Subtle, fontSize = 12.sp)
                    AdminSelect("Kelas tujuan", promotionTarget,
                        validTargets.map { it.label },
                        { promotionTarget = it }, Modifier.fillMaxWidth())
                    if (validTargets.isEmpty()) Text("Buat kelas tujuan terlebih dahulu.",
                        color = Bad, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = directory.copy(roster = directory.roster.map {
                        if (it.id == entry.id) it.copy(classLabel = promotionTarget) else it
                    })
                    commit(next, "Kelas siswa berhasil diperbarui.")
                    promotion = null
                }, enabled = validTargets.any { it.label == promotionTarget }) {
                    Text("Simpan perpindahan", color = Navy)
                }
            },
            dismissButton = {
                TextButton(onClick = { promotion = null }) { Text("Batal", color = Subtle) }
            }
        )
    }
}
