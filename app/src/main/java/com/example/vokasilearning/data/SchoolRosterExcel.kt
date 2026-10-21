package com.example.vokasilearning.data

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Exports only administrative roster fields; passwords never leave the device. */
internal object SchoolRosterExcel {
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private val headers = listOf("No", "Nama Lengkap", "Jurusan", "Kelas", "Angkatan", "Status", "Username")
    private fun escape(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 0x20 || ch in "\n\r\t") append(ch)
            }
        }
    }

    fun write(output: OutputStream, entries: List<SchoolRosterEntry>) {
        val sheet = buildString {
            append(XML).append("<worksheet xmlns=\"").append(NS).append("\"><sheetData>")
            fun row(index: Int, fields: List<String>) {
                append("<row r=\"").append(index).append("\">")
                fields.forEachIndexed { j, field ->
                    append("<c r=\"").append('A' + j).append(index)
                    append("\" t=\"inlineStr\"><is><t>").append(escape(field)).append("</t></is></c>")
                }
                append("</row>")
            }
            row(1, headers)
            entries.sortedWith(compareBy<SchoolRosterEntry> { it.major }
                .thenBy { it.classLabel }.thenBy { it.fullName })
                .forEachIndexed { i, e ->
                    row(i + 2, listOf((i + 1).toString(), e.fullName, e.major, e.classLabel,
                        e.admissionYear.toString(), if (e.registered) "Terdaftar" else "Belum Terdaftar",
                        e.username))
                }
            append("</sheetData></worksheet>")
        }
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
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
                "<workbook xmlns=\"" + NS + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Data Siswa\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels", XML +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>")
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }
}
