package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

    /**
     * Date-driven admin dashboard, anchored to the existing report date.
     * Participation, distribution and average use the latest compulsory result
     * ON that date. Growth compares each pupil with their previous result
     * (not with a synthetic zero when no earlier submission exists).
     * Chart always shows seven consecutive WIB calendar days ending on date.
     */
    fun calculate(
        students: List<Student>,
        attempts: List<Attempt>,
        scope: SchoolScope,
        date: String
    ): SchoolStatistics {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date))
        val group = members(students, scope)
        fun key(value: String) = value.trim().lowercase(Locale.ROOT)
        val ids = group.mapTo(HashSet()) { key(it.username) }
        val all = attempts.filter {
            it.practiceCategory == null &&
                key(it.studentId) in ids &&
                dayKey(it.timeMillis) <= date
        }

        fun latest(runs: List<Attempt>) = runs.groupBy { key(it.studentId) }
            .mapValues { (_, values) ->
                values.maxWithOrNull(compareBy<Attempt> { it.timeMillis }
                    .thenBy { it.sessionKey })!!
            }

        val today = latest(all.filter { dayKey(it.timeMillis) == date })
        val previous = latest(all.filter { dayKey(it.timeMillis) < date })
        val scores = today.values.map { it.score }
        val comparable = today.keys.intersect(previous.keys)

        val dateParts = date.split("-").map { it.toInt() }
        val endDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
            isLenient = false
            clear()
            set(dateParts[0], dateParts[1] - 1, dateParts[2], 12, 0, 0)
        }
        val trend = (6 downTo 0).map { daysBefore ->
            val dayCal = endDay.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, -daysBefore)
            val day = dayKey(dayCal.timeInMillis)
            val daily = latest(all.filter { dayKey(it.timeMillis) == day }).values
                .map { it.score }
            day to daily.takeIf { it.isNotEmpty() }?.average()?.toInt()
        }

        return SchoolStatistics(
            students = group.size,
            classes = group.map { it.classroom.major to it.classroom.label }.distinct().size,
            majors = group.map { it.classroom.major }.distinct().size,
            completed = today.size,
            average = scores.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            high = scores.count { it >= 75 },
            middle = scores.count { it in 60..74 },
            low = scores.count { it < 60 },
            previousAverage = comparable.takeIf { it.isNotEmpty() }
                ?.map { previous.getValue(it).score }?.average()?.toInt(),
            improved = comparable.count { today.getValue(it).score > previous.getValue(it).score },
            declined = comparable.count { today.getValue(it).score < previous.getValue(it).score },
            stable = comparable.count { today.getValue(it).score == previous.getValue(it).score },
            trend = trend
        )
    }
    /**
     * WIB Monday-Sunday monitoring. Scores use each pupil's latest compulsory
     * result within the chosen week; comparisons include only pupils with
     * results in BOTH the selected and preceding week.
     */
    fun weekRange(anchorDate: String): Pair<String, String> {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(anchorDate))
        val parts = anchorDate.split("-").map { it.toInt() }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
            isLenient = false
            clear()
            set(parts[0], parts[1] - 1, parts[2], 12, 0, 0)
        }
        // Monday is day 2 in java.util.Calendar; Sunday is day 1.
        val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        val start = dayKey(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_MONTH, 6)
        return start to dayKey(cal.timeInMillis)
    }

    fun calculateWeekly(
        students: List<Student>,
        attempts: List<Attempt>,
        scope: SchoolScope,
        anchorDate: String
    ): SchoolStatistics {
        val (start, end) = weekRange(anchorDate)
        val firstDay = start.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
            clear()
            set(firstDay[0], firstDay[1] - 1, firstDay[2], 12, 0, 0)
        }
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val previousStart = dayKey(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val previousEnd = dayKey(calendar.timeInMillis)

        val group = members(students, scope)
        val ids = group.mapTo(HashSet()) { it.username }
        val compulsory = attempts.filter { it.practiceCategory == null && it.studentId in ids }

        fun latestBetween(from: String, through: String): Map<String, Attempt> =
            compulsory.filter { dayKey(it.timeMillis) in from..through }
                .groupBy { it.studentId }.mapValues { (_, runs) ->
                    runs.maxWithOrNull(compareBy<Attempt> { it.timeMillis }.thenBy { it.sessionKey })!!
                }

        val current = latestBetween(start, end)
        val previous = latestBetween(previousStart, previousEnd)
        val common = current.keys.intersect(previous.keys)
        val scores = current.values.map { it.score }
        val trend = (0..6).map { step ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
                clear()
                set(firstDay[0], firstDay[1] - 1, firstDay[2], 12, 0, 0)
                add(Calendar.DAY_OF_MONTH, step)
            }
            val day = dayKey(cal.timeInMillis)
            val dailyScores = latestBetween(day, day).values.map { it.score }
            day to dailyScores.takeIf { it.isNotEmpty() }?.average()?.toInt()
        }
        return SchoolStatistics(
            students = group.size,
            classes = group.map { it.classroom.major to it.classroom.label }.distinct().size,
            majors = group.map { it.classroom.major }.distinct().size,
            completed = current.size,
            average = scores.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            high = scores.count { it >= 75 },
            middle = scores.count { it in 60..74 },
            low = scores.count { it < 60 },
            previousAverage = previous.values.map { it.score }
                .takeIf { it.isNotEmpty() }?.average()?.toInt(),
            improved = common.count { current.getValue(it).score > previous.getValue(it).score },
            declined = common.count { current.getValue(it).score < previous.getValue(it).score },
            stable = common.count { current.getValue(it).score == previous.getValue(it).score },
            trend = trend
        )
    }

    /**
     * Monthly compulsory overview. Each pupil is counted only once, using their
     * last compulsory result within the selected WIB calendar month.
     * Growth compares pupils who have a result in BOTH this and previous month.
     */
    fun calculateMonthly(
        students: List<Student>,
        attempts: List<Attempt>,
        scope: SchoolScope,
        year: Int,
        month: Int
    ): SchoolStatistics {
        require(month in 1..12)
        val chosenMonth = String.format(Locale.US, "%04d-%02d", year, month)
        val members = members(students, scope)
        val ids = members.mapTo(HashSet()) { it.username }
        val compulsory = attempts.filter { it.practiceCategory == null && it.studentId in ids }
        val monthly = compulsory.groupBy { dayKey(it.timeMillis).take(7) }

        fun latestPerStudent(key: String): Map<String, Attempt> =
            monthly[key].orEmpty().groupBy { it.studentId }.mapValues { (_, runs) ->
                runs.maxWithOrNull(compareBy<Attempt> { it.timeMillis }.thenBy { it.sessionKey })!!
            }

        val current = latestPerStudent(chosenMonth)
        val cursor = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
            clear()
            set(year, month - 1, 1)
        }
        cursor.add(Calendar.MONTH, -1)
        val previousKey = String.format(Locale.US, "%04d-%02d",
            cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH) + 1)
        val previous = latestPerStudent(previousKey)
        val overlapping = current.keys.intersect(previous.keys)
        val scored = current.values.map { it.score }
        val history = (5 downTo 0).map { offset ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
                clear()
                set(year, month - 1, 1)
                add(Calendar.MONTH, -offset)
            }
            val key = String.format(Locale.US, "%04d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            val monthScores = latestPerStudent(key).values.map { it.score }
            key to monthScores.takeIf { it.isNotEmpty() }?.average()?.toInt()
        }
        return SchoolStatistics(
            students = members.size,
            classes = members.map { it.classroom.major to it.classroom.label }.distinct().size,
            majors = members.map { it.classroom.major }.distinct().size,
            completed = current.size,
            average = scored.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            high = scored.count { it >= 75 },
            middle = scored.count { it in 60..74 },
            low = scored.count { it < 60 },
            previousAverage = previous.values.map { it.score }.takeIf { it.isNotEmpty() }
                ?.average()?.toInt(),
            improved = overlapping.count { current.getValue(it).score > previous.getValue(it).score },
            declined = overlapping.count { current.getValue(it).score < previous.getValue(it).score },
            stable = overlapping.count { current.getValue(it).score == previous.getValue(it).score },
            trend = history
        )
    }

}
