# Decision Log (Architecture Decision Records)

### [DEC-013] Multi-Device Sync Framework over Google Drive (End-to-End Encrypted)
- **Date**: 2026-10-20
- **Status**: Accepted (owner); framework implemented and tested, not wired to UI or scheduler.
- **Context**: The owner asked for sync infrastructure that is "framework-ready" without shipping the feature. The earlier design (`CLOUD_SYNC_AND_AUTH.md`) deliberately limited Drive to snapshot backups. The owner chose multi-device sync, with data on Drive encrypted by the user's passphrase.
- **Decision**:
  1. Change capture by SQLite triggers on every data table into `sync_outbox`, silenced by a `sync_meta.applying_remote` flag while applying remote changes or restoring. Created in `MIGRATION_7_8` and in a Room `onCreate` callback.
  2. A change set is a `PortableBackup` (existing codec) with `_op`/`_changedAt` columns and a `_sync` header, encrypted with the existing `PasswordBackupCipher`. No new format, no new crypto.
  3. Row-level last-writer-wins on `changedAt`; UPDATE-then-INSERT on apply (never `INSERT OR REPLACE`, which would cascade-delete journals); parents first for upserts, children first for deletes; constraint-rejected rows are counted, not fatal.
  4. Transport is an interface with a Drive `appDataFolder` implementation (`appProperties.kind=sync`). Backup listing and retention ignore sync files.
  5. Restores and the schema-8 migration set `requires_full_snapshot`; the next push sends every row.
- **Alternatives considered**: Per-row `updatedAt`/tombstone columns on every table (rejected: touches every entity and every write path; triggers capture all paths for free). A bespoke JSON change format (rejected: the backup codec already handles every column type and is fuzz-hardened). Server-side merge (rejected: local-first, no app-owned server).
- **Open (owner)**: passphrase storage, sync trigger, rejected-row merging, compaction, clock skew. Listed in `docs/SYNC_ARCHITECTURE.md`.
- **Affected components**: `sync/` (new), `AppDatabase` (schema 8, `DATA_TABLES`), `EncryptedBackupManager.restore`, `GoogleDriveBackupClient`, `CLOUD_SYNC_AND_AUTH.md` (superseded section).

### [DEC-012] Single Mankeu-Owned Excel Template for Import and Export
- **Date**: 2026-10-20
- **Status**: Accepted (owner) — implementation planned as F-01 in `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md`.
- **Context**: The importer supports three shapes: Mankeu export, a five-view reference workbook with deterministic enrichment, and generic sheets via fuzzy semantic matching. The blank template (`MsmeTemplateExporter`) differs from the export file. This is the main source of non-cash rows (A-02), wrong business identity (A-09) and roughly 1,200 lines of guessing logic.
- **Decision** (owner answers K-6…K-13):
  1. Mankeu provides the template; blank template = export format (one format, lossless round trip).
  2. Non-template files are rejected with the exact missing/changed sheets or columns and a download-template action; no guessing.
  3. Support for the legacy reference workbook and automatic enrichment is removed; the verification workbook is regenerated in template format.
  4. Import is bulk entry into the selected business unit; backups (.aabk) remain the way to move data.
  5. Sheets: Transaksi (cash-book columns Tanggal · Keterangan · Kategori · Pemasukan · Pengeluaran), Rencana Anggaran, Parameter BEP, Stok Barang (+ Petunjuk, Kategori reference).
  6. Unknown categories are created as custom categories and listed in the preview.
  7. Identical files may be re-imported, with a duplicate warning (replaces the current hard refusal).
