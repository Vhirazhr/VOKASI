package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.BankPolicy
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminAddQuestionScreen(
    onBack: () -> Unit,
    onSave: (String, String, List<String>, Int, String, String) -> Boolean
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
    var correct by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    var categoryMenu by remember { mutableStateOf(false) }
    var codeMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val valid = prompt.isNotBlank() && explanation.isNotBlank() &&
        choices.all { it.isNotBlank() } &&
        choices.map { it.trim() }.distinct().size == 4

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy) } }
        item { VokasiTitle("Tambah Soal", "Form terstruktur sesuai aturan kuis.") }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
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
                                    difficulty = code; codeMenu = false
                                })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = {}, enabled = false,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text("${BankPolicy.secondsByCode.getValue(difficulty)} detik", fontSize = 12.sp, color = Navy)
                        }
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("PERTANYAAN", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it; error = false },
                    placeholder = { Text("Tulis pertanyaan yang jelas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("PILIHAN & KUNCI", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                choices.forEachIndexed { i, choice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = correct == i, onClick = { correct = i },
                            colors = RadioButtonDefaults.colors(selectedColor = Navy)
                        )
                        OutlinedTextField(
                            value = choice, onValueChange = { choices[i] = it; error = false },
                            label = { Text("Jawaban ${'A' + i}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Text("Pilih lingkaran pada jawaban benar.", fontSize = 11.sp, color = Subtle)
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("PEMBAHASAN", color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it; error = false },
                    placeholder = { Text("Cara memperoleh jawaban benar") },
                    minLines = 2, maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        if (error) {
            item { Text("Soal tidak tersimpan. Periksa kesamaan pertanyaan atau pilihan.",
                color = Bad, fontSize = 12.sp) }
        }
        item {
            VokasiButton(
                text = "Simpan Soal",
                enabled = valid,
                onClick = {
                    error = !onSave(
                        category, prompt.trim(), choices.map { it.trim() },
                        correct, difficulty, explanation.trim()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
