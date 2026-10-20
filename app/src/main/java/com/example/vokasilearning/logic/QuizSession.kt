package com.example.vokasilearning.logic

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.vokasilearning.model.*
import java.util.UUID
import kotlin.math.max

/**
 * One-way timed examination: a committed answer cannot be edited, and the
 * remaining seconds of the previous question never carry over.
 *
 * This is a local training engine; server-verified official sessions still
 * require an authenticated backend.
 */
internal data class AnswerFeedback(val questionId: Int, val correct: Boolean, val selectedIndex: Int)

internal class QuizSession {
    var questions by mutableStateOf<List<Question>>(emptyList())
        private set
    val answers = mutableStateMapOf<Int, Int>()
    private val timeouts = mutableSetOf<Int>()
    private val secondsById = mutableMapOf<Int, Int>()
    var index by mutableIntStateOf(0)
        private set
    var selectedIndex by mutableStateOf<Int?>(null)
        private set
    var remainingSeconds by mutableIntStateOf(0)
        private set
    var totalRemainingSeconds by mutableIntStateOf(0)
        private set
    var sessionId by mutableIntStateOf(0)
        private set
    private var startedAtMillis by mutableLongStateOf(0L)
    private var startedAtElapsed by mutableLongStateOf(0L)
    private var questionStartedElapsed by mutableLongStateOf(0L)
    private var questionDeadlineElapsed by mutableLongStateOf(0L)
    private var sessionDeadlineElapsed by mutableLongStateOf(0L)
    private var token: String = ""
    var practiceCategory by mutableStateOf<String?>(null)
        private set
    var feedback by mutableStateOf<AnswerFeedback?>(null)
        private set
    private var feedbackUntilElapsed: Long = 0L

    val isComplete: Boolean get() = questions.isNotEmpty() && index >= questions.size
    val activeQuestion: Question? get() = questions.getOrNull(index)

    fun start(bank: List<Question>, category: String? = null): Boolean {
        val generated = if (category == null) BankPolicy.generate(bank)
            else BankPolicy.generatePractice(bank, category)
        if (generated.isEmpty()) return false
        practiceCategory = category
        feedback = null
        questions = generated
        answers.clear()
        timeouts.clear()
        secondsById.clear()
        selectedIndex = null
        index = 0
        startedAtMillis = System.currentTimeMillis()
        startedAtElapsed = SystemClock.elapsedRealtime()
        sessionDeadlineElapsed = startedAtElapsed + questions.sumOf { it.timeLimitSeconds } * 1_000L
        questionStartedElapsed = startedAtElapsed
        questionDeadlineElapsed = startedAtElapsed + questions.first().timeLimitSeconds * 1_000L
        token = UUID.randomUUID().toString()
        sessionId += 1
        tick()
        return true
    }

    fun choose(choiceIndex: Int) {
        tick()
        if (!isComplete && feedback == null && choiceIndex in 0..3) selectedIndex = choiceIndex
    }

    /** Return true only when all 100 questions have been processed. */
    fun submit(): Boolean {
        tick()
        if (isComplete) return true
        if (feedback != null) return false
        val chosen = selectedIndex ?: return false
        val current = questions[index]
        if (current.id in answers || current.id in timeouts) return false
        answers[current.id] = chosen
        secondsById[current.id] = (
            (SystemClock.elapsedRealtime() - questionStartedElapsed) / 1_000L
        ).toInt().coerceIn(0, current.timeLimitSeconds)
        feedback = AnswerFeedback(current.id, chosen == current.answerIndex, chosen)
        feedbackUntilElapsed = SystemClock.elapsedRealtime() + 1_000L
        remainingSeconds = 0
        return false
    }

