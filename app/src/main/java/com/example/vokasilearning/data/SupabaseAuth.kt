package com.example.vokasilearning.data

import com.example.vokasilearning.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

/**
 * Public client for Android only. The publishable key is intended for client use;
 * never put the Supabase service-role/secret key in this file or the APK.
 */
internal val vokasiSupabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
) {
    install(Auth)
    install(Postgrest)
}

/**
 * Signs in an admin by email and asks Postgres to verify their active admin role.
 * A valid Auth account without an active school_admins row is not allowed into the panel.
 */
internal suspend fun authenticateSchoolAdmin(email: String, password: String): Boolean {
    return try {
        vokasiSupabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        val isAdmin = vokasiSupabase.postgrest
            .rpc("is_school_admin")
            .decodeAs<Boolean>()

        if (!isAdmin) {
            vokasiSupabase.auth.signOut()
        }
        isAdmin
    } catch (_: Exception) {
        try {
            vokasiSupabase.auth.signOut()
        } catch (_: Exception) {
            // Keep the login error generic; never expose server details to the UI.
        }
        false
    }
}
