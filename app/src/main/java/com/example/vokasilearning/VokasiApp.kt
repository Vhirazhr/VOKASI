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
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
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
    // Avoid loading another user's cached school directory before authentication.
    var schoolDirectory by remember { mutableStateOf(SchoolDirectory()) }
    val students = remember { mutableStateListOf<Student>() }
    val bundledQuestions = remember { QuestionBankLoader.load(activity) }
    val bundledIds = remember { bundledQuestions.map { it.id }.toSet() }
    val questions = remember {
        mutableStateListOf<Question>().apply {
            addAll(bundledQuestions)
            addAll(LocalAdminQuestionStore.load(activity).filter { it.id !in bundledIds })
        }
    }
    val attempts = remember { mutableStateListOf<Attempt>().apply { addAll(LocalAttemptStore.load(activity)) } }

    val onboardingPrefs = remember { activity.getSharedPreferences("vokasi_onboarding", android.content.Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(
        if (onboardingPrefs.getBoolean("seen_v1", false)) Page.LOGIN else Page.ONBOARDING
    ) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var isRemoteStudent by remember { mutableStateOf(false) }
    var adminMajorFilter by remember { mutableStateOf("") }
    var adminStudentMajor by remember { mutableStateOf("") }
    var adminClassFilter by remember { mutableStateOf("") }
    var adminDateFilter by remember { mutableStateOf("") }
    var adminQuestionNotice by remember { mutableStateOf<String?>(null) }
    var adminHeaderReturn by remember { mutableStateOf(Page.ADMIN_HOME) }
    var studentHeaderReturn by remember { mutableStateOf(Page.HOME) }
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
    val appScope = rememberCoroutineScope()

    var currentAttempt by remember { mutableStateOf<Attempt?>(null) }
    var reviewingHistory by remember { mutableStateOf(false) }
    var showResultPopup by remember { mutableStateOf(false) }
    var quitDialog by remember { mutableStateOf(false) }
    val me = students.firstOrNull { it.username == currentId }

    fun logout() {
        if (isAdmin) recordAdmin("Masuk", "Admin keluar dari aplikasi")
        appScope.launch {
            // Complete sign-out before presenting another login, so a pending
            // sign-out cannot accidentally invalidate the next person's session.
            if (isAdmin || isRemoteStudent) {
                try { vokasiSupabase.auth.signOut() } catch (_: Exception) { }
            }
            currentId = null
            isAdmin = false
            isRemoteStudent = false
            schoolDirectory = SchoolDirectory()
            students.clear()
            // Clear legacy local directory files that could expose an admin's
            // cached roster to the next user of this shared Android device.
            try { SchoolDirectoryStore.save(activity, SchoolDirectory()) } catch (_: Exception) { }
            page = Page.LOGIN
        }
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
        recordAdmin(
            "Nilai",
            "${student.username} • ${attempt.practiceCategory?.let { "Latihan $it" } ?: "Wajib"} • Nilai ${attempt.score}/100"
        )
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
    BackHandler(enabled = page != Page.ONBOARDING && page != Page.LOGIN &&
        page != Page.QUIZ && page != Page.HOME && page != Page.ADMIN_HOME) {
        page = when (page) {
            Page.REGISTER -> Page.LOGIN
            Page.ADMIN_ADD -> Page.ADMIN_QUESTIONS
            Page.ADMIN_LOG -> Page.ADMIN_PROFILE
            Page.ADMIN_NOTIFICATIONS, Page.ADMIN_PROFILE -> adminHeaderReturn
            Page.ADMIN_SCHOOL -> Page.ADMIN_HOME
            Page.STUDENT_SETTINGS, Page.STUDENT_NOTIFICATIONS -> studentHeaderReturn
            Page.ASSISTANT -> if (isAdmin) adminHeaderReturn else studentHeaderReturn
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

    val studentTabs = page == Page.HOME || page == Page.LEADERBOARD ||
        page == Page.PROFILE || page == Page.RESULT ||
        page == Page.STUDENT_SETTINGS || page == Page.STUDENT_NOTIFICATIONS ||
        (page == Page.ASSISTANT && !isAdmin)
    val adminTabs = page == Page.ADMIN_HOME || page == Page.ADMIN_STUDENTS ||
        page == Page.ADMIN_QUESTIONS || page == Page.ADMIN_MONITOR ||
        page == Page.ADMIN_PROFILE || page == Page.ADMIN_LOG ||
        page == Page.ADMIN_SCHOOL || page == Page.ADMIN_NOTIFICATIONS ||
        (page == Page.ASSISTANT && isAdmin)

    AdaptiveAppFrame {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAdminHeaderActions provides if (isAdmin) AdminHeaderActions(
            onNotifications = {
                if (page != Page.ADMIN_NOTIFICATIONS) {
                    if (page != Page.ADMIN_PROFILE && page != Page.ASSISTANT) adminHeaderReturn = page
                    page = Page.ADMIN_NOTIFICATIONS
                }
            },
            onSettings = {
                if (page != Page.ADMIN_PROFILE) {
                    if (page != Page.ADMIN_NOTIFICATIONS && page != Page.ASSISTANT) adminHeaderReturn = page
                    page = Page.ADMIN_PROFILE
                }
            },
            onAssistant = {
                if (page != Page.ASSISTANT) {
                    if (page != Page.ADMIN_PROFILE && page != Page.ADMIN_NOTIFICATIONS) adminHeaderReturn = page
                    page = Page.ASSISTANT
                }
            }
        ) else AdminHeaderActions(
            onNotifications = {
                if (page != Page.STUDENT_NOTIFICATIONS) {
                    if (page != Page.STUDENT_SETTINGS && page != Page.ASSISTANT) studentHeaderReturn = page
                    page = Page.STUDENT_NOTIFICATIONS
                }
            },
            onSettings = {
                if (page != Page.STUDENT_SETTINGS) {
                    if (page != Page.STUDENT_NOTIFICATIONS && page != Page.ASSISTANT) studentHeaderReturn = page
                    page = Page.STUDENT_SETTINGS
                }
            },
            onAssistant = {
                if (page != Page.ASSISTANT) {
                    if (page != Page.STUDENT_SETTINGS && page != Page.STUDENT_NOTIFICATIONS) studentHeaderReturn = page
                    page = Page.ASSISTANT
                }
            }
        )
    ) {
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
                        items = listOf(
                            Triple(Page.HOME, "Beranda", "⌂"),
                            Triple(Page.LEADERBOARD, "Peringkat", "🏆"),
                            Triple(Page.PROFILE, "Profil", "◉")
                        ),
                        currentPage = when (page) {
                            Page.RESULT -> Page.HOME
                            Page.STUDENT_SETTINGS, Page.STUDENT_NOTIFICATIONS, Page.ASSISTANT ->
                                if (studentHeaderReturn == Page.PROFILE) Page.PROFILE else Page.HOME
                            else -> page
                        },
                        onNavigate = { page = it }
                    )
                } else if (adminTabs) {
                    VokasiBottomNav(
                        items = listOf(
                            Triple(Page.ADMIN_HOME, "Ringkasan", "⌂"),
                            Triple(Page.ADMIN_STUDENTS, "Siswa", "◉"),
                            Triple(Page.ADMIN_MONITOR, "Nilai", "◫"),
                            Triple(Page.ADMIN_QUESTIONS, "Soal", "▤"),
                            Triple(Page.ADMIN_SCHOOL, "Sekolah", "⌂")
                        ),
                        currentPage = when (page) {
                            Page.ADMIN_LOG, Page.ADMIN_NOTIFICATIONS, Page.ADMIN_PROFILE, Page.ASSISTANT -> Page.ADMIN_HOME
                            else -> page
                        },
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
                    Page.ONBOARDING -> VokasiOnboardingScreen(onFinish = {
                        onboardingPrefs.edit().putBoolean("seen_v1", true).apply()
                        page = Page.LOGIN
                    })
                    Page.ASSISTANT -> VokasiAssistantScreen(isAdmin = isAdmin)
                    Page.STUDENT_NOTIFICATIONS -> if (me != null) StudentNotificationsScreen(
                        me = me, attempts = attempts.toList(),
                        onOpenPractice = { page = Page.HOME },
                        onOpenSettings = { page = Page.STUDENT_SETTINGS },
                        onBack = { page = studentHeaderReturn }
                    )
                    Page.STUDENT_SETTINGS -> if (me != null) StudentAccountScreen(
                        me = me,
                        onBack = { page = studentHeaderReturn },
                        onPhoto = { bitmap ->
                            try {
                                val roster = schoolDirectory.roster.firstOrNull {
                                    it.username == me.username && it.registered
                                } ?: throw IllegalArgumentException("Data siswa tidak ditemukan.")
                                val photo = StudentAccountStore.savePhoto(activity, roster.id, bitmap)
                                val i = students.indexOfFirst { it.username == me.username }
                                if (i >= 0) students[i] = students[i].copy(photo = photo)
                                null
                            } catch (e: Exception) {
                                e.message ?: "Gagal menyimpan foto."
                            }
                        },
                        onSave = { username, oldPassword, newPassword ->
                            if (isRemoteStudent) {
                                try {
                                    SupabaseSchoolRepository.updateStudentPassword(
                                        username, oldPassword, newPassword
                                    )
                                    null
                                } catch (e: IllegalArgumentException) {
                                    e.message ?: "Password tidak berhasil diperbarui."
                                } catch (_: Exception) {
                                    "Gagal memperbarui password di server. Periksa koneksi."
                                }
                            } else {
                                try {
                                    val current = students.first { it.username == me.username }
                                    val next = StudentAccountStore.updatedRoster(
                                        schoolDirectory, current, oldPassword, username, newPassword
                                    )
                                    val newId = next.roster.first {
                                        it.registered && it.fullName == current.name &&
                                            it.admissionYear == current.admissionYear &&
                                            it.classLabel == current.classroom.label
                                    }.username
                                    val updatedAttempts = attempts.map {
                                        if (it.studentId == current.username) it.copy(studentId = newId)
                                        else it
                                    }
                                    SchoolDirectoryStore.save(activity, next)
                                    if (newId != current.username) {
                                        LocalAttemptStore.save(activity, updatedAttempts)
                                        QuizProgressStore.moveOwner(activity, current.username, newId)
                                        StudentNoticeStore.migrate(activity, current.username, newId)
                                    }
                                    schoolDirectory = next
                                    if (newId != current.username) {
                                        attempts.clear(); attempts.addAll(updatedAttempts)
                                    }
                                    val account = next.roster.first { it.username == newId }
                                    val i = students.indexOfFirst { it.username == current.username }
                                    if (i >= 0) students[i] = current.copy(
                                        username = newId, password = account.passwordHash
                                    )
                                    currentId = newId
                                    null
                                } catch (e: IllegalArgumentException) {
                                    e.message ?: "Data akun tidak valid."
                                } catch (_: Exception) { "Pengaturan akun gagal disimpan." }
                            }
                        }
                    )
                    Page.LOGIN -> LoginScreen(
                        onLogin = { u, p ->
                            val login = u.trim()
                            if (login.contains("@")) {
                                if (!authenticateSchoolAdmin(login, p)) {
                                    false
                                } else {
                                    // Supabase is the source of truth. A failed directory
                                    // refresh must never present stale, local-only students
                                    // as if they were synchronized school records.
                                    try {
                                        val remote = SupabaseSchoolRepository.loadDirectory()
                                        schoolDirectory = remote
                                        students.clear()
                                        students.addAll(remote.roster.mapNotNull { entry ->
                                            entry.asStudent()?.copy(
                                                photo = StudentAccountStore.photo(activity, entry.id)
                                            )
                                        })
                                        currentId = null
                                        isAdmin = true
                                        isRemoteStudent = false
                                        page = Page.ADMIN_HOME
                                        recordAdmin("Masuk", "Admin masuk ke panel VOKASI")
                                        true
                                    } catch (_: Exception) {
                                        try { vokasiSupabase.auth.signOut() } catch (_: Exception) { }
                                        false
                                    }
                                }
                            } else {
                                // Production student accounts are provisioned and verified
                                // by Supabase. Do not fall back to stale local passwords.
                                val remoteStudent = SupabaseSchoolRepository.authenticateStudent(login, p)
                                if (remoteStudent == null) {
                                    false
                                } else {
                                    // Students only load the directory rows their own
                                    // Supabase session may read (RLS: their own roster).
                                    val ownDirectory = try {
                                        SupabaseSchoolRepository.loadDirectory()
                                    } catch (_: Exception) {
                                        SchoolDirectory()
                                    }
                                    schoolDirectory = ownDirectory
                                    students.clear()
                                    val ownRoster = ownDirectory.roster.firstOrNull {
                                        it.username.equals(remoteStudent.username, true)
                                    }
                                    students.add(remoteStudent.copy(
                                        photo = ownRoster?.let {
                                            StudentAccountStore.photo(activity, it.id)
                                        }
                                    ))
                                    currentId = remoteStudent.username
                                    isAdmin = false
                                    isRemoteStudent = true
                                    page = if (restoreQuiz(remoteStudent.username)) Page.QUIZ else Page.HOME
                                    true
                                }
                            }
                        }
                    )
                    Page.REGISTER -> RegisterScreen(
                        onBack = { page = Page.LOGIN },
                        availableRoster = schoolDirectory.roster,
                        onRegister = { n, u, p, c, admissionYear, ph ->
                            if (students.any { it.username.equals(u, true) }) false
                            else {
                                try {
                                    val next = SchoolDirectoryStore.claim(
                                        schoolDirectory, n, u, p, c, admissionYear
                                    )
                                    if (next == null) false else {
                                        SchoolDirectoryStore.save(activity, next)
                                        schoolDirectory = next
                                        val registered = next.roster.first { it.username == u }
                                        students.add(registered.asStudent()!!.copy(photo = ph))
                                        currentId = u
                                        isAdmin = false
                                        isRemoteStudent = false
                                        page = Page.HOME
                                        recordAdmin("Profil", "Pendaftaran akun siswa: " + n)
                                        true
                                    }
                                } catch (_: Exception) { false }
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
                        roster = schoolDirectory.roster,
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
                        onExcelExport = { major, classroom, date ->
                            recordAdmin("Laporan", "Ekspor Excel: $major • $classroom • $date")
                        },
                        adminName = adminPrefs.displayName,
                        schoolName = adminPrefs.school
                    )
                    Page.ADMIN_STUDENTS -> AdminStudentsScreen(
                        students = students.toList(), attempts = attempts.toList(),
                        initialMajor = adminStudentMajor
                    )
                    Page.ADMIN_QUESTIONS -> AdminQuestionsScreen(
                        questions = questions.toList(), bundledIds = bundledIds,
                        onAdd = { page = Page.ADMIN_ADD },
                        onExport = { count, categories ->
                            recordAdmin(
                                "Soal",
                                "Ekspor Excel: " + count + " soal • " + categories.joinToString(", ")
                            )
                        },
                        onImport = { drafts ->
                            try {
                                val imported = QuestionExcelFile.prepare(questions.toList(), drafts)
                                // Save once, then publish to the UI. Bundled questions stay read-only.
                                LocalAdminQuestionStore.save(
                                    activity,
                                    questions.filter { it.id !in bundledIds } + imported
                                )
                                questions.addAll(imported)
                                recordAdmin("Soal", "Impor Excel: " + imported.size + " soal")
                                "Berhasil mengimpor " + imported.size + " soal dari Excel."
                            } catch (e: IllegalArgumentException) {
                                e.message ?: "Data soal tidak sesuai format."
                            } catch (_: Exception) {
                                "Impor gagal disimpan. Tidak ada soal baru yang ditambahkan."
                            }
                        },
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
                    Page.ADMIN_SCHOOL -> AdminSchoolScreen(
                        directory = schoolDirectory,
                        onImport = { next ->
                            try {
                                val remote = SupabaseSchoolRepository.syncDirectory(next)
                                val merged = SupabaseSchoolRepository.mergeRemoteWithLocal(remote, next)
                                schoolDirectory = merged
                                students.clear()
                                students.addAll(merged.roster.mapNotNull { entry ->
                                    entry.asStudent()?.copy(
                                        photo = StudentAccountStore.photo(activity, entry.id)
                                    )
                                })
                                recordAdmin("Sekolah", "Sinkronisasi data sekolah ke Supabase: ${merged.roster.size} siswa")
                                null
                            } catch (e: IllegalArgumentException) {
                                e.message ?: "Data sekolah tidak valid."
                            } catch (e: Exception) {
                                e.message ?: "Sinkronisasi gagal. Pastikan Edge Function provision-students sudah di-deploy dan secret password awal sudah disetel."
                            }
                        },
                        onBack = { page = Page.ADMIN_HOME },
                        onRosterExport = { count ->
                            recordAdmin("Laporan", "Ekspor data sekolah: " + count + " siswa")
                        }
                    )
                    Page.ADMIN_NOTIFICATIONS -> AdminNotificationsScreen(
                        directory = schoolDirectory,
                        events = adminEvents.toList(),
                        onSchool = { page = Page.ADMIN_SCHOOL },
                        onBack = { page = adminHeaderReturn }
                    )
                    Page.ADMIN_LOG -> AdminLogScreen(
                        events = adminEvents.toList(),
                        onBack = { page = Page.ADMIN_PROFILE }
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
                        onBack = { page = adminHeaderReturn },
                        onLog = { page = Page.ADMIN_LOG },
                        onLogout = { logout() }
                    )
                    Page.ADMIN_MONITOR -> AdminMonitorScreen(
                        students.toList(), attempts.toList(),
                        initialMajor = adminMajorFilter,
                        initialClass = adminClassFilter,
                        initialDate = adminDateFilter,
                        onExcelExport = { major, classroom, date ->
                            recordAdmin("Laporan", "Ekspor Excel: $major • $classroom • $date")
                        }
                    )
                }
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
