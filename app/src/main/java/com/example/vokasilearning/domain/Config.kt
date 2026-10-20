package com.example.vokasilearning.domain

internal const val SCHOOL = "SMK Negeri 1 Singosari"
internal const val DAILY_XP_CAP = 10_000
internal const val SESSION_XP = 20
internal const val QUESTIONS_PER_SESSION = 10
internal const val SECONDS_PER_QUESTION = 60

// Khusus pengujian/presentasi: rekaman diizinkan HANYA pada debug build.
// Build rilis selalu menjaga halaman soal dengan FLAG_SECURE.
internal const val DEMO_RECORDING = true
