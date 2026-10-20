# Audit UI/UX Bisnis & Sistem Mankeu — 20 Oktober 2026

> **Status**: selesai (audit). Rencana perbaikan: [`2026-10-20_RENCANA_PERBAIKAN.md`](2026-10-20_RENCANA_PERBAIKAN.md).
> **Pelaksana**: Claude Code (Opus 5.5), atas permintaan pemilik produk.
> **Build yang diaudit**: `main` @ `9dd5f90` + perubahan finalisasi 20 Okt (Room skema 7, 4 tab: Beranda · Catatan · Stok · Laporan).
> **Klasifikasi bukti** (sesuai `AGENTS.md`): `verified` = dilihat langsung di emulator atau dibaca di kode; `inferred` = disimpulkan dari kode tanpa dijalankan; `unknown` = tidak bisa diuji.

## 1. Cara audit dilakukan

1. **Penelusuran layar** di emulator `Medium_Phone` (1080×2400) dengan data contoh dari impor `simulasi_kelayakan_umkm_verifikasi.xlsx` (58 catatan Realisasi, RAB Proyeksi, 23 kategori), memakai mode Proyeksi dan Realisasi.
   Layar yang dilalui: Beranda, Catatan (daftar, cari, filter, form masuk/keluar, pilih kategori), Stok (tambah barang, keluar, opname), Laporan (3 tab), Berkas & Data (impor, kelola kategori, riwayat impor), Pengaturan, dan tur panduan.
2. **Pembacaan kode** untuk setiap gejala di layar, sampai ke sumber angkanya (ViewModel → Repository → DAO/kalkulator).
3. **Pemeriksaan database emulator** (`run-as … databases/mankeu.db`) untuk memastikan jenis transaksi yang benar-benar tersimpan.
4. **Audit sistem** (gaya `ponytail-audit`): kode mati, dependensi tak terpakai, duplikasi logika, sisa konsep non-kas.

**Tidak dicakup** (`unknown`): login Google / cadangan Drive (butuh akun asli; Claude tidak boleh memasukkan kredensial), isi file PDF/Excel hasil ekspor yang dibuka di aplikasi lain, mode gelap, layar tablet (rail), dan bahasa Inggris.

## 2. Ringkasan eksekutif

Tampilan Mankeu rapi, input transaksinya cepat, dan aplikasinya benar-benar offline. **Untuk dipakai berbisnis, aplikasi ini belum layak rilis**, karena:

- **Beberapa angka utama salah atau saling bertentangan.** Contohnya: Rencana vs Realisasi menampilkan Proyeksi Rp0, ada tiga angka "total biaya" yang berbeda, dan BEP salah metode.
- **Beberapa kebutuhan dasar UMKM tidak punya jalan input**: modal, prive, beli alat, dan RAB.
- **Model "kas murni" belum benar-benar diterapkan.** Importer masih memasukkan transaksi kredit dan pinjaman, dan kodenya masih memuat ±700 baris konsep non-kas.

| Tingkat | Jumlah | Arti |
|---|---|---|
| P0 | 9 | Angka salah/menyesatkan, atau fungsi inti tidak bisa dipakai. Pemblokir rilis. |
| P1 | 12 | Redundansi atau alur yang membuat pengguna bekerja dua kali atau bingung. |
| P2 | 10 | Bahasa, istilah, konsistensi. |
| SYS | 12 | Kompleksitas/kode mati (audit sistem). |

## 3. Temuan P0 — angka salah atau fungsi inti hilang

### A-01 · Rencana vs Realisasi menampilkan Proyeksi = Rp0 `verified`
- **Gejala**: Laporan → "Realisasi vs Target": Pendapatan, Biaya, Laba, dan Kas Proyeksi semuanya **Rp0**, sehingga "Selisih Pendapatan Rp149.240.000". Padahal Beranda mode Proyeksi menunjukkan Pendapatan Rp168.000.000, Biaya Rp128.050.000, Laba Rp39.950.000, Kas Rp26.950.000.
- **Akar masalah**: `ComparisonSection` (`ReportsScreen.kt:434`) membaca `state.projectionSummary`, yang hanya dihitung dari posting transaksi (`AppViewModel.projectionSummary`). Buku Proyeksi normalnya berisi RAB tanpa transaksi. Beranda memakai fallback `BudgetPlanCalculator.summarize` (`HomeScreen.kt`, `planFromBudget`), Laporan tidak.
- **Dampak**: fitur pembeda utama ("rencana vs kenyataan") memberi kesimpulan terbalik.

