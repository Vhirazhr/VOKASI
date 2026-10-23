-- VOKASI: school-wide protection against multiple compulsory XP awards per WIB day.
-- Apply ONCE in Supabase SQL Editor after 202610220002_vokasi_student_results.sql.
-- This does not delete or rewrite existing attempts / grades / accounts.
-- Existing historic XP stays unchanged; future compulsory submissions earn 50 XP
-- only for the first completed compulsory session of the school day.
BEGIN;

CREATE OR REPLACE FUNCTION public.vokasi_limit_daily_compulsory_xp()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  session_day date;
  prior_compulsory boolean;
BEGIN
  IF NEW.practice_category IS NULL AND NEW.question_count = 100 THEN
    session_day := (pg_catalog.to_timestamp(NEW.completed_at_ms / 1000.0)
      AT TIME ZONE 'Asia/Jakarta')::date;

    -- Serialize submissions by student and local day, including concurrent phones.
    PERFORM pg_catalog.pg_advisory_xact_lock(
      pg_catalog.hashtextextended(NEW.auth_user_id::text || ':' || session_day::text, 0)
    );

    SELECT EXISTS (
      SELECT 1
      FROM public.student_attempts AS recorded
      WHERE recorded.auth_user_id = NEW.auth_user_id
        AND recorded.practice_category IS NULL
        AND recorded.question_count = 100
        AND (pg_catalog.to_timestamp(recorded.completed_at_ms / 1000.0)
            AT TIME ZONE 'Asia/Jakarta')::date = session_day
    ) INTO prior_compulsory;

    NEW.earned_xp := CASE WHEN prior_compulsory THEN 0 ELSE 50 END;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS vokasi_limit_daily_compulsory_xp_before_insert
    ON public.student_attempts;
CREATE TRIGGER vokasi_limit_daily_compulsory_xp_before_insert
BEFORE INSERT ON public.student_attempts
FOR EACH ROW
EXECUTE FUNCTION public.vokasi_limit_daily_compulsory_xp();

REVOKE ALL ON FUNCTION public.vokasi_limit_daily_compulsory_xp() FROM PUBLIC, anon, authenticated;
COMMIT;
