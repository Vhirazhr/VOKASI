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

