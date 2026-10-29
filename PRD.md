# PRD --- LockSite

## Aplikasi Android untuk Pemblokiran Website Permanen

-   **Nama proyek:** LockSite (nama sementara)
-   **Jenis produk:** Aplikasi Android native, offline-first
-   **Versi dokumen:** 1.0
-   **Status:** Spesifikasi awal untuk implementasi
-   **Target implementasi:** Android Studio, Kotlin, Jetpack Compose
-   **Target pengguna awal:** Pemilik satu perangkat Android yang ingin
    memblokir domain pilihan sendiri secara persisten
-   **Prinsip utama:** Domain yang sudah dikunci tidak dapat dihapus
    melalui antarmuka aplikasi

------------------------------------------------------------------------

# 1. Ringkasan Produk

LockSite adalah aplikasi Android yang memungkinkan pemilik perangkat
memasukkan domain website ke daftar blokir yang tidak dapat dihapus
melalui UI aplikasi. Filter jaringan lokal memeriksa permintaan DNS dan
menolak resolusi untuk domain yang diblokir. Aplikasi dirancang agar
aturan tersimpan secara lokal, tetap tersedia setelah aplikasi ditutup
atau perangkat direstart, serta memiliki status layanan dan prosedur
pemulihan yang jelas.

LockSite ditujukan untuk membantu pengguna mengurangi akses impulsif ke
website tertentu. Produk tidak boleh mengklaim bahwa blokir mustahil
dilewati. Android tetap memberi pemilik perangkat jalur untuk mengubah
konfigurasi sistem, menghentikan layanan, mencopot aplikasi, menggunakan
VPN lain, memakai DNS terenkripsi, proxy, atau mengakses alamat IP
secara langsung.

## 1.1 Masalah yang ingin diselesaikan

Pemblokir browser biasa mudah dinonaktifkan dengan mematikan atau
menghapus ekstensi. Pemblokiran berbasis DNS sederhana juga bisa gagal
jika hanya diterapkan pada satu browser atau konfigurasi DNS mudah
diganti. Pengguna membutuhkan aplikasi yang: - Menyimpan daftar domain
pilihan secara persisten. - Tidak menawarkan tombol hapus atau buka
blokir untuk domain yang sudah dikunci. - Menerapkan filter di tingkat
jaringan perangkat melalui VPN lokal. - Menampilkan status perlindungan
secara jujur. - Menghindari kehilangan akses internet secara tidak
terduga. - Tidak mengirim daftar domain pengguna ke server eksternal.

## 1.2 Visi produk

Menyediakan alat fokus digital yang sederhana, transparan, lokal, dan
sulit dibatalkan secara impulsif, tanpa berpura-pura memiliki kendali
absolut atas Android.

## 1.3 Definisi "permanen"

Dalam produk ini, "permanen" berarti: 1. Domain yang berhasil dikunci
tidak dapat dihapus melalui UI normal. 2. Tidak ada tombol "Unblock",
"Remove", atau fitur edit untuk domain yang terkunci. 3. Aturan tetap
tersimpan setelah aplikasi ditutup dan perangkat direstart. 4. Aplikasi
tidak menyediakan penghitung waktu kedaluwarsa untuk blokir. 5. Mengubah
atau memulihkan aturan membutuhkan prosedur di luar UI normal aplikasi.

Istilah tersebut **tidak** berarti aturan mustahil diubah oleh pemilik
perangkat yang memiliki akses sistem penuh. Penghapusan aplikasi, reset
perangkat, perubahan jaringan, atau teknik bypass tertentu tetap mungkin
terjadi.

------------------------------------------------------------------------

# 2. Tujuan, Bukan Tujuan, dan Kriteria Keberhasilan

## 2.1 Tujuan

-   Memungkinkan pengguna menambahkan domain ke blocklist.
-   Memblokir domain melalui filter DNS lokal yang berjalan menggunakan
    Android `VpnService`.
-   Menyimpan aturan secara lokal.
-   Tidak menyediakan tindakan untuk menghapus domain yang sudah
    dikunci.
-   Menyediakan status VPN/filter, jumlah domain terkunci, dan indikator
    masalah.
-   Bertahan terhadap penutupan UI dan restart perangkat sejauh yang
    diizinkan Android.
-   Meminimalkan risiko kehilangan seluruh koneksi internet akibat
    kesalahan aplikasi.
-   Menyediakan dokumentasi teknis dan pengujian yang bisa dijalankan
    oleh AI coding agent.

## 2.2 Bukan tujuan MVP

-   Menjamin blokir 100% tidak dapat dilewati.
-   Memblokir seluruh aplikasi atau seluruh konten internet.
-   Memantau riwayat browsing pengguna.
-   Mengumpulkan data penggunaan ke cloud.
-   Membuat VPN komersial atau merutekan semua trafik ke server jarak
    jauh.
-   Menyaring isi halaman, kata kunci, gambar, atau video.
-   Menyediakan akun, login, sinkronisasi cloud, atau langganan.
-   Mengelola perangkat orang lain secara diam-diam.
-   Mengubah atau merusak pengaturan keamanan Android.

## 2.3 Kriteria keberhasilan MVP

MVP dinyatakan layak jika: - Pengguna dapat menambahkan domain valid. -
Input yang invalid ditolak dengan pesan yang jelas. - Domain yang
terkunci tidak memiliki aksi hapus/edit di UI. - Daftar tetap ada
setelah aplikasi ditutup dan perangkat direstart. - Saat VPN aktif,
permintaan DNS untuk domain yang diblokir ditolak. - Domain yang tidak
diblokir tetap dapat diakses melalui jalur DNS yang didukung. - UI
membedakan status aktif, belum diaktifkan, dan mengalami gangguan. -
Aplikasi tidak mengirim daftar domain ke server eksternal. - Unit test
dan tes integrasi inti lulus. - Dokumentasi menjelaskan keterbatasan DNS
filtering dan cara pemulihan.

