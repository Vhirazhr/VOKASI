-- VOKASI school database foundation.
-- Safe to apply after the earlier empty-schema check. Does not import student records.
-- Account provisioning is performed by a server-side Edge Function, never by the Android client.

BEGIN;

CREATE TABLE IF NOT EXISTS public.majors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL CHECK (BTRIM(name) <> ''),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.majors ADD COLUMN IF NOT EXISTS code TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS majors_code_unique ON public.majors (code);

CREATE TABLE IF NOT EXISTS public.classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    major_id UUID NOT NULL REFERENCES public.majors(id) ON DELETE RESTRICT,
    grade_level SMALLINT NOT NULL CHECK (grade_level IN (10, 11, 12)),
    class_name TEXT NOT NULL CHECK (BTRIM(class_name) <> ''),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.classes
    ADD COLUMN IF NOT EXISTS school_year TEXT NOT NULL DEFAULT '2026/2027';

-- Replace the initial single-year unique index with a school-year-aware index.
DROP INDEX IF EXISTS public.classes_unique_name;
CREATE UNIQUE INDEX IF NOT EXISTS classes_unique_school_year_ci
    ON public.classes (major_id, grade_level, LOWER(BTRIM(class_name)), school_year);

CREATE TABLE IF NOT EXISTS public.student_roster (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL CHECK (BTRIM(full_name) <> ''),
    class_id UUID NOT NULL REFERENCES public.classes(id) ON DELETE RESTRICT,
    admission_year INTEGER NOT NULL CHECK (admission_year BETWEEN 2000 AND 2100),
    registration_status TEXT NOT NULL DEFAULT 'pending'
        CHECK (registration_status IN ('pending', 'registered', 'inactive')),
    username TEXT,
    auth_user_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE SET NULL,
    registered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.student_roster
    ADD COLUMN IF NOT EXISTS major_id UUID REFERENCES public.majors(id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS identity_key TEXT,
    ADD COLUMN IF NOT EXISTS current_school_year TEXT NOT NULL DEFAULT '2026/2027',
    ADD COLUMN IF NOT EXISTS data_status TEXT NOT NULL DEFAULT 'valid'
        CHECK (data_status IN ('valid', 'needs_review'));

CREATE UNIQUE INDEX IF NOT EXISTS student_roster_username_unique_ci
    ON public.student_roster (LOWER(BTRIM(username)))
    WHERE username IS NOT NULL AND BTRIM(username) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS student_roster_identity_key_unique
    ON public.student_roster (identity_key)
    WHERE identity_key IS NOT NULL AND BTRIM(identity_key) <> '';
CREATE INDEX IF NOT EXISTS student_roster_class_idx ON public.student_roster (class_id);
CREATE INDEX IF NOT EXISTS student_roster_status_idx ON public.student_roster (registration_status);
CREATE INDEX IF NOT EXISTS student_roster_admission_year_idx ON public.student_roster (admission_year);

CREATE TABLE IF NOT EXISTS public.student_profiles (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    roster_id UUID UNIQUE REFERENCES public.student_roster(id) ON DELETE CASCADE,
    recovery_email TEXT,
    email_verified_at TIMESTAMPTZ,
    avatar_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.school_admins (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT 'Admin Sekolah',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- OTPs are hashed and only accessible to trusted Edge Functions using the service role.
CREATE TABLE IF NOT EXISTS public.student_email_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    purpose TEXT NOT NULL CHECK (purpose IN ('verify_email', 'password_reset')),
    code_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS student_email_challenges_user_purpose_idx
    ON public.student_email_challenges (user_id, purpose, created_at DESC);

CREATE TABLE IF NOT EXISTS public.school_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    rows_received INTEGER NOT NULL DEFAULT 0,
    accounts_created INTEGER NOT NULL DEFAULT 0,
    accounts_existing INTEGER NOT NULL DEFAULT 0,
    failed_rows INTEGER NOT NULL DEFAULT 0,
    result_details JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION public.vokasi_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS student_roster_set_updated_at ON public.student_roster;
CREATE TRIGGER student_roster_set_updated_at
    BEFORE UPDATE ON public.student_roster
    FOR EACH ROW EXECUTE FUNCTION public.vokasi_set_updated_at();

DROP TRIGGER IF EXISTS student_profiles_set_updated_at ON public.student_profiles;
CREATE TRIGGER student_profiles_set_updated_at
    BEFORE UPDATE ON public.student_profiles
    FOR EACH ROW EXECUTE FUNCTION public.vokasi_set_updated_at();

CREATE OR REPLACE FUNCTION public.is_school_admin()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.school_admins AS a
        WHERE a.user_id = auth.uid()
          AND a.is_active = TRUE
    );
$$;
REVOKE ALL ON FUNCTION public.is_school_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_school_admin() TO authenticated;

ALTER TABLE public.majors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_roster ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_email_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_import_batches ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS majors_read_signed_in_school_users ON public.majors;
CREATE POLICY majors_read_signed_in_school_users ON public.majors
    FOR SELECT TO authenticated
    USING (
        public.is_school_admin()
        OR EXISTS (SELECT 1 FROM public.student_profiles p WHERE p.user_id = auth.uid())
    );

DROP POLICY IF EXISTS classes_read_signed_in_school_users ON public.classes;
CREATE POLICY classes_read_signed_in_school_users ON public.classes
    FOR SELECT TO authenticated
    USING (
        public.is_school_admin()
        OR EXISTS (SELECT 1 FROM public.student_profiles p WHERE p.user_id = auth.uid())
    );

DROP POLICY IF EXISTS roster_read_own_or_admin ON public.student_roster;
CREATE POLICY roster_read_own_or_admin ON public.student_roster
    FOR SELECT TO authenticated
    USING (public.is_school_admin() OR auth_user_id = auth.uid());

DROP POLICY IF EXISTS student_profiles_read_own_or_admin ON public.student_profiles;
CREATE POLICY student_profiles_read_own_or_admin ON public.student_profiles
    FOR SELECT TO authenticated
    USING (user_id = auth.uid() OR public.is_school_admin());

DROP POLICY IF EXISTS student_profiles_update_own ON public.student_profiles;
CREATE POLICY student_profiles_update_own ON public.student_profiles
    FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS school_import_batches_read_admin ON public.school_import_batches;
CREATE POLICY school_import_batches_read_admin ON public.school_import_batches
    FOR SELECT TO authenticated
    USING (public.is_school_admin());

-- The Android app may read permitted rows, but it cannot provision users or edit school identity.
REVOKE ALL ON public.majors, public.classes, public.student_roster,
    public.student_profiles, public.school_admins, public.student_email_challenges,
    public.school_import_batches FROM anon, authenticated;

GRANT SELECT ON public.majors, public.classes, public.student_roster,
    public.student_profiles, public.school_import_batches TO authenticated;
GRANT UPDATE (recovery_email, avatar_path) ON public.student_profiles TO authenticated;
GRANT ALL ON public.majors, public.classes, public.student_roster,
    public.student_profiles, public.school_admins, public.student_email_challenges,
    public.school_import_batches TO service_role;

INSERT INTO public.majors (code, name, is_active) VALUES
    ('DPIB', 'Desain Pemodelan dan Informasi Bangunan', TRUE),
    ('GEO',  'Teknik Geomatika', TRUE),
    ('TAB',  'Teknik Alat Berat', TRUE),
    ('TITL', 'Teknik Instalasi Tenaga Listrik', TRUE),
    ('TKP',  'Teknik Konstruksi dan Perumahan', TRUE),
    ('TKR',  'Teknik Kendaraan Ringan', TRUE),
    ('TMI',  'Teknik Mekanik Industri', TRUE),
    ('TOI',  'Teknik Otomasi Industri', TRUE),
    ('TP',   'Teknik Pengelasan', TRUE),
    ('TPM',  'Teknik Pemesinan', TRUE),
    ('TSM',  'Teknik Sepeda Motor', TRUE)
ON CONFLICT (code) DO UPDATE
    SET name = EXCLUDED.name, is_active = TRUE;

COMMIT;
