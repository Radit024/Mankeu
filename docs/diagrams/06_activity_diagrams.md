# Activity Diagrams - Alur Bisnis Kritis Mankeu

Lima alur yang menulis atau mengoreksi uang. Semua pemeriksaan di bawah ini berada di `FinanceRepository`, di dalam `withTransaction`: satu penolakan membatalkan seluruh penulisan.

## 1. Mencatat transaksi

```mermaid
flowchart TD
    tap(["Ketuk Simpan"]) --> uiGuard{"isSaving atau justSaved?"}
    uiGuard -->|"ya"| ignoreTap(["Ketukan diabaikan"])
    uiGuard -->|"tidak"| formValid{"Formulir valid?"}
    formValid -->|"tidak"| formError(["Galat di kolom"])
    formValid -->|"ya"| mutating["AppViewModel.mutating<br/>isSaving compareAndSet"]
    mutating --> reconcile["TransactionCalculation.reconcile<br/>total, kuantitas, harga satuan"]
    reconcile --> openTx["withTransaction"]
    openTx --> ruleAmount{"Jumlah lebih dari 0?"}
    ruleAmount -->|"tidak"| rollback["Rollback dan snackbar penolakan"]
    ruleAmount -->|"ya"| ruleKind{"Jenis pembayaran atau REVERSAL?"}
    ruleKind -->|"ya"| rollback
    ruleKind -->|"tidak"| ruleScope{"Usaha, lahan, buku, dan kategori cocok?"}
    ruleScope -->|"tidak"| rollback
    ruleScope -->|"ya"| ruleParty{"Transaksi kredit tanpa nama pihak?"}
    ruleParty -->|"ya"| rollback
    ruleParty -->|"tidak"| journal["LedgerPolicy.journalFor<br/>requireBalanced"]
    journal --> recheck{"Debit sama dengan kredit dan semua posting positif?"}
    recheck -->|"tidak"| rollback
    recheck -->|"ya"| insertRows["Tulis event, jurnal, dan posting"]
    insertRows --> creditKind{"Transaksi kredit?"}
    creditKind -->|"ya"| insertObligation["Tulis obligation"]
    creditKind -->|"tidak"| commitTx["Commit"]
    insertObligation --> commitTx
    commitTx --> savedDone(["Snackbar tersimpan, Flow Room memperbarui layar"])
    rollback --> failDone(["Tidak ada baris yang tertulis"])
```

## 2. Membalik transaksi

```mermaid
flowchart TD
    askReverse(["Pengguna memilih Balik"]) --> loadEvent["Ambil event"]
    loadEvent --> alreadyReversed{"Sudah dibalik?"}
    alreadyReversed -->|"ya"| refuseReverse["Tolak"]
    alreadyReversed -->|"tidak"| isReversal{"Event itu sendiri REVERSAL?"}
    isReversal -->|"ya"| refuseReverse
    isReversal -->|"tidak"| livePayments{"Ada pembayaran aktif atas piutang atau utangnya?"}
    livePayments -->|"ya"| refuseReverse
    livePayments -->|"tidak"| buildReversal["Buat event REVERSAL<br/>cashFlowRole sama dengan sumber"]
    buildReversal --> invert["LedgerPolicy.reversalFor<br/>tukar sisi setiap posting"]
    invert --> writeReversal["Tulis event dan jurnal pembalik"]
    writeReversal --> markSource{"Tandai sumber REVERSED<br/>tepat satu baris berubah?"}
    markSource -->|"tidak"| refuseReverse
    markSource -->|"ya"| markObligation["Tandai obligation sumber reversed"]
    markObligation --> reverseDone(["Commit"])
    refuseReverse --> reverseFail(["Rollback dan pesan penjelasan"])
```

## 3. Menghapus transaksi

```mermaid
flowchart TD
    askDelete(["Pengguna memilih Hapus"]) --> loadDeleted["Ambil event"]
    loadDeleted --> deletePayments{"Ada pembayaran aktif atas piutang atau utangnya?"}
    deletePayments -->|"ya"| refuseDelete["Tolak"]
    deletePayments -->|"tidak"| isReversalRow{"Event ini REVERSAL?"}
    isReversalRow -->|"ya"| restoreSource["Pulihkan sumber menjadi POSTED<br/>pulihkan obligation sumber"]
    restoreSource --> sourceIsPayment{"Sumbernya pembayaran?"}
    sourceIsPayment -->|"ya"| paymentFits{"Obligation belum dibatalkan dan pembayaran masih muat di sisa tagihan?"}
    paymentFits -->|"tidak"| refuseDelete
    paymentFits -->|"ya"| hasReversal
    sourceIsPayment -->|"tidak"| hasReversal
    isReversalRow -->|"tidak"| hasReversal{"Event ini sudah dibalik?"}
    hasReversal -->|"ya"| dropPair["Hapus event pembaliknya juga"]
    hasReversal -->|"tidak"| dropEvent["Hapus event"]
    dropPair --> dropEvent
    dropEvent --> cascade["CASCADE: jurnal, posting, obligation, settlement"]
    cascade --> deleteDone(["Commit"])
    refuseDelete --> deleteFail(["Rollback dan pesan penjelasan"])
```

