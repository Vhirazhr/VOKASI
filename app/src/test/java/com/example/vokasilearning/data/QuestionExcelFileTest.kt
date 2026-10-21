package com.example.vokasilearning.data

import com.example.vokasilearning.logic.BankPolicy
import com.example.vokasilearning.model.Question
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuestionExcelFileTest {
    private val row = listOf(
        "Perkalian", "K1", "12 x 3 = ...?", "30", "36", "38", "39", "B", "12 dikali 3 adalah 36."
    )

    private fun file(rows: List<List<String>>): ByteArray {
        val template = ByteArrayOutputStream().also { QuestionExcelFile.writeTemplate(it) }.toByteArray()
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { target ->
            ZipInputStream(ByteArrayInputStream(template)).use { source ->
                while (true) {
                    val entry = source.nextEntry ?: break
                    var bytes = source.readBytes()
                    if (entry.name == "xl/worksheets/sheet1.xml") {
                        val base = bytes.toString(Charsets.UTF_8)
                        val addedRows = rows.mapIndexed { index, cells ->
                            val r = index + 2
                            "<row r=\"$r\">" + cells.mapIndexed { i, value ->
                                val escaped = value.replace("&", "&amp;").replace("<", "&lt;")
                                    .replace(">", "&gt;").replace("\"", "&quot;")
                                "<c r=\"" + ('A' + i) + r +
                                    "\" t=\"inlineStr\"><is><t>" + escaped + "</t></is></c>"
                            }.joinToString("") + "</row>"
                        }.joinToString("")
                        bytes = base.replace("</sheetData>", "$addedRows</sheetData>")
                            .toByteArray(Charsets.UTF_8)
                    }
                    target.putNextEntry(ZipEntry(entry.name))
                    target.write(bytes)
                    target.closeEntry()
                    source.closeEntry()
                }
            }
        }
        return out.toByteArray()
    }

    private fun read(rows: List<List<String>>) =
        QuestionExcelFile.read(ByteArrayInputStream(file(rows)))

    private fun existing(): List<Question> {
        var id = 0
        return BankPolicy.quotas.flatMap { (code, quantity) ->
            (1..quantity).map {
                id++
                val category = when (code[0]) {
                    'K' -> "Perkalian"
                    'P' -> "Pembagian"
                    'J' -> "Penjumlahan"
                    else -> "Konversi Satuan"
                }
                Question(id, category, "Soal lama nomor $id?",
                    listOf("A", "B", "C", "D"), 1, code,
                    BankPolicy.secondsByCode.getValue(code), "Pembahasan lama.", "B")
            }
        }
    }

    private fun expectedError(block: () -> Unit, text: String) {
        try {
            block()
            fail("Seharusnya ditolak: $text")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains(text))
        }
    }

    @Test fun templateIsRealXlsxWithRequiredHeader() {
        val out = ByteArrayOutputStream()
        QuestionExcelFile.writeTemplate(out)
        val data = out.toByteArray()
        assertTrue(data.size > 400)
        assertEquals('P'.code.toByte(), data[0])
        assertEquals('K'.code.toByte(), data[1])
        expectedError({ QuestionExcelFile.read(ByteArrayInputStream(data)) }, "Belum ada soal")
    }

    @Test fun correctlyImportsRowsAssignsUniqueIdsAndCodes() {
        val base = existing()
        val drafts = read(listOf(row))
        assertEquals(2, drafts.first().row)
        val added = QuestionExcelFile.prepare(base, drafts)
        assertEquals(1, added.size)
        assertEquals(base.last().id + 1, added[0].id)
        assertEquals(1, added[0].answerIndex)
        assertEquals("36", added[0].correctAnswer)
        assertEquals(8, added[0].timeLimitSeconds)
        assertEquals(base.size, existing().size)
    }

    @Test fun invalidRowRejectsWholeBatchAndShowsRowNumber() {
        val bad = row.copyWithQuestion("15 x 4 = ...?").toMutableList().apply {
            set(1, "P5")
        }
        val drafts = read(listOf(row, bad))
        expectedError({ QuestionExcelFile.prepare(existing(), drafts) },
            "Baris 3: Materi dan tingkat kesulitan tidak sesuai")
    }

    @Test fun rejectsDuplicatesWithinImportAndExistingBank() {
        expectedError({ QuestionExcelFile.prepare(existing(), read(listOf(row, row))) },
            "Baris 3: Pertanyaan ini sudah ada")
        val matchOld = row.copyWithQuestion("Soal lama nomor 1?")
        expectedError({ QuestionExcelFile.prepare(existing(), read(listOf(matchOld))) },
            "Baris 2: Pertanyaan ini sudah ada")
    }

    @Test fun invalidAnswerKeyShowsExcelLine() {
        val badKey = row.toMutableList().apply { set(7, "E") }
        expectedError({ read(listOf(badKey)) }, "Baris 2: Kunci")
    }

    private fun List<String>.copyWithQuestion(value: String): List<String> =
        toMutableList().apply { set(2, value) }
}