### A-02 · Transaksi non-kas masih ada dan masih bisa masuk `verified`
- **Isi database** (buku Realisasi): `CREDIT_SALE` 2 baris (Rp15,4 jt), `CREDIT_PURCHASE_EXPENSE` 2 (Rp7,7 jt), `RECEIVABLE_COLLECTION` 2 (Rp12 jt), `PAYABLE_SETTLEMENT` 2 (Rp7,7 jt), `LOAN_RECEIVED` 1 (Rp20 jt), `LOAN_PRINCIPAL_REPAID` 2 (Rp2 jt), `LOAN_INTEREST_PAID` 2 (Rp390 rb).
- **Akibat yang terlihat**: Laba Rp15.650.000 ≠ Uang masuk bersih Rp12.250.000. Sisa piutang ±Rp3,4 jt dan sisa pinjaman Rp18 jt tidak tampil di mana pun.
- **Akar masalah**: importer masih memetakan kata "piutang", "utang", "tempo", "pinjaman masuk", dll. ke tipe non-kas (`ImportModels.kt:63-80`, `FinanceWorkbookImporter.kt:231, 509, 897-904`). `LedgerPolicy` masih membukukannya ke akun piutang/utang/pinjaman.

### A-03 · Arah uang transaksi lama terbalik di Catatan `verified`
- "Penerimaan sebagian Warung Bu Ani" (pelunasan piutang = **uang masuk**) tampil merah **−Rp5.000.000** dengan panah keluar, dan ikut di filter "Keluar".
- **Akar masalah**: `BusinessWords.INCOMING_TYPES` (`BusinessWords.kt:43`) hanya berisi `CASH_INCOME` dan `OWNER_CAPITAL`, jadi semua tipe lain dianggap keluar.

### A-04 · Modal, prive, dan beli alat tidak bisa dicatat `verified`
- Form hanya punya dua jenis: `entryKinds` = `CASH_INCOME`, `CASH_EXPENSE` (`EntryScreen.kt:123-124`). Tidak ada jalan UI untuk `OWNER_CAPITAL`, `OWNER_DRAWING`, `ASSET_PURCHASE_CASH`, padahal domain menyebut 5 jenis kas.
- **Dampak bisnis**: setoran modal akan dicatat sebagai pendapatan (laba membengkak), prive sebagai biaya, dan beli kulkas Rp5 jt sebagai biaya operasional (laba bulan itu anjlok, BEP rusak). Arus kas investasi dan pembiayaan tidak akan pernah terisi dari input pengguna.

### A-05 · RAB tidak punya layar input `verified`
- `createBudgetItem`, `updateBudgetItem`, `archiveBudgetItem` ada di `AppViewModel` (`:558-630`) dan Repository, **tetapi tidak ada satu pun pemanggil di UI** (dicek: 0 referensi di `presentation/screens`).
- Mode Proyeksi hanya bisa terisi lewat impor Excel. Pengguna baru tanpa Excel tidak bisa membuat rencana, sehingga A-01, kartu kelayakan, dan perbandingan kosong.

### A-06 · Tiga angka "total biaya" berbeda di layar yang sama `verified`
- Realisasi: **Biaya Operasional Rp133.590.000** (kartu Beranda, Laba Rugi, "Total Biaya Produksi" di tab BEP) vs **Komposisi Biaya Rp125.500.000** (donut Beranda).
- Proyeksi: Biaya Operasional Rp128.050.000 vs Komposisi Biaya Rp141.050.000.
- **Akar masalah**: donut memakai `observeCategoryTotals`, yang hanya menjumlah `type = 'CASH_EXPENSE'` (`FinanceDao.kt:180-196`). Biaya operasional dari posting juga memuat bunga dan pembelian kredit (A-02). Di mode Proyeksi, donut memakai semua baris RAB non-pendapatan termasuk peralatan (aset), sedangkan kartu memakai ringkasan RAB.

### A-07 · BEP salah metode: biaya variabel terhitung dua kali, periode tidak cocok `verified` (kode)
- Label form: "Biaya Tetap **Bulanan** (FC, Opsional)". Jika kosong, FC diisi otomatis dengan **total beban operasional sejak awal** + biaya pembiayaan (`BusinessAnalysis.kt:141-142`, `ReportsScreen.AssumptionsDialog` → `fallbackFc`).
- Beban operasional sudah memuat bahan baku, yaitu biaya variabel. BEP = FC/(P−VC) lalu memasukkan VC dua kali: sekali di FC, sekali di margin. Totalnya juga kumulatif (berbulan-bulan), bukan per bulan.
- Buku Realisasi berisi harga jual **Rp100.005.000/unit** (data uji) dan form tidak memberi peringatan apa pun. BEP tampil "1,363 unit / Rp136.306.815".
- **Dampak**: angka yang paling dinilai dosen bisa salah secara metodologis.

