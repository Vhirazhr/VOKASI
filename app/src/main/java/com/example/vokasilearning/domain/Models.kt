package com.example.vokasilearning.domain

import androidx.compose.ui.graphics.ImageBitmap

internal data class Classroom(val label: String, val major: String)


internal data class Student(
    val username: String,
    val password: String,
    val name: String,
    val classroom: Classroom,
    val photo: ImageBitmap? = null,
    val seedXp: Int = 0,
    val seedStreak: Int = 0,
    val seedScore: Int = 0
)

internal data class Question(
    val id: Int, val category: String, val question: String,
    val options: List<String>, val answerIndex: Int
)

internal data class Attempt(
    val studentId: String, val timeMillis: Long, val signature: String,
    val correct: Int, val answered: Int, val questionCount: Int,
    val score: Int, val earnedXp: Int, val secondsSpent: Int
)

internal data class LevelData(val level: Int, val startXp: Int, val nextXp: Int)
internal enum class RankingMetric { XP, STREAK }
internal enum class RankingScope { SCHOOL, MAJOR, CLASS }
