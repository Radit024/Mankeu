# Use Case Diagram - Mankeu

Diagram ini menggambarkan interaksi antara aktor (Pelaku Usaha UMKM, Operator/Kasir, Berkas Eksternal Excel, dan Google Drive) dengan sistem Mankeu.

```mermaid
flowchart LR
    %% Actors
    ActorOwner["👤 Pelaku Usaha / Pemilik UMKM\n(Primary Actor)"]
    ActorCashier["👤 Operator / Kasir\n(Staff)"]
    ActorExcel["📄 Berkas Eksternal\n(XLSX Workbook)"]
    ActorDrive["☁️ Google Drive\n(Optional Cloud Sync)"]

    %% Boundaries
    subgraph SystemMankeu ["Aplikasi Mankeu (Sistem Akuntansi dan Analisis Kelayakan)"]
        direction TB

        subgraph ScopeModule ["1. Pengelolaan Lingkup dan Buku"]
            UC_ManageScope["Kelola Usaha dan Unit Cabang"]
            UC_SwitchMode["Beralih Mode Buku\n(Proyeksi Simulasi vs Realisasi Nyata)"]
        end

        subgraph EntryModule ["2. Pencatatan Transaksi dan Piutang Utang"]
            UC_RecordCash["Catat Kas Cepat\n(Pendapatan dan Pengeluaran Tunai)"]
            UC_RecordCredit["Catat Transaksi Kredit\n(Piutang Pelanggan dan Utang Pemasok)"]
            UC_SettleObligation["Catat Pelunasan\n(Cicilan Piutang dan Utang)"]
            UC_RecordFinancing["Catat Modal dan Pinjaman\n(Setor Tarik Modal, Pinjaman)"]
            UC_ReverseEvent["Balik Transaksi Reversal\n(Guarded: Block if settled)"]
        end

        subgraph FeasibilityModule ["3. Analisis Kelayakan Usaha"]
            UC_InputAssumptions["Input Asumsi Operasional\n(Modal, Kapasitas, Harga Jual, Bunga)"]
            UC_CalcGap["Hitung Kebutuhan Modal Kerja\n(Financing Gap / Defisit Kas Terdalam)"]
            UC_CalcBEP["Hitung Titik Impas\n(BEP Unit dan BEP Rupiah)"]
            UC_EvaluateFeasibility["Evaluasi Kelayakan Usaha\n(BC Ratio dan Net Margin)"]
        end

        subgraph ReportModule ["4. Laporan dan Visualisasi"]
            UC_ViewDashboard["Pantau Saldo Kas dan Metrik Kunci Beranda"]
            UC_InteractiveChart["Eksplorasi Grafik Arus Kas Vico\n(Horizontal Scroll dan Tooltip Nominal)"]
            UC_FinancialStatements["Lihat Laba Rugi dan Arus Kas Lengkap"]
            UC_ComparePlanActual["Bandingkan Target vs Realisasi"]
        end

        subgraph IntegrationModule ["5. Impor Ekspor dan Rekonsiliasi"]
            UC_ImportExcel["Impor Workbook Excel Multi-sheet"]
            UC_AuditNotices["Tinjau Notifikasi dan Jejak Rekonsiliasi"]
            UC_ExportExcel["Ekspor Buku Besar dan Laporan ke Excel"]
            UC_ExportPdf["Ekspor Laporan Resmi ke PDF"]
        end

        subgraph SecurityModule ["6. Cadangan dan Sinkronisasi"]
            UC_LocalBackup["Buat Cadangan Lokal Terenkripsi (.aabk)"]
            UC_RestoreBackup["Pulihkan Cadangan Terenkripsi"]
            UC_DriveSync["Sinkronisasi Cadangan ke Google Drive"]
        end
    end

    %% Actor Connections
    ActorCashier --> UC_RecordCash
    ActorCashier --> UC_RecordCredit
    ActorCashier --> UC_SettleObligation
    ActorCashier --> UC_ViewDashboard

    ActorOwner --> UC_ManageScope
    ActorOwner --> UC_SwitchMode
    ActorOwner --> UC_RecordCash
    ActorOwner --> UC_RecordCredit
    ActorOwner --> UC_SettleObligation
    ActorOwner --> UC_RecordFinancing
    ActorOwner --> UC_ReverseEvent
    ActorOwner --> UC_InputAssumptions
    ActorOwner --> UC_CalcGap
    ActorOwner --> UC_CalcBEP
    ActorOwner --> UC_EvaluateFeasibility
    ActorOwner --> UC_ViewDashboard
    ActorOwner --> UC_InteractiveChart
    ActorOwner --> UC_FinancialStatements
    ActorOwner --> UC_ComparePlanActual
    ActorOwner --> UC_ImportExcel
    ActorOwner --> UC_AuditNotices
    ActorOwner --> UC_ExportExcel
    ActorOwner --> UC_ExportPdf
    ActorOwner --> UC_LocalBackup
    ActorOwner --> UC_RestoreBackup
    ActorOwner --> UC_DriveSync

    %% External Systems
    ActorExcel -.->|Reads xlsx| UC_ImportExcel
    UC_ExportExcel -.->|Generates xlsx| ActorExcel
    UC_DriveSync ---|Syncs encrypted aabk| ActorDrive

    %% Include / Extend relationships
    UC_InputAssumptions -.->|includes| UC_CalcGap
    UC_InputAssumptions -.->|includes| UC_CalcBEP
    UC_InputAssumptions -.->|includes| UC_EvaluateFeasibility
    UC_ImportExcel -.->|generates| UC_AuditNotices
    UC_ReverseEvent -.->|validates| UC_SettleObligation
```

## Deskripsi Ringkas Skenario Utama

| Use Case | Sasaran Bisnis Pengguna |
| :--- | :--- |
| **Catat Kas Cepat** | Menggantikan pencatatan buku tulis kasir dengan input 4-5 tap tanpa beban istilah akuntansi rumit. |
| **Analisis Kelayakan** | Menjawab pertanyaan krusial UMKM: *"Berapa uang yang harus disiapkan sebelum buka usaha?"* (Financing Gap) dan *"Berapa mangkok/porsi harus laku agar tidak rugi?"* (BEP). |
| **Beralih Mode Buku** | Memastikan rencana anggaran (RAB) dan realisasi transaksi berjalan di dua wadah terisolasi penuh sehingga data nyata tidak terkotori oleh angka perkiraan. |
| **Impor & Rekonsiliasi** | Mengizinkan UMKM membawa data pembukuan Excel yang sudah berjalan tanpa perlu mengetik ulang transaksi masa lalu satu per satu. |
| **Grafik Arus Kas Vico** | Memvisualisasikan naik-turunnya kas bulanan secara interaktif dengan kemampuan digeser dan disentuh untuk melihat angka rupiah pastinya. |
