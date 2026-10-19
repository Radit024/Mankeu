# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Explicit dual BEP presentation: **BEP Volume (Unit)** and **BEP Nilai (Uang / Nominal Rupiah)** side-by-side metric tiles in `FeasibilityCard` and `ReportsScreen`.
- Dedicated **Kalkulator BEP & Parameter** dialog with real-time **Live Calculation Preview Card** showing dynamic BEP Unit, BEP Rupiah, and Contribution Margin % as inputs change.
- Automated unit test suite additions in `BusinessAnalysisTest` covering BEP calculations without target quantity, loss-making unit margins, and fixed cost fallbacks.
- Consolidate navigation to 3 clean tabs (`Beranda`, `Catatan`, `Laporan`), eliminating the redundant `Riwayat` tab.
- Integrated full, searchable & filterable transaction ledger directly inside `EntryScreen` (Catatan) with search bar, direction filter chips (`Semua`, `Uang Masuk`, `Uang Keluar`), and empty state feedback card (`EmptyCard`) with immediate `[Catat Sekarang]` CTA.
- Added `SingleChoiceSegmentedButtonRow` directly at the top of the entry `ModalBottomSheet` to toggle between `Uang Masuk` and `Uang Keluar` on the fly.
- Interactive foreground launch task via Windows Task Scheduler (`schtasks /it`), established as the permanent default mechanism to prevent sandbox isolation issues.

### Changed
- Renamed Tab 2 in `ReportsScreen` to **"Kelayakan & BEP"** (`reports_tab_feasibility`) and action button to `[ 🧮 Kalkulator & Parameter BEP ]`.
- Decoupled BEP display from `targetQuantity > 0`, rendering BEP Unit and BEP Rupiah whenever unit price, variable cost, and fixed cost are available.
- `Destination.ENTRY` bottom navigation icon changed from `+` (`Icons.Default.Add`) to `Icons.AutoMirrored.Filled.ReceiptLong` (receipt/ledger).
- Removed giant static buttons for "Uang Masuk" and "Uang Keluar" and the "Jenis lain" dropdown from `EntryScreen`, strictly enforcing the pure cash-only model.
- Eradicated redundant 4 summary metric cards from `FinancialsSection` in `ReportsScreen.kt`. Laporan now immediately displays formal accounting statements (Income Statement, Cash Flow, and Monthly Cash Flow Chart) without echoing Beranda's cards.
- Interactive `HeroCard` on `HomeScreen`: clicking the top cash balance navigates directly to Cash Flow Reports (`Destination.REPORTS`).
- Embedded recent transactions history in `EntryScreen` (`state.events.take(5)`), enabling immediate confirmation and undo/reverse actions right below input buttons.
- Inline custom category creation directly in `EntryScreen`'s category picker (`ChoiceMenu`), allowing on-the-fly creation of user-defined operational expense categories reflected in `ExpenseDistributionCard`.
- R/C Ratio ($TR / TC$) metric card alongside Net B/C Ratio in `FeasibilityCard` and `ReportsScreen`, with dynamic fallback to recorded projection revenue when assumption volume and price are omitted.
- Standard CVP (Cost-Volume-Profit) calculations in `BusinessAnalysisCalculator.calculateCvp`:
  - Unit Contribution Margin ($CM = P - VC$)
  - Contribution Margin Ratio ($CMR = CM / P$)
  - BEP Unit ($FC / CM$) and BEP Rupiah ($FC / CMR$)
  - Revenue-to-Cost Ratio ($R/C = TR / TC$)
  - Benefit-Cost Ratio ($B/C = NetProfit / TC$)
- Minimal Stock Management domain models, calculations, and Room schema (`StockManagement.kt`):
  - `StockItemEntity`, `StockMovementEntity`, and `StockDao`
  - `RecordStockMovementUseCase` (Stok Awal + Masuk - Keluar & Opname)
  - `GetStockSummaryUseCase` (Low stock alert & total inventory valuation in exact rupiah)
- Automated unit test suite verifying real micro-MSME benchmark (Kedai Kopi Berkah: exact BEP 400 units).
- Room database schema version 7 with `MIGRATION_6_7` enforcing pure Cash-Only architecture.

### Changed
- `FeasibilityCard.kt`: Fixed feasibility threshold bug where B/C ratio was checked against R/C ratio threshold ($\ge 1.0$), correctly assessing R/C ratio $\ge 1.0$ and Net B/C ratio $\ge 0.0$.
- `SharedComponents.kt`: Updated transaction reversal icon from `Redo` to `Icons.AutoMirrored.Filled.Undo`.
- `BusinessAnalysisCalculator.calculate` now supports explicit `fixedCostRupiah` and `variableCostPerUnitRupiah` for genuine CVP break-even analysis while preserving backward compatibility for legacy imports.
- `HomeScreen.kt` and `ReportsScreen.kt` re-anchored to 4 primary pure cash metrics: Operating Cash Flow, Net Profit, Cash Income, Cash Expense.
- `EntryScreen.kt` simplified to 5 pure cash transaction types: `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, and `ASSET_PURCHASE_CASH`.
- `EncryptedBackupManager` updated to backup `stock_items` and `stock_movements` while dropping obligation tables.

### Removed
- Removed redundant recent transaction records preview (`home_recent`, `EmptyCard`, `EventCard` list, and `Lihat Semua Catatan` button) from `HomeScreen.kt`, leaving transaction history exclusively to `EntryScreen` (Catatan).
- Removed dead "UNPAID" filter chip from `LedgerScreen.kt`.

### Removed
- Fully purged all debt, loan, interest, installment, and credit/obligation concepts:
  - Dropped database tables `obligations` and `settlements` (`MIGRATION_6_7`).
  - Removed `ObligationEntity`, `SettlementEntity`, and all DAO queries to obligation tables.
  - Removed due date reminder cards and debt settlement dialogs (`SettlementDialog`) from presentation layer.
  - Purged loan amount and interest calculation fields (`loanAmountRupiah`, `interestRateBasisPoints`, `loanDisbursementEpochDay`, `loanPaymentEpochDay`) from domain, Room entities, repository, PDF and Excel exporters, and UI screens.

### Added (Initial)
- Modular logging protocol (`logs/` directory with activity, decision, and changelog files).
- Mandatory `AGENTS.md` guidelines for AI agents and contributors.