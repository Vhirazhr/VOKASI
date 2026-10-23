package com.example.vokasilearning.data

import java.util.Locale

/**
 * Stable school-account matching. Student-generated attempt usernames are
 * historical aliases; Supabase Auth UUID identifies the actual roster student.
 * No data is changed in Postgres; the display key is resolved in memory.
 */
internal object AdminAttemptIdentity {
    fun rosterNames(roster: List<SchoolRosterEntry>): Map<String, String> =
        roster.asSequence().filter {
            it.registered && !it.remoteAuthUserId.isNullOrBlank() && it.username.isNotBlank()
        }.associate { it.remoteAuthUserId!!.trim().lowercase(Locale.ROOT) to it.username.trim() }

    fun resolve(
        authUserId: String, recordedUsername: String,
        nameByAuthId: Map<String, String>
    ): String = nameByAuthId[authUserId.trim().lowercase(Locale.ROOT)]
        ?: recordedUsername.trim()
}
