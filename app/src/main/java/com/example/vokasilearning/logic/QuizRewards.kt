package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt

/**
 * One compulsory XP award per pupil per WIB calendar day.
 * Repeated compulsory sessions remain valid grade submissions, but award no XP.
 * Material practice follows the existing once-per-material-per-day rule.
 */
internal object QuizRewards {
    fun earnedXp(
        username: String,
        previous: List<Attempt>,
        nowMillis: Long,
        sessionKey: String,
        practiceCategory: String?,
        correct: Int
    ): Int {
        val ownToday = previous.filter {
            it.studentId.equals(username, ignoreCase = true) &&
                dayKey(it.timeMillis) == dayKey(nowMillis)
        }
        if (ownToday.any { it.sessionKey == sessionKey }) return 0
        val alreadyEarned = ownToday.sumOf { it.earnedXp }
        val remaining = (DAILY_XP_CAP - alreadyEarned).coerceAtLeast(0)
        return if (practiceCategory == null) {
            // Even a low score is a completed 100-question compulsory session.
            // Exactly one completed session is eligible for its daily 50 XP.
            if (ownToday.any { it.practiceCategory == null }) 0
            else minOf(SESSION_XP, remaining)
        } else {
            if (correct <= 0 || ownToday.any {
                    it.practiceCategory == practiceCategory && it.earnedXp > 0
                }
            ) 0 else minOf(5, remaining)
        }
    }
}
