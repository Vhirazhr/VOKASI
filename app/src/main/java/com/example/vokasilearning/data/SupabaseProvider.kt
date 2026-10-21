package com.example.vokasilearning.data

import com.example.vokasilearning.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Shared Supabase client for VOKASI.
 *
 * Uses the VOKASI project URL and client-safe publishable key configured in BuildConfig.
 * Local properties may override these values for development. Never embed a secret/service_role key.
 */
object SupabaseProvider {
    private val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.trim()

    private val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = projectUrl.startsWith("https://") && publishableKey.isNotBlank()

    val client: SupabaseClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        check(isConfigured) {
            "Konfigurasi Supabase belum valid. Periksa Project URL dan Publishable Key pada app/build.gradle.kts atau local.properties."
        }

        createSupabaseClient(
            supabaseUrl = projectUrl,
            supabaseKey = publishableKey
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
