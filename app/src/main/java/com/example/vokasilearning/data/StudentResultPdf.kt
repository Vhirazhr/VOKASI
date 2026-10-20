package com.example.vokasilearning.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.vokasilearning.logic.scoreGrade
import com.example.vokasilearning.model.AnswerStatus
import com.example.vokasilearning.model.Attempt
import com.example.vokasilearning.model.Student
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Student-owned, educational answer report. Generated entirely on-device.
 * It is NOT a digitally authenticated school certificate/rapor.
 */
internal object StudentResultPdf {
    private const val PAGE_WIDTH = 595   // A4 in points
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val RIGHT = PAGE_WIDTH - MARGIN
    private const val BOTTOM = PAGE_HEIGHT - 54f
    private val navy = Color.rgb(27, 57, 93)
    private val muted = Color.rgb(89, 101, 117)
    private val green = Color.rgb(29, 124, 89)
    private val red = Color.rgb(188, 62, 72)

    fun write(output: OutputStream, student: Student, attempt: Attempt) {
        require(attempt.review.isNotEmpty()) { "Riwayat pembahasan sesi ini tidak tersedia" }
        val pdf = PdfDocument()
        try {
            val writer = Writer(pdf)
            writer.start()
            val type = if (attempt.practiceCategory == null) "Latihan Wajib" else "Latihan Materi: ${attempt.practiceCategory}"
            writer.line("SMK NEGERI 1 SINGOSARI", 15f, true, navy, 23f)
            writer.line("VOKASI  |  LAPORAN HASIL PENGERJAAN SISWA", 12f, true, navy, 26f)
            writer.line("Dokumen pembelajaran - hasil dari sesi siswa, bukan rapor resmi.", 9f, false, muted, 21f)
            writer.rule()

            val date = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(Date(attempt.timeMillis))
            val name = attempt.studentNameSnapshot.ifBlank { student.name }
            val classroom = attempt.classSnapshot.ifBlank { student.classroom.label }
            val major = attempt.majorSnapshot.ifBlank { student.classroom.major }
            writer.line("Nama siswa  : $name", 11f, true)
            writer.line("Kelas             : $classroom", 10f)
            writer.line("Jurusan          : $major", 10f)
            writer.line("Tanggal         : $date", 10f)
            writer.line("Jenis latihan : $type", 10f)
            writer.line("ID sesi          : ${attempt.sessionKey.ifBlank { "-" }}", 9f, false, muted, 23f)
            writer.rule()

            val wrongAnswers = attempt.review.count { it.status == AnswerStatus.SALAH }
            val timedOut = attempt.review.count { it.status == AnswerStatus.TIMEOUT }
            val skipped = attempt.review.count { it.status == AnswerStatus.TIDAK_DIJAWAB }
            val incorrect = attempt.questionCount - attempt.correct
            writer.line("RINGKASAN HASIL", 12f, true, navy, 24f)
            writer.line("Nilai: ${attempt.score}/100    |    Grade: ${scoreGrade(attempt.score)}", 13f, true, gradeColor(attempt.score), 24f)
            writer.line("Benar ${attempt.correct} dari ${attempt.questionCount} soal  |  Salah total: $incorrect", 11f, true)
            writer.line("Rincian: Salah dijawab $wrongAnswers  |  Waktu habis $timedOut  |  Tidak dijawab $skipped", 10f)
            writer.line("Durasi: ${attempt.secondsSpent / 60} menit ${attempt.secondsSpent % 60} detik  |  XP: +${attempt.earnedXp}", 10f, false, muted, 24f)
            writer.rule()
            writer.line("RINCIAN JAWABAN DAN KUNCI", 12f, true, navy, 20f)
            writer.line("Tiap soal menampilkan jawaban siswa, kunci, dan pembahasan jika tersedia.", 9f, false, muted, 23f)

            attempt.review.forEachIndexed { i, review ->
                writer.keepSpace(128f)
                val status = when (review.status) {
                    AnswerStatus.BENAR -> "BENAR"
                    AnswerStatus.SALAH -> "SALAH"
                    AnswerStatus.TIMEOUT -> "WAKTU HABIS"
                    AnswerStatus.TIDAK_DIJAWAB -> "TIDAK DIJAWAB"
                }
                val color = if (review.status == AnswerStatus.BENAR) green else red
                writer.line(
                    "${i + 1}.  ${review.question.category}  [${review.question.difficultyCode}]  -  $status",
                    10f, true, color, 18f
                )
                writer.paragraph("Soal: ${review.question.question}", 10f, true)
                val selected = review.selectedIndex?.let { index ->
                    review.question.options.getOrNull(index)?.let { "${'A' + index}. $it" }
                } ?: "(tidak ada jawaban)"
                val answer = review.question.answerIndex
                writer.paragraph("Jawaban siswa: $selected", 9.5f)
                writer.paragraph(
                    "Kunci jawaban: ${'A' + answer}. ${review.question.options.getOrElse(answer) { "-" }}",
                    9.5f, true, green
                )
                if (review.question.explanation.isNotBlank()) {
                    writer.paragraph("Pembahasan: ${review.question.explanation}", 9.3f)
                }
                writer.line(
                    "Waktu respons: ${review.responseSeconds} / ${review.question.timeLimitSeconds} detik",
                    8.5f, false, muted, 19f
                )
                writer.rule(light = true)
            }
            writer.finish()
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }

    private fun gradeColor(score: Int): Int = when {
        score >= 90 -> green
        score >= 75 -> navy
        score >= 60 -> Color.rgb(155, 99, 19)
        else -> red
    }

    private class Writer(private val pdf: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN
        private var pageNo = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun start() = nextPage()
        fun finish() = closePage()

        private fun nextPage() {
            closePage()
            pageNo++
            page = pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNo).create()
            )
            canvas = page!!.canvas
            y = MARGIN
            if (pageNo > 1) {
                line("VOKASI  /  RINCIAN HASIL PENGERJAAN", 9f, true, navy, 23f)
                rule()
            }
        }

