# Variasi Tampilan Kosong — Implementation Plan

> **For agentic workers:** Gunakan skill executing-plans untuk menjalankan langkah secara berurutan. Checklist di bawah belum dikerjakan; dokumen ini adalah usulan implementasi, bukan bukti UI sudah berubah.

**Goal:** Membedakan tampilan kosong berdasarkan pekerjaan pengguna, terutama layar belum memiliki usaha dan belum memiliki catatan yang dilaporkan melalui screenshot.

**Architecture:** Setiap screen memilih `StateArtwork` secara eksplisit pada `EmptyCard`. Komponen `StateIllustration` tetap memiliki playback, tema, fallback dan aksesibilitas. Gunakan aset lokal yang sudah tersedia; tidak perlu mengubah ViewModel, database, kalkulasi, login atau layanan cloud.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, dependency Lottie Compose yang sudah terpasang, Android instrumentation tests.

## Status dan dasar keputusan

- **user-confirmed:** Pengguna meminta tampilan kosong lebih bervariasi dan rencana implementasinya terlebih dahulu.
- **verified:** Ada 11 call site `EmptyCard` di enam file screen. Delapan masih bergantung pada nilai default `StateArtwork.EMPTY`. Ada satu call site tambahan dalam `VisualSystemTest`.
- **verified:** Beranda tanpa usaha/unit dan Catat transaksi tanpa event sama-sama menampilkan ilustrasi kertas. Lottie Business Plan, Reports, Stock, Import dan Cloud Backup sudah dibundel dengan varian terang/gelap.
- **inferred:** Keluhan utama berasal dari pemetaan ilustrasi yang belum mencakup kondisi awal penggunaan; screenshot tersebut tidak membuktikan instalasi APK lama.
- **proposed:** Reuse aset berdasarkan konteks dan wajibkan pemilihan artwork pada seluruh `EmptyCard`.
- **unknown:** Hasil visual dan performa perubahan ini di perangkat pengguna, sampai implementasi dan pemeriksaan perangkat selesai.

## Alternatif dan pilihan

1. **Direkomendasikan: pemetaan kontekstual menggunakan aset yang ada.** Langsung membedakan dua layar pada screenshot, tanpa penambahan ukuran aset atau lisensi. Beberapa kondisi yang sejenis tetap berbagi ilustrasi.
2. Mencari Lottie baru untuk setiap kondisi: variasi lebih banyak, tetapi memerlukan penelitian sumber, lisensi, recoloring, frame statis dan pengujian ulang. Tidak diperlukan untuk menyelesaikan kasus ini.
3. Rotasi/randomisasi ilustrasi: mudah menghasilkan gambar berbeda tetapi mengaburkan makna kondisi layar. Tidak dipilih.

## Kontrak visual yang diusulkan

| Screen / kondisi | Artwork setelah perubahan | Ukuran yang sudah dimiliki komponen | Tindakan |
| --- | --- | --- | --- |
| Beranda: belum ada usaha | BUSINESS_PLAN | 144 dp | Tambah Usaha, bantuan tetap |
| Beranda: belum ada unit | BUSINESS_PLAN | 144 dp | Tambah Unit, bantuan tetap |
| Beranda Proyeksi: belum ada rencana | BUSINESS_PLAN | 144 dp | Buka rencana tetap |
| Kelola usaha: daftar usaha kosong | BUSINESS_PLAN | 144 dp | Kontrol tambah/bantuan yang ada tetap |
| Kelola usaha: daftar unit kosong | BUSINESS_PLAN | 144 dp | Kontrol tambah yang ada tetap |
| Stok: belum ada usaha | BUSINESS_PLAN | 144 dp | Tambah Usaha tetap |
| Rencana: belum ada unit | BUSINESS_PLAN, sudah terpasang | 144 dp | Tambah Unit tetap |
| Catat transaksi: belum ada catatan | EMPTY, dipilih eksplisit | 88 dp | Catat sekarang tetap |
| Catat transaksi: pencarian/filter tanpa hasil | EMPTY, dipilih eksplisit | 88 dp | Pesan ubah pencarian/filter tetap |
| Laporan: belum ada data | REPORTS, sudah terpasang | 88 dp | Catat transaksi/buka rencana tetap |
| Laporan Proyeksi: belum ada rencana kelayakan | BUSINESS_PLAN, mengganti REPORTS | 144 dp | Buka rencana tetap |

Ilustrasi stok kosong `STOCK` (104 dp), introduksi rencana `BUSINESS_PLAN` (160 dp), impor `IMPORT` (64 dp), dan formulir cloud `CLOUD_BACKUP` (104 dp) tetap dipertahankan. Ini adalah pemanggilan langsung `StateIllustration`, di luar 11 kartu di atas.

Variasi mengikuti makna: persiapan usaha/rencana, pencatatan, laporan, stok, impor dan cloud. Kondisi sejenis boleh memakai aset sama. Kertas tetap masuk akal pada daftar catatan dan pencarian catatan; tahap ini tidak menjanjikan satu file unik untuk setiap kondisi.

