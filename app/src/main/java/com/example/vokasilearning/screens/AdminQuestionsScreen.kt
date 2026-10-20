package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDelete: (Int) -> Unit
) {
    var category by remember { mutableStateOf("Semua") }
    var search by remember { mutableStateOf("") }
    var visibleCount by remember { mutableIntStateOf(15) }
    var deleteId by remember { mutableStateOf<Int?>(null) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    val categories = listOf("Semua") +
        listOf("Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan")
    val filtered = questions.filter {
        (category == "Semua" || it.category == category) &&
            (search.isBlank() || it.question.contains(search.trim(), ignoreCase = true) ||
                it.difficultyCode.contains(search.trim(), ignoreCase = true) ||
                it.id.toString() == search.trim())
    }.sortedBy { it.id }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VokasiTitle("Bank Soal", "500 soal bawaan dijaga; tambahan tersimpan di perangkat.") }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${questions.size} soal tersedia",
                        color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${questions.count { it.id !in bundledIds }} soal tambahan",
                        color = Subtle, fontSize = 11.sp)
                }
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("Tambah Soal", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; visibleCount = 15 },
                placeholder = { Text("Cari soal, ID, atau kode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { value ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = value; visibleCount = 15 },
                        label = { Text(value, fontSize = 11.sp) }
                    )
                }
            }
        }
        item { Text("${filtered.size} soal ditemukan", color = Subtle, fontSize = 11.sp) }
        if (filtered.isEmpty()) {
            item { WhiteCard(Modifier.fillMaxWidth()) { Text("Tidak ada soal yang cocok.", color = Subtle) } }
        } else {
            items(filtered.take(visibleCount), key = { it.id }) { q ->
                val editable = q.id !in bundledIds
                WhiteCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("${q.difficultyCode} • ${q.timeLimitSeconds} dtk", PaleBlue, Navy)
                        Spacer(Modifier.weight(1f))
                        if (editable) {
                            TextButton(onClick = { deleteId = q.id }) {
                                Text("Hapus", color = Bad, fontSize = 12.sp)
                            }
                        } else {
                            Text("Bawaan", color = Subtle, fontSize = 11.sp)
                        }
                    }
                    Text(q.question, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q.category, color = Subtle, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { expandedId = if (expandedId == q.id) null else q.id }) {
                            Text(if (expandedId == q.id) "Tutup" else "Lihat Detail",
                                fontSize = 12.sp, color = Navy)
                        }
                    }
                    if (expandedId == q.id) {
                        HorizontalDivider(color = Border)
                        q.options.forEachIndexed { index, option ->
                            Text("${'A' + index}. $option",
                                color = if (index == q.answerIndex) Good else Subtle,
                                fontSize = 12.sp,
                                fontWeight = if (index == q.answerIndex) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (q.explanation.isNotBlank()) {
                            Text("Pembahasan: ${q.explanation}", color = Ink, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (filtered.size > visibleCount) {
                item {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { visibleCount += 15 }
                    ) { Text("Muat 15 lagi", color = Navy, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Hapus soal tambahan?", fontWeight = FontWeight.Bold) },
            text = { Text("Soal ini akan dihapus dari bank lokal di perangkat ini.") },
            confirmButton = {
                TextButton(onClick = { onDelete(id); deleteId = null; expandedId = null }) {
                    Text("Hapus", color = Bad)
                }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
        )
    }
}
