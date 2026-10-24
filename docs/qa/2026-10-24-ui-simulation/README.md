# Simulasi fitur dan audit UI Arina Agri

Tanggal: 24 Oktober 2026. Target: aplikasi Android native dalam checkout `Arina Agri Kotlin`.

Sumber build adalah workspace yang sudah mempunyai perubahan animasi/UI belum dikomit sebelum sesi ini. Bukti merepresentasikan APK debug dari keadaan tersebut, bukan klaim build rilis atau checkout bersih. Perubahan yang sudah ada dipertahankan.

## Hasil dan cara melihat

- **[Galeri 91 screenshot / 13 kelompok](index.html)** — klik gambar untuk ukuran penuh.
- **Demo yang dapat dioperasikan:** emulator `codex_arina_welcome` / `emulator-5554`, usaha **SIMULASI UI - Kopi Senja**, unit **Gerai Simulasi**. Data tetap tersimpan setelah pengujian; aplikasi ditinggalkan pada Beranda Realisasi.
- **[Workbook fiktif](SIMULASI-UI-KOPI.xlsx)**, **[hasil ekspor Excel](EXPORT-SIMULASI.xlsx)**, dan **[hasil ekspor PDF](EXPORT-SIMULASI.pdf)**.
- [Manifest screenshot dengan hash SHA-256](manifest.json).

Galeri merekam UI aplikasi yang ada, bukan mockup atau implementasi web. Tidak ada perubahan kode UI/domain aplikasi dalam pekerjaan ini. Permintaan membuat simulasi diklasifikasikan **user-confirmed**; hasil tangkapan layar dan langkah yang benar-benar dijalankan **verified**; diagnosis yang belum diuji dengan perbaikan **inferred**; saran desain **proposed**; keadaan di luar cakupan **unknown**.

## Skenario data

Data sepenuhnya fiktif. Impor menambahkan **17 baris**: 7 transaksi kas, 5 baris anggaran, 2 produk, dan 3 barang stok. Dua produk adalah Kopi Susu dan Es Teh. Cup 250 ml mempunyai stok 20 dan minimum 30 untuk menampilkan peringatan stok menipis. Periode rencana Oktober 2026–September 2027.

Kas masuk Rp20.500.000 terdiri dari modal Rp15.000.000 dan penjualan Rp5.500.000. Kas keluar Rp11.350.000 terdiri dari aset Rp7.200.000 dan biaya operasi Rp4.150.000. Saldo kas yang terlihat **Rp9.150.000** dan laba berbasis transaksi **Rp1.350.000**. Kartu BEP menampilkan laba setelah penyusutan Rp1.150.000; ini metrik berbeda, bukan dua tampilan untuk satu definisi laba.

Satu pengeluaran tambahan Rp50.000 dicatat melalui UI, lalu dibalik melalui konfirmasi. Riwayat menjadi 9 transaksi dengan sumber dan pembalik tetap terlihat; saldo kembali Rp9.150.000. Nilai stok demo persisten Rp802.000. Screenshot dari pengujian in-memory memakai fixture lama yang mempunyai **2 stok senilai Rp792.000**; keduanya diberi label berbeda di galeri.

## Cakupan yang benar-benar dijalankan

