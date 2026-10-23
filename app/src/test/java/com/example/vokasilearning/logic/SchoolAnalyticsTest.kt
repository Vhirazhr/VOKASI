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

class SchoolAnalyticsTest {
    private val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }
    private fun run(id: String, day: String, score: Int, optional: String? = null) =
        Attempt(id, dateParser.parse("$day 08:00")!!.time, "test_$id$day", score,
            100, 100, score, 20, 1200, practiceCategory = optional)
    private val students = listOf(
        Student("a", "pass", "A", Classroom("X TPM 1", "Teknik Pemesinan")),
        Student("b", "pass", "B", Classroom("X TPM 2", "Teknik Pemesinan")),
        Student("c", "pass", "C", Classroom("XI TPM 1", "Teknik Pemesinan")),
        Student("d", "pass", "D", Classroom("X TE 1", "Teknik Elektronika")),
        Student("e", "pass", "E", Classroom("XII TE 1", "Teknik Elektronika"))
    )
    private val results = listOf(
        run("a", "2026-10-20", 50),
        run("b", "2026-10-20", 70),
        run("c", "2026-10-20", 90),
        run("d", "2026-10-20", 80),
        run("a", "2026-10-21", 80),
        run("b", "2026-10-21", 65),
        run("c", "2026-10-21", 90),
        run("d", "2026-10-21", 100, "Perkalian")
    )

    @Test fun defaultIsWholeSchoolNotFirstClass() {
        val report = SchoolAnalytics.calculate(students, results, SchoolScope(), "2026-10-21")
        assertEquals(5, report.students)
        assertEquals(5, report.classes)
        assertEquals(2, report.majors)
        assertEquals(3, report.completed)
        assertEquals(2, report.missing)
        assertEquals(78, report.average)
        assertEquals(2, report.high)
        assertEquals(1, report.middle)
        assertEquals(0, report.low)
        assertEquals(1, report.improved)
        assertEquals(1, report.declined)
        assertEquals(1, report.stable)
    }

    @Test fun xXiXiiAndParallelClassesFilterSeparately() {
        assertEquals("X", schoolGrade("X TPM 2"))
        assertEquals("XI", schoolGrade("XI TPM 1"))
        assertEquals("XII", schoolGrade("XII TE 1"))
        assertEquals(3, SchoolAnalytics.members(students,
            SchoolScope(major = "Teknik Pemesinan")).size)
        assertEquals(2, SchoolAnalytics.members(students,
            SchoolScope(major = "Teknik Pemesinan", grade = "X")).size)
        assertEquals(1, SchoolAnalytics.members(students,
            SchoolScope("Teknik Pemesinan", "X", "X TPM 2")).size)
    }

    @Test fun filterWithNoDataHasNoFabricatedAverageOrGrowth() {
        val report = SchoolAnalytics.calculate(students, results,
            SchoolScope(major = "Teknik Elektronika", grade = "XII"), "2026-10-21")
        assertEquals(1, report.students)
        assertEquals(0, report.completed)
        assertNull(report.average)
        assertEquals(0, report.improved)
    }

    @Test fun selectedReportDateControlsStatsAndSevenConsecutiveDays() {
        val on21 = SchoolAnalytics.calculate(students, results, SchoolScope(), "2026-10-21")
        assertEquals(3, on21.completed)
        assertEquals(78, on21.average)
        assertEquals(7, on21.trend.size)
        assertEquals("2026-10-15", on21.trend.first().first)
        assertEquals("2026-10-21", on21.trend.last().first)
        assertEquals(73, on21.trend[5].second)
        assertEquals(78, on21.trend[6].second)
        assertNull(on21.trend[0].second)
        val on20 = SchoolAnalytics.calculate(students, results, SchoolScope(), "2026-10-20")
        assertEquals(4, on20.completed)
        assertEquals(72, on20.average)
        assertEquals(0, on20.improved + on20.declined + on20.stable)
        assertEquals("2026-10-20", on20.trend.last().first)
    }

    @Test fun gradeDistributionIsForSelectedDateAndUsernameMatchesCaseInsensitive() {
        val pupil = Student("Abdul_Rizal", "demo", "Abdul Rizal",
            Classroom("X TPM 1", "Teknik Pemesinan"))
        val records = listOf(
            run(" ABDUL_RIZAL ", "2026-10-22", 30),
            run("abdul_rizal", "2026-10-23", 50),
            run("Abdul_Rizal", "2026-10-23", 100, "Perkalian")
        )
        val before = SchoolAnalytics.calculate(listOf(pupil), records, SchoolScope(),
            "2026-10-22")
        assertEquals(30, before.average)
        assertEquals(1, before.low)
        val after = SchoolAnalytics.calculate(listOf(pupil), records, SchoolScope(),
            "2026-10-23")
        assertEquals(50, after.average)
        assertEquals(1, after.completed)
        assertEquals(1, after.low)
        assertEquals(1, after.improved)
        assertEquals(0, after.declined)
        assertEquals(30, after.previousAverage)
    }

    @Test fun repeatsOnSameDayCountOnceForParticipation() {
        val extra = results + run("a", "2026-10-21", 95)
        val report = SchoolAnalytics.calculate(students, extra, SchoolScope(), "2026-10-21")
        assertEquals(3, report.completed)
        assertEquals(1, report.improved)
    }
}