### A-08 · Format angka volume mengikuti bahasa HP, bukan Indonesia `verified`
- BEP Volume tampil **"1,363"** sementara uang tampil "Rp136.306.815". Pembaca Indonesia membaca "1 koma 363".
- **Akar masalah**: `quantityFormat` (`Formatting.kt:30`) memakai locale default perangkat; `toRupiah` memakai format Indonesia.

### A-09 · Data contoh/impor bisa membuat buku tak masuk akal tanpa peringatan `verified`
- Impor memberi nama usaha sesuai nama file ("simulasi_kelayakan_umkm_verifikasi"), membuat unit "Lahan Utama" atau usaha "Usaha Tani" (`AppViewModel.kt:729-738`), dan menyimpan parameter asumsi yang tidak divalidasi (harga Rp100 jt/unit).
- Digolongkan P0 karena layar pertama pengguna setelah impor langsung menampilkan identitas dan angka yang salah.

## 4. Temuan P1 — redundansi dan alur

| ID | Temuan | Bukti |
|---|---|---|
| A-10 | **Stok terputus dari kas.** Beli bahan dicatat dua kali (Catatan + Stok), penjualan tidak mengurangi stok. | `verified` — DEC-010 sengaja "inventory-only". |
| A-11 | **Tidak ada pilihan periode.** Semua angka kumulatif sejak awal. Tidak ada "bulan ini / minggu ini / pilih bulan". | `verified` di semua layar. |
| A-12 | **Isi Beranda dan Laporan dobel.** Empat kartu Beranda (uang masuk bersih, laba, pendapatan, biaya) = isi Laba Rugi + Arus Kas di Laporan. | `verified` (`HomeScreen` `SimpleMetricGrid`, `ReportsScreen` Financials). |
| A-13 | **Toggle Proyeksi/Realisasi muncul di layar yang tidak memakainya**: tab Stok (stok berlaku per usaha) dan tab Rencana vs Realisasi (membandingkan keduanya). | `verified`. |
| A-14 | **Parameter BEP disimpan per buku**, jadi harus diisi dua kali. Di Realisasi, BEP seharusnya memakai angka aktual. | `verified` (`scenario_assumptions.bookId` unik). |
| A-15 | **Dua cara membatalkan per baris** (↶ Balik dan 🗑 Hapus), dan baris "Pembalikan" tampil sebagai catatan sendiri berlabel "DIBALIK". Membingungkan untuk UMKM. | `verified` (`SharedComponents.kt:218, 230`). |
| A-16 | **Laba Rugi hanya 2 baris** tanpa rincian kategori. Rinciannya justru ada di donut Beranda, dan donut itu hanya menampilkan 4 dari 9–10 kategori tanpa "Lainnya". | `verified`. |
| A-17 | **Kategori dobel dan usang**: "Bahan Baku dan Stok" vs "Bahan Baku Makanan", "Gaji dan Upah" vs "Gaji Koki dan Pegawai", tiga kategori listrik/gas, plus "Biaya Bunga Pinjaman" dan "Pinjaman Usaha". Total 23 kategori aktif. | `verified`; default 16 kategori di `BusinessTemplate.kt`, impor menambah tanpa mencocokkan. |
| A-18 | **Kelola kategori lemah**: tidak bisa ganti nama atau gabung. Ada 5 peran, tapi hanya Pemasukan/Pengeluaran yang bisa dipakai di form. Peran "Pinjaman" berlabel "Modal sendiri" (`BusinessWords.kt:21`). Arsip tanpa konfirmasi. Daftar tidak dikelompokkan. | `verified` (`MoreScreen.MasterDataDialog:566`). |
| A-19 | **Notifikasi impor permanen di Beranda.** Kolom `reviewed` ada tapi tidak pernah bisa diubah dari UI. | `verified` (`FinanceDao.kt:288`, tidak ada setter). |
| A-20 | **Cadangan terpecah dua tempat**: cadangan lokal di Berkas & Data, cadangan Drive di Pengaturan. Kelola usaha juga ada di dua tempat (pemilih di header dan Berkas & Data). | `verified`. |
| A-21 | **Form catat transaksi tidak konsisten dengan sheet lain**: memakai `ModalBottomSheet` sendiri (`EntryScreen.kt:387`), bukan `MankeuSheet`; tanpa tombol Batal; judul "Uang keluar" diulang 3×. Tombol Back tidak menutup form saat diuji lewat adb (`inferred`, perlu uji manual). | `verified` sebagian. |

