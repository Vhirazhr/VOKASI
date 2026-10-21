package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.result.decodeList
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.util.UUID

/**
 * Shared school data repository.
 *
 * The Android client only uses the publishable key. Account creation is delegated
 * to the server-side provision-students Edge Function, which checks the active admin.
 */
internal object SupabaseSchoolRepository {
    private const val DEFAULT_SCHOOL_YEAR = "2026/2027"
    private const val MAX_BATCH = 25
    private const val STUDENT_LOGIN_DOMAIN = "@students.vokasi.invalid"

    private val majorsByCode = linkedMapOf(
        "DPIB" to "Desain Pemodelan dan Informasi Bangunan",
        "GEO" to "Teknik Geomatika",
        "TAB" to "Teknik Alat Berat",
        "TITL" to "Teknik Instalasi Tenaga Listrik",
        "TKP" to "Teknik Konstruksi dan Perumahan",
        "TKR" to "Teknik Kendaraan Ringan",
        "TMI" to "Teknik Mekanik Industri",
        "TOI" to "Teknik Otomasi Industri",
        "TP" to "Teknik Pengelasan",
        "TPM" to "Teknik Pemesinan",
        "TSM" to "Teknik Sepeda Motor"
    )

    @Serializable
    private data class RemoteMajor(
        val id: String,
        val code: String? = null,
        val name: String,
        @SerialName("is_active") val isActive: Boolean = true
    )

    @Serializable
    private data class RemoteClass(
        val id: String,
        @SerialName("major_id") val majorId: String,
        @SerialName("grade_level") val gradeLevel: Int,
        @SerialName("class_name") val className: String,
        @SerialName("school_year") val schoolYear: String = DEFAULT_SCHOOL_YEAR,
        @SerialName("is_active") val isActive: Boolean = true
    )

    @Serializable
    private data class RemoteRoster(
        val id: String,
        @SerialName("full_name") val fullName: String,
        @SerialName("class_id") val classId: String,
        @SerialName("major_id") val majorId: String? = null,
        @SerialName("admission_year") val admissionYear: Int,
        @SerialName("registration_status") val registrationStatus: String,
        val username: String? = null,
        @SerialName("auth_user_id") val authUserId: String? = null,
        @SerialName("current_school_year") val currentSchoolYear: String = DEFAULT_SCHOOL_YEAR,
        @SerialName("data_status") val dataStatus: String = "valid"
    )

    /**
     * Load the directory from Supabase. RLS limits this operation to authenticated
     * school admins or the signed-in student's own roster row.
     */
    suspend fun loadDirectory(): SchoolDirectory {
        val remoteMajors = vokasiSupabase.from("majors")
            .select().decodeList<RemoteMajor>()
        val remoteClasses = vokasiSupabase.from("classes")
            .select().decodeList<RemoteClass>()
        val majorsById = remoteMajors.associateBy { it.id }
        val classesById = remoteClasses.associateBy { it.id }

        // Query roster per class so a large school is not silently truncated by
        // PostgREST's default maximum response size.
        val remoteRoster = mutableListOf<RemoteRoster>()
        remoteClasses.forEach { classroom ->
            remoteRoster += vokasiSupabase.from("student_roster").select {
                filter { eq("class_id", classroom.id) }
            }.decodeList<RemoteRoster>()
        }

        val classrooms = remoteClasses.mapNotNull { remoteClass ->
            val major = majorsById[remoteClass.majorId] ?: return@mapNotNull null
            Classroom(remoteClass.className, major.name)
        }.distinctBy { normalized(it.major) + "|" + normalized(it.label) }

        val entries = remoteRoster.mapNotNull { row ->
            val remoteClass = classesById[row.classId] ?: return@mapNotNull null
            val major = row.majorId?.let { majorsById[it] }
                ?: majorsById[remoteClass.majorId]
            val majorName = major?.name ?: major?.code ?: "Jurusan belum dipetakan"
            val registered = row.registrationStatus == "registered" && row.authUserId != null
            val stableId = try {
                UUID.fromString(row.id).mostSignificantBits and Long.MAX_VALUE
            } catch (_: Exception) {
                row.id.hashCode().toLong().and(Long.MAX_VALUE)
            }
            SchoolRosterEntry(
                id = stableId,
                fullName = row.fullName,
                classLabel = remoteClass.className,
                major = majorName,
                admissionYear = row.admissionYear,
                username = row.username.orEmpty(),
                passwordHash = "",
                remoteId = row.id,
                remoteRegistered = registered
            )
        }

        return SchoolDirectory(
            majors = remoteMajors.map { it.name }.distinct(),
            classes = classrooms,
            roster = entries.sortedWith(
                compareBy<SchoolRosterEntry> { it.major }
                    .thenBy { it.classLabel }
                    .thenBy { it.fullName }
            )
        )
    }

