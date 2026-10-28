# LockSite

Aplikasi Android untuk memblokir domain pilihan sendiri secara **permanen dari sisi UI** —
tanpa tombol hapus atau unblock untuk domain yang sudah dikunci.

Blokir dilakukan dengan **filter DNS lokal** melalui `VpnService`. Semua aturan disimpan
di perangkat: tidak ada akun, tidak ada cloud, tidak ada analytics, tidak ada riwayat
browsing yang dikirim ke mana pun.

> **Status: M1 (UI + blocklist lokal).** Onboarding, dashboard, tambah domain, daftar domain
> terkunci, dan penyimpanan lokal sudah ada. Filter VPN/DNS **belum ada**, jadi status
> perlindungan masih ditampilkan "Belum aktif" secara jujur — belum ada klaim pemblokiran.

## Batasan yang perlu dipahami

- "Permanen" berarti domain tidak bisa dihapus lewat UI aplikasi. Pemilik perangkat tetap
  dapat menonaktifkan VPN, force-stop, atau mencopot aplikasi dari pengaturan Android.
- DNS filtering tidak menangkap semua jalur: DNS-over-HTTPS/DoT, proxy, VPN lain, cache
  DNS, dan akses langsung via IP dapat menembusnya.
- Aplikasi ini **tidak** menjamin blokir 100% tidak bisa dilewati.

## Persyaratan

- JDK 17 atau lebih baru (Android Studio membawa JBR)
- Android SDK Platform 37, Build-Tools 36.0.0, Platform-Tools
- Perangkat Android fisik (minSdk 26 / Android 8.0) untuk pengujian VPN

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # unit test
./gradlew installDebug         # pasang ke perangkat yang terhubung
```

`sdk.dir` pada `local.properties` menunjuk ke Android SDK lokal dan tidak di-commit.

## Struktur

| Path | Isi |
|---|---|
| `PRD.md` | Spesifikasi produk (sumber kebenaran) |
| `AGENTS.md` | Aturan kerja untuk AI coding agent |
| `docs/TECHNICAL_DESIGN.md` | Toolchain, batas modul, rencana jaringan |
| `docs/SECURITY.md` | Model ancaman, privasi, aturan logging |
| `docs/TEST_PLAN.md` | Rencana unit / instrumentasi / acceptance test |
| `docs/ADR/` | Keputusan arsitektur |
| `app/` | Modul aplikasi Android |

## Milestone

M0 fondasi → M1 UI + blocklist → M2 spike VPN/DNS → M3 integrasi filter → M4 ketahanan →
M5 beta pribadi. Detail di `PRD.md` bagian 14.
