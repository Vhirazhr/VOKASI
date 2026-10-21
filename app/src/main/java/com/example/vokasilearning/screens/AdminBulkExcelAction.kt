package com.example.vokasilearning.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.vokasilearning.data.BulkStudentExcelReport
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
    onExport: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var selection by remember { mutableStateOf<Triple<String, String, String>?>(null) }
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
                        out, roster, students, attempts, scope.third, scope.first, scope.second
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
    VokasiButton(
        text = "Unduh Hasil Siswa (" + matchCount + ")",
        enabled = matchCount > 0,
        modifier = modifier,
        onClick = {
            selection = Triple(major, classroom, date)
            fun part(s: String) = s.ifBlank { "Semua" }
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(50)
            launcher.launch("VOKASI_Hasil_" + part(major) + "_" + part(classroom) +
                "_" + date + ".xlsx")
        }
    )
}
