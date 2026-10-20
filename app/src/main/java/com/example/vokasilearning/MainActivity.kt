package com.example.vokasilearning

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

/*
 * VOKASI LEARNING - PREMIUM UI/UX REDESIGN
 * Telah dioptimalkan untuk performa 60 FPS pada perangkat menengah.
 */

// --- DESIGN TOKENS ---
private val Navy = Color(0xFF233F68)
private val NavyLight = Color(0xFF426AA0)
private val DeepNavy = Color(0xFF162D4F)
private val Canvas = Color(0xFFF5F7FB)
private val Ink = Color(0xFF1C2738)
private val Subtle = Color(0xFF748195)
private val Border = Color(0xFFE3EAF1)
private val Good = Color(0xFF23A381)
private val Bad = Color(0xFFE65C66)
private val Warning = Color(0xFFF0AC43)

private val PaleBlue = Color(0xFFE8F0FA)
private val PaleGreen = Color(0xFFE5F4ED)
private val PalePurple = Color(0xFFF1EBFB)
private val PaleGold = Color(0xFFFFF2DB)

private const val SCHOOL = "SMK Negeri 1 Singosari"
private const val DAILY_XP_CAP = 10_000
private const val SESSION_XP = 20
private const val QUESTIONS_PER_SESSION = 10
private const val SECONDS_PER_QUESTION = 60

// Khusus pengujian/presentasi: rekaman diizinkan HANYA pada debug build.
// Build rilis selalu menjaga halaman soal dengan FLAG_SECURE.
private const val DEMO_RECORDING = true

// --- STATE & MODELS ---
private enum class Page {
    LOGIN, REGISTER, HOME, QUIZ, RESULT, LEADERBOARD, PROFILE,
    ADMIN_HOME, ADMIN_QUESTIONS, ADMIN_ADD, ADMIN_MONITOR
}

private data class Classroom(val label: String, val major: String)

