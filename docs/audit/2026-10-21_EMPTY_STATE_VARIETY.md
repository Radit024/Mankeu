# Variasi Tampilan Kosong (Empty State Variety) — Verification & Audit Report

**Date**: 2026-10-21  
**Scope**: Native Android Kotlin / Jetpack Compose (`Mankeu`)  
**Status**: VERIFIED & ACCEPTED  
**ADR Reference**: `[DEC-027]`  

---

## 1. Executive Summary

Pengguna melaporkan tampilan kosong yang monoton dan identik antara kondisi awal (belum ada usaha / unit) dan kondisi transaksi kosong (belum ada catatan uang masuk/keluar). Keduanya sebelumnya menggunakan ilustrasi dokumen/kertas (`StateArtwork.EMPTY`).

Implementasi ini memberlakukan diferensiasi kontekstual penuh:
1. **Diferensiasi Semantik Visual**: Layar onboarding dan persiapan usaha menggunakan ilustrasi **Business Plan** (`BUSINESS_PLAN`, 144 dp), sedangkan alur pencatatan transaksi menggunakan ilustrasi **Dokumen Catatan** (`EMPTY`, 88 dp), dan laporan kosong menggunakan **Grafik Pertumbuhan** (`REPORTS`, 88 dp).
2. **Kontrak Kode yang Kuat**: Menghapus nilai default parameter pada signature `EmptyCard` (`artwork: StateArtwork`), sehingga compiler Kotlin secara otomatis memvalidasi dan mewajibkan penentuan ilustrasi di seluruh call site.
3. **Preservasi Fungsionalitas**: Tidak ada perubahan pada ViewModel, kalkulasi keuangan, schema database, navigasi, tombol CTA (Tambah Usaha, Tambah Unit, Catat Sekarang, Buka Rencana), maupun tautan bantuan (Pusat Bantuan).

---

## 2. Tabel Pemetaan Final (Visual Contract)

| No | Layar & Kondisi | StateArtwork | Ukuran Render | Aksi & Bantuan Terpasang |
|---|---|---|---|---|
| 1 | **Beranda**: Belum ada usaha | `BUSINESS_PLAN` | 144 dp | Tambah Usaha (`onOpenData`), Bantuan (`HelpArticle.START_SCOPE`) |
| 2 | **Beranda**: Belum ada unit/lahan | `BUSINESS_PLAN` | 144 dp | Tambah Unit (`onOpenData`), Bantuan (`HelpArticle.START_SCOPE`) |
| 3 | **Beranda (Proyeksi)**: Belum ada rencana | `BUSINESS_PLAN` | 144 dp | Buka Rencana (`onOpenPlan`), Bantuan (`HelpArticle.PLAN_CREATE`) |
| 4 | **Kelola Usaha**: Daftar usaha kosong | `BUSINESS_PLAN` | 144 dp | Bantuan (`HelpArticle.START_SCOPE`) |
| 5 | **Kelola Usaha**: Daftar unit kosong | `BUSINESS_PLAN` | 144 dp | Tombol Tambah Unit tetap tersedia di bawah kartu |
| 6 | **Stok**: Belum ada usaha setup | `BUSINESS_PLAN` | 144 dp | Tambah Usaha (`onOpenData`), Bantuan (`HelpArticle.START_SCOPE`) |
| 7 | **Rencana Usaha**: Belum ada unit | `BUSINESS_PLAN` | 144 dp | Tambah Unit (`onOpenData`), Bantuan (`HelpArticle.START_SCOPE`) |
| 8 | **Catat Transaksi**: Belum ada catatan | `EMPTY` | 88 dp | Catat Sekarang (`selected = CASH_INCOME`), Bantuan (`HelpArticle.RECORD_INCOME`) |
| 9 | **Catat Transaksi**: Filter/pencarian nihil | `EMPTY` | 88 dp | Teks ubah kata kunci/filter |
| 10 | **Laporan**: Belum ada data transaksi | `REPORTS` | 88 dp | Catat Transaksi / Buka Rencana |
| 11 | **Laporan (Proyeksi)**: Belum ada rencana | `BUSINESS_PLAN` | 144 dp | Buka Rencana (`onOpenPlan`), Bantuan (`HelpArticle.PLAN_CREATE`) |
| 12 | **VisualSystemTest**: Fixture pengujian | `EMPTY` | 88 dp | Catat Contoh callback |

Ilustrasi lain di luar `EmptyCard` tetap dipertahankan:
- Introduksi rencana kosong `StateIllustration(BUSINESS_PLAN, 160.dp)`
- Introduksi stok kosong `StateIllustration(STOCK, 104.dp)`
- Import file card `StateIllustration(IMPORT, 64.dp)`
- Cloud backup dialog `StateIllustration(CLOUD_BACKUP, 104.dp)`

---

## 3. Hasil Pengujian & Verifikasi

### 3.1 Unit & Domain Tests (`gradlew test`)
- **Status**: `BUILD SUCCESSFUL in 1m 41s`
- **Hasil**: 34 tasks dieksekusi / up-to-date, 0 failure. Seluruh financial logic, BEP calculator, CashFlow calculator, Room DAOs, dan ViewModel tests lulus 100%.

### 3.2 Compilation & Lint Checks (`gradlew assembleDebug assembleDebugAndroidTest lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 7m 3s`
- **Output Artifacts**:
  - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
  - AndroidTest APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
  - Lint Report: `app/build/reports/lint-results-debug.html` (0 errors)

### 3.3 Asset & Visual System Checks (`verify_visual_system.py`)
- **Command**: `python scripts/prepare_lottiefiles.py; python scripts/verify_visual_system.py`
- **Hasil**:
  - `PASS`: 5 pinned vector sources, 10 deterministic offline variants, 306,587 bundled JSON bytes.
  - `PASS`: 284 visual & theme checks, 166 contrast pairs, WCAG AA compliant.

### 3.4 Regresi Instrumental Test (`ContextualLottieTest.kt`)
- Ditambahkan test case: `@Test fun firstUseHomeUsesBusinessArtworkAndPreservesSetupAction()`
- Memverifikasi secara independen bahwa pada Beranda awal (`businesses.isEmpty()`):
  - Node dengan test tag `artwork-BUSINESS_PLAN` ditampilkan (`assertIsDisplayed()`).
  - Node dengan test tag `artwork-EMPTY` **tidak ada** (`assertDoesNotExist()`).
  - Klik tombol "Tambah Usaha" memicu callback navigasi data/scope dengan tepat (`opens == 1`).

---

## 4. Rollback Plan

Jika terjadi masalah visual pada perangkat pengguna:
1. Tidak ada migrasi database atau perubahan schema data; pembatalan sepenuhnya terbatas pada layer presentasi UI.
2. Perubahan dapat di-revert secara atomik via git:
   ```bash
   git revert HEAD
   ```
3. Alternatif darurat: Menambahkan kembali default parameter `artwork: StateArtwork = StateArtwork.EMPTY` pada `SharedComponents.kt`.

---

## 5. Keterbatasan & Catatan Operasional
- Pengujian instrumented test langsung pada perangkat fisik pengguna tidak dijalankan untuk mencegah penghapusan data produksi lokal milik pengguna, sesuai instruksi safety pada `docs/superpowers/plans/2026-10-21-empty-state-variety.md`.
- Skenario pengujian visual telah diuji pada build debug APK native. Pemasangan pada perangkat fisik harus dilakukan via upgrade (`install -r`) tanpa uninstall (`clear data`).
