# Sequence Diagram (UML) - Alur Transaksi Double-Entry

Diagram urutan ini memperlihatkan interaksi antar komponen perangkat lunak saat pengguna mencatat sebuah transaksi kas, mulai dari penekanan tombol di Jetpack Compose UI hingga komit transaksi ganda di basis data SQLite.

```mermaid
sequenceDiagram
    autonumber
    actor User as Pengguna UMKM
    participant UI as EntryScreen
    participant VM as AppViewModel
    participant Repo as FinanceRepository
    participant Policy as LedgerPolicy
    participant DAO as FinanceDao
    participant DB as AppDatabase

    User->>UI: Ketuk Simpan (Nominal Rp50.000, Beli Beras, Kategori Bahan Baku)
    activate UI
    UI->>UI: Set isSaving true (Kunci Tombol Simpan)
    UI->>VM: recordTransaction(draft)
    activate VM

    VM->>Repo: recordEvent(draft)
    activate Repo

    Repo->>DB: database.withTransaction
    activate DB

    Repo->>Policy: journalFor(draftAsEvent)
    activate Policy
    Policy-->>Repo: JournalEntry (Debit BIAYA, Credit KAS)
    deactivate Policy

    Repo->>Repo: Invariant Check requireBalanced (Debit equals Credit)
    
    alt Tidak Seimbang
        Repo-->>DB: Rollback Transaksi
        Repo-->>VM: Lempar IllegalStateException
        VM-->>UI: Kirim Pesan Error
        UI->>UI: Set isSaving false
    else Seimbang
        Repo->>DAO: insertEvent(financialEventEntity)
        Repo->>DAO: insertJournalEntry(journalEntryEntity)
        Repo->>DAO: insertPostings(postingEntities)
        
        opt Transaksi Tempo atau Kredit
            Repo->>DAO: insertObligation(obligationEntity)
        end

        Repo-->>DB: Commit SQLite Transaction
        deactivate DB
        Repo-->>VM: Return eventId
        deactivate Repo

        VM->>VM: Feedback CONFIRM (Audio and Haptic)
        VM->>VM: Emit UiMessage Success
        VM-->>UI: Update AppUiState via WhileSubscribed Flow
        deactivate VM

        UI->>UI: Tutup Bottom Sheet Form
        UI->>UI: Set isSaving false (Buka Kunci)
        UI-->>User: Tampilkan Notifikasi Snackbar dan Saldo Terupdate
        deactivate UI
    end
```

## Jaminan Integritas (*Transactional Guarantees*)

1. **Atomisitas SQLite**:
   - Pemanggilan `insertEvent`, `insertJournalEntry`, dan `insertPostings` berada di dalam blok tunggal `database.withTransaction`. Jika terjadi kegagalan sistem pada salah satu tahap (misalnya kegagalan memori atau baterai mati), basis data akan melakukan *rollback* otomatis sehingga tidak ada data setengah jadi (*no orphan entries*).
2. **Pencegahan Klik Ganda (*Race Condition Prevention*)**:
   - Status `isSaving` segera diaktifkan pada langkah pertama sebelum sinyal coroutine dikirimkan ke ViewModel, sehingga penekanan tombol berulang-ulang pada layar sentuh yang responsif tidak akan menciptakan transaksi kembar.
3. **Pemberitahuan Reaktif**:
   - Begitu transaksi berhasil di-*commit*, Room DAO secara otomatis memicu emisi baru pada *Flow* reaktif yang didengarkan oleh `AppViewModel`, sehingga saldo kas dan ringkasan metrik di layar Beranda diperbarui secara seketika (*real-time*).
