package com.example.vokasilearning.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.BankPolicy
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminAddQuestionScreen(
    onBack: () -> Unit,
    onSave: (String, String, List<String>, Int, String, String) -> String?
) {
    val codes = linkedMapOf(
        "Perkalian" to listOf("K1", "K2", "K3", "K4", "K5"),
        "Pembagian" to listOf("P1", "P2", "P3", "P4", "P5"),
        "Penjumlahan" to listOf("J"),
        "Konversi Satuan" to listOf("S1", "S2")
    )
    var category by remember { mutableStateOf("Perkalian") }
    var difficulty by remember { mutableStateOf("K1") }
    var prompt by remember { mutableStateOf("") }
    val choices = remember { mutableStateListOf("", "", "", "") }
    var correct by remember { mutableStateOf<Int?>(null) }
    var explanation by remember { mutableStateOf("") }
    var categoryMenu by remember { mutableStateOf(false) }
    var codeMenu by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    val dirty = prompt.isNotBlank() || choices.any { it.isNotBlank() } ||
        explanation.isNotBlank() || correct != null ||
        category != "Perkalian" || difficulty != "K1"
    val valid = prompt.isNotBlank() && explanation.isNotBlank() &&
        correct != null && choices.all { it.isNotBlank() } &&
        choices.map { it.trim().lowercase() }.distinct().size == 4

    fun requestBack() {
        if (dirty) confirmLeave = true else onBack()
    }
    BackHandler { requestBack() }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Tambah Soal", "Buat soal dengan kunci dan pembahasan", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                        .padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                        TextButton(onClick = { requestBack() }) {
                            Text("‹ Kembali ke Bank Soal", color = Navy,
                                fontWeight = FontWeight.Bold)
                        }
                        Text("Isi semua bagian sebelum menyimpan.",
                            color = Subtle, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("MATERI & TINGKAT", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryMenu = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(category, modifier = Modifier.weight(1f), color = Ink)
                        Text("⌄", color = Navy)
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        codes.keys.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                category = name; difficulty = codes.getValue(name).first()
                                errorMessage = null
                                categoryMenu = false
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { codeMenu = true },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text("Kode $difficulty", fontSize = 12.sp, color = Ink)
                        }
                        DropdownMenu(expanded = codeMenu, onDismissRequest = { codeMenu = false }) {
                            codes.getValue(category).forEach { code ->
                                DropdownMenuItem(text = { Text(code) }, onClick = {
                                    difficulty = code; errorMessage = null; codeMenu = false
                                })
                            }
                        }
                    }
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .background(PaleBlue, RoundedCornerShape(12.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            "${BankPolicy.secondsByCode.getValue(difficulty)} detik / soal",
                            fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("PERTANYAAN", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it; errorMessage = null },
                    placeholder = { Text("Tulis pertanyaan yang jelas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("PILIHAN & KUNCI", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                choices.forEachIndexed { i, choice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = correct == i, onClick = { correct = i; errorMessage = null },
                            modifier = Modifier.semantics {
                                contentDescription = "Kunci jawaban ${'A' + i}"
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Navy)
                        )
                        OutlinedTextField(
                            value = choice, onValueChange = { choices[i] = it; errorMessage = null },
                            label = { Text("Jawaban ${'A' + i}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Text(if (correct == null) "Pilih satu kunci jawaban." else "Kunci: jawaban ${'A' + correct!!}",
                    fontSize = 11.sp, color = if (correct == null) Warning else Good)
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("PEMBAHASAN", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it; errorMessage = null },
                    placeholder = { Text("Cara memperoleh jawaban benar") },
                    minLines = 2, maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        if (errorMessage != null) {
            item {
                Text(
                    errorMessage.orEmpty(),
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = Bad, fontSize = 12.sp
                )
            }
        }
        item {
            VokasiButton(
                text = "Simpan Soal",
                enabled = valid,
                onClick = {
                    correct?.let { selectedKey ->
                        errorMessage = onSave(
                            category, prompt.trim(), choices.map { it.trim() },
                            selectedKey, difficulty, explanation.trim()
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Batalkan soal?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Isian yang belum disimpan akan hilang.") },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onBack() }) {
                    Text("Keluar", color = Bad)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text("Lanjut Edit", color = Navy)
                }
            }
        )
    }
}
