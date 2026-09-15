# State Diagrams - Siklus Hidup Mankeu

## 1. Piutang dan utang (`ObligationStatus`)

Status tidak disimpan. `ObligationBalance.status(today)` menurunkannya dari jumlah awal, pembayaran yang belum dibalik, tanggal jatuh tempo, dan penanda `reversed`.

```mermaid
stateDiagram-v2
    [*] --> OPEN : penjualan atau pembelian kredit dicatat
    OPEN --> PARTIAL : pembayaran sebagian
    PARTIAL --> PARTIAL : pembayaran sebagian berikutnya
    OPEN --> PAID : pembayaran penuh
    PARTIAL --> PAID : pembayaran terakhir
    OPEN --> OVERDUE : jatuh tempo lewat
    PARTIAL --> OVERDUE : jatuh tempo lewat
    OVERDUE --> PAID : dilunasi
    PAID --> PARTIAL : satu pembayaran dibalik
    PARTIAL --> OPEN : semua pembayaran dibalik
    OPEN --> REVERSED : transaksi sumber dibalik
    REVERSED --> OPEN : pembalikan sumber dihapus
    PAID --> [*]
    REVERSED --> [*]

    note right of REVERSED
        Membalik sumber ditolak selama
        masih ada pembayaran aktif.
    end note
```

## 2. Transaksi (`RecordStatus`)

Tidak ada status draf di basis data: formulir yang belum disimpan hanya ada di UI.

```mermaid
stateDiagram-v2
    [*] --> POSTED : recordEvent, settleObligation, atau impor
    POSTED --> REVERSED : reverseEvent membuat event REVERSAL
    REVERSED --> POSTED : event REVERSAL-nya dihapus
    POSTED --> Deleted : deleteEvent tanpa pembayaran aktif
    REVERSED --> Deleted : deleteEvent menghapus pasangan pembalik

    state Deleted {
        [*] --> Removed
        Removed : baris dan jurnalnya terhapus lewat CASCADE
    }

    note right of POSTED
        Event REVERSAL sendiri tidak dapat dibalik.
        Menghapusnya memulihkan sumbernya.
    end note
```

## 3. Batch impor dan pratinjau (`ImportUiState`)

```mermaid
stateDiagram-v2
    state "Pratinjau di UI" as PreviewFlow {
        [*] --> Idle
        Idle --> Loading : berkas dipilih
        Loading --> Ready : pratinjau dapat dikomit
        Loading --> Failure : berkas tidak terbaca atau ada galat
        Ready --> Loading : komit
        Loading --> Committed : transaksi berhasil
        Failure --> Idle : reset
        Committed --> Idle : reset
    }

    state "Baris import_batches" as BatchRow {
        [*] --> COMMITTED
        COMMITTED --> REVERSED : reverseImportBatch
    }

    Committed --> BatchRow : batch tertulis
    REVERSED --> Idle : berkas yang sama boleh diimpor ulang
```

## 4. Buku aktif (`BookMode`)

```mermaid
stateDiagram-v2
    state "REALIZATION, primaryContainer" as Realization
    state "PROJECTION, secondaryContainer" as Projection

    [*] --> Realization : bawaan saat aplikasi dibuka
    Realization --> Projection : pilih Proyeksi
    Projection --> Realization : pilih Realisasi

    note right of Projection
        RAB bertanggal, asumsi, jadwal kas rencana,
        BEP, B/C, dan kebutuhan modal kerja.
    end note

    note left of Realization
        Transaksi nyata, saldo kas,
        piutang, dan utang berjalan.
    end note
```
