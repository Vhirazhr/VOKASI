package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.StudentNoticeStore
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*

@Composable
internal fun StudentNotificationsScreen(
    me: Student,
    attempts: List<Attempt>,
    onOpenPractice: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var remindStreak by remember(me.username) {
        mutableStateOf(StudentNoticeStore.streakReminder(context, me.username))
    }
    var readRevision by remember(me.username) { mutableIntStateOf(0) }
    val notices = remember(me.username, attempts, remindStreak, readRevision) {
        StudentNoticeStore.forStudent(me, attempts, remindStreak)
            .filterNot { StudentNoticeStore.isRead(context, me.username, it.key) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StudentScreenHeader("Notifikasi", "Pengingat belajar dan informasi akun.") }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy) }
                    if (notices.size > 1) {
                        TextButton(onClick = {
                            StudentNoticeStore.markAllRead(context, me.username, notices)
                            readRevision++
                        }) { Text("Tandai semua dibaca", color = Navy, fontSize = 12.sp) }
                    }
                }
                Text(
                    if (notices.isEmpty()) "Tidak ada pemberitahuan baru"
                    else notices.size.toString() + " pemberitahuan baru",
                    color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        }
        items(notices, key = { it.key }) { notification ->
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text(notification.title, color = Ink, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text(notification.description, color = Subtle, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        StudentNoticeStore.markRead(context, me.username, notification.key)
                        readRevision++
                    }) { Text("Tandai dibaca", color = Navy, fontSize = 12.sp) }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Pengingat Streak", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Aktifkan pengingat saat streak masih dapat dilanjutkan.",
                        modifier = Modifier.weight(1f), color = Subtle, fontSize = 12.sp)
                    Switch(checked = remindStreak, onCheckedChange = {
                        remindStreak = it
                        StudentNoticeStore.setStreakReminder(context, me.username, it)
                        readRevision++
                    })
                }
                OutlinedButton(
                    onClick = onOpenPractice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Buka Latihan", color = Navy) }
            }
        }
        item {
            Text("Pengingat tampil di aplikasi, bukan push terjadwal.",
                color = Subtle, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
        }
    }
}