------------------------------------------------------------------------

# 3. Target Pengguna dan Skenario

## 3.1 Persona utama

Pengguna Android yang ingin mengurangi godaan membuka domain tertentu,
misalnya situs hiburan atau media sosial, dan bersedia menambahkan
domain secara manual. Pengguna memahami bahwa aplikasi membutuhkan izin
VPN lokal.

## 3.2 Skenario utama

1.  Pengguna membuka LockSite.
2.  Aplikasi menjelaskan cara kerja, izin VPN, dan keterbatasannya.
3.  Pengguna menambahkan domain, misalnya `example.com`.
4.  Aplikasi menampilkan cakupan aturan: domain persis saja atau domain
    beserta subdomain.
5.  Pengguna mengonfirmasi bahwa domain akan dikunci dan tidak dapat
    dihapus melalui UI normal.
6.  Aplikasi menyimpan aturan.
7.  Pengguna mengaktifkan perlindungan dan menyetujui dialog VPN
    Android.
8.  Aplikasi menampilkan status "Perlindungan aktif".
9.  Permintaan DNS ke domain yang cocok ditolak oleh filter.
10. Setelah restart, pengguna membuka aplikasi dan melihat aturan masih
    tersimpan; status layanan diperiksa kembali.

## 3.3 Skenario kegagalan

-   Pengguna menolak izin VPN.
-   Layanan VPN berhenti.
-   Domain tidak valid atau berupa URL lengkap dengan path.
-   Database tidak dapat dibaca.
-   Perangkat berganti Wi-Fi ke data seluler.
-   DNS-over-HTTPS atau proxy membuat permintaan tidak melewati filter
    DNS.
-   Domain dibuka langsung melalui alamat IP.
-   Pengguna mengaktifkan aplikasi VPN lain.
-   Android membatasi proses latar belakang.
-   Filter gagal meneruskan DNS untuk domain yang diizinkan.

Semua skenario tersebut harus menghasilkan status atau log diagnostik
yang tidak mengandung riwayat browsing sensitif.

------------------------------------------------------------------------

# 4. Platform dan Batasan Teknis

## 4.1 Platform

-   Android native.
-   Bahasa utama: Kotlin.
-   UI: Jetpack Compose.
-   Arsitektur UI: ViewModel + StateFlow.
-   Penyimpanan: Room atau DataStore; pilih satu pendekatan yang sesuai
    dengan kebutuhan transaksi dan migrasi.
-   Filter jaringan: Android `VpnService`.
-   Dependency injection: opsional untuk MVP; jangan menambah
    kompleksitas tanpa manfaat.
-   Build: Gradle Kotlin DSL.
-   Pengujian: JUnit, coroutine test jika diperlukan, dan Android
    instrumentation tests.

Versi `minSdk` dan `targetSdk` harus diputuskan berdasarkan perangkat
uji dan persyaratan Android/Google Play terkini. Jangan menebak nilai
versi SDK; periksa dokumentasi resmi saat proyek dibuat.

## 4.2 Batasan Android yang wajib dipahami

1.  `VpnService` memerlukan persetujuan pengguna melalui alur sistem.
2.  Hanya satu VPN aktif per profil pengguna pada satu waktu.
3.  `VpnService` tidak otomatis memblokir domain. Tim harus
    mengimplementasikan pemrosesan paket, DNS, dan forwarding.
4.  DNS filtering tidak menangkap semua jalur akses. DNS-over-HTTPS/DoT,
    proxy, VPN lain, cache DNS, dan akses IP langsung dapat mengurangi
    efektivitas.
5.  Android dapat menghentikan proses atau membatasi pekerjaan latar
    belakang.
6.  Aplikasi biasa tidak dapat menjamin dirinya tidak bisa dihentikan
    atau dicopot oleh pemilik perangkat.
7.  Kebijakan Device Owner / managed device adalah opsi lanjutan yang
    memerlukan provisioning khusus; bukan bagian wajib MVP.
8.  Jika trafik diambil melalui TUN, paket yang tidak diproses dengan
    benar dapat menyebabkan koneksi rusak atau kebocoran trafik.
    Implementasi jaringan harus ditinjau dengan cermat.

## 4.3 Strategi jaringan MVP

MVP menggunakan DNS filtering melalui VPN lokal: - Membuat interface TUN
melalui `VpnService.Builder`. - Mengatur alamat interface dan route yang
diperlukan. - Memproses paket IP dan UDP/TCP yang relevan untuk DNS. -
Membaca query DNS, menormalkan nama domain, dan memeriksa blocklist. -
Untuk domain yang diblokir, mengembalikan respons DNS yang sesuai dengan
desain dan pengujian. - Untuk domain yang diizinkan, meneruskan query ke
resolver yang dikonfigurasi lalu mengembalikan respons. - Menangani
timeout, retry, UDP/TCP DNS, IPv4/IPv6, dan perubahan jaringan. - Tidak
mengklaim seluruh trafik aman atau terfilter sebelum forwarding dan
routing benar-benar diuji.

**Penting:** jangan membuat VPN yang mengambil semua trafik tetapi hanya
memproses DNS lalu membuang paket lain. Ini akan memutus sebagian besar
koneksi internet. Gunakan implementasi TUN/DNS yang teruji atau pustaka
yang diaudit, dan buat rencana pengujian jaringan sebelum mengaktifkan
route luas.

## 4.4 Pilihan implementasi yang harus diputuskan sebelum coding

Lakukan technical spike untuk membandingkan: - Implementasi parser paket
sendiri dengan cakupan yang terbatas; atau - Menggunakan pustaka
open-source yang sesuai untuk local DNS filtering / VPN TUN.

Jika memilih pustaka pihak ketiga: - Periksa lisensi, aktivitas
pemeliharaan, isu keamanan, dan kompatibilitas Android. - Pin versi
dependency. - Jangan mengimpor kode tanpa audit. - Catat keputusan dalam
`docs/TECHNICAL_DESIGN.md`. - Jika belum ada solusi yang aman untuk
forwarding, jangan mengklaim MVP siap digunakan.

