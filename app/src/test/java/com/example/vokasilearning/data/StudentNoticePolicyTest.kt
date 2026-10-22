package com.example.vokasilearning.data

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StudentNoticePolicyTest {
    private val student = Student(
        username = "test.siswa", password = "", name = "Test Siswa",
        classroom = Classroom("XI TPM 1", "Teknik Pemesinan")
    )
    private fun at(daysBefore: Int): Long = Calendar.getInstance(
        TimeZone.getTimeZone("Asia/Jakarta")
    ).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysBefore)
    }.timeInMillis

    private fun attempt(daysBefore: Int, category: String? = null) = Attempt(
        studentId = student.username, timeMillis = at(daysBefore),
        signature = "test", correct = 1, answered = 1, questionCount = 1,
        score = 100, earnedXp = 10, secondsSpent = 10,
        practiceCategory = category
    )

    @Test fun newStudentGetsOnlyWelcome() {
        val notices = StudentNoticeStore.forStudent(student, emptyList(), true, at(0))
        assertEquals(listOf("welcome"), notices.map { it.key })
    }

    @Test fun yesterdayActivityTriggersStreakOnlyWhenEnabled() {
        val yesterday = listOf(attempt(1))
        val enabled = StudentNoticeStore.forStudent(student, yesterday, true, at(0))
        assertTrue(enabled.any { it.key.startsWith("streak_") })
        val disabled = StudentNoticeStore.forStudent(student, yesterday, false, at(0))
        assertFalse(disabled.any { it.key.startsWith("streak_") })
    }

    @Test fun oldBrokenStreakDoesNotGetFalseUrgentReminder() {
        val notices = StudentNoticeStore.forStudent(student, listOf(attempt(4)), true, at(0))
        assertFalse(notices.any { it.key.startsWith("streak_") })
    }

    @Test fun alreadyPracticedTodayGetsNoStreakReminder() {
        val notices = StudentNoticeStore.forStudent(
            student, listOf(attempt(1), attempt(0)), true, at(0))
        assertFalse(notices.any { it.key.startsWith("streak_") })
        assertFalse(notices.any { it.key.startsWith("daily_") })
    }

    @Test fun optionalPracticeDoesNotClaimRequiredPracticeComplete() {
        val notices = StudentNoticeStore.forStudent(
            student, listOf(attempt(0, "Penjumlahan")), true, at(0))
        assertTrue(notices.any { it.key.startsWith("daily_") })
        assertFalse(notices.any { it.key.startsWith("streak_") })
    }

    @Test fun noDuplicateAdminSuccessNotification() {
        val notifications = StudentNoticeStore.forAdmin(
            SchoolDirectory(
                majors = listOf("Teknik Pemesinan"),
                classes = listOf(Classroom("XI TPM 1", "Teknik Pemesinan")),
                roster = listOf(SchoolRosterEntry(
                    id = 1L, fullName = "Test Siswa", classLabel = "XI TPM 1",
                    major = "Teknik Pemesinan", admissionYear = 2025,
                    remoteRegistered = true
                ))
            )
        )
        assertTrue(notifications.isEmpty())
    }
}
