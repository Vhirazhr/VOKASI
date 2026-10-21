package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.model.Question
import com.example.vokasilearning.data.QuestionExcelFile
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.vokasilearning.logic.AdminPagination
import com.example.vokasilearning.logic.AdminQuestionFilter
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminQuestionsScreen(
    questions: List<Question>,
    bundledIds: Set<Int>,
    onAdd: () -> Unit,
    onImport: (List<QuestionExcelFile.Draft>) -> String,
    onExport: (Int, List<String>) -> Unit,
    onDelete: (Int) -> Boolean,
    notice: String?,
    onDismissNotice: () -> Unit
) {
    var selectedCategories by remember { mutableStateOf(emptySet<String>()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportCategories by remember { mutableStateOf(emptySet<String>()) }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var pagingActivated by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var deleteId by remember { mutableStateOf<Int?>(null) }
    var localNotice by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var excelBusy by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<List<Question>>(emptyList()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val excelMime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !excelBusy) {
            coroutineScope.launch {
                excelBusy = true
                localNotice = try {
                    val drafts = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { QuestionExcelFile.read(it) }
                            ?: throw IllegalArgumentException("File Excel tidak dapat dibuka.")
                    }
                    onImport(drafts)
                } catch (e: IllegalArgumentException) {
                    e.message ?: "Format Excel tidak sesuai."
                } catch (_: Exception) {
                    "Impor gagal. Periksa izin akses dan format file .xlsx."
                } finally {
                    excelBusy = false
                }
            }
        }
    }
    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(excelMime)
    ) { uri ->
        if (uri != null && !excelBusy) {
            coroutineScope.launch {
                excelBusy = true
                localNotice = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use {
                            QuestionExcelFile.writeTemplate(it)
                        } ?: throw IllegalArgumentException("Lokasi penyimpanan tidak dapat dibuka.")
                    }
                    "Template Excel berhasil disimpan."
                } catch (e: IllegalArgumentException) {
                    e.message ?: "Gagal membuat template Excel."
                } catch (_: Exception) {
                    "Gagal menyimpan template Excel."
                } finally {
                    excelBusy = false
                }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(excelMime)
    ) { uri ->
        val batch = pendingExport
        pendingExport = emptyList()
        if (uri != null && batch.isNotEmpty() && !excelBusy) {
            coroutineScope.launch {
                excelBusy = true
                localNotice = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            QuestionExcelFile.writeExport(output, batch)
                        } ?: throw IllegalArgumentException("Lokasi penyimpanan tidak dapat dibuka.")
                    }
                    onExport(batch.size, batch.map { it.category }.distinct())
                    "Ekspor Excel berhasil: " + batch.size + " soal tersimpan."
                } catch (e: IllegalArgumentException) {
                    e.message ?: "Data soal tidak dapat diekspor."
                } catch (_: Exception) {
                    "Gagal mengekspor Excel. Periksa lokasi penyimpanan."
                } finally {
                    excelBusy = false
                }
            }
        }
    }
    val categories = listOf("Semua", "Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan")
    val found = AdminQuestionFilter.apply(questions, selectedCategories, search)
    val pageSize = AdminPagination.PAGE_SIZE
    val pageCount = AdminPagination.count(found.size, pageSize)
    val activePage = AdminPagination.active(page, found.size, pageSize)
    val visibleQuestions = found.drop(activePage * pageSize).take(pageSize)
    LaunchedEffect(activePage, pagingActivated) {
        if (pagingActivated) listState.scrollToItem(2)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        state = listState,
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Bank Soal", "Kelola soal dan kunci jawaban", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp).padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("BANK SOAL", color = NavyLight,
                                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${questions.size} soal tersedia", color = Ink,
                                    fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${questions.count { it.id !in bundledIds }} soal tambahan lokal",
                                    color = Subtle, fontSize = 11.sp)
                            }
                        }
                        VokasiButton("Tambah Soal", onClick = onAdd, modifier = Modifier.fillMaxWidth())
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf(excelMime)) },
                                enabled = !excelBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Impor Excel", fontSize = 12.sp, color = Navy) }
                            OutlinedButton(
                                onClick = { templateLauncher.launch("Template_Soal_VOKASI.xlsx") },
                                enabled = !excelBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Template Excel", fontSize = 12.sp, color = Navy) }
                        }
                        OutlinedButton(
                            onClick = {
                                exportCategories = if (selectedCategories.isEmpty())
                                    categories.drop(1).toSet() else selectedCategories.toSet()
                                showExportDialog = true
                            },
                            enabled = !excelBusy && questions.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Ekspor Soal Excel", color = Navy, fontSize = 12.sp) }
                        Text("Format .xlsx • Kolom A–I • Semua baris diperiksa sebelum disimpan",
                            color = Subtle, fontSize = 11.sp)
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it; page = 0; pagingActivated = false; expandedId = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari soal, ID, atau kode") },
                            singleLine = true, shape = RoundedCornerShape(13.dp)
                        )
                    }
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminSectionTitle("Materi", "Daftar soal")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { value ->
                        FilterChip(
                            selected = if (value == "Semua") selectedCategories.isEmpty()
                                else value in selectedCategories,
                            onClick = {
                                selectedCategories = AdminQuestionFilter.toggle(selectedCategories, value)
                                page = 0
                                pagingActivated = false
                                expandedId = null
                            },
                            label = { Text(value, fontSize = 12.sp) }
                        )
                    }
                }
                Text("Pilih satu atau beberapa materi. Semua untuk reset filter.",
                    color = Subtle, fontSize = 11.sp)
                Text(found.size.toString() + " soal ditemukan • Maksimal " + pageSize + " per halaman",
                    color = Subtle, fontSize = 11.sp)
            }
        }
        if (found.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Tidak ada soal yang cocok.", color = Subtle, fontSize = 12.sp)
                }
            }
        } else {
            items(visibleQuestions, key = { it.id }) { q ->
                val extra = q.id !in bundledIds
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SOAL #${q.id}", color = NavyLight,
                            fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        Pill("${q.difficultyCode} · ${q.timeLimitSeconds} dtk", PaleBlue, Navy)
                    }
                    Text(q.question, color = Ink, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q.category, color = Subtle, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        Text(if (extra) "Tambahan" else "Bawaan",
                            fontSize = 11.sp,
                            color = if (extra) Good else Subtle)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { expandedId = if (expandedId == q.id) null else q.id },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (expandedId == q.id) "Tutup Detail" else "Lihat Detail",
                                color = Navy, fontSize = 12.sp)
                        }
                        if (extra) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { deleteId = q.id }) {
                                Text("Hapus", color = Bad, fontSize = 12.sp)
                            }
                        }
                    }
                    if (expandedId == q.id) {
                        HorizontalDivider(color = Border)
                        q.options.forEachIndexed { index, option ->
                            Text("${'A' + index}. $option",
                                color = if (index == q.answerIndex) Good else Ink,
                                fontSize = 12.sp,
                                fontWeight = if (index == q.answerIndex) FontWeight.Bold
                                    else FontWeight.Normal)
                        }
                        if (q.explanation.isNotBlank()) {
                            Text("Pembahasan: ${q.explanation}", color = Subtle, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                AdminPaginationFooter(
                    label = "Soal",
                    total = found.size,
                    page = activePage,
                    onPrevious = { page = activePage - 1; pagingActivated = true; expandedId = null },
                    onNext = { page = activePage + 1; pagingActivated = true; expandedId = null },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )
            }
        }
    }
    if (showExportDialog) {
        val options = categories.drop(1)
        val selected = questions.filter { it.category in exportCategories }.sortedBy { it.id }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Ekspor Soal Excel", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Pilih materi yang akan dimasukkan ke satu file Excel.",
                        color = Subtle, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exportCategories.size == options.size,
                            onCheckedChange = { all ->
                                exportCategories = if (all) options.toSet() else emptySet()
                            }
                        )
                        Text("Semua materi", color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Border)
                    options.forEach { subject ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = subject in exportCategories,
                                onCheckedChange = { checked ->
                                    exportCategories = if (checked) exportCategories + subject
                                        else exportCategories - subject
                                }
                            )
                            Text(subject, color = Ink, fontSize = 13.sp)
                        }
                    }
                    Text(selected.size.toString() + " soal akan diekspor.",
                        fontWeight = FontWeight.Bold, color = Navy, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingExport = selected
                    showExportDialog = false
                    val suffix = if (exportCategories.size == options.size) "Semua"
                        else options.filter { it in exportCategories }.joinToString("_") {
                            it.replace(" ", "_")
                        }
                    exportLauncher.launch("VOKASI_Soal_" + suffix + ".xlsx")
                }, enabled = selected.isNotEmpty()) { Text("Simpan Excel", color = Navy) }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Batal", color = Subtle)
                }
            }
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Hapus soal tambahan?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Soal ini akan dihapus dari penyimpanan lokal.") },
            confirmButton = {
                TextButton(onClick = {
                    val success = onDelete(id)
                    localNotice = if (success) "Soal tambahan berhasil dihapus."
                        else "Soal gagal dihapus. Coba lagi."
                    if (success) expandedId = null
                    deleteId = null
                }) {
                    Text("Hapus", color = Bad)
                }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
        )
    }
    val alertText = localNotice ?: notice
    if (alertText != null) {
        AlertDialog(
            onDismissRequest = {
                localNotice = null
                if (notice != null) onDismissNotice()
            },
            title = { Text(when {
                alertText.startsWith("Ekspor Excel berhasil") -> "Ekspor Excel"
                alertText.contains("berhasil", ignoreCase = true) -> "Bank Soal Diperbarui"
                else -> "Tindakan Gagal"
            }, fontWeight = FontWeight.ExtraBold) },
            text = { Text(alertText) },
            confirmButton = {
                TextButton(onClick = {
                    localNotice = null
                    if (notice != null) onDismissNotice()
                }) { Text("Oke", color = Navy, fontWeight = FontWeight.Bold) }
            }
        )
    }

}