## 5. Temuan P2 — bahasa dan konsistensi

| ID | Temuan |
|---|---|
| A-22 | Satu konsep punya empat sebutan: **Lahan / Unit usaha / Cabang / Proyek**. Pesan error masih "Nama lahan wajib diisi" (`FinanceRepository.kt:229, 249`). |
| A-23 | Jargon teknis: "COMMITTED", "Balik batch", "Riwayat pengambilan", jumlah "Rp666.780.000" (total semua nilai impor, tidak bermakna), "58 catatan pada unit usaha dan buku yang aktif". |
| A-24 | Label tidak cocok dengan konteks: "Proyeksi Omzet" di mode Realisasi, "Margin: Rp98.005.000" tanpa penjelasan, "Total Biaya Produksi" untuk biaya operasional. |
| A-25 | Label mode diulang tiga kali: toggle "Realisasi", judul "Uang kas realisasi", chip "Mode Aktual". |
| A-26 | Notifikasi impor menunjuk menu yang sudah tidak ada ("Laporan > Kelayakan dan Modal > Ubah Asumsi", `FinanceRepository.kt:710`). Notifikasi yang sudah tersimpan masih menyebut "pinjaman, bunga". |
| A-27 | Kartu kas Beranda: "Belum ada pergerakan kas pada periode terakhir", karena bulan terakhir berisi pasangan transaksi + pembaliknya (net 0) (`HomeScreen.kt:380`). |
| A-28 | Empat ikon header (⇅ ? 📁 👤) tanpa label. "📁" = Berkas & Data, "👤" = Pengaturan. Tur bisa dibuka dari 3 tempat ("?", "Mulai Tur", "Ulangi Semua Panduan"). |
| A-29 | Akun Google tampil paling atas di Pengaturan, padahal aplikasi offline-first dan akun itu opsional. |
| A-30 | Tab Stok belum punya tur panduan, dan diagram di `docs/diagrams/` masih menggambarkan navigasi lama. |
| A-31 | Format tanggal "16 Sep 2026" mengikuti bahasa perangkat; perlu dicek konsisten dengan bahasa aplikasi. `inferred`. |

## 6. Audit sistem (ponytail-audit)

Diurutkan dari potongan terbesar. Format: `tag` apa yang dipotong → pengganti. [lokasi]

1. `delete:` Seluruh konsep non-kas: tipe `CREDIT_SALE`, `CREDIT_PURCHASE_EXPENSE`, `CREDIT_PURCHASE_ASSET`, `RECEIVABLE_COLLECTION`, `PAYABLE_SETTLEMENT`, `LOAN_RECEIVED`, `LOAN_PRINCIPAL_REPAID`, `LOAN_INTEREST_PAID`; akun `ACCOUNTS_RECEIVABLE/PAYABLE`, `LOAN_PAYABLE`, `INTEREST_EXPENSE`; `ObligationKind/Status/Balance`; field `receivable/payableOutstandingRupiah`. Pengganti: tidak ada. [`FinanceModels.kt`, `LedgerPolicy.kt`, `PostingReportCalculator.kt`, `BusinessWords.kt`, `EntryScreen.kt`, importer]
2. `delete:` 96 string tidak terpakai × 2 bahasa (≈190 baris), mis. `obligation_*`, `settle_*`, `kind_loan_*`, `reports_receivable`, `nav_ledger`. [`res/values*/strings.xml`]
3. `delete:` `ReportCalculator` (157 baris) hanya dipakai test; produksi memakai `PostingReportCalculator`. Pindahkan test ke `PostingReportCalculator`. [`domain/ReportCalculator.kt`]
4. `delete:` Cabang form kredit di `EntryScreen` (pihak/jatuh tempo, `kind.credit`, `errDue`, `errCounterparty`). [`EntryScreen.kt`]
5. `delete:` Pemetaan kata kredit/pinjaman di importer, diganti "abaikan + notifikasi". [`ImportModels.kt:63-80`, `FinanceWorkbookImporter.kt`]
6. `shrink:` BEP dihitung dua kali: `calculateCvp` (hanya dipakai test) dan logika BEP di dalam `calculate`. Satukan: `calculate` memanggil satu fungsi CVP. [`BusinessAnalysis.kt:71, 126`]
7. `delete:` Dependensi `androidx.navigation:navigation-compose`, tidak ada satu `import androidx.navigation` pun. [`app/build.gradle.kts`, `libs.versions.toml`]
8. `delete:` `GetStockSummaryUseCase`, `StockCalculation.calculateStockLevel` tidak dipakai produksi. `StockDao.observeMovements/getMovements/archiveStockItem` belum dipakai; pertahankan hanya jika riwayat mutasi/arsip barang masuk rencana (F-14). [`StockManagement.kt`]
9. `delete:` `assumptions.otherFinancingCostRupiah` / "Biaya pembiayaan lain" dan string `assumptions_loan/interest`: sisa konsep pinjaman. [`ScenarioAssumptionEntity`, `AssumptionsDialog`]
10. `delete:` Kolom `counterparty`, `dueDateEpochDay`, `obligationId` di `financial_events`; kolom `counterparty` cukup diganti makna jadi "keterangan pihak" opsional atau dibuang. Keputusan ada di F-02.
11. `delete:` `BusinessWords.eventLabel` dan `TransactionCalculation.isConsistent` tidak dipanggil.
12. `yagni:` Tidak ada penanda/ikon dual untuk peran `LIABILITY`. Peran ini dihapus bersama konsep pinjaman.