    /**
     * Elapsed realtime is monotonic while the device is running. Multiple
     * questions that expired in the background are processed on return.
     */
    fun tick(): Boolean {
        if (questions.isEmpty() || isComplete) return isComplete
        val now = SystemClock.elapsedRealtime()
        if (now >= sessionDeadlineElapsed) {
            while (!isComplete) {
                markTimeout()
                index++
            }
            selectedIndex = null
            feedback = null
            remainingSeconds = 0
            totalRemainingSeconds = 0
            return true
        }
        if (feedback != null) {
            if (now >= feedbackUntilElapsed) {
                feedback = null
                moveNext(now, timedOut = false)
            } else {
                remainingSeconds = 0
                totalRemainingSeconds = ((sessionDeadlineElapsed - now + 999L) / 1_000L).toInt().coerceAtLeast(0)
                return false
            }
        }
        while (!isComplete && now >= questionDeadlineElapsed) {
            markTimeout()
            // Keep the old deadline as the starting point for the next
            // question, so background time never generates free time.
            moveNext(questionDeadlineElapsed, timedOut = true)
        }
        if (!isComplete) {
            remainingSeconds = ((questionDeadlineElapsed - now + 999L) / 1_000L)
                .toInt().coerceAtLeast(0)
        } else remainingSeconds = 0
        totalRemainingSeconds = ((sessionDeadlineElapsed - now + 999L) / 1_000L)
            .toInt().coerceAtLeast(0)
        return isComplete
    }

    private fun markTimeout() {
        val q = activeQuestion ?: return
        if (q.id !in answers) {
            timeouts.add(q.id)
            secondsById[q.id] = q.timeLimitSeconds
        }
    }

    private fun moveNext(baseElapsed: Long, timedOut: Boolean) {
        index++
        selectedIndex = null
        if (!isComplete) {
            questionStartedElapsed = baseElapsed
            questionDeadlineElapsed = baseElapsed + questions[index].timeLimitSeconds * 1_000L
        }
        if (timedOut && isComplete) remainingSeconds = 0
    }

    fun abandon() {
        questions = emptyList()
        answers.clear()
        timeouts.clear()
        secondsById.clear()
        selectedIndex = null
        feedback = null
        practiceCategory = null
        remainingSeconds = 0
        totalRemainingSeconds = 0
        index = 0
    }

    fun finish(student: Student, previous: List<Attempt>): Attempt {
        check(isComplete) { "Sesi belum selesai" }
        val review = questions.map { q ->
            val chosen = answers[q.id]
            AnswerReview(
                question = q,
                selectedIndex = chosen,
                status = when {
                    q.id in timeouts -> AnswerStatus.TIMEOUT
                    chosen == null -> AnswerStatus.TIDAK_DIJAWAB
                    chosen == q.answerIndex -> AnswerStatus.BENAR
                    else -> AnswerStatus.SALAH
                },
                responseSeconds = secondsById[q.id] ?: 0
            )
        }
        val correct = review.count { it.status == AnswerStatus.BENAR }
        val now = System.currentTimeMillis()
        val earnedToday = previous.filter {
            it.studentId == student.username && dayKey(it.timeMillis) == dayKey(now)
        }.sumOf { it.earnedXp }
        val alreadyGranted = previous.any {
            it.studentId == student.username && it.sessionKey == token
        }
        val alreadyPracticedToday = practiceCategory != null && previous.any {
            it.studentId == student.username && it.practiceCategory == practiceCategory &&
                dayKey(it.timeMillis) == dayKey(now) && it.earnedXp > 0
        }
        val reward = if (practiceCategory == null) SESSION_XP else 5
        val xp = if (!alreadyGranted && !alreadyPracticedToday && correct > 0) {
            max(0, minOf(reward, DAILY_XP_CAP - earnedToday))
        } else 0
        return Attempt(
            studentId = student.username,
            timeMillis = now,
            signature = questions.map { it.id }.sorted().joinToString("-"),
            correct = correct,
            answered = answers.size,
            questionCount = questions.size,
            score = correct * 100 / questions.size,
            earnedXp = xp,
            secondsSpent = ((SystemClock.elapsedRealtime() - startedAtElapsed) / 1_000L)
                .toInt().coerceAtLeast(0),
            review = review,
            sessionKey = token,
            practiceCategory = practiceCategory
        )
    }
}

@Composable
internal fun rememberQuizSession(): QuizSession = remember { QuizSession() }