------------------------------------------------------------------------

# 5. Ruang Lingkup Fungsional

## 5.1 Onboarding

**FR-001 --- Penjelasan produk** - Aplikasi menjelaskan bahwa blokir
dilakukan melalui filter DNS lokal. - Aplikasi menjelaskan bahwa domain
yang dikunci tidak bisa dihapus lewat UI normal. - Aplikasi menyebutkan
bahwa perlindungan tidak absolut dan memberikan petunjuk pemulihan.

**FR-002 --- Izin VPN** - Pengguna memulai aktivasi dari tindakan
eksplisit. - Aplikasi menggunakan alur persetujuan resmi Android. - Jika
izin ditolak, status tetap "Belum aktif" dan aplikasi menjelaskan cara
mencoba kembali. - Aplikasi tidak menyamarkan dialog atau mengaktifkan
VPN tanpa persetujuan.

## 5.2 Pengelolaan domain

**FR-010 --- Tambah domain** - Pengguna memasukkan hostname seperti
`example.com`. - MVP tidak menerima URL lengkap, path, query, port, atau
skema `https://`. - Domain dinormalisasi menjadi lowercase dan trailing
dot dihapus. - Validasi menangani spasi, format label DNS, panjang
domain, dan input kosong. - Domain duplikat ditolak atau diberi status
"sudah terkunci".

**FR-011 --- Cakupan domain** - Pilihan default: domain dan semua
subdomain. - UI menjelaskan bahwa `example.com` dapat mencakup
`www.example.com` dan `sub.example.com`. - Jika implementasi MVP hanya
mendukung pencocokan domain/subdomain, jangan tampilkan opsi cakupan
lain yang belum bekerja.

**FR-012 --- Kunci permanen** - Sebelum menyimpan, tampilkan konfirmasi
yang jelas. - Setelah konfirmasi, domain ditambahkan ke daftar
terkunci. - Tidak ada tombol hapus, edit, atau buka blokir di UI
normal. - Pengguna masih dapat menambahkan domain lain.

**FR-013 --- Daftar blokir** - Menampilkan hostname, tanggal
ditambahkan, dan cakupan. - Mendukung pencarian lokal jika jumlah domain
besar. - Tidak menampilkan data riwayat situs yang diakses. - Daftar
kosong menampilkan petunjuk menambahkan domain.

## 5.3 Mesin filter

**FR-020 --- Pemeriksaan domain** - Setiap DNS query yang diproses
dibandingkan dengan aturan domain. - Pencocokan harus berbasis batas
label domain; jangan menggunakan substring naif. - Contoh: aturan
`example.com` harus cocok dengan `example.com` dan `a.example.com`,
tetapi tidak boleh cocok dengan `notexample.com`. - Matching tidak
membedakan huruf besar/kecil.

**FR-021 --- Penolakan DNS** - Query yang cocok dengan blocklist
mendapat respons penolakan yang konsisten. - Pilih dan dokumentasikan
kode respons DNS yang tepat. - Jangan menjawab dengan alamat IP palsu
tanpa alasan teknis yang jelas. - Catat jumlah blokir secara agregat
bila dibutuhkan, tanpa menyimpan hostname permintaan browsing.

**FR-022 --- Domain yang diizinkan** - Query yang tidak cocok diteruskan
ke resolver yang dipilih. - Kegagalan resolver menghasilkan timeout atau
error yang ditangani dengan benar. - DNS TCP, jika diperlukan oleh
ukuran respons atau fallback, harus ditangani atau dinyatakan sebagai
batasan yang teruji.

**FR-023 --- Ketahanan jaringan** - Tangani Wi-Fi, data seluler,
pergantian jaringan, IPv4 dan IPv6 sesuai cakupan yang benar-benar
diimplementasikan. - Ketika layanan gagal, UI harus memperlihatkan bahwa
perlindungan tidak aktif. - Jangan menampilkan "aktif" jika filter tidak
berjalan.

## 5.4 Status dan diagnostik

**FR-030 --- Status layanan** Status minimal: - Belum diaktifkan. -
Menunggu izin VPN. - Perlindungan aktif. - Mengalami gangguan. -
Dihentikan oleh sistem atau pengguna. - Konflik dengan VPN lain.

**FR-031 --- Diagnostik** - Menampilkan status filter dan waktu
pemeriksaan terakhir. - Menyediakan tes internal menggunakan domain
pengujian yang dikontrol pengembang. - Tidak menyimpan riwayat situs
atau payload DNS lengkap. - Pesan error harus membantu tanpa membeberkan
data sensitif.

## 5.5 Persistensi

**FR-040 --- Penyimpanan lokal** - Blocklist disimpan pada penyimpanan
privat aplikasi. - Data tetap ada setelah UI ditutup atau proses
aplikasi direstart. - Migrasi skema database diuji. - Kegagalan membaca
data tidak boleh dianggap sebagai blocklist kosong lalu menampilkan
"aman/aktif".

**FR-041 --- Integritas aturan** - UI tidak menyediakan operasi
delete/update untuk entri terkunci. - Operasi penambahan bersifat
transaksional. - Validasi dilakukan sebelum persistensi. - Enkripsi
database tidak wajib untuk MVP karena domain blokir bukan kredensial,
tetapi data tetap privat dan tidak boleh diekspor tanpa tindakan
pengguna.

------------------------------------------------------------------------

# 6. Aturan Produk untuk "Tidak Ada Tombol Unblock"

-   Setiap domain yang dikonfirmasi masuk status `LOCKED`.
-   Tidak ada fungsi `deleteDomain()` yang dipanggil dari UI produksi.
-   Tidak ada menu konteks untuk menghapus domain.
-   Tidak ada masa kedaluwarsa otomatis.
-   Tidak ada halaman admin tersembunyi atau gesture rahasia untuk
    membuka blokir.
