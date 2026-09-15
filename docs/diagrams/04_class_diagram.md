# Class Diagram - Domain, Data, dan Presentasi Mankeu

Nama kelas, properti, dan metode di bawah ini sama dengan kode sumber. Hanya anggota yang relevan bagi alur bisnis yang ditampilkan.

```mermaid
classDiagram
    direction LR

    class BookMode {
        <<enumeration>>
        PROJECTION
        REALIZATION
    }
    class CashFlowRole {
        <<enumeration>>
        OPERATING
        INVESTING
        FINANCING
        NON_CASH
    }
    class RecordStatus {
        <<enumeration>>
        POSTED
        REVERSED
    }
    class ObligationStatus {
        <<enumeration>>
        OPEN
        PARTIAL
        PAID
        OVERDUE
        REVERSED
    }

    class MoneyEvent {
        +String id
        +String bookId
        +FinancialEventType type
        +Long amountRupiah
        +LocalDate date
        +String description
        +CashFlowRole cashFlowRole
        +RecordStatus status
        +String obligationId
        +String reversesEventId
        +String reversedByEventId
    }
    class JournalEntry {
        +String id
        +String sourceEventId
        +List~Posting~ postings
        +String reversalOfJournalId
        +requireBalanced() JournalEntry
    }
    class Posting {
        +LedgerAccount account
        +PostingSide side
        +Long amountRupiah
    }
    class ObligationBalance {
        +String id
        +ObligationKind kind
        +Long originalAmountRupiah
        +Long settledAmountRupiah
        +Long outstandingAmountRupiah
        +Boolean reversed
        +status(today) ObligationStatus
    }
    class CashFlowRecord {
        +String id
        +LocalDate date
        +FinancialEventType type
        +CashFlowRole cashFlowRole
        +Long amountRupiah
        +String reversesEventId
    }
    class FinancialSummary {
        +Long operatingIncomeRupiah
        +Long operatingExpenseRupiah
        +Long profitRupiah
        +Long cashBalanceRupiah
        +Long receivableOutstandingRupiah
        +Long payableOutstandingRupiah
    }
    class BusinessAnalysis {
        +Long financingGapRupiah
        +Long totalCostRupiah
        +BigDecimal breakEvenQuantity
        +BigDecimal benefitCostRatio
        +Boolean feasible
    }

    class LedgerPolicy {
        <<object>>
        +journalFor(event) JournalEntry
        +reversalFor(original, reversalEvent) JournalEntry
    }
    class PostingReportCalculator {
        <<object>>
        +calculate(records, obligations) FinancialSummary
    }
    class MonthlyCashFlowCalculator {
        <<object>>
        +calculateFromRecords(records, openingCash) List~MonthlyCashFlow~
    }
    class BudgetPlanCalculator {
        <<object>>
        +summarize(lines) FinancialSummary
        +schedule(lines, openingCash) List~MonthlyCashFlow~
    }
    class BusinessAnalysisCalculator {
        <<object>>
        +calculate(summary, assumptions, monthlyFlows) BusinessAnalysis
    }

    class FinanceRepository {
        +inTransaction(block) T
        +recordEvent(draft) String
        +settleObligation(obligationId, amount, date, description) String
        +reverseEvent(eventId, date, reason) String
        +deleteEvent(eventId)
        +createCategory(businessId, name, role, aliases) String
        +commitImport(businessId, projectId, preview, targetBook) Int
        +reverseImportBatch(batchId) Int
        +eventsForBook(bookId) Flow
        +cashFlowRecords(bookId) Flow
        +accountingSummary(bookId) Flow
    }
    class FinanceDao {
        <<interface>>
        +insertEvent(event)
        +insertJournal(entry)
        +insertPostings(postings)
        +observeEventsForBook(bookId, limit) Flow
        +observeCashFlowRows(bookId) Flow
        +countLiveSettlementsForSource(sourceEventId) Int
        +restoreObligationBySource(sourceEventId)
        +findCategory(businessId, role, normalizedName) CategoryEntity
    }
    class AppDatabase {
        <<abstract>>
        +financeDao() FinanceDao
        +MIGRATION_3_4
        +MIGRATION_4_5
    }
    class EncryptedBackupManager {
        +create(password) ByteArray
        +prepareRestore(envelope, password) PreparedRestore
        +restore(prepared, recoveryPassword) RestoreResult
    }

    class AppViewModel {
        +StateFlow~AppUiState~ uiState
        +MutableStateFlow~Boolean~ isSaving
        +record(type, amount, description, ...)
        +settleObligation(id, amount, date, description)
        +reverseEvent(id, reason)
        +deleteTransaction(eventId)
        +commitImport(targetBook)
        +reverseImportBatch(id)
        -mutating(block) Job
    }
    class AppUiState {
        +String selectedBookId
        +BookMode selectedBookMode
        +List~MoneyEvent~ events
        +Int eventCount
        +List~CashFlowRecord~ cashFlowRecords
        +FinancialSummary summary
        +Boolean isLoading
        +planBudgetLines() List~PlanBudgetLine~
    }

    JournalEntry "1" *-- "2..*" Posting : berisi
    MoneyEvent --> RecordStatus
    MoneyEvent --> CashFlowRole
    ObligationBalance --> ObligationStatus
    LedgerPolicy ..> MoneyEvent : membaca
    LedgerPolicy ..> JournalEntry : membuat
    PostingReportCalculator ..> FinancialSummary : membuat
    MonthlyCashFlowCalculator ..> CashFlowRecord : membaca
    BusinessAnalysisCalculator ..> BusinessAnalysis : membuat
    FinanceRepository --> FinanceDao : kueri
    FinanceRepository --> AppDatabase : transaksi
    FinanceRepository ..> LedgerPolicy : jurnal
    EncryptedBackupManager --> AppDatabase : snapshot
    AppViewModel --> FinanceRepository : delegasi
    AppViewModel --> EncryptedBackupManager : cadangan
    AppViewModel --> AppUiState : memancarkan
```

## Catatan desain

1. **Keseimbangan dua lapis.** `LedgerPolicy` memanggil `JournalEntry.requireBalanced()` saat membuat jurnal, lalu `FinanceRepository.insertEventAndJournal` memeriksa ulang debit sama dengan kredit tepat sebelum baris ditulis.
2. **Status dihitung, bukan disimpan.** `ObligationBalance.status(today)` menurunkan OPEN, PARTIAL, PAID, OVERDUE, atau REVERSED dari angka dan tanggal.
3. **Proyeksi sempit.** `CashFlowRecord` hanya membawa kolom yang dibutuhkan jadwal kas, sehingga buku berisi ribuan transaksi tidak memuat seluruh deskripsi ke memori.
