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
internal fun StudentHome(
    me: Student, students: List<Student>, attempts: List<Attempt>, countQuestions: Int,
    onStart: () -> Unit, onRanking: () -> Unit
) {
    val todayCount = attempts.count { it.studentId == me.username && dayKey(it.timeMillis) == dayKey(System.currentTimeMillis()) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            // Premium Header
            Column(
                Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy)))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(SCHOOL.uppercase(), color = Color(0xFFB0C4DE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Halo, ${me.name.substringBefore(' ')}! 👋", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${me.classroom.label} • Siap mengerjakan soal?", color = Color(0xFFD5E2F0), fontSize = 13.sp)
                    }
                    Avatar(me, 52.dp)
                }

                // Hero Card (Latihan Wajib)
                Card(
                    Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("LATIHAN UTAMA", PaleBlue, Navy)
                            Spacer(Modifier.weight(1f))
                            Text("Hari ini", fontSize = 12.sp, color = Subtle, fontWeight = FontWeight.Medium)
                        }
                        Text("Latihan Wajib", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Soal acak otomatis dari perkalian, pembagian, konversi satuan, dan rumus dasar.", fontSize = 13.sp, color = Subtle, lineHeight = 18.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("${minOf(QUESTIONS_PER_SESSION, countQuestions)} soal", Canvas, Ink)
                            Pill("${minOf(QUESTIONS_PER_SESSION, countQuestions) * SECONDS_PER_QUESTION / 60} mnt", Canvas, Ink)
                            Pill("Anti-screenshot", PaleGreen, Good)
                        }

                        VokasiButton(
                            text = "Mulai Mengerjakan →",
                            onClick = onStart,
                            enabled = countQuestions > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Info & Stats
        item {
            StaggeredSlideIn(0) {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistik Sesi", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Pill(seasonTitle(), PaleGold, Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Sesi hari ini", color = Subtle, fontSize = 12.sp)
                            Text("$todayCount", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Navy)
                        }
                        Column {
                            Text("Total XP", color = Subtle, fontSize = 12.sp)
                            AnimatedCounter(target = studentSeasonXp(me, attempts), textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Navy)
                        }
                    }
                }
            }
        }

        // Leaderboard Preview
        item {
            StaggeredSlideIn(1) {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Top 3 Peringkat", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRanking) { Text("Lihat semua →", fontWeight = FontWeight.SemiBold, color = Navy) }
                }
            }
        }

        item {
            StaggeredSlideIn(2) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val leaders = rankList(students, attempts, me, RankingScope.SCHOOL, RankingMetric.XP).take(3)
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Se-Sekolah", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Pill("XP", PaleBlue, Navy)
                        }
                        leaders.forEachIndexed { rank, student ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(listOf("🥇", "🥈", "🥉")[rank], fontSize = 20.sp)
                                Avatar(student, 36.dp)
                                Text(student.name, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.Medium)
                                Text("${studentSeasonXp(student, attempts)} XP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                            }
                        }
                    }
                }
            }
        }
    }
}
