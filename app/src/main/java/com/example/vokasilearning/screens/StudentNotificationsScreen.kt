package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.StudentNoticeStore
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun StudentNotificationsScreen(
    me: Student, attempts: List<Attempt>, onOpenPractice: () -> Unit,
    onOpenSettings: () -> Unit, onBack: () -> Unit
) {
    val context = LocalContext.current
    var remindStreak by remember(me.username) {
        mutableStateOf(StudentNoticeStore.streakReminder(context, me.username))
    }
    val notices = StudentNoticeStore.forStudent(me, attempts, remindStreak)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StudentScreenHeader("Notifikasi", "Informasi dan pengingat belajar yang penting.") }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy) }
                Text("Pusat Pemberitahuan", color = Ink, fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp)
                Text("Informasi terbaru, bukan riwayat aktivitas.",
                    color = Subtle, fontSize = 11.sp)
            }
        }
        items(notices, key = { it.key }) { notification ->
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row {
                    Text("●", color = Good, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(notification.title, fontSize = 14.sp, color = Ink,
                            fontWeight = FontWeight.ExtraBold)
                        Text(notification.description, fontSize = 12.sp, color = Subtle)
                    }
                }
            }
        }
        if (notices.isEmpty()) {
            item { WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Semua beres! Tidak ada pemberitahuan penting saat ini.",
                    color = Good, fontSize = 12.sp)
            } }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Preferensi Pengingat", color = Ink,
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pengingat streak", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Tampilkan pengingat konsistensi jika belum belajar hari ini.",
                            color = Subtle, fontSize = 11.sp)
                    }
                    Switch(checked = remindStreak, onCheckedChange = {
                        remindStreak = it
                        StudentNoticeStore.setStreakReminder(context, me.username, it)
                    })
                }
                Text("Pengingat saat ini tampil ketika aplikasi dibuka. Notifikasi push terjadwal memerlukan komponen tambahan.",
                    color = Subtle, fontSize = 11.sp)
                OutlinedButton(onClick = onOpenPractice, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)) {
                    Text("Buka Latihan", color = Navy)
                }
            }
        }
    }
}
