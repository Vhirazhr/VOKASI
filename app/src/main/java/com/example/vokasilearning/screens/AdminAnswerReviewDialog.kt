package com.example.vokasilearning.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.model.AnswerStatus
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.logic.AdminPagination
import com.example.vokasilearning.ui.*

/** Exactly ten answer rows at a time, even for 100-question sessions. */
@Composable
internal fun AdminAnswerReviewDialog(
    attempt: Attempt,
    studentName: String,
    classroom: String,
    onClose: () -> Unit
) {
    var page by remember(attempt.sessionKey) { mutableIntStateOf(0) }
    var onlyWrong by remember(attempt.sessionKey) { mutableStateOf(false) }
    val selected = attempt.review.withIndex().filter {
        !onlyWrong || it.value.status != AnswerStatus.BENAR
    }
    val count = AdminPagination.count(selected.size, 10)
    val current = AdminPagination.active(page, selected.size, 10)
    val visible = AdminPagination.slice(selected, current, 10)
    val reviewListState = rememberLazyListState()
    LaunchedEffect(current, onlyWrong, attempt.sessionKey) {
        reviewListState.scrollToItem(0)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pembahasan Siswa", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(studentName.ifBlank { "Nama siswa tidak tersedia" },
                    color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 2)
                Text(
                    "${classroom.ifBlank { "Kelas tidak tersedia" }} • Nilai ${attempt.score}/100 • ${attempt.correct}/${attempt.questionCount} benar",
                    color = Subtle, fontSize = 11.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlyWrong,
                        onClick = { onlyWrong = false; page = 0 },
                        label = { Text("Semua", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = onlyWrong,
                        onClick = { onlyWrong = true; page = 0 },
                        label = { Text("Perlu ditinjau", fontSize = 11.sp) }
                    )
                }
                Text("${selected.size} soal • ${current + 1}/$count", color = Subtle, fontSize = 11.sp)
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    state = reviewListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(visible) { _, indexed ->
                        val item = indexed.value
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HorizontalDivider(color = Border)
                            Text("Soal ${indexed.index + 1} • ${item.status.name.replace('_', ' ')}",
                                color = if (item.status == AnswerStatus.BENAR) Good else Bad,
                                fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(item.question.question, color = Ink, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            Text(
                                "Jawaban siswa: ${item.selectedIndex?.let { item.question.options.getOrNull(it) } ?: "Tidak dijawab"}",
                                color = Ink, fontSize = 11.sp
                            )
                            Text(
                                "Kunci: ${item.question.options.getOrElse(item.question.answerIndex) { "-" }}",
                                color = Good, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            if (item.question.explanation.isNotBlank()) {
                                Text(item.question.explanation, color = Subtle, fontSize = 11.sp)
                            }
                        }
                    }
                    if (visible.isEmpty()) {
                        item { Text("Tidak ada soal pada filter ini.", color = Subtle, fontSize = 12.sp) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        enabled = current > 0,
                        onClick = { page = (current - 1).coerceAtLeast(0) }
                    ) { Text("Sebelumnya", fontSize = 11.sp) }
                    Text("${current + 1}/$count", color = Navy, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp))
                    TextButton(
                        enabled = current < count - 1,
                        onClick = { page = (current + 1).coerceAtMost(count - 1) }
                    ) { Text("Berikutnya", fontSize = 11.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup", color = Navy) } },
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White
    )
}