-   Debug/reset tooling hanya tersedia di build pengembangan, bukan
    build rilis.
-   Build pengembangan harus ditandai jelas dan tidak digunakan untuk
    menguji klaim perlindungan rilis.
-   Pengguna harus diberi peringatan sebelum mengunci domain, termasuk
    kemungkinan memblokir login, CDN, atau layanan terkait jika domain
    yang dipilih terlalu luas.

**Batas keamanan:** menyembunyikan tombol bukan kontrol keamanan yang
kuat. Pemilik perangkat dapat menghapus data aplikasi, mencopot
aplikasi, mengubah VPN, atau menggunakan kontrol sistem. Jika
persyaratan berubah menjadi pencegahan pencopotan oleh pemilik
perangkat, evaluasi mode perangkat terkelola secara terpisah.

## 6.1 Pemulihan yang aman

Walaupun UI tidak memiliki tombol unblock, dokumentasi harus menjelaskan
cara memulihkan konektivitas bila terjadi kesalahan: - Membuka
pengaturan VPN Android dan menonaktifkan LockSite. - Menghentikan
aplikasi melalui pengaturan sistem jika service macet. - Menghapus atau
mencopot aplikasi jika pemulihan biasa gagal. - Mengetahui bahwa
penghapusan aplikasi mungkin menghapus konfigurasi lokal. - Jangan
menggunakan mode yang mengunci seluruh trafik sebelum pemulihan diuji.

Pemulihan sistem ini adalah jalur darurat OS, bukan fitur untuk
menghapus domain dari UI LockSite.

------------------------------------------------------------------------

# 7. Struktur UI / UX

## 7.1 Layar

### A. Onboarding

-   Nama dan tujuan aplikasi.
-   Ringkasan cara kerja VPN lokal.
-   Penjelasan "domain yang dikunci tidak dapat dihapus dari UI
    aplikasi".
-   Batasan dan risiko.
-   Tombol "Lanjutkan ke pengaturan perlindungan".

### B. Dashboard

-   Status perlindungan.
-   Jumlah domain terkunci.
-   Tombol utama "Aktifkan perlindungan" atau "Periksa status".
-   Status izin VPN.
-   Indikator gangguan.
-   Shortcut menuju daftar domain.

### C. Tambah domain

-   Field hostname.
-   Contoh input valid.
-   Penjelasan cakupan subdomain.
-   Pesan validasi.
-   Tombol "Kunci domain".
-   Dialog konfirmasi permanen.

### D. Daftar domain terkunci

-   Daftar hostname.
-   Label "Terkunci".
-   Tanggal ditambahkan.
-   Search field jika dibutuhkan.
-   Tidak ada tombol hapus/edit.

### E. Diagnostik

-   Status service.
-   Status VPN Android.
-   Waktu tes terakhir.
-   Hasil tes DNS.
-   Panduan pemulihan.
-   Tidak menampilkan riwayat browsing.

### F. Pengaturan

-   Informasi aplikasi dan versi.
-   Penjelasan privasi.
-   Resolver DNS yang digunakan, jika memang dapat dikonfigurasi dengan
    aman.
-   Panduan pemulihan.
-   Tidak ada toggle untuk menghapus seluruh blocklist di build
    produksi.

## 7.2 Prinsip desain

-   Sederhana, tenang, tidak menghakimi pengguna.
-   Status aktif/gagal harus jelas, bukan hanya dibedakan dengan warna.
-   Gunakan teks dan ikon agar status dapat dipahami pengguna dengan
    gangguan penglihatan warna.
-   Konfirmasi harus menjelaskan konsekuensi sebelum domain dikunci.
-   Tidak ada dark pattern yang menyembunyikan risiko.
-   UI tidak boleh mengklaim "100% tidak bisa dilewati".

------------------------------------------------------------------------

# 8. Arsitektur Teknis

## 8.1 Prinsip arsitektur

-   UI tidak mengakses database atau socket secara langsung.
-   Domain matching adalah modul murni yang bisa diuji tanpa Android.
-   Service VPN bertanggung jawab atas lifecycle dan orkestrasi paket.
-   Packet parser, DNS parser, resolver, dan matcher dipisahkan.
-   Blocklist bersifat sumber kebenaran lokal.
-   Status UI berasal dari status service aktual, bukan hanya nilai
    boolean yang disimpan.
-   Error harus eksplisit; jangan menganggap kegagalan filter sebagai
    status aktif.

## 8.2 Diagram arsitektur tingkat tinggi

``` mermaid
flowchart TD
    U[Pengguna] --> UI[Jetpack Compose UI]
    UI --> VM[ViewModel / StateFlow]
    VM --> UC[Use Cases]
    UC --> REPO[Blocklist Repository]
    REPO --> DB[(Room Database)]
    UC --> STATUS[Protection Status Monitor]
    STATUS --> VPN[Android VpnService]
    VPN --> TUN[TUN Interface / Packet Loop]
    TUN --> IP[IP and UDP/TCP Handling]
    IP --> DNS[DNS Parser]
    DNS --> MATCH[Domain Matcher]
    MATCH -->|Blocked| DENY[DNS Block Response]
    MATCH -->|Allowed| RESOLVER[Configured DNS Resolver]
    RESOLVER --> DNS
    DENY --> DNS
    DNS --> IP
    IP --> TUN
    VPN --> STATUS
    STATUS --> VM
```

## 8.3 Diagram komponen

