# VOKASI LEARNING — Petunjuk struktur kode

Aku sederhanakan semua file Kotlin yang sering kamu edit. Setelah package utama, setiap folder hanya **satu tingkat**. Tidak perlu buka `features/student/...` atau `ui/components/...` lagi.

```text
app/src/main/java/com/example/vokasilearning/
├── MainActivity.kt       ← pembuka Android, tidak untuk fitur
├── VokasiApp.kt          ← root + perpindahan halaman
├── Page.kt               ← daftar halaman
├── screens/              ← SEMUA halaman (siswa + admin)
│   ├── LoginScreen.kt
│   ├── RegisterScreen.kt
│   ├── StudentHome.kt
│   ├── QuizScreen.kt
│   ├── ResultsScreens.kt
│   ├── LeaderboardScreen.kt
│   ├── ProfileScreen.kt
│   ├── AdminHome.kt
│   ├── AdminQuestionsScreen.kt
│   ├── AdminAddQuestionScreen.kt
│   └── AdminMonitorScreen.kt
├── logic/                ← aturan sistem
│   ├── QuizSession.kt
│   ├── ProgressRules.kt
│   └── AppConfig.kt
├── model/
│   └── Models.kt         ← bentuk data
├── data/
│   └── DemoData.kt       ← contoh siswa dan soal matematika
└── ui/                   ← komponen & tampilan bersama
    ├── VokasiTheme.kt
    ├── VokasiComponents.kt
    ├── AdaptiveAppFrame.kt
    └── QuizScreenProtection.kt
```

## Edit di mana?

- Beranda siswa: **screens/StudentHome.kt**
- Tampilan soal: **screens/QuizScreen.kt**
- Popup hasil, nilai: **screens/ResultsScreens.kt**
- Login dan pendaftaran: **screens/LoginScreen.kt**, **screens/RegisterScreen.kt**
- XP, streak, level, season: **logic/ProgressRules.kt**
- Timer, soal acak, skor dan XP sesi: **logic/QuizSession.kt**
- Soal demo: **data/DemoData.kt**
- Warna, tema: **ui/VokasiTheme.kt**
- Tombol, kartu, avatar: **ui/VokasiComponents.kt**
- Android entry point: **MainActivity.kt** (cukup memanggil VokasiApp)

Tekan **Shift dua kali** di Android Studio, ketik nama file, lalu Enter.

## Yang tidak diubah

- Kotlin + Jetpack Compose, konfigurasi Gradle/SDK tetap.
- `minSdk=24`: target Android 7.0+.
- Hanya satu sekolah, dua role, dashboard fokus latihan acak.
- XP 20 per sesi valid, batas 10.000 XP per siswa per hari, season tiga bulan.
- `FLAG_SECURE` tetap untuk build rilis; build debug boleh direkam khusus demo.
- Desain visual mengacu Figma; sistem aplikasi tetap mengikuti kesepakatan.

## Verifikasi sebelum merge

Build dan tes login, kuis, timeout, hasil, admin, peringkat di Android Studio. Uji HP kecil dan tablet. **Belum teruji build APK langsung** di sini.

iPhone/iPad butuh target iOS terpisah; kode APK Android tidak langsung berjalan di iOS.
