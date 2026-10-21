package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question

/**
 * Same effective filter for the admin question list and Excel export.
 * Empty categories means "Semua"; multiple selected categories are combined.
 */
internal object AdminQuestionFilter {
    fun toggle(current: Set<String>, selected: String): Set<String> = when {
        selected == "Semua" -> emptySet()
        selected in current -> current - selected
        else -> current + selected
    }

    fun apply(
        questions: List<Question>,
        selectedCategories: Set<String>,
        search: String
    ): List<Question> {
        val query = search.trim()
        return questions.filter { question ->
            (selectedCategories.isEmpty() || question.category in selectedCategories) &&
                (query.isBlank() || question.question.contains(query, ignoreCase = true) ||
                    question.difficultyCode.contains(query, ignoreCase = true) ||
                    question.id.toString() == query)
        }.sortedBy { it.id }
    }
}