    /**
     * Keep local-only rows while remote setup is empty or has not yet received
     * those rows. Remote entries win for matching school identity.
     */
    fun mergeRemoteWithLocal(remote: SchoolDirectory, local: SchoolDirectory): SchoolDirectory {
        val remoteKeys = remote.roster.map { identity(it) }.toSet()
        val localOnly = local.roster.filter { identity(it) !in remoteKeys }
        val roster = if (remote.roster.isEmpty()) local.roster
            else remote.roster + localOnly
        return SchoolDirectory(
            majors = (remote.majors + local.majors).distinctBy { normalized(it) },
            classes = (remote.classes + local.classes).distinctBy {
                (resolveMajor(it.major)?.second?.let(::normalized) ?: normalized(it.major)) +
                    "|" + normalized(it.label)
            },
            roster = roster.sortedWith(
                compareBy<SchoolRosterEntry> { it.major }
                    .thenBy { it.classLabel }
                    .thenBy { it.fullName }
            )
        )
    }

    /**
     * Provision every roster row in idempotent batches. The Edge Function owns
     * username generation, password setup, Auth user creation, and role checks.
     */
    suspend fun syncDirectory(directory: SchoolDirectory): SchoolDirectory {
        require(directory.roster.isNotEmpty() || directory.classes.isNotEmpty() ||
            directory.majors.isNotEmpty()) {
            "Belum ada data sekolah untuk disinkronkan."
        }

        val payloadMajors = directory.majors.map { name ->
            val major = resolveMajor(name)
                ?: throw IllegalArgumentException(
                    "Jurusan ‘$name’ belum dikenali. Gunakan salah satu dari 11 kode jurusan resmi."
                )
            buildJsonObject {
                put("code", major.first)
                put("name", major.second)
            }
        }.distinctBy { it["code"]?.toString() }

        val payloadClasses = directory.classes.map { classroom ->
            val major = resolveMajor(classroom.major)
                ?: throw IllegalArgumentException(
                    "Jurusan ‘${classroom.major}’ belum dikenali. Gunakan salah satu dari 11 kode jurusan resmi."
                )
            val grade = gradeLevel(classroom.label)
                ?: throw IllegalArgumentException(
                    "Tingkat kelas ‘${classroom.label}’ tidak terbaca. Gunakan format X, XI, atau XII."
                )
            buildJsonObject {
                put("majorCode", major.first)
                put("majorName", major.second)
                put("gradeLevel", grade)
                put("classLabel", classroom.label.trim().replace(Regex("\\s+"), " "))
                put("schoolYear", DEFAULT_SCHOOL_YEAR)
            }
        }.distinctBy { "${it["majorCode"]}|${it["classLabel"]}" }

        val payloadRows = directory.roster.map { entry ->
            val major = resolveMajor(entry.major)
                ?: throw IllegalArgumentException(
                    "Jurusan ‘${entry.major}’ belum dikenali. Gunakan salah satu dari 11 kode jurusan resmi."
                )
            val grade = gradeLevel(entry.classLabel)
                ?: throw IllegalArgumentException(
                    "Tingkat kelas ‘${entry.classLabel}’ tidak terbaca. Gunakan format X, XI, atau XII."
                )
            buildJsonObject {
                put("fullName", entry.fullName)
                put("majorCode", major.first)
                put("majorName", major.second)
                put("gradeLevel", grade)
                put("classLabel", entry.classLabel.trim().replace(Regex("\\s+"), " "))
                put("admissionYear", entry.admissionYear)
                put("schoolYear", DEFAULT_SCHOOL_YEAR)
            }
        }

        val batches = payloadRows.chunked(MAX_BATCH).ifEmpty { listOf(emptyList()) }
        batches.forEachIndexed { batchIndex, batch ->
            val response = vokasiSupabase.functions.invoke(
                function = "provision-students",
                body = buildJsonObject {
                    put("students", buildJsonArray { batch.forEach { add(it) } })
                    if (batchIndex == 0) {
                        put("majors", buildJsonArray { payloadMajors.forEach { add(it) } })
                        put("classes", buildJsonArray { payloadClasses.forEach { add(it) } })
                    }
                }
            )
            val result = JSONObject(response.bodyAsText())
            val failed = result.optInt("failed", 0)
            if (failed > 0 || !result.optBoolean("success", false)) {
                val failures = result.optJSONArray("failures")
                val first = failures?.optJSONObject(0)
                val row = first?.optInt("row", -1) ?: -1
                val reason = first?.optString("error").orEmpty()
                val prefix = "Batch ${batchIndex + 1}: ${failed} data gagal diproses."
                throw IllegalStateException(
                    if (reason.isNotBlank()) "$prefix Baris batch $row: $reason. " +
                        "Data yang berhasil diproses tetap tersimpan; impor ulang aman dilakukan."
                    else "$prefix Data yang berhasil diproses tetap tersimpan; impor ulang aman dilakukan."
                )
            }
        }

        return loadDirectory()
    }

