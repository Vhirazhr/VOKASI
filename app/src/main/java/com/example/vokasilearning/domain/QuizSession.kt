package com.example.vokasilearning.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Mesin sesi quiz prototipe: soal acak, jawaban, timer dan penilaian.
 * Belum terhubung ke penyimpanan/server.
 */
internal class QuizSession {
    var questions by mutableStateOf<List<Question>>(emptyList())
        private set
    val answers = mutableStateMapOf<Int, Int>()
    var index by mutableIntStateOf(0)
    var remainingSeconds by mutableIntStateOf(0)
    var sessionId by mutableIntStateOf(0)
    private var startedAt by mutableLongStateOf(0L)

    fun start(bank: List<Question>): Boolean {
        if (bank.isEmpty()) return false
        questions = bank.shuffled().take(QUESTIONS_PER_SESSION)
        answers.clear()
        index = 0
        remainingSeconds = questions.size * SECONDS_PER_QUESTION
        startedAt = System.currentTimeMillis()
        sessionId++
        return true
    }

    fun finish(student: Student, previous: List<Attempt>): Attempt {
        val correct = questions.count { answers[it.id] == it.answerIndex }
        val score = if (questions.isEmpty()) 0 else correct * 100 / questions.size
        val signature = questions.map { it.id }.sorted().joinToString("-")
        val now = System.currentTimeMillis()
        val repeat = previous.any {
            it.studentId == student.username && it.signature == signature && it.earnedXp > 0
        }
        val xpToday = previous.filter {
            it.studentId == student.username && dayKey(it.timeMillis) == dayKey(now)
        }.sumOf { it.earnedXp }
        val xp = if (answers.isNotEmpty() && !repeat && xpToday < DAILY_XP_CAP) {
            minOf(SESSION_XP, DAILY_XP_CAP - xpToday)
        } else 0
        return Attempt(
            student.username, now, signature, correct, answers.size,
            questions.size, score, xp, ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
        )
    }
}

@Composable
internal fun rememberQuizSession(): QuizSession = remember { QuizSession() }