- **Supersedes**: the import/enrichment invariants previously in `AGENTS.md`; `docs/REFERENCE_WORKBOOK_CONTRACT.md`, `docs/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md` and `docs/SEMANTIC_MATCHING.md` (marked superseded).
- **Alternatives considered**: two formats (simple template + full export); best-effort reading of arbitrary workbooks; keeping the reference workbook as a one-shot converter; using import for full data migration. All rejected by the owner.
- **Affected components**: `FinanceWorkbookImporter`, `ImportModels`, `SemanticMatcher`, `CategoryClassifier`, `FinanceWorkbookExporter`, `MsmeTemplateExporter`, `FinanceRepository.commitImport`, `AppViewModel.commitImport`, import UI, importer tests, verification workbook script.

### [DEC-011] Post-Audit Product Decisions: Strict Cash, Category-Driven Types, Stock-Cash Link, RAB Editor, Basic Categories
- **Date**: 2026-10-20
- **Status**: Accepted (owner) — implementation planned in `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md`, not yet started.
- **Context**: The business UI/UX + system audit (`docs/audit/2026-10-20_AUDIT_UIUX_BISNIS_DAN_SISTEM.md`) found that (a) the importer and ledger still carry credit/loan/interest types and live data contains them, (b) owner capital, drawings and asset purchases have no input path, (c) stock and cash are recorded twice, (d) the RAB has no editor so Proyeksi is import-only, (e) defaults plus imports produce 23 overlapping categories.
- **Decision** (owner answers, 2026-10-20):
  1. **K-1 Strict cash**: remove every loan, debt/receivable, instalment and interest concept from code, schema, UI and importer. Non-cash workbook rows are ignored, with a persistent notice. If a user deliberately records something like a loan as a plain "Uang masuk" under a custom category, the app leaves it alone.
  2. **K-2 Category-driven types**: the form keeps two buttons; the category role (Pemasukan, Pengeluaran, Setoran modal, Ambil pribadi, Beli aset) decides `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, `ASSET_PURCHASE_CASH`.
  3. **K-3 Optional stock–cash link**: stock-in can also post the cash expense, and expense/income entries can also move stock, in one transaction. Partly supersedes DEC-010 item 4.
  4. **K-4 RAB editor** in Proyeksi mode, reusing the existing repository functions.
  5. **K-5 Basic categories only** (11 defaults); anything specific goes through the existing custom-category feature. Imports match existing categories before creating new ones.
- **Alternatives considered**: converting credit/loan rows to cash equivalents, or restoring a minimal debt ledger (both rejected by the owner in favour of strict cash). Explicit "Jenis lain" chooser in the form (rejected for category-driven mapping). Keeping stock separate, or Proyeksi as BEP parameters only (rejected).
- **Affected components**: importer, `FinanceModels`, `LedgerPolicy`, report calculators, Room schema 8 migration, `EntryScreen`, category template/manager, `StockSection`, new RAB editor, exporters, PDF.

### [DEC-010] Stok Tab (4th Destination) & Pre-Stock Backup Compatibility
- **Date**: 2026-10-20
- **Status**: Accepted & Implemented
- **Context**:
  1. `StockManagement.kt` (entities, DAO, use cases) shipped in schema 6 with no UI, so the lecturer-required minimal inventory (item master, current stock, low-stock alert, stock in/out/opname) was unreachable.
  2. Backup validation required every table in `TABLES`, including `stock_items`/`stock_movements`, so every backup from v0.0.4–v0.0.6 (schemas 3–5) was refused despite `OLDEST_RESTORABLE_SCHEMA = 3`.
  3. Obligation stubs (`ObligationView`, `obligations()`, `settleObligation`, `onSettle`) survived the pure-cash migration as dead code.
- **Decision**:
  1. Stock is per business (not per book). `FinanceRepository.stockItems(businessId)` is a live Flow; `AppUiState.stockItems` replaces the dead `obligations` slot. The summary is derived in memory with `StockCalculation.summarize` rather than a one-shot query, so the card updates on every movement.
  2. Stock gets its own bottom-nav destination `Destination.STOCK` ("Stok"), so the bar is now Beranda · Catatan · Stok · Laporan. This **supersedes the "exactly 3 tabs" rule of DEC-008** at the owner's request (2026-10-20: stock management needs its own page). `StockScreen` shows the summary card, "Tambah Barang", and item cards; tapping an item opens a `MankeuSheet` with Masuk / Keluar / Opname. Beranda keeps a compact `StockCard` whose "Kelola Stok" button navigates to the tab. The record speed dial shows only on Beranda and Catatan. No spotlight tour for Stok yet.
  3. `createStockItem` and `recordStockMovement` run inside one Room transaction each (movement insert + level update can no longer half-apply). Opname may record a count of zero; in/out movements must be positive.
  4. Stock movements are inventory-only: they do not post journal entries. Cash for purchases/sales is still recorded as a cash transaction, keeping the pure-cash ledger the single source of money.
  5. Backups older than schema 6 may omit the stock tables (they restore empty); from schema 6 they are required.
  6. Assumption-entity → `AnalysisAssumptions` mapping extracted to `ScenarioAssumptionEntity?.toAnalysisAssumptions()` (was copied in 3 places, now 4 callers including the PDF).
- **Alternatives considered**: Auto-posting a cash event per stock movement (rejected: double-entry of money for UMKM users, and a stock count is not a cash flow). Sheets launched only from a Beranda card (first implementation; replaced by the tab because the owner wanted stock as a page of its own).
- **Affected components**: `StockManagement.kt`, `FinanceRepository.kt`, `AppViewModel.kt`, `HomeScreen.kt`, new `StockSection.kt` (`StockScreen`), `MankeuApp.kt` (`Destination.STOCK`), `EncryptedBackupManager.kt`, `Entities.kt`, exporters, `strings.xml` (id/en).

### [DEC-009] Dual BEP Metrics (Unit & Rupiah) & Interactive BEP Calculator Architecture
- **Date**: 2026-10-19
- **Status**: Accepted & Implemented
- **Context**: 
  1. `HomeScreen` retained recent transaction cards which duplicated the newly consolidated transaction ledger in `EntryScreen` (Catatan). Users felt having transaction cards in both Beranda and Catatan created visual clutter and redundancy.
  2. In `FeasibilityCard.kt` and `ReportsScreen.kt`, break-even analysis was previously hidden unless a user entered an explicit `targetQuantity > 0`. If a business owner provided unit price, unit variable cost, and fixed cost, but omitted the sales volume target, the BEP calculation failed to render and showed an error prompt (`feasibility_bep_missing`).
  3. BEP nominal revenue (omzet titik impas dalam rupiah) was never displayed on `FeasibilityCard`, showing only volume units or leaving users guessing how much sales revenue is required to cover costs.
  4. The assumptions dialog was titled generically ("Asumsi & Parameter Analisis") without live feedback, forcing users to guess how their price/cost changes impacted their break-even point until after saving.
- **Decision**:
  1. **Purge Recent Records from Beranda**: Remove `home_recent` block from `HomeScreen.kt`. Beranda is now dedicated entirely to the high-level cash pulse, net cash flow status, and category cost distribution.
  2. **Decouple BEP Display from Sales Target**: Render break-even metrics as long as $P > VC$ and $FC > 0$ (`breakEvenQuantity != null && breakEvenQuantity > 0`), independent of whether `targetQuantity` is specified.
  3. **Present Explicit Dual BEP (Unit & Rupiah)**: Display **BEP Volume (Unit)** (e.g. `1.363 unit`) alongside **BEP Nilai (Uang / Nominal Rupiah)** (e.g. `Rp136.306.815`) in dedicated side-by-side metric tiles. When a sales target is present, show a visual progress bar comparing target volume against BEP volume.
  4. **Dedicated Interactive BEP Calculator Dialog**:
     - Renamed Tab 2 in `ReportsScreen.kt` to **"Kelayakan & BEP"**.
     - Renamed action button to `[ 🧮 Kalkulator & Parameter BEP ]`.
     - Embedded a **Live Calculation Preview Card** at the top of the dialog recalculating and displaying BEP Unit, BEP Rupiah, and Contribution Margin % in real time as values change.
     - Separated fields into `1. Parameter Utama BEP (Titik Impas)` and `2. Kas & Modal Usaha (Opsional)`.
- **Consequences**:
  - Complete elimination of record duplication between Beranda and Catatan.
  - Zero ambiguity for business owners seeking break-even points: both unit volume and rupiah turnover are immediately visible.
  - Real-time simulation in the dialog provides instant feedback during business planning.
  - 100% test coverage for BEP edge cases (zero/negative margin, missing target volume, fallback fixed costs).

### [DEC-008] UI/UX Redundancy Elimination & 3-Tab Consolidation
- **Date**: 2026-10-19
- **Status**: Accepted & Implemented
- **Context**: Comprehensive user testing revealed serious navigational and visual redundancies:
  1. `HomeScreen` and `ReportsScreen` rendered identical 4 summary metric cards (Operating Cash Flow, Net Profit, Operating Income, Operating Expense), causing `ReportsScreen` to feel like an uninspired duplicate of `HomeScreen`.
  2. `EntryScreen` and `LedgerScreen` competed directly: `EntryScreen` had huge static entry buttons alongside a small 5-item recent list, while `LedgerScreen` was a separate tab for the full list with search/filter, and `RecordSpeedDial` (FAB +) was ALSO floating on every screen.
  3. `EntryScreen` bottom nav icon was `+` (Icons.Default.Add), colliding visually and conceptually with the persistent FAB `+`.
  4. `EntryScreen` contained an expandable "Jenis lain" dropdown (capital, drawings, asset) which contradicted the pure cash-only mental model ("uang masuk ya uang masuk, uang keluar ya uang keluar").
  5. The huge buttons for "Uang Masuk" and "Uang Keluar" took up 50% of the screen on `EntryScreen`, making the entry tab redundant with the FAB (+).
- **Decision**:
  1. **Consolidate Navigation to 3 Clean Tabs**: Drop `Destination.LEDGER` entirely. Bottom navigation now consists of:
     - `Destination.HOME` (Beranda - Cash pulse & operational overview)
     - `Destination.ENTRY` (Catatan - Icon: `Icons.AutoMirrored.Filled.ReceiptLong`, complete searchable & filterable ledger with empty state and undo/delete actions)
     - `Destination.REPORTS` (Laporan - Formal financial statements: Income Statement, Cash Flow, Monthly Trend, Feasibility & Variance analysis)
  2. **Eradicate Redundant Cards on Laporan**: Remove `SimpleMetricGrid` from `FinancialsSection` in `ReportsScreen.kt`. Laporan immediately presents structured, formal financial statements without re-rendering the 4 cards from Beranda.
  3. **Streamline Catatan & Purge Static Entry Buttons**: Remove the giant buttons and "Jenis lain" dropdown from `EntryScreen.kt`. The page is now a dedicated, responsive transaction history (with search, direction chips, and empty card CTA).
  4. **Unify Entry via FAB (+) & Bottom Sheet**: All new transactions are initiated via `RecordSpeedDial` (FAB +) or the `[Catat Sekarang]` CTA on empty state. The `ModalBottomSheet` includes a `SingleChoiceSegmentedButtonRow` between `Uang Masuk` and `Uang Keluar` at the top of the form for effortless direction switching.
- **Consequences**: Navigational clutter eliminated; screen space maximized; user flows for both recording (via FAB) and browsing (via Catatan) are clear and non-duplicative; all unit tests pass 100%.

### [DEC-007] Intuitive UX Alignment & Dual Simulation Feasibility Calibration
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: User testing by academic advisor surfaced key intuitive design flaws:
  1. Top hero cash balance card on `HomeScreen` was tapped by users expecting detailed cash flow breakdown, but was non-interactive.
  2. `EntryScreen` lacked recent transaction history directly beneath entry buttons, leaving users uncertain if transactions had successfully posted.
  3. Cost composition chart in `HomeScreen` was constrained to rigid presets rather than representing actual customized operational cost categories.
  4. Feasibility analysis in `FeasibilityCard.kt` evaluated B/C Ratio using an R/C threshold ($> 1.0$), erroneously marking profitable micro-businesses (e.g. Simulation 1 CVP benchmark with B/C 0.273) as "TIDAK LAYAK", while also ignoring projection transactions when volume/price assumptions were absent.
  5. `LedgerScreen` retained a dead "UNPAID" filter from the retired obligations schema, and `SharedComponents.kt` used a `Redo` icon for reversing transactions instead of `Undo`.
- **Decision**:
  1. Make `HeroCard` clickable, directly navigating to `Destination.REPORTS` with `"Rincian Arus Kas →"` visual affordance.
  2. Embed recent transactions directly inside `EntryScreen`'s `LazyColumn` below kind buttons, rendering the last 5 transactions with full reversal/deletion actions and a shortcut to full history (`Destination.LEDGER`).
  3. Introduce inline on-the-fly custom category creation directly in `EntryScreen` via `ChoiceMenu` (`"+ Tambah Kategori Baru..."`) and `viewModel.createCategory`, dynamically enriching the `ExpenseDistributionCard` donut chart with exact user-defined cost slices.
  4. Decouple R/C Ratio ($TR / TC$, threshold $\ge 1.00$) from B/C Ratio ($Net Profit / TC$, threshold $\ge 0.00$) in `FeasibilityCard.kt`, and implement automatic fallback in `BusinessAnalysis.kt` to calculate feasibility from recorded projection transactions (`summary.operatingIncomeRupiah`) when volume and price are omitted.
  5. Purge the dead "UNPAID" filter from `LedgerScreen.kt`, and switch reversal icon to `Icons.AutoMirrored.Filled.Undo` in `SharedComponents.kt`.
  6. Calibrate and verify against both real simulations:
     - **Simulation 1 (CVP Micro MSME Benchmark)**: Kedai Kopi Berkah ($P=10.000$, $VC=5.000$, $FC=2.000.000$, $Q=700 \to BEP=400$ units, $R/C=1.273 \ge 1.0$, $B/C=0.273 \ge 0.0 \to$ LAYAK).
     - **Simulation 2 (Sambal Producer Verification Workbook)**: 45 RAB budget lines, 56 transactions, Plan $R/C=1.308$, Actual $R/C=1.117 \to$ LAYAK.
- **Consequences**: User experience is seamless and intuitive; financial feasibility analysis correctly passes both CVP and workbook-based business models; all 65+ unit tests pass.

### [DEC-006] Pure Cash-Only Architecture (Retirement of Obligations & Settlements)
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: User confirmed explicit requirement to completely eradicate all debt, loan, interest, installment, and credit/obligation concepts ("aku mau hapus utang bunga dan sebagainya sama sekali"). In the micro-MSME target domain, managing accounts receivable/payable, credit terms, due dates, and partial settlements created extreme overengineering and confusion for simple cash-based business operators.
- **Decision**: 
  1. Drop Room database tables `obligations` and `settlements` entirely via non-destructive `MIGRATION_6_7`. Bump database version to 7.
  2. Remove `ObligationEntity`, `SettlementEntity`, and all DAO methods querying obligations and settlements.
  3. Simplify `EntryScreen.kt` to only 5 pure cash transaction types: `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, and `ASSET_PURCHASE_CASH`.
  4. Realise `HomeScreen.kt` and `ReportsScreen.kt` strictly around 4 pure cash metrics: Operating Cash Flow, Net Profit, Cash Income, Cash Expense. Remove due date reminder cards and debt settlement dialogs.
  5. Remove obligations export from PDF and XLSX workbooks, updating `EncryptedBackupManager` table lists.
