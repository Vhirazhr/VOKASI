package com.example.vokasilearning.screens

import androidx.compose.foundation.background
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
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminQuestionsScreen(
    questions: List<Question>,
    bundledIds: Set<Int>,
    onAdd: () -> Unit,
    onDelete: (Int) -> Boolean,
    notice: String?,
    onDismissNotice: () -> Unit
) {
    var category by remember { mutableStateOf("Semua") }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var pagingActivated by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var deleteId by remember { mutableStateOf<Int?>(null) }
    var localNotice by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    val categories = listOf("Semua", "Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan")
    val found = questions.filter { q ->
        (category == "Semua" || q.category == category) &&
            (search.isBlank() || q.question.contains(search.trim(), true) ||
                q.difficultyCode.contains(search.trim(), true) ||
                q.id.toString() == search.trim())
    }.sortedBy { it.id }
    val pageSize = 15
    val pageCount = maxOf(1, (found.size + pageSize - 1) / pageSize)
    val activePage = page.coerceIn(0, pageCount - 1)
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
                            selected = value == category,
                            onClick = { category = value; page = 0; pagingActivated = false; expandedId = null },
                            label = { Text(value, fontSize = 12.sp) }
                        )
                    }
                }
                Text("${found.size} soal ditemukan • Maksimal 15 per halaman",
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
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Soal ${activePage * pageSize + 1}–${activePage * pageSize + visibleQuestions.size} dari ${found.size}",
                        color = Subtle, fontSize = 11.sp
                    )
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            enabled = activePage > 0,
                            onClick = { page = activePage - 1; pagingActivated = true; expandedId = null },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Sebelumnya", fontSize = 12.sp) }
                        Text("${activePage + 1}/$pageCount",
                            color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            enabled = activePage < pageCount - 1,
                            onClick = { page = activePage + 1; pagingActivated = true; expandedId = null },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Berikutnya", fontSize = 12.sp) }
                    }
                }
            }
        }
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
            title = { Text(if (alertText.contains("berhasil", ignoreCase = true))
                "Bank Soal Diperbarui" else "Tindakan Gagal",
                fontWeight = FontWeight.ExtraBold) },
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
