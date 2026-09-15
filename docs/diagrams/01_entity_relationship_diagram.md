# Entity Relationship Diagram - Basis Data Mankeu

Skema Room SQLite **versi 5** (`com.mankeu.data`), diselaraskan dengan `Entities.kt` dan `app/schemas/com.mankeu.data.AppDatabase/5.json`.

> Garis **tegas** adalah foreign key sungguhan (dengan aturan `CASCADE` atau `RESTRICT`). Garis **putus-putus** adalah referensi logis: kolomnya diindeks, tetapi tidak dijaga foreign key, sehingga integritasnya ditegakkan oleh `FinanceRepository`.

```mermaid
erDiagram
    businesses ||--o{ projects : "CASCADE"
    businesses ||--o{ categories : "CASCADE"
    projects ||--|{ books : "tepat dua buku"
    books ||--o{ budget_items : "CASCADE"
    categories ||--o{ budget_items : "RESTRICT"
    books ||--o{ financial_events : "CASCADE"
    financial_events ||--|| journal_entries : "CASCADE"
    journal_entries ||--|{ postings : "CASCADE"
    books ||--o{ obligations : "CASCADE"
    financial_events ||--o| obligations : "sumber kredit, CASCADE"
    obligations ||--o{ settlements : "CASCADE"
    financial_events ||--o| settlements : "event kas, CASCADE"
    books ||--o| scenario_assumptions : "CASCADE"
    import_batches ||--o{ import_notices : "CASCADE"
    import_batches ||--o{ provenance_records : "CASCADE"
    categories ||..o{ financial_events : "categoryId logis"
    import_batches ||..o{ financial_events : "importBatchId logis"
    import_batches ||..o{ budget_items : "importBatchId logis"
    financial_events |o..o| financial_events : "reversesEventId logis"

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
        string description
        long startDateEpochDay
        long endDateEpochDay
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    books {
        string id PK
        string projectId FK "unik bersama mode"
        string mode "PROJECTION atau REALIZATION"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    categories {
        string id PK
        string businessId FK
        string role "REVENUE EXPENSE ASSET EQUITY LIABILITY"
        string name
        string normalizedName "unik bersama businessId dan role"
        string aliases
        string templateOrigin
        boolean active "arsip = 0, dapat diaktifkan lagi"
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
        string importBatchId "logis"
        long createdAtEpochMillis
        long updatedAtEpochMillis
    }

    financial_events {
        string id PK
        string businessId FK
        string projectId FK
        string bookId FK
        string type
        long amountRupiah "rupiah bulat lebih dari 0"
        long dateEpochDay
        string description
        string category
        string categoryId "logis"
        string counterparty
        string reference
        long dueDateEpochDay
        string quantityText
        string unit
        long unitPriceRupiah
        string budgetItemId "logis"
        string obligationId "logis"
        string importBatchId "logis"
        string cashFlowRole
        string status "POSTED atau REVERSED"
        string reversesEventId "logis"
        string reversedByEventId "logis"
        long createdAtEpochMillis
    }

    journal_entries {
        string id PK
        string sourceEventId FK, UK
        long dateEpochDay
        string description
        string reversalOfJournalId
    }

    postings {
        string id PK
        string journalId FK
        string account
        string side "DEBIT atau CREDIT"
        long amountRupiah
    }

    obligations {
        string id PK
        string bookId FK
        string kind "RECEIVABLE atau PAYABLE"
        string sourceEventId FK, UK
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
        string eventId FK, UK
        long amountRupiah
        long createdAtEpochMillis
    }

    scenario_assumptions {
        string id PK
        string bookId FK, UK
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
        string sourceSha256 "indeks biasa sejak v5"
        string importerVersion
        string state "COMMITTED atau REVERSED"
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

## Aturan integritas

1. **Dua buku per lahan.** Indeks unik `(projectId, mode)` dan `ensureBookPair` menjamin tepat satu buku `PROJECTION` dan satu `REALIZATION`.
2. **Jurnal selalu seimbang.** Setiap event memiliki tepat satu `journal_entries` (unik pada `sourceEventId`). Jumlah debit dan kredit posting diperiksa sebelum baris ditulis, di dalam transaksi yang sama.
3. **Piutang dan utang.** Transaksi kredit membuat satu `obligations`. Setiap pembayaran menambah satu `settlements` yang menunjuk event kasnya sendiri. Sisa tagihan dan status tidak disimpan, melainkan dihitung dari pembayaran yang belum dibalik.
4. **Koreksi.** Pembalikan dan penghapusan mengikuti tabel aturan di `docs/DOMAIN_MODEL.md` ("Correction rules"), termasuk penolakan saat masih ada pembayaran aktif.
5. **Impor.** `sourceSha256` diindeks tanpa unik sejak v5 (D-076). Duplikat ditolak oleh `hasImported`, yang hanya menghitung batch berstatus `COMMITTED`, sehingga berkas yang batch-nya sudah dibalik dapat diimpor ulang.
6. **Migrasi.** `MIGRATION_3_4` menghapus `units` dan `categories.defaultUnitId`. `MIGRATION_4_5` mengganti indeks unik `sourceSha256` menjadi indeks biasa. Tidak ada migrasi destruktif.
