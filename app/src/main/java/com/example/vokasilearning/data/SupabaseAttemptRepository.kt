package com.example.vokasilearning.data

import com.example.vokasilearning.model.AnswerReview
import com.example.vokasilearning.model.AnswerStatus
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Question
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * School results are shared through Supabase, not through the administrator's
 * Android device. Only a student's own Auth user can insert their own results;
 * admins are read-only. The SQL migration must be applied before use.
 */
internal object SupabaseAttemptRepository {
    private const val PAGE_SIZE = 500

    @Serializable
    private data class CompactReview(
        @SerialName("question_id") val questionId: Int,
        @SerialName("selected_index") val selectedIndex: Int? = null,
        val status: String,
        @SerialName("response_seconds") val responseSeconds: Int = 0
    )

    @Serializable
    private data class StoredAttempt(
        @SerialName("auth_user_id") val authUserId: String,
        val username: String,
        @SerialName("session_key") val sessionKey: String,
        @SerialName("completed_at_ms") val completedAtMs: Long,
        val signature: String,
        val correct: Int,
        val answered: Int,
        @SerialName("question_count") val questionCount: Int,
        val score: Int,
        @SerialName("earned_xp") val earnedXp: Int,
        @SerialName("seconds_spent") val secondsSpent: Int,
        @SerialName("practice_category") val practiceCategory: String? = null,
        @SerialName("name_snapshot") val nameSnapshot: String = "",
        @SerialName("class_snapshot") val classSnapshot: String = "",
        @SerialName("major_snapshot") val majorSnapshot: String = "",
        @SerialName("review_summary") val reviewSummary: List<CompactReview> = emptyList(),
        @SerialName("created_at") val createdAt: String = ""
    )

    @Serializable
    private data class UploadAttempt(
        @SerialName("auth_user_id") val authUserId: String,
        val username: String,
        @SerialName("session_key") val sessionKey: String,
        @SerialName("completed_at_ms") val completedAtMs: Long,
        val signature: String,
        val correct: Int,
        val answered: Int,
        @SerialName("question_count") val questionCount: Int,
        val score: Int,
        @SerialName("earned_xp") val earnedXp: Int,
        @SerialName("seconds_spent") val secondsSpent: Int,
        @SerialName("practice_category") val practiceCategory: String? = null,
        @SerialName("name_snapshot") val nameSnapshot: String = "",
        @SerialName("class_snapshot") val classSnapshot: String = "",
        @SerialName("major_snapshot") val majorSnapshot: String = "",
        @SerialName("review_summary") val reviewSummary: List<CompactReview> = emptyList()
    )

    private suspend fun fetchOwnRows(userId: String): List<StoredAttempt> {
        val rows = mutableListOf<StoredAttempt>()
        var offset = 0L
        do {
            val page = vokasiSupabase.from("student_attempts").select {
                filter { eq("auth_user_id", userId) }
                range(offset, offset + PAGE_SIZE - 1)
            }.decodeList<StoredAttempt>()
            rows.addAll(page)
            offset += page.size
        } while (page.size == PAGE_SIZE)
        return rows
    }

    /** Send only unsynced local sessions, never all accounts on this device. */
    suspend fun syncOwn(username: String, local: List<Attempt>): Int {
        val authUserId = vokasiSupabase.auth.currentUserOrNull()?.id?.toString()
            ?: throw IllegalStateException("Sesi siswa sudah berakhir.")
        val alreadyStored = fetchOwnRows(authUserId)
            .mapTo(HashSet()) { it.sessionKey }
        var added = 0
        local.filter {
            it.studentId.equals(username, ignoreCase = true) &&
                it.sessionKey.isNotBlank() &&
                it.questionCount in listOf(10, 100) &&
                it.answered in 0..it.questionCount &&
                it.correct in 0..it.answered
        }.distinctBy { it.sessionKey }.forEach { run ->
            if (run.sessionKey in alreadyStored) return@forEach
            // QuizSession emits UUID session keys. Skip invalid old demo rows.
            if (runCatching { UUID.fromString(run.sessionKey) }.isFailure) return@forEach
            vokasiSupabase.from("student_attempts").insert(
                UploadAttempt(
                    authUserId = authUserId,
                    username = username,
                    sessionKey = run.sessionKey,
                    completedAtMs = run.timeMillis,
                    signature = run.signature,
                    correct = run.correct,
                    answered = run.answered,
                    questionCount = run.questionCount,
                    score = run.score,
                    earnedXp = run.earnedXp,
                    secondsSpent = run.secondsSpent,
                    practiceCategory = run.practiceCategory,
                    nameSnapshot = run.studentNameSnapshot,
                    classSnapshot = run.classSnapshot,
                    majorSnapshot = run.majorSnapshot,
                    reviewSummary = run.review.map {
                        CompactReview(it.question.id, it.selectedIndex,
                            it.status.name, it.responseSeconds)
                    }
                )
            )
            alreadyStored.add(run.sessionKey)
            added++
        }
        return added
    }

