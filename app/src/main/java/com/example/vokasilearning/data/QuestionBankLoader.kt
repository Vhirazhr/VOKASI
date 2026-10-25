package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.model.Question

/**
 * Old embedded 500-question bank withdrawn on request.
 * The replacement visual bank will be connected after approval; no fallback.
 */
internal object QuestionBankLoader {
    @Suppress("UNUSED_PARAMETER")
    fun load(context: Context): List<Question> = emptyList()
}
