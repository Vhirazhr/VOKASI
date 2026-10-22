package com.example.vokasilearning.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.vokasilearning.MainActivity
import com.example.vokasilearning.logic.dayKey
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * A local, opt-in reminder around 18:00 WIB. WorkManager may defer delivery;
 * this is not an exact alarm or remote push service.
 */
internal object StreakReminderScheduler {
    private const val WORK_PREFIX = "vokasi-streak-"
    internal const val USERNAME = "username"

    fun schedule(context: Context, username: String) {
        if (username.isBlank() || !StudentNoticeStore.pushReminder(context, username)) return
        val now = System.currentTimeMillis()
        val target = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(target - now, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(USERNAME to username))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_PREFIX + username.lowercase(java.util.Locale.ROOT),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, username: String) {
        if (username.isBlank()) return
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(
            WORK_PREFIX + username.lowercase(java.util.Locale.ROOT)
        )
    }
}

internal class StreakReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val username = inputData.getString(StreakReminderScheduler.USERNAME)
            ?.takeIf { it.isNotBlank() } ?: return Result.success()
        val context = applicationContext
        if (!StudentNoticeStore.pushReminder(context, username) ||
            !StudentNoticeStore.streakReminder(context, username)) return Result.success()

        // No Android 13+ notification without explicit runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return Result.success()

        val now = System.currentTimeMillis()
        val today = dayKey(now)
        if (StudentNoticeStore.isRead(context, username, "streak_$today")) return Result.success()
        val yesterday = dayKey(Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis)
        val mine = LocalAttemptStore.load(context).filter {
            it.studentId == username && it.answered > 0
        }
        if (!mine.any { dayKey(it.timeMillis) == yesterday } ||
            mine.any { dayKey(it.timeMillis) == today }) return Result.success()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vokasi_streak_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                channelId, "Pengingat Belajar", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Pengingat streak belajar yang diaktifkan siswa" })
        }
        val activityIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_LOW)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Lanjutkan streak hari ini")
            .setContentText("Kamu sudah belajar kemarin. Yuk latihan lagi!")
            .setContentIntent(activityIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(username.hashCode() and Int.MAX_VALUE, notification)
        return Result.success()
    }
}
