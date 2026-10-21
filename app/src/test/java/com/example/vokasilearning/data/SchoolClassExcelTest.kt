package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class SchoolClassExcelTest {
    private val school = SchoolDirectory(majors = listOf("Pemesinan"), classes = listOf(Classroom("X TPM 1", "Pemesinan")))
    private fun workbook(rows: List<List<String>>): ByteArray {
        val template = ByteArrayOutputStream().also { SchoolClassExcel.template(it) }.toByteArray()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { target ->
            ZipInputStream(ByteArrayInputStream(template)).use { source ->
                while (true) {
                    val item = source.nextEntry ?: break
                    var bytes = source.readBytes()
                    if (item.name == "xl/worksheets/sheet1.xml") {
                        val xml = bytes.toString(Charsets.UTF_8)
                        val additions = rows.mapIndexed { index, values ->
                            val r = index + 2
                            "<row r=\"" + r + "\">" + values.mapIndexed { col, text ->
                                "<c r=\"" + ('A' + col) + r +
                                    "\" t=\"inlineStr\"><is><t>" + text + "</t></is></c>"
                            }.joinToString("") + "</row>"
                        }.joinToString("")
                        bytes = xml.replace("</sheetData>", additions + "</sheetData>").toByteArray()
                    }
                    target.putNextEntry(ZipEntry(item.name))
                    target.write(bytes)
                    target.closeEntry()
                    source.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }
    private fun load(vararg rows: List<String>): List<Classroom> =
        SchoolClassExcel.read(ByteArrayInputStream(workbook(rows.toList())), school)

    @Test fun importsMultipleClassRowsWithGradeAndExistingMajor() {
        val rows = load(
            listOf("Pemesinan","X","TPM 2"),
            listOf("Pemesinan","XI","TPM 1"),
            listOf("Pemesinan","XII","XII TPM 1")
        )
        assertEquals(3, rows.size)
        assertEquals("X TPM 2", rows[0].label)
        assertEquals("XI TPM 1", rows[1].label)
        assertEquals("XII TPM 1", rows[2].label)
    }

    @Test fun rejectsDuplicateExistingAndRepeatedBatch() {
        try {
            load(listOf("Pemesinan","X","TPM 2"), listOf("Pemesinan","X","TPM 2"))
            fail("duplicate should reject entire batch")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Baris 3"))
        }
        try {
            load(listOf("Pemesinan","X","TPM 1"))
            fail("existing must reject")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("sudah ada"))
        }
    }

    @Test fun rejectsUnknownMajorAndInvalidGrade() {
        try {
            load(listOf("Jurusan Fiktif","X","TPM 2"))
            fail("unknown major")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("jurusan belum terdaftar"))
        }
        try {
            load(listOf("Pemesinan","XIII","TPM 2"))
            fail("invalid grade")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("tingkat"))
        }
    }
}
