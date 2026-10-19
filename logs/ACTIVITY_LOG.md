# Activity Log

### [2026-10-19 01:05] [Senior Android & Financial Architect]
- **Goal**: Remove redundant transaction records from Beranda, overhaul BEP display into explicit dual BEP Unit & BEP Rupiah metrics, make BEP calculations intuitive with a live calculator dialog, and verify BEP test suite.
- **Actions**:
  1. Purged redundant recent transactions section from `HomeScreen.kt` (`home_recent`, `EmptyCard`, `EventCard` preview list, and `Lihat Semua Catatan` button). Beranda now focuses solely on cash pulse, net cash flow, and cost distribution, completely eliminating duplicate transaction listings between Beranda and Catatan.
  2. Overhauled BEP presentation in `FeasibilityCard.kt` and `ReportsScreen.kt`:
     - Displayed side-by-side metric tiles: **BEP Volume (Unit)** (e.g. `1.363 unit`) and **BEP Nilai (Uang / Nominal Rupiah)** (e.g. `Rp136.306.815`).
     - Decoupled BEP display from `targetQuantity != null && targetQuantity > 0`, ensuring owners immediately see both BEP Unit and BEP Rupiah once unit price, variable cost, and fixed cost are known, even before setting a speculative sales target.
     - Added comparison progress meter `Target Penjualan vs BEP` when a target quantity is entered.
     - Added `ReportRow(label: String, value: String, emphasized: Boolean)` overload in `SharedComponents.kt` to cleanly render non-currency units.
  3. Redesigned BEP navigation & interactive calculation experience:
     - Renamed Tab 2 in `ReportsScreen.kt` to **"Kelayakan & BEP"** (`reports_tab_feasibility`).
     - Renamed card title to **"Kelayakan & Titik Impas (BEP)"** with prominent action button `[ 🧮 Kalkulator & Parameter BEP ]` (`Icons.Default.Calculate`).
     - Restructured `AssumptionsDialog` into a dedicated **"Kalkulator BEP & Parameter"** bottom sheet featuring an interactive **Live Calculation Preview Card** that recalculates and previews BEP Unit, BEP Rupiah, and Contribution Margin % in real time as the user types.
     - Logically split inputs into `1. Parameter Utama BEP (Titik Impas)` (Harga Jual, Biaya Variabel/HPP, Biaya Tetap Bulanan, Satuan Produk, Target Volume) and `2. Kas & Modal Usaha (Opsional)` (Saldo Kas Awal, Modal Sendiri, Biaya Pembiayaan).
  4. Expanded automated domain tests in `BusinessAnalysisTest.kt`:
     - Added `BEP calculates both unit and rupiah even when target quantity is null` (Warung Bakso scenario: P=20.000, VC=9.000, FC=15.000.000 -> BEP Unit=1.364, BEP Rupiah=27.272.727).
     - Added `BEP returns null when price is less than or equal to variable cost` (loss-making unit margin handling).
     - Added `BEP falls back to operating expense when fixed cost assumption is omitted`.
  5. Verified live on interactive emulator:
     - Ran `./gradlew testDebugUnitTest` (100% pass, 34 tasks, 0 failures).
     - Ran `./gradlew assembleDebug` (16s build, zero errors).
     - Deployed to emulator and captured visual verification of Beranda cleanup, Tab "Kelayakan & BEP", and interactive BEP calculator sheet.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/components/FeasibilityCard.kt`
  - `app/src/main/java/com/mankeu/presentation/components/SharedComponents.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `app/src/test/java/com/mankeu/domain/BusinessAnalysisTest.kt`
  - `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: `./gradlew testDebugUnitTest` passed 100% (34 tasks, 0 failures); `./gradlew assembleDebug` passed; live app verified on running emulator.

### [2026-10-19 00:25] [Senior Android & UX Architect]
- **Goal**: Eliminate visual & navigational redundancies across Beranda, Catatan, Riwayat, and Laporan, and consolidate navigation to 3 clean tabs.
- **Actions**:
  1. Audited process environment and fixed Android emulator interactive foreground launch using Windows Task Scheduler (`schtasks /it` via `LaunchAndroidEmulator`), establishing it as the default launcher mechanism.
  2. Consolidated bottom navigation from 4 tabs down to 3:
     - Dropped `Destination.LEDGER` and deleted `LedgerScreen.kt`.
     - Changed `Destination.ENTRY` icon from `+` (`Icons.Default.Add`) to `Icons.AutoMirrored.Filled.ReceiptLong` (receipt/ledger).
  3. Redesigned `EntryScreen.kt` (Catatan):
     - Removed giant static buttons for "Uang Masuk" and "Uang Keluar" to maximize screen space.
     - Removed "Jenis lain" dropdown, strictly enforcing pure cash-only mental model (`CASH_INCOME` and `CASH_EXPENSE`).
     - Embedded the complete, searchable and filterable transaction ledger directly on the page with real-time text search, filter chips (`Semua`, `Uang Masuk`, `Uang Keluar`), and full reversal/deletion actions.
     - Added empty state card (`EmptyCard`) with feedback ("Belum ada catatan... [Catat Sekarang]") that directly triggers the entry bottom sheet.
  4. Streamlined `ReportsScreen.kt` (Laporan):
     - Removed duplicate `SimpleMetricGrid` (4 metric cards) from `FinancialsSection`.
     - Laporan now immediately displays formal accounting statements (Income Statement, Cash Flow, Monthly Cash Flow Chart) without echoing Beranda's cards.
  5. Optimized `ModalBottomSheet` (Entry Form):
     - Added `SingleChoiceSegmentedButtonRow` at the top of the sheet for instant, effortless toggling between "Uang Masuk" and "Uang Keluar".
  6. Verified against live emulator:
     - Took live screenshots of Beranda, Catatan, Empty State, Bottom Sheet, and Laporan screens.
     - All screens render cleanly with zero visual or functional collisions.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/presentation/MankeuApp.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/LedgerScreen.kt` (Deleted)
  - `logs/DECISION_LOG.md`, `logs/ACTIVITY_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: `./gradlew testDebugUnitTest` passed 100% (34 tasks, 0 failures), `./gradlew assembleDebug` passed in 16s, APK installed and verified live on Android Emulator `Medium_Phone`.

### [2026-10-18 21:05] [Senior Android & Financial Architect]
- **Goal**: Resolve intuitive UX flaws, calibrate financial feasibility analysis across dual simulations, and support inline custom cost categories.
- **Actions**:
  1. Made `HeroCard` on `HomeScreen.kt` interactive by adding an `onClick` callback navigating to `Destination.REPORTS` with `"Rincian Arus Kas →"` visual affordance.
  2. Embedded recent transactions list directly inside `EntryScreen.kt` beneath entry buttons (`state.events.take(5)`), rendering `EventCard` with reversal and delete actions, plus a button navigating to `Destination.LEDGER`.
  3. Added inline on-the-fly custom category creation inside `EntryScreen.kt` via `ChoiceMenu`'s `addLabel`/`onAddAction` (`"+ Tambah Kategori Baru..."`) and `viewModel.createCategory`, dynamically enriching the `ExpenseDistributionCard` donut chart with exact user-defined operational cost categories.
  4. Decoupled R/C Ratio ($TR / TC$, threshold $\ge 1.00$) from B/C Ratio ($Net Profit / TC$, threshold $\ge 0.00$) in `FeasibilityCard.kt` and `BusinessAnalysis.kt`:
     - Fixed revenue blindness by having `BusinessAnalysisCalculator` fall back to `summary.operatingIncomeRupiah` when volume and price are omitted in projection mode.
     - Added `revenueCostRatio` alongside `benefitCostRatio` in `ReportsScreen.kt` and `FeasibilityCard.kt`.
     - Eliminated the bug where profitable CVP models (e.g. Kedai Kopi Berkah with B/C 0.273) were erroneously marked "TIDAK LAYAK".
  5. Purged the dead "UNPAID" filter chip from `LedgerScreen.kt`, and changed the reverse icon in `SharedComponents.kt` from `Redo` to `Icons.AutoMirrored.Filled.Undo`.
  6. Verified against both simulations:
     - **Simulation 1**: CVP Micro MSME Benchmark (*Kedai Kopi & Teh Susu Berkah*: P=10.000, VC=5.000, FC=2.000.000, Q=700 -> BEP=400 units, R/C=1.273, B/C=0.273 -> LAYAK).
     - **Simulation 2**: Sambal Producer Verification Workbook (`simulasi_kelayakan_umkm_verifikasi.xlsx`: 45 RAB lines, 56 transactions, Plan R/C=1.308, Actual R/C=1.117 -> LAYAK).
  7. Ran full verification test suite (`./gradlew testDebugUnitTest`) with 100% pass, and compiled debug APK (`./gradlew assembleDebug`) in 23s.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/domain/BusinessAnalysis.kt`
  - `app/src/main/java/com/mankeu/presentation/components/FeasibilityCard.kt`
  - `app/src/main/java/com/mankeu/presentation/components/SharedComponents.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/LedgerScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/MankeuApp.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: `./gradlew testDebugUnitTest` and targeted tests (`BusinessAnalysisTest`, `VerificationWorkbookTest`) passed completely (`BUILD SUCCESSFUL`), and `./gradlew assembleDebug` passed without error.

### [2026-10-18 19:30] [Senior Android & Financial Architect]
- **Goal**: Full elimination of all debt, credit, obligations, settlements, and installments to enforce a pure Cash-Only architecture.
- **Actions**:
  1. Dropped Room database tables `obligations` and `settlements` with non-destructive `MIGRATION_6_7`. Bumped Room DB schema to version 7.
  2. Removed `ObligationEntity` and `SettlementEntity` from `Entities.kt`, removed unused `ObligationBalanceRow`, and removed all DAO queries for obligations and settlements from `FinanceDao.kt`.
  3. Purged credit and obligation logic from `FinanceRepository.kt`: simplified `recordEvent`, `reverseEvent`, and `deleteEvent` by removing cascading debt locks; deprecated `settleObligation`; decoupled `accountingSummary` and `buildProjectExportBundle` from obligations.
  4. Updated `EncryptedBackupManager.kt` tables list to exclude dropped tables and include `stock_items` and `stock_movements`.
  5. Refactored UI presentation layer:
     - `HomeScreen.kt` & `ReportsScreen.kt`: Replaced receivable/payable cards with pure cash metrics (Kas Operasi, Laba Bersih, Pendapatan Operasional, Biaya Operasional). Removed overdue obligation alerts and `SettlementDialog`.
     - `EntryScreen.kt`: Restricted transaction options strictly to 5 cash-based types (`CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, `ASSET_PURCHASE_CASH`).
  6. Cleaned exporters (`FinancePdfExporter.kt` and `FinanceWorkbookExporter.kt`) by eliminating obligation tables and balance rows.
  7. Fixed unit test suites (`CanonicalRoundTripTest.kt`, `GoldenWorkbookEndToEndTest.kt`, `ReportCalculatorTest.kt`, `VerificationWorkbookTest.kt`) to ensure 100% test pass.
  8. Deployed APK to Android emulator and visually verified UI on device screen.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/data/AppDatabase.kt`
  - `app/src/main/java/com/mankeu/data/Entities.kt`
  - `app/src/main/java/com/mankeu/data/FinanceDao.kt`
  - `app/src/main/java/com/mankeu/data/FinanceRepository.kt`
  - `app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt`
  - `app/src/main/java/com/mankeu/presentation/BusinessWords.kt`
  - `app/src/main/java/com/mankeu/exporter/FinancePdfExporter.kt`
  - `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt`
  - `app/src/main/java/com/mankeu/domain/ReportCalculator.kt`
  - `app/src/main/java/com/mankeu/domain/PostingReportCalculator.kt`
  - `app/src/test/java/com/mankeu/importer/CanonicalRoundTripTest.kt`
  - `app/src/test/java/com/mankeu/importer/GoldenWorkbookEndToEndTest.kt`
  - `app/schemas/com.mankeu.data.AppDatabase/7.json`
  - `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: Executed `./gradlew testDebugUnitTest` (BUILD SUCCESSFUL, 65 tests passed, 0 failed), installed on emulator (`./gradlew installDebug`), and visually verified live in UI.

