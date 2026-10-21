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
internal fun QuizScreen(
    questions: List<Question>, index: Int, selectedIndex: Int?, seconds: Int, totalSeconds: Int,
    feedback: AnswerFeedback?,
    onPick: (Int) -> Unit, onSubmit: () -> Unit, onQuit: () -> Unit
) {
    if (questions.isEmpty() || index !in questions.indices) return
    val current = questions[index]

    Column(Modifier.fillMaxSize().background(Canvas)) {
        // Header
        Column(Modifier.fillMaxWidth().background(Navy).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onQuit) { Text("‹ Keluar", color = Color.White, fontWeight = FontWeight.Bold) }
                VokasiBrandMark(size = 28.dp, cornerRadius = 8.dp)
                Spacer(Modifier.weight(1f))
                // Animated timer color
                val timerColor by animateColorAsState(
                    targetValue = if (seconds <= 5) Bad else Color.White,
                    label = "TimerColor"
                )
                Pill("⏱ ${clockText(seconds)}", Color.White.copy(alpha = 0.15f), timerColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Soal ${index + 1} dari ${questions.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("Total sisa ${clockText(totalSeconds)}", color = Color(0xFFB0C4DE), fontSize = 12.sp)
            }
            // Smooth Progress Bar
            val progress by animateFloatAsState(
                targetValue = (index + 1f) / questions.size,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "ProgressBar"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color(0xFF92C7F3), trackColor = Color(0xFF506E91)
            )
        }

        // Animated Question Content
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { 100 }) + fadeIn()).togetherWith(
                    slideOutHorizontally(targetOffsetX = { -100 }) + fadeOut()
                )
            },
            modifier = Modifier.weight(1f),
            label = "QuestionTransition"
        ) { q ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Pill(q.category, PalePurple, Navy)

                WhiteCard(Modifier.fillMaxWidth()) {
                    Text(
                        q.question, color = Ink, fontSize = 22.sp, lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text("Pilih satu jawaban:", color = Subtle, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                q.options.forEachIndexed { optionIndex, text ->
                    val selected = (feedback?.selectedIndex ?: selectedIndex) == optionIndex
                    val bgTarget = when {
                        selected && feedback?.correct == true -> PaleGreen
                        selected && feedback?.correct == false -> Color(0xFFFFE8EB)
                        selected -> PaleBlue
                        else -> Color.White
                    }
                    val borderTarget = when {
                        selected && feedback?.correct == true -> Good
                        selected && feedback?.correct == false -> Bad
                        selected -> Navy
                        else -> Border
                    }
                    val bgColor by animateColorAsState(bgTarget, label = "OptBg")
                    val borderColor by animateColorAsState(borderTarget, label = "OptBorder")

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(enabled = feedback == null) { onPick(optionIndex) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (selected && feedback?.correct == true) Good else if (selected && feedback?.correct == false) Bad else if (selected) Navy else Canvas),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${'A' + optionIndex}", color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), color = Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        // Once submitted, a question cannot be revisited.
        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (feedback != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (feedback.correct) PaleGreen else Color(0xFFFFE8EB))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (feedback.correct) "✓ Jawaban kamu BENAR!" else "✕ Jawaban kamu SALAH.",
                        color = if (feedback.correct) Good else Bad,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            VokasiButton(
                text = "Kirim Jawaban",
                onClick = onSubmit,
                enabled = selectedIndex != null && seconds > 0 && feedback == null,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}
