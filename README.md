# LockSite

Aplikasi Android untuk memblokir domain pilihan sendiri secara **permanen dari sisi UI** —
tanpa tombol hapus atau unblock untuk domain yang sudah dikunci.

Blokir dilakukan dengan **filter DNS lokal** melalui `VpnService`. Semua aturan disimpan
di perangkat: tidak ada akun, tidak ada cloud, tidak ada analytics, tidak ada riwayat
browsing yang dikirim ke mana pun.

> **Status: M5 (beta pribadi).** Filter DNS terhubung ke daftar domain, status berasal dari
> layanan nyata, layar Diagnostik tersedia, dan aplikasi yang salah melaporkan "offline"
> (mis. GitHub Mobile) sudah dikecualikan dari tunnel. Sejak M5 dashboard menampilkan cara
> menghentikan perlindungan **sebelum** diaktifkan. Yang sudah terverifikasi di perangkat
> nyata baru pemasangan dan perilaku GitHub Mobile pada satu HP (OPPO CPH2217, Android 13);
> **uji pemblokiran menyeluruh di jaringan nyata belum dijalankan.** Daftar batasan lengkap:
> `docs/KNOWN_LIMITATIONS.md`.

## Batasan yang perlu dipahami

- "Permanen" berarti domain tidak bisa dihapus lewat UI aplikasi. Pemilik perangkat tetap
  dapat menonaktifkan VPN, force-stop, atau mencopot aplikasi dari pengaturan Android.
- DNS filtering tidak menangkap semua jalur: DNS-over-HTTPS/DoT, proxy, VPN lain, cache
  DNS, dan akses langsung via IP dapat menembusnya.
- Aplikasi ini **tidak** menjamin blokir 100% tidak bisa dilewati.
- Setelah perangkat restart atau proses aplikasi dihentikan paksa, perlindungan tidak
  menyala sendiri; status akan jujur menunjukkan "Belum aktif" sampai diaktifkan lagi.
  Lihat `docs/RECOVERY.md`.
- Aplikasi yang menolak VPN DNS-only (mis. GitHub Mobile) dikecualikan dari tunnel agar tetap
  online; DNS aplikasi itu tidak ikut disaring. Domain yang diblokir tetap diblokir di
  aplikasi lain dan browser.

## Persyaratan

- JDK 17 atau lebih baru (Android Studio membawa JBR)
- Android SDK Platform 37, Build-Tools 36.0.0, Platform-Tools
- Perangkat Android fisik (minSdk 26 / Android 8.0) untuk pengujian VPN

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # unit test
./gradlew lintDebug            # Android lint
./gradlew installDebug         # pasang ke perangkat yang terhubung
```

`sdk.dir` pada `local.properties` menunjuk ke Android SDK lokal dan tidak di-commit.

## Struktur

| Path | Isi |
|---|---|
| `PRD.md` | Spesifikasi produk (sumber kebenaran) |
| `AGENTS.md` | Aturan kerja untuk AI coding agent |
| `docs/TECHNICAL_DESIGN.md` | Toolchain, batas modul, desain jaringan, status |
| `docs/SECURITY.md` | Model ancaman, privasi, aturan logging |
| `docs/TEST_PLAN.md` | Rencana unit / perangkat / acceptance test |
| `docs/RECOVERY.md` | Prosedur pemulihan (restart, force-stop, uninstall) |
| `docs/DEPENDENCIES.md` | Audit dependency: versi, lisensi, status |
| `docs/RELEASE_NOTES.md` | Catatan rilis beta |
| `docs/BETA_CHECKLIST.md` | Checklist uji manual beta |
| `docs/DEVICE_NOTES.md` | Catatan perangkat dan versi Android |
| `docs/KNOWN_LIMITATIONS.md` | Daftar batasan yang belum ditangani |
| `docs/ADR/` | Keputusan arsitektur |
| `app/` | Modul aplikasi Android |

## Milestone

M0 fondasi → M1 UI + blocklist → M2 spike VPN/DNS → M3 integrasi filter →
M4 ketahanan + pemulihan → **M5 beta pribadi** → M6 opsional hardening.
Detail di `PRD.md` bagian 14.

M0–M5 selesai pada sisi implementasi dan dokumentasi. M5 menyiapkan paket beta: APK debug,
checklist uji manual, catatan perangkat, daftar batasan, dan catatan rilis. Uji pemblokiran
menyeluruh di jaringan nyata (D-01…D-10, I-01…I-09) masih terbuka; yang sudah terverifikasi
hanya pemasangan dan perilaku GitHub Mobile di satu perangkat (OPPO CPH2217, Android 13).
