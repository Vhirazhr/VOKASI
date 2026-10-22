package com.example.vokasilearning.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * School-wide leaderboard from an access-controlled PostgreSQL RPC.
 * Only aggregated XP and streak and a short display name leave the server;
 * answer reviews, grades, auth IDs and full names remain private.
 */
@Serializable
internal data class SchoolLeaderboardEntry(
    val username: String,
    @SerialName("display_name") val displayName: String,
    val major: String,
    val classroom: String,
    @SerialName("season_xp") val seasonXp: Long,
    @SerialName("lifetime_xp") val lifetimeXp: Long,
    val streak: Long
)

internal object SchoolLeaderboardRepository {
    suspend fun fetch(): List<SchoolLeaderboardEntry> =
        vokasiSupabase.postgrest
            .rpc("vokasi_school_leaderboard")
            .decodeAs<List<SchoolLeaderboardEntry>>()
}
