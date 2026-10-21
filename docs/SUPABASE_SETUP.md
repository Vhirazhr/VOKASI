# Supabase setup for VOKASI Android

This change adds the Supabase Kotlin SDK foundation only. It does not yet migrate local student data, create production tables, or change any VOKASI screens.

## Configure local credentials

1. Open the project root `local.properties` file in Android Studio. Keep the existing `sdk.dir=...` line.
2. Add the project URL and the **publishable** key from the Supabase project's Connect panel:

```properties
supabase.url=https://YOUR_PROJECT_REF.supabase.co
supabase.publishableKey=sb_publishable_YOUR_KEY
```

3. Save the file and click **Sync Project with Gradle Files**.

The repository ignores `local.properties`; do not commit it. If you share a screenshot, hide keys. A publishable key is designed for client apps, but it is not a substitute for Row Level Security (RLS).

**Never put a Supabase secret key or legacy `service_role` key in the Android app.** Those keys bypass normal RLS protections and belong only in trusted server environments.

## Kotlin usage

Use `SupabaseProvider.isConfigured` to check whether local configuration exists. Access `SupabaseProvider.client` only after configuration is present.

The provider installs Auth and PostgREST. Database access should not be wired to real student data until the schema and restrictive RLS policies have been reviewed and applied.

## Included in this setup

- Supabase Kotlin BOM, Auth and PostgREST modules
- Ktor Android HTTP engine
- Kotlin serialization compiler plugin
- Internet permission
- Core library desugaring for the app's current minSdk
- Shared `SupabaseProvider`

Official guide: https://supabase.com/docs/guides/getting-started/quickstarts/kotlin
