# VOKASI — impor siswa lewat GitHub Actions

Data untuk 2.378 siswa tahun 2026/2027 telah disiapkan dalam `roster-2026-2027.enc.json` (AES-256-GCM). File ini aman disimpan di repository publik **hanya selama kunci dekripsi disimpan terpisah**. Jangan unggah data Excel asli atau kunci ke repository.

## Persiapan

1. Simpan workflow `import-vokasi-students.yml` di `.github/workflows/` pada branch `main`.
2. Settings → Secrets and variables → Actions → New repository secret. Buat `VOKASI_ADMIN_EMAIL` (email admin aktif), `VOKASI_ADMIN_PASSWORD` (password akun admin), dan `VOKASI_IMPORT_AES_KEY` (kunci hex 64 karakter yang dibagikan terpisah).
3. Pastikan Edge Function Supabase `provision-students` dan secret `VOKASI_INITIAL_STUDENT_PASSWORD` masih aktif.

## Jalankan

Actions → **VOKASI - Impor 2378 Siswa** → **Run workflow**. Pertama pilih `preview` untuk validasi (tanpa menulis data). Setelah berhasil, pilih `import`, `resume=false`, isi confirmation `IMPORT_2378_VOKASI`.

Jika proses terhenti sebagian, selesaikan error lalu jalankan kembali dengan `resume=true`. Jangan hapus admin atau master jurusan/kelas. Setelah berhasil, periksa total siswa `student_roster` harus 2378.

Workflow mengirim per batch 25 siswa untuk mengurangi risiko timeout. Angka 25 adalah ukuran batch, bukan batas total. Dua belas siswa tanpa rombel dari file sumber ditunda sampai tervalidasi.
