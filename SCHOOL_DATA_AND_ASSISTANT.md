# VOKASI — Alur Pendataan Sekolah, Registrasi, dan Asisten

## Alur data siswa

1. Admin login (saat ini masih kredensial demo untuk pengujian, bukan autentikasi sekolah yang aman).
2. Tekan ikon **Pengaturan** di kanan atas **Ringkasan**, lalu pilih **Manajemen Jurusan, Kelas & Siswa**.
3. Di **Jurusan**, masukkan nama jurusan. Di **Kelas**, pilih jurusan dan tingkat X/XI/XII lalu masukkan kode rombel (contoh `TPM 1`).
4. Di **Siswa**, pilih jurusan, kelas, tahun masuk, dan tambahkan **nama lengkap** dari daftar resmi sekolah. Daftar ini adalah **data calon siswa**, bukan pembuatan akun otomatis.
5. Ketika siswa membuka **Daftar**, pilihan jurusan/kelas/angkatan diambil dari data siswa yang **Belum Terdaftar**. Siswa mengetik nama lengkap yang harus cocok dengan daftar admin, memilih kelas/angkatan, membuat username dan password.
6. Setelah berhasil, status data roster berubah dari **Belum terdaftar (merah)** menjadi **Terdaftar (hijau)**; username terhubung ke baris roster yang benar.

Siswa tidak dapat mendaftar dengan kelas atau jurusan yang tidak tersedia di daftar admin. Nama yang sama dalam satu kelas/angkatan tidak boleh dicatat dua kali. Tahun masuk tercatat di setiap data siswa.

## Penyimpanan dan keterbatasan penting

- **Saat ini prototipe lokal/perangkat**. Data admin dan siswa **tidak dibagikan otomatis antar-HP**. Artinya roster yang dibuat di HP admin **belum bisa digunakan oleh siswa pada HP masing-masing**. Sistem resmi memerlukan backend berotorisasi dan database bersama.
- Data roster dan kredensial hash PBKDF2 disimpan di satu file lokal. Password asli tidak dimasukkan ke file Excel.
- **Akun admin demo yang ada di aplikasi bukan mekanisme otorisasi produksi**. Wajib diganti dengan login admin terpusat, validasi peran, dan kebijakan keamanan sebelum peluncuran resmi.
- Akun demo siswa otomatis sudah dinonaktifkan agar siswa baru harus melalui roster.
- Foto profil dari pendaftaran saat ini tidak disimpan permanen setelah aplikasi ditutup.
- Untuk migrasi ke multi-device, pertahankan data model Jurusan → Kelas (tingkat) → Roster Siswa (angkatan + status + akun), lalu pindahkan operasi klaim akun ke server, dengan verifikasi tambahan agar hanya siswa bersangkutan yang dapat mengklaim nama.

## Arsip data sekolah

Di **Pengaturan Panel** tekan **Unduh Data Sekolah (Excel)**. Berkas `.xlsx` berisi nomor, nama lengkap, jurusan, kelas, angkatan, status dan username **tanpa password**. Log Aktivitas tersedia di menu terpisah, dengan pencarian/filter dan urutan terbaru. Untuk laporan nilai harian gunakan menu **Nilai** atau **Ringkasan** yang sudah tersedia.

## Navigasi

- Admin: Ringkasan, Siswa, Nilai, Soal, Asisten. **Pengaturan** dipindah ke pojok kanan atas Ringkasan. Di Pengaturan terdapat Manajemen Data Sekolah, Log Aktivitas, dan arsip Excel.
- Siswa: Beranda, Peringkat, Asisten, Profil.
- Pertama kali aplikasi dibuka, ada 3 layar pengenalan dengan pesan kebiasaan belajar, numerasi, dan penggunaan identitas yang bertanggung jawab.
- Logo singa–gear diberikan pengguna dan ditampilkan sebagai identitas visual di launcher/auth/onboarding.

## Asisten VOKASI offline

Asisten memiliki basis jawaban lokal untuk navigasi, latihan, nilai, XP, bank soal, impor/ekspor, status pendaftaran, dan operasi aritmetika sederhana. Tidak mengakses API dan tidak mengirim pertanyaan keluar perangkat. **Ini asisten FAQ deterministik, bukan model AI generatif**; tidak dapat menjawab semua pertanyaan bebas dan tidak mengklaim akses data siswa saat menjawab.

## Export Excel Bank Soal

Di **Admin → Soal**, tombol **Ekspor Soal Excel** kini berada tepat di bawah opsi impor/template. Saat ditekan, dialog menampilkan **checkbox** masing-masing materi dan **Semua materi**. Klik **Simpan Excel** untuk mengekspor materi terpilih ke satu file `.xlsx`, termasuk semua halaman dan kunci/pembahasan. Filter daftar soal tetap tersedia untuk meninjau soal; dialog ekspor punya pilihan khusus sehingga tidak harus mengubah daftar terlebih dahulu.
