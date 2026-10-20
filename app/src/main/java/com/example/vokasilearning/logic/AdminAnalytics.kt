package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student

/** Local-only school admin metrics. Optional subject drills never become compulsory grades. */
internal data class ClassDailySummary(
    val classroom: String,
    val studentCount: Int,
    val completedCount: Int,
    val averageScore: Int?,
    val sessions: Int
) {
    val missingCount: Int get() = studentCount - completedCount
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
        date: String
    ): ClassDailySummary {
        val members = students.filter { it.classroom.label == classroom }
        val completed = latestCompulsoryByStudent(attempts, date)
            .filterKeys { id -> members.any { it.username == id } }
        return ClassDailySummary(
            classroom = classroom,
            studentCount = members.size,
            completedCount = completed.size,
            averageScore = completed.values.takeIf { it.isNotEmpty() }
                ?.let { records -> records.sumOf { it.score } / records.size },
            sessions = compulsoryOnDate(attempts, date).count { session ->
                members.any { it.username == session.studentId }
            }
        )
    }

    fun allClasses(
        students: List<Student>, attempts: List<Attempt>, date: String
    ): List<ClassDailySummary> =
        students.map { it.classroom.label }.distinct().sorted()
            .map { classSummary(it, students, attempts, date) }
}