``` mermaid
flowchart LR
    subgraph Presentation
        Dashboard[Dashboard Screen]
        AddDomain[Add Domain Screen]
        List[Locked Domains Screen]
        Diagnostics[Diagnostics Screen]
        ViewModel[ViewModels]
    end

    subgraph Domain
        AddUseCase[Add Domain Use Case]
        Validate[Domain Validator]
        Matcher[Domain Matcher]
        Models[Domain Models]
        Rules[Block Rules]
    end

    subgraph Data
        Repository[Blocklist Repository]
        DAO[Room DAO]
        Database[(Local Database)]
    end

    subgraph Network
        Service[LockSiteVpnService]
        PacketLoop[TUN Packet Loop]
        DnsParser[DNS Parser]
        Resolver[DNS Forwarder]
        Status[VPN Status Monitor]
    end

    Dashboard --> ViewModel
    AddDomain --> ViewModel
    List --> ViewModel
    Diagnostics --> ViewModel
    ViewModel --> AddUseCase
    AddUseCase --> Validate
    AddUseCase --> Repository
    Repository --> DAO
    DAO --> Database
    Repository --> Rules
    Rules --> Matcher
    Service --> PacketLoop
    PacketLoop --> DnsParser
    DnsParser --> Matcher
    DnsParser --> Resolver
    Service --> Status
    Status --> ViewModel
```

## 8.4 Diagram alur menambahkan domain

``` mermaid
sequenceDiagram
    actor User as Pengguna
    participant UI as Compose UI
    participant VM as ViewModel
    participant Validator as Domain Validator
    participant Repo as Repository
    participant DB as Local Database

    User->>UI: Memasukkan hostname
    UI->>VM: Submit hostname
    VM->>Validator: Validasi dan normalisasi
    alt Input tidak valid
        Validator-->>VM: Error validasi
        VM-->>UI: Tampilkan pesan error
    else Input valid
        Validator-->>VM: Normalized domain
        VM->>Repo: Tambah aturan terkunci
        Repo->>DB: Insert transaksi
        DB-->>Repo: Berhasil / duplikat
        Repo-->>VM: Hasil operasi
        VM-->>UI: Perbarui daftar
        UI-->>User: Konfirmasi domain terkunci
    end
```

## 8.5 Diagram alur DNS

``` mermaid
flowchart TD
    A[DNS packet diterima melalui TUN] --> B{Paket dapat diproses?}
    B -->|Tidak| C[Catat error teknis tanpa payload sensitif]
    B -->|Ya| D[Parse header IP dan UDP/TCP]
    D --> E{DNS query valid?}
    E -->|Tidak| F[Tangani sesuai kebijakan protokol]
    E -->|Ya| G[Normalisasi QNAME]
    G --> H[Domain Matcher]
    H --> I{Domain cocok dengan blocklist?}
    I -->|Ya| J[Buat respons DNS penolakan]
    I -->|Tidak| K[Forward ke resolver]
    K --> L{Resolver berhasil?}
    L -->|Ya| M[Validasi dan teruskan respons]
    L -->|Tidak| N[Timeout / SERVFAIL sesuai desain]
    J --> O[Kirim respons kembali melalui TUN]
    M --> O
    N --> O
```

## 8.6 Diagram lifecycle service

``` mermaid
stateDiagram-v2
    [*] --> NotConfigured
    NotConfigured --> PermissionPending: Pengguna mengaktifkan
    PermissionPending --> Starting: Izin diberikan
    PermissionPending --> NotConfigured: Izin ditolak
    Starting --> Active: TUN dan DNS siap
    Starting --> Error: Inisialisasi gagal
    Active --> Reconnecting: Jaringan berubah
    Reconnecting --> Active: Reconnect berhasil
    Reconnecting --> Error: Reconnect gagal
    Active --> Stopped: Service berhenti
    Error --> Starting: Pengguna mencoba lagi
    Stopped --> Starting: Aktivasi ulang
```

------------------------------------------------------------------------

# 9. Struktur Folder yang Direkomendasikan

Nama package contoh: `com.example.locksite`. Ganti dengan namespace
final sebelum rilis.

``` text
LockSite/
├── AGENTS.md
├── PRD.md
├── README.md
├── CHANGELOG.md
├── docs/
│   ├── TECHNICAL_DESIGN.md
│   ├── SECURITY.md
│   ├── TEST_PLAN.md
│   ├── ADR/
│   │   ├── 0001-platform-and-stack.md
│   │   └── 0002-dns-filtering-approach.md
│   └── diagrams/
│       ├── system-architecture.mmd
│       ├── add-domain-sequence.mmd
│       ├── dns-flow.mmd
│       └── service-lifecycle.mmd
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/locksite/
│       │   │   ├── LockSiteApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── presentation/
│       │   │   │   ├── navigation/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── adddomain/
│       │   │   │   ├── blocklist/
│       │   │   │   ├── diagnostics/
│       │   │   │   └── settings/
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   ├── validation/
│       │   │   │   ├── matcher/
│       │   │   │   └── usecase/
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   ├── dao/
│       │   │   │   ├── entity/
│       │   │   │   └── repository/
│       │   │   ├── vpn/
│       │   │   │   ├── LockSiteVpnService.kt
│       │   │   │   ├── VpnStatus.kt
│       │   │   │   └── ServiceController.kt
│       │   │   ├── dns/
│       │   │   │   ├── DnsMessageParser.kt
│       │   │   │   ├── DnsResponseBuilder.kt
│       │   │   │   ├── DnsForwarder.kt
│       │   │   │   └── DomainMatcher.kt
│       │   │   └── diagnostics/
│       │   └── res/
│       ├── test/
│       │   └── java/com/example/locksite/
│       └── androidTest/
│           └── java/com/example/locksite/
├── gradle/
└── settings.gradle.kts
```

Catatan: - Jangan membuat semua file sekaligus sebelum arsitektur dan
pendekatan jaringan divalidasi. - Struktur folder dapat berubah
berdasarkan hasil technical spike. - Jika memakai pustaka DNS/TUN,
tempatkan adapter di modul network dan dokumentasikan lisensinya. -
`DomainMatcher` sebaiknya berupa komponen domain murni yang tidak
bergantung pada Android framework.

------------------------------------------------------------------------

# 10. Model Data

