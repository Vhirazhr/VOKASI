package com.example.vokasilearning.data

import com.example.vokasilearning.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Shared Supabase client for VOKASI.
 *
 * Set supabase.url and supabase.publishableKey in the local, git-ignored
 * local.properties file. Never put a Supabase secret/service_role key in the app.
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
            "Supabase belum dikonfigurasi. Tambahkan supabase.url dan supabase.publishableKey ke local.properties, lalu Sync Gradle."
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