private val demoClasses = listOf(
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

private data class Student(
    val username: String,
    val password: String,
    val name: String,
    val classroom: Classroom,
    val photo: ImageBitmap? = null,
    val seedXp: Int = 0,
    val seedStreak: Int = 0,
    val seedScore: Int = 0
)

private data class Question(
    val id: Int, val category: String, val question: String,
    val options: List<String>, val answerIndex: Int
)

private data class Attempt(
    val studentId: String, val timeMillis: Long, val signature: String,
    val correct: Int, val answered: Int, val questionCount: Int,
    val score: Int, val earnedXp: Int, val secondsSpent: Int
)

private data class LevelData(val level: Int, val startXp: Int, val nextXp: Int)
private enum class RankingMetric { XP, STREAK }
private enum class RankingScope { SCHOOL, MAJOR, CLASS }

// --- DUMMY DATA & LOGIC (Tidak diubah) ---
private fun initialStudents() = listOf(
    Student("siswa.demo", "123456", "Aditya Pratama", demoClasses[0], seedXp = 620, seedStreak = 7, seedScore = 90),
    Student("andi", "123456", "Andi Saputra", demoClasses[0], seedXp = 950, seedStreak = 12, seedScore = 100),
    Student("siti", "123456", "Siti Rahma", demoClasses[0], seedXp = 840, seedStreak = 9, seedScore = 95),
    Student("bima", "123456", "Bima Putra", demoClasses[1], seedXp = 790, seedStreak = 10, seedScore = 85),
    Student("nabila", "123456", "Nabila Putri", demoClasses[4], seedXp = 1010, seedStreak = 15, seedScore = 100),
    Student("rizky", "123456", "Rizky Ramadhan", demoClasses[7], seedXp = 740, seedStreak = 8, seedScore = 90),
    Student("dewi", "123456", "Dewi Anggraini", demoClasses[5], seedXp = 700, seedStreak = 11, seedScore = 80),
    Student("farhan", "123456", "Farhan Akbar", demoClasses[2], seedXp = 680, seedStreak = 6, seedScore = 85)
)

private fun initialQuestions(): List<Question> {
    val rows = listOf(
        Triple("Perkalian", "12 × 8 = ...", listOf("88", "96", "108", "112")),
        Triple("Perkalian", "25 × 12 = ...", listOf("250", "300", "320", "350")),
        Triple("Perkalian", "18 × 7 = ...", listOf("116", "124", "126", "136")),
        Triple("Perkalian", "14 × 15 = ...", listOf("190", "200", "210", "215")),
        Triple("Perkalian", "32 × 6 = ...", listOf("182", "192", "202", "212")),
        Triple("Perkalian", "45 × 4 = ...", listOf("160", "170", "180", "190")),
        Triple("Pembagian", "144 ÷ 12 = ...", listOf("10", "11", "12", "14")),
        Triple("Pembagian", "225 ÷ 15 = ...", listOf("12", "13", "15", "18")),
        Triple("Pembagian", "360 ÷ 8 = ...", listOf("40", "45", "48", "50")),
        Triple("Pembagian", "196 ÷ 14 = ...", listOf("12", "13", "14", "16")),
        Triple("Pembagian", "420 ÷ 7 = ...", listOf("50", "55", "60", "65")),
        Triple("Pembagian", "625 ÷ 25 = ...", listOf("20", "22", "25", "30")),
        Triple("Konversi Satuan", "2,5 meter sama dengan ... sentimeter", listOf("25", "250", "2.500", "0,25")),
        Triple("Konversi Satuan", "1.500 milimeter sama dengan ... meter", listOf("0,15", "1,5", "15", "150")),
        Triple("Konversi Satuan", "3 kilogram sama dengan ... gram", listOf("30", "300", "3.000", "30.000")),
        Triple("Konversi Satuan", "0,75 liter sama dengan ... mililiter", listOf("75", "750", "7.500", "0,075")),
        Triple("Konversi Satuan", "2 jam sama dengan ... detik", listOf("120", "3.600", "7.200", "12.000")),
        Triple("Konversi Satuan", "450 sentimeter sama dengan ... meter", listOf("0,45", "4,5", "45", "450")),
        Triple("Rumus Dasar", "Luas persegi panjang 12 cm × 5 cm adalah ...", listOf("17 cm²", "34 cm²", "60 cm²", "120 cm²")),
        Triple("Rumus Dasar", "Keliling persegi dengan sisi 9 cm adalah ...", listOf("18 cm", "27 cm", "36 cm", "81 cm")),
        Triple("Rumus Dasar", "Luas segitiga, alas 10 cm dan tinggi 8 cm, adalah ...", listOf("18 cm²", "40 cm²", "80 cm²", "160 cm²")),
        Triple("Rumus Dasar", "Jarak tempuh pada 60 km/jam selama 2 jam adalah ...", listOf("30 km", "60 km", "90 km", "120 km")),
        Triple("Rumus Dasar", "Volume balok 4 cm × 3 cm × 5 cm adalah ...", listOf("12 cm³", "20 cm³", "60 cm³", "120 cm³")),
        Triple("Rumus Dasar", "Jika 5x = 45, nilai x adalah ...", listOf("5", "8", "9", "10"))
    )
    val correct = listOf(1, 1, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 3, 2, 2)
    return rows.mapIndexed { index, row -> Question(index + 1, row.first, row.second, row.third, correct[index]) }
}

private fun clockText(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

private fun scoreGrade(score: Int): String = when {
    score >= 90 -> "Sangat Baik"
    score >= 75 -> "Baik"
    score >= 60 -> "Cukup"
    else -> "Perlu Latihan"
}

private fun wibCalendar() = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
private fun dayKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
}.format(Date(millis))
private fun seasonKey(timeMillis: Long): String {
    val cal = wibCalendar().apply { timeInMillis = timeMillis }
    return "${cal.get(Calendar.YEAR)}-S${cal.get(Calendar.MONTH) / 3 + 1}"
}
private fun seasonTitle(): String {
    val c = wibCalendar()
    return "Season ${c.get(Calendar.MONTH) / 3 + 1} • ${c.get(Calendar.YEAR)}"
}
private fun levelFor(totalXp: Int): LevelData {
    var level = 1; var threshold = 0; var cost = 100
    while (totalXp >= threshold + cost && level < 500) {
        threshold += cost; level++; cost += 50
    }
    return LevelData(level, threshold, threshold + cost)
}
private fun calculateStreak(student: Student, attempts: List<Attempt>): Int {
    val validDays = attempts.filter { it.studentId == student.username && it.answered > 0 }
        .map { dayKey(it.timeMillis) }.toSet()
    val day = wibCalendar().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    if (dayKey(day.timeInMillis) !in validDays) day.add(Calendar.DAY_OF_YEAR, -1)
    var actual = 0
    while (dayKey(day.timeInMillis) in validDays) { actual++; day.add(Calendar.DAY_OF_YEAR, -1) }
    return max(actual, student.seedStreak)
}
private fun studentSeasonXp(student: Student, attempts: List<Attempt>): Int {
    val currentSeason = seasonKey(System.currentTimeMillis())
    return student.seedXp + attempts.filter {
        it.studentId == student.username && seasonKey(it.timeMillis) == currentSeason
    }.sumOf { it.earnedXp }
}
private fun lifetimeXp(student: Student, attempts: List<Attempt>): Int =
    student.seedXp + attempts.filter { it.studentId == student.username }.sumOf { it.earnedXp }

private fun rankList(
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

// --- MAIN APP COMPONENT ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy, onPrimary = Color.White, secondary = NavyLight,
                    background = Canvas, surface = Color.White,
                    onBackground = Ink, onSurface = Ink
                )
            ) { VokasiApp(this) }
        }
    }
}