## 10.1 LockedDomain

Contoh model konseptual:

``` kotlin
data class LockedDomain(
    val id: Long,
    val hostname: String,
    val includeSubdomains: Boolean,
    val createdAtEpochMillis: Long,
    val state: LockState
)

enum class LockState {
    LOCKED
}
```

Model di atas adalah rancangan konseptual; sesuaikan tipe ID dengan
strategi database. Tidak perlu menyimpan field `UNBLOCKED` jika produk
tidak mendukung pembukaan blokir dari UI.

## 10.2 Entitas database

Tabel `locked_domains`: - `id`: primary key. - `hostname_normalized`:
hostname lowercase dan tervalidasi; unique. - `include_subdomains`:
boolean. - `created_at`: timestamp. - `state`: hanya `LOCKED` untuk MVP.

Indeks unik harus mencegah domain duplikat. Tambahkan migrasi skema dan
uji migrasi database.

## 10.3 Pencocokan domain

Pencocokan tidak boleh menggunakan `query.contains(hostname)`.

Aturan untuk `example.com` dengan subdomain: - Cocok: `example.com` -
Cocok: `www.example.com` - Cocok: `a.b.example.com` - Tidak cocok:
`notexample.com` - Tidak cocok: `example.com.attacker.test`

Normalisasi harus menangani huruf besar/kecil, trailing dot, input
Unicode/IDN sesuai kebijakan yang didokumentasikan, dan hostname
invalid. Pilih representasi IDN yang konsisten untuk validasi dan
matching.

------------------------------------------------------------------------

# 11. Keamanan dan Privasi

## 11.1 Prinsip privasi

-   Semua aturan disimpan secara lokal.
-   Tidak ada akun atau telemetry cloud pada MVP.
-   Tidak ada pengumpulan riwayat browsing.
-   Jangan mencatat payload DNS atau hostname yang diakses ke log
    produksi.
-   Log harus menyimpan status service dan error teknis minimum.
-   Tidak ada analytics SDK pada MVP.

## 11.2 Ancaman dan mitigasi

  -----------------------------------------------------------------------
  Ancaman                             Mitigasi / batasan
  ----------------------------------- -----------------------------------
  Salah input domain                  Validasi sebelum menyimpan dan
                                      konfirmasi cakupan

  Database rusak                      Deteksi error, jangan menganggap
                                      daftar kosong sebagai status aman

  Service berhenti                    Status tidak aktif ditampilkan
                                      dengan jelas

  Aplikasi dicopot                    Tidak dapat dicegah sepenuhnya oleh
                                      aplikasi biasa

  DNS-over-HTTPS / VPN lain           Jelaskan batasan; pengelolaan lebih
                                      kuat memerlukan kontrol perangkat
                                      tambahan

  Akses IP langsung                   DNS filter saja tidak cukup

  Domain pihak ketiga ikut terblokir  Konfirmasi domain, jelaskan cakupan
                                      subdomain

  Parser paket berbahaya              Gunakan pustaka teruji, fuzz test,
                                      validasi panjang paket

  DNS response palsu atau malformed   Validasi ID transaksi, header,
                                      ukuran, dan sumber respons

  Resolver tidak tersedia             Timeout dan status error yang jelas

  Aplikasi melaporkan status palsu    Status aktif hanya jika TUN,
                                      parser, dan resolver siap
  -----------------------------------------------------------------------

## 11.3 Prinsip keamanan jaringan

-   Jangan mengirim seluruh trafik ke server eksternal tanpa kebutuhan
    dan penjelasan.
-   Jika menggunakan resolver DNS pihak ketiga, dokumentasikan penyedia
    dan konsekuensi privasinya.
-   Jangan menyimpan kunci atau rahasia yang tidak dibutuhkan.
-   Batasi izin aplikasi.
-   Gunakan `VpnService` sesuai dokumentasi Android.
-   Uji kegagalan parsers dan paket malformed.
-   Lakukan review dependency dan lisensi sebelum mengintegrasikan
    pustaka jaringan.

------------------------------------------------------------------------

# 12. Non-Functional Requirements

**NFR-001 --- Keandalan** - UI tidak crash ketika VPN izin ditolak. -
Database tetap konsisten ketika aplikasi berhenti mendadak. - Kesalahan
DNS tidak menyebabkan service mengklaim perlindungan aktif.

**NFR-002 --- Performa** - Penambahan domain terasa instan untuk
blocklist berukuran normal. - Matching domain harus efisien dan tidak
melakukan query database berat pada setiap paket. - Ukur latensi DNS;
jangan membuat klaim angka performa sebelum pengujian.

**NFR-003 --- Penggunaan baterai** - Tidak melakukan polling agresif. -
Gunakan mekanisme status berbasis event bila memungkinkan. - Uji
konsumsi baterai pada perangkat target.

**NFR-004 --- Kompatibilitas** - Catat versi Android dan perangkat
uji. - Uji Wi-Fi dan data seluler. - Uji IPv4/IPv6 sesuai cakupan. - Uji
konflik dengan VPN lain. - Jangan mengklaim kompatibilitas universal
berdasarkan satu perangkat.

**NFR-005 --- Aksesibilitas** - Label kontrol jelas. - Kontras
memadai. - Status tidak hanya dibedakan dengan warna. - Ukuran teks
mendukung pengaturan sistem.

**NFR-006 --- Maintainability** - Modul UI, domain, storage, VPN, dan
DNS dipisahkan. - Fungsi domain matching mudah diuji. - Keputusan
arsitektur penting ditulis dalam ADR. - Semua milestone disertai tes dan
ringkasan perubahan.

------------------------------------------------------------------------

# 13. Strategi Pengujian

## 13.1 Unit tests

-   Domain valid dan invalid.
-   Normalisasi uppercase/lowercase.
-   Trailing dot.
-   Domain duplikat.
-   Matching exact domain.
-   Matching subdomain.
-   False positive seperti `notexample.com`.
-   Validasi IDN sesuai kebijakan.
-   Transaksi repository.
-   Pemetaan status service.