| Kelompok | Bukti | Status |
| --- | --- | --- |
| Tanpa akun, tambah usaha dan unit | 01–03 | **verified**: alur offline dipilih, usaha/unit dibuat dari UI |
| Beranda dan metrik berisi data | 07; phone/tablet captures | **verified**: UI dan nilai yang disebut di atas terlihat |
| Pemasukan/pengeluaran, rincian opsional | 08–13 | **verified**: kedua form dibuka; pengeluaran Rp50.000 disimpan |
| Pembalikan dan hapus | 14–15, 55 | **verified**: pembalikan dilakukan; hapus hanya dibuka lalu dibatalkan |
| Stok normal dan menipis, tambah barang | 16–17 | **verified**: UI dibuka; stok berasal dari impor; tambah barang manual belum disimpan |
| Stok masuk, keluar, opname | 18–20 | **verified UI**: ketiga form dibuka; penyimpanan mutasi **unknown** |
| Laba rugi, arus kas, grafik bulanan | 21–22 | **verified UI**: tampilan dan scroll diperiksa; grafik memakai satu bulan |
| Kelayakan/BEP dan perbandingan | 23–24 | **verified UI**: ketiga tab laporan dibuka |
| Periode, produk/resep, biaya, aset, modal | 25–34 | **verified UI**: seluruh jenis editor dibuka; penyimpanan manual editor **unknown** |
| Impor valid | 04–06, 35 | **verified** dengan workaround keyboard karena tombol bertumpuk; bukan penerimaan jalur sentuh |
| Ekspor Excel/PDF | 36–38 dan berkas ekspor | **verified**: pemilih lokasi Android diselesaikan dan berkas berhasil ditarik dari emulator |
| Kategori dan balik batch | 42, 53 | **verified UI**: pengelola dan konfirmasi dibuka; perubahan/pembalikan batch tidak dijalankan |
| Penolakan workbook tidak valid | 54, INVALID-TEMPLATE.xlsx | **verified**: penanda/lembar yang hilang dilaporkan; tata letaknya bermasalah |
| Cadangan/pemulihan lokal | 39–41 | **verified UI**: form dibuka; pembuatan dan pemulihan penuh **unknown** |
| Akun/Google Drive | 39, 44, light-cloud | **verified UI**: keadaan belum masuk dan form cloud dengan auth fiktif dalam test; layanan nyata **unknown** |
| Profil, Umum, Sistem, tema | 43–48 | **verified UI**: tab dibuka, tema gelap lalu terang dipilih; simpan profil/bahasa **unknown** |
| Bantuan dan Tunjukkan di aplikasi | 49–52 | **verified**: artikel dan navigasi sorotan bekerja pada skenario ini |
| Keadaan kosong | light-reports/stock/plan/import | **verified UI** dengan fixture in-memory kosong |
| Ponsel terang/gelap dan tablet terang | phone-*; tablet-light-* | **verified UI** empat tujuan utama; bukan semua kombinasi keadaan/layout |
| 19 target panduan fitur | help_* | **verified capture**; scrim gelap disengaja untuk menyorot target |

## Temuan yang perlu diprioritaskan

### P1 — Konten impor saling menimpa

**verified:** pada [pratinjau](05-import-preview.png), [hasil impor](06-import-result.png), dan [penolakan format](54-import-invalid.png), teks ringkasan dan tombol berada pada posisi yang sama. Teks sulit dibaca dan mengetuk area tombol pratinjau membuka pemilih workbook lain. Keyboard Tab/Enter dipakai sebagai workaround untuk memasukkan fixture; masalah sentuh tetap ada.

**inferred:** `MoreScreen.kt`, `ImportCard`, membungkus cabang status dalam `AnimatedContent` tetapi cabang Ready/Committed/Failure mengeluarkan beberapa sibling tanpa satu `Column`. Layout animasi menempatkan sibling bertumpuk. Area `transferStatusMotion` juga perlu diperiksa untuk cabang yang mengeluarkan beberapa komponen; kegagalan cabang tersebut belum direproduksi di sini.

**proposed:** setiap target animasi memiliki satu root layout yang mengatur susunan vertikal; gunakan state target yang sesuai untuk konten keluar/masuk. Verifikasi dengan alur pilih file → preview dapat dibaca → simpan lewat sentuhan → hasil, lalu invalid template dan impor ulang. Tambahkan bukti regresi pada UI asli; jangan hanya menguji helper animasi terpisah.

### P2 — Hierarki dan warna ringkasan masih seragam

**verified:** [Beranda](07-home-filled.png) dan [tablet](tablet-light-home.webp) menampilkan empat kartu dengan permukaan putih, ikon mint, dan nominal hijau yang sama. [Daftar stok](16-stock-low.png) terutama memakai nama dan satu baris stok/nilai. [Perbandingan](24-report-comparison.png) berisi pasangan angka kecil dengan selisih yang lebih menonjol.

**proposed:** terapkan kategori biru/lavender/mint/krem pada kartu metrik sesuai rekomendasi sebelumnya, nominal utama gelap, serta status finansial terpisah. Pada stok, perjelas hubungan stok kini dan batas minimum tanpa mencampur satuan berbeda. Pada perbandingan, sejajarkan Proyeksi, Realisasi, dan Selisih dengan judul periode yang jelas. Jangan menambah grafik dekoratif untuk data satu bulan atau Rp0.

### P2 — Kepadatan form dan daftar bantuan perlu dirapikan

**verified:** [form transaksi](10-income-form.png) memuat enam shortcut nominal dalam satu baris; label paling kanan hanya tampak sebagian pada lebar ini. [Editor resep](28-product-recipe-editor.png) memiliki banyak field dan komponen dalam satu kolom panjang. [Pusat Bantuan](49-help-center.png) menggunakan ikon buku seragam untuk banyak topik.

**proposed:** pastikan shortcut nominal memiliki petunjuk scroll atau pembagian baris yang jelas; tonjolkan biaya per satuan dan margin pada editor resep; beri ikon per kelompok bantuan yang membantu pencarian. Ini prioritas visual sesudah masalah impor diperbaiki. Tidak diperlukan perubahan urutan fitur, salinan, atau arsitektur domain untuk mengevaluasi usulan tersebut.

