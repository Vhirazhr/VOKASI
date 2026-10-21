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
import com.example.vokasilearning.data.LocalAdminLog
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
    val latest = events.sortedByDescending { it.timestamp }.take(5)
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
                Text("Buka Manajemen Sekolah untuk memeriksa nama dan kelasnya.",
                    color = Subtle, fontSize = 12.sp)
                OutlinedButton(
                    onClick = onSchool,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Buka Manajemen Sekolah", color = Navy, fontSize = 12.sp) }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Aktivitas Terbaru", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                if (latest.isEmpty()) {
                    Text("Belum ada aktivitas tercatat.", color = Subtle, fontSize = 12.sp)
                } else latest.forEach { event ->
                    HorizontalDivider(color = Border)
                    Text(event.type + " • " + LocalAdminLog.date(event.timestamp),
                        color = NavyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(event.detail, color = Ink, fontSize = 12.sp)
                }
            }
        }
        item {
            Text("Notifikasi ini belum disinkronkan antarperangkat.",
                color = Subtle, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp))
        }
    }
}
