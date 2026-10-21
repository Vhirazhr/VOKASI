package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.logic.dayKey
import com.example.vokasilearning.logic.calculateStreak
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student

internal data class VokasiNotice(val title: String, val description: String, val key: String)
internal object StudentNoticeStore {
    private const val PREFS = "vokasi_notice_settings"
    fun streakReminder(context: Context, username: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("streak_" + username.lowercase(), true)

    fun setStreakReminder(context: Context, username: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("streak_" + username.lowercase(), enabled).apply()
    }

    fun migrate(context: Context, old: String, updated: String) {
        if (old == updated) return
        val pref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = streakReminder(context, old)
        pref.edit().putBoolean("streak_" + updated.lowercase(), state)
            .remove("streak_" + old.lowercase()).apply()
    }

    /** Only actionable/current information; not a copied activity history. */
    fun forStudent(me: Student, attempts: List<Attempt>, remindersEnabled: Boolean): List<VokasiNotice> {
        val mine = attempts.filter { it.studentId == me.username }
        val today = dayKey(System.currentTimeMillis())
        val todayRequired = mine.any { it.practiceCategory == null &&
            it.answered > 0 && dayKey(it.timeMillis) == today }
        val notices = mutableListOf<VokasiNotice>()
        if (!todayRequired) notices.add(VokasiNotice(
            "Latihan wajib hari ini",
            "Latihan 100 soal belum tercatat hari ini. Kerjakan saat sudah siap.",
            "daily_" + today
        ))
        if (remindersEnabled && mine.isNotEmpty() &&
            mine.none { it.answered > 0 && dayKey(it.timeMillis) == today }) {
            notices.add(VokasiNotice(
                "Jaga streak belajarmu",
                "Sempatkan latihan hari ini agar kebiasaan belajarmu tetap konsisten.",
                "streak_" + today
            ))
        }
        if (mine.isEmpty()) notices.add(VokasiNotice(
            "Mulai perjalanan belajarmu",
            "Latihan pertama akan membuka progres nilai dan XP. Semangat!",
            "welcome"
        ))
        val latest = mine.maxByOrNull { it.timeMillis }
        if (latest != null && latest.classSnapshot.isNotBlank() &&
            latest.classSnapshot != me.classroom.label) {
            notices.add(VokasiNotice(
                "Kelas aktif diperbarui",
                "Kelas sekarang: " + me.classroom.label +
                    ". Riwayat nilai lama tetap menggunakan kelas saat latihan.",
                "class_change_" + me.classroom.label
            ))
        }
        return notices.take(4)
    }

    fun forAdmin(directory: SchoolDirectory): List<VokasiNotice> {
        val notifications = mutableListOf<VokasiNotice>()
        if (directory.pendingCount > 0) notifications.add(VokasiNotice(
            "Siswa belum mengaktifkan akun",
            directory.pendingCount.toString() + " siswa masih berstatus Belum Terdaftar.",
            "admin_pending"
        ))
        if (directory.majors.isEmpty() || directory.classes.isEmpty()) {
            notifications.add(VokasiNotice(
                "Lengkapi data sekolah",
                "Tambahkan jurusan dan kelas resmi sebelum membuat daftar siswa.",
                "admin_setup"
            ))
        }
        if (directory.roster.isNotEmpty() && directory.pendingCount == 0) {
            notifications.add(VokasiNotice(
                "Pendaftaran siswa lengkap",
                "Seluruh siswa dalam daftar lokal telah memiliki akun.",
                "admin_complete"
            ))
        }
        return notifications
    }
}
