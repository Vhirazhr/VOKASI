package com.example.vokasilearning.screens

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.vokasilearning.data.BulkStudentExcelReport
import com.example.vokasilearning.data.SupabaseAttemptRepository
import com.example.vokasilearning.data.SchoolRosterEntry
import com.example.vokasilearning.model.Student
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.ui.VokasiButton

/** Ringkasan-only export: one click, one workbook, all matching school records. */
@Composable
internal fun AdminBulkExcelAction(
    roster: List<SchoolRosterEntry>,
    students: List<Student>,
    attempts: List<Attempt>,
    major: String,
    classroom: String,
    date: String,
    modifier: Modifier = Modifier,
    scoresReady: Boolean = true,
    onExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var selection by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var selectedGrades by remember { mutableStateOf<List<Attempt>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val matchCount = BulkStudentExcelReport.rows(
        roster, students, attempts, date, major, classroom
    ).size
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val scope = selection
        selection = null
        if (uri != null && scope != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    BulkStudentExcelReport.write(
                        out, roster, students, selectedGrades, scope.third, scope.first, scope.second
                    )
                } ?: error("Tidak bisa menyimpan berkas.")
                onExport(scope.first, scope.second, scope.third)
                Toast.makeText(context, "Hasil siswa berhasil diekspor", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ekspor gagal: " + e.message,
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    Column(modifier) {
        VokasiButton(
            text = if (fetching) "Mengambil nilai dari Supabase..."
                else "Unduh Hasil Siswa (" + matchCount + ")",
            enabled = matchCount > 0 && !fetching,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    fetching = true
                    try {
                        val remoteGrades = SupabaseAttemptRepository.loadForAdminDate(date, roster = roster)
                        if (remoteGrades.isEmpty()) {
                            Toast.makeText(context,
                                "Belum ada nilai latihan wajib di Supabase untuk tanggal " + date +
                                    ". Periksa status sinkronisasi di akun siswa.",
                                Toast.LENGTH_LONG).show()
                        } else {
                            val matched = BulkStudentExcelReport.rows(
                                roster, students, remoteGrades, date, major, classroom
                            ).count { it.score != null }
                            if (matched == 0) {
                                Toast.makeText(context,
                                    "Server memiliki " + remoteGrades.size +
                                        " hasil wajib pada " + date +
                                        ", tetapi tidak cocok dengan siswa/filter. Periksa jurusan, kelas, dan username.",
                                    Toast.LENGTH_LONG).show()
                            } else {
                                selectedGrades = remoteGrades
                                selection = Triple(major, classroom, date)
                                fun part(s: String) = s.ifBlank { "Semua" }
                                    .replace(Regex("[^A-Za-z0-9_-]"), "_").take(50)
                                launcher.launch("VOKASI_Hasil_" + part(major) + "_" +
                                    part(classroom) + "_" + date + ".xlsx")
                            }
                        }
                    } catch (error: Exception) {
                        Toast.makeText(context,
                            "Gagal mengambil nilai dari Supabase: " +
                                (error.message ?: "Periksa internet dan login admin."),
                            Toast.LENGTH_LONG).show()
                    } finally {
                        fetching = false
                    }
                }
            }
        )
        Spacer(Modifier.height(3.dp))
        Text("Nilai tanggal terpilih • langsung dari Supabase",
            fontSize = 10.sp, color = Color(0xFF53667B))
    }
}
