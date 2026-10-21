# Impor Data Sekolah VOKASI — Excel

Buka **Admin → Sekolah → Impor Data Sekolah**. Fitur ini berbeda dengan **Impor Kelas** (hanya kelas) dan **Unduh Data Siswa** (arsip).

## Satu template untuk semua data

1. Tekan **Unduh Template Data Sekolah**.
2. Isi sheet **Data Sekolah**, mulai baris 2, dengan lima kolom berikut:

| A — Jurusan | B — Tingkat | C — Kelas | D — Angkatan | E — Nama Lengkap |
|---|---|---|---|---|
| Teknik Pemesinan | X | TPM 1 | 2026 | Budi Santoso |
| Teknik Pemesinan | X | TPM 1 | 2026 | Rina Maharani |
| Teknik Pemesinan | XI | TPM 2 | 2025 | Dimas Pratama |
| Akuntansi | XII | AK 1 | 2024 | Fitria Lestari |
| Perhotelan | | | | |

- **Jurusan saja:** isi A, kosongkan B–E.
- **Jurusan dan kelas:** isi A–C, kosongkan D–E.
- **Daftar siswa:** isi A–E. Tingkat hanya X, XI, XII. Kolom Kelas boleh berisi `TPM 1` atau `X TPM 1`; sistem menormalkan awalan tingkat. Angkatan berarti **tahun masuk sekolah**, bukan nomor kelas atau tahun kelulusan.
- Simpan sebagai `.xlsx`, bukan `.xls` atau `.csv`. Gunakan nama lengkap minimal dua kata.

3. Tekan **Impor Data Sekolah (Excel)**, pilih file.
4. Aplikasi memeriksa semua baris. Jika ada kesalahan, nomor baris dan alasannya ditampilkan; **tidak ada data yang berubah**.
5. Jika valid, dialog menampilkan jumlah **jurusan baru, kelas baru, siswa baru, dan siswa duplikat yang dilewati**. Tekan **Simpan Data** untuk menyimpan seluruhnya sekaligus.

**Perlindungan:** data lama tidak dihapus, kelas/jurusan yang sudah ada dipakai kembali, dan siswa yang sudah tercatat akan diperbarui **kelas aktifnya** bila berbeda. Akun yang sudah terdaftar, username, password hash, ID roster, dan riwayat nilai tetap dipertahankan. Siswa baru berstatus **Belum Terdaftar** dan belum mempunyai username atau password; ia membuat akun sendiri dari halaman Daftar.

Maksimal 10.000 baris, sekitar 15 MB data terurai. Impor dimulai dari sheet pertama `xl/worksheets/sheet1.xml`.

**Batasan penggunaan sekolah:** data sekarang masih tersimpan pada **perangkat yang mengimpor file**. Belum tersinkron ke HP siswa atau ke admin lain. Pemakaian sekolah resmi membutuhkan backend/database bersama, autentikasi admin produksi, dan mekanisme verifikasi identitas siswa sebelum mereka mengklaim nama pada daftar admin.


## Update data sekolah ketika naik kelas

Gunakan **template yang sama** setiap tahun ajaran. Untuk siswa yang sudah ada, isi **Nama Lengkap, Jurusan, dan Angkatan tetap sama**, lalu isi **Tingkat serta Kelas aktif yang baru**. Contoh: Andi Pratama, Pemesinan, angkatan 2026, dari `X TPM 1` menjadi `XI TPM 1`.

Sistem mencocokkan siswa menggunakan **nama lengkap + jurusan + angkatan**, bukan kelas (karena kelas dapat berubah). Jika ditemukan tepat satu siswa:
- Kelas sama → baris dilewati.
- Kelas baru setingkat atau lebih tinggi → data roster diperbarui, bukan membuat siswa ganda. **ID, username, password hash, angkatan, dan akun aktif dipertahankan.**
- Kelas turun tingkat → file ditolak agar admin memeriksanya secara manual.
- Nama yang sama dengan lebih dari satu akun pada jurusan/angkatan itu, atau satu siswa tercantum di dua kelas berbeda dalam satu Excel → **seluruh impor ditolak** untuk menghindari salah identitas.

Laporan validasi memperlihatkan jumlah **jurusan baru, kelas baru, siswa baru, siswa pindah/naik kelas, serta data duplikat yang dilewati**. Hanya setelah menekan **Simpan Data**, seluruh perubahan masuk sekaligus. Riwayat nilai yang sudah memiliki snapshot kelas tetap menyimpan kelas pada tanggal latihan.

**Keterbatasan pencocokan:** nama lengkap + jurusan + angkatan belum setara dengan NIS/NISN unik. Jika dua siswa memiliki nama yang sama, perlu verifikasi admin/identitas unik sebelum memakai impor otomatis untuk memindahkan kelasnya.

