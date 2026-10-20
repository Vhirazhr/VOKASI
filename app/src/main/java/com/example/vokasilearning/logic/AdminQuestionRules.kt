package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question

/**
 * Validation for manually entered questions. Independent of the UI so every
 * admin save is checked even if future screens call it directly.
 */
internal object AdminQuestionRules {
    private val allowedCodes = mapOf(
        "Perkalian" to setOf("K1", "K2", "K3", "K4", "K5"),
        "Pembagian" to setOf("P1", "P2", "P3", "P4", "P5"),
        "Penjumlahan" to setOf("J"),
        "Konversi Satuan" to setOf("S1", "S2")
    )

    /** Returns an Indonesian error, or null if the draft is valid. */
    fun error(
        bank: List<Question>,
        category: String,
        question: String,
        options: List<String>,
        correctIndex: Int?,
        difficultyCode: String,
        explanation: String
    ): String? {
        if (difficultyCode !in allowedCodes[category].orEmpty()) {
            return "Materi dan tingkat kesulitan tidak sesuai."
        }
        if (question.isBlank()) return "Pertanyaan belum diisi."
        if (options.size != 4 || options.any { it.isBlank() }) {
            return "Lengkapi keempat pilihan jawaban."
        }
        if (options.map { it.trim().lowercase() }.distinct().size != 4) {
            return "Pilihan jawaban harus berbeda."
        }
        if (correctIndex == null || correctIndex !in options.indices) {
            return "Pilih jawaban yang benar."
        }
        if (explanation.isBlank()) return "Pembahasan belum diisi."
        if (bank.any { it.question.trim().equals(question.trim(), ignoreCase = true) }) {
            return "Pertanyaan ini sudah ada di Bank Soal."
        }
        return null
    }
}