## 13.2 Tes DNS

Gunakan domain uji yang dikontrol sendiri atau domain test yang aman: -
Domain dalam blocklist ditolak. - Domain di luar blocklist dapat
di-resolve. - Subdomain diblokir sesuai kebijakan. - Domain yang mirip
tetapi berbeda tidak terblokir. - Query malformed tidak membuat service
crash. - Timeout resolver ditangani. - Respons DNS tidak valid
ditolak. - UDP dan TCP DNS diuji sesuai dukungan. - Perubahan jaringan
tidak membuat filter diam-diam berhenti.

## 13.3 Tes integrasi Android

-   Izin VPN diberikan.
-   Izin VPN ditolak.
-   Aktivasi dan penghentian service.
-   Aplikasi ditutup dan dibuka kembali.
-   Restart perangkat.
-   Wi-Fi ke data seluler.
-   VPN lain aktif.
-   Aplikasi force-stop.
-   Database rusak atau migrasi gagal.
-   Pemulihan melalui pengaturan Android.

## 13.4 Uji penerimaan

MVP diterima bila: 1. Domain valid dapat dikunci. 2. Tidak ada tombol
hapus pada daftar domain terkunci. 3. Aturan bertahan setelah restart.
4. DNS untuk domain uji yang diblokir mendapat respons penolakan. 5.
Domain uji yang diizinkan tetap dapat diakses melalui jalur yang
didukung. 6. Status aplikasi benar ketika VPN tidak berjalan. 7.
Prosedur pemulihan diuji dan didokumentasikan. 8. Tidak ada data
browsing yang dikirim ke jaringan eksternal oleh aplikasi. 9. Semua tes
yang relevan tercatat dan hasilnya dapat direproduksi.

------------------------------------------------------------------------

# 14. Milestone Implementasi untuk Vibecoding

Gunakan pola **audit → rencana → implementasi satu milestone → tes →
review → commit**. Jangan meminta agent membuat seluruh aplikasi dalam
satu prompt.

## M0 --- Audit dan fondasi

Deliverables: - `AGENTS.md` - `PRD.md` - `docs/TECHNICAL_DESIGN.md` -
`docs/SECURITY.md` - `docs/TEST_PLAN.md` - Git repository dan baseline
commit - Android project dapat dibangun

Exit criteria: - Build berhasil. - Struktur proyek disepakati. - Belum
ada klaim bahwa filter jaringan sudah berfungsi.

## M1 --- UI dan domain blocklist

Deliverables: - Onboarding. - Dashboard. - Add Domain. - Locked
Domains. - Validasi hostname. - Penyimpanan lokal. - Konfirmasi
permanen. - Tidak ada tombol hapus di UI rilis.

Exit criteria: - Unit tests validasi dan repository lulus. - Aturan
bertahan setelah proses aplikasi direstart. - UI state tidak mengarang
status VPN.

## M2 --- Technical spike VPN/DNS

Deliverables: - Bukti `VpnService` dapat diaktifkan dan dihentikan
dengan benar. - Pemilihan pendekatan TUN/DNS didokumentasikan. - Proof
of concept parsing dan forwarding DNS. - Tes dasar domain yang diblokir
dan diizinkan. - ADR yang menjelaskan dependency dan trade-off.

Exit criteria: - Domain yang diblokir benar-benar ditolak. - Domain lain
tetap dapat diakses. - Tidak ada route luas yang membuang paket yang
tidak didukung. - Jika syarat ini belum tercapai, jangan lanjut ke
hardening.

## M3 --- Integrasi filter

Deliverables: - Repository blocklist terhubung ke domain matcher. - DNS
deny response. - DNS forwarding. - Status service nyata. - Penanganan
error dan jaringan berubah.

Exit criteria: - Tes DNS dan integrasi lulus pada perangkat target. - UI
membedakan aktif dan gagal. - Tidak ada error fatal pada skenario uji
utama.

## M4 --- Ketahanan dan pemulihan

> Status: M0–M3 selesai. M4 implementasi + dokumentasi dikerjakan; uji perangkat belum
> dijalankan sehingga exit criteria M4 belum terpenuhi.

Deliverables: - Uji restart. - Uji Wi-Fi dan seluler. - Uji penghentian
proses. - Dokumentasi pemulihan. - Review privasi dan logging. - Audit
dependency. - **Perbaikan validasi koneksi:** `VpnService.Builder
.setUnderlyingNetworks()` agar aplikasi lain (mis. GitHub Mobile) tidak
melaporkan "offline" saat VPN aktif — lihat ADR 0002. Tanpa ini, VPN DNS-only
membuat Android gagal memvalidasi jaringan, sehingga app pelacak koneksi salah
melaporkan offline padahal internet aman.

Exit criteria: - Perilaku setelah restart terdokumentasi. - Prosedur
pemulihan telah diuji. - Tidak ada klaim anti-bypass absolut.

## M5 --- Beta pribadi

Deliverables: - APK debug/internal release. - Checklist uji manual. -
Catatan perangkat dan versi Android. - Daftar known limitations. -
Release notes.

Exit criteria: - Semua tes penerimaan yang didukung lulus. - Batasan
yang belum ditangani ditulis secara eksplisit. - Pengguna memahami cara
pemulihan sebelum mengaktifkan perlindungan.

## M6 --- Opsional: Managed device hardening

Hanya setelah MVP stabil: - Evaluasi Android Device Owner /
managed-device provisioning. - Tinjau kebijakan Android dan konsekuensi
reset/provisioning. - Uji apakah penghapusan aplikasi dan perubahan VPN
bisa dibatasi pada konfigurasi perangkat yang dikelola. - Dokumentasikan
bahwa konfigurasi ini bukan mode normal untuk semua pengguna.

------------------------------------------------------------------------

# 15. Aturan untuk AI Coding Agent

