# Impor soal Excel — VOKASI

Menu: **Admin → Soal → Template Excel** untuk menyimpan template `.xlsx`, lalu **Impor Excel** untuk memilih berkas yang telah diisi.

## Format wajib: sheet pertama, baris 1

| Kolom | Header | Isi |
|---|---|---|
| A | Materi | Perkalian / Pembagian / Penjumlahan / Konversi Satuan |
| B | Kode | Perkalian: K1–K5; Pembagian: P1–P5; Penjumlahan: J; Konversi Satuan: S1/S2 |
| C | Pertanyaan | Teks soal lengkap |
| D–G | A, B, C, D | Empat opsi jawaban berbeda |
| H | Kunci | Huruf A, B, C, atau D, sesuai opsi jawaban benar |
| I | Pembahasan | Penjelasan jawaban benar (wajib) |

**Contoh baris 2**: `Perkalian | K1 | 12 × 3 = ...? | 30 | 36 | 38 | 39 | B | 12 dikali 3 adalah 36.`

## Cara pakai

1. Simpan template dari aplikasi dan isi soal mulai **baris 2**. Jangan ubah nama kolom.
2. Simpan sebagai **Excel Workbook (.xlsx)**, bukan `.xls`, `.csv`, atau file dengan rumus pada sel.
3. Buka menu **Admin → Soal → Impor Excel** dan pilih berkas tersebut.
4. Aplikasi memeriksa seluruh soal. Jika ada yang salah, ditampilkan nomor baris dan alasan kesalahan. **Tidak ada soal yang dimasukkan** sampai semua baris valid.
5. Setelah valid, aplikasi otomatis membuat ID baru, menerapkan durasi sesuai kode kesulitan, dan menyimpan soal sebagai **soal tambahan lokal**. Soal bawaan tidak ditimpa.

**Aturan**: maksimal 1.000 soal per impor, data Excel terdekompresi maksimal 12 MB, kode kesulitan harus sesuai materi, pilihan A–D harus berbeda, semua kunci dan pembahasan harus ada, dan teks soal tidak boleh duplikat (termasuk dibandingkan dengan soal bawaan). Nomor ID **tidak perlu** dicantumkan di Excel.

**Batasan**: penyimpanan saat ini hanya pada perangkat admin yang melakukan impor, belum menyinkronkan bank soal antarperangkat atau menggunakan backend sekolah. Tidak ada impor parsial dan tidak mengubah kuota 100 soal latihan wajib yang berlaku.

**Catatan:** kategori materi mengikuti versi aplikasi yang ada di repository saat ini. Penambahan kategori Pengurangan perlu perubahan tersendiri pada aturan bank soal dan pengaturan sesi.
