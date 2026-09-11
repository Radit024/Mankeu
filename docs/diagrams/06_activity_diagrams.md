# Activity Diagrams (UML) - Mankeu Core Workflows

Dokumen ini memodelkan logika eksekusi 4 proses bisnis paling kritikal di aplikasi Mankeu:
1. **Pencatatan Transaksi & Penegakan Double-Entry**
2. **Pembatalan Transaksi Berpenjaga (*Guarded Reversal*)**
3. **Impor Workbook Excel & Penelusuran Jejak Asal (*Provenance Audit*)**
4. **Enkripsi Cadangan Portabel (*Zero-Knowledge Backup*) & Sinkronisasi Drive**

---

## 1. Alur Pencatatan Transaksi & Penegakan Double-Entry

Memastikan setiap transaksi keuangan yang dicatat selalu menghasilkan jurnal berpasangan yang seimbang secara matematis sebelum disimpan ke database SQLite.

```mermaid
flowchart TD
    Start([Mulai: Pengguna Klik Simpan Transaksi]) --> ValidateUI{"Validasi Form UI:\nNominal > 0, Keterangan Lengkap"}
    
    ValidateUI -->|Tidak Valid| ShowUIError[Tampilkan Pesan Error di Form] --> EndFailure([Selesai: Transaksi Batal])
    
    ValidateUI -->|Valid| LockButton[Kunci Tombol Simpan\nisSaving true\nCegah Race Condition / Double Submit]
    
    LockButton --> CreateDraft[Bentuk EventDraft\nHitung Rekonsiliasi Total dan Unit Price]
    
    CreateDraft --> CallRepo[FinanceRepository.recordEvent]
    
    CallRepo --> OpenTx[Mulai SQLite Transaction\ndatabase.withTransaction]
    
    OpenTx --> PolicyMapping[LedgerPolicy.journalFor: Petakan Akun\nPengeluaran Tunai:\n- Debit: OPERATING_EXPENSE\n- Credit: CASH]
    
    PolicyMapping --> BalanceCheck{"Periksa Invarian Double-Entry:\nSum Debit == Sum Credit?"}
    
    BalanceCheck -->|Tidak Seimbang| RollbackTx[Rollback Transaksi Basis Data\nThrow IllegalStateException]
    RollbackTx --> UnlockButton[Buka Kunci Tombol Simpan]
    UnlockButton --> ShowFatalError[Tampilkan Pesan Kegagalan Sistem]
    ShowFatalError --> EndFailure
    
    BalanceCheck -->|Seimbang| InsertEvent[Insert Baris ke financial_events]
    InsertEvent --> InsertJournal[Insert Baris ke journal_entries]
    InsertJournal --> InsertPostings[Insert Semua Baris ke postings]
    
    InsertPostings --> CheckObligation{"Apakah Jenis Transaksi\nMembuat Kewajiban?"}
    
    CheckObligation -->|Ya| InsertObligation[Insert Baris ke obligations\nCatat Sisa Piutang / Utang]
    CheckObligation -->|Tidak| CommitTx[Commit Transaksi Basis Data]
    InsertObligation --> CommitTx
    
    CommitTx --> TriggerFeedback[Mainkan Audio dan Haptic Feedback]
    TriggerFeedback --> ShowSnackbar[Tampilkan Snackbar Sukses]
    ShowSnackbar --> CloseSheet[Tutup Modal Bottom Sheet]
    CloseSheet --> RefreshFlows[Reactive StateFlow Emit State Terbaru ke Beranda]
    RefreshFlows --> EndSuccess([Selesai: Berhasil])
```

---

## 2. Alur Pembatalan Transaksi Berpenjaga (*Guarded Reversal*)

Aturan ketat integritas: Menolak pembatalan transaksi piutang/utang jika sudah ada pembayaran cicilan yang tercatat, mencegah timbulnya uang kas tak bertuan.