## Bukti eksekusi

- `assembleDebug assembleDebugAndroidTest --console=plain`: **BUILD SUCCESSFUL**, 5m46s, 73 actionable tasks. JDK launcher memakai Android Studio; daemon proyek memakai JBR 21 sesuai konfigurasi yang sudah ada.
- `HelpScreenshotTest`, args `helpScreenshots=true`, `brandScreenshots=true`: **OK (2 tests)**, 17.189 detik; 19 sorotan dan 4 layar utama terang.
- `ContextualLottieTest#realNavigationUsesEachArtworkOnlyInItsRelevantContext`: **OK (1 test)**, 11.042 detik; lima keadaan kosong/cloud bertema terang.
- `HelpScreenshotTest#captureBrandScreenshotsThroughRealNavigation`, `brandTheme=dark`: **OK (1 test)**, 7.815 detik; empat layar gelap.
- Metode yang sama, `brandLabel=tablet`, tema terang, viewport emulator 1600×2560/density 240: **OK (1 test)**, 16.202 detik; empat layar tablet. Ukuran/density dikembalikan ke 1080×2400/density 420 sesudahnya.
- 55 tangkapan dari navigasi langsung, 12 dari navigasi fixture terang/gelap/tablet, 5 keadaan kosong/cloud, 19 sorotan: **91 screenshot**.
- Verifikasi artefak: **PASS**, 91 berkas gambar dengan hash/signature sesuai, 200 tautan galeri valid, fixture dan dua berkas ekspor tersedia. Beberapa capture menampilkan keadaan yang sama, sehingga tidak berarti 91 desain layar berbeda. Navigasi kelompok dan gambar galeri juga diperiksa melalui browser.
- Test screenshot memakai in-memory Room. Demo persisten diisi melalui alur impor aplikasi. Semua perintah ADB ditargetkan ke `emulator-5554`; ponsel fisik yang terhubung tidak dipakai.

## Cara mengulang dan memeriksa artefak

1. Build aplikasi dan APK test dengan JDK/SDK lokal; jangan mengubah identitas Firebase atau kredensial untuk audit ini.
2. Pilih emulator terpisah. Instal kedua APK dengan `adb -s <serial> install -r ...`. Jangan menjalankan fixture pada buku usaha nyata.
3. Jalankan dua metode screenshot di atas dengan instrumentation runner `com.mankeu.test/androidx.test.runner.AndroidJUnitRunner`. File test berada di `files/help`, `files/brand-ui`, dan `files/lottie-proof` milik aplikasi. Tarik hanya nama yang dihasilkan run saat ini; folder dapat berisi artefak run lama.
4. Untuk demo persisten, pilih Lanjut tanpa akun, tambah usaha bertanda SIMULASI, tambah unit, dan pilih workbook fiktif dari Download. Periksa preview; jika tampilan bertumpuk seperti bukti, catat kegagalan jalur sentuh. Workaround keyboard pada run ini tidak memperbaiki UI.
5. Jalankan alur sesuai matriks cakupan. Gunakan `adb -s <serial> shell uiautomator dump ...` untuk membaca label/bounds terbaru dan `adb -s <serial> exec-out screencap -p` untuk capture biner. Jangan memakai redirection teks PowerShell untuk PNG.
6. Bangun ulang galeri dari screenshot yang telah direkam: `python scripts/qa/build_ui_simulation_gallery.py`.
7. Periksa integritas bukti dan semua tautan lokal: `python scripts/qa/verify_ui_simulation.py`.

## Batas pemeriksaan

Audit ini bukan bukti seluruh fitur end-to-end sudah benar. Login/unggah Drive nyata, backup/restore penuh, simpan seluruh editor, mutasi stok, impor ulang duplikat, multi-usaha lintas skenario, proses mati, kondisi disk penuh, font 200%, TalkBack, tema gelap semua dialog, perangkat fisik, performa animasi, serta ketepatan semua rumus belum diverifikasi di sesi ini. Layar atau fungsi yang belum diuji tidak diberi label rusak. Screenshot statis tidak membuktikan transisi smooth. Tampilan PDF hasil ekspor belum diaudit halaman demi halaman.

Penggunaan panduan `mobile-app-ui-design` dan `ui-slop-score` dibatasi untuk menilai bukti tampilan; tidak ada skor numerik karena tidak diminta. Pemeriksaan mandiri opsional tersedia di [UIZZE UI Slop Score](https://uizze.com/tools/ui-slop-score).
