package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question
import kotlin.random.Random

/** Fixed blueprint issued by the school for one 100-question session. */
internal object BankPolicy {
    val quotas: Map<String, Int> = linkedMapOf(
        "K1" to 6, "K2" to 6, "K3" to 6, "K4" to 6, "K5" to 6,
        "P1" to 6, "P2" to 6, "P3" to 6, "P4" to 6, "P5" to 6,
        "J" to 20, "S1" to 15, "S2" to 5
    )
    val secondsByCode: Map<String, Int> = mapOf(
        "K1" to 8, "K2" to 15, "K3" to 22, "K4" to 30, "K5" to 30,
        "P1" to 8, "P2" to 15, "P3" to 22, "P4" to 30, "P5" to 30,
        "J" to 8, "S1" to 16, "S2" to 28
    )
    const val BANK_VERSION = "2026-10-v1"
    const val QUESTION_COUNT = 100
    const val TOTAL_SECONDS = 1_800

    fun validateBank(bank: List<Question>, requireExactly500: Boolean = true) {
        if (requireExactly500) require(bank.size == 500) { "Bank harus berisi tepat 500 soal, ditemukan ${bank.size}" }
        require(bank.map { it.id }.toSet().size == bank.size) { "ID soal duplikat" }
        require(bank.map { it.question.trim() }.toSet().size == bank.size) { "Teks pertanyaan duplikat" }
        bank.forEach { q ->
            require(q.difficultyCode in quotas) { "Kode kesulitan tidak dikenal: ${q.id}" }
            require(q.timeLimitSeconds == secondsByCode[q.difficultyCode]) { "Timer soal ${q.id} tidak sesuai kode" }
            require(q.options.size == 4 && q.options.toSet().size == 4) { "Opsi soal ${q.id} tidak valid" }
            require(q.answerIndex in 0..3) { "Indeks jawaban soal ${q.id} invalid" }
            require(q.correctAnswer.isEmpty() || q.options[q.answerIndex] == q.correctAnswer) { "Kunci soal ${q.id} tidak cocok" }
        }
        val counts = bank.groupingBy { it.difficultyCode }.eachCount()
        quotas.forEach { (code, needed) ->
            require((counts[code] ?: 0) >= needed) { "Soal kode $code kurang dari $needed" }
        }
        require(quotas.values.sum() == QUESTION_COUNT)
        require(quotas.entries.sumOf { (code, amount) -> amount * secondsByCode.getValue(code) } == TOTAL_SECONDS)
    }

    fun generate(bank: List<Question>, random: Random = Random.Default): List<Question> {
        validateBank(bank, requireExactly500 = false)
        val grouped = bank.groupBy { it.difficultyCode }
        val result = quotas.flatMap { (code, number) ->
            grouped.getValue(code).shuffled(random).take(number)
        }.shuffled(random).map { question ->
            val correct = question.options[question.answerIndex]
            val choices = question.options.shuffled(random)
            question.copy(options = choices, answerIndex = choices.indexOf(correct))
        }
        require(result.size == QUESTION_COUNT && result.map { it.id }.distinct().size == QUESTION_COUNT)
        require(result.sumOf { it.timeLimitSeconds } == TOTAL_SECONDS)
        require(result.groupingBy { it.difficultyCode }.eachCount() == quotas)
        return result
    }
    /** Optional remedial drill: ten questions from one subject, not the compulsory exam. */
    fun generatePractice(bank: List<Question>, category: String, random: Random = Random.Default): List<Question> {
        require(category in setOf("Perkalian", "Pembagian", "Penjumlahan", "Konversi Satuan")) {
            "Materi latihan tidak dikenal: $category"
        }
        val pool = bank.filter { it.category == category }
        require(pool.size >= 10) { "Minimal 10 soal untuk latihan $category" }
        return pool.shuffled(random).take(10).map { question ->
            val answer = question.options.getOrNull(question.answerIndex)
                ?: error("Opsi soal ${question.id} tidak valid")
            require(question.timeLimitSeconds > 0 && question.options.size == 4)
            val newOptions = question.options.shuffled(random)
            question.copy(options = newOptions, answerIndex = newOptions.indexOf(answer))
        }
    }

}
