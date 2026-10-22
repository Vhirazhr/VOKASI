-- VOKASI | Read-only Supabase cross-table integrity audit
-- Execute in the Supabase SQL Editor with appropriate administrative access.
-- This script never inserts, updates, or deletes rows.
-- Returns a count of each potential problem; ideally all checks are zero.
WITH problems AS (
    SELECT 'roster_without_auth' AS issue, COUNT(*)::bigint AS total
    FROM public.student_roster r
    WHERE r.registration_status = 'registered' AND r.auth_user_id IS NULL

    UNION ALL
    SELECT 'registered_without_profile', COUNT(*)::bigint
    FROM public.student_roster r
    LEFT JOIN public.student_profiles p
        ON p.roster_id = r.id AND p.user_id = r.auth_user_id
    WHERE r.registration_status = 'registered' AND p.user_id IS NULL

    UNION ALL
    SELECT 'profile_link_mismatch', COUNT(*)::bigint
    FROM public.student_profiles p
    JOIN public.student_roster r ON r.id = p.roster_id
    WHERE p.user_id IS DISTINCT FROM r.auth_user_id

    UNION ALL
    SELECT 'roster_major_vs_class_mismatch', COUNT(*)::bigint
    FROM public.student_roster r
    JOIN public.classes c ON c.id = r.class_id
    WHERE r.major_id IS DISTINCT FROM c.major_id

    UNION ALL
    SELECT 'roster_schoolyear_vs_class_mismatch', COUNT(*)::bigint
    FROM public.student_roster r
    JOIN public.classes c ON c.id = r.class_id
    WHERE r.current_school_year IS DISTINCT FROM c.school_year

    UNION ALL
    SELECT 'registered_auth_email_vs_username_mismatch', COUNT(*)::bigint
    FROM public.student_roster r
    JOIN auth.users u ON u.id = r.auth_user_id
    WHERE r.registration_status = 'registered'
      AND LOWER(COALESCE(u.email, '')) IS DISTINCT FROM
          LOWER(COALESCE(r.username, '') || '@students.vokasi.invalid')

    UNION ALL
    SELECT 'registration_pending_with_auth', COUNT(*)::bigint
    FROM public.student_roster r
    WHERE r.registration_status = 'pending' AND r.auth_user_id IS NOT NULL

    UNION ALL
    SELECT 'missing_or_blank_username', COUNT(*)::bigint
    FROM public.student_roster r
    WHERE r.registration_status = 'registered'
      AND NULLIF(BTRIM(COALESCE(r.username, '')), '') IS NULL

    UNION ALL
    SELECT 'needs_review', COUNT(*)::bigint
    FROM public.student_roster r
    WHERE r.data_status = 'needs_review'
)
SELECT issue, total FROM problems ORDER BY issue;

-- For counts / completeness, run separately if needed:
-- SELECT
--   (SELECT COUNT(*) FROM public.student_roster) AS roster,
--   (SELECT COUNT(*) FROM public.student_profiles) AS profiles,
--   (SELECT COUNT(*) FROM auth.users) AS auth_accounts;
