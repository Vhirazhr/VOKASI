package com.example.vokasilearning.data

import android.content.Context
import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

internal data class SchoolRosterEntry(
    val id: Long,
    val fullName: String,
    val classLabel: String,
    val major: String,
    val admissionYear: Int,
    val username: String = "",
    val passwordHash: String = ""
) {
    val registered: Boolean get() = username.isNotBlank() && passwordHash.isNotBlank()
    fun asStudent(): Student? = if (!registered) null else
        Student(username, passwordHash, fullName, Classroom(classLabel, major),
            admissionYear = admissionYear)
}

internal data class SchoolDirectory(
    val majors: List<String> = emptyList(),
    val classes: List<Classroom> = emptyList(),
    val roster: List<SchoolRosterEntry> = emptyList()
) {
    val registeredCount get() = roster.count { it.registered }
    val pendingCount get() = roster.count { !it.registered }
}

internal object SchoolCredentials {
    private const val ITERATIONS = 120_000
    fun hash(password: String): String {
        require(password.length >= 6)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val secret = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
        return "pbkdf2$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" +
            Base64.encodeToString(secret, Base64.NO_WRAP)
    }

    fun verify(password: String, stored: String): Boolean {
        if (!stored.startsWith("pbkdf2$")) return password == stored // legacy demo-only
        return try {
            val parts = stored.split('$')
            if (parts.size != 3) return false
            val salt = Base64.decode(parts[1], Base64.DEFAULT)
            val expected = Base64.decode(parts[2], Base64.DEFAULT)
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, expected.size * 8)
            val actual = try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            } finally { spec.clearPassword() }
            java.security.MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) { false }
    }
}

internal object SchoolDirectoryStore {
    private const val FILE = "vokasi_school_directory.json"

    fun load(context: Context): SchoolDirectory {
        return try {
        val file = context.getFileStreamPath(FILE)
        if (!file.exists()) SchoolDirectory() else {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        val majorsJson = json.optJSONArray("majors") ?: JSONArray()
        val classJson = json.optJSONArray("classes") ?: JSONArray()
        val rosterJson = json.optJSONArray("roster") ?: JSONArray()
        SchoolDirectory(
            majors = (0 until majorsJson.length()).map { majorsJson.getString(it) },
            classes = (0 until classJson.length()).map { i ->
                val c = classJson.getJSONObject(i)
                Classroom(c.getString("label"), c.getString("major"))
            },
            roster = (0 until rosterJson.length()).map { i ->
                val e = rosterJson.getJSONObject(i)
                SchoolRosterEntry(
                    id = e.getLong("id"), fullName = e.getString("fullName"),
                    classLabel = e.getString("classLabel"), major = e.getString("major"),
                    admissionYear = e.getInt("admissionYear"),
                    username = e.optString("username", ""),
                    passwordHash = e.optString("passwordHash", "")
                )
            }
        )
        }
        } catch (_: Exception) { SchoolDirectory() }
    }

    fun save(context: Context, directory: SchoolDirectory) {
        val json = JSONObject()
        json.put("majors", JSONArray(directory.majors))
        json.put("classes", JSONArray().apply {
            directory.classes.forEach { c ->
                put(JSONObject().put("label", c.label).put("major", c.major))
            }
        })
        json.put("roster", JSONArray().apply {
            directory.roster.forEach { e ->
                put(JSONObject().put("id", e.id).put("fullName", e.fullName)
                    .put("classLabel", e.classLabel).put("major", e.major)
                    .put("admissionYear", e.admissionYear).put("username", e.username)
                    .put("passwordHash", e.passwordHash))
            }
        })
        val target = context.getFileStreamPath(FILE)
        val temp = java.io.File(target.absolutePath + ".tmp")
        temp.writeText(json.toString(), Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    /** Exact full-name + school-specified class and cohort; one roster row per account. */
    fun claim(
        directory: SchoolDirectory, name: String, username: String,
        password: String, classroom: Classroom, year: Int
    ): SchoolDirectory? {
        if (username.isBlank() || password.length < 6) return null
        if (directory.roster.any { it.username.equals(username, true) && it.registered }) return null
        val normalize: (String) -> String = { it.trim().replace(Regex("\\s+"), " ").lowercase() }
        val candidates = directory.roster.filter {
            !it.registered && normalize(it.fullName) == normalize(name) &&
                it.classLabel == classroom.label && it.major == classroom.major &&
                it.admissionYear == year
        }
        if (candidates.size != 1) return null
        val id = candidates.single().id
        val hash = SchoolCredentials.hash(password)
        return directory.copy(roster = directory.roster.map { e ->
            if (e.id == id) e.copy(username = username, passwordHash = hash) else e
        })
    }
}
