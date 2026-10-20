package com.example.vokasilearning.data

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import com.example.vokasilearning.app.*
import com.example.vokasilearning.navigation.*
import com.example.vokasilearning.domain.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.features.auth.*
import com.example.vokasilearning.features.student.*
import com.example.vokasilearning.features.admin.*
import com.example.vokasilearning.ui.components.*
import com.example.vokasilearning.ui.theme.*
import com.example.vokasilearning.ui.adaptive.*
import com.example.vokasilearning.ui.security.*

internal val demoClasses = listOf(
    Classroom("X TPM 1", "Teknik Pemesinan"),
    Classroom("X TPM 2", "Teknik Pemesinan"),
    Classroom("XI TPM 1", "Teknik Pemesinan"),
    Classroom("XII TPM 1", "Teknik Pemesinan"),
    Classroom("X TE 1", "Teknik Elektronika"),
    Classroom("XI TE 1", "Teknik Elektronika"),
    Classroom("XII TE 1", "Teknik Elektronika"),
    Classroom("X TKR 1", "Teknik Kendaraan Ringan"),
    Classroom("XI TKR 1", "Teknik Kendaraan Ringan"),
    Classroom("XII TKR 1", "Teknik Kendaraan Ringan")
)


internal fun initialStudents() = listOf(
    Student("siswa.demo", "123456", "Aditya Pratama", demoClasses[0], seedXp = 620, seedStreak = 7, seedScore = 90),
    Student("andi", "123456", "Andi Saputra", demoClasses[0], seedXp = 950, seedStreak = 12, seedScore = 100),
    Student("siti", "123456", "Siti Rahma", demoClasses[0], seedXp = 840, seedStreak = 9, seedScore = 95),
    Student("bima", "123456", "Bima Putra", demoClasses[1], seedXp = 790, seedStreak = 10, seedScore = 85),
    Student("nabila", "123456", "Nabila Putri", demoClasses[4], seedXp = 1010, seedStreak = 15, seedScore = 100),
    Student("rizky", "123456", "Rizky Ramadhan", demoClasses[7], seedXp = 740, seedStreak = 8, seedScore = 90),
    Student("dewi", "123456", "Dewi Anggraini", demoClasses[5], seedXp = 700, seedStreak = 11, seedScore = 80),
    Student("farhan", "123456", "Farhan Akbar", demoClasses[2], seedXp = 680, seedStreak = 6, seedScore = 85)
)


// Soal simulasi hanya operasi hitung dasar. Bank soal produksi nantinya dikelola admin.
internal fun initialQuestions(): List<Question> = listOf(
    Question(1, "Penjumlahan", "8 + 7 = ...", listOf("15", "13", "17", "20"), 0),
    Question(2, "Penjumlahan", "16 + 9 = ...", listOf("23", "27", "30", "25"), 3),
    Question(3, "Penjumlahan", "27 + 18 = ...", listOf("47", "50", "45", "43"), 2),
    Question(4, "Penjumlahan", "35 + 26 = ...", listOf("66", "61", "59", "63"), 1),
    Question(5, "Penjumlahan", "48 + 17 = ...", listOf("65", "63", "67", "70"), 0),
    Question(6, "Penjumlahan", "56 + 38 = ...", listOf("92", "96", "99", "94"), 3),
    Question(7, "Penjumlahan", "64 + 29 = ...", listOf("95", "98", "93", "91"), 2),
    Question(8, "Penjumlahan", "75 + 16 = ...", listOf("96", "91", "89", "93"), 1),
    Question(9, "Pengurangan", "18 − 9 = ...", listOf("9", "7", "11", "14"), 0),
    Question(10, "Pengurangan", "23 − 7 = ...", listOf("14", "18", "21", "16"), 3),
    Question(11, "Pengurangan", "42 − 16 = ...", listOf("28", "31", "26", "24"), 2),
    Question(12, "Pengurangan", "57 − 29 = ...", listOf("33", "28", "26", "30"), 1),
    Question(13, "Pengurangan", "64 − 37 = ...", listOf("27", "25", "29", "32"), 0),
    Question(14, "Pengurangan", "83 − 46 = ...", listOf("35", "39", "42", "37"), 3),
    Question(15, "Pengurangan", "90 − 58 = ...", listOf("34", "37", "32", "30"), 2),
    Question(16, "Pengurangan", "100 − 63 = ...", listOf("42", "37", "35", "39"), 1),
    Question(17, "Perkalian", "3 × 8 = ...", listOf("24", "22", "26", "29"), 0),
    Question(18, "Perkalian", "6 × 7 = ...", listOf("40", "44", "47", "42"), 3),
    Question(19, "Perkalian", "7 × 8 = ...", listOf("58", "61", "56", "54"), 2),
    Question(20, "Perkalian", "9 × 6 = ...", listOf("59", "54", "52", "56"), 1),
    Question(21, "Perkalian", "8 × 9 = ...", listOf("72", "70", "74", "77"), 0),
    Question(22, "Perkalian", "12 × 7 = ...", listOf("82", "86", "89", "84"), 3),
    Question(23, "Perkalian", "11 × 8 = ...", listOf("90", "93", "88", "86"), 2),
    Question(24, "Perkalian", "12 × 12 = ...", listOf("149", "144", "142", "146"), 1),
    Question(25, "Perkalian", "15 × 4 = ...", listOf("60", "58", "62", "65"), 0),
    Question(26, "Perkalian", "9 × 9 = ...", listOf("79", "83", "86", "81"), 3),
    Question(27, "Pembagian", "48 ÷ 6 = ...", listOf("10", "13", "8", "6"), 2),
    Question(28, "Pembagian", "72 ÷ 9 = ...", listOf("13", "8", "6", "10"), 1),
    Question(29, "Pembagian", "84 ÷ 7 = ...", listOf("12", "10", "14", "17"), 0)
)
