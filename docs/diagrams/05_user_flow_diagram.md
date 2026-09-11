# User Flow Diagram - Mankeu Mobile Application

Diagram ini memetakan alur navigasi pengguna (*User Journey & Information Architecture*) mulai dari peluncuran aplikasi, penentuan lingkup (*scope*), pencatatan transaksi cepat, simulasi kelayakan, hingga impor berkas Excel.

```mermaid
flowchart TD
    %% Start
    StartApp([Buka Aplikasi Mankeu]) --> CheckScope{"Apakah Usaha dan Unit Sudah Dibuat?"}

    %% Onboarding Branch
    CheckScope -->|Belum Ada| SetupBusiness["Layar Setup Usaha Awal\n- Masukkan Nama Usaha\n- Buat Unit/Cabang Default\n- Pasang Kategori Default UMKM"]
    SetupBusiness --> InitBooks["Inisialisasi 2 Buku Otomatis:\n1. Buku Proyeksi\n2. Buku Realisasi"]
    InitBooks --> HomeScreen

    %% Main Shell
    CheckScope -->|Sudah Ada| HomeScreen["Layar Beranda (Home)\n- Saldo Kas Utama\n- 4 Metrik Kunci\n- Notifikasi Piutang/Utang"]

    %% Top Bar Scope and Mode Switching
    HomeScreen ---|Ketuk ScopeSummary| ScopeSheet["Bottom Sheet: Kelola Usaha\n- Ganti Usaha Aktif\n- Tambah/Ganti Unit/Cabang\n- Arsipkan Unit"]
    HomeScreen ---|Toggle Mode| SwitchMode{"Pilih Mode Buku"}
    SwitchMode -->|Mode Proyeksi| PlanTheme["Tema Berubah Nada Netral Plan\n- Menampilkan Asumsi dan RAB\n- Estimasi Kebutuhan Modal Kerja"]
    SwitchMode -->|Mode Realisasi| ActualTheme["Tema Berubah Nada Sukses Teal\n- Menampilkan Kas Nyata\n- Menampilkan Transaksi Berjalan"]

    %% Main Navigation Destinations
    HomeScreen --> NavBottom{"Navigasi Menu Utama"}
    
    %% Tab 1: Catat
    NavBottom -->|Tab Catat| EntryScreen["Layar Catat Transaksi"]
    EntryScreen --> SelectKind{"Pilih Jenis Transaksi"}
    
    SelectKind -->|Kas Cepat| CashForm["Pencatatan Tunai 5-Tap\n1. Ketuk Pendapatan / Pengeluaran\n2. Isi Nominal\n3. Isi Keterangan Singkat\n4. Kategori otomatis terisi\n5. Ketuk Simpan"]
    CashForm --> SaveAction["Simpan ke Buku Besar\n- Double-entry terbentuk otomatis\n- Haptic dan Audio Feedback\n- Saldo Kas Beranda Terupdate"]
    
    SelectKind -->|Kredit / Bon| CreditForm["Transaksi Tempo\n- Isi Pihak Ketiga (Pelanggan/Pemasok)\n- Tentukan Tanggal Jatuh Tempo\n- Simpan Kewajiban (Piutang/Utang)"]
    CreditForm --> SaveAction

    %% Tab 2: Laporan
    NavBottom -->|Tab Laporan| ReportsScreen["Layar Laporan Keuangan"]
    ReportsScreen --> ReportSections{"Pilih Bagian Laporan"}
    ReportSections -->|Laba Rugi dan Neraca| IncomeStatement["Ringkasan Laba Bersih dan\nMargin Operasional"]
    ReportSections -->|Arus Kas Bulanan| VicoChart["Grafik Batang Interaktif Vico\n- Geser horizontal untuk riwayat bulan\n- Sentuh batang untuk nominal pasti (Rp)"]
    ReportSections -->|Kelayakan Usaha| FeasibilitySection["Analisis Kelayakan\n- Buka Dialog Asumsi\n- Ubah Modal, Target Jual, Harga\n- Tinjau BEP dan Rasio BC"]
    ReportSections -->|Rencana vs Realisasi| CompareSection["Perbandingan Capaian\n- Deviasi Target Penjualan vs Realisasi\n- Selisih Pengeluaran vs Rencana"]

    %% Tab 3: Berkas dan Lainnya
    NavBottom -->|Tab Lainnya| MoreScreen["Menu Lainnya dan Data"]
    MoreScreen --> MoreActions{"Pilih Aksi Pengelolaan"}
    
    MoreActions -->|Impor Excel| ImportFlow["Berkas dan Data: Impor Excel\n1. Pilih Berkas XLSX\n2. Sistem Baca Multi-sheet Koordinatif\n3. Tinjau Pratinjau dan Notifikasi\n4. Eksekusi Komit Atomik"]
    ImportFlow --> AuditTrace["Jejak Audit Tersimpan\n(Dapat Dibalik Sewaktu-waktu)"]
    
    MoreActions -->|Ekspor Laporan| ExportFlow["Ekspor Data\n- Unduh Buku Besar Excel (.xlsx)\n- Unduh Laporan Resmi (.pdf)\n- Unduh Template Kosong UMKM"]

    MoreActions -->|Cadangan Terenkripsi| BackupFlow["Keamanan dan Cadangan (.aabk)\n1. Masukkan Sandi Pengguna (Min 8 Karakter)\n2. Enkripsi AES-256-GCM Lokal\n3. Simpan Berkas atau Sync Google Drive"]

    %% Reversal Path from History / Home
    HomeScreen --> DetailEvent["Detail Transaksi"]
    DetailEvent --> GuardCheck{"Apakah Transaksi Kredit Memiliki Pelunasan Aktif?"}
    GuardCheck -->|Ya| BlockReversal["Ditolak Sistem:\nBatalkan pelunasan terlebih dahulu"]
    GuardCheck -->|Tidak| AllowReversal["Reversal Berhasil:\n- Membalik Jurnal Kas\n- Mengembalikan Saldo"]

    %% Styling
    classDef action fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px;
    classDef warning fill:#FFF3E0,stroke:#EF6C00,stroke-width:2px;
    classDef screen fill:#E3F2FD,stroke:#1565C0,stroke-width:2px;
    classDef blocked fill:#FFEBEE,stroke:#C62828,stroke-width:2px;

    class CashForm,SaveAction,InitBooks action;
    class SetupBusiness,ScopeSheet,DetailEvent warning;
    class HomeScreen,EntryScreen,ReportsScreen,MoreScreen screen;
    class BlockReversal blocked;
```

## Evaluasi Kemudahan Penggunaan (*Ease of Use Audit*)

1. **Zero-Friction Cash Recording**:
   - Alur pencatatan pengeluaran operasional warung dirancang dalam 5 sentuhan layar tanpa membingungkan pengguna dengan terminologi debit/kredit akuntansi.
2. **Perlindungan Kesalahan Manusia (*Error Prevention*)**:
   - Sistem menolak pembatalan faktur piutang yang sudah menerima uang cicilan, mencegah piutang "hantu" tanpa jejak kas.
3. **Penyelarasan Warna Antarmuka**:
   - Mode Proyeksi (Simulasi) dan Realisasi (Kas Nyata) mengubah palet kontainer utama antarmuka secara visual sehingga pengguna tidak pernah salah mencatat transaksi nyata ke dalam simulasi.
