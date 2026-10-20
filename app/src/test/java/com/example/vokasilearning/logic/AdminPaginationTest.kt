package com.example.vokasilearning.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminPaginationTest {
    @Test fun gradesAlwaysShowAtMostFifteenPerPage() {
        val ids = (1..137).toList()
        assertEquals(10, AdminPagination.count(ids.size, 15))
        assertEquals((1..15).toList(), AdminPagination.slice(ids, 0, 15))
        assertEquals((16..30).toList(), AdminPagination.slice(ids, 1, 15))
        assertEquals((136..137).toList(), AdminPagination.slice(ids, 9, 15))
        assertEquals(15, AdminPagination.slice(ids, 5, 15).size)
    }

    @Test fun filterShrinksPagesAndClampsOldSelection() {
        val filtered = (1..7).toList()
        assertEquals(1, AdminPagination.count(filtered.size, 15))
        assertEquals(0, AdminPagination.active(6, filtered.size, 15))
        assertEquals(filtered, AdminPagination.slice(filtered, 6, 15))
    }

    @Test fun emptyRecordsNeverShowInvalidPage() {
        assertEquals(1, AdminPagination.count(0, 15))
        assertEquals(0, AdminPagination.active(3, 0, 15))
        assertEquals(emptyList<Int>(), AdminPagination.slice(emptyList(), 3, 15))
    }

    @Test fun hundredAnswerReviewsAreTenAtATime() {
        val items = (1..100).toList()
        assertEquals(10, AdminPagination.count(100, 10))
        assertEquals((91..100).toList(), AdminPagination.slice(items, 9, 10))
        assertEquals((1..10).toList(), AdminPagination.slice(items, -1, 10))
    }
}
