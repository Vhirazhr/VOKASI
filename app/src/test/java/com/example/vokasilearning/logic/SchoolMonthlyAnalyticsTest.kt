package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SchoolMonthlyAnalyticsTest {
    private fun millis(dayTime: String) =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.parse(dayTime)!!.time

    private fun attempt(id: String, whenWib: String, score: Int, subject: String? = null) =
        Attempt(id, millis(whenWib), id + whenWib, score, 100, 100,
            score, 20, 1000, practiceCategory = subject)

    private val students = listOf(
        Student("a", "pw", "A", Classroom("X TPM 1", "Pemesinan")),
        Student("b", "pw", "B", Classroom("X TPM 2", "Pemesinan")),
        Student("c", "pw", "C", Classroom("XI TE 1", "Elektronika")),
        Student("d", "pw", "D", Classroom("XII TE 1", "Elektronika")),
        Student("e", "pw", "E", Classroom("XII TPM 1", "Pemesinan"))
    )

    @Test fun selectedMonthUsesOnlyLatestCompulsoryGradePerStudent() {
        val attempts = listOf(
            attempt("a", "2026-09-10 08:00", 70),
            attempt("b", "2026-09-10 08:00", 80),
            attempt("c", "2026-09-10 08:00", 90),
            attempt("a", "2026-10-01 08:00", 85),
            attempt("a", "2026-10-21 08:00", 95),
            attempt("a", "2026-10-21 09:00", 100, "Perkalian"),
            attempt("b", "2026-10-21 08:00", 60),
            attempt("c", "2026-10-15 08:00", 90)
        )
        val stats = SchoolAnalytics.calculateMonthly(students, attempts, SchoolScope(), 2026, 10)
        assertEquals(5, stats.students)
        assertEquals(3, stats.completed)
        assertEquals(2, stats.missing)
        assertEquals(81, stats.average)
        assertEquals(2, stats.high)
        assertEquals(1, stats.middle)
        assertEquals(0, stats.low)
        assertEquals(80, stats.previousAverage)
        assertEquals(1, stats.improved)
        assertEquals(1, stats.declined)
        assertEquals(1, stats.stable)
        assertEquals(6, stats.trend.size)
        assertEquals("2026-09", stats.trend[4].first)
        assertEquals(80, stats.trend[4].second)
        assertEquals("2026-10", stats.trend[5].first)
        assertEquals(81, stats.trend[5].second)
        assertNull(stats.trend[0].second)
    }

    @Test fun sameNamedParallelClassesAreScopedByMajorAndLevel() {
        val another = Student("z", "pw", "Z", Classroom("X TPM 1", "Lainnya"))
        val records = listOf(
            attempt("a", "2026-10-03 08:00", 80),
            attempt("z", "2026-10-03 08:00", 20)
        )
        val s = SchoolAnalytics.calculateMonthly(students + another, records,
            SchoolScope(major = "Pemesinan", grade = "X", classroom = "X TPM 1"), 2026, 10)
        assertEquals(1, s.students)
        assertEquals(1, s.completed)
        assertEquals(80, s.average)
    }

    @Test fun januaryComparesToPreviousDecemberNotFollowingMonth() {
        val records = listOf(
            attempt("a", "2025-12-30 08:00", 45),
            attempt("a", "2026-01-05 08:00", 85)
        )
        val s = SchoolAnalytics.calculateMonthly(students, records, SchoolScope(), 2026, 1)
        assertEquals(1, s.completed)
        assertEquals(85, s.average)
        assertEquals(45, s.previousAverage)
        assertEquals(1, s.improved)
        assertEquals("2025-12", s.trend[4].first)
        assertEquals("2026-01", s.trend[5].first)
    }

    @Test fun emptyMonthIsNotMisrepresentedAsZeroScore() {
        val s = SchoolAnalytics.calculateMonthly(students,
            listOf(attempt("a", "2026-09-01 08:00", 90)), SchoolScope(), 2026, 10)
        assertEquals(0, s.completed)
        assertEquals(5, s.missing)
        assertNull(s.average)
        assertEquals(0, s.improved)
        assertNull(s.trend.last().second)
    }
}
