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
}
