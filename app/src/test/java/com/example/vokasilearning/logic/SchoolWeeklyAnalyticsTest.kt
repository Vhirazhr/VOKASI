package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SchoolWeeklyAnalyticsTest {
    private fun millis(value: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }.parse(value)!!.time

    private fun attempt(id: String, date: String, score: Int, category: String? = null) =
        Attempt(id, millis(date), id + date, score, 100, 100, score, 50, 1400,
            practiceCategory = category)

    private val students = listOf(
        Student("a", "pw", "A", Classroom("X TPM 1", "Pemesinan")),
        Student("b", "pw", "B", Classroom("X TPM 1", "Pemesinan")),
        Student("c", "pw", "C", Classroom("XI TAB 1", "Alat Berat"))
    )

    @Test fun weekStartsMondayAndSundayBelongsToSameWibWeek() {
        assertEquals("2026-10-19" to "2026-10-25", SchoolAnalytics.weekRange("2026-10-23"))
        assertEquals("2026-10-19" to "2026-10-25", SchoolAnalytics.weekRange("2026-10-25"))
        assertEquals("2026-12-28" to "2027-01-03", SchoolAnalytics.weekRange("2027-01-01"))
    }

    @Test fun weeklyGradesUseLatestCompulsoryAndComparePreviousWeek() {
        val records = listOf(
            attempt("a", "2026-10-15 08:00", 25),
            attempt("b", "2026-10-14 08:00", 70),
            attempt("a", "2026-10-21 08:00", 30),
            attempt("a", "2026-10-23 08:00", 50),
            attempt("b", "2026-10-22 08:00", 50),
            attempt("c", "2026-10-23 10:00", 90),
            attempt("a", "2026-10-23 11:00", 100, "Perkalian")
        )
        val stats = SchoolAnalytics.calculateWeekly(
            students, records, SchoolScope(), "2026-10-23"
        )
        assertEquals(3, stats.completed)
        assertEquals(63, stats.average)
        assertEquals(1, stats.high)
        assertEquals(0, stats.middle)
        assertEquals(2, stats.low)
        assertEquals(47, stats.previousAverage)
        assertEquals(1, stats.improved)
        assertEquals(1, stats.declined)
        assertEquals(0, stats.stable)
        assertEquals(7, stats.trend.size)
        assertEquals("2026-10-19", stats.trend.first().first)
        assertEquals("2026-10-25", stats.trend.last().first)
        assertEquals(70, stats.trend[4].second)
    }

    @Test fun noComparablePreviousWeekDoesNotFabricateChanges() {
        val stats = SchoolAnalytics.calculateWeekly(students,
            listOf(attempt("a", "2026-10-23 10:00", 30)), SchoolScope(), "2026-10-23"
        )
        assertEquals(0, stats.improved + stats.declined + stats.stable)
        assertNull(stats.previousAverage)
        assertEquals(30, stats.average)
    }
}
