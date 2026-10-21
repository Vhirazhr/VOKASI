package com.example.vokasilearning.data

import com.example.vokasilearning.logic.AdminQuestionRules
import com.example.vokasilearning.logic.BankPolicy
import com.example.vokasilearning.model.Question
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Safe, dependency-free .xlsx question import, with all-or-nothing validation. */
internal object QuestionExcelFile {
    private val headers = listOf("Materi", "Kode", "Pertanyaan", "A", "B", "C", "D", "Kunci", "Pembahasan")
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val MAX_ROWS = 1000
    private const val MAX_XML_BYTES = 5 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 12 * 1024 * 1024

    data class Draft(
        val row: Int,
        val category: String,
        val code: String,
        val question: String,
        val options: List<String>,
        val answerIndex: Int,
        val explanation: String
    )

    /** Empty, import-ready template with the exact required column names. */
    fun writeTemplate(output: OutputStream) = writeWorkbook(output, emptyList())

    /**
     * Exports every supplied question (not just the current ten visible rows).
     * The same nine columns can be imported again as a VOKASI question sheet.
     */
    fun writeExport(output: OutputStream, questions: List<Question>) {
        require(questions.isNotEmpty()) { "Tidak ada soal yang dapat diekspor." }
        questions.forEach { question ->
            require(question.options.size == 4 && question.answerIndex in 0..3) {
                "Soal #" + question.id + " tidak memiliki empat pilihan dan satu kunci yang valid."
            }
        }
        writeWorkbook(output, questions)
    }