@Composable
private fun VokasiApp(activity: ComponentActivity) {
    val students = remember { mutableStateListOf<Student>().apply { addAll(initialStudents()) } }
    val questions = remember { mutableStateListOf<Question>().apply { addAll(initialQuestions()) } }
    val attempts = remember { mutableStateListOf<Attempt>() }

    var page by remember { mutableStateOf(Page.LOGIN) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var isAdmin by remember { mutableStateOf(false) }

    var quizQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var questionIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var sessionId by remember { mutableIntStateOf(0) }

    var currentAttempt by remember { mutableStateOf<Attempt?>(null) }
    var showResultPopup by remember { mutableStateOf(false) }
    var quitDialog by remember { mutableStateOf(false) }
    var sessionStartTime by remember { mutableLongStateOf(0L) }

    val me = students.firstOrNull { it.username == currentId }

    fun logout() { currentId = null; isAdmin = false; page = Page.LOGIN }
    fun startQuiz() {
        if (questions.isEmpty()) return
        quizQuestions = questions.shuffled().take(QUESTIONS_PER_SESSION)
        answers.clear(); questionIndex = 0
        remainingSeconds = quizQuestions.size * SECONDS_PER_QUESTION
        sessionStartTime = System.currentTimeMillis(); sessionId++
        page = Page.QUIZ
    }
    fun finishQuiz() {
        val student = students.firstOrNull { it.username == currentId } ?: return
        if (page != Page.QUIZ) return
        val correct = quizQuestions.count { answers[it.id] == it.answerIndex }
        val score = if (quizQuestions.isNotEmpty()) correct * 100 / quizQuestions.size else 0
        val signature = quizQuestions.map { it.id }.sorted().joinToString("-")
        val now = System.currentTimeMillis()
        val previousSignature = attempts.any { it.studentId == student.username && it.signature == signature && it.earnedXp > 0 }
        val xpToday = attempts.filter { it.studentId == student.username && dayKey(it.timeMillis) == dayKey(now) }.sumOf { it.earnedXp }

        val xp = if (answers.isNotEmpty() && !previousSignature && xpToday < DAILY_XP_CAP) {
            minOf(SESSION_XP, DAILY_XP_CAP - xpToday)
        } else 0

        val attempt = Attempt(
            student.username, now, signature, correct, answers.size,
            quizQuestions.size, score, xp, ((now - sessionStartTime) / 1000).toInt().coerceAtLeast(0)
        )
        attempts.add(attempt)
        currentAttempt = attempt
        showResultPopup = true
        page = Page.RESULT
    }

    // Saat demo (debug build), rekaman layar soal diizinkan.
    // Pada build rilis, FLAG_SECURE tetap otomatis aktif selama mengerjakan soal.
    val isDebugBuild = (activity.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val allowDemoRecording = DEMO_RECORDING && isDebugBuild

    DisposableEffect(page, allowDemoRecording) {
        if (page == Page.QUIZ && !allowDemoRecording) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    LaunchedEffect(page, sessionId) {
        if (page == Page.QUIZ) {
            while (page == Page.QUIZ && remainingSeconds > 0) {
                delay(1000L); if (page == Page.QUIZ) remainingSeconds--
            }
            if (page == Page.QUIZ && remainingSeconds <= 0) finishQuiz()
        }
    }

    BackHandler(enabled = page == Page.QUIZ) { quitDialog = true }
    BackHandler(enabled = page != Page.LOGIN && page != Page.QUIZ && page != Page.HOME && page != Page.ADMIN_HOME) {
        page = when (page) {
            Page.REGISTER -> Page.LOGIN
            Page.ADMIN_ADD -> Page.ADMIN_QUESTIONS
            else -> if (isAdmin) Page.ADMIN_HOME else Page.HOME
        }
    }

    if (quitDialog) {
        AlertDialog(
            onDismissRequest = { quitDialog = false },
            title = { Text("Keluar dari latihan?", fontWeight = FontWeight.Bold) },
            text = { Text("Jawaban sesi ini tidak dinilai dan XP tidak akan diberikan.") },
            confirmButton = { TextButton(onClick = { quitDialog = false; page = Page.HOME }) { Text("Keluar", color = Bad) } },
            dismissButton = { TextButton(onClick = { quitDialog = false }) { Text("Lanjut") } }
        )
    }

    val studentTabs = page == Page.HOME || page == Page.LEADERBOARD || page == Page.PROFILE
    val adminTabs = page == Page.ADMIN_HOME || page == Page.ADMIN_QUESTIONS || page == Page.ADMIN_MONITOR

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            AnimatedVisibility(
                visible = studentTabs || adminTabs,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (studentTabs) {
                    VokasiBottomNav(
                        items = listOf(Triple(Page.HOME, "Beranda", "⌂"), Triple(Page.LEADERBOARD, "Peringkat", "🏆"), Triple(Page.PROFILE, "Profil", "◉")),
                        currentPage = page,
                        onNavigate = { page = it }
                    )
                } else if (adminTabs) {
                    VokasiBottomNav(
                        items = listOf(Triple(Page.ADMIN_HOME, "Ringkasan", "⌂"), Triple(Page.ADMIN_QUESTIONS, "Soal", "▤"), Triple(Page.ADMIN_MONITOR, "Monitoring", "◫")),
                        currentPage = page,
                        onNavigate = { page = it }
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            // Animated Screen Transitions
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 10 }))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 10 }))
                },
                label = "ScreenTransition"
            ) { targetPage ->
                when (targetPage) {
                    Page.LOGIN -> LoginScreen(
                        onStudentLogin = { u, p ->
                            val found = students.firstOrNull { it.username.equals(u.trim(), true) && it.password == p }
                            if (found != null) { currentId = found.username; isAdmin = false; page = Page.HOME; true } else false
                        },
                        onAdminLogin = { u, p ->
                            val good = u.trim() == "admin.demo" && p == "admin123"
                            if (good) { currentId = null; isAdmin = true; page = Page.ADMIN_HOME }
                            good
                        },
                        onRegister = { page = Page.REGISTER }
                    )
                    Page.REGISTER -> RegisterScreen(
                        onBack = { page = Page.LOGIN },
                        onRegister = { n, u, p, c, ph ->
                            if (students.any { it.username.equals(u, true) }) false else {
                                students.add(Student(u, p, n, c, ph))
                                currentId = u; isAdmin = false; page = Page.HOME; true
                            }
                        }
                    )
                    Page.HOME -> if (me != null) StudentHome(
                        me, students.toList(), attempts.toList(), questions.size,
                        onStart = { startQuiz() }, onRanking = { page = Page.LEADERBOARD }
                    )
                    Page.QUIZ -> QuizScreen(
                        questions = quizQuestions, index = questionIndex, answers = answers,
                        seconds = remainingSeconds, onPick = { id, selection -> answers[id] = selection },
                        onNext = { if (questionIndex < quizQuestions.lastIndex) questionIndex++ },
                        onPrevious = { if (questionIndex > 0) questionIndex-- },
                        onFinish = { finishQuiz() }, onQuit = { quitDialog = true }
                    )
                    Page.RESULT -> if (me != null && currentAttempt != null) ResultPage(
                        currentAttempt!!, onHome = { showResultPopup = false; page = Page.HOME },
                        onRanking = { showResultPopup = false; page = Page.LEADERBOARD }
                    )
                    Page.LEADERBOARD -> if (me != null) LeaderboardScreen(me, students.toList(), attempts.toList())
                    Page.PROFILE -> if (me != null) ProfileScreen(me, attempts.toList(), onLogout = { logout() })
                    Page.ADMIN_HOME -> AdminHome(
                        students.toList(), questions.toList(), attempts.toList(),
                        onQuestions = { page = Page.ADMIN_QUESTIONS }, onMonitoring = { page = Page.ADMIN_MONITOR },
                        onLogout = { logout() }
                    )
                    Page.ADMIN_QUESTIONS -> AdminQuestionsScreen(
                        questions.toList(), onAdd = { page = Page.ADMIN_ADD },
                        onDelete = { id -> questions.removeAll { it.id == id } }
                    )
                    Page.ADMIN_ADD -> AdminAddQuestionScreen(
                        onBack = { page = Page.ADMIN_QUESTIONS },
                        onSave = { cat, prm, opts, corr ->
                            val id = (questions.maxOfOrNull { it.id } ?: 0) + 1
                            questions.add(Question(id, cat, prm, opts, corr))
                            page = Page.ADMIN_QUESTIONS
                        }
                    )
                    Page.ADMIN_MONITOR -> AdminMonitorScreen(students.toList(), attempts.toList())
                }
            }
        }
    }

    // Result Popup Overlay
    AnimatedVisibility(
        visible = showResultPopup && me != null && currentAttempt != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        if (me != null && currentAttempt != null) {
            ResultPopup(
                me, students.toList(), attempts.toList(), currentAttempt!!,
                onClose = { showResultPopup = false; page = Page.HOME },
                onAllRanking = { showResultPopup = false; page = Page.LEADERBOARD }
            )
        }
    }
}

