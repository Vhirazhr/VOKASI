package com.example.vokasilearning.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.vokasilearning.model.Student
import java.io.File
import java.security.MessageDigest

/**
 * Account changes on a single-device prototype. Roster ID is stable;
 * username is a mutable login identifier, not the student's school identity.
 */
internal object StudentAccountStore {
    private fun photoFile(context: Context, rosterId: Long): File =
        File(context.filesDir, "vokasi_avatar_" + rosterId + ".jpg")

    fun photo(context: Context, rosterId: Long): ImageBitmap? = try {
        val f = photoFile(context, rosterId)
        if (!f.exists()) null else BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
    } catch (_: Exception) { null }

    fun savePhoto(context: Context, rosterId: Long, image: Bitmap): ImageBitmap {
        val width = image.width.coerceAtLeast(1)
        val height = image.height.coerceAtLeast(1)
        val ratio = (720f / maxOf(width, height)).coerceAtMost(1f)
        val scaled = Bitmap.createScaledBitmap(image,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1), true)
        val target = photoFile(context, rosterId)
        val temp = File(target.absolutePath + ".tmp")
        temp.outputStream().use { out ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                "Gagal mengompresi foto."
            }
        }
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        return scaled.asImageBitmap()
    }

    /**
     * Verifies current password and returns a fully validated roster.
     * Caller is responsible for migrating attempt/student-ID references.
     */
    fun updatedRoster(
        directory: SchoolDirectory,
        existing: Student,
        currentPassword: String,
        username: String,
        newPassword: String
    ): SchoolDirectory {
        require(SchoolCredentials.verify(currentPassword, existing.password)) {
            "Password saat ini salah."
        }
        val proposed = username.trim()
        require(proposed.matches(Regex("[A-Za-z0-9._]{3,30}"))) {
            "Username harus 3–30 karakter (huruf, angka, titik, garis bawah)."
        }
        require(!proposed.equals("admin.demo", true)) { "Username tidak tersedia." }
        require(directory.roster.none { it.registered &&
            !it.username.equals(existing.username, true) &&
            it.username.equals(proposed, true) }) {
            "Username sudah digunakan siswa lain."
        }
        require(newPassword.isBlank() || newPassword.length >= 8) {
            "Password baru minimal 8 karakter."
        }
        val matched = directory.roster.filter {
            it.registered && it.username.equals(existing.username, true)
        }
        require(matched.size == 1) { "Identitas siswa tidak ditemukan dalam data admin." }
        val newHash = if (newPassword.isBlank()) matched.single().passwordHash
            else SchoolCredentials.hash(newPassword)
        return directory.copy(roster = directory.roster.map {
            if (it.id == matched.single().id) it.copy(username = proposed, passwordHash = newHash)
            else it
        })
    }
}
