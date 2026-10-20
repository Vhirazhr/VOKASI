package com.example.vokasilearning.logic

import com.example.vokasilearning.model.AnswerStatus
import com.example.vokasilearning.model.Attempt

/** Learning feedback from actual answer history; no grades or diagnoses are inferred. */
internal data class SubjectInsight(
    val category: String,
    val attempted: Int,
    val correct: Int,
    val focus: String?
) {
    val accuracy: Int get() = if (attempted == 0) 0 else (correct * 100 / attempted)
    val needsPractice: Boolean get() = attempted >= 5 && accuracy < 70
    val status: String get() = when {
        attempted == 0 -> "Belum dicoba"
        attempted < 5 -> "Butuh lebih banyak latihan"
        needsPractice -> "Disarankan ulang"
        accuracy < 85 -> "Terus tingkatkan"
        else -> "Sudah bagus"
    }
}

internal object PracticeInsights {
    val categories = listOf("Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan")

    private val displayCode = mapOf(
        "K1" to "perkalian satuan", "K2" to "perkalian satuan × puluhan/ratusan",
        "K3" to "perkalian puluhan", "K4" to "perkalian puluhan × ratusan",
        "K5" to "perkalian ratusan",
        "P1" to "pembagian satuan", "P2" to "pembagian bersusun sederhana",
        "P3" to "pembagian puluhan", "P4" to "pembagian hasil ratusan",
        "P5" to "pembagian ratusan",
        "J" to "penjumlahan", "S1" to "konversi mm, cm, dan m",
        "S2" to "konversi inci ke mm"
    )

    fun forStudent(studentId: String, attempts: List<Attempt>): List<SubjectInsight> {
        // Latest five sessions provide a useful rolling picture of learning.
        val recent = attempts.asSequence()
            .filter { it.studentId == studentId && it.review.isNotEmpty() }
            .sortedByDescending { it.timeMillis }
            .take(5).flatMap { it.review.asSequence() }.toList()
        return categories.map { category ->
            val subject = recent.filter { it.question.category == category }
            val correct = subject.count { it.status == AnswerStatus.BENAR }
            val codeFocus = subject.groupBy { it.question.difficultyCode }.entries
                .mapNotNull { (code, answers) ->
                    if (answers.size >= 3) {
                        val rate = answers.count { it.status == AnswerStatus.BENAR } * 100 / answers.size
                        if (rate < 70) code to rate else null
                    } else null
                }.minByOrNull { it.second }?.first
            SubjectInsight(category, subject.size, correct, codeFocus?.let { displayCode[it] ?: category })
        }
    }
}
