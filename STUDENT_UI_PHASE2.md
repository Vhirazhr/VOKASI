# Tahap 2 — Konsistensi UI Siswa (VOKASI)

## Tujuan
Menyamakan tiga halaman utama **Beranda, Papan Peringkat, Profil** mengikuti gaya beranda yang sudah ada (header navy, latar abu terang, kartu putih rounded, tipografi tegas). Perubahan UI tidak boleh mengubah aturan skor, XP, login, atau kuis.

## Peta file

| Area | Lokasi |
|---|---|
| Beranda / anchor visual | `screens/StudentHome.kt` |
| Papan peringkat | `screens/LeaderboardScreen.kt` |
| Profil | `screens/ProfileScreen.kt` |
| Header & judul seksi reusable (baru) | `ui/StudentScreenHeader.kt` |
| Warna, tipografi dasar | `ui/VokasiTheme.kt` |
| Kartu, avatar, tombol, bottom nav | `ui/VokasiComponents.kt` |
| Routing layar & bottom navigation | `VokasiApp.kt` |
| Perhitungan XP, streak, level, sorting | `logic/ProgressRules.kt` |

## Sudah dikerjakan di branch ini

- **Beranda** tetap sebagai acuan visual; fitur kuis tidak diubah.
- **Ranking**: header navy, switch XP Season / Daily Streak, tab Sekolah / Jurusan / Kelas, filter grup, posisi pengguna, podium Top 3, daftar seluruh peserta termasuk Top 3, highlight “Kamu”.
- **Profil**: header navy, identitas, kelas/jurusan, progres level, tiga ringkasan Lifetime XP / Akurasi / Streak Aktif, aktivitas 7 hari terakhir, rangkuman season, riwayat nilai, logout.
- **Komponen bersama**: `StudentScreenHeader` dan `StudentSectionHeading`; `VokasiBottomNav` yang ada tetap dipakai ketiga screen.

## Uji penerimaan sebelum merge

1. Login siswa demo -> buka Beranda -> Peringkat -> Profil -> kembali Beranda.
2. Di Peringkat, alihkan XP / Streak, lalu Sekolah / Jurusan / Kelas dan pilih nilai dropdown; pastikan klasemen mengikuti pilihan dan label “KAMU” muncul bila termasuk filter.
3. Uji daftar dengan 0, 1, 2, atau >= 3 siswa, serta dengan nama panjang.
4. Profil harus menampilkan data kosong dengan rapi, dan sesudah latihan angka XP, akurasi, sesi selesai, dan riwayat nilai mengikuti data.
5. Uji logout serta login kembali; timer, nilai dan proteksi layar soal tidak berubah.
6. Uji layar Android sempit (320 dp), layar normal, landscape, serta tablet.

> Perubahan kode sudah disimpan ke branch. Build Gradle/ujicoba emulator **belum dijalankan** dari lingkungan ini. Ini adalah perubahan UI, bukan implementasi penyimpanan data online.
