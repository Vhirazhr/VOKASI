package com.example.vokasilearning.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.logic.SchoolStatistics
import com.example.vokasilearning.ui.*
import java.util.Locale

/**
 * Lightweight Compose Canvas charts, no extra charting dependency.
 * All values and corresponding categories remain readable without relying on color alone.
 */
@Composable
internal fun SchoolScoreDonut(
    stats: SchoolStatistics, dataAvailable: Boolean = true, weekly: Boolean = false
) {
    val pieces = listOf(
        Triple("75–100", stats.high, Good),
        Triple("60–74", stats.middle, Warning),
        Triple("0–59", stats.low, Bad),
        Triple("Belum tercatat", stats.missing, Subtle)
    )
    WhiteCard(Modifier.fillMaxWidth()) {
        Text("Distribusi nilai", color = Ink, fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold)
        if (!dataAvailable) {
            Text("Menunggu hasil latihan tersinkron dari Supabase.",
                color = Subtle, fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val width = 17.dp.toPx()
                    val inset = width / 2 + 3.dp.toPx()
                    val square = Size(size.width - 2 * inset, size.height - 2 * inset)
                    val origin = Offset(inset, inset)
                    drawArc(
                        color = Border, startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = origin, size = square,
                        style = Stroke(width = width)
                    )
                    if (dataAvailable && stats.students > 0) {
                        var angle = -90f
                        pieces.forEach { (_, count, color) ->
                            if (count > 0) {
                                val sweep = 360f * count.toFloat() / stats.students
                                drawArc(
                                    color = color, startAngle = angle, sweepAngle = sweep,
                                    useCenter = false, topLeft = origin, size = square,
                                    style = Stroke(width = width)
                                )
                                angle += sweep
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (dataAvailable) "${stats.participationPercent}%" else "—", color = Navy,
                        fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (dataAvailable) "Selesai" else "Menunggu", color = Subtle, fontSize = 11.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pieces.forEach { (label, count, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Text(label, color = Subtle, fontSize = 11.sp,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        Text(if (dataAvailable) "$count" else "—", color = Ink, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(if (dataAvailable)
            if (weekly) "Setiap siswa dihitung sekali dari nilai wajib terakhir pada pekan terpilih."
            else "Setiap siswa dihitung sekali berdasarkan nilai wajib terakhir pada bulan terpilih."
            else "Grafik akan terisi setelah data berhasil dimuat.",
            color = Subtle, fontSize = 11.sp)
    }
}

@Composable
internal fun SchoolMonthlyTrend(
    stats: SchoolStatistics, dataAvailable: Boolean = true, weekly: Boolean = false
) {
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    WhiteCard(Modifier.fillMaxWidth()) {
        Text(if (weekly) "Perkembangan rata-rata harian" else "Perkembangan rata-rata bulanan",
            color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (weekly) "7 hari dalam pekan terpilih • Skala nilai 0–100"
            else "6 bulan hingga periode terpilih • Skala nilai 0–100",
            color = Subtle, fontSize = 11.sp)
        val recordedMonths = if (dataAvailable) stats.trend.count { it.second != null } else 0
        val hasData = recordedMonths > 0
        if (!hasData) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Text(if (dataAvailable) "Belum ada nilai wajib untuk grafik ini."
                    else "Menunggu hasil latihan tersinkron.",
                    color = Subtle, fontSize = 12.sp)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Column(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("100", "75", "50", "25", "0").forEach {
                        Text(it, color = Subtle, fontSize = 10.sp)
                    }
                }
                Canvas(
                    Modifier.weight(1f).height(150.dp)
                ) {
                    val pad = 7.dp.toPx()
                    val top = pad
                    val bottom = size.height - pad
                    val graphHeight = bottom - top
                    val left = pad
                    val right = size.width - pad
                    val points = stats.trend
                    val gap = if (points.size <= 1) 0f else (right - left) / (points.size - 1)
                    fun y(score: Int): Float = bottom - score.coerceIn(0, 100) / 100f * graphHeight
                    listOf(0, 25, 50, 75, 100).forEach { tick ->
                        drawLine(Border, Offset(left, y(tick)), Offset(right, y(tick)),
                            strokeWidth = 1.dp.toPx())
                    }
                    var previous: Offset? = null
                    points.forEachIndexed { index, (_, grade) ->
                        if (grade != null) {
                            val point = Offset(left + index * gap, y(grade))
                            previous?.let { from ->
                                drawLine(NavyLight, from, point,
                                    strokeWidth = 3.dp.toPx())
                            }
                            drawCircle(Navy, radius = 4.dp.toPx(), center = point)
                            previous = point
                        } else {
                            // Missing months are gaps, never a misleading zero-grade line.
                            previous = null
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                stats.trend.forEach { (key, value) ->
                    val monthIndex = (key.takeLast(2).toIntOrNull() ?: 1) - 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (weekly) key.takeLast(2) else monthNames[monthIndex.coerceIn(0, 11)],
                            color = Subtle, fontSize = 10.sp)
                        Text(value?.toString() ?: "—",
                            color = if (value == null) Subtle else Navy,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            if (recordedMonths == 1 && weekly)
                "Baru 1 hari memiliki nilai wajib. Grafik mencatat titiknya; " +
                    "garis akan tampak saat ada nilai pada hari lain di pekan ini."
            else if (recordedMonths == 1)
                "Baru 1 bulan memiliki nilai wajib. Titiknya sudah dicatat; " +
                    "garis perkembangan baru terbentuk jika ada bulan lain yang memiliki nilai."
            else if (weekly)
                "Tiap titik adalah rata-rata nilai wajib pada hari tersebut. " +
                    "Hari tanpa data bukan nilai 0."
            else "Tiap titik memakai nilai wajib terbaru per siswa dalam bulan tersebut. " +
                "Bulan tanpa data tidak dihitung sebagai nilai 0.",
            color = Subtle, fontSize = 11.sp
        )
    }
}
