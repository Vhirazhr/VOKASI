package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentHistoryFilterTest {
    private fun timestamp(wib: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            isLenient = false
        }.parse(wib)!!.time

    private fun attempt(date: String, practice: String? = null): Attempt =
        Attempt(
            studentId = "siswa1",
            timeMillis = timestamp(date),
            signature = date,
            correct = 8,
            answered = 10,
            questionCount = 10,
            score = 80,
            earnedXp = 5,
            secondsSpent = 70,
            practiceCategory = practice,
            sessionKey = date
        )

    @Test fun allHistoryAlwaysNewestFirst() {
        val older = attempt("2025-01-01 06:00")
        val newer = attempt("2026-10-21 07:00", "Perkalian")
        val middle = attempt("2026-10-20 23:59")
        assertEquals(listOf(newer, middle, older),
            StudentHistoryFilter.filter(listOf(older, newer, middle), HistorySelection()))
    }

    @Test fun dateAndTypeCanBeCombined() {
        val required = attempt("2026-10-20 15:00")
        val practice = attempt("2026-10-20 16:00", "Pembagian")
        val differentDay = attempt("2026-10-21 00:01", "Pembagian")
        val all = listOf(required, practice, differentDay)
        val date = HistorySelection(
            type = HistoryType.PRACTICE,
            period = HistoryPeriod.EXACT_DATE,
            date = "2026-10-20"
        )
        assertEquals(listOf(practice), StudentHistoryFilter.filter(all, date))
        assertEquals(listOf(required), StudentHistoryFilter.filter(
            all, date.copy(type = HistoryType.REQUIRED)
        ))
    }

    @Test fun monthYearAndYearFilterWork() {
        val old = attempt("2025-10-20 10:00")
        val october = attempt("2026-10-21 01:00")
        val november = attempt("2026-11-02 12:00")
        val history = listOf(old, october, november)
        assertEquals(listOf(october), StudentHistoryFilter.filter(history,
            HistorySelection(period = HistoryPeriod.MONTH_YEAR, month = 10, year = 2026)
        ))
        assertEquals(listOf(november, october), StudentHistoryFilter.filter(history,
            HistorySelection(period = HistoryPeriod.YEAR, year = 2026)
        ))
    }

    @Test fun recentSevenDaysRespectJakartaMidnight() {
        val now = timestamp("2026-10-21 00:05")
        val today = attempt("2026-10-21 00:01")
        val firstIncluded = attempt("2026-10-15 00:00")
        val previousDay = attempt("2026-10-14 23:59")
        val selection = HistorySelection(period = HistoryPeriod.LAST_SEVEN_DAYS)
        assertEquals(listOf(today, firstIncluded), StudentHistoryFilter.filter(
            listOf(previousDay, firstIncluded, today), selection, now
        ))
        assertEquals(listOf(today), StudentHistoryFilter.filter(
            listOf(previousDay, firstIncluded, today),
            selection.copy(period = HistoryPeriod.TODAY), now
        ))
        assertEquals(listOf(today, firstIncluded, previousDay), StudentHistoryFilter.filter(
            listOf(previousDay, firstIncluded, today),
            selection.copy(period = HistoryPeriod.THIS_MONTH), now
        ))
    }
}
