package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student

/** Aggregate compulsory submissions, never optional 10-question drills. */
internal data class ClassDailySummary(
    val classroom: String,
    val studentCount: Int,
    val completedCount: Int,
    val averageScore: Int?,
    val sessions: Int,
    val major: String = ""
) {
    val missingCount: Int get() = (studentCount - completedCount).coerceAtLeast(0)
}

internal object AdminAnalytics {
    fun compulsoryOnDate(attempts: List<Attempt>, date: String): List<Attempt> =
        attempts.filter { it.practiceCategory == null && dayKey(it.timeMillis) == date }

    fun latestCompulsoryByStudent(
        attempts: List<Attempt>, date: String
    ): Map<String, Attempt> = compulsoryOnDate(attempts, date)
        .groupBy { it.studentId }
        .mapValues { (_, sessions) ->
            sessions.maxWithOrNull(compareBy<Attempt> { it.timeMillis }.thenBy { it.sessionKey })!!
        }

    fun classSummary(
        classroom: String,
        students: List<Student>,
        attempts: List<Attempt>,
        date: String,
        major: String? = null
    ): ClassDailySummary {
        val members = students.filter {
            it.classroom.label == classroom && (major == null || it.classroom.major == major)
        }
        val ids = members.mapTo(HashSet()) { it.username }
        val latest = latestCompulsoryByStudent(attempts, date).filterKeys { it in ids }
        return ClassDailySummary(
            classroom = classroom,
            studentCount = members.size,
            completedCount = latest.size,
            averageScore = latest.values.takeIf { it.isNotEmpty() }
                ?.let { results -> results.sumOf { it.score } / results.size },
            sessions = compulsoryOnDate(attempts, date).count { it.studentId in ids },
            major = major ?: members.firstOrNull()?.classroom?.major.orEmpty()
        )
    }

    fun allClasses(
        students: List<Student>, attempts: List<Attempt>, date: String
    ): List<ClassDailySummary> =
        students.map { it.classroom.major to it.classroom.label }
            .distinct()
            .sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })
            .map { (major, classroom) -> classSummary(classroom, students, attempts, date, major) }
}
