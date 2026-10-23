package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.model.*
import com.example.vokasilearning.ui.*

/** School-wide admin overview; grade export defaults to all majors/classes. */
@Composable
internal fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    roster: List<SchoolRosterEntry>,
    scoresReady: Boolean = true,
    scoresNotice: String = "",
    onStudents: (String) -> Unit,
    onMonitoring: (String, String, String) -> Unit,
    onExcelExport: (String, String, String) -> Unit,
    adminName: String,
    schoolName: String
) {
    val majors = (students.map { it.classroom.major } + roster.map { it.major })
        .distinct().sorted()
    var major by remember { mutableStateOf("") }
    val classOptions = (students.filter { major.isBlank() || it.classroom.major == major }
        .map { it.classroom.label } + roster.filter { major.isBlank() || it.major == major }
        .map { it.classLabel }).distinct().sorted()
    var classroom by remember(major) { mutableStateOf("") }
    var date by remember { mutableStateOf(dayKey(System.currentTimeMillis())) }

    LazyColumn(
        Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                AdminHero(
                    title = "Halo, ${adminName.take(20)}!",
                    subtitle = "Pantau hasil latihan siswa • $schoolName"
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 225.dp)
                        .shadow(14.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(17.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HASIL LATIHAN WAJIB",
                                modifier = Modifier.weight(1f), color = NavyLight,
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Pill(if (scoresReady) "Supabase" else "Menunggu data",
                                if (scoresReady) PaleGreen else PaleGold,
                                if (scoresReady) Good else Warning)
                        }
                        Text("Unduh Hasil Siswa", color = Ink, fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Pilih tanggal dan filter untuk laporan nilai wajib.",
                            color = Subtle, fontSize = 11.sp)
                        AdminSelect("Jurusan", major, listOf("") + majors, { major = it },
                            Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSelect("Kelas", classroom, listOf("") + classOptions, { classroom = it },
                                Modifier.weight(1f))
                            AdminDateSelect(date, { date = it }, Modifier.weight(1f))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal cepat:", color = Subtle, fontSize = 11.sp)
                            TextButton(
                                onClick = { date = dayKey(System.currentTimeMillis()) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                            ) { Text("Hari ini", fontSize = 11.sp) }
                            TextButton(
                                onClick = {
                                    val yesterday = wibCalendar().apply {
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    date = dayKey(yesterday.timeInMillis)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                            ) { Text("Kemarin", fontSize = 11.sp) }
                        }
                        VokasiButton(
                            text = "Lihat Hasil Siswa",
                            onClick = { onMonitoring(major, classroom, date) },
                            enabled = students.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AdminBulkExcelAction(
                            roster = roster, students = students, attempts = attempts,
                            major = major, classroom = classroom, date = date,
                            modifier = Modifier.fillMaxWidth(), onExport = onExcelExport,
                            scoresReady = scoresReady
                        )
                    }
                }
            }
        }

    }
}
