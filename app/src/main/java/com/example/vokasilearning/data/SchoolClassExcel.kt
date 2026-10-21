package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Excel class template: Jurusan | Tingkat | Kelas. All-or-nothing import. */
internal object SchoolClassExcel {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    private val headers = listOf("Jurusan", "Tingkat", "Kelas")

    fun template(output: OutputStream) {
        val sheet = XML + "<worksheet xmlns=\"" + NS + "\"><sheetData><row r=\"1\">" +
            headers.mapIndexed { i, h ->
                "<c r=\"" + ('A' + i) + "1\" t=\"inlineStr\"><is><t>" + h + "</t></is></c>"
            }.joinToString("") + "</row></sheetData></worksheet>"
        ZipOutputStream(output).use { z ->
            fun write(path: String, data: String) {
                z.putNextEntry(ZipEntry(path)); z.write(data.toByteArray(Charsets.UTF_8)); z.closeEntry()
            }
            val relationships = "http://schemas.openxmlformats.org/package/2006/relationships"
            val officeRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            write("[Content_Types].xml", XML +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>")
            write("_rels/.rels", XML + "<Relationships xmlns=\"" + relationships + "\">" +
                "<Relationship Id=\"rId1\" Type=\"" + officeRel +
                "/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            write("xl/workbook.xml", XML + "<workbook xmlns=\"" + NS +
                "\" xmlns:r=\"" + officeRel + "\"><sheets>" +
                "<sheet name=\"Daftar Kelas\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            write("xl/_rels/workbook.xml.rels", XML + "<Relationships xmlns=\"" + relationships + "\">" +
                "<Relationship Id=\"rId1\" Type=\"" + officeRel +
                "/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>")
            write("xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun xml(data: ByteArray): org.w3c.dom.Document {
        val txt = data.toString(Charsets.UTF_8)
        require(!txt.contains("<!DOCTYPE", true) && !txt.contains("<!ENTITY", true)) {
            "Excel mengandung XML tidak aman."
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true; isExpandEntityReferences = false
        }
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entity blocked") }
        return builder.parse(ByteArrayInputStream(data))
    }

    private fun value(c: Element, shared: List<String>): String {
        val inline = c.getAttribute("t") == "inlineStr"
        val nodes = c.getElementsByTagNameNS(NS, if (inline) "t" else "v")
        val raw = buildString { for (i in 0 until nodes.length) append(nodes.item(i).textContent) }
        return if (c.getAttribute("t") == "s") {
            shared.getOrNull(raw.toIntOrNull() ?: -1)
                ?: throw IllegalArgumentException("Indeks teks Excel tidak valid.")
        } else raw
    }

    fun read(input: InputStream, existing: SchoolDirectory): List<Classroom> {
        val parts = HashMap<String, ByteArray>()
        var inflated = 0
        var entryCount = 0
        try {
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(8192)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    require(entryCount <= 100) { "Excel terlalu kompleks." }
                    val name = entry.name.replace('\\', '/')
                    require(!name.startsWith("/") && ".." !in name.split('/')) { "Arsip Excel tidak aman." }
                    val needed = name in setOf("xl/sharedStrings.xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml")
                    val bytes = if (needed) ByteArrayOutputStream() else null
                    var part = 0
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        inflated += n; part += n
                        require(inflated <= 10_000_000 && part <= 5_000_000) {
                            "File Excel terlalu besar (maksimal 10 MB)."
                        }
                        bytes?.write(buffer, 0, n)
                    }
                    if (bytes != null) parts[name] = bytes.toByteArray()
                    zip.closeEntry()
                }
            }
            require(parts.containsKey("xl/workbook.xml")) { "Bukan buku kerja Excel .xlsx." }
            val sheet = parts["xl/worksheets/sheet1.xml"]
                ?: throw IllegalArgumentException("Sheet pertama tidak ditemukan.")
            val shared = parts["xl/sharedStrings.xml"]?.let {
                val items = xml(it).getElementsByTagNameNS(NS, "si")
                require(items.length <= 100_000) { "Terlalu banyak teks Excel." }
                (0 until items.length).map { i ->
                    val nodes = (items.item(i) as Element).getElementsByTagNameNS(NS, "t")
                    buildString { for (j in 0 until nodes.length) append(nodes.item(j).textContent) }
                }
            }.orEmpty()
            val rows = xml(sheet).getElementsByTagNameNS(NS, "row")
            val additions = mutableListOf<Classroom>()
            val known = existing.classes.map {
                it.major.lowercase() to it.label.lowercase()
            }.toMutableSet()
            var hasHeader = false
            for (i in 0 until rows.length) {
                val row = rows.item(i) as Element
                val number = row.getAttribute("r").toIntOrNull() ?: i + 1
                val values = MutableList(3) { "" }
                val cells = row.getElementsByTagNameNS(NS, "c")
                for (j in 0 until cells.length) {
                    val c = cells.item(j) as Element
                    require(c.getElementsByTagNameNS(NS, "f").length == 0) {
                        "Baris " + number + ": rumus tidak didukung."
                    }
                    val address = Regex("^([A-Za-z]+)[0-9]+$").matchEntire(c.getAttribute("r"))
                        ?: throw IllegalArgumentException("Baris " + number + ": alamat sel invalid.")
                    var column = 0
                    address.groupValues[1].uppercase().forEach { column = column*26 + (it-'A'+1) }
                    val text = value(c, shared).trim()
                    require(column in 1..3 || text.isEmpty()) {
                        "Baris " + number + ": gunakan kolom A–C saja."
                    }
                    if (column in 1..3) values[column-1] = text
                }
                if (number == 1) {
                    require(values.map { it.lowercase() } == headers.map { it.lowercase() }) {
                        "Header wajib: Jurusan, Tingkat, Kelas. Gunakan template."
                    }
                    hasHeader = true; continue
                }
                if (values.all { it.isBlank() }) continue
                require(additions.size < 1000) { "Maksimal 1.000 kelas sekali impor." }
                val major = existing.majors.firstOrNull { it.equals(values[0], true) }
                    ?: throw IllegalArgumentException("Baris " + number + ": jurusan belum terdaftar: " + values[0])
                val grade = values[1].uppercase()
                require(grade in listOf("X", "XI", "XII")) {
                    "Baris " + number + ": tingkat wajib X, XI, atau XII."
                }
                val suffix = values[2].replace(Regex("\\s+"), " ").trim()
                require(suffix.length in 2..50) { "Baris " + number + ": nama kelas tidak valid." }
                val label = if (suffix.startsWith(grade + " ", true)) suffix else grade + " " + suffix
                require(known.add(major.lowercase() to label.lowercase())) {
                    "Baris " + number + ": kelas " + label + " sudah ada/duplikat."
                }
                additions.add(Classroom(label, major))
            }
            require(hasHeader) { "Header tidak ditemukan." }
            require(additions.isNotEmpty()) { "Tidak ada kelas baru untuk diimpor." }
            return additions
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (_: Exception) {
            throw IllegalArgumentException("Gagal membaca Excel .xlsx sesuai template.")
        }
    }
}