### [2026-10-18 18:55] [Senior Android & Financial Architect]
- **Goal**: Full purge of loan & interest feature across codebase, Room DB migration 5 -> 6, and stock management database integration.
- **Actions**:
  1. Purged loan and interest fields (`loanAmountRupiah`, `interestRateBasisPoints`, `loanDisbursementEpochDay`, `loanPaymentEpochDay`) from Room entity (`ScenarioAssumptionEntity`), repository (`FinanceRepository`), presentation screens (`HomeScreen`, `ReportsScreen`), view model (`AppViewModel`), PDF exporter (`FinancePdfExporter`), Excel exporter (`FinanceWorkbookExporter`), and unit tests.
  2. Integrated minimal stock management into Room database: added `StockItemEntity` and `StockMovementEntity` to `AppDatabase`, exposed `stockDao()`, bumped database version from 5 to 6, and implemented robust `MIGRATION_5_6` with non-destructive data migration for existing books.
  3. Re-aligned unit test suites (`BusinessAnalysisTest` and `VerificationWorkbookTest`) to verify business calculations without artificial interest loading.
  4. Executed full test suite (`./gradlew testDebugUnitTest`): 35 actionable tasks executed/up-to-date, 0 failures, 100% test pass.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/data/AppDatabase.kt` (modified: added entities, stockDao, version 6, MIGRATION_5_6)
  - `app/src/main/java/com/mankeu/data/FinanceRepository.kt` (modified: exposed stockDao, cleaned assumption saving)
  - `app/src/main/java/com/mankeu/exporter/FinancePdfExporter.kt` (modified: removed loan/interest, added FC/VC)
  - `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt` (modified: removed loan/interest, added CVP & R/C, B/C ratios)
  - `app/src/test/java/com/mankeu/domain/BusinessAnalysisTest.kt` (modified: updated expected outputs)
  - `app/src/test/java/com/mankeu/importer/VerificationWorkbookTest.kt` (modified: updated plan feasibility values)
  - `app/schemas/com.mankeu.data.AppDatabase/6.json` (auto-generated by Room schema export)
  - `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: `./gradlew testDebugUnitTest` passed cleanly (`BUILD SUCCESSFUL in 56s`).

