package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.SchoolLeaderboardEntry
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

private fun SchoolLeaderboardEntry.asDisplayStudent(me: Student): Student =
    if (username.equals(me.username, ignoreCase = true)) me
    else Student(username, "", displayName, Classroom(classroom, major))

@Composable
internal fun LeaderboardScreen(me: Student, leaderboard: List<SchoolLeaderboardEntry>, dataReady: Boolean, dataError: String, onRetry: () -> Unit) {
    var metric by remember { mutableStateOf(RankingMetric.XP) }
    var scope by remember { mutableStateOf(RankingScope.SCHOOL) }
    var chosenMajor by remember { mutableStateOf(me.classroom.major) }
    var chosenClass by remember { mutableStateOf(me.classroom.label) }
    var filterOpen by remember { mutableStateOf(false) }

    val groups = if (scope == RankingScope.CLASS) {
        leaderboard.filter { it.major == chosenMajor }.map { it.classroom }.distinct().sorted()
    } else {
        leaderboard.map { it.major }.distinct().sorted()
    }
    LaunchedEffect(scope, chosenMajor, leaderboard.size) {
        if (scope == RankingScope.CLASS && chosenClass !in groups) {
            chosenClass = groups.firstOrNull().orEmpty()
        }
    }
    val groupValue = if (scope == RankingScope.CLASS) chosenClass else chosenMajor
    val ordered = if (dataReady) leaderboard.filter { entry ->
        val matchesScope = when (scope) {
            RankingScope.SCHOOL -> true
            RankingScope.MAJOR -> entry.major == chosenMajor
            RankingScope.CLASS -> entry.major == chosenMajor && entry.classroom == chosenClass
        }
        matchesScope && (metric != RankingMetric.SCORE || entry.mandatoryDays > 0)
    }.sortedWith(compareByDescending<SchoolLeaderboardEntry> {
        when (metric) {
            RankingMetric.XP -> it.seasonXp
            RankingMetric.SCORE -> (it.mandatoryAverage ?: -1).toLong()
            RankingMetric.STREAK -> it.streak
        }
    }.thenByDescending { if (metric == RankingMetric.SCORE) it.mandatoryDays else 0L }
        .thenBy { it.displayName }.thenBy { it.username }) else emptyList()
    val meRank = ordered.indexOfFirst { it.username.equals(me.username, true) }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StudentScreenHeader(
                title = "Papan Peringkat",
                subtitle = if (dataReady) "Peringkat peserta dari hasil di Supabase." else "Menghubungkan papan peringkat sekolah..."
            )
        }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                StudentSectionHeading("KLASEMEN", "Pilih kategori")
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        RankingMetric.XP to "XP Season",
                        RankingMetric.SCORE to "Nilai Wajib",
                        RankingMetric.STREAK to "Daily Streak"
                    ).forEach { (option, label) ->
                        val active = metric == option
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (active) Navy else Color.Transparent)
                                .clickable { metric = option }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (active) Color.White else Subtle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        RankingScope.SCHOOL to "Sekolah",
                        RankingScope.MAJOR to "Jurusan",
                        RankingScope.CLASS to "Kelas"
                    ).forEach { (option, label) ->
                        val active = scope == option
                        Box(
                            Modifier.weight(1f).height(39.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) PaleBlue else Color.White)
                                .border(1.dp, if (active) NavyLight else Border, RoundedCornerShape(12.dp))
                                .clickable { scope = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (active) Navy else Subtle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                if (scope != RankingScope.SCHOOL) {
                    Box {
                        OutlinedButton(
                            onClick = { filterOpen = true },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Pilih ${if (scope == RankingScope.CLASS) "kelas" else "jurusan"}: $groupValue  ▾",
                                color = Ink,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = filterOpen,
                            onDismissRequest = { filterOpen = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            groups.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        if (scope == RankingScope.CLASS) chosenClass = option
                                        else chosenMajor = option
                                        filterOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (meRank >= 0) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("POSISIMU", color = NavyLight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text("#${meRank + 1} dari ${ordered.size} peserta aktif", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Pill(
                            when (metric) {
                                RankingMetric.XP -> "${ordered[meRank].seasonXp} XP"
                                RankingMetric.SCORE -> "${ordered[meRank].mandatoryAverage ?: 0}/100"
                                RankingMetric.STREAK -> "${ordered[meRank].streak} hari"
                            },
                            PaleBlue, Navy
                        )
                    }
                }
            }
        }
        if (ordered.isNotEmpty()) {
            item {
                Column(
                    Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StudentSectionHeading("TOP 3", "Terbaik saat ini")
                    if (ordered.size >= 3) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            PodiumItem(ordered[1], me, 2, metric, Modifier.weight(1f).height(163.dp))
                            PodiumItem(ordered[0], me, 1, metric, Modifier.weight(1f).height(194.dp))
                            PodiumItem(ordered[2], me, 3, metric, Modifier.weight(1f).height(163.dp))
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            ordered.forEachIndexed { i, student ->
                                PodiumItem(
                                    student, me, i + 1, metric,
                                    Modifier.weight(1f).height(if (i == 0) 190.dp else 163.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                StudentSectionHeading(
                    "SEMUA PESERTA",
                    "Daftar peringkat",
                    Modifier.padding(horizontal = 18.dp)
                )
            }
            itemsIndexed(ordered, key = { _, entry -> entry.username }) { index, entry ->
                val isMe = entry.username.equals(me.username, true)
                val student = entry.asDisplayStudent(me)
                Column(Modifier.padding(horizontal = 18.dp)) {
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                "#${index + 1}",
                                modifier = Modifier.width(27.dp),
                                color = if (index == 0) Warning else NavyLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Avatar(student, 42.dp, syncFromCloud = true)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        student.name,
                                        color = Ink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Text(
                                    "${entry.classroom} • LV ${levelFor(entry.lifetimeXp.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).level}",
                                    color = Subtle,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                if (isMe) {
                                    Text("KAMU", color = NavyLight, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                                }
                            }
                            Text(
                                when (metric) {
                                    RankingMetric.XP -> "${entry.seasonXp} XP"
                                    RankingMetric.SCORE -> "${entry.mandatoryAverage ?: 0}/100"
                                    RankingMetric.STREAK -> "${entry.streak} hari"
                                },
                                color = Navy,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            when {
                                !dataReady && dataError.isNotBlank() -> dataError
                                !dataReady -> "Memuat hasil peringkat dari Supabase..."
                                else -> if (metric == RankingMetric.SCORE)
                                    "Belum ada nilai Latihan Wajib pada musim ini."
                                    else "Belum ada peserta dengan hasil tercatat di kategori ini."
                            },
                            color = Subtle
                        )
                        if (!dataReady && dataError.isNotBlank()) {
                            TextButton(onClick = onRetry) { Text("Coba lagi", color = Navy) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PodiumItem(
    entry: SchoolLeaderboardEntry,
    me: Student,
    rank: Int,
    metric: RankingMetric,
    modifier: Modifier = Modifier
) {
    val student = entry.asDisplayStudent(me)
    val accent = when (rank) {
        1 -> Color(0xFFFFE1A5)
        2 -> Color(0xFFE5EBF2)
        else -> Color(0xFFF6D9C3)
    }
    val label = when (metric) {
        RankingMetric.XP -> "${entry.seasonXp} XP"
        RankingMetric.SCORE -> "${entry.mandatoryAverage ?: 0}/100"
        RankingMetric.STREAK -> "${entry.streak} hari"
    }
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(19.dp))
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("★", color = NavyLight, fontSize = 17.sp)
        Avatar(student, if (rank == 1) 52.dp else 43.dp, syncFromCloud = true)
        Text(
            entry.displayName,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.fillMaxWidth()
                .background(accent, RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("#$rank", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(
                label,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
