# State Machine Diagrams (UML) - Mankeu Lifecycles

Dokumen ini mendeskripsikan mesin status (*State Machines*) untuk siklus hidup entitas vital:
1. **Siklus Hidup Kewajiban (Piutang & Utang)**
2. **Siklus Hidup Transaksi Keuangan (*Financial Event*)**
3. **Siklus Hidup Batch Impor (*Import Batch*)**
4. **Siklus Pergantian Mode Buku (*Book Mode*)**

---

## 1. Siklus Hidup Kewajiban (`Obligation`)

Menelusuri status tagihan piutang pelanggan atau utang dagang ke pemasok.

```mermaid
stateDiagram-v2
    [*] --> UNPAID : Transaksi Kredit Dibuat\n(CREDIT_SALE / CREDIT_PURCHASE)
    
    UNPAID --> PARTIALLY_PAID : Pelunasan Sebagian\n(0 < settledAmount < originalAmount)
    
    UNPAID --> SETTLED : Pelunasan Penuh\n(settledAmount == originalAmount)
    
    PARTIALLY_PAID --> PARTIALLY_PAID : Tambah Pembayaran Cicilan Baru\n(settledAmount < originalAmount)
    
    PARTIALLY_PAID --> SETTLED : Pembayaran Terakhir Lunas\n(settledAmount == originalAmount)
    
    UNPAID --> REVERSED : Transaksi Sumber Dibatalkan\n(Hanya jika BELUM ada pembayaran)
    
    note right of PARTIALLY_PAID
        Transisi ke REVERSED DIBLOKIR:
        Transaksi sumber kredit tidak dapat dibalik
        selama masih ada pembayaran cicilan aktif.
    end note

    SETTLED --> [*] : Kewajiban Tuntas
    REVERSED --> [*] : Kewajiban Gugur
```

---

## 2. Siklus Hidup Transaksi Keuangan (`FinancialEvent`)

Prinsip dasar akuntansi buku besar Mankeu: **Kekekalan Transaksi (*Immutability*)**. Transaksi yang sudah masuk buku besar tidak pernah di-*update* atau di-`DELETE` secara sembarangan, melainkan dinetralkan melalui jurnal pembalik (*reversal*).

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Pengguna Mengisi Formulir Masukan

    DRAFT --> POSTED : Validasi Sukses & Commit SQLite\n(Jurnal Debit-Kredit Terbentuk Seimbang)
    DRAFT --> DISCARDED : Batal / Validasi Gagal

    POSTED --> REVERSED : Reversal Dieksekusi Pengguna\n(Menghasilkan Event REVERSAL Baru bertanda pembalik)

    note right of POSTED
        Transaksi berstatus POSTED
        berkontribusi langsung terhadap:
        - Saldo Kas
        - Laba Rugi
        - Grafik Arus Kas
    end note

    note right of REVERSED
        Transaksi berstatus REVERSED
        tetap tersimpan di database
        sebagai jejak audit akuntansi permanen,
        namun netral dari saldo kas.
    end note

    DISCARDED --> [*]
    REVERSED --> [*]
```

---

## 3. Siklus Hidup Batch Impor (`ImportBatch`)

Setiap impor dari workbook Excel diperlakukan sebagai satu unit transaksi besar yang atomik dan dapat diputar balik (*reversible*).

```mermaid
stateDiagram-v2
    [*] --> PARSING : Membaca Seluruh Sheet Workbook XLSX
    
    PARSING --> PREVIEW : Ekstraksi & Rekonsiliasi Sukses\n(Notifikasi & Jejak Asal Terbentuk)
    PARSING --> FAILED : Format Berkas Rusak / Formula Tidak Valid
    
    PREVIEW --> COMMITTED : Pengguna Menyetujui Impor\n(Semua Item Ditulis ke Database)
    PREVIEW --> CANCELLED : Pengguna Menolak / Batal
    
    COMMITTED --> REVERSED : Pengguna Membatalkan Impor\n(reverseImportBatch membalik seluruh transaksi\ndan mengarsipkan item anggaran)

    FAILED --> [*]
    CANCELLED --> [*]
    REVERSED --> [*] : Data Impor Bersih Kembali
```

---

## 4. Siklus Pergantian Mode Buku (`BookMode`)

Aplikasi Mankeu membedakan secara tegas antara **Proyeksi** (simulasi rencana) dan **Realisasi** (fakta kas nyata) pada setiap unit/proyek bisnis.

```mermaid
stateDiagram-v2
    state "PROJECTION (Mode Rencana / Simulasi)" as Proj
    state "REALIZATION (Mode Kas Nyata)" as Real

    [*] --> Real : Peluncuran Default Aplikasi

    Real --> Proj : Pengguna Menggeser Tab Mode ke 'Proyeksi'
    note right of Proj
        Karakteristik Mode Proyeksi:
        - Warna Tema: Netral / Secondary M3
        - Menampilkan Perhitungan RAB & Jadwal
        - Menghitung BEP, B/C Ratio, dan Financing Gap
        - Tidak mempengaruhi saldo uang kas di saku
    end note

    Proj --> Real : Pengguna Menggeser Tab Mode ke 'Realisasi'
    note left of Real
        Karakteristik Mode Realisasi:
        - Warna Tema: Aksen Utama / Primary M3
        - Menampilkan Catatan Transaksi Kas Nyata
        - Menghitung Arus Kas Riil & Saldo Dompet
        - Menampilkan Hutang & Piutang Berjalan
    end note
```
