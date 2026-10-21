package com.example.vokasilearning.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vokasilearning.R

/**
 * Shared official VOKASI logo for student, admin, auth and onboarding.
 * No navy wrapper or nested padding: the rounded logo fills its entire slot.
 */
@Composable
internal fun VokasiBrandMark(
    size: Dp = 42.dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.vokasi_logo),
        contentDescription = "Logo VOKASI",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
    )
}