```mermaid
flowchart TD
    StartRev([Mulai: Pengguna Meminta Pembatalan Transaksi]) --> OpenRevTx[Mulai SQLite Transaction]
    
    OpenRevTx --> FetchEvent[Ambil Data Transaksi Asli berdasarkan eventId]
    
    FetchEvent --> CheckCreditEvent{"Apakah Transaksi Sumber\nMerupakan Penjualan/Pembelian Kredit?"}
    
    CheckCreditEvent -->|Ya| QuerySettlements[FinanceDao.countLiveSettlementsForSource\nHitung Pembayaran Cicilan Aktif]
    QuerySettlements --> HasSettlements{"Jumlah Pelunasan > 0?"}
    
    HasSettlements -->|Ya: Ditemukan Cicilan| BlockAction["TOLAK PEMBATALAN:\nTransaksi ini sudah memiliki cicilan aktif.\nBatalkan pembayaran cicilannya terlebih dahulu."]
    BlockAction --> RollbackRev[Rollback Transaksi]
    RollbackRev --> NotifyUser[Munculkan Dialog Penjelasan ke Pengguna]
    NotifyUser --> EndRevFail([Selesai: Dibatalkan Tanpa Perubahan Data])
    
    HasSettlements -->|Tidak Ada Cicilan| MarkObligationReversed[Update obligations: Set reversed = true]
    CheckCreditEvent -->|Tidak / Transaksi Tunai Biasa| FetchOriginalJournal[Ambil Jurnal Asli Transaksi]
    
    MarkObligationReversed --> FetchOriginalJournal
    
    FetchOriginalJournal --> CreateReversalEvent[Buat FinancialEvent Baru bertipe REVERSAL\n- amount = original.amount\n- reversesEventId = original.id]
    
    CreateReversalEvent --> InvertPostings[LedgerPolicy.reversalFor:\nBalik Semua Sisi Posting\nDebit jadi Credit, Credit jadi Debit]
    
    InvertPostings --> VerifyInvertedBalance{"Sum Debit == Sum Credit?"}
    VerifyInvertedBalance -->|Tidak| RollbackRev
    
    VerifyInvertedBalance -->|Seimbang| WriteReversalEntities[Insert Event Reversal dan Jurnal Pembalik]
    WriteReversalEntities --> MarkOriginalEvent[Update Event Asli: Set reversedByEventId]
    MarkOriginalEvent --> CommitRev[Commit Transaksi Database]
    CommitRev --> EmitUpdatedMetrics[Perbarui Saldo Kas dan Metrik di UI]
    EmitUpdatedMetrics --> EndRevSuccess([Selesai: Transaksi Berhasil Dibalik])
```

---

## 3. Alur Impor Excel Multi-Sheet & Jejak Audit Asal (*Provenance*)

Menafsirkan workbook Excel sebagai kesatuan lembar yang terkoordinasi (bukan file terpisah), melakukan klasifikasi otomatis semantik, dan merekam audit asal per sel.

