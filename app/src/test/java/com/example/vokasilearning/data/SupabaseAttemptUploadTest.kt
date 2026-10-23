package com.example.vokasilearning.data

import com.example.vokasilearning.model.Attempt
import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseAttemptUploadTest {
    private fun session(
        username: String, time: Long, session: String, count: Int = 100
    ) = Attempt(
        studentId = username, timeMillis = time, signature = "test",
        correct = 30, answered = count, questionCount = count,
        score = if (count == 100) 30 else 100, earnedXp = 50, secondsSpent = 180,
        sessionKey = session
    )

    @Test fun newestSchoolResultIsAlwaysAttemptedBeforeOldLocalBacklog() {
        val recent = "00000000-0000-4000-8000-000000000002"
        val old = "00000000-0000-4000-8000-000000000001"
        val local = listOf(
            session("abdul", 1000, old),
            session(" ABDUL ", 3000, recent)
        )
        val pending = SupabaseAttemptRepository.eligibleUploads(
            "abdul", local, emptySet()
        )
        assertEquals(listOf(recent, old), pending.map { it.sessionKey })
    }

    @Test fun rejectsCorruptOrAlreadyStoredSessionsWithoutBlockingOtherStudent() {
        val valid = "00000000-0000-4000-8000-000000000003"
        val stored = "00000000-0000-4000-8000-000000000004"
        val local = listOf(
            session("abdul", 1000, "legacy_invalid"),
            session("abdul", 2000, stored),
            session("other", 3000, "00000000-0000-4000-8000-000000000005"),
            session("abdul", 4000, valid),
            session("abdul", 4000, valid)
        )
        val pending = SupabaseAttemptRepository.eligibleUploads(
            "abdul", local, setOf(stored)
        )
        assertEquals(listOf(valid), pending.map { it.sessionKey })
    }
}
