package com.example.vokasilearning.screens

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
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

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

