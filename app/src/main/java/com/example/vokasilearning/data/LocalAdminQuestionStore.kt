package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.model.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local supplementary question bank. The bundled 500 official questions
 * are read-only, so required quiz quotas never disappear after admin actions.
 * A real shared bank requires a backend later.
 */
internal object LocalAdminQuestionStore {
    private const val FILE = "vokasi_admin_extra_questions.json"

    fun load(context: Context): List<Question> = try {
        val file = context.getFileStreamPath(FILE)
        val arr = if (file.exists()) JSONArray(file.readText(Charsets.UTF_8)) else JSONArray()
        (0 until arr.length()).map { i ->
            val value = arr.getJSONObject(i)
            val options = value.getJSONArray("options")
            Question(
                id = value.getInt("id"),
                category = value.getString("category"),
                question = value.getString("question"),
                options = (0 until options.length()).map { options.getString(it) },
                answerIndex = value.getInt("answerIndex"),
                difficultyCode = value.getString("difficultyCode"),
                timeLimitSeconds = value.getInt("timeLimitSeconds"),
                explanation = value.optString("explanation", ""),
                correctAnswer = value.getString("correctAnswer")
            )
        }.filter { it.options.size == 4 && it.answerIndex in 0..3 }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(context: Context, questions: List<Question>) {
        val arr = JSONArray()
        questions.forEach { q ->
            arr.put(JSONObject()
                .put("id", q.id)
                .put("category", q.category)
                .put("question", q.question)
                .put("options", JSONArray(q.options))
                .put("answerIndex", q.answerIndex)
                .put("difficultyCode", q.difficultyCode)
                .put("timeLimitSeconds", q.timeLimitSeconds)
                .put("explanation", q.explanation)
                .put("correctAnswer", q.correctAnswer)
            )
        }
        val target = context.getFileStreamPath(FILE)
        val temp = java.io.File(target.absolutePath + ".tmp")
        temp.writeText(arr.toString(), Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }
}
