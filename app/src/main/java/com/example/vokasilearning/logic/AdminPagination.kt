package com.example.vokasilearning.logic

/** Pure paging rules shared by admin lists, unit-testable without Android UI. */
internal object AdminPagination {
    /** Consistent maximum entries displayed in admin directories. */
    const val PAGE_SIZE = 10
    fun count(total: Int, size: Int): Int {
        require(total >= 0 && size > 0)
        return maxOf(1, (total + size - 1) / size)
    }

    fun active(page: Int, total: Int, size: Int): Int =
        page.coerceIn(0, count(total, size) - 1)

    fun <T> slice(items: List<T>, page: Int, size: Int): List<T> {
        val safe = active(page, items.size, size)
        return items.drop(safe * size).take(size)
    }
}
