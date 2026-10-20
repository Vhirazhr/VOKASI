package com.example.vokasilearning.ui.components

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
import com.example.vokasilearning.app.*
import com.example.vokasilearning.navigation.*
import com.example.vokasilearning.domain.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.features.auth.*
import com.example.vokasilearning.features.student.*
import com.example.vokasilearning.features.admin.*
import com.example.vokasilearning.ui.components.*
import com.example.vokasilearning.ui.theme.*
import com.example.vokasilearning.ui.adaptive.*
import com.example.vokasilearning.ui.security.*

@Composable
internal fun VokasiBottomNav(
    items: List<Triple<Page, String, String>>,
    currentPage: Page,
    onNavigate: (Page) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        items.forEach { (target, label, symbol) ->
            val selected = currentPage == target
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(target) },
                icon = {
                    Text(
                        symbol,
                        fontSize = 20.sp,
                        color = if (selected) Navy else Subtle
                    )
                },
                label = {
                    Text(
                        label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Navy else Subtle
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PaleBlue
                )
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
            modifier = modifier.height(52.dp).scale(scale),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
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
