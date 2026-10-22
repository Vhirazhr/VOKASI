package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Question
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*

/**
 * Admin home displays only verified school-roster facts.
 * Student quiz results are currently persisted on student devices, NOT in
 * a school-wide Supabase result table. NEVER use the admin phone's attempts
 * to claim 0 students completed or N students failed to participate.
 */
@Composable
internal fun AdminHome(
    students: List<Student>,
    questions: List<Question>,
    attempts: List<Attempt>,
    roster: List<SchoolRosterEntry>,
    onStudents: (String) -> Unit,
    onMonitoring: (String, String, String) -> Unit,
    onExcelExport: (String, String, String) -> Unit,
    adminName: String,
    schoolName: String
) {
    val studentCount = roster.size
    val registered = roster.count { it.registered }
    val unregistered = studentCount - registered
    val majorsCount = roster.map { it.major }.filter { it.isNotBlank() }.distinct().size
    val classesCount = roster.map { it.major to it.classLabel }
        .filter { it.second.isNotBlank() }.distinct().size

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminHero(
                title = "Halo, ${adminName.take(20)}!",
                subtitle = schoolName,
                compact = true
            )
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Data sekolah", color = Ink, fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp)
                Text("Terdaftar di Supabase", color = Good, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total siswa", color = Subtle, fontSize = 12.sp)
                        Text(studentCount.toString(), color = Ink,
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("Akun aktif", color = Subtle, fontSize = 12.sp)
                        Text(registered.toString(), color = Good,
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("Perlu akun", color = Subtle, fontSize = 12.sp)
                        Text(unregistered.toString(), color = Ink,
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                HorizontalDivider(color = Border)
                Text("$majorsCount jurusan  •  $classesCount kelas",
                    color = Subtle, fontSize = 12.sp)
                VokasiButton(
                    text = "Lihat Data Siswa",
                    onClick = { onStudents("") },
                    enabled = students.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Statistik nilai sekolah", color = Ink, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("Belum tersedia", color = Navy, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp)
                Text(
                    "Hasil kuis siswa belum tersinkron ke database sekolah. " +
                        "Jumlah siswa yang sudah atau belum latihan belum dapat dihitung.",
                    color = Subtle, fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = { onMonitoring("", "", "") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Buka Informasi Nilai", color = Navy) }
            }
        }
    }
}
