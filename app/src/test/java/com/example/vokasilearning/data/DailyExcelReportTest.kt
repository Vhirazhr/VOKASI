package com.example.vokasilearning.data

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

class DailyExcelReportTest {
    private fun at(time: String): Long = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }.parse(time)!!.time

    private fun workbookSheet(students: List<Student>, attempts: List<Attempt>): String {
        val out = ByteArrayOutputStream()
        DailyExcelReport.write(out, "X 1", "2026-10-21", students, attempts, "Teknik Industri")
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.name == "xl/worksheets/sheet1.xml") return zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        error("Worksheet missing")
    }

    @Test fun excelContainsOnlySelectedDepartmentAndLatestCompulsoryGrade() {
        val students = listOf(
            Student("ti", "demo", "Tina", Classroom("X 1", "Teknik Industri")),
            Student("ak", "demo", "Andra", Classroom("X 1", "Akuntansi"))
        )
        val attempts = listOf(
            Attempt("ti", at("2026-10-21 08:00"), "a", 70, 100, 100, 70, 20, 1600),
            Attempt("ti", at("2026-10-21 09:00"), "b", 85, 100, 100, 85, 20, 1600),
            Attempt("ti", at("2026-10-21 10:00"), "c", 100, 10, 10, 100, 5, 80,
                practiceCategory = "Perkalian"),
            Attempt("ak", at("2026-10-21 09:00"), "d", 99, 100, 100, 99, 20, 1700)
        )
        val sheet = workbookSheet(students, attempts)
        assertTrue(sheet.contains("Tina"))
        assertTrue(sheet.contains("<v>85</v>"))
        assertFalse(sheet.contains("Andra"))
        assertFalse(sheet.contains("<v>99</v>"))
        assertFalse(sheet.contains("<v>100</v>"))
    }
}
