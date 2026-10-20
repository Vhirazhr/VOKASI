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
import com.example.vokasilearning.data.demoClasses
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

@Composable
internal fun LeaderboardScreen(me: Student, users: List<Student>, attempts: List<Attempt>) {
    var metric by remember { mutableStateOf(RankingMetric.XP) }
    var scope by remember { mutableStateOf(RankingScope.SCHOOL) }
    var chosenMajor by remember { mutableStateOf(me.classroom.major) }
    var chosenClass by remember { mutableStateOf(me.classroom.label) }
    var filterOpen by remember { mutableStateOf(false) }

    val groups = if (scope == RankingScope.CLASS) {
        users.map { it.classroom.label }.plus(demoClasses.map { it.label }).distinct()
    } else {
        users.map { it.classroom.major }.plus(demoClasses.map { it.major }).distinct()
    }
    val groupValue = if (scope == RankingScope.CLASS) chosenClass else chosenMajor
    val ordered = rankList(users, attempts, me, scope, metric, chosenMajor, chosenClass)
    val meRank = ordered.indexOfFirst { it.username == me.username }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StudentScreenHeader(
                title = "Papan Peringkat",
                subtitle = "Lihat pencapaian terbaik di sekolahmu."
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
                                fontSize = 12.sp,
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
                            onDismissRequest = { filterOpen = false }
                        ) {
                            groups.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
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
                            Text("#${meRank + 1} dari ${ordered.size} siswa", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Pill(
                            if (metric == RankingMetric.XP) "${studentSeasonXp(me, attempts)} XP"
                            else "${calculateStreak(me, attempts)} hari",
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
                            PodiumItem(ordered[1], 2, metric, attempts, Modifier.weight(1f).height(163.dp))
                            PodiumItem(ordered[0], 1, metric, attempts, Modifier.weight(1f).height(194.dp))
                            PodiumItem(ordered[2], 3, metric, attempts, Modifier.weight(1f).height(150.dp))
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            ordered.forEachIndexed { i, student ->
                                PodiumItem(
                                    student, i + 1, metric, attempts,
                                    Modifier.weight(1f).height(if (i == 0) 190.dp else 160.dp)
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
            itemsIndexed(ordered, key = { _, student -> student.username }) { index, student ->
                val isMe = student.username == me.username
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
                            Avatar(student, 42.dp)
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
                                    "${student.classroom.label} • LV ${levelFor(lifetimeXp(student, attempts)).level}",
                                    color = Subtle,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                if (isMe) {
                                    Text("KAMU", color = NavyLight, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                                }
                            }
                            Text(
                                if (metric == RankingMetric.XP) "${studentSeasonXp(student, attempts)} XP"
                                else "${calculateStreak(student, attempts)}",
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
                    Text("Belum ada siswa pada kelompok ini.", color = Subtle)
                }
            }
        }
    }
}

@Composable
internal fun PodiumItem(
    student: Student,
    rank: Int,
    metric: RankingMetric,
    attempts: List<Attempt>,
    modifier: Modifier = Modifier
) {
    val accent = when (rank) {
        1 -> Color(0xFFFFE1A5)
        2 -> Color(0xFFE5EBF2)
        else -> Color(0xFFF6D9C3)
    }
    val label = if (metric == RankingMetric.XP) {
        "${studentSeasonXp(student, attempts)} XP"
    } else {
        "${calculateStreak(student, attempts)}"
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
        Avatar(student, if (rank == 1) 52.dp else 43.dp)
        Text(
            student.name.substringBefore(' '),
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
