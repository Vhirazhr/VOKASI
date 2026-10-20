package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Device-local history for offline training and demonstrations.
 * This does not synchronize to a school admin on a different device.
 * Do not mistake these records for backend-verified school reports.
 */
internal object LocalAttemptStore {
    private const val FILE_NAME = "attempt_history_v1.json"

    fun load(context: Context): List<Attempt> = try {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) emptyList() else {
            val items = JSONArray(file.readText(Charsets.UTF_8))
            (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                val rawReview = item.optJSONArray("review") ?: JSONArray()
                val review = (0 until rawReview.length()).map { i ->
                    val record = rawReview.getJSONObject(i)
                    val q = record.getJSONObject("question")
                    val arr = q.getJSONArray("options")
                    val question = Question(
                        id = q.getInt("id"), category = q.getString("category"),
                        question = q.getString("question"),
                        options = (0 until arr.length()).map { arr.getString(it) },
                        answerIndex = q.getInt("answerIndex"),
                        difficultyCode = q.optString("difficultyCode", ""),
                        timeLimitSeconds = q.optInt("timeLimitSeconds", 0),
                        explanation = q.optString("explanation", ""),
                        correctAnswer = q.optString("correctAnswer", "")
                    )
                    AnswerReview(
                        question = question,
                        selectedIndex = if (record.isNull("selectedIndex")) null else record.getInt("selectedIndex"),
                        status = AnswerStatus.valueOf(record.getString("status")),
                        responseSeconds = record.optInt("responseSeconds", 0)
                    )
                }
                Attempt(
                    studentId = item.getString("studentId"),
                    timeMillis = item.getLong("timeMillis"),
                    signature = item.getString("signature"),
                    correct = item.getInt("correct"),
                    answered = item.getInt("answered"),
                    questionCount = item.getInt("questionCount"),
                    score = item.getInt("score"),
                    earnedXp = item.getInt("earnedXp"),
                    secondsSpent = item.getInt("secondsSpent"),
                    review = review,
                    sessionKey = item.optString("sessionKey", ""),
                    practiceCategory = if (item.isNull("practiceCategory") || !item.has("practiceCategory")) null else item.getString("practiceCategory"),
                    studentNameSnapshot = item.optString("studentNameSnapshot", ""),
                    classSnapshot = item.optString("classSnapshot", ""),
                    majorSnapshot = item.optString("majorSnapshot", "")
                )
            }
        }
    } catch (_: Exception) {
        // Keep the app usable. An unreadable history must not be overwritten
        // until a new session is intentionally saved.
        emptyList()
    }

    fun save(context: Context, attempts: List<Attempt>) {
        val result = JSONArray()
        attempts.forEach { item ->
            val data = JSONObject()
                .put("studentId", item.studentId)
                .put("timeMillis", item.timeMillis)
                .put("signature", item.signature)
                .put("correct", item.correct)
                .put("answered", item.answered)
                .put("questionCount", item.questionCount)
                .put("score", item.score)
                .put("earnedXp", item.earnedXp)
                .put("secondsSpent", item.secondsSpent)
                .put("sessionKey", item.sessionKey)
                .put("practiceCategory", item.practiceCategory ?: JSONObject.NULL)
                .put("studentNameSnapshot", item.studentNameSnapshot)
                .put("classSnapshot", item.classSnapshot)
                .put("majorSnapshot", item.majorSnapshot)
            val reviews = JSONArray()
            item.review.forEach { record ->
                val q = record.question
                val question = JSONObject()
                    .put("id", q.id)
                    .put("category", q.category)
                    .put("question", q.question)
                    .put("options", JSONArray(q.options))
                    .put("answerIndex", q.answerIndex)
                    .put("difficultyCode", q.difficultyCode)
                    .put("timeLimitSeconds", q.timeLimitSeconds)
                    .put("explanation", q.explanation)
                    .put("correctAnswer", q.correctAnswer)
                reviews.put(
                    JSONObject()
                        .put("question", question)
                        .put("selectedIndex", record.selectedIndex ?: JSONObject.NULL)
                        .put("status", record.status.name)
                        .put("responseSeconds", record.responseSeconds)
                )
            }
            result.put(data.put("review", reviews))
        }
        val target = File(context.filesDir, FILE_NAME)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(result.toString(), Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }
}
