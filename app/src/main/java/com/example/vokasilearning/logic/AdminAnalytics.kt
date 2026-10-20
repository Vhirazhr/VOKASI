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

    /** Pre-index once, rather than rescanning every attempt for each of many classes. */
    fun allClasses(
        students: List<Student>, attempts: List<Attempt>, date: String
    ): List<ClassDailySummary> {
        val dated = compulsoryOnDate(attempts, date)
        val latest = dated.groupBy { it.studentId }.mapValues { (_, sessions) ->
            sessions.maxWithOrNull(compareBy<Attempt> { it.timeMillis }.thenBy { it.sessionKey })!!
        }
        val submissionCounts = dated.groupingBy { it.studentId }.eachCount()
        return students.groupBy { it.classroom.major to it.classroom.label }
            .toList()
            .sortedWith(compareBy<Pair<Pair<String, String>, List<Student>>> { it.first.first }
                .thenBy { it.first.second })
            .map { (group, members) ->
                val grades = members.mapNotNull { latest[it.username]?.score }
                ClassDailySummary(
                    classroom = group.second,
                    studentCount = members.size,
                    completedCount = grades.size,
                    averageScore = grades.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                    sessions = members.sumOf { submissionCounts[it.username] ?: 0 },
                    major = group.first
                )
            }
    }
}
