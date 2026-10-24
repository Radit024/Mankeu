# Penyederhanaan Informasi Card — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Execute sequentially with review checkpoints; no subagent is needed.

**Goal:** Membuat card Arina lebih mudah dipindai dengan satu hasil utama, informasi pendukung secukupnya, dan rincian yang dapat dibuka tanpa mengubah angka keuangan atau alur pencatatan.

**Architecture:** Pertahankan sumber perhitungan domain, `AppUiState`, navigasi, dan penyimpanan yang ada. Tambahkan satu komponen disclosure Compose untuk informasi sekunder; perbaiki kartu bersama sebelum call site Beranda, Rencana, dan Laporan. Jangan membuat model analisis atau penyimpanan preferensi baru untuk mode ringkas.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, `rememberSaveable`, Compose UI Test, JUnit, formatter rupiah dan motion token yang sudah tersedia. Tidak ada dependency baru.

---

## Status, batas, dan sumber kebenaran

- **user-confirmed:** pengguna meminta implementation plan untuk perbaikan baru setelah riset penyederhanaan card. Dokumen ini merencanakan pekerjaan; checkbox belum menandakan implementasi.
- **verified dari source:** Beranda memakai `plan?.summary ?: state.summary`; `FinancialSummary` berisi hasil kas. `ActualMonthAnalysis.profitRupiah` dan `PlanAnalysis.profitRupiah` juga memasukkan penyusutan dan mempunyai basis periode berbeda.
- **verified dari source:** `PlanCards.kt` dipakai Beranda, Rencana, dan Laporan; `StockCard` dipakai Beranda dan layar Stok; `ImportCard` sudah mempunyai state Idle/Loading/Failure/Ready/Committed.
- **proposed:** hierarki ringkasan/detail di bawah. Plan ini melengkapi [plan refinement sebelumnya](2026-10-24-ui-refinement.md), menggantikan tampilan empat metrik setara di Beranda dan kepadatan informasi sekunder. Perbaikan import overlap, aksen pastel, target sentuh, dan motion yang sudah ada tetap dipertahankan.
- **unknown:** APK terpasang belum terbukti sama persis dengan working tree. Keberhasilan build/test dan perbaikan pemahaman pengguna belum dibuktikan oleh plan ini.

Sebelum mengerjakan kode, baca `AGENTS.md`, `README.md`, `docs/PROJECT_GUIDE.md`, `docs/PRODUCT_REQUIREMENTS.md`, `docs/DOMAIN_MODEL.md`, `docs/QUALITY_GATES.md`, `docs/UX_AND_ACCESSIBILITY.md`, dan [rancangan riset](../../design/2026-10-24-card-simplification/README.md). Pedoman workbook lama yang bertanda superseded tidak mengalahkan DEC-012: impor hanya template milik aplikasi, tanpa menebak atau memperkaya data.

### Referensi visual yang menjadi dasar

Screenshot adalah bukti riset terdahulu, bukan mockup hasil implementasi plan ini. Gunakan berkas asli, tanpa menyalin ulang aset ke folder baru.