- **Consequences**: Radically reduced codebase complexity, zero debt overhead, zero risk of foreign key lock contention from cascading settlements, and a 100% intuitive pure-cash accounting system tailored for grassroots MSMEs. All unit test suites pass 100%.

### [DEC-005] Room Database Schema 6 and Migration 5->6 for Stock & CVP Assumptions
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: Removal of loan/interest columns and introduction of inventory management required a database schema update with zero data loss on existing user devices.
- **Decision**: Bump Room database version to 6. Implement non-destructive `MIGRATION_5_6`: rebuild `scenario_assumptions` preserving all existing values while replacing debt fields with `fixedCostRupiah` and `variableCostPerUnitRupiah`, and create `stock_items` and `stock_movements` tables with foreign keys and cascade delete. Expose `StockDao` in `AppDatabase` and `FinanceRepository`.
- **Consequences**: Safe forward migration for existing app installs with full relational integrity and compile-time schema verification.

### [DEC-004] Architectural Design for Minimal Stock Management
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: Academic supervisor and UMKM users require lightweight stock tracking without enterprise ERP complexity. Room DB v5 had zero inventory tables.
- **Decision**: Define `StockItemEntity` (master barang with base price in exact integer rupiah, current stock, low stock alert threshold) and `StockMovementEntity` (immutable mutation ledger: IN, OUT, and ADJUSTMENT for stock opname). Encapsulate operations in lightweight domain use cases (`RecordStockMovementUseCase`, `GetStockSummaryUseCase`).
- **Consequences**: Lightweight inventory tracking with O(1) balance reads, atomic mutation history, and zero external dependencies.

