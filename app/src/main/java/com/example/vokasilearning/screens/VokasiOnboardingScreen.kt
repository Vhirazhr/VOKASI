package com.example.vokasilearning.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.R
import com.example.vokasilearning.ui.DeepNavy
import com.example.vokasilearning.ui.Navy
import com.example.vokasilearning.ui.NavyLight
import com.example.vokasilearning.ui.Good
import com.example.vokasilearning.ui.Subtle
import com.example.vokasilearning.ui.VokasiBrandMark

// The three-page, first-launch Android onboarding. Other app screens are unchanged.
private val SplashBackground = Color(0xFFF5F7FB)
private val SplashOutline = Color(0xFFE1E9F2)
private val SplashOrange = Color(0xFFC47E26)

private data class SplashCopy(val eyebrow: String, val heading: String, val description: String)

private val splashCopy = listOf(
    SplashCopy(
        "BELAJAR LEBIH TERARAH",
        "Latihan singkat,\nprogres yang nyata.",
        "Latih kemampuan lewat soal pilihan ganda."
    ),
    SplashCopy(
        "PANTAU PERKEMBANGAN",
        "Setiap jawaban\nmembawamu maju.",
        "Lihat nilai, XP, level, dan streak di HP ini."
    ),
    SplashCopy(
        "TUMBUH BERSAMA",
        "Pantau progresmu\nsetiap hari.",
        "Ranking saat ini berasal dari riwayat di perangkat, belum lintas HP."
    )
)

@Composable
internal fun VokasiOnboardingScreen(onFinish: () -> Unit) {
    var slide by remember { mutableIntStateOf(0) }
    val copy = splashCopy[slide]

    Column(
        modifier = Modifier.fillMaxSize()
            .background(SplashBackground)
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(55.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Only the official lion/gear icon is shown here. No book icon/LEARNING text.
            VokasiBrandMark(size = 45.dp, cornerRadius = 14.dp)
            Spacer(Modifier.width(11.dp))
            Text(
                text = "VOKASI",
                color = DeepNavy,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.9.sp
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFinish, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Lewati", color = Subtle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Center illustrations resize to the real screen height without clipping small devices.
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val artworkSize = minOf(maxWidth - 4.dp, maxHeight - 6.dp, 350.dp)
            if (artworkSize > 0.dp) {
                OnboardingArtwork(slide = slide, dimension = artworkSize)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = copy.eyebrow,
                color = NavyLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.3.sp
            )
            Text(
                text = copy.heading,
                color = DeepNavy,
                fontSize = 29.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = copy.description,
                color = Subtle,
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(37.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = if (index == slide) 27.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (index == slide) Navy else Color(0xFFCCD5E1))
                    )
                }
            }
            Button(
                onClick = {
                    if (slide < splashCopy.lastIndex) slide++ else onFinish()
                },
                modifier = Modifier.widthIn(min = 165.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp)
            ) {
                Text(
                    if (slide == splashCopy.lastIndex) "Mulai Sekarang" else "Lanjutkan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.width(9.dp))
                Text("›", fontSize = 26.sp, color = Color.White)
            }
        }
    }
}

/** Illustration coordinates use a fixed 320dp design canvas, scaled proportionally. */
@Composable
private fun OnboardingArtwork(slide: Int, dimension: Dp) {
    BoxWithConstraints(modifier = Modifier.size(dimension), contentAlignment = Alignment.Center) {
        fun p(value: Float): Dp = maxWidth * (value / 320f)
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFE7EFF8), Color(0xFFF8FAFE))
                    ),
                    CircleShape
                )
        )
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFD4E0EF),
                radius = size.minDimension * 0.416f,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Box(
            Modifier.offset(x = p(-116f), y = p(-110f)).size(p(24f))
                .background(Color(0xFFE4F5F0), CircleShape)
        )
        Box(
            Modifier.offset(x = p(112f), y = p(98f)).size(p(13f))
                .background(Color(0xFFF4C991), CircleShape)
        )
        when (slide) {
            0 -> PracticeArtwork(::p)
            1 -> ProgressArtwork(::p)
            else -> CommunityArtwork(::p)
        }
    }
}

