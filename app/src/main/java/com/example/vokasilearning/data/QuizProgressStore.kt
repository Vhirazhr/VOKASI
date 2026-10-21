package com.example.vokasilearning.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * One pending local session per student. Private app storage, not a backend.
 * Separate filenames avoid a shared-device student replacing another student's work.
 */
internal object QuizProgressStore {
    private fun file(context: Context, studentId: String): File {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(studentId.lowercase().toByteArray(Charsets.UTF_8))
        val suffix = bytes.take(12).joinToString("") { "%02x".format(it) }
        return File(context.filesDir, "vokasi_pending_$suffix.json")
    }

    fun save(context: Context, studentId: String, snapshot: JSONObject) {
        val target = file(context, studentId)
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(snapshot.toString(), Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    fun load(context: Context, studentId: String): JSONObject? = try {
        val target = file(context, studentId)
        if (!target.isFile) null else JSONObject(target.readText(Charsets.UTF_8)).takeIf {
            it.optString("owner") == studentId
        }
    } catch (_: Exception) {
        null
    }

    fun clear(context: Context, studentId: String) {
        file(context, studentId).delete()
    }

    /** Preserve an unfinished session when the student changes their username. */
    fun moveOwner(context: Context, oldUsername: String, newUsername: String) {
        if (oldUsername == newUsername) return
        val pending = load(context, oldUsername) ?: return
        pending.put("owner", newUsername)
        save(context, newUsername, pending)
        clear(context, oldUsername)
    }
}
