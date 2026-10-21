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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.logic.schoolGrade
import com.example.vokasilearning.ui.*

/**
 * A student may claim only an existing, unregistered admin roster entry.
 * Name search is a selector, not a free-text registration field.
 */
@Composable
internal fun RegisterScreen(
    onBack: () -> Unit,
    availableRoster: List<SchoolRosterEntry>,
    onRegister: (String, String, String, Classroom, Int, androidx.compose.ui.graphics.ImageBitmap?) -> Boolean
) {
    var nameSearch by remember { mutableStateOf("") }
    var selectedRosterId by remember { mutableStateOf<Long?>(null) }
    var nameMenu by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val pending = availableRoster.filter { !it.registered }
    val selected = pending.firstOrNull { it.id == selectedRosterId }
    val query = nameSearch.trim()
    val matches = if (query.length < 2) emptyList() else pending.filter {
        it.fullName.contains(query, ignoreCase = true)
    }.sortedWith(compareBy<SchoolRosterEntry> { it.fullName }
        .thenBy { it.classLabel }.thenBy { it.admissionYear }).take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item { AuthHero("Daftar Siswa", "Cari namamu di daftar resmi sekolah.") }
        item {
            Column(
                Modifier.fillMaxWidth().offset(y = (-18).dp)
                    .background(Color.White, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("‹ Kembali ke Login", color = Navy, fontSize = 12.sp)
                }
                Text("Nama lengkap sesuai data sekolah", color = Ink,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nameSearch,
                        onValueChange = {
                            nameSearch = it
                            selectedRosterId = null
                            error = false
                            nameMenu = it.trim().length >= 2
                        },
                        placeholder = { Text("Ketik minimal 2 huruf namamu") },
                        trailingIcon = { Text(if (selected != null) "✓" else "⌄", color = Navy) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        colors = authFieldColors()
                    )
                    DropdownMenu(
                        expanded = nameMenu && selected == null,
                        onDismissRequest = { nameMenu = false },
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        if (matches.isEmpty()) {
                            DropdownMenuItem(
                                enabled = false,
                                text = { Text("Nama belum ditemukan. Hubungi admin.", fontSize = 12.sp) },
                                onClick = {}
                            )
                        } else matches.forEach { entry ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(entry.fullName, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, color = Ink)
                                        Text(entry.major + " • " + entry.classLabel +
                                            " • Angkatan " + entry.admissionYear,
                                            fontSize = 11.sp, color = Subtle)
                                    }
                                },
                                onClick = {
                                    selectedRosterId = entry.id
                                    nameSearch = entry.fullName
                                    nameMenu = false
                                    error = false
                                }
                            )
                        }
                    }
                }
                if (selected != null) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(PaleGreen, RoundedCornerShape(13.dp))
                            .padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("✓ Identitas ditemukan", color = Good,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Jurusan: " + selected.major, color = Ink, fontSize = 12.sp)
                        Text("Tingkat: " + schoolGrade(selected.classLabel) +
                            "  •  Kelas: " + selected.classLabel, color = Ink, fontSize = 12.sp)
                        Text("Angkatan: " + selected.admissionYear,
                            color = Ink, fontSize = 12.sp)
                        Text("Informasi sekolah terisi otomatis dan tidak perlu diketik.",
                            color = Subtle, fontSize = 11.sp)
                    }
                } else {
                    Text("Pilih nama dari daftar yang muncul agar identitas kelas terisi otomatis.",
                        color = Subtle, fontSize = 11.sp)
                }

                HorizontalDivider(color = Border)
                Text("Buat username", color = Ink,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim(); error = false },
                    placeholder = { Text("Minimal 3 karakter") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(13.dp),
                    colors = authFieldColors()
                )
                Text("Buat password", color = Ink,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = false },
                    placeholder = { Text("Minimal 6 karakter") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            PasswordEyeIcon(passwordVisible)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(13.dp),
                    colors = authFieldColors()
                )
                if (pending.isEmpty()) {
                    Text("Belum ada daftar siswa dari admin pada perangkat ini.",
                        color = Bad, fontSize = 12.sp)
                }
                if (error) {
                    Text("Pendaftaran belum berhasil. Username mungkin sudah dipakai atau data berubah. Coba lagi atau hubungi admin.",
                        color = Bad, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                VokasiButton(
                    text = "Daftar Akun",
                    onClick = {
                        selected?.let { roster ->
                            error = !onRegister(
                                roster.fullName, username.trim(), password,
                                Classroom(roster.classLabel, roster.major),
                                roster.admissionYear, null
                            )
                        }
                    },
                    enabled = selected != null &&
                        username.matches(Regex("[A-Za-z0-9._]{3,}")) &&
                        password.length >= 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Status pendaftaran dan username akan tercatat di panel admin. Password tidak ditampilkan kepada admin.",
                    color = Subtle, fontSize = 11.sp)
            }
        }
    }
}
