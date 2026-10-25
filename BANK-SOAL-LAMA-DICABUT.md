# Penarikan bank soal bawaan Android

Pada 26 Oktober 2026 pemilik meminta penghapusan seluruh **500 soal lama** agar tidak tercampur dengan 500 soal baru dari dokumen Word.

- Asset `app/src/main/assets/vokasi_bank_500.json` dihapus dari repo.
- `QuestionBankLoader` tidak melakukan fallback ke bank lama.
- Latihan wajib, latihan mandiri, dan pemulihan sesi lama diblokir sampai bank baru bergambar divalidasi dan dipasang.
- Penyimpanan lokal ekstra di perangkat tidak dimuat sebagai bank resmi.
- **Tidak ada** nilai, akun, data sekolah, atau riwayat Supabase yang dihapus.
- APK yang sudah terinstal tetap membawa bank lama sampai diperbarui dengan build APK terbaru; mengubah kode GitHub tidak bisa menarik asset APK yang telah terpasang.
- Format baru memerlukan dukungan gambar soal, 3 opsi, dan blueprint 100 soal / 30 menit. Perubahan ini belum mengaktifkannya.

Jangan memulihkan berkas soal lama atau memakai ulang soal lama dari riwayat Git.
