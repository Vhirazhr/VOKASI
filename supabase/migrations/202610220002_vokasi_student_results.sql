-- VOKASI: shared quiz results for cross-device school dashboards.
-- Apply once in Supabase SQL Editor. Additive: does not delete roster or accounts.
BEGIN;
CREATE TABLE IF NOT EXISTS public.student_attempts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 auth_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
 username TEXT NOT NULL CHECK (LENGTH(BTRIM(username)) BETWEEN 2 AND 100),
 session_key UUID NOT NULL,
 completed_at_ms BIGINT NOT NULL CHECK (completed_at_ms > 0),
 signature TEXT NOT NULL DEFAULT '',
 correct SMALLINT NOT NULL CHECK (correct >= 0),
 answered SMALLINT NOT NULL CHECK (answered >= 0),
 question_count SMALLINT NOT NULL CHECK (question_count IN (10, 100)),
 score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
 earned_xp SMALLINT NOT NULL DEFAULT 0 CHECK (earned_xp BETWEEN 0 AND 100),
 seconds_spent INTEGER NOT NULL DEFAULT 0 CHECK (seconds_spent BETWEEN 0 AND 7200),
 practice_category TEXT,
 name_snapshot TEXT NOT NULL DEFAULT '',
 class_snapshot TEXT NOT NULL DEFAULT '',
 major_snapshot TEXT NOT NULL DEFAULT '',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT student_attempts_counts_check CHECK (correct <= answered AND answered <= question_count),
 CONSTRAINT student_attempts_score_check CHECK (score = (correct * 100 / question_count)),
 CONSTRAINT student_attempts_session_unique UNIQUE (auth_user_id, session_key)
);
CREATE INDEX IF NOT EXISTS student_attempts_admin_timeline_idx ON public.student_attempts (completed_at_ms DESC);
CREATE INDEX IF NOT EXISTS student_attempts_username_timeline_idx ON public.student_attempts (LOWER(username), completed_at_ms DESC);
CREATE INDEX IF NOT EXISTS student_attempts_auth_timeline_idx ON public.student_attempts (auth_user_id, completed_at_ms DESC);
ALTER TABLE public.student_attempts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS student_attempts_select_own_or_admin ON public.student_attempts;
CREATE POLICY student_attempts_select_own_or_admin ON public.student_attempts
 FOR SELECT TO authenticated
 USING (auth_user_id = auth.uid() OR public.is_school_admin());
DROP POLICY IF EXISTS student_attempts_insert_own ON public.student_attempts;
CREATE POLICY student_attempts_insert_own ON public.student_attempts
 FOR INSERT TO authenticated
 WITH CHECK (
    auth_user_id = auth.uid()
    AND EXISTS (
        SELECT 1 FROM public.student_roster AS r
        WHERE r.auth_user_id = auth.uid()
          AND r.registration_status = 'registered'
          AND LOWER(BTRIM(r.username)) = LOWER(BTRIM(student_attempts.username))
    )
 );
-- Scores are append-only. These are client-computed training results, not server-verified exams.
REVOKE ALL ON public.student_attempts FROM PUBLIC, anon, authenticated;
GRANT SELECT, INSERT ON public.student_attempts TO authenticated;
GRANT ALL ON public.student_attempts TO service_role;
COMMIT;