### [2026-10-18 18:20] [Senior Android & Financial Architect]
- **Goal**: Financial domain audit, loan/interest feature deprecation, CVP formula realignment (BEP Unit & Rupiah, R/C & B/C ratio), and minimal stock management design.
- **Actions**:
  1. Identified all occurrences of `loanAmount`, `interestRateBasisPoints`, and loan interest calculations across domain, data, importer/exporter, and presentation layers.
  2. Deprecated loan/interest assumptions in `AnalysisAssumptions` and marked removal roadmap to cleanse domain model from irrelevant micro-MSME debt overhead.
  3. Corrected BEP calculation in `BusinessAnalysisCalculator` using authentic CVP analysis ($BEP_{unit} = FC / (P - VC)$, $BEP_{rupiah} = FC / CMR$) and separated $R/C\ Ratio$ ($TR / TC$) from $B/C\ Ratio$ ($NetProfit / TC$).
  4. Formulated and verified benchmark study case ("Kedai Kopi & Teh Susu Berkah": P=10.000, VC=5.000, FC=2.000.000 -> BEP Unit exact 400, BEP Rupiah 4.000.000, R/C 1.27, B/C 0.27).
  5. Confirmed absence of stock entities in Room DB v5 and designed minimal stock management (`StockItemEntity`, `StockMovementEntity`, `StockDao`, `RecordStockMovementUseCase`, `GetStockSummaryUseCase`).
  6. Implemented and passed automated unit tests for CVP benchmark and stock movement calculations.
- **Files Created/Modified**:
  - `app/src/main/java/com/mankeu/domain/BusinessAnalysis.kt` (modified: CVP formulas, R/C & B/C ratios, deprecation of loan fields)
  - `app/src/test/java/com/mankeu/domain/BusinessAnalysisTest.kt` (modified: added benchmark test suite)
  - `app/src/main/java/com/mankeu/domain/StockManagement.kt` (created: Room schema, DAO, domain models, calculations, and use cases)
  - `app/src/test/java/com/mankeu/domain/StockManagementTest.kt` (created: stock flow & valuation test suite)
  - `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`
- **Verification**: Executed `./gradlew testDebugUnitTest` — 65 passed, 0 failed, full regression integrity maintained.

### [2026-09-16 17:25] [Antigravity]
- **Goal**: Initialize modular logging system and mandatory agent protocols.
- **Actions**: Created `logs/` directory and established `AGENTS.md`.
- **Files Modified**: `AGENTS.md`, `logs/README.md`, `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md`.
- **Verification**: Git status verified.\n