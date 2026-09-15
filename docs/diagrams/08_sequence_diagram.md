# Sequence Diagrams - Mankeu

## 1. Mencatat transaksi kas

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Entry as EntryScreen
    participant VM as AppViewModel
    participant Repo as FinanceRepository
    participant Policy as LedgerPolicy
    participant Dao as FinanceDao
    participant Db as Room SQLite

    Owner->>Entry: Ketuk Simpan, Rp50.000 beli bahan
    Entry->>Entry: Abaikan bila isSaving atau justSaved
    Entry->>VM: record(type, amount, description, ...)
    VM->>VM: mutating, isSaving compareAndSet false ke true
    VM->>Repo: recordEvent(EventDraft)
    Repo->>Db: withTransaction dimulai
    Repo->>Repo: Periksa jumlah, jenis, lingkup, kategori, pihak
    Repo->>Policy: journalFor(event)
    Policy-->>Repo: JournalEntry seimbang
    Repo->>Repo: Periksa ulang debit sama dengan kredit

    alt Aturan dilanggar
        Repo-->>Db: Rollback
        Repo-->>VM: IllegalArgumentException
        VM-->>Entry: message berisi alasan penolakan
    else Semua aturan terpenuhi
        Repo->>Dao: insertEvent
        Repo->>Dao: insertJournal
        Repo->>Dao: insertPostings
        opt Transaksi kredit
            Repo->>Dao: insertObligation
        end
        Repo-->>Db: Commit
        Repo-->>VM: eventId
        VM-->>Entry: message tersimpan
        Db-->>VM: Invalidation Room memicu Flow
        VM-->>Entry: uiState baru
    end
    VM->>VM: isSaving kembali false
```

## 2. Komit impor ke lingkup yang belum ada

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Data as DataScreen
    participant VM as AppViewModel
    participant Repo as FinanceRepository
    participant Db as Room SQLite

    Owner->>Data: Setujui pratinjau
    Data->>VM: commitImport(targetBook)
    VM->>Repo: inTransaction
    Repo->>Db: Transaksi dimulai
    opt Belum ada usaha terpilih
        VM->>Repo: createBusiness(nama berkas)
    end
    opt Belum ada lahan terpilih
        VM->>Repo: createProject(Lahan Utama)
    end
    VM->>Repo: commitImport(businessId, projectId, preview, destination)
    alt Hash sudah dikomit atau baris tidak valid
        Repo-->>Db: Rollback, termasuk usaha dan lahan baru
        Repo-->>VM: Galat
        VM-->>Data: ImportUiState.Failure
    else Berhasil
        Repo-->>Db: Commit
        Repo-->>VM: Jumlah baris
        VM->>VM: Pilih usaha dan lahan
        VM-->>Data: ImportUiState.Committed
    end
```

## Jaminan

1. **Atomik.** Semua penulisan dan pemeriksaan satu aksi berada dalam satu transaksi SQLite. Penolakan tidak meninggalkan baris setengah jadi, termasuk usaha atau lahan yang dibuat untuk impor yang gagal (D-079).
2. **Tidak ada ketukan ganda.** `compareAndSet` di `mutating` menolak aksi kedua yang tiba sebelum aksi pertama selesai, termasuk ketukan dalam satu frame (D-072).
3. **Reaktif.** Setelah commit, invalidation Room memancarkan ulang Flow, dan `uiState` memperbarui layar tanpa pemanggilan manual.
