package com.example.vokasilearning.logic

import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
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
        // One second of correctness feedback is not part of the question budget.
        sessionDeadlineElapsed += 1_000L
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
                moveNext(feedbackUntilElapsed, timedOut = false)
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

    /** Full immutable question/option snapshot; persistent only in private app files. */
    fun snapshot(owner: String): JSONObject {
        val qs = JSONArray()
        questions.forEach { q ->
            qs.put(JSONObject()
                .put("id", q.id).put("category", q.category).put("question", q.question)
                .put("options", JSONArray(q.options)).put("answerIndex", q.answerIndex)
                .put("difficultyCode", q.difficultyCode)
                .put("timeLimitSeconds", q.timeLimitSeconds)
                .put("explanation", q.explanation).put("correctAnswer", q.correctAnswer)
            )
        }
        val storedAnswers = JSONObject()
        answers.forEach { (id, option) -> storedAnswers.put(id.toString(), option) }
        val storedSeconds = JSONObject()
        secondsById.forEach { (id, seconds) -> storedSeconds.put(id.toString(), seconds) }
        return JSONObject()
            .put("formatVersion", 1).put("bankVersion", BankPolicy.BANK_VERSION)
            .put("owner", owner)
            .put("questions", qs)
            .put("answers", storedAnswers)
            .put("timeouts", JSONArray(timeouts.toList()))
            .put("secondsById", storedSeconds)
            .put("index", index)
            .put("selectedIndex", selectedIndex ?: JSONObject.NULL)
            .put("startedAtMillis", startedAtMillis)
            .put("startedAtElapsed", startedAtElapsed)
            .put("questionStartedElapsed", questionStartedElapsed)
            .put("questionDeadlineElapsed", questionDeadlineElapsed)
            .put("sessionDeadlineElapsed", sessionDeadlineElapsed)
            .put("token", token)
            .put("practiceCategory", practiceCategory ?: JSONObject.NULL)
            .put("feedbackUntilElapsed", feedbackUntilElapsed)
            .put("feedbackIndex", feedback?.selectedIndex ?: JSONObject.NULL)
            .put("feedbackQuestionId", feedback?.questionId ?: JSONObject.NULL)
    }

    /**
     * Restore after process death. Elapsed realtime survives process restarts
     * but not device reboot. On reboot, unanswered questions are expired (no free time).
     */
    fun restore(saved: JSONObject): Boolean {
        return try {
            require(saved.optInt("formatVersion") == 1)
            val serialized = saved.getJSONArray("questions")
            val recovered = (0 until serialized.length()).map { i ->
                val o = serialized.getJSONObject(i)
                val options = o.getJSONArray("options")
                Question(
                    id = o.getInt("id"), category = o.getString("category"),
                    question = o.getString("question"),
                    options = (0 until options.length()).map { options.getString(it) },
                    answerIndex = o.getInt("answerIndex"),
                    difficultyCode = o.getString("difficultyCode"),
                    timeLimitSeconds = o.getInt("timeLimitSeconds"),
                    explanation = o.optString("explanation", ""),
                    correctAnswer = o.optString("correctAnswer", "")
                )
            }
            require(recovered.size in listOf(10, 100))
            require(recovered.map { it.id }.distinct().size == recovered.size)
            require(recovered.all {
                it.answerIndex in 0..3 && it.options.size == 4 &&
                    it.options[it.answerIndex] == it.correctAnswer &&
                    it.timeLimitSeconds > 0
            })
            if (recovered.size == 100) {
                require(recovered.sumOf { it.timeLimitSeconds } == BankPolicy.TOTAL_SECONDS)
                require(recovered.groupingBy { it.difficultyCode }.eachCount() == BankPolicy.quotas)
            }
            val recoveredIndex = saved.getInt("index")
            require(recoveredIndex in 0..recovered.size)
            val recoveredStart = saved.getLong("startedAtElapsed")
            require(recoveredStart >= 0L)
            val storedSessionDeadline = saved.getLong("sessionDeadlineElapsed")
            val storedQuestionDeadline = saved.getLong("questionDeadlineElapsed")
            require(storedQuestionDeadline >= recoveredStart && storedSessionDeadline >= recoveredStart)
            val storedToken = saved.getString("token")
            require(storedToken.isNotBlank())

            questions = recovered
            index = recoveredIndex
            answers.clear()
            val answerMap = saved.getJSONObject("answers")
            val keys = answerMap.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val chosen = answerMap.getInt(id)
                require(recovered.any { it.id == id.toInt() } && chosen in 0..3)
                answers[id.toInt()] = chosen
            }
            timeouts.clear()
            val expired = saved.getJSONArray("timeouts")
            repeat(expired.length()) { timeouts.add(expired.getInt(it)) }
            secondsById.clear()
            val times = saved.getJSONObject("secondsById")
            val timeKeys = times.keys()
            while (timeKeys.hasNext()) {
                val id = timeKeys.next()
                secondsById[id.toInt()] = times.getInt(id)
            }
            startedAtMillis = saved.getLong("startedAtMillis")
            startedAtElapsed = recoveredStart
            questionStartedElapsed = saved.getLong("questionStartedElapsed")
            questionDeadlineElapsed = storedQuestionDeadline
            sessionDeadlineElapsed = storedSessionDeadline
            token = storedToken
            practiceCategory = if (saved.isNull("practiceCategory")) null else saved.getString("practiceCategory")
            selectedIndex = if (saved.isNull("selectedIndex")) null else saved.getInt("selectedIndex")
            feedbackUntilElapsed = saved.optLong("feedbackUntilElapsed", 0L)
            feedback = if (!saved.isNull("feedbackIndex") && !isComplete) {
                val option = saved.getInt("feedbackIndex")
                val active = questions[index]
                AnswerFeedback(active.id, option == active.answerIndex, option)
            } else null
            sessionId++
            if (SystemClock.elapsedRealtime() < startedAtElapsed) {
                // System restarted; old monotonic timestamps cannot be trusted.
                while (!isComplete) { markTimeout(); index++ }
                feedback = null
            } else tick()
            true
        } catch (_: Exception) {
            abandon()
            false
        }
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
        val xp = QuizRewards.earnedXp(
            username = student.username,
            previous = previous,
            nowMillis = now,
            sessionKey = token,
            practiceCategory = practiceCategory,
            correct = correct
        )
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
            practiceCategory = practiceCategory,
            studentNameSnapshot = student.name,
            classSnapshot = student.classroom.label,
            majorSnapshot = student.classroom.major
        )
    }
}

@Composable
internal fun rememberQuizSession(): QuizSession = remember { QuizSession() }
