package com.example.vokasilearning.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.DailyExcelReport
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Same navy header language, brand, background and rounded corners as student Home. */
@Composable
internal fun AdminHero(
    title: String,
    subtitle: String,
    compact: Boolean = false
) {
    Box(
        Modifier.fillMaxWidth()
            .height(if (compact) 256.dp else 310.dp)
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF294D7A))))
            .padding(22.dp)
    ) {
        Box(
            Modifier.size(220.dp).align(Alignment.TopEnd)
                .offset(x = 124.dp, y = 25.dp)
                .border(36.dp, Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▣", color = Color.White, fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("VOKASI", color = Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Text("LEARNING", color = Color.White.copy(alpha = 0.72f),
                    fontSize = 8.sp, letterSpacing = 2.sp)
            }
            Text("ADMIN", color = Color(0xFFDAE5F3), fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp))
        }
        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(bottom = if (compact) 80.dp else 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White, fontSize = if (compact) 24.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFFD5E2F0), fontSize = 12.sp, maxLines = 2)
        }
    }
}

@Composable
internal fun AdminSectionTitle(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow.uppercase(Locale("id", "ID")), color = NavyLight,
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
        Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun AdminSelect(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = Subtle, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { open = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(value.ifBlank { "Semua" }, modifier = Modifier.weight(1f),
                    fontSize = 12.sp, color = Ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("⌄", color = Navy)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.ifBlank { "Semua" }, fontSize = 13.sp) },
                        onClick = { onChange(option); open = false }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AdminDateSelect(
    date: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tanggal", fontSize = 11.sp, color = Subtle, fontWeight = FontWeight.Bold)
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                val parsed = date.split("-").mapNotNull { it.toIntOrNull() }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        onChange(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
                    },
                    parsed.getOrNull(0) ?: cal.get(Calendar.YEAR),
                    (parsed.getOrNull(1) ?: cal.get(Calendar.MONTH) + 1) - 1,
                    parsed.getOrNull(2) ?: cal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                }.show()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentPadding = PaddingValues(horizontal = 9.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(date, color = Ink, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun AdminExcelAction(
    students: List<Student>,
    attempts: List<Attempt>,
    major: String,
    classroom: String,
    date: String,
    modifier: Modifier = Modifier,
    text: String = "Unduh Excel",
    onExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var target by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val selection = target
        target = null
        if (uri != null && selection != null) {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Tidak bisa membuat berkas")
                stream.use {
                    DailyExcelReport.write(
                        it, selection.second, selection.third, students, attempts, selection.first
                    )
                }
                Toast.makeText(context, "Excel tersimpan", Toast.LENGTH_SHORT).show()
                onExport(selection.first, selection.second, selection.third)
            } catch (ex: Exception) {
                Toast.makeText(context, "Ekspor gagal: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    VokasiButton(
        text = text,
        enabled = major.isNotBlank() && classroom.isNotBlank(),
        modifier = modifier,
        onClick = {
            target = Triple(major, classroom, date)
            val fileClass = classroom.replace(Regex("[^A-Za-z0-9_-]"), "_")
            launcher.launch("VOKASI_Nilai_${fileClass}_$date.xlsx")
        }
    )
}