**Tidak ditemukan** (sudah ramping): interface satu implementasi, factory, wrapper delegasi kosong. Vico, Room, dan Firebase dipakai sesuai fungsinya.

`net: ≈ -700 baris, -1 dependensi possible.`

## 7. Yang sudah baik (pertahankan)

- Tombol nominal cepat (+50 rb … +5 jt), input rupiah integer tanpa desimal, pemilih tanggal native.
- Konfirmasi sebelum hapus/balik, jejak pembalikan untuk audit, impor dengan pratinjau 3 langkah dan pembatalan batch.
- Stok: alur sederhana (Masuk/Keluar/Opname), peringatan menipis, nilai persediaan rupiah integer.
- Offline penuh, cadangan terenkripsi, pemulihan skema lama.
- Tema dan aksesibilitas dasar (target sentuh 48dp, kontras hero diperhatikan).

## 8. Keputusan pemilik yang dipakai untuk rencana (20 Okt 2026)

| # | Pertanyaan | Keputusan pemilik |
|---|---|---|
| K-1 | Data non-kas | **Hapus seluruh konsep pinjaman, utang/piutang, cicilan, bunga.** Murni kas. Baris non-kas dari impor **diabaikan**. Jika pengguna sendiri sengaja mencatat (mis. pinjaman sebagai "Uang masuk" berkategori buatannya), biarkan; itu logika pengguna. |
| K-2 | Input modal/prive/aset | **Lewat kategori.** Form tetap 2 tombol; kategori khusus menentukan jenis transaksinya. |
| K-3 | Stok ↔ kas | **Sambung opsional.** Stok Masuk dapat sekaligus mencatat uang keluar; uang keluar kategori bahan dapat sekaligus menambah stok. |
| K-4 | RAB | **Bangun editor RAB** di mode Proyeksi. |
| K-5 | Kategori | **Hanya kategori dasar**; yang spesifik memakai fitur kategori kustom yang sudah ada. |
| K-6 | Prinsip impor/ekspor | **Mankeu yang menyediakan template Excel.** Template kosong = file ekspor (satu format, round-trip utuh). |
| K-7 | File bukan template | **Ditolak** dengan daftar lembar/kolom yang salah + tombol unduh template. Tidak ada penebakan. |
| K-8 | Workbook referensi lama | **Dukungan dihapus**, termasuk enrichment otomatis. Workbook verifikasi dibuat ulang dalam format template. |
| K-9 | Fungsi impor | **Input massal** ke unit usaha yang dipilih. Pindah data tetap lewat cadangan `.aabk`. |
| K-10 | Isi template | Lembar **Transaksi**, **Rencana Anggaran**, **Parameter BEP**, **Stok Barang** (+ Petunjuk dan daftar Kategori sebagai referensi). |
| K-11 | Kolom transaksi | **Gaya buku kas**: Tanggal · Keterangan · Kategori · Pemasukan (Rp) · Pengeluaran (Rp). |
| K-12 | Kategori tak dikenal | **Dibuat otomatis** sebagai kategori kustom dan ditampilkan di pratinjau. |
| K-13 | File sama diimpor ulang | **Diizinkan dengan peringatan** (mengganti penolakan yang berlaku sekarang). |
