package com.example.vokasilearning.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.*
import com.example.vokasilearning.ui.*
import java.util.Calendar
import java.util.Locale

/** One compact filter on Profile: optional type plus a calendar date range. */
@Composable
internal fun StudentHistoryFilterControls(
    applied: HistorySelection,
    onApply: (HistorySelection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(applied) { mutableStateOf(applied) }
    val context = LocalContext.current

    fun chooseDate(start: Boolean) {
        val existing = if (start) draft.fromDate else draft.toDate
        val bits = existing.split("-").mapNotNull { it.toIntOrNull() }
        val calendar = wibCalendar()
        val year = bits.getOrNull(0) ?: calendar.get(Calendar.YEAR)
        val month = (bits.getOrNull(1) ?: calendar.get(Calendar.MONTH) + 1) - 1
        val day = bits.getOrNull(2) ?: calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val selected = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                draft = if (start) {
                    draft.copy(
                        fromDate = selected,
                        toDate = draft.toDate.takeIf { it >= selected }.orEmpty()
                    )
                } else {
                    draft.copy(
                        toDate = selected,
                        fromDate = draft.fromDate.takeIf { it <= selected }.orEmpty()
                    )
                }
            }, year, month, day
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            val lower = if (start) "" else draft.fromDate
            if (lower.isNotBlank()) {
                val range = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                    isLenient = false
                }.parse(lower)?.time
                if (range != null) datePicker.minDate = range
            }
        }.show()
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudentSectionHeading("AKTIVITAS", "Riwayat nilai", Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    if (!expanded) draft = applied
                    expanded = !expanded
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Filter", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }

        if (expanded) {
            WhiteCard(Modifier.fillMaxWidth()) {
                Text("Jenis latihan", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    HistoryType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.type == type,
                            onClick = { draft = draft.copy(type = type) },
                            label = { Text(type.label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("Rentang tanggal", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { chooseDate(true) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            draft.fromDate.ifBlank { "Dari tanggal" },
                            fontSize = 11.sp, color = Ink,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { chooseDate(false) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            draft.toDate.ifBlank { "Sampai tanggal" },
                            fontSize = 11.sp, color = Ink,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        val reset = draft.copy(
                            type = HistoryType.ALL, period = HistoryPeriod.ALL,
                            fromDate = "", toDate = ""
                        )
                        draft = reset
                        onApply(reset)
                        expanded = false
                    }) { Text("Reset", color = Subtle) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApply(draft.copy(period = HistoryPeriod.ALL))
                            expanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Terapkan") }
                }
            }
        }
    }
}
