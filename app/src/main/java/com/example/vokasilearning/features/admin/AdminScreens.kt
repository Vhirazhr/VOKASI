package com.example.vokasilearning.features.admin

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.vokasilearning.app.*
import com.example.vokasilearning.navigation.*
import com.example.vokasilearning.domain.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.features.auth.*
import com.example.vokasilearning.features.student.*
import com.example.vokasilearning.features.admin.*
import com.example.vokasilearning.ui.components.*
import com.example.vokasilearning.ui.theme.*
import com.example.vokasilearning.ui.adaptive.*
import com.example.vokasilearning.ui.security.*

@Composable
internal fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    onQuestions: () -> Unit, onMonitoring: () -> Unit, onLogout: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy))).padding(24.dp)
            ) {
                Text("VOKASI • ADMIN", color = Color(0xFFB0C4DE), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Panel Pengelola", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(SCHOOL, color = Color(0xFFD5E2F0), fontSize = 14.sp)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Siswa demo", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = students.size, textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Soal tersedia", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = questions.size, textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Kelola Soal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text("Tambah pertanyaan, empat pilihan jawaban, dan kunci jawabannya.", color = Subtle, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                VokasiButton(text = "Buka Bank Soal →", onClick = onQuestions, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Monitoring", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text("Lihat siswa, kelas, XP, dan hasil latihan yang tercatat secara lokal.", color = Subtle, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                VokasiButton(text = "Lihat Monitoring →", onClick = onMonitoring, isSecondary = true, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Aturan otomatis sistem", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("• 10 soal acak per sesi", color = Subtle, fontSize = 13.sp)
                Text("• 60 detik per soal", color = Subtle, fontSize = 13.sp)
                Text("• +20 XP per sesi, max 10.000/hari", color = Subtle, fontSize = 13.sp)
                Text("• Season berlangsung 3 bulan", color = Subtle, fontSize = 13.sp)
            }
        }

        item { VokasiButton(text = "Keluar dari Admin", onClick = onLogout, isSecondary = true, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
internal fun AdminQuestionsScreen(questions: List<Question>, onAdd: () -> Unit, onDelete: (Int) -> Unit) {
    var category by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua") + questions.map { it.category }.distinct()
    var deleteId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { VokasiTitle("Bank Soal", "Pertanyaan langsung, tanpa materi atau kisi-kisi.") }
        item { VokasiButton(text = "+ Tambah Soal", onClick = onAdd, modifier = Modifier.fillMaxWidth()) }

        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    FilterChip(
                        selected = item == category,
                        onClick = { category = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaleBlue, selectedLabelColor = Navy)
                    )
                }
            }
        }

        items(questions.filter { category == "Semua" || it.category == category }, key = { it.id }) { q ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(q.category)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { deleteId = q.id }) { Text("Hapus", color = Bad, fontWeight = FontWeight.Bold) }
                }
                Text(q.question, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                Text("Kunci: ${'A' + q.answerIndex}. ${q.options[q.answerIndex]}", color = Good, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Hapus soal?", fontWeight = FontWeight.Bold) },
            text = { Text("Soal ini akan hilang dari prototipe.") },
            confirmButton = { TextButton(onClick = { deleteId?.let(onDelete); deleteId = null }) { Text("Hapus", color = Bad) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
        )
    }
}

@Composable
internal fun AdminAddQuestionScreen(onBack: () -> Unit, onSave: (String, String, List<String>, Int) -> Unit) {
    var category by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    val choices = remember { mutableStateListOf("", "", "", "") }
    var correct by remember { mutableIntStateOf(0) }

    val valid = category.isNotBlank() && prompt.isNotBlank() && choices.all { it.isNotBlank() } && choices.map { it.trim() }.distinct().size == 4

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Kembali ke Bank Soal", color = Navy) } }
        item { VokasiTitle("Tambah Soal Baru", "Admin cukup mengisi soal, opsi jawaban, dan kunci.") }

        item { OutlinedTextField(category, { category = it }, label = { Text("Kategori (contoh: Perkalian)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(prompt, { prompt = it }, label = { Text("Pertanyaan") }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        items(4) { i ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(
                    selected = correct == i,
                    onClick = { correct = i },
                    colors = RadioButtonDefaults.colors(selectedColor = Navy)
                )
                OutlinedTextField(
                    value = choices[i], onValueChange = { choices[i] = it },
                    label = { Text("Jawaban ${'A' + i}") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item { Text("Pilih lingkaran di kiri jawaban yang BENAR.", color = Subtle, fontSize = 12.sp) }
        item {
            VokasiButton(
                text = "Simpan Soal",
                onClick = { onSave(category.trim(), prompt.trim(), choices.map { it.trim() }, correct) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AdminMonitorScreen(students: List<Student>, attempts: List<Attempt>) {
    var selectedClass by remember { mutableStateOf("Semua kelas") }
    var filterOpen by remember { mutableStateOf(false) }
    val group = listOf("Semua kelas") + demoClasses.map { it.label }.distinct()
    val filtered = students.filter { selectedClass == "Semua kelas" || it.classroom.label == selectedClass }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { VokasiTitle("Monitoring Siswa", "Ringkasan hasil dan progres latihan • dummy") }

        item {
            Box {
                OutlinedButton(onClick = { filterOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(selectedClass, color = Ink)
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    group.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { selectedClass = item; filterOpen = false })
                    }
                }
            }
        }

        items(filtered, key = { it.username }) { student ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(student)
                    Column(Modifier.weight(1f)) {
                        Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("${student.classroom.label} • ${student.classroom.major}", fontSize = 12.sp, color = Subtle)
                    }
                    Pill("${studentSeasonXp(student, attempts)} XP", PaleBlue, Navy)
                }
                Text("Streak: ${calculateStreak(student, attempts)} hari  •  Sesi baru: ${attempts.count { it.studentId == student.username }}", color = Subtle, fontSize = 12.sp)
            }
        }
    }
}
