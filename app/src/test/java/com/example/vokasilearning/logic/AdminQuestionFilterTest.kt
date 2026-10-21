package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminQuestionFilterTest {
    private val questions = listOf(
        Question(1, "Perkalian", "3 x 4 = ?", listOf("11", "12", "13", "14"), 1, "K1"),
        Question(2, "Pembagian", "18 / 3 = ?", listOf("3", "5", "6", "9"), 2, "P1"),
        Question(3, "Penjumlahan", "6 + 7 = ?", listOf("12", "13", "14", "15"), 1, "J"),
        Question(4, "Perkalian", "4 x 5 = ?", listOf("18", "19", "20", "21"), 2, "K1")
    )

    @Test fun oneCategoryFiltersOnlyThatMaterial() {
        assertEquals(listOf(1, 4),
            AdminQuestionFilter.apply(questions, setOf("Perkalian"), "").map { it.id })
    }

    @Test fun multipleCategoriesCreateCombinedExportScope() {
        val selected = AdminQuestionFilter.toggle(
            AdminQuestionFilter.toggle(emptySet(), "Perkalian"), "Pembagian"
        )
        assertEquals(setOf("Perkalian", "Pembagian"), selected)
        assertEquals(listOf(1, 2, 4),
            AdminQuestionFilter.apply(questions, selected, "").map { it.id })
        assertEquals(setOf("Perkalian"), AdminQuestionFilter.toggle(selected, "Pembagian"))
        assertTrue(AdminQuestionFilter.toggle(selected, "Semua").isEmpty())
    }

    @Test fun queryAndMultipleCategoriesRestrictBothDisplayedAndExportedRows() {
        assertEquals(listOf(4),
            AdminQuestionFilter.apply(
                questions, setOf("Perkalian", "Pembagian"), "4 x 5"
            ).map { it.id })
        assertEquals(listOf(1, 4),
            AdminQuestionFilter.apply(questions, emptySet(), "k1").map { it.id })
    }

    @Test fun listingAndExportKeepAllMatchingRowsBeyondPaginationSize() {
        val longer = (1..27).map { idx ->
            Question(idx, "Perkalian", "Pertanyaan nomor " + idx,
                listOf("1", "2", "3", "4"), 0, "K1")
        }
        val filtered = AdminQuestionFilter.apply(longer, setOf("Perkalian"), "")
        assertEquals(27, filtered.size)
        assertEquals(10, AdminPagination.slice(filtered, 0, AdminPagination.PAGE_SIZE).size)
    }
}
