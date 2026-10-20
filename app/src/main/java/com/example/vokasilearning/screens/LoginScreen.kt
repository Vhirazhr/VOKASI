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
    var isPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(Modifier.height(20.dp)) }

        // Premium Header
        item {
            Box(
                Modifier.fillMaxWidth().height(180.dp).shadow(16.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("◈", fontSize = 40.sp, color = Color.White)
                    Text("VOKASI LEARNING", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 1.sp)
                    Text("SMK Negeri 1 Singosari", color = Color(0xFFB0C4DE), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { VokasiTitle("Selamat datang", "Masuk untuk mulai latihan soal harian.") }

        // Role Selector
        item {
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Siswa", true to "Admin").forEach { (isAdmin, label) ->
                    val selected = admin == isAdmin
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Navy else Color.Transparent)
                            .clickable { admin = isAdmin; error = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) Color.White else Subtle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                username, { username = it; error = false },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy)
            )
        }
        item {
            OutlinedTextField(
                password, { password = it; error = false },
                label = { Text("Password") },
                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "Sembunyi" else "Lihat", fontSize = 12.sp, color = Navy)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy)
            )
        }

        if (error) item { Text("Username atau password tidak cocok.", color = Bad, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

        item {
            VokasiButton(
                text = "Masuk",
                onClick = { error = !(if (admin) onAdminLogin(username, password) else onStudentLogin(username, password)) },
                enabled = username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!admin) item {
            TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                Text("Belum punya akun? Daftar sebagai siswa", color = Navy, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            WhiteCard {
                Text("AKUN DEMO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Siswa: siswa.demo / 123456", fontSize = 13.sp, color = Subtle)
                Text("Admin: admin.demo / admin123", fontSize = 13.sp, color = Subtle)
                Spacer(Modifier.height(4.dp))
                Text("Prototype lokal: data akan reset saat aplikasi ditutup.", fontSize = 11.sp, color = Subtle)
            }
        }
    }
}