## 4. Impor workbook dan pembatalan batch

```mermaid
flowchart TD
    pickFile(["Pilih berkas"]) --> readBytes["Baca maksimal 25 MB, hitung SHA-256"]
    readBytes --> unzip["XlsxWorkbookReader<br/>maks 1.000 entri, 64 MB setelah dekompresi,<br/>berhenti begitu batas terlewati"]
    unzip --> parseXml["Parser DOM platform<br/>DOCTYPE dan entitas eksternal ditolak"]
    parseXml --> recognize["Kenali lembar: parameter, RAB, harian, laba rugi, arus kas"]
    recognize --> preview["Pratinjau: kandidat, RAB, asumsi, peringatan"]
    preview --> confirmImport{"Pengguna menyetujui?"}
    confirmImport -->|"tidak"| cancelImport(["Pratinjau dibuang"])
    confirmImport -->|"ya"| importTx["inTransaction"]
    importTx --> scope["Buat usaha dan lahan bila belum ada"]
    scope --> duplicate{"Hash sama dengan batch COMMITTED?"}
    duplicate -->|"ya"| importRollback(["Rollback, usaha dan lahan baru ikut batal"])
    duplicate -->|"tidak"| batchRow["Tulis import_batches COMMITTED"]
    batchRow --> eachRow["Setiap kandidat: event dan jurnal,<br/>obligation atau settlement,<br/>provenance dan notice bila hasil inferensi"]
    eachRow --> budgets["Setiap baris RAB: temukan, aktifkan lagi, atau buat kategori,<br/>lalu tulis budget_items"]
    budgets --> assumptions["Asumsi ke buku Proyeksi, dengan provenance dan notice"]
    assumptions --> importCommit(["Commit, lalu pilih usaha dan lahan"])

    reverseBatch(["Batalkan batch"]) --> activeRows["Ambil event aktif batch"]
    activeRows --> paymentsFirst["Urutkan pembayaran lebih dulu"]
    paymentsFirst --> reverseEach["reverseEvent untuk setiap baris"]
    reverseEach --> archiveBudgets["Arsipkan RAB batch"]
    archiveBudgets --> markBatch["Batch menjadi REVERSED<br/>berkas yang sama boleh diimpor ulang"]
```

## 5. Cadangan dan pemulihan terenkripsi

```mermaid
flowchart TD
    startBackup(["Buat cadangan"]) --> passwordLength{"Kata sandi minimal 8 karakter?"}
    passwordLength -->|"tidak"| backupRefused(["Tolak"])
    passwordLength -->|"ya"| capture["Snapshot 14 tabel dalam satu transaksi baca"]
    capture --> encode["PortableBackupCodec<br/>MANKEU-PORTABLE-DB"]
    encode --> derive["Salt 16 byte, IV 12 byte<br/>PBKDF2-HMAC-SHA256 310.000 iterasi"]
    derive --> encrypt["AES-256-GCM, AAD MANKEU-BKP"]
    encrypt --> destination{"Tujuan"}
    destination -->|"berkas"| safFile["Tulis lewat Storage Access Framework"]
    destination -->|"Drive"| driveUpload["Unggah multipart ke appDataFolder<br/>sisakan 5 cadangan terbaru"]
    safFile --> wipePassword(["Kata sandi dihapus dari memori"])
    driveUpload --> wipePassword

    startRestore(["Pulihkan"]) --> decrypt["Dekripsi dan dekode, galat bila sandi salah"]
    decrypt --> schemaCheck{"Schema cadangan antara 3 dan versi aplikasi?"}
    schemaCheck -->|"tidak"| restoreRefused(["Tolak"])
    schemaCheck -->|"ya"| previewRestore["Pratinjau jumlah usaha, lahan, RAB, transaksi"]
    previewRestore --> recoveryCopy["Salinan pemulihan otomatis terenkripsi"]
    recoveryCopy --> replaceTx["Satu transaksi: kosongkan tabel,<br/>isi hanya kolom yang masih ada"]
    replaceTx --> restoreDone(["Pilih ulang usaha dan lahan"])
```
