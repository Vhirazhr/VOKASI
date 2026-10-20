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
import com.example.vokasilearning.*
import com.example.vokasilearning.data.*
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.model.*
import com.example.vokasilearning.screens.*
import com.example.vokasilearning.ui.*

@Composable
internal fun VokasiApp(activity: ComponentActivity) {
    val students = remember { mutableStateListOf<Student>().apply { addAll(initialStudents()) } }
    val bundledQuestions = remember { QuestionBankLoader.load(activity) }
    val bundledIds = remember { bundledQuestions.map { it.id }.toSet() }
    val questions = remember {
        mutableStateListOf<Question>().apply {
            addAll(bundledQuestions)
            addAll(LocalAdminQuestionStore.load(activity).filter { it.id !in bundledIds })
        }
    }
    val attempts = remember { mutableStateListOf<Attempt>().apply { addAll(LocalAttemptStore.load(activity)) } }

    var page by remember { mutableStateOf(Page.LOGIN) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var adminMajorFilter by remember { mutableStateOf("") }
    var adminStudentMajor by remember { mutableStateOf("") }
    var adminClassFilter by remember { mutableStateOf("") }
    var adminDateFilter by remember { mutableStateOf("") }
    var adminQuestionNotice by remember { mutableStateOf<String?>(null) }
    var adminPrefs by remember { mutableStateOf(LocalAdminPreferences.load(activity)) }
    val adminEvents = remember { mutableStateListOf<AdminLogEvent>().apply {
        addAll(LocalAdminLog.load(activity))
    } }
    fun recordAdmin(type: String, detail: String) {
        try {
            LocalAdminLog.append(activity, type, detail)
            adminEvents.clear()
            adminEvents.addAll(LocalAdminLog.load(activity))
        } catch (_: Exception) {
            // Local log is best-effort, never blocks core school administration.
        }
    }

    val quizSession = rememberQuizSession()

    var currentAttempt by remember { mutableStateOf<Attempt?>(null) }
    var reviewingHistory by remember { mutableStateOf(false) }
    var showResultPopup by remember { mutableStateOf(false) }
    var quitDialog by remember { mutableStateOf(false) }
    val me = students.firstOrNull { it.username == currentId }

    fun logout() {
        if (isAdmin) recordAdmin("Masuk", "Admin keluar dari aplikasi")
        currentId = null; isAdmin = false; page = Page.LOGIN
    }
    fun saveQuizCheckpoint() {
        val owner = currentId ?: return
        if (quizSession.questions.isNotEmpty()) {
            QuizProgressStore.save(activity, owner, quizSession.snapshot(owner))
        }
    }
    fun startQuiz(category: String? = null) {
        if (quizSession.start(questions.toList(), category)) {
            saveQuizCheckpoint()
            page = Page.QUIZ
        }
    }
    fun restoreQuiz(owner: String): Boolean {
        val stored = QuizProgressStore.load(activity, owner) ?: return false
        val restored = quizSession.restore(stored)
        if (!restored) QuizProgressStore.clear(activity, owner)
        return restored
    }
    fun finishQuiz() {
        val student = students.firstOrNull { it.username == currentId } ?: return
        if (page != Page.QUIZ || !quizSession.isComplete) return
        val attempt = quizSession.finish(student, attempts.toList())
        attempts.add(attempt)
        LocalAttemptStore.save(activity, attempts.toList())
        QuizProgressStore.clear(activity, student.username)
        currentAttempt = attempt
        reviewingHistory = false
        showResultPopup = true
        page = Page.RESULT
    }

    QuizScreenProtection(activity = activity, enabled = page == Page.QUIZ)

    LaunchedEffect(page, quizSession.sessionId) {
        if (page == Page.QUIZ) {
            var savedIndex = quizSession.index
            while (page == Page.QUIZ && !quizSession.isComplete) {
                if (quizSession.tick()) { saveQuizCheckpoint(); break }
                if (quizSession.index != savedIndex) {
                    saveQuizCheckpoint()
                    savedIndex = quizSession.index
                }
                delay(250L)
            }
            if (page == Page.QUIZ && quizSession.isComplete) finishQuiz()
        }
    }

    DisposableEffect(activity, page, currentId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && page == Page.QUIZ) {
                saveQuizCheckpoint()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = page == Page.QUIZ) { quitDialog = true }
    BackHandler(enabled = page != Page.LOGIN && page != Page.QUIZ && page != Page.HOME && page != Page.ADMIN_HOME) {
        page = when (page) {
            Page.REGISTER -> Page.LOGIN
            Page.ADMIN_ADD -> Page.ADMIN_QUESTIONS
            Page.RESULT -> if (reviewingHistory) Page.PROFILE else Page.HOME
            else -> if (isAdmin) Page.ADMIN_HOME else Page.HOME
        }
    }

    if (quitDialog) {
        AlertDialog(
            onDismissRequest = { quitDialog = false },
            title = { Text("Keluar dari latihan?", fontWeight = FontWeight.Bold) },
            text = { Text("Jawaban sesi ini tidak dinilai dan XP tidak akan diberikan.") },
            confirmButton = { TextButton(onClick = {
                currentId?.let { QuizProgressStore.clear(activity, it) }
                quitDialog = false; quizSession.abandon(); page = Page.HOME
            }) { Text("Keluar", color = Bad) } },
            dismissButton = { TextButton(onClick = { quitDialog = false }) { Text("Lanjut") } }
        )
    }

    val studentTabs = page == Page.HOME || page == Page.LEADERBOARD || page == Page.PROFILE || page == Page.RESULT
    val adminTabs = page == Page.ADMIN_HOME || page == Page.ADMIN_STUDENTS ||
        page == Page.ADMIN_QUESTIONS || page == Page.ADMIN_MONITOR

    AdaptiveAppFrame {
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
                        currentPage = when (page) { Page.RESULT -> Page.HOME; else -> page },
                        onNavigate = { page = it }
                    )
                } else if (adminTabs) {
                    VokasiBottomNav(
                        items = listOf(
                            Triple(Page.ADMIN_HOME, "Ringkasan", "⌂"),
                            Triple(Page.ADMIN_STUDENTS, "Siswa", "◉"),
                            Triple(Page.ADMIN_MONITOR, "Nilai", "◫"),
                            Triple(Page.ADMIN_QUESTIONS, "Soal", "▤")
                        ),
                        currentPage = page,
                        onNavigate = {
                            if (it == Page.ADMIN_STUDENTS) adminStudentMajor = ""
                            page = it
                        }
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
                        onLogin = { u, p ->
                            val username = u.trim()
                            // Demo-only role detection. Production authentication must be server-side.
                            if (username == "admin.demo" && p == "admin123") {
                                currentId = null
                                isAdmin = true
                                page = Page.ADMIN_HOME
                                recordAdmin("Masuk", "Admin membuka panel VOKASI")
                                true
                            } else {
                                val found = students.firstOrNull {
                                    it.username.equals(username, true) && it.password == p
                                }
                                if (found != null) {
                                    currentId = found.username
                                    isAdmin = false
                                    page = if (restoreQuiz(found.username)) Page.QUIZ else Page.HOME
                                    true
                                } else false
                            }
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
                        onStart = { startQuiz() }, onRanking = { page = Page.LEADERBOARD },
                        onPractice = { category -> startQuiz(category) }
                    )
                    Page.QUIZ -> QuizScreen(
                        questions = quizSession.questions, index = quizSession.index,
                        selectedIndex = quizSession.selectedIndex,
                        seconds = quizSession.remainingSeconds,
                        totalSeconds = quizSession.totalRemainingSeconds,
                        feedback = quizSession.feedback,
                        onPick = { selection -> quizSession.choose(selection); saveQuizCheckpoint() },
                        onSubmit = { quizSession.submit(); saveQuizCheckpoint() },
                        onQuit = { quitDialog = true }
                    )
                    Page.RESULT -> if (me != null && currentAttempt != null) ResultPage(
                        student = me, attempt = currentAttempt!!,
                        onBack = { page = if (reviewingHistory) Page.PROFILE else Page.HOME },
                        onHome = { reviewingHistory = false; page = Page.HOME },
                        onRanking = { reviewingHistory = false; page = Page.LEADERBOARD },
                        onPractice = { category -> reviewingHistory = false; startQuiz(category) },
                        openReviewOnEntry = reviewingHistory
                    )
                    Page.LEADERBOARD -> if (me != null) LeaderboardScreen(me, students.toList(), attempts.toList())
                    Page.PROFILE -> if (me != null) ProfileScreen(
                        me, attempts.toList(), onLogout = { logout() },
                        onReview = { attempt -> currentAttempt = attempt; reviewingHistory = true; page = Page.RESULT }
                    )
                    Page.ADMIN_HOME -> AdminHome(
                        students.toList(), questions.toList(), attempts.toList(),
                        onStudents = { chosenMajor ->
                            adminStudentMajor = chosenMajor
                            page = Page.ADMIN_STUDENTS
                        },
                        onMonitoring = { major, classroom, date ->
                            adminMajorFilter = major
                            adminClassFilter = classroom
                            adminDateFilter = date
                            page = Page.ADMIN_MONITOR
                        },
                        onLog = { page = Page.ADMIN_LOG },
                        onProfile = { page = Page.ADMIN_PROFILE },
                        adminName = adminPrefs.displayName,
                        schoolName = adminPrefs.school,
                        onLogout = { logout() }
                    )
                    Page.ADMIN_STUDENTS -> AdminStudentsScreen(
                        students = students.toList(), attempts = attempts.toList(),
                        initialMajor = adminStudentMajor
                    )
                    Page.ADMIN_QUESTIONS -> AdminQuestionsScreen(
                        questions = questions.toList(), bundledIds = bundledIds,
                        onAdd = { page = Page.ADMIN_ADD },
                        onDelete = { id ->
                            if (id in bundledIds) {
                                false
                            } else {
                                val removed = questions.firstOrNull { it.id == id }
                                if (removed == null) {
                                    false
                                } else {
                                    try {
                                        // Persist first. The UI list is updated only after a successful write.
                                        LocalAdminQuestionStore.save(
                                            activity, questions.filter { it.id !in bundledIds && it.id != id }
                                        )
                                        questions.remove(removed)
                                        recordAdmin("Soal", "Menghapus soal tambahan #$id")
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                            }
                        },
                        notice = adminQuestionNotice,
                        onDismissNotice = { adminQuestionNotice = null }
                    )
                    Page.ADMIN_ADD -> AdminAddQuestionScreen(
                        onBack = { page = Page.ADMIN_QUESTIONS },
                        onSave = { category, prompt, options, correct, difficulty, explanation ->
                            val reason = AdminQuestionRules.error(
                                questions.toList(), category, prompt, options, correct,
                                difficulty, explanation
                            )
                            if (reason != null) {
                                reason
                            } else {
                                val seconds = BankPolicy.secondsByCode.getValue(difficulty)
                                val id = (questions.maxOfOrNull { it.id } ?: 0) + 1
                                val newQuestion = Question(
                                    id = id, category = category, question = prompt,
                                    options = options, answerIndex = correct,
                                    difficultyCode = difficulty, timeLimitSeconds = seconds,
                                    explanation = explanation, correctAnswer = options[correct]
                                )
                                try {
                                    BankPolicy.validateBank(
                                        questions.toList() + newQuestion, requireExactly500 = false
                                    )
                                    // Keep the database and list in sync if saving fails.
                                    LocalAdminQuestionStore.save(
                                        activity, questions.filter { it.id !in bundledIds } + newQuestion
                                    )
                                    questions.add(newQuestion)
                                    adminQuestionNotice = "Soal #$id berhasil disimpan di perangkat."
                                    recordAdmin("Soal", "Menambahkan soal #$id ($category, $difficulty)")
                                    page = Page.ADMIN_QUESTIONS
                                    null
                                } catch (_: IllegalArgumentException) {
                                    "Soal tidak sesuai aturan bank. Periksa kode dan kunci."
                                } catch (_: Exception) {
                                    "Gagal menyimpan di perangkat. Coba lagi."
                                }
                            }
                        }
                    )
                    Page.ADMIN_LOG -> AdminLogScreen(
                        events = adminEvents.toList(),
                        onBack = { page = Page.ADMIN_HOME }
                    )
                    Page.ADMIN_PROFILE -> AdminProfileScreen(
                        config = adminPrefs,
                        onSave = { value ->
                            try {
                                val success = LocalAdminPreferences.save(activity, value)
                                if (success) {
                                    adminPrefs = value
                                    recordAdmin("Profil", "Memperbarui nama tampilan admin dan sekolah")
                                }
                                success
                            } catch (_: Exception) { false }
                        },
                        onBack = { page = Page.ADMIN_HOME },
                        onLogout = { logout() }
                    )
                    Page.ADMIN_MONITOR -> AdminMonitorScreen(
                        students.toList(), attempts.toList(),
                        initialMajor = adminMajorFilter,
                        initialClass = adminClassFilter,
                        initialDate = adminDateFilter
                    )
                }
            }
        }
    }

    }

    // One completion modal per newly finished session. Revisiting history never opens it.
    if (page == Page.RESULT && showResultPopup && !reviewingHistory &&
        currentAttempt != null && me != null
    ) {
        CompletionDialog(
            attempt = currentAttempt!!,
            streak = calculateStreak(me, attempts.toList()),
            onViewResults = { showResultPopup = false }
        )
    }

}
