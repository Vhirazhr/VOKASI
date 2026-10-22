package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var search by remember { mutableStateOf("") }
    var page by remember(type) { mutableIntStateOf(0) }
    val types = listOf("Semua", "Masuk", "Nilai", "Soal", "Laporan", "Profil", "Sekolah")
    val selected = events.filter { (type == "Semua" || it.type == type) &&
        (search.isBlank() || it.detail.contains(search.trim(), ignoreCase = true)) }
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
                AdminSectionTitle("Audit operasional", "Riwayat Aktivitas")
                OutlinedTextField(
                    value = search, onValueChange = { search = it; page = 0 },
                    placeholder = { Text("Cari tindakan admin") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                AdminSelect("Jenis aktivitas", type, types, { type = it }, Modifier.fillMaxWidth())
                Text("10 aktivitas per halaman • Diurutkan dari yang terbaru",
                    color = Subtle, fontSize = 11.sp)
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
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("●  " + event.type.uppercase(),
                            color = when (event.type) {
                                "Sekolah" -> Good
                                "Masuk" -> Navy
                                "Soal" -> NavyLight
                                else -> Ink
                            }, fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f))
                        Text(LocalAdminLog.date(event.timestamp), color = Subtle, fontSize = 10.sp)
                    }
                    HorizontalDivider(color = Border)
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
    var profileSaved by remember { mutableStateOf(false) }

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
                Text("Profil & Tampilan Admin", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
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
                    Text(result.orEmpty(), color = Bad,
                        fontSize = 12.sp)
                }
                VokasiButton(
                    text = "Simpan Profil",
                    enabled = name.isNotBlank() && school.isNotBlank(),
                    onClick = {
                        if (onSave(AdminPreferences(name.trim(), school.trim()))) {
                            result = null
                            profileSaved = true
                        } else result = "Gagal menyimpan profil."
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onLog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Riwayat Aktivitas Admin", color = Navy, fontSize = 12.sp)
                }
            }
        }
        item {
            TextButton(onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth()) { Text("Keluar Akun", color = Bad) }
        }
    }
    if (profileSaved) {
        ActionFeedbackDialog(
            title = "Profil admin tersimpan",
            message = "Nama tampilan perangkat sudah diperbarui.",
            onDismiss = { profileSaved = false }
        )
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
