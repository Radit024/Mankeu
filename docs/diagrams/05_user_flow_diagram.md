# User Flow Diagram - Mankeu

Alur navigasi sesuai `MankeuApp`: empat destinasi di `NavigationBar` (atau `AppRail` pada layar lebar), dua overlay yang dibuka dari bar atas, dan satu bottom sheet lingkup.

```mermaid
flowchart TD
    launch(["Buka aplikasi"]) --> workspace{"currentWorkspace menemukan usaha?"}
    workspace -->|"belum ada"| addBusiness["Tambah usaha<br/>kategori bawaan ikut dibuat"]
    addBusiness --> addProject["Tambah lahan atau siklus<br/>buku Proyeksi dan Realisasi ikut dibuat"]
    addProject --> home
    workspace -->|"ada"| home["Beranda<br/>kas, metrik, kelayakan, transaksi terbaru"]

    home --> scopeHeader["ScopeHeader"]
    scopeHeader -->|"ketuk ringkasan"| scopeSheet["ManageScopeSheet<br/>ganti, tambah, ubah, hapus atau arsipkan"]
    scopeHeader -->|"tombol mode"| modeChoice{"Buku aktif"}
    modeChoice -->|"Realisasi"| actualBook["Transaksi nyata<br/>warna primaryContainer"]
    modeChoice -->|"Proyeksi"| planBook["Rencana, RAB, asumsi<br/>warna secondaryContainer"]

    home --> navBar{"NavigationBar"}
    navBar -->|"Catat"| entry["Catat<br/>pilih jenis transaksi"]
    entry --> entryForm["Formulir<br/>jumlah, keterangan, tanggal, kategori"]
    entryForm --> validate{"Valid?"}
    validate -->|"tidak"| fieldError["Pesan galat di kolom"]
    fieldError --> entryForm
    validate -->|"ya"| saving["Simpan<br/>tombol nonaktif dan indikator berputar"]
    saving --> saved["Snackbar tersimpan<br/>formulir dikosongkan"]

    navBar -->|"Riwayat"| ledger["Riwayat<br/>cari dan saring Semua, Masuk, Keluar, Belum Lunas"]
    ledger --> eventAction{"Aksi pada transaksi"}
    eventAction -->|"Balik"| reverseCheck{"Pembalikan atau ada pembayaran aktif?"}
    reverseCheck -->|"ya"| refused["Snackbar penolakan<br/>menjelaskan langkah yang harus didahulukan"]
    reverseCheck -->|"tidak"| reversed["Jurnal pembalik tercatat"]
    eventAction -->|"Hapus"| deleteCheck{"Ada pembayaran aktif atau sisa tagihan tidak cukup?"}
    deleteCheck -->|"ya"| refused
    deleteCheck -->|"tidak"| deleted["Transaksi terhapus<br/>pasangan pembaliknya ikut terhapus"]

    navBar -->|"Laporan"| reports["Laporan"]
    reports --> reportTabs{"Tab"}
    reportTabs -->|"Ringkasan"| summaryTab["Laba rugi dan arus kas"]
    reportTabs -->|"Arus kas"| chartTab["Grafik bulanan Vico<br/>geser dan ketuk batang"]
    reportTabs -->|"Kelayakan"| feasibilityTab["Asumsi, BEP, B/C, modal kerja"]

    home -->|"ikon Data"| dataOverlay["Data<br/>impor, ekspor, cadangan, kategori"]
    dataOverlay --> importFlow["Pilih workbook, pratinjau, komit"]
    importFlow --> batchCard["Kartu batch<br/>dapat dibatalkan dan diimpor ulang"]
    dataOverlay --> backupFlow["Cadangan terenkripsi<br/>berkas lokal atau Google Drive"]
    home -->|"ikon Pengaturan"| settingsOverlay["Pengaturan<br/>tema, gerak, bahasa, akun, tur"]

    classDef screen fill:#E3F2FD,stroke:#1565C0,stroke-width:2px
    classDef blocked fill:#FFEBEE,stroke:#C62828,stroke-width:2px
    classDef success fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px
    class home,entry,ledger,reports,dataOverlay,settingsOverlay screen
    class refused,fieldError blocked
    class saved,reversed,deleted success
```

## Catatan kegunaan

1. **Catat hanya formulir, Riwayat hanya daftar** (D-070), sehingga pencarian dan saringan tidak tersembunyi di balik tombol segmen.
2. **Penolakan selalu menjelaskan jalan keluarnya.** Contohnya: batalkan pembayaran dulu, atau hapus pembalikan untuk memulihkan transaksi asli.
3. **Warna buku berbeda** agar transaksi nyata tidak tercatat ke buku rencana.
