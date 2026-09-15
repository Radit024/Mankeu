# Use Case Diagram - Mankeu

Mankeu adalah aplikasi satu pengguna tanpa peran atau hak akses bertingkat: satu pemilik usaha mengelola seluruh buku di perangkatnya. Aktor lain adalah sistem eksternal.

```mermaid
flowchart LR
    owner["Pemilik usaha UMKM"]
    workbookFile["Berkas Excel XLSX"]
    googleDrive["Google Drive appDataFolder"]
    firebaseAuth["Firebase Auth"]

    subgraph mankeuSystem ["Aplikasi Mankeu"]
        subgraph scopeModule ["1. Lingkup dan buku"]
            ucScope["Kelola usaha dan lahan"]
            ucMode["Beralih buku Proyeksi atau Realisasi"]
            ucCategory["Kelola kategori pembukuan"]
        end

        subgraph entryModule ["2. Pencatatan"]
            ucCash["Catat uang masuk atau keluar"]
            ucCredit["Catat penjualan atau pembelian kredit"]
            ucSettle["Catat pembayaran piutang atau utang"]
            ucFinancing["Catat modal, pinjaman, dan bunga"]
            ucBudget["Susun RAB bertanggal"]
        end

        subgraph correctionModule ["3. Koreksi berpenjaga"]
            ucReverse["Balik transaksi"]
            ucDelete["Hapus transaksi"]
            ucGuard["Periksa pembayaran aktif dan kecocokan sisa tagihan"]
        end

        subgraph analysisModule ["4. Laporan dan kelayakan"]
            ucDashboard["Pantau kas dan metrik di Beranda"]
            ucHistory["Cari dan saring Riwayat"]
            ucChart["Jelajahi grafik arus kas bulanan"]
            ucAssumptions["Isi asumsi kelayakan"]
            ucFeasibility["Hitung BEP, B/C, dan kebutuhan modal kerja"]
            ucCompare["Bandingkan rencana dan realisasi"]
        end

        subgraph dataModule ["5. Data"]
            ucImport["Impor workbook"]
            ucReverseBatch["Batalkan batch impor"]
            ucExport["Ekspor Excel, PDF, atau templat"]
            ucBackup["Buat cadangan terenkripsi"]
            ucRestore["Pulihkan cadangan"]
        end
    end

    owner --> ucScope
    owner --> ucMode
    owner --> ucCategory
    owner --> ucCash
    owner --> ucCredit
    owner --> ucSettle
    owner --> ucFinancing
    owner --> ucBudget
    owner --> ucReverse
    owner --> ucDelete
    owner --> ucDashboard
    owner --> ucHistory
    owner --> ucChart
    owner --> ucAssumptions
    owner --> ucCompare
    owner --> ucImport
    owner --> ucReverseBatch
    owner --> ucExport
    owner --> ucBackup
    owner --> ucRestore

    ucReverse -.->|"include"| ucGuard
    ucDelete -.->|"include"| ucGuard
    ucAssumptions -.->|"include"| ucFeasibility
    ucReverseBatch -.->|"include"| ucReverse
    workbookFile -.->|"dibaca"| ucImport
    ucExport -.->|"menghasilkan"| workbookFile
    ucBackup -.->|"opsional, ciphertext"| googleDrive
    googleDrive -.->|"unduh"| ucRestore
    firebaseAuth -.->|"identitas untuk Drive"| ucBackup
```

| Use case | Tujuan bagi pemilik usaha |
| --- | --- |
| Catat uang masuk atau keluar | Mengganti buku kas tulis dengan beberapa ketukan, tanpa istilah debit dan kredit. |
| Koreksi berpenjaga | Membalik atau menghapus catatan yang salah tanpa pernah membuat saldo kas, piutang, atau utang menjadi tidak cocok. |
| Kelayakan | Menjawab "berapa modal yang harus disiapkan" dan "berapa unit harus terjual agar tidak rugi". |
| Impor workbook | Membawa pembukuan Excel yang sudah ada tanpa mengetik ulang. Setiap batch dapat dibatalkan dan diimpor ulang. |
| Cadangan terenkripsi | Memindahkan atau menyelamatkan data tanpa pihak mana pun dapat membaca isinya. |
