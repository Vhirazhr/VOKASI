package com.example.vokasilearning.features.student

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
internal fun ProfileScreen(me: Student, attempts: List<Attempt>, onLogout: () -> Unit) {
    val mine = attempts.filter { it.studentId == me.username }
    val level = levelFor(lifetimeXp(me, attempts))
    val answered = mine.sumOf { it.questionCount }
    val accuracy = if (answered > 0) mine.sumOf { it.correct } * 100 / answered else me.seedScore
    val totalXp = lifetimeXp(me, attempts)
    var showLogout by remember { mutableStateOf(false) }

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(totalXp) {
        progressAnim.animateTo(
            targetValue = ((totalXp - level.startXp).toFloat() / (level.nextXp - level.startXp)).coerceIn(0f, 1f),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { VokasiTitle("Profil Siswa", "Riwayat dan perkembangan belajarmu") }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Avatar(me, 76.dp)
                    Column(Modifier.weight(1f)) {
                        Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("@${me.username}", color = Subtle, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Pill("${me.classroom.label} • ${me.classroom.major}", PaleBlue, Navy)
                    }
                }
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Level ${level.level}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                    Pill("$totalXp XP", PaleGold, Ink)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Good, trackColor = PaleGreen
                )
                Spacer(Modifier.height(4.dp))
                Text("${max(0, level.nextXp - totalXp)} XP lagi untuk naik level", color = Subtle, fontSize = 12.sp)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Akurasi", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = accuracy, suffix = "%", textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Streak", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = calculateStreak(me, attempts), prefix = "🔥 ", textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Warning)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("XP Season", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = studentSeasonXp(me, attempts), textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Navy)
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Sesi Selesai", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = mine.size, textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        item { Text("Riwayat Pengerjaan", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 8.dp)) }

        if (mine.isEmpty()) item {
            WhiteCard(Modifier.fillMaxWidth()) { Text("Belum ada latihan yang kamu selesaikan.", color = Subtle, fontSize = 13.sp) }
        }

        items(mine.reversed()) { a ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Latihan Acak", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                        Text(dayKey(a.timeMillis), color = Subtle, fontSize = 12.sp)
                    }
                    Pill("${a.score}%  •  +${a.earnedXp} XP", PaleGreen, Good)
                }
            }
        }

        item { VokasiButton(text = "Keluar dari Akun", onClick = { showLogout = true }, isSecondary = true, modifier = Modifier.fillMaxWidth()) }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Keluar akun?", fontWeight = FontWeight.Bold) },
            text = { Text("Kamu perlu login kembali untuk mengakses aplikasi.") },
            confirmButton = { TextButton(onClick = { showLogout = false; onLogout() }) { Text("Keluar", color = Bad) } },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Batal") } }
        )
    }
}

// --- ADMIN SCREENS ---