// --- REUSABLE PREMIUM COMPONENTS ---

@Composable
private fun VokasiBottomNav(
    items: List<Triple<Page, String, String>>,
    currentPage: Page,
    onNavigate: (Page) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        items.forEach { (target, label, symbol) ->
            val selected = currentPage == target
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(target) },
                icon = {
                    Text(
                        symbol,
                        fontSize = 20.sp,
                        color = if (selected) Navy else Subtle
                    )
                },
                label = {
                    Text(
                        label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Navy else Subtle
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PaleBlue
                )
            )
        }
    }
}

@Composable
private fun VokasiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "ButtonScale"
    )

    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(52.dp).scale(scale),
            shape = RoundedCornerShape(14.dp),
            border = borderStrokeOrDefault(enabled),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = modifier.height(52.dp).scale(scale),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun borderStrokeOrDefault(enabled: Boolean) =
    if (enabled) BorderStroke(1.dp, Navy.copy(alpha = 0.5f)) else BorderStroke(1.dp, Border)

@Composable
private fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Ink,
    prefix: String = "",
    suffix: String = ""
) {
    val animatedValue by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "Counter"
    )
    Text("$prefix$animatedValue$suffix", style = textStyle, color = color, modifier = modifier)
}

@Composable
private fun StaggeredSlideIn(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L) // 60ms delay per item
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        content()
    }
}

@Composable
private fun VokasiTitle(text: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, letterSpacing = (-0.5).sp)
        if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Subtle, lineHeight = 18.sp)
    }
}