@Composable
private fun PracticeArtwork(p: (Float) -> Dp) {
    FloatingCard(
        x = p(48f), y = p(55f), width = p(224f), height = p(203f),
        padding = p(19f)
    ) {
        Text("LATIHAN HARI INI", color = NavyLight,
            fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(p(18f)))
        Box(Modifier.fillMaxWidth(0.98f).height(p(12f))
            .clip(RoundedCornerShape(8.dp)).background(Navy))
        Spacer(Modifier.height(p(9f)))
        Box(Modifier.fillMaxWidth(0.73f).height(p(9f))
            .clip(RoundedCornerShape(8.dp)).background(SplashOutline))
        Spacer(Modifier.height(p(17f)))
        Box(Modifier.fillMaxWidth().height(p(32f))
            .clip(RoundedCornerShape(11.dp)).background(Color(0xFFF8FAFD))
            .border(1.dp, SplashOutline, RoundedCornerShape(11.dp)))
        Spacer(Modifier.height(p(6f)))
        Box(Modifier.fillMaxWidth().height(p(33f))
            .clip(RoundedCornerShape(11.dp)).background(Good),
            contentAlignment = Alignment.Center) {
            Text("✓", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(p(6f)))
        Box(Modifier.fillMaxWidth().height(p(28f))
            .clip(RoundedCornerShape(11.dp)).background(Color(0xFFF8FAFD)))
    }
    FloatingBadge(x = p(244f), y = p(48f)) {
        Text("✦", color = SplashOrange, fontSize = 17.sp)
        Text("+20 XP", color = SplashOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
    FloatingBadge(x = p(12f), y = p(252f)) {
        Text("◷", color = NavyLight, fontSize = 21.sp)
        Text("12 menit", color = NavyLight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ProgressArtwork(p: (Float) -> Dp) {
    FloatingCard(x = p(52f), y = p(50f), width = p(217f),
        height = p(225f), padding = p(19f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PROGRES BELAJAR", color = NavyLight, fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f),
                letterSpacing = 0.6.sp)
            Text("▥", fontSize = 18.sp, color = NavyLight)
        }
        Spacer(Modifier.height(p(13f)))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(p(112f)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val thickness = 9.dp.toPx()
                    val inset = thickness / 2 + 2.dp.toPx()
                    val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                    drawArc(Color(0xFFE4F0ED), -90f, 360f, false,
                        Offset(inset, inset), arcSize,
                        style = Stroke(thickness, cap = StrokeCap.Butt))
                    drawArc(Good, -90f, 302.4f, false,
                        Offset(inset, inset), arcSize,
                        style = Stroke(thickness, cap = StrokeCap.Butt))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("84", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                    Text("%", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(p(10f)))
        Text("Akurasi meningkat", color = DeepNavy, fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("Terus pertahankan ritmemu", color = Subtle, fontSize = 8.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    FloatingBadge(x = p(20f), y = p(50f), minWidth = p(62f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("12", color = SplashOrange, fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold)
            Text("LEVEL", color = SplashOrange, fontSize = 8.sp,
                fontWeight = FontWeight.Bold)
        }
    }
    FloatingBadge(x = p(251f), y = p(239f)) {
        Text("♨", color = Color(0xFFE47E37), fontSize = 16.sp)
        Text("12 hari", color = Color(0xFFE47E37), fontSize = 10.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CommunityArtwork(p: (Float) -> Dp) {
    FloatingCard(x = p(37f), y = p(56f), width = p(246f),
        height = p(206f), padding = p(17f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PERINGKAT KELAS", color = NavyLight, fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp)
            Spacer(Modifier.weight(1f))
            Text("✦", color = SplashOrange, fontSize = 16.sp)
        }
        Spacer(Modifier.height(p(11f)))
        Row(
            modifier = Modifier.fillMaxWidth().height(p(135f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            PodiumPerson("NP", 2, Color(0xFFCEF1E9), p)
            Spacer(Modifier.width(p(8f)))
            PodiumPerson("AP", 1, Color(0xFFFFD6C1), p)
            Spacer(Modifier.width(p(8f)))
            PodiumPerson("RM", 3, Color(0xFFD4E5FF), p)
        }
    }
    FloatingBadge(x = p(5f), y = p(251f)) {
        // No book icon in the onboarding brand or supporting labels.
        Text("SMKN 1 Singosari", color = NavyLight, fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold)
    }
    FloatingBadge(x = p(238f), y = p(47f)) {
        Text("↑", color = Good, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text("Naik 2 posisi", color = Good, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PodiumPerson(
    initials: String, rank: Int, avatarColor: Color, p: (Float) -> Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(if (rank == 1) p(52f) else p(43f))
                .border(3.dp, Color.White, CircleShape)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontSize = 11.sp, color = Navy,
                fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(p(7f)))
        Box(
            modifier = Modifier
                .width(p(58f)).height(if (rank == 1) p(73f) else p(50f))
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    if (rank == 1) Brush.verticalGradient(
                        listOf(NavyLight, Navy)
                    ) else Brush.verticalGradient(
                        listOf(Color(0xFFEDF2F7), Color(0xFFE8EFF5))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(rank.toString(), modifier = Modifier.padding(top = 13.dp),
                color = if (rank == 1) Color.White else Subtle, fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** A content card positioned using the reference illustration's coordinates. */
@Composable
private fun FloatingCard(
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    padding: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Surface(
            modifier = Modifier.offset(x = x, y = y).size(width = width, height = height),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 13.dp
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    }
}

@Composable
private fun FloatingBadge(
    x: Dp,
    y: Dp,
    minWidth: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Surface(
            modifier = Modifier.offset(x = x, y = y).widthIn(min = minWidth),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.heightIn(min = 42.dp).padding(horizontal = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
