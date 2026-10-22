-- VOKASI school-wide leaderboard: privacy-minimal, read-only RPC.
-- Run in Supabase SQL Editor after student_attempts migration.
-- Does not edit existing student accounts, submissions, or dashboards.
BEGIN;

CREATE OR REPLACE FUNCTION public.vokasi_school_leaderboard()
RETURNS TABLE (
  username TEXT,
  display_name TEXT,
  major TEXT,
  classroom TEXT,
  season_xp BIGINT,
  lifetime_xp BIGINT,
  streak BIGINT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
  WITH local_today AS (
    SELECT (NOW() AT TIME ZONE 'Asia/Jakarta')::date AS d
  ),
  earned AS (
    SELECT a.auth_user_id,
      COALESCE(SUM(a.earned_xp) FILTER (
        WHERE (TO_TIMESTAMP(a.completed_at_ms / 1000.0)
          AT TIME ZONE 'Asia/Jakarta')::date >=
          DATE_TRUNC('quarter', NOW() AT TIME ZONE 'Asia/Jakarta')::date
      ), 0)::bigint AS season_points,
      COALESCE(SUM(a.earned_xp), 0)::bigint AS all_points
    FROM public.student_attempts AS a
    GROUP BY a.auth_user_id
  ),
  practice_days AS (
    SELECT DISTINCT
      a.auth_user_id,
      (TO_TIMESTAMP(a.completed_at_ms / 1000.0)
        AT TIME ZONE 'Asia/Jakarta')::date AS d
    FROM public.student_attempts AS a
    WHERE a.answered > 0
  ),
  numbered_days AS (
    SELECT p.auth_user_id, p.d,
      MAX(p.d) OVER (PARTITION BY p.auth_user_id) AS latest_day,
      (ROW_NUMBER() OVER (
        PARTITION BY p.auth_user_id ORDER BY p.d DESC
      ) - 1)::integer AS sequence_index
    FROM practice_days AS p
  ),
  streaks AS (
    SELECT n.auth_user_id,
      CASE WHEN MAX(n.latest_day) >= (SELECT d - 1 FROM local_today)
        THEN COUNT(*) FILTER (
          WHERE n.d = n.latest_day - n.sequence_index
        )::bigint
        ELSE 0::bigint
      END AS active_streak
    FROM numbered_days AS n
    GROUP BY n.auth_user_id
  )
  SELECT r.username,
    SPLIT_PART(BTRIM(r.full_name), ' ', 1) AS display_name,
    m.name AS major,
    c.class_name AS classroom,
    earned.season_points AS season_xp,
    earned.all_points AS lifetime_xp,
    COALESCE(streaks.active_streak, 0)::bigint AS streak
  FROM public.student_roster AS r
  INNER JOIN earned ON earned.auth_user_id = r.auth_user_id
  INNER JOIN public.classes AS c ON c.id = r.class_id
  INNER JOIN public.majors AS m ON m.id = c.major_id
  LEFT JOIN streaks ON streaks.auth_user_id = r.auth_user_id
  WHERE r.registration_status = 'registered'
    AND r.username IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM public.student_roster AS caller
      WHERE caller.auth_user_id = auth.uid()
        AND caller.registration_status = 'registered'
    )
  ORDER BY earned.season_points DESC, r.username
  LIMIT 2500;
$$;

REVOKE ALL ON FUNCTION public.vokasi_school_leaderboard() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.vokasi_school_leaderboard() FROM anon;
GRANT EXECUTE ON FUNCTION public.vokasi_school_leaderboard() TO authenticated;
COMMIT;