    /** Authenticate a student with the username alias created by the Edge Function. */
    suspend fun authenticateStudent(username: String, password: String): Student? {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || password.isBlank()) return null
        return try {
            // A login attempt must never inherit another person's previous session.
            vokasiSupabase.auth.signOut()
            vokasiSupabase.auth.signInWith(Email) {
                email = cleanUsername + STUDENT_LOGIN_DOMAIN
                this.password = password
            }
            val userId = vokasiSupabase.auth.currentUserOrNull()?.id ?: return null
            val roster = vokasiSupabase.from("student_roster")
                .select().decodeList<RemoteRoster>()
                .singleOrNull {
                    it.authUserId == userId && it.registrationStatus == "registered"
                } ?: run {
                    vokasiSupabase.auth.signOut()
                    return null
                }
            val directory = loadDirectory()
            val entry = directory.roster.singleOrNull { it.remoteId == roster.id }
                ?: run {
                    vokasiSupabase.auth.signOut()
                    return null
                }
            entry.asStudent()
        } catch (_: Exception) {
            try { vokasiSupabase.auth.signOut() } catch (_: Exception) { }
            null
        }
    }

    /** Verify the current password and change it in Supabase Auth. */
    suspend fun updateStudentPassword(username: String, currentPassword: String, newPassword: String) {
        require(newPassword.length >= 8) { "Password baru minimal 8 karakter." }
        val cleanUsername = username.trim()
        try {
            vokasiSupabase.auth.signOut()
            vokasiSupabase.auth.signInWith(Email) {
                email = cleanUsername + STUDENT_LOGIN_DOMAIN
                password = currentPassword
            }
            vokasiSupabase.auth.updateUser {
                password = newPassword
            }
        } catch (_: Exception) {
            try { vokasiSupabase.auth.signOut() } catch (_: Exception) { }
            throw IllegalArgumentException("Password saat ini salah atau koneksi ke server gagal.")
        }
    }

    private fun resolveMajor(value: String): Pair<String, String>? {
        val clean = value.trim()
        val code = majorsByCode.keys.firstOrNull { it.equals(clean, true) }
            ?: majorsByCode.entries.firstOrNull { it.value.equals(clean, true) }?.key
            ?: return null
        return code to majorsByCode.getValue(code)
    }

    private fun gradeLevel(classLabel: String): Int? = when {
        Regex("^XII\\b", RegexOption.IGNORE_CASE).containsMatchIn(classLabel.trim()) -> 12
        Regex("^XI\\b", RegexOption.IGNORE_CASE).containsMatchIn(classLabel.trim()) -> 11
        Regex("^X\\b", RegexOption.IGNORE_CASE).containsMatchIn(classLabel.trim()) -> 10
        else -> null
    }

    private fun identity(entry: SchoolRosterEntry): String =
        listOf(
            normalized(entry.fullName),
            resolveMajor(entry.major)?.second?.let(::normalized) ?: normalized(entry.major),
            entry.admissionYear.toString()
        ).joinToString("|")

    private fun normalized(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()
}