```mermaid
flowchart TD
    StartImp([Mulai: Pengguna Memilih Berkas XLSX]) --> ComputeSHA[Hitung SHA-256 Checksum Berkas]
    
    ComputeSHA --> CheckDuplicate{"Apakah Checksum Ini Sudah Pernah Diimpor?"}
    CheckDuplicate -->|Pernah| RejectDuplicate["Tolak Berkas:\nWorkbook ini sudah pernah diimpor sebelumnya"] --> EndImpFail([Selesai])
    
    CheckDuplicate -->|Belum Pernah| ParseSheets[FinanceWorkbookImporter:\nBaca Lembar Koordinatif\n- Lembar Ringkasan / Parameter Asumsi\n- Lembar Anggaran / RAB Bertanggal\n- Lembar Catatan Kas Nyata]
    
    ParseSheets --> ReconcileItems[Cocokkan Kategori dan Pos Transaksi\nCategoryClassifier dan Fuzzy Matching]
    
    ReconcileItems --> GenerateNotices[Deteksi Ambigu dan Perbedaan:\n- Catat ImportNoticeEntity jika ada sel ragu\n- Catat ProvenanceEntity untuk asal sel]
    
    GenerateNotices --> BuildPreview[Susun ImportPreview:\n- Jumlah baris valid\n- Total nominal\n- Daftar notifikasi perhatian]
    
    BuildPreview --> DisplayPreview[Tampilkan Layar Pratinjau Impor ke Pengguna]
    
    DisplayPreview --> UserConfirm{"Pengguna Menyetujui Komit?"}
    UserConfirm -->|Batal| DiscardPreview[Hapus Pratinjau Memori] --> EndImpFail
    
    UserConfirm -->|Setuju Komit| OpenImpTx[Mulai SQLite Transaction]
    
    OpenImpTx --> InsertBatch[Insert Baris import_batches\nStatus: COMMITTED]
    InsertBatch --> InsertNotices[Insert Semua import_notices]
    InsertNotices --> InsertProvenance[Insert Semua provenance_records]
    InsertProvenance --> InsertBudgets[Insert Pos Anggaran ke budget_items]
    InsertBudgets --> InsertEvents[Insert Transaksi ke financial_events]
    InsertEvents --> GenerateJournals[Bentuk Jurnal Double-Entry untuk Setiap Transaksi Impor]
    
    GenerateJournals --> CommitImpTx[Commit Transaksi Basis Data]
    CommitImpTx --> RefreshBook[Buku Keuangan Siap Digunakan Lengkap dengan Riwayat]
    RefreshBook --> EndImpSuccess([Selesai: Impor Tuntas])
```

---

## 4. Alur Enkripsi Cadangan Portabel (*Zero-Knowledge Backup*)

Proses pengamanan data keuangan lokal menggunakan kriptografi standar industri tanpa ketergantungan server terpusat.

```mermaid
flowchart TD
    StartBkp([Mulai: Pengguna Meminta Buat Cadangan]) --> InputPass[Pengguna Memasukkan Kata Sandi\nMinimal 8 Karakter]
    
    InputPass --> ValidatePass{"Panjang Sandi >= 8?"}
    ValidatePass -->|Tidak| ShowPassError[Tampilkan Peringatan Panjang Sandi] --> EndBkpFail([Selesai])
    
    ValidatePass -->|Valid| ReadSnapshot[EncryptedBackupManager.capture:\nBaca seluruh baris 14 tabel database]
    
    ReadSnapshot --> BinaryEncode[PortableBackupCodec.encode:\nSerialisasi ke format biner portabel MANKEU-PORTABLE-DB]
    
    BinaryEncode --> GenSaltIV[SecureRandom:\n- Generate 16-byte Salt\n- Generate 12-byte GCM IV]
    
    GenSaltIV --> DeriveKey[PBKDF2WithHmacSHA256:\nTurunkan Kunci 256-bit dari Sandi\n310.000 Iterasi Penguatan]
    
    DeriveKey --> EncryptAES[Enkripsi AES-256-GCM:\n- Ciphertext data biner\n- AAD Header: MANKEU-BKP]
    
    EncryptAES --> BuildEnvelope[Bungkus Amplop Cadangan:\nMagic Header + Version + Salt + IV + Ciphertext]
    
    BuildEnvelope --> SaveOption{"Pilihan Penyimpanan Pengguna"}
    
    SaveOption -->|Berkas Lokal| WriteFile[Simpan berkas .aabk ke Storage ponsel via SAF]
    SaveOption -->|Google Drive| UploadDrive[Unggah terenkripsi ke AppDataFolder Google Drive]
    
    WriteFile --> SuccessNotify[Tampilkan Notifikasi Cadangan Sukses]
    UploadDrive --> SuccessNotify
    SuccessNotify --> ClearMem[Bersihkan Password CharArray dari Memori via fill spasi]
    ClearMem --> EndBkpSuccess([Selesai: Data Aman])
```
