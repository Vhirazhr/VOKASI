package com.example.vokasilearning.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.DailyExcelReport
import com.example.vokasilearning.data.SupabaseAttemptRepository
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

internal data class AdminHeaderActions(
    val onNotifications: () -> Unit,
    val onSettings: () -> Unit,
    val onAssistant: () -> Unit
)

internal val LocalAdminHeaderActions = staticCompositionLocalOf<AdminHeaderActions?> { null }

@Composable
internal fun AdminTopIcon(kind: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
        Canvas(Modifier.size(21.dp)) {
            val unit = size.minDimension / 24f
            val path = Path()
            fun m(x: Float, y: Float) { path.moveTo(x * unit, y * unit) }
            fun l(x: Float, y: Float) { path.lineTo(x * unit, y * unit) }
            when (kind) {
                0 -> { // Notification bell
                    m(5f, 17f); l(7f, 14f); l(7f, 10f)
                    l(8.5f, 7f); l(11f, 6f); l(13f, 6f)
                    l(15.5f, 7f); l(17f, 10f); l(17f, 14f)
                    l(19f, 17f); l(5f, 17f)
                    m(10f, 20f); l(14f, 20f)
                }
                1 -> { // Settings gear
                    val radius = 8f * unit
                    drawCircle(Color.White, radius,
                        Offset(12f * unit, 12f * unit), style = Stroke(2.dp.toPx()))
                    drawCircle(Color.White, 3f * unit,
                        Offset(12f * unit, 12f * unit), style = Stroke(2.dp.toPx()))
                    for (i in 0 until 8) {
                        val radians = i * kotlin.math.PI.toFloat() / 4f
                        val dx = kotlin.math.cos(radians)
                        val dy = kotlin.math.sin(radians)
                        drawLine(
                            Color.White,
                            Offset((12f + 8f * dx) * unit, (12f + 8f * dy) * unit),
                            Offset((12f + 10.5f * dx) * unit, (12f + 10.5f * dy) * unit),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                2 -> { // Assistant message
                    m(4f, 5f); l(20f, 5f); l(20f, 17f)
                    l(11f, 17f); l(7f, 21f); l(7f, 17f)
                    l(4f, 17f); l(4f, 5f)
                    m(8f, 10f); l(16f, 10f)
                    m(8f, 13f); l(14f, 13f)
                }
            }
            drawPath(path, Color.White, style = Stroke(2.dp.toPx()))
        }
    }
}

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
        val actions = LocalAdminHeaderActions.current
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VokasiBrandMark(size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("VOKASI", color = Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            }
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    AdminTopIcon(0, "Notifikasi", actions.onNotifications)
                    AdminTopIcon(1, "Pengaturan", actions.onSettings)
                    AdminTopIcon(2, "Asisten", actions.onAssistant)
                }
            }
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
    var search by remember(open) { mutableStateOf("") }
    val searchable = label.contains("jurusan", ignoreCase = true)
    val visibleOptions = if (searchable) options.filter {
        it.isBlank() || it.contains(search.trim(), ignoreCase = true)
    } else options
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
                Text(value.ifBlank { if (options.any { it.isBlank() }) "Semua" else "Pilih ${label.lowercase(Locale("id", "ID"))}" }, modifier = Modifier.weight(1f),
                    fontSize = 12.sp, color = Ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("⌄", color = Navy)
            }
            if (!searchable) {
                DropdownMenu(
                    expanded = open,
                    onDismissRequest = { open = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(option.ifBlank { "Semua" }, fontSize = 13.sp,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            onClick = { onChange(option); open = false }
                        )
                    }
                }
            }
        }
    }
    if (open && searchable) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Pilih jurusan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Cari jurusan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (visibleOptions.isEmpty()) {
                            item {
                                Text("Jurusan tidak ditemukan", color = Subtle,
                                    fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                        items(visibleOptions.distinct()) { option ->
                            TextButton(
                                onClick = { onChange(option); open = false },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp, vertical = 11.dp
                                )
                            ) {
                                Text(
                                    option.ifBlank { "Semua jurusan" },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Ink, fontSize = 13.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text("Tutup", color = Navy)
                }
            }
        )
    }
}


/** Shared pagination used by every data listing in the admin panel. */
@Composable
internal fun AdminPaginationFooter(
    label: String,
    total: Int,
    page: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = AdminPagination.PAGE_SIZE
) {
    val current = AdminPagination.active(page, total, pageSize)
    val count = AdminPagination.count(total, pageSize)
    val start = if (total == 0) 0 else current * pageSize + 1
    val end = minOf(total, (current + 1) * pageSize)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label $start–$end dari $total",
            fontSize = 11.sp, color = Subtle)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onPrevious, enabled = current > 0,
                shape = RoundedCornerShape(12.dp)) { Text("Sebelumnya", fontSize = 12.sp) }
            Text("${current + 1}/$count", color = Ink,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onNext, enabled = current < count - 1,
                shape = RoundedCornerShape(12.dp)) { Text("Berikutnya", fontSize = 12.sp) }
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
    roster: List<SchoolRosterEntry> = emptyList(),
    onExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var target by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var selectedGrades by remember { mutableStateOf<List<Attempt>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
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
                        it, selection.second, selection.third, students, selectedGrades,
                        selection.first
                    )
                }
                Toast.makeText(context, "Excel tersimpan untuk " + selection.third,
                    Toast.LENGTH_SHORT).show()
                onExport(selection.first, selection.second, selection.third)
            } catch (ex: Exception) {
                Toast.makeText(context, "Ekspor gagal: " + ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    VokasiButton(
        text = if (fetching) "Mengambil nilai dari Supabase..." else text,
        enabled = major.isNotBlank() && classroom.isNotBlank() && !fetching,
        modifier = modifier,
        onClick = {
            coroutineScope.launch {
                fetching = true
                try {
                    val freshGrades = SupabaseAttemptRepository.loadForAdminDate(date, roster = roster)
                    if (freshGrades.isEmpty()) {
                        Toast.makeText(context,
                            "Belum ada nilai wajib di Supabase untuk " + date +
                                ". Pilih tanggal lain atau cek sinkronisasi siswa.",
                            Toast.LENGTH_LONG).show()
                    } else {
                        val members = students.filter {
                            it.classroom.label == classroom && it.classroom.major == major
                        }.map { it.username.trim().lowercase(Locale.ROOT) }.toSet()
                        val matched = AdminAnalytics.latestCompulsoryByStudent(freshGrades, date)
                            .count { (username, _) -> username.trim().lowercase(Locale.ROOT) in members }
                        if (matched == 0) {
                            Toast.makeText(context, "Server memiliki " + freshGrades.size +
                                " hasil wajib, tetapi tidak cocok dengan kelas " + classroom +
                                " pada " + date + ".", Toast.LENGTH_LONG).show()
                        } else {
                            target = Triple(major, classroom, date)
                            selectedGrades = freshGrades
                            val fileClass = classroom.replace(Regex("[^A-Za-z0-9_-]"), "_")
                            launcher.launch("VOKASI_Nilai_" + fileClass + "_" + date + ".xlsx")
                        }
                    }
                } catch (error: Exception) {
                    Toast.makeText(context, "Gagal memuat nilai dari Supabase: " +
                        (error.message ?: "Periksa koneksi internet."), Toast.LENGTH_LONG).show()
                } finally {
                    fetching = false
                }
            }
        }
    )
}
