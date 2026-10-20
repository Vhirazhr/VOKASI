package com.example.vokasilearning.screens

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
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
    var joinCode by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(demoClasses.first()) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var openClasses by remember { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf(false) }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val bitmap = context.contentResolver.openInputStream(it)?.use { stream -> BitmapFactory.decodeStream(stream) }
                if (bitmap != null) image = bitmap.asImageBitmap()
            } catch (_: Exception) { }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Kembali ke login", color = Navy) } }
        item { VokasiTitle("Daftar siswa", "Isi identitas singkat untuk mengatur kelas dan peringkatmu.") }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (image == null) Avatar(null, 72.dp) else Image(image!!, null, Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Column {
                    Text("Foto profil (opsional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    TextButton(onClick = { chooser.launch("image/*") }) { Text("Pilih dari galeri", color = Navy) }
                }
            }
        }

        item { OutlinedTextField(name, { name = it }, label = { Text("Nama lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(username, { username = it.trim() }, label = { Text("Username") }, supportingText = { Text("Tanpa spasi, minimal 3 karakter") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(password, { password = it }, label = { Text("Password (min. 6 karakter)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        item {
            Text("Jurusan & kelas (contoh)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Box {
                OutlinedButton(onClick = { openClasses = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("${chosen.label} • ${chosen.major}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                }
                DropdownMenu(expanded = openClasses, onDismissRequest = { openClasses = false }) {
                    demoClasses.forEach { classroom ->
                        DropdownMenuItem(text = { Text("${classroom.label} — ${classroom.major}") }, onClick = { chosen = classroom; openClasses = false })
                    }
                }
            }
        }
        item { OutlinedTextField(joinCode, { joinCode = it }, label = { Text("Kode kelas (opsional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        if (duplicate) item { Text("Username sudah dipakai.", color = Bad, fontSize = 13.sp) }

        item {
            VokasiButton(
                text = "Buat Akun & Masuk",
                onClick = { duplicate = !onRegister(name.trim(), username.trim(), password, chosen, image) },
                enabled = name.trim().isNotEmpty() && username.matches(Regex("[A-Za-z0-9._]{3,}")) && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