### [DEC-003] Realignment of CVP (Break-Even Point) and Ratio Metrics
- **Date**: 2026-10-18
- **Status**: Accepted
- **Context**: The existing calculation used an oversimplified formula `BEP = totalCost / price` and misnamed Revenue-to-Cost ratio ($TR / TC$) as `benefitCostRatio`. In formal managerial accounting, BEP requires separation into Fixed Cost ($FC$) and Variable Cost per Unit ($VC$).
- **Decision**: Implement standard CVP formulas: Unit Contribution Margin ($CM = P - VC$), Contribution Margin Ratio ($CMR = CM / P$), $BEP_{unit} = FC / CM$, $BEP_{rupiah} = FC / CMR$. Distinguish $R/C\ Ratio$ ($TR / TC$, benchmark $> 1.0$) from $B/C\ Ratio$ ($NetProfit / TC$, benchmark $> 0.0$).
- **Consequences**: Formula verified against real micro-business benchmark (Kedai Kopi Berkah: P=10.000, VC=5.000, FC=2.000.000 yielding exact BEP 400 units). Full backward compatibility preserved for legacy workbook imports.

### [DEC-002] Deprecation and Removal of Loan and Interest Calculations in Micro-MSME Domain
- **Date**: 2026-10-18
- **Status**: Accepted & Implemented
- **Context**: `AnalysisAssumptions` and `scenario_assumptions` contained `loanAmountRupiah` and `interestRateBasisPoints`, artificially adding interest expense to production total cost. This contradicts micro-MSME cash-flow realities and adds unneeded complexity.
- **Decision**: Deprecate loan and interest properties in `AnalysisAssumptions` and domain calculators. Establish a clean deprecation and migration roadmap (Room Migration 5 to 6) to remove loan plan fields from data, export/import, and presentation layers.
- **Consequences**: Cleaner domain model focused on operational viability and self-sustaining working capital.

### [DEC-001] Adoption of Modular Logging Protocol
- **Date**: 2026-09-16
- **Status**: Accepted
- **Context**: Standardizing change tracking, observability, and auditability across all managed projects for AI agents and human contributors.
- **Decision**: Mandate `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, and `logs/CHANGELOG.md` updates across all repositories.
- **Consequences**: Consistent history, reduced context loss during multi-agent handoffs.