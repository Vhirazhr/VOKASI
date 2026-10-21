# Supabase setup for VOKASI Android

This repository now includes the first database migration and the server-side account-provisioning function. The existing Android screens still use local storage until a later integration step; adding these files does not automatically move local data online.

## 1. Apply the database schema

Open the existing VOKASI project in Supabase and open SQL Editor. Run the complete migration file:

`supabase/migrations/202610210001_vokasi_school_core.sql`

It creates/updates the school master tables, seeds the 11 confirmed major codes, adds student roster/profile tables, and enables Row Level Security. It does not import the Excel file or create student accounts.

## 2. Account model

- Admin imports a verified school roster in batches of up to 25 rows through the server-side `provision-students` Edge Function.
- The function creates a Supabase Auth account and a roster row for each valid student. Username is generated from the full name; collisions receive a numeric suffix.
- Initial password is read from the Edge Function secret `VOKASI_INITIAL_STUDENT_PASSWORD`. For the currently agreed policy, set that secret to `12345678`. It is only applied when an account is first created; importing again does not reset a student's changed password.
- Login alias is `<username>@students.vokasi.invalid`. This is a non-deliverable internal Auth identifier; students still enter only their username in the VOKASI UI. The Android sign-in integration must derive the same alias.
- Never use the service-role key in Android or commit it to GitHub.

## 3. Configure and deploy the Edge Function (not yet connected to the Android import UI)

Deploy `supabase/functions/provision-students/index.ts` to the same existing Supabase project. Supabase supplies `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` to the Edge Function runtime. Configure the additional secret `VOKASI_INITIAL_STUDENT_PASSWORD=12345678` in Edge Function secrets.

The caller must already be signed in through Supabase Auth and have an active row in `public.school_admins`. The existing local-only demo admin does not satisfy this check until admin authentication is migrated. Do not bypass this check or expose the service-role key.

Input body:

```json
{
  "students": [
    {
      "fullName": "Contoh Nama Siswa",
      "majorCode": "TITL",
      "majorName": "Teknik Instalasi Tenaga Listrik",
      "gradeLevel": 11,
      "classLabel": "XI TITL 2",
      "admissionYear": 2025,
      "schoolYear": "2026/2027"
    }
  ]
}
```

Send no more than 25 students per request. The function is intended to be retried with the same roster rows; it preserves existing account passwords and reports per-row failures. Ambiguous duplicate identity keys (same normalized name + major + admission year) must be reviewed before production import.

## 4. Shared initial password — security note

The agreed initial password is `12345678` for all provisioned students, and students may keep it until they choose to change it. This is a significant account-takeover risk because usernames are derived from names. Use only with the school's explicit approval; never store the password in the roster table. Supabase Auth stores password verifiers, not plaintext passwords. The function secret keeps the value out of the Android client and source code.

## 5. Email and profile status

`student_profiles.recovery_email`, `email_verified_at`, and `avatar_path` provide the fields needed for profile-completion monitoring. OTP delivery/verification and password recovery are not implemented yet; they require a mail provider and dedicated server-side functions. The client is intentionally unable to update `email_verified_at` directly.

## 6. Confirmed school catalog

The migration seeds these confirmed codes/names: DPIB, GEO, TAB, TITL, TKP, TKR, TMI, TOI, TP, TPM, TSM. Student rows with missing or ambiguous class/major/year data should be marked for review instead of guessed.

Official Supabase Kotlin guide: https://supabase.com/docs/guides/getting-started/quickstarts/kotlin