        private fun closePage() {
            val current = page ?: return
            paint.color = muted
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            current.canvas.drawLine(MARGIN, PAGE_HEIGHT - 39f, RIGHT, PAGE_HEIGHT - 39f, paint)
            current.canvas.drawText("VOKASI - Salinan hasil latihan siswa", MARGIN, PAGE_HEIGHT - 26f, paint)
            current.canvas.drawText("Halaman $pageNo", RIGHT - 66f, PAGE_HEIGHT - 26f, paint)
            pdf.finishPage(current)
            page = null
            canvas = null
        }

        fun keepSpace(required: Float) {
            if (y + required > BOTTOM) nextPage()
        }

        fun rule(light: Boolean = false) {
            keepSpace(18f)
            paint.color = if (light) Color.rgb(225, 231, 238) else Color.rgb(193, 205, 218)
            paint.strokeWidth = 1f
            canvas!!.drawLine(MARGIN, y, RIGHT, y, paint)
            y += if (light) 15f else 20f
        }

        fun line(
            value: String,
            fontSize: Float,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            height: Float = fontSize + 7f
        ) {
            paragraph(value, fontSize, bold, color, height)
        }

        fun paragraph(
            value: String,
            fontSize: Float,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            height: Float = fontSize + 5f
        ) {
            paint.color = color
            paint.textSize = fontSize
            paint.typeface = Typeface.create(
                Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL
            )
            val lines = wrap(value, paint, RIGHT - MARGIN)
            lines.forEach { segment ->
                keepSpace(height)
                paint.color = color
                paint.textSize = fontSize
                paint.typeface = Typeface.create(
                    Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL
                )
                canvas!!.drawText(segment, MARGIN, y, paint)
                y += height
            }
        }

        private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            val output = mutableListOf<String>()
            for (raw in text.split("\n")) {
                var current = ""
                for (word in raw.split(Regex("\\s+"))) {
                    if (word.isEmpty()) continue
                    val combined = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(combined) <= maxWidth) {
                        current = combined
                        continue
                    }
                    if (current.isNotEmpty()) output.add(current)
                    current = word
                    while (current.isNotEmpty() && paint.measureText(current) > maxWidth) {
                        val count = paint.breakText(current, true, maxWidth, null).coerceAtLeast(1)
                        output.add(current.take(count))
                        current = current.drop(count)
                    }
                }
                if (current.isNotEmpty()) output.add(current)
            }
            return output.ifEmpty { listOf("-") }
        }
    }
}