Agent harus mengikuti aturan berikut:

1.  Baca `AGENTS.md`, `PRD.md`, dan dokumen teknis terkait sebelum
    mengubah kode.
2.  Audit kondisi repository dan jalankan build/test awal sebelum
    implementasi.
3.  Jangan mengasumsikan file atau fungsi sudah ada; periksa repository.
4.  Kerjakan satu milestone dalam satu waktu.
5.  Buat rencana file yang akan diubah sebelum coding.
6.  Jangan mengimplementasikan filter VPN yang hanya tampak berfungsi
    tetapi tidak meneruskan DNS dengan benar.
7.  Jangan menyatakan website berhasil diblokir tanpa tes jaringan nyata
    atau tes integrasi yang sesuai.
8.  Jangan menghapus tombol pemulihan sistem Android atau mencoba
    mengunci pengguna dari perangkatnya sendiri.
9.  Jangan menambahkan telemetry, akun, iklan, atau backend tanpa
    persetujuan eksplisit.
10. Jangan mengubah dependency besar tanpa menjelaskan alasan, lisensi,
    dan trade-off.
11. Jalankan unit test, lint, dan build yang tersedia setelah perubahan.
12. Laporkan tes yang benar-benar dijalankan, hasil, dan tes yang belum
    dilakukan.
13. Jangan melakukan `git reset --hard`, menghapus sejarah Git, atau
    melakukan push tanpa izin eksplisit.
14. Buat commit setelah milestone selesai dan lolos tes, kecuali
    pengguna meminta alur berbeda.
15. Jika ada keputusan teknis yang memengaruhi keamanan atau kemampuan
    pemblokiran, berhenti dan dokumentasikan pilihan sebelum
    melanjutkan.

------------------------------------------------------------------------

# 16. Definisi Selesai (Definition of Done)

Sebuah milestone dianggap selesai hanya jika: - Implementasi sesuai
ruang lingkup milestone. - Tidak ada placeholder yang diklaim sebagai
fungsi nyata. - Build berhasil. - Tes yang relevan dijalankan dan
hasilnya dilaporkan. - Error dan state loading ditangani. - Tidak ada
log sensitif yang tidak diperlukan. - Dokumentasi diperbarui. - Batasan
yang belum terselesaikan dicatat. - Git diff ditinjau. - Commit dibuat
setelah hasil diverifikasi, sesuai aturan repository.

Produk dianggap siap untuk beta pribadi hanya setelah M0--M5 selesai dan
skenario penerimaan diuji pada perangkat Android nyata.

------------------------------------------------------------------------

# 17. Referensi Teknis Resmi

Agent harus memeriksa dokumentasi terkini sebelum implementasi: -
Android `VpnService`:
https://developer.android.com/reference/android/net/VpnService - Panduan
membangun VPN: https://developer.android.com/develop/connectivity/vpn -
Android `DevicePolicyManager`:
https://developer.android.com/reference/android/app/admin/DevicePolicyManager -
Android background execution limits:
https://developer.android.com/about/versions/oreo/background - Kotlin:
https://kotlinlang.org/docs/home.html - Jetpack Compose:
https://developer.android.com/compose - Room:
https://developer.android.com/training/data-storage/room

Jangan mengandalkan PRD ini sebagai pengganti pemeriksaan dokumentasi
terbaru, khususnya untuk target SDK, kebijakan distribusi, permission,
dan API VPN.

------------------------------------------------------------------------

# 18. Prompt Awal untuk DeepSeek Harness

Gunakan prompt berikut di direktori proyek:

> Kamu bertindak sebagai senior Android engineer dan technical lead
> untuk proyek LockSite.
>
> Baca `PRD.md` secara penuh. Jika `AGENTS.md` sudah ada, baca dan
> patuhi juga. Jangan langsung membangun seluruh aplikasi.
>
> Tugas pertama hanya M0: audit lingkungan dan repository, periksa versi
> Java/JDK, Android SDK, Gradle, dan kondisi Git; lalu buat atau
> lengkapi `AGENTS.md`, `docs/TECHNICAL_DESIGN.md`, `docs/SECURITY.md`,
> `docs/TEST_PLAN.md`, serta ADR awal sesuai PRD. Buat scaffold Android
> Kotlin + Jetpack Compose yang bisa dibangun.
>
> Jangan implementasikan VPN/DNS filtering pada M0. Jangan mengklaim
> fungsi jaringan sudah ada. Jangan menambah backend, analytics, akun,
> atau layanan berbayar.
>
> Sebelum mengubah file, berikan ringkasan audit dan rencana perubahan.
> Setelah implementasi, jalankan build dan tes yang tersedia, periksa
> diff, lalu laporkan file yang berubah, perintah yang dijalankan, hasil
> nyata, masalah yang tersisa, dan langkah berikutnya.
>
> Jangan melakukan reset Git destruktif atau push tanpa izin. Berhenti
> setelah M0 selesai dan tunggu persetujuan sebelum M1.

------------------------------------------------------------------------

# 19. Keputusan yang Masih Terbuka

Sebelum M2, putuskan dan dokumentasikan: 1. Versi Android minimum yang
akan didukung. 2. Perangkat Android nyata untuk pengujian. 3. Pustaka
TUN/DNS yang akan dipakai, jika ada. 4. Resolver DNS dan kebijakan
privasinya. 5. Apakah MVP harus mendukung IPv6 dan DNS-over-TCP. 6.
Apakah target hanya penggunaan pribadi atau nantinya distribusi publik.
7. Apakah opsi managed-device hardening diperlukan.

**Rekomendasi akhir:** selesaikan M0 dan M1 dahulu. Risiko teknis
terbesar proyek ini bukan UI atau penyimpanan daftar, melainkan membuat
VPN lokal yang benar-benar memfilter DNS tanpa memutus koneksi lain.
Validasi hal itu pada M2 sebelum memperluas fitur.
