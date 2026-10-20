package com.example.vokasilearning.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {
    @Test fun increasingXpThresholds() {
        assertEquals(1, levelFor(0).level)
        assertEquals(2, levelFor(100).level)
        assertEquals(3, levelFor(250).level)
        assertTrue(levelFor(250).nextXp - 250 > levelFor(0).nextXp)
    }
    @Test fun scoreBands() {
        assertEquals("Sangat Baik", scoreGrade(100))
        assertEquals("Perlu Latihan", scoreGrade(50))
    }
    @Test fun completionMessagesMatchRealResultAndXp() {
        val great = completionNotice(95, 95, 100, 20, 4)
        assertTrue(great.contains("Luar biasa"))
        assertTrue(great.contains("95/100"))
        assertTrue(great.contains("+20 XP"))
        assertTrue(great.contains("Streak 4 hari"))

        val repeatPractice = completionNotice(50, 5, 10, 0, 0)
        assertTrue(repeatPractice.contains("5/10"))
        assertTrue(repeatPractice.contains("Belum ada XP tambahan"))
        assertTrue(repeatPractice.contains("Mulai streak pertamamu"))

        val good = completionNotice(80, 8, 10, 5, 1)
        assertTrue(good.contains("Kerja bagus"))
        assertTrue(good.contains("+5 XP"))
    }

}
