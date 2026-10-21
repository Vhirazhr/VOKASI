package com.example.vokasilearning.data

import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class SchoolRosterExcelTest {
    @Test fun exportIncludesStatusAndNeverIncludesCredentialHash() {
        val entries = listOf(
            SchoolRosterEntry(1, "Dewi Rahma", "X TPM 1", "Pemesinan", 2026),
            SchoolRosterEntry(2, "Andi Pratama", "X TPM 1", "Pemesinan", 2026,
                "andip", "pbkdf2\$salt\$SECRET_HASH")
        )
        val bytes = ByteArrayOutputStream().also { SchoolRosterExcel.write(it, entries) }.toByteArray()
        val xml = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var sheet = ""
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet1.xml") sheet = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
            sheet
        }
        assertTrue(xml.contains("Belum Terdaftar"))
        assertTrue(xml.contains("Terdaftar"))
        assertTrue(xml.contains("Dewi Rahma"))
        assertTrue(xml.contains("Andi Pratama"))
        assertTrue(xml.contains("2026"))
        assertFalse(xml.contains("SECRET_HASH"))
    }
}
