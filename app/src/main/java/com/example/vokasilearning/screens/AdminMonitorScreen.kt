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
internal fun AdminMonitorScreen(students: List<Student>, attempts: List<Attempt>) {
    var selectedClass by remember { mutableStateOf("Semua kelas") }
    var filterOpen by remember { mutableStateOf(false) }
    val group = listOf("Semua kelas") + demoClasses.map { it.label }.distinct()
    val filtered = students.filter { selectedClass == "Semua kelas" || it.classroom.label == selectedClass }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { VokasiTitle("Monitoring Siswa", "Ringkasan hasil dan progres latihan • dummy") }

        item {
            Box {
                OutlinedButton(onClick = { filterOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(selectedClass, color = Ink)
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    group.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { selectedClass = item; filterOpen = false })
                    }
                }
            }
        }

        items(filtered, key = { it.username }) { student ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(student)
                    Column(Modifier.weight(1f)) {
                        Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("${student.classroom.label} • ${student.classroom.major}", fontSize = 12.sp, color = Subtle)
                    }
                    Pill("${studentSeasonXp(student, attempts)} XP", PaleBlue, Navy)
                }
                Text("Streak: ${calculateStreak(student, attempts)} hari  •  Sesi baru: ${attempts.count { it.studentId == student.username }}", color = Subtle, fontSize = 12.sp)
            }
        }
    }
}
