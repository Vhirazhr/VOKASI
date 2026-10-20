package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.ui.*

@Composable
internal fun LoginScreen(
    onLogin: (String, String) -> Boolean,
    onRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item { AuthHero("Selamat datang", "Masuk untuk melanjutkan latihanmu.") }
        item {
            Column(
                Modifier.fillMaxWidth().offset(y = (-18).dp)
                    .background(Color.White, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .padding(horizontal = 22.dp, vertical = 25.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text("Masuk ke VOKASI", fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("Username", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim(); error = false },
                    placeholder = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = authFieldColors()
                )
                Text("Password", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = false },
                    placeholder = { Text("Password") },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            PasswordEyeIcon(visible)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = authFieldColors()
                )
                if (error) Text("Username atau password tidak cocok.", color = Bad, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                VokasiButton(
                    text = "Masuk",
                    onClick = { error = !onLogin(username, password) },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Belum punya akun?", color = Subtle, fontSize = 12.sp)
                    TextButton(onClick = onRegister) {
                        Text("Daftar", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
