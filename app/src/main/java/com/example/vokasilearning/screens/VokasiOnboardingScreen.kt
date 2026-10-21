package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.vokasilearning.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.ui.*

/** Three educational onboarding pages shown only on first launch. */
@Composable
internal fun VokasiOnboardingScreen(onFinish: () -> Unit) {
    val headings = listOf("Selamat datang di VOKASI", "Numerasi untuk Masa Depan", "Belajar Aman dan Terukur")
    val bodies = listOf(
        "Bangun kebiasaan belajar mandiri dengan latihan matematika yang jelas, bertahap, dan menyenangkan.",
        "Latih penjumlahan, pengurangan, perkalian, dan pembagian. Pahami jawaban melalui umpan balik setelah latihan.",
        "Gunakan identitas yang sesuai daftar sekolah. Pantau kemajuan, hargai proses belajar, dan jaga kerahasiaan akunmu."
    )
    val captions = listOf("BERKARAKTER", "BERNALAR KRITIS", "BERTANGGUNG JAWAB")
    var index by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, Navy, Color(0xFF245080))))
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.vokasi_logo),
            contentDescription = "Logo VOKASI", modifier = Modifier.size(92.dp))
        Spacer(Modifier.height(18.dp))
        Text("VOKASI", color = Color.White, fontSize = 35.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("L E A R N I N G", color = Color(0xFFC4D9ED), fontSize = 11.sp)
        Spacer(Modifier.height(55.dp))
        Box(
            Modifier.size(110.dp)
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (index == 0) "01" else if (index == 1) "02" else "03",
                color = Color(0xFFFFBF37), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(24.dp))
        Text(captions[index], color = Color(0xFFFFC951),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(14.dp))
        Text(headings[index], color = Color.White, fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(13.dp))
        Text(bodies[index], color = Color(0xFFE7F0F9), fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(width = if (i == index) 30.dp else 9.dp, height = 9.dp)
                    .background(if (i == index) Color(0xFFFFC951) else Color.White.copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp)))
            }
        }
        Spacer(Modifier.height(25.dp))
        Button(onClick = {
            if (index < 2) index++ else onFinish()
        }, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
            Text(if (index == 2) "Mulai VOKASI" else "Lanjut",
                color = DeepNavy, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onFinish, enabled = index < 2) {
            Text(if (index == 2) " " else "Lewati pengenalan", color = Color(0xFFCEDCF0))
        }
    }
}
