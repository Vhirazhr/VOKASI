package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AdminAnalyticsTest {
    private val students = listOf(
        Student("a", "demo", "Alya", Classroom("X TPM 1", "Teknik Pemesinan")),
        Student("b", "demo", "Bima", Classroom("X TPM 1", "Teknik Pemesinan")),
        Student("c", "demo", "Cici", Classroom("X TE 1", "Teknik Elektronika"))
    )

    private fun date(text: String) =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.parse(text)!!.time

    private fun attempt(id: String, date: String, score: Int, category: String? = null) =
        Attempt(id, date(date), date, score, 100, 100, score, 20, 900,
            practiceCategory = category)

    @Test fun requiredDailyGradeExcludesPracticeAndUsesLatestCompulsory() {
        val runs = listOf(
            attempt("a", "2026-10-21 08:00", 50),
            attempt("a", "2026-10-21 09:00", 80),
            attempt("a", "2026-10-21 10:00", 100, "Perkalian"),
            attempt("b", "2026-10-20 13:00", 90),
            attempt("c", "2026-10-21 11:00", 100)
        )
        val date = "2026-10-21"
        val latest = AdminAnalytics.latestCompulsoryByStudent(runs, date)
        assertEquals(80, latest.getValue("a").score)
        assertEquals(100, latest.getValue("c").score)
        assertEquals(false, latest.containsKey("b"))
        val summary = AdminAnalytics.classSummary("X TPM 1", students, runs, date)
        assertEquals(2, summary.studentCount)
        assertEquals(1, summary.completedCount)
        assertEquals(1, summary.missingCount)
        assertEquals(80, summary.averageScore)
        assertEquals(2, summary.sessions)
    }

    @Test fun emptyDayShowsNoAverageRatherThanFalseZero() {
        val summary = AdminAnalytics.classSummary("X TPM 1", students, emptyList(), "2026-10-21")
        assertEquals(0, summary.completedCount)
        assertEquals(2, summary.missingCount)
        assertNull(summary.averageScore)
        assertEquals(0, summary.sessions)
    }

    @Test fun allClassesOnlyIncludesRegisteredClasses() {
        val overview = AdminAnalytics.allClasses(students, emptyList(), "2026-10-21")
        assertEquals(listOf("X TE 1", "X TPM 1"), overview.map { it.classroom })
        assertEquals(listOf(1, 2), overview.map { it.studentCount })
    }
    @Test fun duplicateClassLabelsFromDifferentMajorsAreNeverCombined() {
        val a = Student("ti", "demo", "Tina", Classroom("X 1", "Teknik Industri"))
        val b = Student("ak", "demo", "Andra", Classroom("X 1", "Akuntansi"))
        val dated = listOf(
            attempt("ti", "2026-10-21 09:00", 90),
            attempt("ak", "2026-10-21 10:00", 50),
            attempt("ak", "2026-10-21 11:00", 100, "Perkalian")
        )
        val classes = AdminAnalytics.allClasses(listOf(a, b), dated, "2026-10-21")
        assertEquals(2, classes.size)
        val industrial = classes.single { it.major == "Teknik Industri" }
        val accounting = classes.single { it.major == "Akuntansi" }
        assertEquals(1, industrial.studentCount)
        assertEquals(90, industrial.averageScore)
        assertEquals(1, accounting.studentCount)
        assertEquals(50, accounting.averageScore)
        assertEquals(1, accounting.sessions)
        assertEquals(50, AdminAnalytics.classSummary(
            "X 1", listOf(a, b), dated, "2026-10-21", major = "Akuntansi"
        ).averageScore)
    }

}
