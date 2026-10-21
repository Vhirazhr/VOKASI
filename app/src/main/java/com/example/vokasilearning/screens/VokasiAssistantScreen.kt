package com.example.vokasilearning.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vokasilearning.ui.*

/** Fully offline, domain-limited FAQ + arithmetic tutor. No external APIs or data transfer. */
internal object VokasiHelp {
    data class Answer(val text: String, val recognized: Boolean)
    private val topics: List<Pair<List<String>, String>> = listOf(
        listOf("daftar", "registrasi", "akun baru") to
            "Akun hanya bisa dibuat jika nama lengkap, jurusan, kelas, dan angkatan sudah dimasukkan admin. Buka Daftar, pilih kelas dari daftar, lalu isi username dan password. Jika ditolak, hubungi admin sekolah.",
        listOf("masuk", "login", "password", "sandi") to
            "Di halaman Masuk, isi username dan password yang didaftarkan. Jika gagal, cek penulisan dan minta bantuan admin. Jangan bagikan sandi kepada siapa pun.",
        listOf("angkatan", "tingkat", "kelas", "jurusan") to
            "Tingkat X, XI, XII menunjukkan kelas 1, 2, 3 SMK. Angkatan adalah tahun masuk siswa. Jurusan dan kelas ditetapkan admin agar tidak ada kelas fiktif.",
        listOf("waktu", "durasi", "timer", "30 menit") to
            "Latihan wajib VOKASI memiliki target 100 soal dan total waktu 30 menit. Waktu tiap soal mengikuti kode tingkat kesulitan yang ditentukan sistem.",
        listOf("nilai", "skor", "benar", "salah") to
            "Setelah latihan selesai, aplikasi menampilkan skor dan pembahasan jawaban. Admin dapat memeriksa rekap Nilai per kelas dan tanggal.",
        listOf("xp", "streak", "peringkat", "leaderboard") to
            "XP dan streak memberi apresiasi atas latihan konsisten. Peringkat dapat ditinjau sesuai lingkup yang tersedia di halaman Peringkat.",
        listOf("perkalian", "kali") to
            "Perkalian adalah penjumlahan berulang. Misalnya 12 × 3 = 12 + 12 + 12 = 36. Untuk hitungan tertentu, ketik: 12 x 3.",
        listOf("pembagian", "bagi") to
            "Pembagian adalah kebalikan perkalian. Contoh 36 ÷ 3 = 12, karena 12 × 3 = 36. Kamu juga dapat mengetik: 36 / 3.",
        listOf("penjumlahan", "tambah") to
            "Penjumlahan menggabungkan bilangan. Contoh 25 + 17 = 42. Kamu bisa mengetik operasi hitung, misalnya 25 + 17.",
        listOf("pengurangan", "kurang") to
            "Pengurangan mencari selisih. Contoh 25 − 17 = 8. Kamu bisa mengetik operasi hitung, misalnya 25 - 17.",
        listOf("soal", "bank", "materi") to
            "Admin mengelola Bank Soal melalui Tambah Soal atau Impor Excel. Soal wajib memakai empat pilihan A–D, kunci, kode tingkat kesulitan, dan pembahasan.",
        listOf("ekspor", "export", "excel", "impor", "import") to
            "Pada Admin → Soal, pilih satu atau beberapa materi, lalu tekan Ekspor Excel. Ekspor mengambil seluruh hasil filter, bukan hanya 10 soal yang terlihat. Impor menggunakan Template Excel.",
        listOf("admin", "pengaturan", "sekolah", "kelola") to
            "Admin dapat mengelola jurusan, kelas, angkatan, dan daftar nama siswa pada Pengaturan → Data Sekolah. Tanda hijau berarti sudah mendaftar, merah berarti belum. Data masih lokal perangkat.",
        listOf("privasi", "offline", "internet", "api") to
            "Asisten ini berjalan offline, tidak mengakses API atau internet, dan hanya menyediakan bantuan tentang VOKASI. Data sekolah pada versi ini tersimpan lokal, bukan tersinkron antarperangkat."
    )

    fun answer(input: String): Answer {
        val question = input.trim().lowercase()
        if (question.isBlank()) return Answer("Tulis pertanyaan seputar penggunaan VOKASI.", false)
        val arithmetic = Regex("(-?\\d{1,9})\\s*([+\\-×x*/÷])\\s*(-?\\d{1,9})").find(question)
        if (arithmetic != null) {
            val a = arithmetic.groupValues[1].toLongOrNull()
            val b = arithmetic.groupValues[3].toLongOrNull()
            val op = arithmetic.groupValues[2]
            if (a != null && b != null) {
                val result = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "x", "×", "*" -> a * b
                    else -> {
                        if (b == 0L) return Answer("Pembagian dengan nol tidak diperbolehkan.", true)
                        if (a % b == 0L) a / b
                        else return Answer("Hasil " + a + " ÷ " + b + " = " +
                            "%.3f".format(java.util.Locale.US, a.toDouble() / b) +
                            " (dibulatkan tiga desimal).", true)
                    }
                }
                return Answer("Hasil: " + a + " " + op + " " + b + " = " + result + ".", true)
            }
        }
        val matched = topics.firstOrNull { (keywords, _) -> keywords.any { keyword ->
            question.contains(keyword)
        } }
        return if (matched != null) Answer(matched.second, true)
        else Answer(
            "Aku hanya membantu seputar sistem VOKASI dan aritmetika dasar. Coba tanyakan tentang pendaftaran, kelas, nilai, latihan, XP, Bank Soal, impor/ekspor, atau 12 × 3.",
            false
        )
    }
}

@Composable
internal fun VokasiAssistantScreen() {
    val messages = remember { mutableStateListOf(
        "Asisten" to "Halo! Aku Asisten VOKASI offline. Tanyakan penggunaan aplikasi atau latihan hitung dasar. Aku tidak menjawab pertanyaan di luar VOKASI."
    ) }
    var prompt by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Canvas)) {
        AdminHero("Asisten VOKASI", "Panduan numerasi dan fitur • 100% offline", compact = true)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 14.dp)) {
            items(messages) { (who, text) ->
                WhiteCard(Modifier.fillMaxWidth()) {
                    Text(who, color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    Text(text, color = Ink, fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = prompt, onValueChange = { prompt = it.take(250) },
                modifier = Modifier.weight(1f), maxLines = 3,
                placeholder = { Text("Tanya seputar VOKASI…") },
                shape = RoundedCornerShape(14.dp))
            Button(onClick = {
                val question = prompt.trim()
                if (question.isNotBlank()) {
                    messages.add("Kamu" to question)
                    messages.add("Asisten" to VokasiHelp.answer(question).text)
                    prompt = ""
                }
            }, enabled = prompt.isNotBlank()) { Text("Kirim") }
        }
    }
}
