package com.example.vokasilearning.data

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class BulkStudentExcelReportTest {
    private val date = "2026-10-21"
    private fun time(hour: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }.parse("$date $hour")!!.time
    private val roster = listOf(
        SchoolRosterEntry(1L,"Andi Pratama","X TPM 1","Pemesinan",2026,"andi","hash"),
        SchoolRosterEntry(2L,"Budi Saputra","X TPM 2","Pemesinan",2026,"budi","hash"),
        SchoolRosterEntry(3L,"Citra Putri","X TPM 1","Akuntansi",2026,"citra","hash"),
        SchoolRosterEntry(4L,"Dina Rahma","XI AK 1","Akuntansi",2025)
    )
    private val students = roster.mapNotNull { it.asStudent() }
    private val attempts = listOf(
        Attempt("andi",time("08:00"),"a",70,100,100,70,20,1500),
        Attempt("andi",time("10:00"),"b",86,100,100,86,20,1520),
        Attempt("budi",time("09:00"),"practice",100,10,10,100,5,120,
            practiceCategory = "Perkalian"),
        Attempt("citra",time("11:00"),"c",93,100,100,93,20,1500)
    )

    @Test fun allScopeIncludesMultipleDepartmentsAndAllClassrooms() {
        val records = BulkStudentExcelReport.rows(roster,students,attempts,date)
        assertEquals(4,records.size)
        assertEquals(setOf("Pemesinan","Akuntansi"),records.map { it.major }.toSet())
        assertEquals(86,records.first { it.name == "Andi Pratama" }.score)
        assertEquals("Selesai",records.first { it.name == "Citra Putri" }.status)
        assertEquals("Belum Tercatat",records.first { it.name == "Budi Saputra" }.status)
        assertEquals("Belum Terdaftar",records.first { it.name == "Dina Rahma" }.status)
    }

    @Test fun filtersDepartmentClassAndDateWithoutLeakingCrossDepartmentNames() {
        val onlyMajor = BulkStudentExcelReport.rows(roster,students,attempts,date,"Pemesinan")
        assertEquals(2,onlyMajor.size)
        assertTrue(onlyMajor.all { it.major == "Pemesinan" })
        val onlyClass = BulkStudentExcelReport.rows(roster,students,attempts,date,
            "Pemesinan","X TPM 1")
        assertEquals(listOf("Andi Pratama"),onlyClass.map { it.name })
        val commonClassLabel = BulkStudentExcelReport.rows(roster,students,attempts,date,
            classroom = "X TPM 1")
        assertEquals(setOf("Andi Pratama","Citra Putri"),
            commonClassLabel.map { it.name }.toSet())
    }

    @Test fun workbookHasActualXlsxXmlAndCorrectColumns() {
        val bytes = ByteArrayOutputStream().also {
            BulkStudentExcelReport.write(it,roster,students,attempts,date)
        }.toByteArray()
        assertEquals('P'.code.toByte(),bytes[0])
        assertEquals('K'.code.toByte(),bytes[1])
        var xml = ""
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet1.xml")
                    xml = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertTrue(xml.contains("Jurusan"))
        assertTrue(xml.contains("Angkatan"))
        assertTrue(xml.contains("Andi Pratama"))
        assertTrue(xml.contains("Belum Terdaftar"))
        assertTrue(xml.contains("<v>86</v>"))
        assertFalse(xml.contains("<v>100</v>"))
        assertTrue(xml.contains("autoFilter ref=\"A1:H5\""))
    }

    @Test fun selectedDateAndUsernameCaseDoNotHideRecordedGrades() {
        val pupil = SchoolRosterEntry(
            10L, "Abdul Rizal", "X TPM 1", "Pemesinan", 2026, "abdul_rizal", "hash"
        )
        val submission = Attempt(
            " ABDUL_RIZAL ", time("13:30"), "actual", 30, 100, 100, 30, 20, 1450
        )
        val submitted = BulkStudentExcelReport.rows(
            listOf(pupil), listOfNotNull(pupil.asStudent()), listOf(submission), date
        ).single()
        assertEquals(30, submitted.score)
        assertEquals("Selesai", submitted.status)

        val nextDay = BulkStudentExcelReport.rows(
            listOf(pupil), listOfNotNull(pupil.asStudent()), listOf(submission), "2026-10-22"
        ).single()
        assertNull(nextDay.score)
        assertEquals("Belum Tercatat", nextDay.status)
    }

    @Test fun compatibilityForStudentNotYetRecordedInRoster() {
        val legacy = Student("legacy","hash","Siswa Lama",Classroom("X RPL 1","RPL"))
        val grades = BulkStudentExcelReport.rows(roster,students + legacy,attempts,date)
        assertEquals(5,grades.size)
        assertTrue(grades.any { it.name == "Siswa Lama" && it.status == "Belum Mengerjakan" })
    }
}
