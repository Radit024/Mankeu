# Entity Relationship Diagram (ERD) - Mankeu Database

Diagram ini merefleksikan skema basis data Room SQLite **Schema Version 4** pada aplikasi Mankeu (`com.mankeu.data`). 

> [!NOTE]
> Pada Schema Version 4, entitas `units` telah dihapus (migrasi murni tanpa kehilangan data/tanpa `destructiveFallback`). Satuan kini dicatat sebagai teks bebas (*free-text string*) pada transaksi dan item anggaran untuk fleksibilitas UMKM.

```mermaid
erDiagram
    %% Core Scope Hierarchy
    businesses ||--o{ projects : has
    projects ||--o{ books : contains
    businesses ||--o{ categories : defines

    %% Financial Core and Planning
    books ||--o{ budget_items : plans
    categories ||--o{ budget_items : classifies
    books ||--o{ financial_events : records
    categories ||--o{ financial_events : categorizes
    budget_items ||--o{ financial_events : realizes

    %% Double Entry Ledger
    financial_events ||--|| journal_entries : generates
    journal_entries ||--|{ postings : balances

    %% Obligations and Receivables/Payables
    books ||--o{ obligations : tracks
    financial_events ||--o{ obligations : originates
    obligations ||--o{ settlements : settles
    financial_events ||--|| settlements : settles_with

    %% Simulation and Assumptions
    books ||--o| scenario_assumptions : configures

    %% Import Engine and Provenance Audit
    import_batches ||--o{ import_notices : notifies
    import_batches ||--o{ provenance_records : audits
    import_batches ||--o{ budget_items : imports_budget
    import_batches ||--o{ financial_events : imports_event

    %% ==========================================
    %% TABLE DEFINITIONS
    %% ==========================================

    businesses {
        string id PK
        string name
        string currency
        boolean archived
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    projects {
        string id PK
        string businessId FK
        string name
        boolean isGeneral
        boolean archived
        long createdAtEpochMillis
        string description
        long startDateEpochDay
        long endDateEpochDay
        long updatedAtEpochMillis
    }

    books {
        string id PK
        string projectId FK
        string mode
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    categories {
        string id PK
        string businessId FK
        string role
        string name
        string normalizedName
        string aliases
        string templateOrigin
        boolean active
        long createdAtEpochMillis
    }

    budget_items {
        string id PK
        string bookId FK
        string categoryId FK
        string name
        string normalizedName
        string quantityText
        string unit
        long unitPriceRupiah
        long plannedTotalRupiah
        long plannedDateEpochDay
        string aliases
        string amountSource
        long formulaDifferenceRupiah
        int sortOrder
        boolean archived
        string importBatchId FK
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    financial_events {
        string id PK
        string businessId FK
        string projectId FK
        string bookId FK
        string type
        long amountRupiah
        long dateEpochDay
        string description
        string category
        string categoryId FK
        string counterparty
        string reference
        long dueDateEpochDay
        string quantityText
        string unit
        long unitPriceRupiah
        string budgetItemId FK
        string obligationId FK
        string importBatchId FK
        string cashFlowRole
        string status
        string reversesEventId FK
        string reversedByEventId FK
        long createdAtEpochMillis
    }

    journal_entries {
        string id PK
        string sourceEventId FK
        long dateEpochDay
        string description
        string reversalOfJournalId
    }

    postings {
        string id PK
        string journalId FK
        string account
        string side
        long amountRupiah
    }

    obligations {
        string id PK
        string bookId FK
        string kind
        string sourceEventId FK
        string counterparty
        string description
        long issueDateEpochDay
        long dueDateEpochDay
        long originalAmountRupiah
        boolean reversed
        long createdAtEpochMillis
    }

    settlements {
        string id PK
        string obligationId FK
        string eventId FK
        long amountRupiah
        long createdAtEpochMillis
    }

    scenario_assumptions {
        string id PK
        string bookId FK
        long openingCashRupiah
        long ownerCapitalRupiah
        long loanAmountRupiah
        int interestRateBasisPoints
        long loanDisbursementEpochDay
        long loanPaymentEpochDay
        long otherFinancingCostRupiah
        string outputQuantityText
        string outputUnit
        long outputPriceRupiah
        long updatedAtEpochMillis
    }

    import_batches {
        string id PK
        string sourceName
        string sourceSha256
        string importerVersion
        string state
        long committedAtEpochMillis
        string businessId
        string projectId
        string bookId
        string mappingVersion
        int recordCount
        long totalAmountRupiah
        long reversedAtEpochMillis
    }

    import_notices {
        string id PK
        string batchId FK
        string severity
        string title
        string explanation
        long amountRupiah
        string sourceEvidence
        boolean reviewed
        string action
        long createdAtEpochMillis
    }

    provenance_records {
        string id PK
        string batchId FK
        string targetEventId
        string targetBudgetItemId
        string targetField
        string classification
        string sourceEvidence
        string sourceSheet
        string sourceCell
        string sourceValue
        string rationale
        string inferredFields
        int confidenceBasisPoints
        string ruleVersion
    }
```

## Relasi Kunci & Aturan Integritas

1. **Isolasi Buku (`books`)**:
   - Setiap `project` memiliki tepat 2 buku: `mode = 'PROJECTION'` dan `mode = 'REALIZATION'`.
   - Data transaksi dan anggaran terisolasi secara ketat per buku sehingga rencana/simulasi tidak pernah mencemari realisasi kas nyata.
2. **Double-Entry Invariant**:
   - Setiap `financial_events` yang berstatus `POSTED` memiliki relasi 1:1 ke `journal_entries`.
   - `journal_entries` memiliki $\ge 2$ baris `postings` di mana $\sum \text{Debit} == \sum \text{Credit}$.
3. **Integritas Piutang & Utang (`obligations` & `settlements`)**:
   - Transaksi kredit (`CREDIT_SALE`, `CREDIT_PURCHASE_EXPENSE`, `CREDIT_PURCHASE_ASSET`) menghasilkan `obligations`.
   - Pembayaran cicilan/pelunasan menghasilkan `settlements` yang menghubungkan event kas pelunasan dengan kewajiban awal.
   - Aturan pembatalan (*Reversal Guard*): Sebuah transaksi sumber kewajiban tidak dapat dibalik jika telah memiliki `settlements` aktif.
4. **Audit Trail Impor & Pembalikan Atomik**:
   - Seluruh baris yang diimpor dari berkas Excel menyimpan `importBatchId`.
   - Pembalikan batch (`reverseImportBatch`) membatalkan seluruh transaksi dalam batch secara atomik dan mengarsipkan item anggaran terkait.
