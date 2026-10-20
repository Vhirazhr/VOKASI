package com.example.vokasilearning.logic

import com.example.vokasilearning.model.Attempt
import java.util.Calendar
import java.util.TimeZone

internal enum class HistoryType(val label: String) {
    ALL("Semua"), REQUIRED("Wajib"), PRACTICE("Latihan")
}

internal enum class HistoryPeriod(val label: String) {
    ALL("Semua waktu"),
    TODAY("Hari ini"),
    LAST_SEVEN_DAYS("7 hari terakhir"),
    THIS_MONTH("Bulan ini"),
    EXACT_DATE("Tanggal tertentu"),
    MONTH_YEAR("Bulan & tahun"),
    YEAR("Tahun tertentu")
}

internal data class HistorySelection(
    val type: HistoryType = HistoryType.ALL,
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val date: String = "",
    val month: Int = 1,
    val year: Int = 2026
)

/** All dates use the school's Asia/Jakarta time zone, including day boundaries. */
internal object StudentHistoryFilter {
    fun filter(
        history: List<Attempt>,
        selection: HistorySelection,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Attempt> {
        val today = dayKey(nowMillis)
        val sevenDayStart = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -6)
        }.let { dayKey(it.timeInMillis) }
        val currentMonth = today.substring(0, 7)

        return history.asSequence()
            .filter { attempt ->
                when (selection.type) {
                    HistoryType.ALL -> true
                    HistoryType.REQUIRED -> attempt.practiceCategory == null
                    HistoryType.PRACTICE -> attempt.practiceCategory != null
                }
            }
            .filter { attempt ->
                val day = dayKey(attempt.timeMillis)
                when (selection.period) {
                    HistoryPeriod.ALL -> true
                    HistoryPeriod.TODAY -> day == today
                    HistoryPeriod.LAST_SEVEN_DAYS -> day >= sevenDayStart && day <= today
                    HistoryPeriod.THIS_MONTH -> day.startsWith(currentMonth)
                    HistoryPeriod.EXACT_DATE -> day == selection.date
                    HistoryPeriod.MONTH_YEAR ->
                        selection.month in 1..12 &&
                            day.startsWith("%04d-%02d".format(java.util.Locale.US, selection.year, selection.month))
                    HistoryPeriod.YEAR -> day.startsWith("%04d-".format(java.util.Locale.US, selection.year))
                }
            }
            .sortedWith(compareByDescending<Attempt> { it.timeMillis }.thenByDescending { it.sessionKey })
            .toList()
    }
}
