package com.example.vokasilearning.ui

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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
internal fun VokasiBottomNav(
    items: List<Triple<Page, String, String>>,
    currentPage: Page,
    onNavigate: (Page) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .padding(horizontal = if (items.size >= 5) 8.dp else 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (target, label, _) ->
                val active = currentPage == target
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { onNavigate(target) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier.size(width = 44.dp, height = 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) PaleBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        MonochromeNavIcon(target = target, active = active)
                    }
                    Text(
                        label, fontSize = if (items.size >= 5) 9.sp else 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Navy else Subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * One coherent outline icon family, including admin tabs. No emoji/color glyphs
 * that vary by device font or default system palette.
 */
@Composable
private fun MonochromeNavIcon(target: Page, active: Boolean) {
    val tint = if (active) Navy else Subtle
    Canvas(Modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        val path = Path()
        fun move(x: Float, y: Float) { path.moveTo(x * scale, y * scale) }
        fun line(x: Float, y: Float) { path.lineTo(x * scale, y * scale) }

        when (target) {
            Page.HOME, Page.ADMIN_HOME -> {
                move(3f, 11f); line(12f, 4f); line(21f, 11f)
                move(6f, 10f); line(6f, 20f); line(18f, 20f); line(18f, 10f)
                move(10f, 20f); line(10f, 14f); line(14f, 14f); line(14f, 20f)
            }
            Page.LEADERBOARD -> {
                move(6f, 4f); line(18f, 4f); line(17f, 11f)
                line(14f, 14f); line(10f, 14f); line(7f, 11f); line(6f, 4f)
                move(6f, 6f); line(3f, 6f); line(3f, 9f); line(7f, 12f)
                move(18f, 6f); line(21f, 6f); line(21f, 9f); line(17f, 12f)
                move(12f, 14f); line(12f, 19f)
                move(8f, 20f); line(16f, 20f)
            }
            Page.PROFILE, Page.ADMIN_STUDENTS -> {
                move(12f, 3f); line(13f, 3f)
                move(6f, 21f); line(6f, 18f); line(9f, 15f)
                line(15f, 15f); line(18f, 18f); line(18f, 21f); line(6f, 21f)
            }
            Page.ADMIN_PROFILE -> {
                move(9f, 3f); line(10f, 5f); line(14f, 5f); line(15f, 3f)
                line(19f, 6f); line(18f, 9f); line(20f, 11f); line(20f, 13f)
                line(18f, 15f); line(19f, 18f); line(15f, 21f)
                line(14f, 19f); line(10f, 19f); line(9f, 21f)
                line(5f, 18f); line(6f, 15f); line(4f, 13f); line(4f, 11f)
                line(6f, 9f); line(5f, 6f); line(9f, 3f)
            }
            Page.ASSISTANT -> {
                move(4f, 5f); line(20f, 5f); line(20f, 17f)
                line(12f, 17f); line(8f, 21f); line(8f, 17f)
                line(4f, 17f); line(4f, 5f)
                move(8f, 10f); line(16f, 10f)
                move(8f, 13f); line(14f, 13f)
            }
            Page.ADMIN_SCHOOL -> {
                move(3f, 21f); line(21f, 21f)
                move(5f, 21f); line(5f, 9f); line(12f, 4f)
                line(19f, 9f); line(19f, 21f)
                move(9f, 21f); line(9f, 15f); line(15f, 15f); line(15f, 21f)
                move(9f, 11f); line(15f, 11f)
            }
            Page.ADMIN_QUESTIONS -> {
                move(5f, 3f); line(18f, 3f); line(18f, 21f); line(5f, 21f); line(5f, 3f)
                move(8f, 8f); line(15f, 8f)
                move(8f, 12f); line(15f, 12f)
                move(8f, 16f); line(13f, 16f)
            }
            Page.ADMIN_MONITOR -> {
                move(3f, 21f); line(21f, 21f)
                move(5f, 18f); line(5f, 12f); line(9f, 12f); line(9f, 18f)
                move(11f, 18f); line(11f, 7f); line(15f, 7f); line(15f, 18f)
                move(17f, 18f); line(17f, 4f); line(21f, 4f); line(21f, 18f)
            }
            else -> { move(4f, 12f); line(20f, 12f) }
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        if (target == Page.ADMIN_PROFILE) {
            drawCircle(
                color = tint, radius = 3.5f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        if (target == Page.PROFILE || target == Page.ADMIN_STUDENTS) {
            drawCircle(
                color = tint, radius = 4.3f * scale,
                center = Offset(12f * scale, 8f * scale),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
internal fun VokasiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "ButtonScale"
    )

    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(52.dp).scale(scale),
            shape = RoundedCornerShape(14.dp),
            border = borderStrokeOrDefault(enabled),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(52.dp).scale(scale)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(if (enabled) listOf(Navy, Color(0xFF345F93))
                    else listOf(Color(0xFFE3E8EF), Color(0xFFE3E8EF)))),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.18f),
                disabledContentColor = Subtle
            )
        ) {
            Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun borderStrokeOrDefault(enabled: Boolean) =
    if (enabled) BorderStroke(1.dp, Navy.copy(alpha = 0.5f)) else BorderStroke(1.dp, Border)

@Composable
internal fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Ink,
    prefix: String = "",
    suffix: String = ""
) {
    val animatedValue by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "Counter"
    )
    Text("$prefix$animatedValue$suffix", style = textStyle, color = color, modifier = modifier)
}

@Composable
internal fun StaggeredSlideIn(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L) // 60ms delay per item
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        content()
    }
}

@Composable
internal fun VokasiTitle(text: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, letterSpacing = (-0.5).sp)
        if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Subtle, lineHeight = 18.sp)
    }
}

@Composable
internal fun Pill(text: String, color: Color = PaleBlue, textColor: Color = Navy) {
    Box(Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
internal fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun Avatar(student: Student?, size: Dp = 48.dp) {
    val mod = Modifier.size(size).clip(CircleShape).border(2.dp, Color.White, CircleShape)
    if (student?.photo != null) {
        Image(student.photo, contentDescription = "Foto profil", modifier = mod, contentScale = ContentScale.Crop)
    } else {
        Box(mod.background(Brush.linearGradient(listOf(NavyLight, Navy))), contentAlignment = Alignment.Center) {
            Text(student?.name?.split(" ")?.take(2)?.mapNotNull { it.firstOrNull()?.uppercase() }?.joinToString("") ?: "?",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 3.2f).sp)
        }
    }
}

// --- SCREENS ---
