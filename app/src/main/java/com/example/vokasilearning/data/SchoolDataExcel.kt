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

/** Excel mass import of school master data and student roster, with preview before commit. */
internal object SchoolDataExcel {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    private val headers = listOf("Jurusan", "Tingkat", "Kelas", "Angkatan", "Nama Lengkap")
    data class Preview(val original: SchoolDirectory, val next: SchoolDirectory,
        val newMajors: Int, val newClasses: Int, val newStudents: Int, val skipped: Int)

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
                "<sheet name=\"Data Sekolah\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
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

    private fun normalized(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)

    /**
     * A-E = Jurusan, Tingkat, Kelas, Angkatan, Nama Lengkap.
     * Blank B-E = major only; blank D-E = class only.
     * Exact previously imported pupils are skipped without changing account credentials.
     */
    fun read(input: InputStream, existing: SchoolDirectory): Preview {
        val parts = HashMap<String, ByteArray>()
        var inflated = 0
        var entryCount = 0
        try {
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(8192)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    require(entryCount <= 100) { "File Excel terlalu kompleks." }
                    val name = entry.name.replace('\\', '/')
                    require(!name.startsWith("/") && ".." !in name.split('/')) { "Arsip Excel tidak aman." }
                    val needed = name in setOf("xl/sharedStrings.xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml")
                    val bytes = if (needed) ByteArrayOutputStream() else null
                    var part = 0
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        inflated += n; part += n
                        require(inflated <= 15_000_000 && part <= 10_000_000) {
                            "File Excel terlalu besar (maksimal 15 MB setelah dibuka)."
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
                require(items.length <= 50_000) { "Terlalu banyak teks Excel." }
                (0 until items.length).map { i ->
                    val nodes = (items.item(i) as Element).getElementsByTagNameNS(NS, "t")
                    buildString { for (j in 0 until nodes.length) append(nodes.item(j).textContent) }
                }
            }.orEmpty()
            val rows = xml(sheet).getElementsByTagNameNS(NS, "row")
            require(rows.length <= 10001) { "Maksimal 10.000 baris data." }

            val majors = existing.majors.toMutableList()
            val classes = existing.classes.toMutableList()
            val roster = existing.roster.toMutableList()
            val majorNames = majors.associateByTo(mutableMapOf()) { normalized(it) }
            val classNames = classes.associateByTo(mutableMapOf()) {
                normalized(it.major) to normalized(it.label)
            }
            val knownStudents = roster.mapTo(mutableSetOf()) {
                listOf(normalized(it.fullName), normalized(it.major), normalized(it.classLabel),
                    it.admissionYear.toString()).joinToString("|")
            }
            var nextId = (roster.maxOfOrNull { it.id } ?: 0L) + 1L
            var newMajors = 0
            var newClasses = 0
            var newStudents = 0
            var skipped = 0
            var hasHeader = false

            for (i in 0 until rows.length) {
                val row = rows.item(i) as Element
                val number = row.getAttribute("r").toIntOrNull() ?: i + 1
                val values = MutableList(5) { "" }
                val cells = row.getElementsByTagNameNS(NS, "c")
                for (j in 0 until cells.length) {
                    val c = cells.item(j) as Element
                    val address = Regex("^([A-Za-z]+)[0-9]+$").matchEntire(c.getAttribute("r"))
                        ?: throw IllegalArgumentException("Baris " + number + ": alamat sel tidak valid.")
                    var column = 0
                    address.groupValues[1].uppercase().forEach { column = column * 26 + (it-'A'+1) }
                    val text = try { value(c, shared).trim() }
                        catch (e: IllegalArgumentException) {
                            throw IllegalArgumentException("Baris " + number + ": " + e.message)
                        }
                    require(c.getElementsByTagNameNS(NS, "f").length == 0) {
                        "Baris " + number + ": rumus tidak didukung."
                    }
                    require(column in 1..5 || text.isEmpty()) {
                        "Baris " + number + ": gunakan kolom A–E saja."
                    }
                    if (column in 1..5) values[column - 1] = text
                }
                if (number == 1) {
                    require(values.map { normalized(it) } == headers.map { normalized(it) }) {
                        "Header wajib: Jurusan, Tingkat, Kelas, Angkatan, Nama Lengkap. Gunakan template."
                    }
                    hasHeader = true
                    continue
                }
                if (values.all { it.isBlank() }) continue
                val majorRaw = values[0].replace(Regex("\\s+"), " ")
                require(majorRaw.length in 3..80 && majorRaw.none { it == '\n' || it == '\r' }) {
                    "Baris " + number + ": jurusan tidak valid."
                }
                val major = majorNames[normalized(majorRaw)] ?: majorRaw.also {
                    majors.add(it)
                    majorNames[normalized(it)] = it
                    newMajors++
                }
                if (values.drop(1).all { it.isBlank() }) continue

                val grade = values[1].uppercase()
                require(grade in listOf("X", "XI", "XII")) {
                    "Baris " + number + ": tingkat harus X, XI, atau XII."
                }
                val suffix = values[2].replace(Regex("\\s+"), " ")
                require(suffix.length in 2..60 && suffix.none { it == '\n' || it == '\r' }) {
                    "Baris " + number + ": nama kelas tidak valid."
                }
                val classLabel = if (suffix.startsWith(grade + " ", ignoreCase = true)) suffix
                    else grade + " " + suffix
                val classKey = normalized(major) to normalized(classLabel)
                val classroom = classNames[classKey] ?: Classroom(classLabel, major).also {
                    classes.add(it)
                    classNames[classKey] = it
                    newClasses++
                }
                if (values[3].isBlank() && values[4].isBlank()) continue
                require(values[3].isNotBlank() && values[4].isNotBlank()) {
                    "Baris " + number + ": nama lengkap dan angkatan harus diisi bersama."
                }
                val year = values[3].toIntOrNull()
                val maxYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1
                require(year != null && year in 2000..maxYear) {
                    "Baris " + number + ": angkatan harus tahun masuk, contoh 2026."
                }
                val name = values[4].replace(Regex("\\s+"), " ")
                require(name.length in 5..120 && name.contains(' ')) {
                    "Baris " + number + ": nama siswa harus nama lengkap minimal dua kata."
                }
                val studentKey = listOf(normalized(name), normalized(major),
                    normalized(classroom.label), year.toString()).joinToString("|")
                if (!knownStudents.add(studentKey)) {
                    skipped++
                    continue
                }
                roster.add(SchoolRosterEntry(nextId++, name, classroom.label, classroom.major, year))
                newStudents++
            }
            require(hasHeader) { "Header Excel tidak ditemukan." }
            require(newMajors + newClasses + newStudents > 0 || skipped > 0) {
                "Tidak ada data untuk diimpor. Isi Excel mulai baris kedua."
            }
            return Preview(existing, existing.copy(majors = majors, classes = classes,
                roster = roster), newMajors, newClasses, newStudents, skipped)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (_: Exception) {
            throw IllegalArgumentException("Gagal membaca Excel .xlsx. Gunakan template Data Sekolah.")
        }
    }
}