@Composable
private fun Pill(text: String, color: Color = PaleBlue, textColor: Color = Navy) {
    Box(Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Avatar(student: Student?, size: Dp = 48.dp) {
    val mod = Modifier.size(size).clip(CircleShape).border(2.dp, Color.White, CircleShape)
    if (student?.photo != null) {
        Image(student.photo, contentDescription = "Foto profil", modifier = mod, contentScale = ContentScale.Crop)
    } else {
        Box(mod.background(Brush.linearGradient(listOf(NavyLight, Navy))), contentAlignment = Alignment.Center) {
            Text(student?.name?.split(" ")?.take(2)?.mapNotNull { it.firstOrNull()?.uppercase() }?.joinToString("") ?: "?",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 3.2f).sp)
        }
    }
}

// --- SCREENS ---

@Composable
private fun LoginScreen(
    onStudentLogin: (String, String) -> Boolean,
    onAdminLogin: (String, String) -> Boolean,
    onRegister: () -> Unit
) {
    var admin by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(Modifier.height(20.dp)) }

        // Premium Header
        item {
            Box(
                Modifier.fillMaxWidth().height(180.dp).shadow(16.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("◈", fontSize = 40.sp, color = Color.White)
                    Text("VOKASI LEARNING", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 1.sp)
                    Text("SMK Negeri 1 Singosari", color = Color(0xFFB0C4DE), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { VokasiTitle("Selamat datang", "Masuk untuk mulai latihan soal harian.") }

        // Role Selector
        item {
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Siswa", true to "Admin").forEach { (isAdmin, label) ->
                    val selected = admin == isAdmin
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Navy else Color.Transparent)
                            .clickable { admin = isAdmin; error = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) Color.White else Subtle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                username, { username = it; error = false },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy)
            )
        }
        item {
            OutlinedTextField(
                password, { password = it; error = false },
                label = { Text("Password") },
                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "Sembunyi" else "Lihat", fontSize = 12.sp, color = Navy)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy)
            )
        }

        if (error) item { Text("Username atau password tidak cocok.", color = Bad, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

        item {
            VokasiButton(
                text = "Masuk",
                onClick = { error = !(if (admin) onAdminLogin(username, password) else onStudentLogin(username, password)) },
                enabled = username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!admin) item {
            TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                Text("Belum punya akun? Daftar sebagai siswa", color = Navy, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            WhiteCard {
                Text("AKUN DEMO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Siswa: siswa.demo / 123456", fontSize = 13.sp, color = Subtle)
                Text("Admin: admin.demo / admin123", fontSize = 13.sp, color = Subtle)
                Spacer(Modifier.height(4.dp))
                Text("Prototype lokal: data akan reset saat aplikasi ditutup.", fontSize = 11.sp, color = Subtle)
            }
        }
    }
}

@Composable
private fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String, Classroom, ImageBitmap?) -> Boolean
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(demoClasses.first()) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var openClasses by remember { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf(false) }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val bitmap = context.contentResolver.openInputStream(it)?.use { stream -> BitmapFactory.decodeStream(stream) }
                if (bitmap != null) image = bitmap.asImageBitmap()
            } catch (_: Exception) { }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Kembali ke login", color = Navy) } }
        item { VokasiTitle("Daftar siswa", "Isi identitas singkat untuk mengatur kelas dan peringkatmu.") }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (image == null) Avatar(null, 72.dp) else Image(image!!, null, Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Column {
                    Text("Foto profil (opsional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    TextButton(onClick = { chooser.launch("image/*") }) { Text("Pilih dari galeri", color = Navy) }
                }
            }
        }

        item { OutlinedTextField(name, { name = it }, label = { Text("Nama lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(username, { username = it.trim() }, label = { Text("Username") }, supportingText = { Text("Tanpa spasi, minimal 3 karakter") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(password, { password = it }, label = { Text("Password (min. 6 karakter)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        item {
            Text("Jurusan & kelas (contoh)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Box {
                OutlinedButton(onClick = { openClasses = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("${chosen.label} • ${chosen.major}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                }
                DropdownMenu(expanded = openClasses, onDismissRequest = { openClasses = false }) {
                    demoClasses.forEach { classroom ->
                        DropdownMenuItem(text = { Text("${classroom.label} — ${classroom.major}") }, onClick = { chosen = classroom; openClasses = false })
                    }
                }
            }
        }
        item { OutlinedTextField(joinCode, { joinCode = it }, label = { Text("Kode kelas (opsional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        if (duplicate) item { Text("Username sudah dipakai.", color = Bad, fontSize = 13.sp) }

        item {
            VokasiButton(
                text = "Buat Akun & Masuk",
                onClick = { duplicate = !onRegister(name.trim(), username.trim(), password, chosen, image) },
                enabled = name.trim().isNotEmpty() && username.matches(Regex("[A-Za-z0-9._]{3,}")) && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StudentHome(
    me: Student, students: List<Student>, attempts: List<Attempt>, countQuestions: Int,
    onStart: () -> Unit, onRanking: () -> Unit
) {
    val todayCount = attempts.count { it.studentId == me.username && dayKey(it.timeMillis) == dayKey(System.currentTimeMillis()) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            // Premium Header
            Column(
                Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy)))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(SCHOOL.uppercase(), color = Color(0xFFB0C4DE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Halo, ${me.name.substringBefore(' ')}! 👋", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${me.classroom.label} • Siap mengerjakan soal?", color = Color(0xFFD5E2F0), fontSize = 13.sp)
                    }
                    Avatar(me, 52.dp)
                }

                // Hero Card (Latihan Wajib)
                Card(
                    Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("LATIHAN UTAMA", PaleBlue, Navy)
                            Spacer(Modifier.weight(1f))
                            Text("Hari ini", fontSize = 12.sp, color = Subtle, fontWeight = FontWeight.Medium)
                        }
                        Text("Latihan Wajib", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Soal acak otomatis dari perkalian, pembagian, konversi satuan, dan rumus dasar.", fontSize = 13.sp, color = Subtle, lineHeight = 18.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("${minOf(QUESTIONS_PER_SESSION, countQuestions)} soal", Canvas, Ink)
                            Pill("${minOf(QUESTIONS_PER_SESSION, countQuestions) * SECONDS_PER_QUESTION / 60} mnt", Canvas, Ink)
                            Pill("Anti-screenshot", PaleGreen, Good)
                        }

                        VokasiButton(
                            text = "Mulai Mengerjakan →",
                            onClick = onStart,
                            enabled = countQuestions > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Info & Stats
        item {
            StaggeredSlideIn(0) {
                WhiteCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistik Sesi", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Pill(seasonTitle(), PaleGold, Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Sesi hari ini", color = Subtle, fontSize = 12.sp)
                            Text("$todayCount", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Navy)
                        }
                        Column {
                            Text("Total XP", color = Subtle, fontSize = 12.sp)
                            AnimatedCounter(target = studentSeasonXp(me, attempts), textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Navy)
                        }
                    }
                }
            }
        }

        // Leaderboard Preview
        item {
            StaggeredSlideIn(1) {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Top 3 Peringkat", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRanking) { Text("Lihat semua →", fontWeight = FontWeight.SemiBold, color = Navy) }
                }
            }
        }

        item {
            StaggeredSlideIn(2) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val leaders = rankList(students, attempts, me, RankingScope.SCHOOL, RankingMetric.XP).take(3)
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Se-Sekolah", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Pill("XP", PaleBlue, Navy)
                        }
                        leaders.forEachIndexed { rank, student ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(listOf("🥇", "🥈", "🥉")[rank], fontSize = 20.sp)
                                Avatar(student, 36.dp)
                                Text(student.name, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.Medium)
                                Text("${studentSeasonXp(student, attempts)} XP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizScreen(
    questions: List<Question>, index: Int, answers: Map<Int, Int>, seconds: Int,
    onPick: (Int, Int) -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onFinish: () -> Unit, onQuit: () -> Unit
) {
    if (questions.isEmpty()) return
    val current = questions[index]

    Column(Modifier.fillMaxSize().background(Canvas)) {
        // Header
        Column(Modifier.fillMaxWidth().background(Navy).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onQuit) { Text("‹ Keluar", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                // Animated timer color
                val timerColor by animateColorAsState(
                    targetValue = if (seconds < 60) Bad else Color.White,
                    label = "TimerColor"
                )
                Pill("⏱ ${clockText(seconds)}", Color.White.copy(alpha = 0.15f), timerColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Soal ${index + 1} dari ${questions.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("${answers.size} dijawab", color = Color(0xFFB0C4DE), fontSize = 12.sp)
            }
            // Smooth Progress Bar
            val progress by animateFloatAsState(
                targetValue = (index + 1f) / questions.size,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "ProgressBar"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color(0xFF92C7F3), trackColor = Color(0xFF506E91)
            )
        }

        // Animated Question Content
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { 100 }) + fadeIn()).togetherWith(
                    slideOutHorizontally(targetOffsetX = { -100 }) + fadeOut()
                )
            },
            modifier = Modifier.weight(1f),
            label = "QuestionTransition"
        ) { q ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Pill(q.category, PalePurple, Navy)

                WhiteCard(Modifier.fillMaxWidth()) {
                    Text(
                        q.question, color = Ink, fontSize = 22.sp, lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text("Pilih satu jawaban:", color = Subtle, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                q.options.forEachIndexed { optionIndex, text ->
                    val selected = answers[q.id] == optionIndex
                    val bgColor by animateColorAsState(if (selected) PaleBlue else Color.White, label = "OptBg")
                    val borderColor by animateColorAsState(if (selected) Navy else Border, label = "OptBorder")

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable { onPick(q.id, optionIndex) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (selected) Navy else Canvas),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${'A' + optionIndex}", color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f), color = Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        // Bottom Navigation (Prev/Next)
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VokasiButton(
                text = "Sebelumnya",
                onClick = onPrevious,
                enabled = index > 0,
                isSecondary = true,
                modifier = Modifier.weight(1f)
            )
            VokasiButton(
                text = if (index == questions.lastIndex) "Selesai" else "Berikutnya →",
                onClick = if (index == questions.lastIndex) onFinish else onNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultPage(attempt: Attempt, onHome: () -> Unit, onRanking: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        VokasiTitle("Hasil Latihan", "Nilai langsung dihitung setelah sesi selesai.")

        WhiteCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedCounter(
                    target = attempt.score, suffix = "%",
                    textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Navy
                )
                Text("${attempt.correct} benar dari ${attempt.questionCount} soal", color = Subtle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Pill("Grade: ${scoreGrade(attempt.score)}", PaleGreen, Good)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill("+${attempt.earnedXp} XP", PaleGold, Ink)
                    Pill("${attempt.answered} dijawab", Canvas, Ink)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        VokasiButton(text = "Kembali ke Beranda", onClick = onHome, modifier = Modifier.fillMaxWidth())
        VokasiButton(text = "Lihat Papan Peringkat", onClick = onRanking, isSecondary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ResultPopup(
    me: Student, users: List<Student>, attempts: List<Attempt>, attempt: Attempt,
    onClose: () -> Unit, onAllRanking: () -> Unit
) {
    val scores = users.filter { it.classroom.label == me.classroom.label }.map { user ->
        val last = attempts.lastOrNull { it.studentId == user.username }
        user to (last?.score ?: user.seedScore)
    }.sortedByDescending { it.second }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth(0.92f).heightIn(max = 700.dp).shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Latihan Selesai!", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Nilai dihitung otomatis", color = Subtle, fontSize = 13.sp)
                }

                // Animated Stats Row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultNumber(label = "Nilai", amount = attempt.score, suffix = "%", modifier = Modifier.weight(1f))
                    ResultNumber(label = "XP", amount = attempt.earnedXp, prefix = "+", modifier = Modifier.weight(1f), color = Good)
                    ResultNumber(label = "Streak", amount = calculateStreak(me, attempts), prefix = "🔥 ", modifier = Modifier.weight(1f), color = Warning)
                }

                Text("Peringkat Kelas • ${me.classroom.label}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)

                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(scores) { index, pair ->
                        Row(
                            Modifier.fillMaxWidth().background(if (pair.first.username == me.username) PaleBlue else Canvas, RoundedCornerShape(12.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Subtle, modifier = Modifier.width(24.dp))
                            Avatar(pair.first, 32.dp)
                            Text(pair.first.name, Modifier.weight(1f), fontSize = 13.sp, fontWeight = if (pair.first.username == me.username) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${pair.second}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                VokasiButton(text = "Tutup / Beranda", onClick = onClose, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onAllRanking, modifier = Modifier.fillMaxWidth()) {
                    Text("Buka papan peringkat lengkap", color = Navy, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ResultNumber(label: String, amount: Int, prefix: String = "", suffix: String = "", modifier: Modifier, color: Color = Navy) {
    Column(modifier.background(Canvas, RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Subtle, fontWeight = FontWeight.Medium)
        AnimatedCounter(
            target = amount, prefix = prefix, suffix = suffix,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = color
        )
    }
}

@Composable
private fun LeaderboardScreen(me: Student, users: List<Student>, attempts: List<Attempt>) {
    var metric by remember { mutableStateOf(RankingMetric.XP) }
    var scope by remember { mutableStateOf(RankingScope.SCHOOL) }
    var chosenMajor by remember { mutableStateOf(me.classroom.major) }
    var chosenClass by remember { mutableStateOf(me.classroom.label) }
    var filterOpen by remember { mutableStateOf(false) }

    val groups = if (scope == RankingScope.CLASS) demoClasses.map { it.label }.distinct() else demoClasses.map { it.major }.distinct()
    val groupValue = if (scope == RankingScope.CLASS) chosenClass else chosenMajor
    val ordered = rankList(users, attempts, me, scope, metric, chosenMajor, chosenClass)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { VokasiTitle("Papan Peringkat 🏆", "${seasonTitle()} • $SCHOOL") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RankingMetric.XP to "XP Season", RankingMetric.STREAK to "🔥 Streak").forEach { (m, title) ->
                    val selected = metric == m
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Navy else Color.White)
                            .border(1.dp, if (selected) Navy else Border, RoundedCornerShape(12.dp))
                            .clickable { metric = m }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, color = if (selected) Color.White else Subtle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(RankingScope.SCHOOL to "Sekolah", RankingScope.MAJOR to "Jurusan", RankingScope.CLASS to "Kelas").forEach { (s, title) ->
                    FilterChip(
                        selected = scope == s, onClick = { scope = s },
                        label = { Text(title, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaleBlue, selectedLabelColor = Navy)
                    )
                }
            }
        }

        if (scope != RankingScope.SCHOOL) item {
            Box {
                OutlinedButton(onClick = { filterOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Pilih ${if (scope == RankingScope.CLASS) "kelas" else "jurusan"}: $groupValue", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    groups.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            if (scope == RankingScope.CLASS) chosenClass = option else chosenMajor = option
                            filterOpen = false
                        })
                    }
                }
            }
        }

        // Podium Top 3 (If available)
        if (ordered.size >= 3) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    // Rank 2
                    PodiumItem(ordered[1], 2, metric, attempts, Modifier.weight(1f).height(160.dp))
                    // Rank 1
                    PodiumItem(ordered[0], 1, metric, attempts, Modifier.weight(1f).height(190.dp))
                    // Rank 3
                    PodiumItem(ordered[2], 3, metric, attempts, Modifier.weight(1f).height(140.dp))
                }
            }
        }

        itemsIndexed(ordered, key = { _, student -> student.username }) { i, student ->
            if (i >= 3 || ordered.size < 3) {
                StaggeredSlideIn(i) {
                    WhiteCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${i + 1}.", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = Subtle)
                            Avatar(student, 44.dp)
                            Column(Modifier.weight(1f)) {
                                Text(student.name + if (student.username == me.username) " (Kamu)" else "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, maxLines = 1)
                                Text("${student.classroom.label} • LV ${levelFor(lifetimeXp(student, attempts)).level}", color = Subtle, fontSize = 11.sp)
                            }
                            Text(
                                if (metric == RankingMetric.XP) "${studentSeasonXp(student, attempts)} XP" else "🔥 ${calculateStreak(student, attempts)}",
                                color = Navy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumItem(student: Student, rank: Int, metric: RankingMetric, attempts: List<Attempt>, modifier: Modifier = Modifier) {
    val gradient = when(rank) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFA9A9A9)))
        else -> Brush.verticalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(gradient).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(if(rank == 1) "👑" else "", fontSize = 24.sp)
        Avatar(student, if(rank == 1) 60.dp else 48.dp)
        Spacer(Modifier.height(8.dp))
        Text(student.name.substringBefore(' '), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (metric == RankingMetric.XP) "${studentSeasonXp(student, attempts)} XP" else "🔥 ${calculateStreak(student, attempts)}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileScreen(me: Student, attempts: List<Attempt>, onLogout: () -> Unit) {
    val mine = attempts.filter { it.studentId == me.username }
    val level = levelFor(lifetimeXp(me, attempts))
    val answered = mine.sumOf { it.questionCount }
    val accuracy = if (answered > 0) mine.sumOf { it.correct } * 100 / answered else me.seedScore
    val totalXp = lifetimeXp(me, attempts)
    var showLogout by remember { mutableStateOf(false) }

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(totalXp) {
        progressAnim.animateTo(
            targetValue = ((totalXp - level.startXp).toFloat() / (level.nextXp - level.startXp)).coerceIn(0f, 1f),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { VokasiTitle("Profil Siswa", "Riwayat dan perkembangan belajarmu") }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Avatar(me, 76.dp)
                    Column(Modifier.weight(1f)) {
                        Text(me.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("@${me.username}", color = Subtle, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Pill("${me.classroom.label} • ${me.classroom.major}", PaleBlue, Navy)
                    }
                }
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Level ${level.level}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
                    Pill("$totalXp XP", PaleGold, Ink)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Good, trackColor = PaleGreen
                )
                Spacer(Modifier.height(4.dp))
                Text("${max(0, level.nextXp - totalXp)} XP lagi untuk naik level", color = Subtle, fontSize = 12.sp)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Akurasi", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = accuracy, suffix = "%", textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Streak", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = calculateStreak(me, attempts), prefix = "🔥 ", textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Warning)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("XP Season", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = studentSeasonXp(me, attempts), textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Navy)
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Sesi Selesai", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = mine.size, textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        item { Text("Riwayat Pengerjaan", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 8.dp)) }

        if (mine.isEmpty()) item {
            WhiteCard(Modifier.fillMaxWidth()) { Text("Belum ada latihan yang kamu selesaikan.", color = Subtle, fontSize = 13.sp) }
        }

        items(mine.reversed()) { a ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Latihan Acak", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                        Text(dayKey(a.timeMillis), color = Subtle, fontSize = 12.sp)
                    }
                    Pill("${a.score}%  •  +${a.earnedXp} XP", PaleGreen, Good)
                }
            }
        }

        item { VokasiButton(text = "Keluar dari Akun", onClick = { showLogout = true }, isSecondary = true, modifier = Modifier.fillMaxWidth()) }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Keluar akun?", fontWeight = FontWeight.Bold) },
            text = { Text("Kamu perlu login kembali untuk mengakses aplikasi.") },
            confirmButton = { TextButton(onClick = { showLogout = false; onLogout() }) { Text("Keluar", color = Bad) } },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Batal") } }
        )
    }
}

// --- ADMIN SCREENS ---

@Composable
private fun AdminHome(
    students: List<Student>, questions: List<Question>, attempts: List<Attempt>,
    onQuestions: () -> Unit, onMonitoring: () -> Unit, onLogout: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepNavy))).padding(24.dp)
            ) {
                Text("VOKASI • ADMIN", color = Color(0xFFB0C4DE), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Panel Pengelola", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(SCHOOL, color = Color(0xFFD5E2F0), fontSize = 14.sp)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhiteCard(Modifier.weight(1f)) {
                    Text("Siswa demo", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = students.size, textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                WhiteCard(Modifier.weight(1f)) {
                    Text("Soal tersedia", color = Subtle, fontSize = 12.sp)
                    AnimatedCounter(target = questions.size, textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Kelola Soal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text("Tambah pertanyaan, empat pilihan jawaban, dan kunci jawabannya.", color = Subtle, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                VokasiButton(text = "Buka Bank Soal →", onClick = onQuestions, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Monitoring", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text("Lihat siswa, kelas, XP, dan hasil latihan yang tercatat secara lokal.", color = Subtle, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                VokasiButton(text = "Lihat Monitoring →", onClick = onMonitoring, isSecondary = true, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Aturan otomatis sistem", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("• 10 soal acak per sesi", color = Subtle, fontSize = 13.sp)
                Text("• 60 detik per soal", color = Subtle, fontSize = 13.sp)
                Text("• +20 XP per sesi, max 10.000/hari", color = Subtle, fontSize = 13.sp)
                Text("• Season berlangsung 3 bulan", color = Subtle, fontSize = 13.sp)
            }
        }

        item { VokasiButton(text = "Keluar dari Admin", onClick = onLogout, isSecondary = true, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun AdminQuestionsScreen(questions: List<Question>, onAdd: () -> Unit, onDelete: (Int) -> Unit) {
    var category by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua") + questions.map { it.category }.distinct()
    var deleteId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { VokasiTitle("Bank Soal", "Pertanyaan langsung, tanpa materi atau kisi-kisi.") }
        item { VokasiButton(text = "+ Tambah Soal", onClick = onAdd, modifier = Modifier.fillMaxWidth()) }

        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    FilterChip(
                        selected = item == category,
                        onClick = { category = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaleBlue, selectedLabelColor = Navy)
                    )
                }
            }
        }

        items(questions.filter { category == "Semua" || it.category == category }, key = { it.id }) { q ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(q.category)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { deleteId = q.id }) { Text("Hapus", color = Bad, fontWeight = FontWeight.Bold) }
                }
                Text(q.question, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                Text("Kunci: ${'A' + q.answerIndex}. ${q.options[q.answerIndex]}", color = Good, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Hapus soal?", fontWeight = FontWeight.Bold) },
            text = { Text("Soal ini akan hilang dari prototipe.") },
            confirmButton = { TextButton(onClick = { deleteId?.let(onDelete); deleteId = null }) { Text("Hapus", color = Bad) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun AdminAddQuestionScreen(onBack: () -> Unit, onSave: (String, String, List<String>, Int) -> Unit) {
    var category by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    val choices = remember { mutableStateListOf("", "", "", "") }
    var correct by remember { mutableIntStateOf(0) }

    val valid = category.isNotBlank() && prompt.isNotBlank() && choices.all { it.isNotBlank() } && choices.map { it.trim() }.distinct().size == 4

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Kembali ke Bank Soal", color = Navy) } }
        item { VokasiTitle("Tambah Soal Baru", "Admin cukup mengisi soal, opsi jawaban, dan kunci.") }

        item { OutlinedTextField(category, { category = it }, label = { Text("Kategori (contoh: Perkalian)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
        item { OutlinedTextField(prompt, { prompt = it }, label = { Text("Pertanyaan") }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }

        items(4) { i ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(
                    selected = correct == i,
                    onClick = { correct = i },
                    colors = RadioButtonDefaults.colors(selectedColor = Navy)
                )
                OutlinedTextField(
                    value = choices[i], onValueChange = { choices[i] = it },
                    label = { Text("Jawaban ${'A' + i}") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item { Text("Pilih lingkaran di kiri jawaban yang BENAR.", color = Subtle, fontSize = 12.sp) }
        item {
            VokasiButton(
                text = "Simpan Soal",
                onClick = { onSave(category.trim(), prompt.trim(), choices.map { it.trim() }, correct) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AdminMonitorScreen(students: List<Student>, attempts: List<Attempt>) {
    var selectedClass by remember { mutableStateOf("Semua kelas") }
    var filterOpen by remember { mutableStateOf(false) }
    val group = listOf("Semua kelas") + demoClasses.map { it.label }.distinct()
    val filtered = students.filter { selectedClass == "Semua kelas" || it.classroom.label == selectedClass }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { VokasiTitle("Monitoring Siswa", "Ringkasan hasil dan progres latihan • dummy") }

        item {
            Box {
                OutlinedButton(onClick = { filterOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(selectedClass, color = Ink)
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    group.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { selectedClass = item; filterOpen = false })
                    }
                }
            }
        }

        items(filtered, key = { it.username }) { student ->
            WhiteCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(student)
                    Column(Modifier.weight(1f)) {
                        Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("${student.classroom.label} • ${student.classroom.major}", fontSize = 12.sp, color = Subtle)
                    }
                    Pill("${studentSeasonXp(student, attempts)} XP", PaleBlue, Navy)
                }
                Text("Streak: ${calculateStreak(student, attempts)} hari  •  Sesi baru: ${attempts.count { it.studentId == student.username }}", color = Subtle, fontSize = 12.sp)
            }
        }
    }
}