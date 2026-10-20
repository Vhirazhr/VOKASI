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
internal fun LoginScreen(
    onStudentLogin: (String, String) -> Boolean,
    onAdminLogin: (String, String) -> Boolean,
    onRegister: () -> Unit
) {
    var admin by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().height(320.dp)
                    .clip(RoundedCornerShape(bottomStart = 39.dp, bottomEnd = 39.dp))
                    .background(Brush.linearGradient(listOf(DeepNavy, Navy, Color(0xFF294F7F))))
                    .padding(23.dp)
            ) {
                Box(
                    Modifier.size(265.dp).align(Alignment.TopEnd)
                        .offset(x = 115.dp, y = (-148).dp)
                        .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape)
                )
                Row(
                    Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▣", color = Color.White, fontSize = 26.sp)
                    }
                    Column {
                        Text("VOKASI", color = Color.White, fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
                        Text("L E A R N I N G", color = Color.White.copy(alpha = 0.72f), fontSize = 8.sp)
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomStart).padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("RUANG LATIHAN DIGITAL", color = Color(0xFFCAD9ED),
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Tumbuh lewat latihan\nsetiap hari.", color = Color.White,
                        fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Latihan hitung dasar untuk siswa SMK Negeri 1 Singosari.",
                        color = Color(0xFFCBD7E5), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().offset(y = (-22).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(if (admin) "Login Admin" else "Selamat datang",
                    fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Masuk untuk melanjutkan progres latihanmu.",
                    color = Subtle, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))

                Text("Username", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = username, onValueChange = { username = it; error = false },
                    placeholder = { Text("Masukkan username") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyLight, unfocusedBorderColor = Border,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFBFD)
                    )
                )
                Text("Password", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = false },
                    placeholder = { Text("Masukkan password") },
                    visualTransformation = if (passwordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Tutup" else "Lihat",
                                fontSize = 12.sp, color = Navy)
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyLight, unfocusedBorderColor = Border,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFBFD)
                    )
                )
                if (error) {
                    Text("Username atau password tidak cocok.", color = Bad, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                VokasiButton(
                    text = "Masuk ke Vokasi  →",
                    onClick = {
                        error = !(if (admin) onAdminLogin(username, password)
                                  else onStudentLogin(username, password))
                    },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!admin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Belum punya akun?", color = Subtle, fontSize = 12.sp)
                        TextButton(onClick = onRegister) {
                            Text("Daftar siswa", color = Navy,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { admin = !admin; error = false },
                    modifier = Modifier.fillMaxWidth().height(47.dp),
                    border = BorderStroke(1.dp, Border),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                ) {
                    Text(
                        if (admin) "← Kembali ke Login Siswa" else "♢  Masuk sebagai Admin",
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
                Column(
                    Modifier.fillMaxWidth().background(Canvas, RoundedCornerShape(13.dp)).padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("AKUN DEMO", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Siswa: siswa.demo / 123456", color = Subtle, fontSize = 11.sp)
                    Text("Admin: admin.demo / admin123", color = Subtle, fontSize = 11.sp)
                }
                Text("Prototipe lokal, bukan data siswa sebenarnya.",
                    modifier = Modifier.fillMaxWidth(), color = Subtle,
                    fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
