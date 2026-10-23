package com.example.vokasilearning.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminAttemptIdentityTest {
    private val roster = listOf(
        SchoolRosterEntry(
            id = 10,
            fullName = "Abdul Rizal",
            classLabel = "X TPM 1",
            major = "Teknik Pemesinan",
            admissionYear = 2026,
            username = "abdul_rizal",
            remoteId = "roster-1",
            remoteRegistered = true,
            remoteAuthUserId = "7D9ECA7E-1111-4444-AAAA-123456789012"
        ),
        SchoolRosterEntry(
            id = 20,
            fullName = "Siswa Kedua",
            classLabel = "X TPM 2",
            major = "Teknik Pemesinan",
            admissionYear = 2026,
            username = "siswa_kedua",
            remoteRegistered = true,
            remoteAuthUserId = "ed5e9d50-2222-4444-bbbb-123456789012"
        )
    )

    @Test fun twoDistinctSupabaseAuthAccountsNeverCollapseIntoOneStudent() {
        val mapping = AdminAttemptIdentity.rosterNames(roster)
        assertEquals("abdul_rizal", AdminAttemptIdentity.resolve(
            "7d9eca7e-1111-4444-aaaa-123456789012", "old_abdul_alias", mapping
        ))
        assertEquals("siswa_kedua", AdminAttemptIdentity.resolve(
            "ED5E9D50-2222-4444-BBBB-123456789012", "other_display_name", mapping
        ))
        assertEquals(2, listOf(
            AdminAttemptIdentity.resolve(
                "7d9eca7e-1111-4444-aaaa-123456789012", "old_abdul_alias", mapping
            ),
            AdminAttemptIdentity.resolve(
                "ed5e9d50-2222-4444-bbbb-123456789012", "other_display_name", mapping
            )
        ).distinct().size)
    }

    @Test fun unregisteredAndMissingAuthRowsUseOriginalUsernameWithoutDroppingGrade() {
        val mapping = AdminAttemptIdentity.rosterNames(roster +
            SchoolRosterEntry(
                id = 30, fullName = "Belum Terdaftar",
                classLabel = "X TPM 1", major = "Teknik Pemesinan",
                admissionYear = 2026, username = "", remoteRegistered = false,
                remoteAuthUserId = "unregistered-uuid"
            )
        )
        assertEquals("old_username", AdminAttemptIdentity.resolve(
            "unknown-user", " old_username ", mapping
        ))
        assertEquals(false, mapping.containsKey("unregistered-uuid"))
    }
}
