package com.example.vokasilearning.data

import com.example.vokasilearning.model.Classroom
import com.example.vokasilearning.model.Student
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
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

        // PostgREST limits each response to 1,000 rows. Always page the
        // authoritative Supabase roster so no student disappears when a
        // major grows beyond the server's row limit.
        val remoteRoster = mutableListOf<RemoteRoster>()
        val batchSize = 500
        remoteMajors.forEach { major ->
            var from = 0L
            do {
                val page = vokasiSupabase.from("student_roster").select {
                    filter { eq("major_id", major.id) }
                    range(from, from + batchSize - 1L)
                }.decodeList<RemoteRoster>()
                remoteRoster.addAll(page)
                from += page.size
            } while (page.size == batchSize)
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
     * Supabase is authoritative after a successful server refresh. Returning
     * locally cached/imported rows here would resurrect deleted students and
     * cause duplicates in the admin directory.
     */
    fun mergeRemoteWithLocal(
        remote: SchoolDirectory,
        @Suppress("UNUSED_PARAMETER") local: SchoolDirectory
    ): SchoolDirectory = remote

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

        // Send the whole roster in one authenticated request. The Edge Function
        // handles account creation concurrently, so admins don't wait for ~100 small requests.
        val response = vokasiSupabase.functions.invoke(
            function = "provision-students",
            body = buildJsonObject {
                put("students", buildJsonArray { payloadRows.forEach { add(it) } })
                put("majors", buildJsonArray { payloadMajors.forEach { add(it) } })
                put("classes", buildJsonArray { payloadClasses.forEach { add(it) } })
            }
        )
        val result = JSONObject(response.bodyAsText())
        val failed = result.optInt("failed", 0)
        val created = result.optInt("created", 0)
        val existing = result.optInt("existing", 0)
        if (failed > 0 || !result.optBoolean("success", false)) {
            val failures = result.optJSONArray("failures")
            val first = failures?.optJSONObject(0)
            val row = first?.optInt("row", -1) ?: -1
            val reason = first?.optString("error").orEmpty()
            val summary = "Proses server selesai: $created akun baru, $existing akun sudah ada, $failed gagal."
            throw IllegalStateException(
                if (reason.isNotBlank()) "$summary Contoh kendala pada baris $row: $reason. " +
                    "Data yang sudah tersimpan tidak dihapus; impor ulang aman dilakukan setelah kendala diperbaiki."
                else "$summary Data yang sudah tersimpan tidak dihapus; periksa riwayat impor di server."
            )
        }

        return loadDirectory()
    }

    /**
     * Sign in using a human-readable full name. The existing internal Auth
     * username/email is derived invisibly, so no migration of 2,378 accounts
     * is needed. A class hint disambiguates identical full names.
     */
    suspend fun authenticateStudent(fullNameOrUsername: String, password: String): Student? {
        val identity = com.example.vokasilearning.logic.StudentLoginName.parse(fullNameOrUsername)
            ?: return null
        if (password.isBlank()) return null
        for (loginAlias in identity.aliases) {
            try {
                // A login attempt must never inherit a previous user's session.
                vokasiSupabase.auth.signOut()
                vokasiSupabase.auth.signInWith(Email) {
                    email = loginAlias + STUDENT_LOGIN_DOMAIN
                    this.password = password
                }
                val userId = vokasiSupabase.auth.currentUserOrNull()?.id ?: continue
                // PostgREST caps unfiltered selects; always look up the logged-in
                // account by ID rather than downloading the first 1,000 students.
                val roster = vokasiSupabase.from("student_roster").select {
                    filter { eq("auth_user_id", userId) }
                }.decodeList<RemoteRoster>()
                    .singleOrNull { it.registrationStatus == "registered" }
                    ?: continue
                if (identity.normalizedFullName != null &&
                    com.example.vokasilearning.logic.StudentLoginName.normalizeFullName(
                        roster.fullName
                    ) != identity.normalizedFullName) continue

                val remoteClass = vokasiSupabase.from("classes").select {
                    filter { eq("id", roster.classId) }
                }.decodeList<RemoteClass>().singleOrNull() ?: continue
                if (identity.classHint != null &&
                    remoteClass.className.trim().replace(Regex("\\s+"), " ")
                        .uppercase(java.util.Locale.ROOT) != identity.classHint) continue
                val majorId = roster.majorId ?: remoteClass.majorId
                val major = vokasiSupabase.from("majors").select {
                    filter { eq("id", majorId) }
                }.decodeList<RemoteMajor>().singleOrNull() ?: continue
                return Student(
                    username = roster.username ?: loginAlias,
                    password = "",
                    name = roster.fullName,
                    classroom = Classroom(remoteClass.className, major.name),
                    admissionYear = roster.admissionYear
                )
            } catch (_: Exception) {
                // A username collision can have a different alias. Try the next
                // candidate only when the user supplied an explicit class.
            } finally {
                // Retain the Supabase session only when authentication succeeded.
                // Kotlin finally runs even after return, so don't sign out here.
            }
        }
        try { vokasiSupabase.auth.signOut() } catch (_: Exception) { }
        return null
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
            resolveMajor(entry.major)?.second?.let { name -> normalized(name) } ?: normalized(entry.major),
            entry.admissionYear.toString()
        ).joinToString("|")

    private fun normalized(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()
}
