package com.example.vokasilearning.data

import com.example.vokasilearning.logic.AdminAnalytics
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Bulk daily grades across optional jurisdiction/class scopes; includes pending roster pupils. */
internal object BulkStudentExcelReport {
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private val headers = listOf("No", "Nama Siswa", "Jurusan", "Kelas", "Angkatan", "Tanggal", "Nilai", "Status")
    internal data class GradeRow(
        val name: String, val major: String, val classroom: String,
        val admissionYear: Int, val date: String, val score: Int?, val status: String
    )

    fun rows(
        roster: List<SchoolRosterEntry>,
        students: List<Student>,
        attempts: List<Attempt>,
        date: String,
        major: String = "",
        classroom: String = ""
    ): List<GradeRow> {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "Tanggal wajib YYYY-MM-DD." }
        val latest = AdminAnalytics.latestCompulsoryByStudent(attempts, date)
        val rosterRows = roster.asSequence().filter {
            (major.isBlank() || it.major == major) &&
                (classroom.isBlank() || it.classLabel == classroom)
        }.map { student ->
            val grade = if (student.registered) latest[student.username] else null
            GradeRow(student.fullName, student.major, student.classLabel,
                student.admissionYear, date, grade?.score, when {
                    !student.registered -> "Belum Terdaftar"
                    grade == null -> "Belum Tercatat"
                    else -> "Selesai"
                })
        }.toList()
        val rosterUsers = roster.mapNotNull { it.username.takeIf(String::isNotBlank) }.toSet()
        // Keep compatibility with students created before the admin-controlled roster.
        val legacyRows = students.asSequence().filter {
            it.username !in rosterUsers &&
                (major.isBlank() || it.classroom.major == major) &&
                (classroom.isBlank() || it.classroom.label == classroom)
        }.map { student ->
            val grade = latest[student.username]
            GradeRow(student.name, student.classroom.major, student.classroom.label,
                student.admissionYear, date, grade?.score,
                if (grade == null) "Belum Mengerjakan" else "Selesai")
        }.toList()
        return (rosterRows + legacyRows).sortedWith(compareBy<GradeRow> { it.major }
            .thenBy { it.classroom }.thenBy { it.name })
    }

    fun write(
        output: OutputStream,
        roster: List<SchoolRosterEntry>,
        students: List<Student>,
        attempts: List<Attempt>,
        date: String,
        major: String = "",
        classroom: String = ""
    ) {
        val data = rows(roster, students, attempts, date, major, classroom)
        val sheet = buildString {
            append(XML)
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
            append("<cols>")
            listOf(7, 33, 26, 20, 14, 17, 12, 25).forEachIndexed { index, width ->
                append("<col min=\"").append(index + 1).append("\" max=\"")
                    .append(index + 1).append("\" width=\"").append(width)
                    .append("\" customWidth=\"1\"/>")
            }
            append("</cols><sheetData>")
            row(1, headers)
            data.forEachIndexed { index, entry ->
                row(index + 2, listOf((index + 1).toString(), entry.name, entry.major,
                    entry.classroom, entry.admissionYear.takeIf { it > 0 }?.toString().orEmpty(),
                    entry.date, entry.score?.toString().orEmpty(), entry.status),
                    numeric = setOf(0, 4, 6))
            }
            append("</sheetData><autoFilter ref=\"A1:H").append(data.size + 1)
                .append("\"/></worksheet>")
        }
        ZipOutputStream(output).use { zip ->
            fun entry(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", XML +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>")
            entry("_rels/.rels", XML +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>")
            entry("xl/workbook.xml", XML +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Hasil Siswa\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels", XML +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>")
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun StringBuilder.row(index: Int, values: List<String>, numeric: Set<Int> = emptySet()) {
        append("<row r=\"").append(index).append("\">")
        values.forEachIndexed { col, value ->
            val address = ('A' + col).toString() + index
            if (col in numeric && value.toIntOrNull() != null) {
                append("<c r=\"").append(address).append("\"><v>").append(value.toInt())
                    .append("</v></c>")
            } else {
                append("<c r=\"").append(address).append("\" t=\"inlineStr\"><is><t>")
                append(escapeXml(value)).append("</t></is></c>")
            }
        }
        append("</row>")
    }

    private fun escapeXml(s: String): String = buildString {
        s.forEach { c ->
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
