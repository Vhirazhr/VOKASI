package com.example.vokasilearning.logic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

internal fun clockText(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

internal fun scoreGrade(score: Int): String = when {
    score >= 90 -> "Sangat Baik"
    score >= 75 -> "Baik"
    score >= 60 -> "Cukup"
    else -> "Perlu Latihan"
}

internal fun wibCalendar() = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
internal fun dayKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
}.format(Date(millis))
internal fun seasonKey(timeMillis: Long): String {
    val cal = wibCalendar().apply { timeInMillis = timeMillis }
    return "${cal.get(Calendar.YEAR)}-S${cal.get(Calendar.MONTH) / 3 + 1}"
}
internal fun seasonTitle(): String {
    val c = wibCalendar()
    return "Season ${c.get(Calendar.MONTH) / 3 + 1} • ${c.get(Calendar.YEAR)}"
}
internal fun levelFor(totalXp: Int): LevelData {
    var level = 1; var threshold = 0; var cost = 100
    while (totalXp >= threshold + cost && level < 500) {
        threshold += cost; level++; cost += 50
    }
    return LevelData(level, threshold, threshold + cost)
}
internal fun calculateStreak(student: Student, attempts: List<Attempt>): Int {
    val validDays = attempts.filter { it.studentId == student.username && it.answered > 0 }
        .map { dayKey(it.timeMillis) }.toSet()
    val day = wibCalendar().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    if (dayKey(day.timeInMillis) !in validDays) day.add(Calendar.DAY_OF_YEAR, -1)
    var actual = 0
    while (dayKey(day.timeInMillis) in validDays) { actual++; day.add(Calendar.DAY_OF_YEAR, -1) }
    return max(actual, student.seedStreak)
}
internal fun studentSeasonXp(student: Student, attempts: List<Attempt>): Int {
    val currentSeason = seasonKey(System.currentTimeMillis())
    return student.seedXp + attempts.filter {
        it.studentId == student.username && seasonKey(it.timeMillis) == currentSeason
    }.sumOf { it.earnedXp }
}
internal fun lifetimeXp(student: Student, attempts: List<Attempt>): Int =
    student.seedXp + attempts.filter { it.studentId == student.username }.sumOf { it.earnedXp }

internal fun rankList(
    users: List<Student>, attempts: List<Attempt>, me: Student, scope: RankingScope, metric: RankingMetric,
    chosenMajor: String = me.classroom.major, chosenClass: String = me.classroom.label
): List<Student> {
    val filtered = users.filter { u -> when (scope) {
        RankingScope.SCHOOL -> true
        RankingScope.MAJOR -> u.classroom.major == chosenMajor
        RankingScope.CLASS -> u.classroom.label == chosenClass
    } }
    return if (metric == RankingMetric.XP) filtered.sortedWith(
        compareByDescending<Student> { studentSeasonXp(it, attempts) }.thenBy { it.name }
    ) else filtered.sortedWith(
        compareByDescending<Student> { calculateStreak(it, attempts) }.thenBy { it.name }
    )
}
