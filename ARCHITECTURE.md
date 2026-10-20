# VOKASI Learning — struktur aplikasi Android

Refaktor dari MainActivity.kt monolitik menjadi paket berdasarkan fungsi tanpa perubahan Gradle, SDK atau framework (tetap Kotlin + Jetpack Compose).

| Folder | Peran |
|---|---|
| MainActivity.kt | Android entry point saja |
| app/VokasiApp.kt | Aplikasi utama dan perpindahan halaman |
| navigation/Page.kt | Definisi halaman |
| domain/Models.kt | Student, Question, Attempt |
| domain/QuizSession.kt | State sesi, acak soal, timer, penghitungan nilai dan XP |
| domain/ProgressRules.kt | Level, season, streak, WIB, ranking |
| data/DemoData.kt | Akun dan bank soal demo |
| features/auth/ | Login & pendaftaran |
| features/student/ | Beranda, kuis, hasil, profil, ranking |
| features/admin/ | Admin dan monitoring |
| ui/components/ | Komponen UI bersama |
| ui/adaptive/ | Layar HP, landscape dan tablet |
| ui/security/ | FLAG_SECURE kuis, pengecualian khusus rekaman debug |
| ui/theme/VokasiPalette.kt | Tema warna VOKASI |

Kompatibilitas saat ini Android 7.0 ke atas (minSdk 24). iPhone/iPad tidak otomatis bisa menjalankan APK; perlu port iOS tersendiri.

## Verifikasi sebelum merge

- Build > Make Project pada Android Studio; kode belum dikompilasi langsung di mesin Android.
- Login dummy siswa dan admin, latihan wajib, timer, popup hasil, XP, leaderboard.
- Lihat rendering di HP kecil dan tablet; ukuran font besar dan rotasi landscape.
- Coba screen record pada debug dan pastikan soal tetap terlindungi pada build release.
- Materi dummy terbatas pada penjumlahan, pengurangan, perkalian dan sedikit pembagian.

Ini masih prototipe: belum ada backend, sinkronisasi, validasi identitas sekolah yang aman, atau penyimpanan akun sungguhan.