    private fun writeWorkbook(output: OutputStream, questions: List<Question>) {
        val sheet = buildString {
            append(XML).append("<worksheet xmlns=\"").append(NS)
            append("\"><sheetData>")
            fun excelRow(rowNumber: Int, values: List<String>) {
                append("<row r=\"").append(rowNumber).append("\">")
                values.forEachIndexed { column, value ->
                    append("<c r=\"").append('A' + column).append(rowNumber)
                    append("\" t=\"inlineStr\"><is><t>")
                    append(escapeXml(value))
                    append("</t></is></c>")
                }
                append("</row>")
            }
            excelRow(1, headers)
            questions.forEachIndexed { index, question ->
                excelRow(
                    index + 2,
                    listOf(
                        question.category,
                        question.difficultyCode,
                        question.question
                    ) + question.options + listOf(
                        ('A' + question.answerIndex).toString(),
                        question.explanation
                    )
                )
            }
            append("</sheetData></worksheet>")
        }
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, data: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data.toByteArray(Charsets.UTF_8))
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
                "<workbook xmlns=\"" + NS + "\" xmlns:r=\"" + REL_NS + "\">" +
                "<sheets><sheet name=\"Soal VOKASI\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels", XML +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>")
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun escapeXml(input: String): String = buildString {
        input.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (c.code >= 0x20 || c == '\n' || c == '\r' || c == '\t') append(c)
            }
        }
    }

    /** Supports inline strings, shared strings and normal numeric cells from Excel. */
    fun read(input: InputStream): List<Draft> {
        val parts = linkedMapOf<String, ByteArray>()
        var total = 0
        var entryCount = 0
        try {
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(8192)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    require(entryCount <= 200) { "Excel terlalu kompleks. Gunakan template VOKASI." }
                    val name = entry.name.replace('\\', '/')
                    require(!name.startsWith("/") && ".." !in name.split('/')) { "Arsip Excel tidak valid." }
                    val useful = name == "xl/sharedStrings.xml" ||
                        name == "xl/workbook.xml" || name == "xl/_rels/workbook.xml.rels" ||
                        Regex("xl/worksheets/sheet[0-9]+\\.xml").matches(name)
                    val dest = if (useful) ByteArrayOutputStream() else null
                    var partSize = 0
                    while (true) {
                        val n = zip.read(buffer)
                        if (n == -1) break
                        total += n
                        partSize += n
                        require(total <= MAX_TOTAL_BYTES) { "Excel terlalu besar (maksimal 12 MB data)." }
                        if (dest != null) {
                            require(partSize <= MAX_XML_BYTES) { "Sheet Excel terlalu besar (maksimal 5 MB)." }
                            dest.write(buffer, 0, n)
                        }
                    }
                    if (dest != null) parts[name] = dest.toByteArray()
                    zip.closeEntry()
                }
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (_: Exception) {
            throw IllegalArgumentException("File tidak dapat dibaca sebagai Excel .xlsx.")
        }
        require("xl/workbook.xml" in parts) { "File bukan buku kerja Excel .xlsx." }
        val sheetPaths = parts.keys.filter {
            Regex("xl/worksheets/sheet[0-9]+\\.xml").matches(it)
        }.sortedBy { it.substringAfterLast("sheet").substringBefore(".xml").toIntOrNull() ?: Int.MAX_VALUE }
        val sheetPath = firstSheet(parts, sheetPaths)
            ?: throw IllegalArgumentException("Sheet pertama tidak ditemukan.")
        val shared = parts["xl/sharedStrings.xml"]?.let { sharedStrings(it) }.orEmpty()
        val rows = xml(parts.getValue(sheetPath)).getElementsByTagNameNS(NS, "row")
        require(rows.length > 0) { "Excel kosong. Isi template VOKASI." }
        val drafts = mutableListOf<Draft>()
        var headerFound = false
        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            val rowNum = row.getAttribute("r").toIntOrNull() ?: (i + 1)
            val values = MutableList(9) { "" }
            val cells = row.getElementsByTagNameNS(NS, "c")
            for (j in 0 until cells.length) {
                val cell = cells.item(j) as Element
                require(cell.getElementsByTagNameNS(NS, "f").length == 0) {
                    "Baris $rowNum: rumus Excel tidak didukung. Gunakan nilai."
                }
                val match = Regex("^([A-Za-z]+)[0-9]+$").matchEntire(cell.getAttribute("r"))
                    ?: throw IllegalArgumentException("Baris $rowNum: alamat sel tidak valid.")
                var col = 0
                match.groupValues[1].uppercase().forEach { col = col * 26 + (it - 'A' + 1) }
                val value = cellValue(cell, shared).trim()
                if (col !in 1..9) {
                    require(value.isBlank()) { "Baris $rowNum: gunakan hanya kolom A sampai I." }
                } else values[col - 1] = value
            }
            if (rowNum == 1) {
                require(values.map { it.lowercase() } == headers.map { it.lowercase() }) {
                    "Header tidak sesuai. Gunakan template: " + headers.joinToString(", ")
                }
                headerFound = true
                continue
            }
            if (values.all { it.isBlank() }) continue
            require(drafts.size < MAX_ROWS) { "Maksimal $MAX_ROWS soal per impor." }
            val key = values[7].uppercase()
            require(key.length == 1 && key[0] in 'A'..'D') {
                "Baris $rowNum: Kunci harus A, B, C, atau D."
            }
            drafts.add(Draft(
                rowNum, values[0], values[1].uppercase(), values[2],
                values.subList(3, 7).toList(), key[0] - 'A', values[8]
            ))
        }
        require(headerFound) { "Header Excel tidak ditemukan di baris 1." }
        require(drafts.isNotEmpty()) { "Belum ada soal. Isi baris 2 dan seterusnya." }
        return drafts
    }

    /** Validate the entire sheet and existing questions before a single storage write. */
    fun prepare(existing: List<Question>, drafts: List<Draft>): List<Question> {
        require(drafts.isNotEmpty() && drafts.size <= MAX_ROWS) { "Jumlah soal tidak valid." }
        val topId = existing.maxOfOrNull { it.id } ?: 0
        require(topId <= Int.MAX_VALUE - drafts.size) { "ID bank soal sudah penuh." }
        val bank = existing.toMutableList()
        val added = ArrayList<Question>(drafts.size)
        drafts.forEachIndexed { i, d ->
            val reason = AdminQuestionRules.error(
                bank, d.category, d.question, d.options, d.answerIndex, d.code, d.explanation
            )
            require(reason == null) { "Baris " + d.row + ": " + reason }
            val q = Question(
                id = topId + i + 1,
                category = d.category,
                question = d.question,
                options = d.options,
                answerIndex = d.answerIndex,
                difficultyCode = d.code,
                timeLimitSeconds = BankPolicy.secondsByCode.getValue(d.code),
                explanation = d.explanation,
                correctAnswer = d.options[d.answerIndex]
            )
            added.add(q)
            bank.add(q)
        }
        BankPolicy.validateBank(bank, requireExactly500 = false)
        return added
    }

    private fun firstSheet(parts: Map<String, ByteArray>, candidates: List<String>): String? {
        val workbook = parts["xl/workbook.xml"] ?: return null
        val rels = parts["xl/_rels/workbook.xml.rels"] ?: return candidates.firstOrNull()
        val sheet = xml(workbook).getElementsByTagNameNS(NS, "sheet").item(0) as? Element
            ?: return candidates.firstOrNull()
        val id = sheet.getAttributeNS(REL_NS, "id")
        val relationships = xml(rels).getElementsByTagNameNS(
            "http://schemas.openxmlformats.org/package/2006/relationships", "Relationship"
        )
        for (i in 0 until relationships.length) {
            val relation = relationships.item(i) as Element
            if (relation.getAttribute("Id") != id) continue
            val target = relation.getAttribute("Target").replace('\\', '/')
            if (".." in target.split('/')) return null
            val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
            return path.takeIf { it in candidates }
        }
        return candidates.firstOrNull()
    }

    private fun sharedStrings(bytes: ByteArray): List<String> {
        val items = xml(bytes).getElementsByTagNameNS(NS, "si")
        require(items.length <= 100_000) { "Terlalu banyak teks Excel." }
        return (0 until items.length).map { index ->
            val strings = (items.item(index) as Element).getElementsByTagNameNS(NS, "t")
            buildString { for (i in 0 until strings.length) append(strings.item(i).textContent) }
        }
    }

    private fun cellValue(cell: Element, shared: List<String>): String {
        val raw = if (cell.getAttribute("t") == "inlineStr") {
            val strings = cell.getElementsByTagNameNS(NS, "t")
            buildString { for (i in 0 until strings.length) append(strings.item(i).textContent) }
        } else {
            val values = cell.getElementsByTagNameNS(NS, "v")
            if (values.length == 0) "" else values.item(0).textContent
        }
        return if (cell.getAttribute("t") == "s") {
            val index = raw.toIntOrNull() ?: throw IllegalArgumentException("Indeks teks tidak valid.")
            shared.getOrNull(index) ?: throw IllegalArgumentException("Teks Excel tidak ditemukan.")
        } else raw
    }

    private fun xml(bytes: ByteArray): org.w3c.dom.Document {
        // Android's DocumentBuilderFactory does not support FEATURE_SECURE_PROCESSING.
        // Reject DTD/entity declarations before parsing, and forbid external resolution.
        require(!(bytes.size >= 2 &&
            ((bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) ||
                (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())))) {
            "XML UTF-16 tidak didukung. Simpan kembali sebagai Excel .xlsx."
        }
        val source = bytes.toString(Charsets.UTF_8)
        require(!source.contains("<!DOCTYPE", ignoreCase = true) &&
            !source.contains("<!ENTITY", ignoreCase = true)) {
            "File Excel mengandung deklarasi XML yang tidak aman."
        }
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            factory.isExpandEntityReferences = false
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ ->
                throw org.xml.sax.SAXException("External XML entities are not allowed")
            }
            return builder.parse(ByteArrayInputStream(bytes))
        } catch (_: Exception) {
            throw IllegalArgumentException("Format XML Excel tidak didukung atau tidak aman.")
        }
    }
}
