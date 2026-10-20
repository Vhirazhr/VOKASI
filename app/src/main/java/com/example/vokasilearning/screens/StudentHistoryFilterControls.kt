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
import java.util.Locale

/** Compact controls hosted inside Profile, not a separate history page. */
@Composable
internal fun StudentHistoryFilterControls(
    applied: HistorySelection,
    availableYears: List<Int>,
    onApply: (HistorySelection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(applied) { mutableStateOf(applied) }
    var periodsOpen by remember { mutableStateOf(false) }
    var monthsOpen by remember { mutableStateOf(false) }
    var yearsOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

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
            ) { Text("☷  Filter", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
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
                Text("Periode", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { periodsOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(draft.period.label, modifier = Modifier.weight(1f), color = Ink)
                        Text("⌄", color = Navy)
                    }
                    DropdownMenu(expanded = periodsOpen, onDismissRequest = { periodsOpen = false }) {
                        HistoryPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = {
                                    draft = draft.copy(period = period)
                                    periodsOpen = false
                                }
                            )
                        }
                    }
                }
                if (draft.period == HistoryPeriod.EXACT_DATE) {
                    OutlinedButton(
                        onClick = {
                            val parts = draft.date.split("-").mapNotNull { it.toIntOrNull() }
                            val calendar = wibCalendar()
                            val yy = parts.getOrNull(0) ?: calendar.get(java.util.Calendar.YEAR)
                            val mm = parts.getOrNull(1)?.minus(1) ?: calendar.get(java.util.Calendar.MONTH)
                            val dd = parts.getOrNull(2) ?: calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    draft = draft.copy(
                                        date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                    )
                                },
                                yy, mm, dd
                            ).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(draft.date, modifier = Modifier.weight(1f), color = Ink)
                        Text("Pilih tanggal", color = Navy, fontSize = 12.sp)
                    }
                }
                if (draft.period == HistoryPeriod.MONTH_YEAR || draft.period == HistoryPeriod.YEAR) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (draft.period == HistoryPeriod.MONTH_YEAR) {
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { monthsOpen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(months[draft.month.coerceIn(1, 12) - 1], fontSize = 12.sp) }
                                DropdownMenu(
                                    expanded = monthsOpen,
                                    onDismissRequest = { monthsOpen = false }
                                ) {
                                    months.forEachIndexed { index, month ->
                                        DropdownMenuItem(
                                            text = { Text(month) },
                                            onClick = { draft = draft.copy(month = index + 1); monthsOpen = false }
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { yearsOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(draft.year.toString(), fontSize = 12.sp) }
                            DropdownMenu(
                                expanded = yearsOpen,
                                onDismissRequest = { yearsOpen = false }
                            ) {
                                availableYears.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString()) },
                                        onClick = { draft = draft.copy(year = year); yearsOpen = false }
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val reset = applied.copy(type = HistoryType.ALL, period = HistoryPeriod.ALL)
                        draft = reset
                        onApply(reset)
                        expanded = false
                    }) { Text("Reset", color = Subtle) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(draft); expanded = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Terapkan") }
                }
            }
        }
    }
}
