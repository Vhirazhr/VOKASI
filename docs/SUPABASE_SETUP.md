# Supabase setup for VOKASI Android

The Android app is configured to use the existing VOKASI Supabase project. The URL and **publishable** key are supplied as default values in `app/build.gradle.kts`, so no manual key copy into `local.properties` is required for the current project.

## Important security notes

- The `sb_publishable_...` key is designed for client applications and is not a secret. It will be visible in the APK; this is expected.
- The publishable key does **not** secure database rows by itself. Before real student, score, or school data is used, create and test restrictive Row Level Security (RLS) policies and authentication/authorization rules.
- Never put an `sb_secret_...` key or legacy `service_role` key in the Android app, GitHub, or any client-side code.
- Do not commit database passwords or PostgreSQL connection strings.

## Local override (optional)

For a different development project, add these to the root `local.properties` while keeping the existing `sdk.dir` line:

```properties
supabase.url=https://YOUR_PROJECT_REF.supabase.co
supabase.publishableKey=sb_publishable_YOUR_KEY
```

The values in `local.properties` override the defaults and the file remains git-ignored.

## Current integration

- Supabase Kotlin BOM, Auth and PostgREST modules
- Ktor Android HTTP engine
- Kotlin serialization plugin
- Android internet permission
- Shared `SupabaseProvider` at `app/src/main/java/com/example/vokasilearning/data/SupabaseProvider.kt`

This is the connection foundation only. It does not yet create the school/student tables, migrate local data, or connect existing login and admin/student screens to remote data.

Official guide: https://supabase.com/docs/guides/getting-started/quickstarts/kotlin
