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
internal fun LeaderboardScreen(me: Student, users: List<Student>, attempts: List<Attempt>) {
    var metric by remember { mutableStateOf(RankingMetric.XP) }
    var scope by remember { mutableStateOf(RankingScope.SCHOOL) }
    var chosenMajor by remember { mutableStateOf(me.classroom.major) }
    var chosenClass by remember { mutableStateOf(me.classroom.label) }
    var filterOpen by remember { mutableStateOf(false) }

    val groups = if (scope == RankingScope.CLASS) demoClasses.map { it.label }.distinct() else demoClasses.map { it.major }.distinct()
    val groupValue = if (scope == RankingScope.CLASS) chosenClass else chosenMajor
    val ordered = rankList(users, attempts, me, scope, metric, chosenMajor, chosenClass)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { VokasiTitle("Papan Peringkat 🏆", "${seasonTitle()} • $SCHOOL") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RankingMetric.XP to "XP Season", RankingMetric.STREAK to "🔥 Streak").forEach { (m, title) ->
                    val selected = metric == m
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Navy else Color.White)
                            .border(1.dp, if (selected) Navy else Border, RoundedCornerShape(12.dp))
                            .clickable { metric = m }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, color = if (selected) Color.White else Subtle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(RankingScope.SCHOOL to "Sekolah", RankingScope.MAJOR to "Jurusan", RankingScope.CLASS to "Kelas").forEach { (s, title) ->
                    FilterChip(
                        selected = scope == s, onClick = { scope = s },
                        label = { Text(title, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaleBlue, selectedLabelColor = Navy)
                    )
                }
            }
        }

        if (scope != RankingScope.SCHOOL) item {
            Box {
                OutlinedButton(onClick = { filterOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Pilih ${if (scope == RankingScope.CLASS) "kelas" else "jurusan"}: $groupValue", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    groups.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            if (scope == RankingScope.CLASS) chosenClass = option else chosenMajor = option
                            filterOpen = false
                        })
                    }
                }
            }
        }

        // Podium Top 3 (If available)
        if (ordered.size >= 3) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    // Rank 2
                    PodiumItem(ordered[1], 2, metric, attempts, Modifier.weight(1f).height(160.dp))
                    // Rank 1
                    PodiumItem(ordered[0], 1, metric, attempts, Modifier.weight(1f).height(190.dp))
                    // Rank 3
                    PodiumItem(ordered[2], 3, metric, attempts, Modifier.weight(1f).height(140.dp))
                }
            }
        }

        itemsIndexed(ordered, key = { _, student -> student.username }) { i, student ->
            if (i >= 3 || ordered.size < 3) {
                StaggeredSlideIn(i) {
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${i + 1}.", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = Subtle)
                            Avatar(student, 44.dp)
                            Column(Modifier.weight(1f)) {
                                Text(student.name + if (student.username == me.username) " (Kamu)" else "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, maxLines = 1)
                                Text("${student.classroom.label} • LV ${levelFor(lifetimeXp(student, attempts)).level}", color = Subtle, fontSize = 11.sp)
                            }
                            Text(
                                if (metric == RankingMetric.XP) "${studentSeasonXp(student, attempts)} XP" else "🔥 ${calculateStreak(student, attempts)}",
                                color = Navy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PodiumItem(student: Student, rank: Int, metric: RankingMetric, attempts: List<Attempt>, modifier: Modifier = Modifier) {
    val gradient = when(rank) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFA9A9A9)))
        else -> Brush.verticalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(gradient).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(if(rank == 1) "👑" else "", fontSize = 24.sp)
        Avatar(student, if(rank == 1) 60.dp else 48.dp)
        Spacer(Modifier.height(8.dp))
        Text(student.name.substringBefore(' '), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (metric == RankingMetric.XP) "${studentSeasonXp(student, attempts)} XP" else "🔥 ${calculateStreak(student, attempts)}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
