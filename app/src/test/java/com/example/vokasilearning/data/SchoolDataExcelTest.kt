package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class SchoolDataExcelTest {
    private val original = SchoolDirectory(
        majors = listOf("Pemesinan"),
        classes = listOf(Classroom("X TPM 1", "Pemesinan")),
        roster = listOf(SchoolRosterEntry(42L, "Andi Pratama", "X TPM 1",
            "Pemesinan", 2026, "andi", "pbkdf2\$salt\$hash"))
    )
    private fun workbook(rows: List<List<String>>): ByteArray {
        val template = ByteArrayOutputStream().also {
            SchoolDataExcel.template(it)
        }.toByteArray()
        val result = ByteArrayOutputStream()
        ZipOutputStream(result).use { target ->
            ZipInputStream(ByteArrayInputStream(template)).use { source ->
                while (true) {
                    val entry = source.nextEntry ?: break
                    var bytes = source.readBytes()
                    if (entry.name == "xl/worksheets/sheet1.xml") {
                        val xml = bytes.toString(Charsets.UTF_8)
                        val additions = rows.mapIndexed { index, cols ->
                            val row = index + 2
                            "<row r=\"" + row + "\">" + cols.mapIndexed { j, value ->
                                val safe = value.replace("&","&amp;").replace("<","&lt;")
                                "<c r=\"" + ('A' + j) + row +
                                    "\" t=\"inlineStr\"><is><t>" + safe + "</t></is></c>"
                            }.joinToString("") + "</row>"
                        }.joinToString("")
                        bytes = xml.replace("</sheetData>", additions + "</sheetData>")
                            .toByteArray(Charsets.UTF_8)
                    }
                    target.putNextEntry(ZipEntry(entry.name))
                    target.write(bytes)
                    target.closeEntry()
                    source.closeEntry()
                }
            }
        }
        return result.toByteArray()
    }
    private fun parse(rows: List<List<String>>, directory: SchoolDirectory = original) =
        SchoolDataExcel.read(ByteArrayInputStream(workbook(rows)), directory)

    @Test fun bulkImportCreatesMajorClassesAndUnregisteredStudents() {
        val p = parse(listOf(
            listOf("Pemesinan","X","TPM 2","2026","Budi Santoso"),
            listOf("Akuntansi","XI","AK 1","2025","Citra Maharani"),
            listOf("Akuntansi","XII","AK 2","",""),
            listOf("Perhotelan","","","","")
        ))
        assertEquals(2, p.newMajors)
        assertEquals(3, p.newClasses)
        assertEquals(2, p.newStudents)
        assertEquals(0, p.skipped)
        assertEquals(3, p.next.roster.size)
        assertEquals("X TPM 2", p.next.classes.first { it.label == "X TPM 2" }.label)
        assertFalse(p.next.roster.last().registered)
        assertEquals(43L, p.next.roster[1].id)
        assertEquals("andi", p.next.roster[0].username)
        assertEquals("pbkdf2\$salt\$hash", p.next.roster[0].passwordHash)
        assertEquals(listOf("Pemesinan"), original.majors)
    }

    @Test fun duplicateRowsAreSkippedAndOriginalAccountIsNeverOverwritten() {
        val p = parse(listOf(
            listOf("Pemesinan","X","TPM 1","2026","Andi Pratama"),
            listOf("Pemesinan","X","TPM 1","2026","Andi Pratama"),
            listOf("Pemesinan","X","TPM 2","2026","Budi Santoso"),
            listOf("Pemesinan","X","TPM 2","2026","Budi Santoso")
        ))
        assertEquals(3, p.skipped)
        assertEquals(1, p.newStudents)
        assertEquals(1, p.newClasses)
        assertEquals(2, p.next.roster.size)
        assertEquals("andi", p.next.roster[0].username)
        val repeat = parse(listOf(
            listOf("Pemesinan","X","TPM 1","2026","Andi Pratama"),
            listOf("Pemesinan","X","TPM 2","2026","Budi Santoso")
        ), p.next)
        assertEquals(0, repeat.newStudents)
        assertEquals(2, repeat.skipped)
    }

    @Test fun wrongGradeOrIncompleteRowRejectsEntireWorkbook() {
        val invalid = listOf(
            listOf("Pemesinan","X","TPM 3","2026","Susi Ananda"),
            listOf("Pemesinan","XIII","TPM 4","2026","Reno Sutanto")
        )
        try {
            parse(invalid)
            fail("invalid workbook must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Baris 3"))
        }
        try {
            parse(listOf(listOf("Pemesinan","X","TPM 3","2026","")))
            fail("missing student name must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Baris 2"))
        }
        assertEquals(1, original.roster.size)
    }

    @Test fun exportedTemplateHasCorrectFiveColumnHeader() {
        val bytes = ByteArrayOutputStream().also { SchoolDataExcel.template(it) }.toByteArray()
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])
        try {
            SchoolDataExcel.read(ByteArrayInputStream(bytes), original)
            fail("blank template must not create any data")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Tidak ada data"))
        }
    }
}
