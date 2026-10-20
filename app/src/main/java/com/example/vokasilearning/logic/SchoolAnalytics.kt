package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student

/** X, XI and XII are distinct years; sections within each year remain distinct. */
internal fun schoolGrade(classLabel: String): String =
    classLabel.trim().split(Regex("\\s+")).firstOrNull()
        ?.takeIf { it == "X" || it == "XI" || it == "XII" }.orEmpty()

internal data class SchoolScope(
    val major: String = "",
    val grade: String = "",
    val classroom: String = ""
)

internal data class SchoolStatistics(
    val students: Int,
    val classes: Int,
    val majors: Int,
    val completed: Int,
    val average: Int?,
    val high: Int,
    val middle: Int,
    val low: Int,
    val previousAverage: Int?,
    val improved: Int,
    val declined: Int,
    val stable: Int,
    val trend: List<Pair<String, Int?>>
) {
    val missing: Int get() = students - completed
    val participationPercent: Int get() = if (students == 0) 0 else completed * 100 / students
}

internal object SchoolAnalytics {
    fun members(students: List<Student>, scope: SchoolScope): List<Student> = students.filter {
        (scope.major.isBlank() || it.classroom.major == scope.major) &&
        (scope.grade.isBlank() || schoolGrade(it.classroom.label) == scope.grade) &&
        (scope.classroom.isBlank() || it.classroom.label == scope.classroom)
    }

    fun calculate(
        students: List<Student>,
        attempts: List<Attempt>,
        scope: SchoolScope,
        date: String
    ): SchoolStatistics {
        val members = members(students, scope)
        val ids = members.mapTo(HashSet()) { it.username }
        val all = attempts.filter { it.practiceCategory == null && it.studentId in ids &&
            dayKey(it.timeMillis) <= date }
        val days = all.map { dayKey(it.timeMillis) }.distinct().sorted()
        val today = all.filter { dayKey(it.timeMillis) == date }
            .groupBy { it.studentId }.mapValues { (_, list) -> list.maxByOrNull { it.timeMillis }!! }
        val previous = all.filter { dayKey(it.timeMillis) < date }
            .groupBy { it.studentId }.mapValues { (_, list) -> list.maxByOrNull { it.timeMillis }!! }
        val scores = today.values.map { it.score }
        val withHistory = today.keys.filter { it in previous.keys }
        val improved = withHistory.count { today.getValue(it).score > previous.getValue(it).score }
        val declined = withHistory.count { today.getValue(it).score < previous.getValue(it).score }
        val stable = withHistory.size - improved - declined
        val trend = days.takeLast(7).map { day ->
            val latest = all.filter { dayKey(it.timeMillis) == day }
                .groupBy { it.studentId }.values.mapNotNull { it.maxByOrNull { run -> run.timeMillis }?.score }
            day to latest.takeIf { it.isNotEmpty() }?.average()?.toInt()
        }
        return SchoolStatistics(
            students = members.size,
            classes = members.map { it.classroom.major to it.classroom.label }.distinct().size,
            majors = members.map { it.classroom.major }.distinct().size,
            completed = today.size,
            average = scores.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            high = scores.count { it >= 75 },
            middle = scores.count { it in 60..74 },
            low = scores.count { it < 60 },
            previousAverage = previous.values.map { it.score }.takeIf { it.isNotEmpty() }
                ?.average()?.toInt(),
            improved = improved,
            declined = declined,
            stable = stable,
            trend = trend
        )
    }
}
