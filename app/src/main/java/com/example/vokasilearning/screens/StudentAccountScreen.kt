package com.example.vokasilearning.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*
import kotlinx.coroutines.launch

@Composable
internal fun StudentAccountScreen(
    me: Student,
    onSave: suspend (String, String, String) -> String?,
    onPhoto: (android.graphics.Bitmap) -> String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accountScope = rememberCoroutineScope()
    var passwordBusy by remember { mutableStateOf(false) }
    var passwordUpdated by remember { mutableStateOf(false) }
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var photoBusy by remember { mutableStateOf(false) }
    val choosePhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !photoBusy) {
            photoBusy = true
            notice = try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (bitmap == null) "Foto tidak dapat dibuka."
                else onPhoto(bitmap) ?: "Foto profil berhasil diperbarui."
            } catch (_: Exception) { "Gagal menyimpan foto profil." }
            photoBusy = false
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { StudentScreenHeader("Pengaturan Akun", "Keamanan akun dan foto profilmu.") }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                TextButton(onClick = onBack) { Text("‹ Kembali", color = Navy) }
                Text("Identitas Sekolah", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(me, 61.dp)
                    Column {
                        Text(me.name, color = Ink, fontWeight = FontWeight.Bold)
                        Text(me.classroom.major + " • " + me.classroom.label,
                            color = Subtle, fontSize = 12.sp)
                        Text("Angkatan " + me.admissionYear, color = Subtle, fontSize = 11.sp)
                    }
                }
                Text("Nama lengkap, jurusan, angkatan, dan kelas hanya bisa diubah admin sekolah.",
                    color = Subtle, fontSize = 11.sp)
                OutlinedButton(onClick = { choosePhoto.launch("image/*") },
                    enabled = !photoBusy, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Ganti Foto Profil", color = Navy)
                }
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Keamanan Akun", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("Login siswa menggunakan nama lengkap yang terdaftar di sekolah. Identitas akun dikelola admin.",
                    color = Subtle, fontSize = 12.sp)
                OutlinedTextField(
                    value = oldPass, onValueChange = { oldPass = it; notice = "" },
                    label = { Text("Password saat ini") },
                    visualTransformation = if (reveal) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it; notice = "" },
                    label = { Text("Password baru (minimal 8 karakter)") },
                    visualTransformation = if (reveal) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (newPass.isNotBlank()) {
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it; notice = "" },
                        label = { Text("Ulangi password baru") },
                        visualTransformation = if (reveal) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Row {
                    Checkbox(checked = reveal, onCheckedChange = { reveal = it })
                    Text("Tampilkan password", modifier = Modifier.padding(top = 12.dp),
                        color = Subtle, fontSize = 12.sp)
                }
                VokasiButton(
                    text = if (passwordBusy) "Memperbarui..." else "Simpan Password Baru",
                    enabled = !passwordBusy && oldPass.isNotBlank() &&
                        newPass.length >= 8 && confirm == newPass,
                    onClick = {
                        accountScope.launch {
                            passwordBusy = true
                            notice = ""
                            val error = try {
                                onSave(me.username, oldPass, newPass)
                            } catch (_: Exception) {
                                "Password tidak berhasil diperbarui."
                            }
                            notice = error.orEmpty()
                            if (error == null) {
                                oldPass = ""; newPass = ""; confirm = ""
                                passwordUpdated = true
                            }
                            passwordBusy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()
                )
                if (notice.isNotBlank()) Text(notice, color = if (notice.contains("berhasil")) Good else Bad,
                    fontSize = 12.sp)

            }
        }
    }
    if (passwordUpdated) {
        ActionFeedbackDialog(
            title = "Password diperbarui",
            message = "Password baru sudah aktif. Gunakan saat masuk berikutnya.",
            onDismiss = { passwordUpdated = false }
        )
    }
}
