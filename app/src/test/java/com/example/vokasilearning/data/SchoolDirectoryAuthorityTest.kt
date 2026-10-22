package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolDirectoryAuthorityTest {
    @Test
    fun oldLocalStudentsAreNotRestoredAfterSuccessfulSupabaseRefresh() {
        val remote = SchoolDirectory(
            majors = listOf("Teknik Pemesinan"),
            classes = listOf(Classroom("XI TPM 1", "Teknik Pemesinan")),
            roster = listOf(SchoolRosterEntry(
                id = 12L,
                fullName = "Siswa Server",
                classLabel = "XI TPM 1",
                major = "Teknik Pemesinan",
                admissionYear = 2025,
                username = "siswa.server",
                remoteId = "00000000-0000-0000-0000-000000000012",
                remoteRegistered = true
            ))
        )
        val local = SchoolDirectory(
            majors = listOf("Jurusan Demo"),
            classes = listOf(Classroom("X DEMO", "Jurusan Demo")),
            roster = listOf(SchoolRosterEntry(
                id = 1L,
                fullName = "Siswa Lama",
                classLabel = "X DEMO",
                major = "Jurusan Demo",
                admissionYear = 2024,
                username = "demo",
                passwordHash = "legacy"
            ))
        )
        val merged = SupabaseSchoolRepository.mergeRemoteWithLocal(remote, local)
        assertEquals(remote, merged)
        assertEquals(1, merged.roster.size)
        assertTrue(merged.roster.none { it.fullName == "Siswa Lama" })
    }

    @Test
    fun successfulEmptyServerDirectoryMustNotReintroduceLocalData() {
        val local = SchoolDirectory(
            roster = listOf(SchoolRosterEntry(
                id = 1L, fullName = "Siswa Lama", classLabel = "X DEMO",
                major = "Jurusan Demo", admissionYear = 2024
            ))
        )
        assertEquals(
            SchoolDirectory(),
            SupabaseSchoolRepository.mergeRemoteWithLocal(SchoolDirectory(), local)
        )
    }
}