Tidak mengubah layout utama, copy, CTA, navigasi, warna token, atau syarat unit aktif. Jangan menampilkan ilustrasi cloud pada backup lokal atau grafik pertumbuhan sebagai klaim laba. Full tetap satu kali putar; Reduced/Off tetap pada frame bermakna. Gambar dekoratif tidak menambah ucapan TalkBack.

## Task 1 — Kunci reproduksi kasus pengguna

**Modify:** `app/src/androidTest/java/com/mankeu/presentation/ContextualLottieTest.kt`.

- [ ] Tambahkan pengujian berikut dalam class yang sudah memiliki `compose`, `setContent`, dan fixture bahasa. Gunakan screen asli agar tes menangkap kesalahan pada call site, bukan hanya menguji komponen ilustrasi.

```kotlin
@Test fun firstUseHomeUsesBusinessArtworkAndPreservesSetupAction() {
    var opens = 0
    setContent {
        MankeuTheme(darkTheme = false, motion = MankeuMotion.Off) {
            com.mankeu.presentation.screens.HomeScreen(
                state = AppUiState(isLoading = false),
                onNavigate = {},
                onReverse = { _, _ -> },
                onOpenData = { opens++ },
            )
        }
    }
    compose.onNodeWithTag("artwork-BUSINESS_PLAN").assertIsDisplayed()
    compose.onNodeWithTag("artwork-EMPTY").assertDoesNotExist()
    compose.onNodeWithText(text(R.string.scope_add_business)).performClick()
    compose.runOnIdle { assertEquals(1, opens) }
}
```

- [ ] Bangun APK aplikasi dan test APK, lalu jalankan tes baru pada emulator terisolasi. Sebelum perubahan, harapkan gagal karena `artwork-BUSINESS_PLAN` tidak ditemukan. Jangan menghapus data atau menjalankan fixture pada ponsel pengguna.

## Task 2 — Terapkan pemetaan pada pemilik screen

**Modify:** `HomeScreen.kt`, `ManageScopeScreen.kt`, `StockSection.kt`, `EntryScreen.kt`, `ReportsScreen.kt` dalam `app/src/main/java/com/mankeu/presentation/screens/`.

- [ ] Tambahkan import `com.mankeu.presentation.components.StateArtwork` pada HomeScreen, ManageScopeScreen dan EntryScreen; StockSection/ReportsScreen sudah memilikinya.
- [ ] Pada tiga kartu HomeScreen, dua kartu ManageScopeScreen dan kartu setup di StockSection, tambahkan argumen berikut sesuai tabel. Jangan ubah kondisi `if`, callback, atau data yang memutuskan kapan kartu muncul.

```kotlin
artwork = StateArtwork.BUSINESS_PLAN,
```

- [ ] Pada dua kartu EntryScreen, nyatakan pilihan saat ini secara eksplisit:

```kotlin
artwork = StateArtwork.EMPTY,
```

- [ ] Pada `FeasibilitySection` di ReportsScreen, cabang `planMode && plan == null`, ubah hanya argumen ilustrasi:

```kotlin
artwork = StateArtwork.BUSINESS_PLAN,
```

- [ ] Kartu laporan kosong utama tetap `REPORTS`; PlanScreen tetap `BUSINESS_PLAN`. Jalankan ulang tes Task 1; harapkan PASS dan callback Tambah Usaha tepat satu kali.

## Task 3 — Cegah pengulangan pemetaan default

**Modify:** `app/src/main/java/com/mankeu/presentation/components/SharedComponents.kt`, `app/src/androidTest/java/com/mankeu/presentation/VisualSystemTest.kt`.

- [ ] Pada signature `EmptyCard`, pertahankan posisi argumen terakhir tetapi hapus default artwork:

```kotlin
artwork: StateArtwork,
```

- [ ] Pada tes existing yang memanggil `EmptyCard("Belum ada catatan", ...)`, tambahkan named argument `artwork = StateArtwork.EMPTY`. Jangan mengubah default enum atau playback di `StateIllustration`.
- [ ] Compile main dan androidTest. Compiler menjadi pemeriksaan kelengkapan: semua call site wajib memilih ilustrasi. Tidak perlu registry pemetaan baru, helper domain, atau skrip pemeriksa teks Kotlin.

## Task 4 — Verifikasi alur dan tampilan

**Modify:** `app/src/androidTest/java/com/mankeu/presentation/ContextualLottieTest.kt` hanya jika diperlukan untuk menyimpan bukti screen tambahan; reuse fixture in-memory dan pemulihan preferences yang ada.

