package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.ui.*

/**
 * No school-level grades are available until quiz attempts are synced to a
 * secure Supabase table. LocalAttemptStore on an administrator's phone is not
 * evidence that students elsewhere have not done their compulsory exercises.
 *
 * Keep the route available but fail CLOSED instead of presenting fabricated
 * completion counts, distributions, or Excel reports.
 */
@Composable
internal fun AdminMonitorScreen(
    students: List<Student>,
    attempts: List<Attempt>,
    initialMajor: String = "",
    initialClass: String = "",
    initialDate: String = "",
    onExcelExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AdminHero("Nilai Siswa", "Rekap nilai sekolah", compact = true) }
        item {
            WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Sinkronisasi nilai belum aktif", color = Ink,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(
                    "Saat ini hasil latihan disimpan di HP masing-masing siswa. " +
                        "Admin belum bisa memastikan siapa yang sudah atau belum mengerjakan.",
                    color = Subtle, fontSize = 13.sp
                )
                HorizontalDivider(color = Border)
                Text("Data siswa terdaftar tetap tersedia di menu Siswa.",
                    color = Navy, fontSize = 12.sp)
                Text(
                    "Rekap, grafik, dan ekspor nilai akan diaktifkan setelah " +
                        "hasil kuis tersinkron aman ke Supabase.",
                    color = Subtle, fontSize = 12.sp
                )
            }
        }
    }
}