| Referensi | Pola yang diadaptasi | Penerapan |
| --- | --- | --- |
| [Copilot Cash Flow](https://help.copilot.money/en/articles/9682232-cash-flow-tab-overview) | Nominal utama dan jalur menuju rincian | Hasil usaha, saldo |
| [Monzo Trends](https://monzo.com/blog/trends), contoh historis 2021 | Satu angka kuat dan daftar kategori singkat | Pengeluaran terbesar |
| [GOV.UK Details](https://design-system.service.gov.uk/components/details/) | Bantuan sekunder dapat dibuka; informasi penting tetap terlihat | Rumus, definisi, asumsi |

**Baseline aplikasi:**

![Beranda sebelum penyederhanaan](../../design/2026-10-24-card-simplification/arina-current.png)

**Referensi Copilot:**

![Nominal utama dan rincian Copilot](../../design/2026-10-24-card-simplification/copilot.png)

**Referensi Monzo, historis 2021:**

![Kategori ringkas Monzo](../../design/2026-10-24-card-simplification/monzo.png)

**Referensi disclosure:**

![Contoh detail tambahan dibuka](../../design/2026-10-24-card-simplification/details-expanded.png)

Provenance dan hash referensi: [sources.json](../../design/2026-10-24-card-simplification/sources.json). Tidak diperlukan riset browser ulang kecuali arah desain berubah atau bukti tidak lagi dapat digunakan.

## Kontrak hasil akhir

| Area | Default | Rincian / tindakan |
| --- | --- | --- |
| Saldo | Label, nominal lengkap, basis data/periode; defisit tetap terlihat | Rincian kas menuju Laporan yang sudah ada |
| Hasil usaha | Laba/rugi kas; pendapatan dan biaya sebagai pendukung | Arus operasi; definisi laba kas; tautan Laporan |
| Pengeluaran | Maksimal tiga kategori terbesar dan nominal | Lihat semua kategori dalam kartu; diagram hanya bila membantu |
| Stok di Beranda | Jumlah barang menipis; atau kondisi di atas minimum; atau belum ada barang | Periksa/Kelola stok menuju tab Stok |
| Stok pada tab Stok | Nilai persediaan dan jumlah item tetap tersedia | Daftar dan mutasi tetap seperti sekarang |
| BEP aktual | Bulan sumber, status impas, omzet terhadap ambang | Volume produk, biaya, laba setelah penyusutan, R/C |
| Kelayakan rencana | Status, basis waktu, target dan ambang impas | BEP per produk, rasio, laba, modal; kekurangan modal tetap terlihat |
| Produk | Nama, biaya variabel per unit dan margin kontribusi per unit | Harga, target, waktu mulai jual dan resep di editor |
| Form | Label, input, error, hasil yang dibutuhkan untuk menyimpan | Petunjuk panjang di bantuan kontekstual |
| Laporan | Angka, scope/periode, tabel analisis utama | Penjelasan panjang dan asumsi dapat dibuka |
| Impor | File, jumlah baris, total, kategori baru, duplikat, seluruh konflik | Metadata teknis; tombol Simpan mengikuti guard yang ada |

**Aturan keuangan:** Jangan mengubah rumus, menggunakan `Float` untuk nominal uang, atau menyimpulkan usaha aman dari saldo positif. Jangan menamai biaya variabel resep sebagai HPP penuh: perhitungan ini belum mengalokasikan seluruh biaya tetap. Bila memakai istilah HPP, jelaskan cakupannya. Laba kas tidak diganti laba setelah penyusutan, dan data seluruh periode tidak diberi label Bulan ini.

**Aturan visual:** Satu fokus utama per kartu, paling banyak dua metrik pendukung, satu CTA utama; disclosure adalah kontrol sekunder. Tetap memakai palet `MetricAccent` dan token semantik yang ada. Angka rupiah lengkap, label dapat membungkus, tidak mengecilkan font demi muat. Loss/deficit, input gagal, data tidak cukup, harga di bawah biaya, kekurangan modal, dan konflik impor tidak diletakkan di konten tertutup.

Contoh untuk fixture satu bulan, bukan nilai statis di aplikasi:

```text
Saldo kas · Seluruh catatan
Rp9.150.000
Rincian kas →

Hasil usaha · Seluruh catatan
Laba kas Rp1.350.000
Pendapatan usaha Rp5.500.000
Biaya usaha Rp4.150.000
Lihat perhitungan ▾

Stok
1 barang menipis
Periksa stok →
```

Di fixture tersebut laba setelah penyusutan adalah Rp1.150.000 untuk bulan sumber. Nilai ini hanya boleh ditampilkan dari `actualMonth` dengan label bulannya, bukan dikurangkan dari summary seluruh waktu di UI.

## Peta file dan kepemilikan

Seluruh path berikut relatif terhadap root `D:\Arina Agri\Arina Agri Kotlin`.

| File | Tanggung jawab perubahan |
| --- | --- |
| `app/src/main/java/com/mankeu/presentation/components/CardDetails.kt` (baru) | Disclosure informasi sekunder, state dan aksesibilitas |
| `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt` | Hero, ringkasan hasil usaha, tiga pengeluaran terbesar |
| `app/src/main/java/com/mankeu/presentation/components/PlanCards.kt` | Ringkasan/detail kelayakan dan BEP bersama |
| `app/src/main/java/com/mankeu/presentation/screens/StockSection.kt` | Varian ringkas khusus Beranda, ringkasan Stok tetap lengkap |
| `app/src/main/java/com/mankeu/presentation/screens/PlanScreen.kt` | Ringkasan produk dan bantuan editor |
| `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt` | Bantuan form tanpa menyembunyikan input atau error |
| `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt` | Call site BEP dan penjelasan sekunder |
| `app/src/main/java/com/mankeu/presentation/screens/MoreScreen.kt` | Hierarki preview impor tanpa mengubah guard/state |
| `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` | Copy ID/EN, label tindakan dan expanded/collapsed |
| `app/src/androidTest/java/com/mankeu/presentation/CardDetailsTest.kt` (baru) | Kontrak disclosure |
| `app/src/androidTest/java/com/mankeu/presentation/CardSimplificationTest.kt` (baru) | Kontrak card, peringatan, scope, fixture |
| `app/src/androidTest/java/com/mankeu/presentation/ImportCardLayoutTest.kt` (ada) | Regresi layout dan guard impor |
| `docs/UX_AND_ACCESSIBILITY.md`, `logs/ACTIVITY_LOG.md`, `logs/CHANGELOG.md` | Standar informasi dan bukti implementasi |

Re-use `FinancialSummary`, `PlanAnalysis`, `ActualMonthAnalysis`, `StockCalculation.summarize`, `PlanAssembly.products`, `toRupiah`, `ReportRow`, `MankeuStatusBadge`, `LocalMotion`, dan `MankeuTransitions`. Tidak mengubah schema Room, parser Excel, export, akun, dependensi, tab navigasi, atau desain brand. `SharedComponents.kt` tidak perlu dirombak hanya karena grid di Beranda diganti.

## Urutan pelaksanaan

### Task 1: Kunci baseline, makna angka, dan periode

**Files:** baca `HomeScreen.kt`, `AppViewModel.kt`, `domain/PostingReportCalculator.kt`, `domain/BusinessPlan.kt`, `data/PlanAssembly.kt`; buat `docs/qa/2026-10-24-card-simplification/README.md` saat eksekusi.

- [ ] Jalankan `rtk git status --short` dan `rtk git diff --cached --name-only`. Catat perubahan awal; jangan commit perubahan UI/motion milik sesi lain. Pastikan build/test memakai working tree yang akan diverifikasi dan catat commit serta daftar dirty files.
- [ ] Jalankan baseline unit test secara serial: `rtk proxy .\gradlew.bat testDebugUnitTest --console=plain`. Expected: `BUILD SUCCESSFUL`; bila gagal sebelum perubahan, rekam penyebabnya dan pisahkan dari regresi baru.
- [ ] Gunakan fixture sintetis dari `UiRefinementWorkflowTest.seedFixture`: saldo 9.150.000; pendapatan 5.500.000; biaya kas 4.150.000; laba kas 1.350.000; penyusutan 200.000; laba sesudah penyusutan 1.150.000. Pertahankan fixture di pemilik test yang ada; buat variasi ringan dalam test baru tanpa memindahkan seluruh harness.
- [ ] Tambahkan skenario dua bulan dengan penjualan/biaya yang berbeda. Pastikan summary seluruh catatan berbeda dari `actualMonth`; ini mendeteksi salah label bulan maupun sumber angka, bukan sekadar kesamaan fixture satu bulan.
- [ ] Catat basis sumber: `state.summary` adalah selected book seluruh catatan; `plan.summary` seluruh periode rencana; `actualMonth.month` adalah bulan analisis. Untuk Proyeksi yang tidak mempunyai plan, pakai summary book dan label seluruh catatan. Jangan membuat filter periode baru.
- [ ] Ambil baseline Beranda, BEP, produk, Stok, Laporan dan preview impor dari build yang diuji. Hindari data finansial pribadi. Tulis lokasi, scope, mode dan versi APK pada QA README; jangan memakai screenshot lama sebagai bukti paritas build baru.

**Acceptance:** nilai dan basis periode terdokumentasi; tidak ada test baseline yang diam-diam dianggap lulus. Checkpoint pertama sebelum perubahan tampilan.

### Task 2: Buat disclosure native yang dapat diakses

**Create:** `app/src/main/java/com/mankeu/presentation/components/CardDetails.kt`, `app/src/androidTest/java/com/mankeu/presentation/CardDetailsTest.kt`.
**Modify:** kedua `strings.xml`.

- [ ] Tambahkan resources berikut pada locale ID dan padanan EN (`Show calculation`, `Hide calculation`, `Expanded`, `Collapsed`). Jangan menimpa string lain yang masih dipakai.

```xml
<string name="card_show_calculation">Lihat perhitungan</string>
<string name="card_hide_calculation">Sembunyikan perhitungan</string>
<string name="card_expanded">Terbuka</string>
<string name="card_collapsed">Tertutup</string>
```

- [ ] Tulis test kontrak di `CardDetailsTest`: konten tidak ada saat awal; klik menampilkan; klik lagi menghilangkan; `StateRestorationTester.emulateSavedInstanceStateRestore()` mempertahankan kondisi dalam scope yang sama; mengganti `stateKey` menutup rincian. Jangan menyimpan state global atau preference permanen.
- [ ] Implementasikan komponen kecil berikut. Disclosure instan sudah cukup untuk kebutuhan ini; navigasi dan meter tetap memakai motion existing. Tidak menambah API transisi baru.

```kotlin
package com.mankeu.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mankeu.R

@Composable
fun CardDetails(
    stateKey: String,
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var open by rememberSaveable(stateKey) { mutableStateOf(false) }
    val description = stringResource(
        if (open) R.string.card_expanded else R.string.card_collapsed,
    )
    Column(modifier.fillMaxWidth()) {
        TextButton(
            onClick = { open = !open },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { stateDescription = description },
        ) {
            Text(if (open) hideLabel else showLabel)
        }
        if (open) content()
    }
}
```

Versi awal di atas sengaja memakai disclosure instan, yang aman pada Full/Reduced/Off. Untuk scope plan ini, **gunakan versi instan tersebut**; tidak perlu menambah animasi baru atau mengubah `ContentTransitions.kt`. Animasi navigasi dan meter yang sudah ada tetap memakai token yang ada. Dengan ini tidak ada API motion baru yang perlu ditebak.

- [ ] Gunakan class test berikut untuk kontrak dasar, restoration dan scope. Assertion TalkBack dapat ditambahkan memakai `SemanticsProperties.StateDescription` serta string resource dari target context.

```kotlin
package com.mankeu.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.presentation.components.CardDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDetailsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun detailsAreHiddenUntilRequestedAndCanBeClosed() {
        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails("unit-a:actual", "Lihat perhitungan", "Tutup") {
                    Text("Arus operasi Rp1.350.000")
                }
            }
        }
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertDoesNotExist()
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertIsDisplayed()
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithText("Arus operasi Rp1.350.000").assertDoesNotExist()
    }

    @Test
    fun restorationPreservesDetailsWithinTheSameScope() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails("unit-a:actual", "Lihat", "Tutup") { Text("Rincian A") }
            }
        }
        compose.onNodeWithText("Lihat").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Rincian A").assertIsDisplayed()
    }

    @Test
    fun changingScopeClosesDetails() {
        var scope by mutableStateOf("unit-a:actual")
        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                CardDetails(scope, "Lihat", "Tutup") { Text("Rincian") }
            }
        }
        compose.onNodeWithText("Lihat").performClick()
        compose.onNodeWithText("Rincian").assertIsDisplayed()
        compose.runOnIdle { scope = "unit-b:projection" }
        compose.onNodeWithText("Rincian").assertDoesNotExist()
    }
}
```

- [ ] Compile kedua APK dan jalankan class baru sesuai perintah Task 8. Expected: test kontrak lulus. Periksa TalkBack menyebut tombol dan state; bukan hanya icon chevron. Header card jangan sekaligus menjadi toggle yang menduplikasi aksi tombol.
- [ ] Update activity log dan commit hanya komponen, resources, test, dan entry log task ini: `feat(ui): add accessible secondary card details`.

**Acceptance:** konten sekunder dapat dicari melalui label, tidak ada kebutuhan gesture tersembunyi, expanded state tidak bocor antar unit/mode. Jangan memakai komponen ini untuk membungkus input editable: konten tertutup dilepas dari composition.

### Task 3: Ringkas saldo dan gabungkan hasil usaha Beranda

**Modify:** `HomeScreen.kt`, kedua `strings.xml`.
**Create:** `CardSimplificationTest.kt`.

- [ ] Tulis test Home dengan `AppUiState(isLoading = false, ...)` yang mempunyai usaha/unit, summary fixture dan `actualMonth`. Assert angka saldo dan laba kas tampil sejak awal; definisi dan arus operasi tidak tampil sebelum disclosure dibuka; setelah dibuka arus operasi benar. Tambahkan kasus rugi kas dan dua bulan dari Task 1.
- [ ] Pada `HeroCard`, hapus paragraf definisi, badge mode yang mengulang header, dan trend bulanan yang dicampur dengan saldo seluruh waktu. Pertahankan label mode pada konteks layar dan basis periode di kartu. Defisit tetap badge terlihat; jangan menyebut kas aman. Hapus parameter `latestMonth` jika tidak lagi digunakan, berikut argumen call site dan imports yang benar-benar tidak terpakai. CTA menjadi string resource `home_cash_details` (ID `Rincian kas`, EN `Cash details`).
- [ ] Ganti blok `SimpleMetricGrid` di Home dengan satu card hasil usaha. Gunakan binding berikut, tidak hitung ulang dari `state.events` yang merupakan potongan ledger.

```kotlin
val plan = state.plan.takeIf { state.selectedBookMode == BookMode.PROJECTION }
val summary = plan?.summary ?: state.summary
val detailsKey = "${state.selectedBusinessId}:${state.selectedProjectId}:${state.selectedBookMode}"
```

Di dalam card, tampilkan `summary.profitRupiah.toRupiah()` sebagai headline dan label resource `Laba / rugi kas` (atau `Proyeksi laba / rugi kas` pada Proyeksi), lalu dua `ReportRow` untuk income/expense. Sediakan `CardDetails` dengan struktur berikut; `home_cash_profit_help` menjelaskan kas sebelum penyusutan dalam satu kalimat, bukan hasil analisis baru.

```kotlin
ReportRow(stringResource(R.string.reports_operating_income), summary.operatingIncomeRupiah)
ReportRow(stringResource(R.string.reports_operating_expense), summary.operatingExpenseRupiah)
CardDetails(
    stateKey = detailsKey,
    showLabel = stringResource(R.string.card_show_calculation),
    hideLabel = stringResource(R.string.card_hide_calculation),
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportRow(stringResource(R.string.home_cash_flow), summary.operatingCashFlowRupiah)
        Text(stringResource(R.string.home_cash_profit_help))
    }
}
```

- [ ] Tampilkan `Seluruh catatan` pada saldo/hasil kas book; untuk `plan.summary` format rentang `plan.months.first()` sampai `last()` dengan `appLocale()`. Jangan memakai `actualMonth.month` sebagai periode `summary`. Jangan mengurangi `actualMonth` depreciation dari summary seluruh waktu.
- [ ] Pertahankan `TourTarget.HERO_SUMMARY`, `rememberTourListState`, CTA Reports, skeleton, setup empty states dan urutan section. Cek pergantian mode/unit saat rincian terbuka: label serta angka harus satu snapshot, rincian kembali tertutup pada scope baru.
- [ ] Run class `CardSimplificationTest` dan bandingkan screenshot Home sebelum/sesudah. Expected: angka utama cocok dengan fixture, empat metrik setara hilang dari Home, tidak ada string hardcoded ID di UI. Catat log/changelog lalu commit `feat(ui): simplify home cash and business result cards`.

**Acceptance:** pengguna dapat membaca saldo, hasil usaha, dan basis periode tanpa membuka detail; rugi/defisit tidak tersembunyi. Tidak ada perubahan domain.

### Task 4: Pengeluaran tiga baris dan stok yang berorientasi tindakan

**Modify:** `HomeScreen.kt`, `StockSection.kt`, kedua `strings.xml`, `CardSimplificationTest.kt`.

- [ ] Tambah test pengeluaran 0, 1, 3, dan 5 kategori; tiga terbesar terlihat saat awal, kategori keempat/kelima hanya setelah Lihat semua, nominal total tidak berubah. Tambah test kategori nama panjang dan nominal negatif: nominal tetap terlihat dengan tanda yang benar, tanpa proporsi pie negatif.
- [ ] Di `ExpenseDistributionCard`, pertahankan pengambilan data dan grouping existing `categoryTotals`/fallback projection. Jangan menamai total ini `Biaya operasional`: ia dapat mencakup kategori kas lain. Perbaiki daftar dependency `remember` agar menyertakan `state.categories` yang dipakai fallback. Setelah `expenseCategories` dihitung, render tiga baris nominal; tidak perlu donut dan legend yang mengulang data.

```kotlin
expenseCategories.take(3).forEach { slice ->
    ReportRow(slice.category, slice.amount, highlightNegative = false)
}
```

Untuk `expenseCategories.size > 3`, gunakan `CardDetails` berlabel `Lihat semua kategori` / `Tampilkan lebih sedikit`. Konten disclosure merender **hanya** `expenseCategories.drop(3)` dengan `ReportRow`, bukan mengulang tiga pertama. Urutkan nominal menurun dengan category sebagai tie-break agar hasil stabil. Total memakai semua kategori, bukan tiga teratas. Ubah guard existing `if (total == 0L) emptyList()` menjadi cek map kategori kosong, supaya data koreksi dengan net nol tidak dihilangkan. Hapus property percentage dan color pada `ExpenseSlice` bila tidak lagi dipakai; data cukup category dan amount. Bila total nol/negatif karena koreksi, tampilkan jumlah bertanda dan penjelasan singkat; jangan membuat persen, bar, atau tren fiktif.

- [ ] Ubah signature `StockCard(items, onManage, compact: Boolean = false)`. Call Home memakai `compact = true`; call tab Stok tetap default. Di compact, headline dari `StockCalculation.summarize(items)` adalah count menipis; jika kosong gunakan `Belum ada barang`, bukan status aman. Nilai persediaan tetap terlihat di tab Stok, bukan dihapus dari aplikasi. Berikut keputusan branch yang digunakan:

```kotlin
when {
    items.isEmpty() -> Text(stringResource(R.string.stock_empty))
    summary.lowStockCount > 0 -> MankeuStatusBadge(
        label = stringResource(R.string.stock_low_count, summary.lowStockCount),
        intent = SemanticIntent.WARNING,
    )
    else -> Text(stringResource(R.string.stock_all_ok))
}
```

Branch tersebut menggantikan ringkasan nilai **hanya ketika compact**. Pertahankan CTA `onManage`, status per-item Habis/Menipis pada tab Stok, dan scope stok bersama/per-unit yang sudah dimiliki domain. Jangan mengaitkan stok ke book Proyeksi/Realisasi.

- [ ] Run `CardSimplificationTest`: 5 kategori dapat diakses lengkap; list Stok lengkap dan callback menuju Stok bekerja; warning tetap terlihat saat rincian lain tertutup. Catat log/changelog dan commit `feat(ui): simplify expense and home stock summaries`.

**Acceptance:** card lebih ringkas tanpa kehilangan akses ke kategori/valuasi. Tidak mengubah definisi pengeluaran atau ambang minimum stok.

### Task 5: Ringkasan BEP bersama dan detail kelayakan

**Modify:** `components/PlanCards.kt`, `HomeScreen.kt`, `PlanScreen.kt`, `ReportsScreen.kt`, kedua `strings.xml`, `CardSimplificationTest.kt`.

- [ ] Test `PlanFeasibilityCard` dan `ActualMonthCard` langsung dengan analisis sintetis: impas tercapai, belum tercapai, tanpa penjualan, BEP null, BEP nol, produk rugi, kekurangan modal. Assert warning tidak perlu membuka disclosure; rasio dan volume baru terlihat sesudah dibuka.
- [ ] Tambahkan parameter terakhir `detailsKey: String = "plan"` pada `PlanFeasibilityCard` dan `detailsKey: String = "actual"` pada `ActualMonthCard` agar call site lama tetap kompatibel. Semua call site layar mengirim scope dari selected business/project/mode; actual menambahkan `actual.month`. Panggil `CardDetails` sekali per kartu.
- [ ] Di `PlanFeasibilityCard`, pertahankan `CardHeader`, status, basis periode/seasonal, meter dan target omzet terlihat. Pindahkan `ReportRow` BEP per produk, laba, R/C, B/C dan uraian modal ke disclosure. **Pisahkan terlebih dahulu** `sellsAtALoss` warnings dan `capitalShortfallRupiah > 0` warning agar selalu terlihat di luar disclosure; nama produk yang rugi wajib disebut.
- [ ] Di `ActualMonthCard`, pertahankan bulan `actual.month`, omzet dan ambang impas terlihat; pindahkan `unitsSold`, biaya, laba setelah penyusutan dan R/C ke disclosure. Bila `actual.profitRupiah < 0`, tampilkan pula peringatan kerugian setelah penyusutan dan nominalnya di luar disclosure, menyebut bulan sumber. Pada plan, `plan.profitRupiah < 0` juga wajib warning terlihat dengan basis per-bulan rencana. Jadi laba kas positif tidak menutupi kerugian yang dihitung domain. Status headline memakai `actual.reached`, bukan menyebut kelayakan keseluruhan usaha dari bulan itu. Variance yang menunjukkan biaya melebihi standar tetap warning singkat di luar disclosure.
- [ ] Hindari meter palsu untuk ambang null/nol. Penggantian guard meter:

```kotlin
val bep = actual.breakEvenRupiah
if (bep != null && bep > 0L) {
    MeterRow(
        label = stringResource(R.string.actual_revenue),
        value = actual.revenueRupiah.toRupiah(),
        progress = progressTo(actual.revenueRupiah, bep),
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        barColor = if (actual.reached == true) semantic.success else MaterialTheme.colorScheme.tertiary,
        supporting = if (actual.reached == true) {
            stringResource(R.string.actual_reached, bep.toRupiah())
        } else {
            stringResource(R.string.actual_short, (bep - actual.revenueRupiah).toRupiah(), bep.toRupiah())
        },
    )
} else {
    ReportRow(stringResource(R.string.actual_revenue), actual.revenueRupiah)
    Text(stringResource(R.string.actual_no_bep))
}
```

Untuk tanpa penjualan, pertahankan penjelasan existing `actual_no_sales`; untuk BEP nol, label `Tanpa ambang impas positif` bila analisis memang menghasilkan nol, bukan `100% tercapai`. Terapkan guard positif yang sama pada plan. `progressTo` hanya untuk progress display; nominal tetap `Long`, hasil mencapai/tidak memakai domain. Pastikan `MeterRow` langsung menuju nilai akhir bila motion Reduced/Off, tanpa perjalanan bar yang diperkecil durasinya saja.

- [ ] Pastikan `PlanBreakdownCard` pada Laporan tetap dapat dibaca sebagai tabel lengkap; jangan membuat accordion bertingkat untuk setiap baris. Periksa semua call site melalui `rtk proxy rg -n "PlanFeasibilityCard\(|ActualMonthCard\(" app/src/main/java`.
- [ ] Run test card dan `BusinessPlanTest`, `PlanAssemblyTest`. Expected: seluruh hasil domain sama, critical warnings tersedia dalam default view di ketiga layar. Update standar UX dan log/changelog; catat ADR hanya bila keputusan arsitektur benar-benar berubah. Commit `feat(ui): expose clear break-even summaries with optional detail`.

**Acceptance:** status impas tidak disamakan dengan sehat/layak menyeluruh; rincian dan sumber keuangan tetap tersedia.

### Task 6: Sederhanakan produk, bantuan form, dan penjelasan laporan

**Modify:** `PlanScreen.kt`, `EntryScreen.kt`, `ReportsScreen.kt`, kedua `strings.xml`, `CardSimplificationTest.kt`.

- [ ] Pada list produk, gunakan `PlanAssembly.products(listOf(row)).first()` sebagai sumber `variableCostPerUnit`/`contributionMargin`. Pertahankan pembulatan existing `HALF_UP` untuk biaya variabel satuan; margin dibulatkan dari nilai `contributionMargin` domain dengan `setScale(0, RoundingMode.HALF_UP).toLong().toRupiah()`, bukan `price - biaya yang sudah dibulatkan`. Tampilkan nama, `Biaya variabel / unit`, `Margin kontribusi / unit`. Harga/target/waktu jual tetap tersedia saat tap editor. Warning margin tidak positif memakai nilai BigDecimal asli `contributionMargin.signum() <= 0`; `sellsAtALoss` dimiliki `ProductBep`, bukan `PlanProduct`. Jangan menyebut margin kontribusi sebagai laba bersih.
- [ ] Pertahankan `PlanRow` yang dapat diklik; ubah hanya isi `subtitle`/`detail` dari rangkaian harga, target dan biaya menjadi dua nilai unit tadi. Gunakan resource format dengan `%1$s`/`%2$s` untuk unit/nominal di ID dan EN. Tidak membuat disclosure per produk karena editor yang ada sudah merupakan tujuan rincian.
- [ ] Pada `ProductSheet` dan form Entry, pindahkan **hanya paragraf petunjuk** ke `CardDetails` berlabel `Bantuan pengisian`. Input nama, unit, harga, target, komponen resep, hasil hitung dan error tetap composed di luar disclosure. Key bantuan berdasarkan scope, editor product id/jenis entry; jangan memakai toggle untuk melepas textfield.
- [ ] Pada `FinancialsSection`, angka pendapatan, biaya dan `summary.profitRupiah` tetap tampil. Ubah label laba menjadi laba/rugi kas agar konsisten; pindahkan `reports_income_note` dan uraian working capital panjang ke rincian terpisah dalam masing-masing card. Jangan menyembunyikan seluruh tabel finansial, grafik arus kas yang dipilih pengguna, atau basis rata-rata di `ComparisonSection`.
- [ ] Test workflow: edit nama/harga/komponen, buka/tutup bantuan, ubah tema, rotasi, lalu simpan; draft dan hasil tidak berubah. Nama panjang harus membungkus saat perlu; target 48dp dan font 200% tetap layak. Test Laporan menunjukkan nilai fixture dan basis per-bulan/periode yang benar.
- [ ] Run `CardSimplificationTest` dan regresi `VisualSystemTest` untuk preservation draft/tema. Catat log/changelog; commit `feat(ui): clarify product metrics and contextual form guidance`.

**Acceptance:** informasi lebih pendek tetapi input, error, aksi edit dan laporan lengkap tetap dapat ditemukan. Tidak mengubah hasil HPP/BEP.

### Task 7: Preview impor yang ringkas tetapi tetap transparan

**Modify:** `MoreScreen.kt`, kedua `strings.xml`, `ImportCardLayoutTest.kt`.

- [ ] Tambah kasus Ready dengan `previouslyImported = true` serta `conflicts = listOf("Produk Kopi Susu mempunyai resep berbeda")`: peringatan duplikat dan seluruh konflik harus tampil sebelum Simpan; tombol tidak enabled; callback commit tetap nol. Tambah kasus Ready valid: simpan memanggil callback sekali.
- [ ] Dalam branch `is ImportUiState.Ready`, tampilkan `preview.sourceName`, counts existing, total income/expense dan periode yang tersedia. Header/subtitle memakai satu kalimat singkat; tidak mengulang petunjuk tiga langkah yang sudah terlihat di step indicator. Jangan menyembunyikan nama kategori baru, stok yang dilewati, warning duplikat atau daftar konflik.
- [ ] Jika ada metadata teknis nonkritis yang memang sudah ditampilkan, pindahkan ke `CardDetails(stateKey = preview.sourceSha256, ...)`; jangan menambah baris hash/provenance hanya untuk mengisi accordion. Semua yang sudah cukup ringkas tetap seperti sekarang. Jangan membuat state import baru.
- [ ] Pertahankan persis guard dan satu root Column dalam lambda `AnimatedContent`, dengan pembacaan dari `displayState`:

```kotlin
Button(
    onClick = onCommit,
    enabled = preview.canCommit && review.conflicts.isEmpty(),
    modifier = Modifier.fillMaxWidth(),
) {
    Text(stringResource(R.string.import_commit, review.addedRowCount))
}
```

- [ ] Failure tetap berisi exact problem dan akses download template; Ready tidak menulis database; Committed menampilkan hasil/notice, pembatalan batch existing tetap tersedia. Jalankan `ImportCardLayoutTest`, `TemplateImportTest` dan `ImportCommitTest` untuk preview → commit → reversal menggunakan data sintetis. Expected: duplikat diperingatkan, konflik memblokir, valid import atomic/reversible.
- [ ] Catat log/changelog lalu commit `feat(ui): streamline import review without hiding warnings`.

**Acceptance:** simplifikasi tidak mengurangi informasi yang diperlukan sebelum keputusan penyimpanan dan tidak menghidupkan kembali perilaku enrichment lama.

### Task 8: QA lintas fitur, bukti visual, dan handoff

**Modify:** `CardSimplificationTest.kt`, `docs/qa/2026-10-24-card-simplification/README.md`, `docs/UX_AND_ACCESSIBILITY.md`, logs yang relevan.

- [ ] Jalankan pemeriksaan statis dan build di bawah secara serial. Gunakan JDK dan non-secret debug configuration yang sudah didokumentasikan; jangan membuat Firebase project atau akun baru. Expected: verifier exit 0, Gradle `BUILD SUCCESSFUL`, diff check kosong.

```powershell
rtk proxy python scripts/verify_visual_system.py
rtk proxy .\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console=plain
rtk git diff --check
```

- [ ] Jalankan focused instrumentation pada satu emulator yang sudah boot. Dengan satu target `emulator-5554`, gunakan contoh berikut; sesuaikan serial hanya berdasarkan `adb devices`, bukan asumsi perangkat. Dua class Card* adalah target baru pada plan, bukan test yang sudah ada.

```powershell
rtk proxy adb devices
rtk proxy adb -s emulator-5554 shell getprop sys.boot_completed
rtk proxy .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.CardDetailsTest,com.mankeu.presentation.CardSimplificationTest,com.mankeu.presentation.ImportCardLayoutTest,com.mankeu.presentation.VisualSystemTest,com.mankeu.data.ImportCommitTest" --console=plain
```

Jika perangkat lain juga terhubung, set environment **hanya untuk proses QA** sebelum Gradle: `$env:ANDROID_SERIAL = 'emulator-5554'`. Jangan menguji beberapa Gradle invocation bersamaan. Expected: tidak ada failure/skip pada kasus wajib; simpan report aktual `app/build/reports/androidTests/connected/debug/index.html` bila dihasilkan. Build sukses saja tidak membuktikan workflow atau aksesibilitas.

- [ ] Jalankan alur: offline tanpa login → pilih usaha/unit → baca saldo/laba → buka/tutup rincian → ganti mode → ganti unit → buka Rencana/edit produk/bantuan → simpan → buka Laporan/BEP → Stok/mutasi → impor preview konflik → impor valid → batalkan batch. Cocokkan angka di Beranda dan Laporan dengan sumber yang sama; tidak boleh mengambil total dari ledger yang dipotong untuk tampilan.
- [ ] Periksa state kosong, loading, rugi, nol, data belum lengkap, nama panjang dan nominal besar. Periksa light/dark, ponsel 360dp dan tablet, font 100%/200%, ID/EN, Full/Reduced/Off, TalkBack/focus/Back. Reduced/Off tidak menambahkan spatial travel atau autoplay dekoratif. Disclosure tidak mengembalikan list ke awal dan tidak membuka state unit lain.
- [ ] Simpan screenshot before/after dengan scope/periode/mode setara serta rincian open/closed. Buat tabel bukti: scenario, expected, actual, pass/fail, path capture/test report. Jangan mengganti 91 capture simulasi lama; bukti baru mempunyai folder sendiri.
- [ ] Lakukan review pemahaman tiga pertanyaan: saldo berapa, hasil usaha untung/rugi untuk periode mana, apa yang perlu ditindaklanjuti. Evaluasi internal memberi bukti review desain; klaim peningkatan pemahaman pengguna baru boleh dibuat setelah pengujian pengguna representatif dengan hasil tercatat.
- [ ] Review diff terhadap baseline Task 1; tidak ada perubahan domain/schema/dependency atau file web read-only. Update activity log dengan perintah/hasil nyata, changelog `[Unreleased]` untuk implementasi, serta ADR hanya untuk perubahan keputusan material. Stage hanya file task dan entry log milik pekerjaan ini; jangan `git add .`. Commit `test(ui): verify simplified card workflows and accessibility`.

**Acceptance:** seluruh area di tabel kontrak tercakup, nilai tidak berubah, detail bisa ditemukan, peringatan tetap terlihat. QA menyebutkan secara eksplisit jika TalkBack, tablet, user study atau upgrade APK belum diverifikasi; jangan menyebut release-ready hanya dari screenshot.

## Risiko dan rollback

| Risiko | Pencegahan |
| --- | --- |
| Summary semua waktu diberi bulan terkini | Label periode dari sumber; fixture dua bulan |
| Laba kas tertukar dengan laba setelah penyusutan | Binding berbeda dan test nilai yang sengaja tidak sama |
| Detail menyembunyikan kerugian/konflik | Warning dirender di luar disclosure dan diuji saat tertutup |
| Draft hilang karena konten tidak composed | Disclosure hanya bantuan; seluruh input di luar |
| State rincian bocor antar unit | `rememberSaveable(stateKey)` dan test switch scope |
| Perbaikan bersama mengubah semua layar tanpa disadari | Verifikasi tiga call site BEP dan dua call site Stok |
| Dirty work tersertakan commit | Stage path/hunk milik task; preserve changes awal |

Rollback cukup revert commit implementasi yang bersangkutan bersama log koreksinya; tidak ada migrasi database atau transformasi data. Jangan melakukan reset checkout karena ada pekerjaan UI/motion lain yang belum commit.

## Verifikasi dokumen plan

```powershell
rtk proxy python scripts/qa/verify_card_simplification_plan.py
rtk git diff --check
```

Expected: delapan task berurutan, empat screenshot lokal tersedia, tiga hash sumber cocok, seluruh link lokal valid, code fences seimbang. Ini memverifikasi dokumen dan bukti referensi saja; perintah build/test di Task 1–8 baru dijalankan ketika implementasi dimulai.