- [ ] Jalankan regresi Task 1 serta seluruh `ContextualLottieTest` dan `VisualSystemTest`. Pastikan native render, tema, frame Reduced/Off, lisensi dan callback yang sudah diuji tetap lulus.
- [ ] Pada emulator offline dengan data fiktif, lakukan alur: tanpa usaha → Tambah Usaha → buka Beranda tanpa unit → Tambah Unit → Catat transaksi kosong → buka formulir → batalkan → Laporan → Stok → Proyeksi/rencana. Pastikan ilustrasi berubah sesuai konteks, form benar-benar terbuka, dan tidak ada transaksi yang dibuat hanya karena membuka/membatalkan form.
- [ ] Buka Kelola usaha sebelum dan sesudah membuat usaha untuk memeriksa dua kartu daftar kosong. Periksa filter tanpa hasil memakai satu catatan fiktif pada fixture terpisah; data asli pengguna tidak digunakan.
- [ ] Simpan screenshot kondisi tanpa usaha dan transaksi kosong berdampingan, ditambah unit kosong, rencana, laporan dan stok. Periksa terang/gelap, lebar compact/expanded, dan font 200%: ilustrasi tidak menutup teks/CTA, CTA tetap dapat dicapai, tanpa clipping. Jika layout gagal pada font besar, catat penyebab dan perbaiki minimal sebelum melanjutkan.
- [ ] Periksa TalkBack: judul, isi dan CTA terbaca; ilustrasi tidak menjadi fokus tambahan. Bandingkan Full/Reduced/Off; perpindahan screen tidak membuat beberapa animasi berjalan terus-menerus. Jangan menyatakan performa perangkat rendah sudah terbukti tanpa pengukuran.

Perintah PowerShell dari root repository (gunakan JDK/SDK lokal yang berlaku):

```powershell
rtk proxy python scripts/prepare_lottiefiles.py
$env:JAVA_HOME = 'E:/Android Studio/jbr'
rtk proxy ./gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug --max-workers=1 --console=plain
```

Harapkan asset checker PASS, Gradle `BUILD SUCCESSFUL`, dan lint nol error. Jangan menjalankan build Gradle paralel. Siapkan emulator khusus, ambil serial aktual dari `adb devices`, dan gunakan hanya serial tersebut:

```powershell
$testSerial = 'emulator-5580'
$adbPath = 'E:/Android SDK/platform-tools/adb.exe'
rtk proxy $adbPath -s $testSerial install -r app/build/outputs/apk/debug/app-debug.apk
rtk proxy $adbPath -s $testSerial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
rtk proxy $adbPath -s $testSerial shell am instrument -w -e class com.mankeu.presentation.ContextualLottieTest,com.mankeu.presentation.VisualSystemTest com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
```

Harapkan `OK` tanpa failures. Serial contoh di atas harus cocok dengan emulator terisolasi yang benar-benar tersedia. Unit/domain suite tambahan hanya diperlukan jika implementasi melebar ke perilaku domain; rencana ini tidak mengubahnya.

## Task 5 — Dokumentasikan dan kirim bukti

**Modify:** `docs/UX_AND_ACCESSIBILITY.md`, `logs/ACTIVITY_LOG.md`, `logs/CHANGELOG.md`; tambahkan ADR di `logs/DECISION_LOG.md` untuk kontrak artwork eksplisit jika diterapkan.
**Create:** `docs/audit/2026-10-21_EMPTY_STATE_VARIETY.md` dan screenshot bukti di `docs/design/previews/empty-state-variety/`.

- [ ] Catat tabel final, hasil tes dengan nama/jumlah sebenarnya, screenshot, APK yang dibangun, keterbatasan, dan rollback. Rollback hanya membatalkan pemetaan UI/kontrak argumen; tidak ada migrasi data.
- [ ] Tambahkan changelog hanya setelah UI benar-benar diterapkan. Log aktivitas harus membedakan plan, implemented dan workflow-verified.
- [ ] Jalankan `rtk git diff --check`, periksa status/diff, lalu commit hanya perubahan tugas beserta logs secara atomik. Pertahankan perubahan pihak lain.
- [ ] Serahkan APK dan perbandingan dua kondisi dari screenshot pengguna. Bila pemasangan perangkat diminta, upgrade tanpa uninstall/clear data dan verifikasi build yang dipasang; jangan menyimpulkan versi APK dari ilustrasi saja.

## Acceptance akhir

1. Tanpa usaha menampilkan Business Plan; Catat transaksi kosong menampilkan kertas. Keduanya jelas berbeda pada screenshot perangkat yang sama.
2. Semua 11 kartu memiliki artwork eksplisit dan konsisten dengan tabel; pencarian kosong tidak dianggap kehilangan seluruh data.
3. CTA dan bantuan tetap berfungsi, scope/buku aktif tetap benar, dan membuka layar tidak membuat data finansial.
4. Tema terang/gelap, Full/Reduced/Off, teks 200%, compact/expanded dan TalkBack memiliki bukti pemeriksaan yang jelas.
5. APK/test build, tes native dan lint lulus; keterbatasan perangkat/performa dilaporkan apa adanya.

## Review rencana

**verified:** Rencana dicocokkan dengan call site, enum, signature dan tes yang ada pada commit `0d500d9`. **proposed:** Seluruh perubahan UI dan pengujian baru di atas menunggu pelaksanaan. Tidak ada perubahan aplikasi dalam sesi penyusunan rencana ini.
