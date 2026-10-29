# LockSite

Aplikasi open source Android untuk memblokir domain pilihan sendiri secara **permanen dari sisi UI** —
tanpa tombol hapus atau unblock untuk domain yang sudah dikunci.

Blokir dilakukan dengan **filter DNS lokal** melalui `VpnService`. Semua aturan disimpan
di perangkat: tidak ada akun, tidak ada cloud, tidak ada analytics, tidak ada riwayat
browsing yang dikirim ke mana pun.

> **Status: M6 — fitur MVP selesai (beta).** Filter DNS terhubung ke daftar domain, status
> berasal dari layanan nyata, layar Diagnostik tersedia, aplikasi yang salah melaporkan
> "offline" (mis. GitHub Mobile) dikecualikan dari tunnel, dashboard menampilkan cara
> menghentikan perlindungan sebelum diaktifkan, dan nama domain terkunci bisa disamarkan dari
> Pengaturan. Managed-device hardening (M6) dievaluasi dan **sengaja tidak diaktifkan**
> (lihat `docs/ADR/0003`). Di perangkat nyata sudah terverifikasi: pemasangan, tampilan,
> penyamaran nama, penolakan Private DNS ketat, perilaku GitHub Mobile, dan **pemblokiran DNS
> inti (D-01…D-04)** — nama terkunci beserta subdomainnya gagal resolve, nama lain tetap
> resolve. **Belum diuji:** reboot, perpindahan Wi-Fi ↔ seluler, force-stop, konflik VPN, dan
> DB rusak. Daftar batasan: `docs/KNOWN_LIMITATIONS.md`.

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
- **Private DNS harus Off atau Otomatis.** Jika Private DNS diatur ke hostname (DNS-over-TLS
  ketat), resolver tidak mau jatuh ke plaintext dan semua nama gagal. Aplikasi mendeteksi ini
  dan menolak aktif dengan penjelasan, bukan merusak DNS sambil mengaku aktif.

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
| `docs/RELEASE_NOTES.md` | Catatan rilis |
| `docs/KNOWN_LIMITATIONS.md` | Daftar batasan yang belum ditangani |
| `docs/ADR/` | Keputusan arsitektur |
| `app/` | Modul aplikasi Android |

## Milestone

M0 fondasi → M1 UI + blocklist → M2 spike VPN/DNS → M3 integrasi filter →
M4 ketahanan + pemulihan → M5 beta pribadi → **M6 evaluasi managed device**.
Detail di `PRD.md` bagian 14.

M0–M6 selesai pada sisi implementasi dan dokumentasi. M6 dievaluasi dan sengaja tidak
diaktifkan (`docs/ADR/0003`). Uji pemblokiran menyeluruh di jaringan nyata
(D-01…D-10, I-01…I-09) masih terbuka; yang sudah terverifikasi hanya pemasangan, tampilan,
dan perilaku GitHub Mobile di satu perangkat Android 13.

## Lisensi

Dirilis di bawah [MIT License](LICENSE). MIT **gratis** dan boleh dipakai untuk proyek pribadi
maupun komersial, dengan syarat menyertakan pemberitahuan hak cipta dan izin lisensi ini di
salinan perangkat lunak. Perangkat lunak disediakan "apa adanya", tanpa jaminan, dan penulis
tidak bertanggung jawab atas kerugian yang timbul dari penggunaannya.
