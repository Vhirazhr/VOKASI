package com.example.vokasilearning.logic

import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*
internal const val SCHOOL = "SMK Negeri 1 Singosari"
internal const val DAILY_XP_CAP = 10_000
internal const val SESSION_XP = 20
internal const val QUESTIONS_PER_SESSION = 100
internal const val SESSION_TOTAL_SECONDS = 1_800

// Khusus pengujian/presentasi: rekaman diizinkan HANYA pada debug build.
// Build rilis selalu menjaga halaman soal dengan FLAG_SECURE.
internal const val DEMO_RECORDING = true
