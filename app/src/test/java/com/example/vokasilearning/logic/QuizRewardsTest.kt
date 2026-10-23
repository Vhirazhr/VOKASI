package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class QuizRewardsTest {
    private fun time(value: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }.parse(value)!!.time

    private fun done(username: String, date: String, xp: Int, category: String? = null) =
        Attempt(username, time(date), "bank", 30, 100, 100, 30, xp, 1000,
            sessionKey = username + date, practiceCategory = category)

    @Test fun firstCompulsoryEarns50AndSubsequentAttemptCannotEarnAgain() {
        val now = time("2026-10-23 09:00")
        assertEquals(50, QuizRewards.earnedXp("abdul", emptyList(), now, "first", null, 30))
        val prior = listOf(done("abdul", "2026-10-23 07:00", 50))
        assertEquals(0, QuizRewards.earnedXp("abdul", prior, now, "second", null, 80))
    }

    @Test fun repeatAfterPreviousZeroPointCompulsoryStillEarnsZero() {
        val prior = listOf(done("abdul", "2026-10-23 07:00", 0))
        assertEquals(0, QuizRewards.earnedXp(
            "abdul", prior, time("2026-10-23 12:00"), "repeat", null, 95
        ))
    }

    @Test fun rewardResetsAtNextWibMidnightAndIsIsolatedPerStudent() {
        val before = listOf(done("abdul", "2026-10-22 23:50", 50))
        assertEquals(50, QuizRewards.earnedXp(
            "abdul", before, time("2026-10-23 00:05"), "today", null, 20
        ))
        assertEquals(50, QuizRewards.earnedXp(
            "rizal", before, time("2026-10-22 23:55"), "today", null, 10
        ))
    }

    @Test fun firstCompulsoryCanBeZeroScoreWithoutLosingCompletionReward() {
        assertEquals(50, QuizRewards.earnedXp(
            "abdul", emptyList(), time("2026-10-23 10:00"), "first", null, 0
        ))
    }

    @Test fun optionalPracticeBonusRemainsSeparate() {
        val prior = listOf(done("abdul", "2026-10-23 07:00", 50))
        val now = time("2026-10-23 10:00")
        assertEquals(5, QuizRewards.earnedXp("abdul", prior, now, "perkalian1", "Perkalian", 5))
        assertEquals(0, QuizRewards.earnedXp(
            "abdul", prior + done("abdul", "2026-10-23 08:00", 5, "Perkalian"),
            now, "perkalian2", "Perkalian", 9
        ))
    }
}
