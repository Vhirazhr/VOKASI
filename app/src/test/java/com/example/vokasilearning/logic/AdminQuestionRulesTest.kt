package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdminQuestionRulesTest {
    private val options = listOf("10", "11", "12", "13")
    private val bank = listOf(
        Question(1, "Perkalian", "3 × 4 = ?", options, 2, "K1", 8, "Hitung.", "12")
    )

    private fun validate(
        key: Int? = 2,
        question: String = "7 × 3 = ?",
        variants: List<String> = options,
        category: String = "Perkalian",
        code: String = "K1",
        explanation: String = "Kalikan bilangan."
    ) = AdminQuestionRules.error(bank, category, question, variants, key, code, explanation)

    @Test fun keyMustBeExplicitlyChosen() {
        assertEquals("Pilih jawaban yang benar.", validate(key = null))
    }

    @Test fun duplicateOptionsRejectedEvenIfCaseAndWhitespaceDiffer() {
        assertEquals("Pilihan jawaban harus berbeda.",
            validate(variants = listOf("A", "b", "a ", "C")))
    }

    @Test fun duplicateQuestionDetectedIgnoringCaseAndWhitespace() {
        assertEquals("Pertanyaan ini sudah ada di Bank Soal.",
            validate(question = " 3 × 4 = ?  "))
    }

    @Test fun mismatchedCategoryAndDifficultyRejected() {
        assertEquals("Materi dan tingkat kesulitan tidak sesuai.",
            validate(category = "Pembagian", code = "K1"))
    }

    @Test fun missingExplanationRejected() {
        assertEquals("Pembahasan belum diisi.", validate(explanation = "   "))
    }

    @Test fun validDraftAllowed() {
        assertNull(validate())
    }
}
