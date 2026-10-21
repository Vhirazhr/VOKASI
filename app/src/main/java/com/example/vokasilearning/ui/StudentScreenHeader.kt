package com.example.vokasilearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.seasonTitle

/**
 * Header layar siswa yang memakai bahasa visual sama dengan StudentHome:
 * navy gradient, lengkung bawah, ornament lingkaran, label season.
 */
@Composable
internal fun StudentScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().height(187.dp)
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF294D7A))))
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Box(
            Modifier.size(180.dp).align(Alignment.TopEnd)
                .offset(x = 95.dp, y = 14.dp)
                .border(30.dp, Color.White.copy(alpha = 0.035f), CircleShape)
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VokasiBrandMark(size = 29.dp, cornerRadius = 8.dp)
            Text(
                "VOKASI",
                color = Color.White.copy(alpha = 0.87f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
        Text(
            seasonTitle(),
            modifier = Modifier.align(Alignment.TopEnd)
                .background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            color = Color(0xFFE0E9F4),
            fontSize = 10.sp
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = Color(0xFFD5E1F0),
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
internal fun StudentSectionHeading(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            eyebrow,
            color = NavyLight,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(title, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}
