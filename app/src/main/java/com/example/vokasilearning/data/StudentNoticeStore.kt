package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.logic.dayKey
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal data class VokasiNotice(val title: String, val description: String, val key: String)

/**
 * In-app reminders only; no push/scheduled delivery is implied.
 * Streak reminders are actionable only if there was practice yesterday.
 * Read state is scoped to a single account on this Android device.
 */
internal object StudentNoticeStore {
    private const val PREFS = "vokasi_notice_settings"

    private fun accountKey(username: String) = username.trim().lowercase(Locale.ROOT)
    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun streakReminder(context: Context, username: String): Boolean =
        preferences(context).getBoolean("streak_" + accountKey(username), true)

    fun setStreakReminder(context: Context, username: String, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean("streak_" + accountKey(username), enabled).apply()
    }

    fun isRead(context: Context, username: String, noticeKey: String): Boolean =
        preferences(context).getStringSet("read_" + accountKey(username), emptySet())
            ?.contains(noticeKey) == true

    fun markRead(context: Context, username: String, noticeKey: String) {
        val key = "read_" + accountKey(username)
        val stored = preferences(context).getStringSet(key, emptySet()).orEmpty().toMutableSet()
        val today = dayKey(System.currentTimeMillis())
        stored.removeAll {
            (it.startsWith("daily_") && it != "daily_$today") ||
                (it.startsWith("streak_") && it != "streak_$today")
        }
        stored.add(noticeKey)
        preferences(context).edit().putStringSet(key, stored).apply()
    }

    fun markAllRead(context: Context, username: String, notices: List<VokasiNotice>) {
        notices.forEach { markRead(context, username, it.key) }
    }

    fun migrate(context: Context, old: String, updated: String) {
        if (old.equals(updated, true)) return
        val prefs = preferences(context)
        val oldKey = accountKey(old)
        val newKey = accountKey(updated)
        val read = prefs.getStringSet("read_" + oldKey, emptySet()).orEmpty()
        val existing = prefs.getStringSet("read_" + newKey, emptySet()).orEmpty()
        prefs.edit()
            .putBoolean("streak_" + newKey, streakReminder(context, old))
            .putStringSet("read_" + newKey, (read + existing).toSet())
            .remove("streak_" + oldKey)
            .remove("read_" + oldKey).apply()
    }

    /** Pure notification policy, with a clock argument for unit testing. */
    fun forStudent(
        me: Student,
        attempts: List<Attempt>,
        remindersEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): List<VokasiNotice> {
        val mine = attempts.filter { it.studentId == me.username && it.answered > 0 }
        val today = dayKey(nowMillis)
        val yesterday = dayKey(Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis)
        val practicedToday = mine.any { dayKey(it.timeMillis) == today }
        val practicedYesterday = mine.any { dayKey(it.timeMillis) == yesterday }
        val completedRequiredToday = mine.any {
            it.practiceCategory == null && dayKey(it.timeMillis) == today
        }
        val notices = mutableListOf<VokasiNotice>()
        if (mine.isEmpty()) {
            notices.add(VokasiNotice(
                "Mulai latihan pertamamu",
                "Kerjakan latihan untuk mencatat progres belajar.",
                "welcome"
            ))
        } else if (!completedRequiredToday) {
            notices.add(VokasiNotice(
                "Latihan wajib belum tercatat",
                "Latihan wajib hari ini belum selesai di perangkat ini.",
                "daily_$today"
            ))
        }
        if (remindersEnabled && practicedYesterday && !practicedToday) {
            notices.add(VokasiNotice(
                "Lanjutkan streak hari ini",
                "Kamu sudah belajar kemarin. Ayo lanjutkan hari ini!",
                "streak_$today"
            ))
        }
        val latest = mine.maxByOrNull { it.timeMillis }
        if (latest != null && latest.classSnapshot.isNotBlank() &&
            latest.classSnapshot != me.classroom.label) {
            notices.add(VokasiNotice(
                "Kelasmu diperbarui",
                "Kelas sekarang: " + me.classroom.label,
                "class_change_" + me.classroom.label
            ))
        }
        return notices.take(3)
    }

    fun forAdmin(directory: SchoolDirectory): List<VokasiNotice> {
        val notifications = mutableListOf<VokasiNotice>()
        if (directory.pendingCount > 0) notifications.add(VokasiNotice(
            "Akun siswa perlu ditinjau",
            directory.pendingCount.toString() + " siswa belum memiliki akun aktif.",
            "admin_pending"
        ))
        if (directory.majors.isEmpty() || directory.classes.isEmpty()) {
            notifications.add(VokasiNotice(
                "Data sekolah belum lengkap",
                "Periksa jurusan dan kelas sebelum menambahkan siswa.",
                "admin_setup"
            ))
        }
        return notifications
    }
}