    private fun decodeRows(
        rows: List<StoredAttempt>,
        questions: List<Question>,
        roster: List<SchoolRosterEntry> = emptyList()
    ): List<Attempt> {
        val namesByAuth = AdminAttemptIdentity.rosterNames(roster)
        val questionById = questions.associateBy { it.id }
        return rows.map { row ->
            Attempt(
                studentId = AdminAttemptIdentity.resolve(
                    row.authUserId, row.username, namesByAuth
                ),
                timeMillis = row.completedAtMs,
                signature = row.signature,
                correct = row.correct,
                answered = row.answered,
                questionCount = row.questionCount,
                score = row.score,
                earnedXp = row.earnedXp,
                secondsSpent = row.secondsSpent,
                sessionKey = row.sessionKey,
                practiceCategory = row.practiceCategory,
                studentNameSnapshot = row.nameSnapshot,
                classSnapshot = row.classSnapshot,
                majorSnapshot = row.majorSnapshot,
                review = row.reviewSummary.mapNotNull { saved ->
                    val question = questionById[saved.questionId] ?: return@mapNotNull null
                    val status = runCatching { AnswerStatus.valueOf(saved.status) }.getOrNull()
                        ?: return@mapNotNull null
                    AnswerReview(question, saved.selectedIndex, status, saved.responseSeconds)
                }
            )
        }
    }

    suspend fun loadForStudent(questions: List<Question>): List<Attempt> {
        val userId = vokasiSupabase.auth.currentUserOrNull()?.id?.toString()
            ?: throw IllegalStateException("Sesi siswa berakhir.")
        return decodeRows(fetchOwnRows(userId), questions)
    }

    /**
     * Fresh, date-scoped school export. Never use stale dashboard state as the
     * authoritative source of an Excel report. Dates use the school's WIB
     * timezone; the end is exclusive to avoid mixing consecutive school days.
     */
    suspend fun loadForAdminDate(
        date: String,
        questions: List<Question> = emptyList(),
        roster: List<SchoolRosterEntry> = emptyList()
    ): List<Attempt> {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) {
            "Tanggal harus menggunakan format YYYY-MM-DD."
        }
        val parts = date.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale.US).apply {
            isLenient = false
            clear()
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }
        val startMs = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endMs = calendar.timeInMillis

        val permitted = vokasiSupabase.postgrest.rpc("is_school_admin").decodeAs<Boolean>()
        check(permitted) { "Akun ini bukan admin sekolah aktif." }

        val rows = mutableListOf<StoredAttempt>()
        var offset = 0L
        do {
            val page = vokasiSupabase.from("student_attempts").select {
                filter {
                    gte("completed_at_ms", startMs)
                    lt("completed_at_ms", endMs)
                }
                order("completed_at_ms", Order.ASCENDING)
                order("id", Order.ASCENDING)
                range(offset, offset + PAGE_SIZE - 1)
            }.decodeList<StoredAttempt>()
            rows.addAll(page)
            offset += page.size
        } while (page.size == PAGE_SIZE)
        return decodeRows(rows.filter { it.practiceCategory == null }, questions, roster)
    }

    /** The cursor lets the open dashboard request only newer submissions. */
    data class AdminBatch(val attempts: List<Attempt>, val lastCreatedAt: String?)

    /** Use >= for the cursor boundary so sessions with the same timestamp are not lost. */
    suspend fun loadForAdmin(
        questions: List<Question>,
        sinceCreatedAt: String? = null,
        roster: List<SchoolRosterEntry> = emptyList()
    ): AdminBatch {
        val permitted = vokasiSupabase.postgrest.rpc("is_school_admin").decodeAs<Boolean>()
        check(permitted) { "Akun ini bukan admin sekolah aktif." }
        val rows = mutableListOf<StoredAttempt>()
        var offset = 0L
        do {
            val page = vokasiSupabase.from("student_attempts").select {
                if (!sinceCreatedAt.isNullOrBlank()) {
                    filter { gte("created_at", sinceCreatedAt) }
                }
                order("created_at", Order.ASCENDING)
                order("id", Order.ASCENDING)
                range(offset, offset + PAGE_SIZE - 1)
            }.decodeList<StoredAttempt>()
            rows.addAll(page)
            offset += page.size
        } while (page.size == PAGE_SIZE)
        val decoded = decodeRows(rows, questions, roster)
        return AdminBatch(decoded, rows.lastOrNull()?.createdAt ?: sinceCreatedAt)
    }
}
