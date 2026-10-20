package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.logic.BankPolicy
import com.example.vokasilearning.model.Question
import org.json.JSONArray

/** Original 500-question CSV transformed losslessly into a JSON asset. */
internal object QuestionBankLoader {
    fun load(context: Context): List<Question> {
        val raw = context.assets.open("vokasi_bank_500.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(raw)
        val questions = (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val values = obj.getJSONArray("options")
            val options = (0 until values.length()).map { values.getString(it) }
            val correct = obj.getString("correctAnswer")
            val answerIndex = obj.getString("correctOption").single() - 'A'
            Question(
                id = obj.getInt("id"),
                category = obj.getString("category"),
                question = obj.getString("question"),
                options = options,
                answerIndex = answerIndex,
                difficultyCode = obj.getString("difficultyCode"),
                timeLimitSeconds = obj.getInt("timeLimitSeconds"),
                explanation = obj.getString("explanation"),
                correctAnswer = correct
            )
        }
        BankPolicy.validateBank(questions)
        return questions
    }
}
