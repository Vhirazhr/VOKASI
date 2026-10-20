package com.example.vokasilearning.data

import com.example.vokasilearning.logic.dayKey
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a genuine minimal XLSX file, not a CSV renamed to .xlsx.
 * Export includes only one sheet and the 6 agreed daily-grade columns.
 * Local-device records are not official backend-verified grades.
 */
internal object DailyExcelReport {
    private val header = listOf("No", "Nama Siswa", "Kelas", "Tanggal", "Nilai", "Status")
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    fun write(
        output: OutputStream,
        classroom: String,
        date: String,
        students: List<Student>,
        attempts: List<Attempt>,
        major: String? = null
    ) {
        require(classroom.isNotBlank())
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date))
        val members = students.filter {
            it.classroom.label == classroom &&
                (major == null || it.classroom.major == major)
        }.sortedBy { it.name }
        val rows = members.mapIndexed { i, student ->
            val latest = attempts.asSequence()
                .filter { it.studentId == student.username && it.practiceCategory == null && dayKey(it.timeMillis) == date }
                .maxWithOrNull(compareBy<Attempt> { it.timeMillis }.thenBy { it.sessionKey })
            listOf(
                (i + 1).toString(),
                student.name,
                student.classroom.label,
                date,
                latest?.score?.toString() ?: "",
                if (latest == null) "Belum Mengerjakan" else "Selesai"
            )
        }
        val sheet = buildString {
            append(XML)
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
            append("<cols>")
            listOf(7, 32, 20, 17, 12, 24).forEachIndexed { i, w ->
                append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
            }
            append("</cols><sheetData>")
            row(1, header, numericColumns = emptySet())
            rows.forEachIndexed { i, cells -> row(i + 2, cells, numericColumns = setOf(0, 4)) }
            append("</sheetData><autoFilter ref=\"A1:F${rows.size + 1}\"/></worksheet>")
        }
        ZipOutputStream(output).use { zip ->
            fun entry(path: String, data: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(data.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                XML + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>"
            )
            entry(
                "_rels/.rels",
                XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>"
            )
            entry(
                "xl/workbook.xml",
                XML + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"Nilai Siswa\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
            )
            entry(
                "xl/_rels/workbook.xml.rels",
                XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "</Relationships>"
            )
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun StringBuilder.row(index: Int, values: List<String>, numericColumns: Set<Int>) {
        append("<row r=\"$index\">")
        values.forEachIndexed { column, value ->
            val address = "${'A' + column}$index"
            if (column in numericColumns && value.toIntOrNull() != null) {
                append("<c r=\"$address\"><v>${value.toInt()}</v></c>")
            } else {
                // Inline string cells stay strings even when containing '=' or other formula prefixes.
                append("<c r=\"$address\" t=\"inlineStr\"><is><t>")
                append(escapeXml(value))
                append("</t></is></c>")
            }
        }
        append("</row>")
    }

    private fun escapeXml(input: String): String = buildString {
        input.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (c.code >= 0x20) append(c)
            }
        }
    }
}
