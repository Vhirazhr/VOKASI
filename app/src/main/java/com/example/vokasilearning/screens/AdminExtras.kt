package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.AdminPagination
import com.example.vokasilearning.ui.*

@Composable
internal fun AdminLogScreen(events: List<AdminLogEvent>, onBack: () -> Unit) {
    var type by remember { mutableStateOf("Semua") }
    var page by remember(type) { mutableIntStateOf(0) }
    val types = listOf("Semua", "Masuk", "Nilai", "Soal", "Laporan", "Profil")
    val selected = events.filter { type == "Semua" || it.type == type }
        .sortedByDescending { it.timestamp }
    val pageSize = AdminPagination.PAGE_SIZE
    val pages = AdminPagination.count(selected.size, pageSize)
    val current = AdminPagination.active(page, selected.size, pageSize)

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box {
                AdminHero("Log Aktivitas", "Catatan tindakan admin di perangkat ini", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        TextButton(onClick = onBack) { Text("‹ Pengaturan", color = Navy) }
                        Text("${events.size} peristiwa tersimpan • Maksimal 500 terbaru",
                            fontSize = 12.sp, color = Subtle)
                        Text("Log lokal belum mencatat tindakan admin di perangkat lain.",
                            fontSize = 11.sp, color = Subtle)
                    }
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminSectionTitle("Riwayat", "Aktivitas Admin")
                AdminSelect("Jenis aktivitas", type, types, { type = it }, Modifier.fillMaxWidth())
            }
        }
        if (selected.isEmpty()) {
            item {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text("Belum ada aktivitas untuk filter ini.", color = Subtle, fontSize = 12.sp)
                }
            }
        } else {
            items(selected.drop(current * pageSize).take(pageSize), key = { "${it.timestamp}_${it.type}_${it.detail}" }) { event ->
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row {
                        Text(event.type.uppercase(), color = NavyLight, fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Text(LocalAdminLog.date(event.timestamp), color = Subtle, fontSize = 10.sp)
                    }
                    Text(event.detail, color = Ink, fontSize = 13.sp)
                }
            }
        }
        item {
            AdminPaginationFooter(
                label = "Aktivitas",
                total = selected.size,
                page = current,
                onPrevious = { page = current - 1 },
                onNext = { page = current + 1 },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
internal fun AdminProfileScreen(
    config: AdminPreferences,
    onSave: (AdminPreferences) -> Boolean,
    onBack: () -> Unit,
    onLog: () -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(config) { mutableStateOf(config.displayName) }
    var school by remember(config) { mutableStateOf(config.school) }
    var result by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box {
                AdminHero("Pengaturan Panel", "Konfigurasi dan identitas admin", compact = true)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 198.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        TextButton(onClick = onBack) { Text("‹ Dashboard", color = Navy) }
                        Text("ADMIN PANEL", color = NavyLight, fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text(config.displayName, color = Ink, fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text(config.school, color = Subtle, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Pengaturan tampilan", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                OutlinedButton(
                    onClick = onLog,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Lihat Log Aktivitas", color = Navy, fontSize = 12.sp) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; result = null },
                    label = { Text("Nama tampilan admin") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it; result = null },
                    label = { Text("Nama sekolah") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Ini hanya nama tampilan lokal. Tidak mengganti username, sandi, atau hak akses.",
                    color = Subtle, fontSize = 11.sp)
                if (result != null) {
                    Text(result.orEmpty(), color = if (result == "Profil tersimpan.") Good else Bad,
                        fontSize = 12.sp)
                }
                VokasiButton(
                    text = "Simpan Profil",
                    enabled = name.isNotBlank() && school.isNotBlank(),
                    onClick = {
                        result = if (onSave(AdminPreferences(name.trim(), school.trim())))
                            "Profil tersimpan." else "Gagal menyimpan profil."
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            TextButton(onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth()) { Text("Keluar Akun", color = Bad) }
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar dari akun admin?") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Keluar", color = Bad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Batal", color = Navy) }
            }
        )
    }
}
