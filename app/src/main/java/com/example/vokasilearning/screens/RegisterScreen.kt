package com.example.vokasilearning.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.demoClasses
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.ui.*

@Composable
internal fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String, Classroom, ImageBitmap?) -> Boolean
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedMajor by remember { mutableStateOf(demoClasses.first().major) }
    var selectedClass by remember { mutableStateOf(demoClasses.first()) }
    var majorMenu by remember { mutableStateOf(false) }
    var classMenu by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var duplicate by remember { mutableStateOf(false) }

    val majors = remember { demoClasses.map { it.major }.distinct().sorted() }
    val availableClasses = demoClasses.filter { it.major == selectedMajor }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap != null) image = bitmap.asImageBitmap()
            } catch (_: Exception) { }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item { AuthHero("Daftar Siswa", "Lengkapi akun dan identitas kelasmu.") }
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
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (image == null) Avatar(null, 62.dp)
                    else Image(
                        image!!, contentDescription = "Foto profil",
                        modifier = Modifier.size(62.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text("Foto profil", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = { photoPicker.launch("image/*") },
                            contentPadding = PaddingValues(0.dp)) {
                            Text(if (image == null) "Tambah foto (opsional)" else "Ganti foto",
                                color = Navy, fontSize = 12.sp)
                        }
                    }
                }
                Text("Nama lengkap", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it; duplicate = false },
                    placeholder = { Text("Nama sesuai identitas") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(13.dp), colors = authFieldColors()
                )
                Text("Username", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim(); duplicate = false },
                    placeholder = { Text("Minimal 3 karakter") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(13.dp), colors = authFieldColors()
                )
                Text("Password", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = password, onValueChange = { password = it; duplicate = false },
                    placeholder = { Text("Minimal 6 karakter") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            PasswordEyeIcon(passwordVisible)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(13.dp), colors = authFieldColors()
                )
                HorizontalDivider(color = Border)
                Text("Jurusan", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { majorMenu = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp)
                    ) {
                        Text(selectedMajor, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                        Text("⌄", color = Navy)
                    }
                    DropdownMenu(
                        expanded = majorMenu,
                        onDismissRequest = { majorMenu = false }
                    ) {
                        majors.forEach { major ->
                            DropdownMenuItem(text = { Text(major) }, onClick = {
                                selectedMajor = major
                                selectedClass = demoClasses.first { it.major == major }
                                majorMenu = false
                            })
                        }
                    }
                }
                Text("Kelas", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { classMenu = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp)
                    ) {
                        Text(selectedClass.label, modifier = Modifier.weight(1f), color = Ink)
                        Text("⌄", color = Navy)
                    }
                    DropdownMenu(
                        expanded = classMenu,
                        onDismissRequest = { classMenu = false }
                    ) {
                        availableClasses.forEach { classroom ->
                            DropdownMenuItem(
                                text = { Text(classroom.label) },
                                onClick = { selectedClass = classroom; classMenu = false }
                            )
                        }
                    }
                }
                if (duplicate) Text("Username sudah digunakan.", color = Bad, fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                VokasiButton(
                    text = "Daftar",
                    onClick = { duplicate = !onRegister(name.trim(), username.trim(), password, selectedClass, image) },
                    enabled = name.isNotBlank() &&
                        username.matches(Regex("[A-Za-z0-9._]{3,}")) &&
                        password.length >= 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
