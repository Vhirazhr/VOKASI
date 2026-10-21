package com.example.vokasilearning.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.vokasilearning.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual identity for login and registration. */
@Composable
internal fun AuthHero(title: String, subtitle: String) {
    Box(
        Modifier.fillMaxWidth().height(255.dp)
            .clip(RoundedCornerShape(bottomStart = 35.dp, bottomEnd = 35.dp))
            .background(Brush.linearGradient(listOf(DeepNavy, Navy, Color(0xFF294F7F))))
            .padding(23.dp)
    ) {
        Box(
            Modifier.size(200.dp).align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-95).dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )
        Row(
            Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VokasiBrandMark(size = 42.dp, cornerRadius = 13.dp)
            Column {
                Text("VOKASI", color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(bottom = 15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = Color.White, fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Color(0xFFCBD7E5), fontSize = 12.sp)
        }
    }
}

@Composable
internal fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NavyLight,
    unfocusedBorderColor = Border,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFFAFBFD)
)

/** Consistent vector eye and crossed-out eye for password visibility. */
@Composable
internal fun PasswordEyeIcon(visible: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val outline = Path().apply {
            moveTo(2f * sx, 12f * sy)
            cubicTo(6f * sx, 5f * sy, 18f * sx, 5f * sy, 22f * sx, 12f * sy)
            cubicTo(18f * sx, 19f * sy, 6f * sx, 19f * sy, 2f * sx, 12f * sy)
            close()
        }
        drawPath(outline, Navy, style = Stroke(width = 1.8.dp.toPx()))
        drawCircle(Navy, radius = 3f * sx, center = Offset(12f * sx, 12f * sy),
            style = Stroke(width = 1.8.dp.toPx()))
        if (!visible) {
            drawLine(
                color = Navy,
                start = Offset(3f * sx, 21f * sy),
                end = Offset(21f * sx, 3f * sy),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
