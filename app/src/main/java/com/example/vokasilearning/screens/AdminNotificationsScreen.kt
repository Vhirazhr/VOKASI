package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.AdminLogEvent
import com.example.vokasilearning.data.SchoolDirectory
import com.example.vokasilearning.data.StudentNoticeStore
import com.example.vokasilearning.ui.*

/**
 * Local admin notification center: actionable unclaimed student records and
 * recent activity only. No push notifications or remote sync are implied.
 */
@Composable
internal fun AdminNotificationsScreen(
    directory: SchoolDirectory,
    events: List<AdminLogEvent>,
    onSchool: () -> Unit,
    onBack: () -> Unit
) {
    val unregistered = directory.pendingCount
    val messages = StudentNoticeStore.forAdmin(directory)
    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero("Notifikasi", "Informasi terbaru panel VOKASI", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                        .padding(top = 198.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy) }
                        Text("Notifikasi lokal perangkat",
                            color = Subtle, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Status Pendaftaran", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text(
                    if (unregistered > 0) "$unregistered siswa belum mendaftarkan akun"
                    else "Tidak ada siswa tertunda dalam daftar sekolah.",
                    color = if (unregistered > 0) Bad else Good,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                Text("Notifikasi berisi kondisi yang perlu ditindaklanjuti, bukan riwayat aktivitas.",
                    color = Subtle, fontSize = 12.sp)
                OutlinedButton(
                    onClick = onSchool,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Buka Manajemen Sekolah", color = Navy, fontSize = 12.sp) }
            }
        }
        items(messages.size) { index ->
            val message = messages[index]
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text(message.title, color = Ink, fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text(message.description, color = Subtle, fontSize = 12.sp)
            }
        }
        if (messages.isEmpty()) item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Tidak ada pemberitahuan yang memerlukan tindakan saat ini.",
                    color = Good, fontSize = 12.sp)
            }
        }
        item {
            Text("Notifikasi ini belum disinkronkan antarperangkat.",
                color = Subtle, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp))
        }
    }
}
