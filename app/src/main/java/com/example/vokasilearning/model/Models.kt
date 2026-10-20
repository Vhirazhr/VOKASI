package com.example.vokasilearning.model

import androidx.compose.ui.graphics.ImageBitmap
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

internal data class Classroom(val label: String, val major: String)


internal data class Student(
    val username: String,
    val password: String,
    val name: String,
    val classroom: Classroom,
    val photo: ImageBitmap? = null,
    val seedXp: Int = 0,
    val seedStreak: Int = 0,
    val seedScore: Int = 0,
    val admissionYear: Int = 0
)

internal data class Question(
    val id: Int, val category: String, val question: String,
    val options: List<String>, val answerIndex: Int,
    val difficultyCode: String = "",
    val timeLimitSeconds: Int = 0,
    val explanation: String = "",
    val correctAnswer: String = ""
)

internal data class Attempt(
    val studentId: String, val timeMillis: Long, val signature: String,
    val correct: Int, val answered: Int, val questionCount: Int,
    val score: Int, val earnedXp: Int, val secondsSpent: Int,
    val review: List<AnswerReview> = emptyList(),
    val sessionKey: String = "",
    val practiceCategory: String? = null,
    val studentNameSnapshot: String = "",
    val classSnapshot: String = "",
    val majorSnapshot: String = ""
)

internal enum class AnswerStatus { BENAR, SALAH, TIMEOUT, TIDAK_DIJAWAB }
internal data class AnswerReview(
    val question: Question,
    val selectedIndex: Int?,
    val status: AnswerStatus,
    val responseSeconds: Int = 0
)

internal data class LevelData(val level: Int, val startXp: Int, val nextXp: Int)
internal enum class RankingMetric { XP, STREAK }
internal enum class RankingScope { SCHOOL, MAJOR, CLASS }
