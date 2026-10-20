package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.dayKey
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.ui.*

/** Independent student history, latest first; does not dump all sessions in Profile. */
@Composable
internal fun StudentHistoryScreen(
    attempts: List<Attempt>,
    onBack: () -> Unit,
    onReview: (Attempt) -> Unit
) {
    var filter by remember { mutableStateOf("Semua") }
    var currentPage by remember { mutableIntStateOf(0) }
    val filtered = attempts.filter { attempt ->
        when (filter) {
            "Wajib" -> attempt.practiceCategory == null
            "Latihan" -> attempt.practiceCategory != null
            else -> true
        }
    }.sortedWith(compareByDescending<Attempt> { it.timeMillis }.thenByDescending { it.sessionKey })
    val pageSize = 10
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val page = currentPage.coerceIn(0, totalPages - 1)
    val visible = filtered.drop(page * pageSize).take(pageSize)

    Column(Modifier.fillMaxSize().background(Canvas)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Profil", color = Navy, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text("Riwayat Nilai", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semua", "Wajib", "Latihan").forEach { value ->
                        val selected = filter == value
                        Button(
                            onClick = { filter = value; currentPage = 0 },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Navy else Color.White,
                                contentColor = if (selected) Color.White else Subtle
                            )
                        ) { Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item {
                Text(
                    "${filtered.size} sesi • Terbaru lebih dulu",
                    color = Subtle, fontSize = 12.sp
                )
            }
            if (visible.isEmpty()) {
                item {
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Text("Belum ada riwayat pada filter ini.", color = Subtle)
                    }
                }
            } else {
                items(visible, key = { it.sessionKey.ifBlank { it.timeMillis.toString() } }) { attempt ->
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    attempt.practiceCategory?.let { "Latihan $it" } ?: "Latihan Wajib",
                                    color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${dayKey(attempt.timeMillis)} • ${attempt.correct}/${attempt.questionCount} benar",
                                    color = Subtle, fontSize = 11.sp
                                )
                            }
                            Text("${attempt.score}/100", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = { onReview(attempt) }) {
                            Text("Lihat hasil & pembahasan →", color = Navy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (totalPages > 1) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                enabled = page > 0,
                                onClick = { currentPage = (page - 1).coerceAtLeast(0) }
                            ) { Text("‹ Sebelumnya") }
                            Text("${page + 1} / $totalPages", color = Ink, fontWeight = FontWeight.Bold)
                            TextButton(
                                enabled = page < totalPages - 1,
                                onClick = { currentPage = (page + 1).coerceAtMost(totalPages - 1) }
                            ) { Text("Berikutnya ›") }
                        }
                    }
                }
            }
        }
    }
}
