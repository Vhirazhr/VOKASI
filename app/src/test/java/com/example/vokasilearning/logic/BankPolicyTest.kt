package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BankPolicyTest {
    private fun syntheticBank(): List<Question> {
        var nextId = 1
        // The original CSV has 5 groups of the per-session quota = 500.
        return BankPolicy.quotas.entries.flatMap { (code, quota) ->
            (1..(quota * 5)).map { n ->
                val answer = "${nextId + 5000}"
                Question(
                    id = nextId++,
                    category = code,
                    question = "Pengujian soal $code nomor $n",
                    options = listOf(answer, "A$n", "B$n", "C$n"),
                    answerIndex = 0,
                    difficultyCode = code,
                    timeLimitSeconds = BankPolicy.secondsByCode.getValue(code),
                    correctAnswer = answer
                )
            }
        }
    }

    @Test
    fun oneThousandSeedsKeepQuotasTimerAndCorrectAnswer() {
        val bank = syntheticBank()
        BankPolicy.validateBank(bank)
        repeat(1_000) { seed ->
            val session = BankPolicy.generate(bank, Random(seed))
            assertEquals(100, session.size)
            assertEquals(100, session.map { it.id }.toSet().size)
            assertEquals(1_800, session.sumOf { it.timeLimitSeconds })
            assertEquals(BankPolicy.quotas, session.groupingBy { it.difficultyCode }.eachCount())
            session.forEach { q ->
                assertEquals(q.correctAnswer, q.options[q.answerIndex])
                assertEquals(4, q.options.distinct().size)
            }
        }
    }

    @Test
    fun rejectsWrongTimeBudget() {
        val bank = syntheticBank().toMutableList()
        bank[0] = bank[0].copy(timeLimitSeconds = 900)
        var threw = false
        try {
            BankPolicy.validateBank(bank)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }
    @Test
    fun eachSubjectPracticeHasTenUniqueQuestionsAndMatchingKey() {
        val bank = syntheticBank()
        // Test the actual API against the four official subject names.
        val practices = bank.map { q ->
            val subject = when (q.difficultyCode.first()) {
                'K' -> "Perkalian"
                'P' -> "Pembagian"
                'J' -> "Penjumlahan"
                else -> "Konversi Satuan"
            }
            q.copy(category = subject)
        }
        listOf("Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan").forEach { category ->
            repeat(50) { seed ->
                val session = BankPolicy.generatePractice(practices, category, Random(seed))
                assertEquals(10, session.size)
                assertEquals(10, session.map { it.id }.toSet().size)
                assertTrue(session.all { it.category == category })
                session.forEach { q -> assertEquals(q.correctAnswer, q.options[q.answerIndex]) }
            }
        }
    }

}
