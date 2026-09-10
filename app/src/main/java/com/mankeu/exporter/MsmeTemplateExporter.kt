package com.mankeu.exporter

/**
 * Generates an intuitive, ready-to-use Excel (.xlsx) template for MSMEs (UMKM)
 * covering daily cash transactions, sales with unit COGS (HPP), business plan (Proyeksi), and assumptions.
 */
class MsmeTemplateExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun generateTemplate(): ByteArray {
        val tables = listOf(
            instructionsSheet(),
            dailyCashSheet(),
            salesAndCogsSheet(),
            planSheet(),
            assumptionsSheet(),
        )
        return writer.write(tables)
    }

    private fun instructionsSheet() = WorkbookTable(
        name = "Petunjuk Pengisian",
        headers = listOf("Topik", "Panduan Pengisian"),
        rows = listOf(
            listOf("Format Berkas", "Template Excel ini dirancang khusus untuk pembukuan UMKM dan kompatibel langsung dengan aplikasi Mankeu."),
            listOf("Format Tanggal", "Gunakan format tanggal YYYY-MM-DD (contoh: 2026-08-01) atau format tanggal standar Excel."),
            listOf("Lembar 'Buku Kas Harian'", "Gunakan lembar ini untuk mencatat uang masuk (penjualan tunai, modal, piutang cair) dan uang keluar (belanja stok, biaya operasional, gaji, dsb) yang benar-benar terjadi (Realisasi)."),
            listOf(
                "Kolom 'Jenis'",
                "Isi dengan salah satu: Uang Masuk, Uang Keluar, Penjualan Tempo, Belanja Tempo, Terima Piutang, Bayar Utang, " +
                    "Modal Masuk, Modal Keluar, Pinjaman Masuk, Bayar Pokok Pinjaman, Bayar Bunga, atau Beli Alat Tunai. " +
                    "Kolom ini yang membuat setoran modal, pencairan pinjaman, dan penjualan tempo tidak ikut terhitung sebagai omzet.",
            ),
            listOf("Lembar 'Penjualan dan HPP'", "Gunakan lembar ini untuk merinci produk atau jasa terjual beserta perhitungan Harga Pokok Penjualan (HPP) dan laba kotor per transaksi."),
            listOf("Lembar 'Rencana Usaha (Proyeksi)'", "Gunakan lembar ini untuk mencatat simulasi dan target rencana transaksi keuangan (Proyeksi) sebelum periode usaha berjalan."),
            listOf("Lembar 'Asumsi dan Target Usaha'", "Gunakan lembar ini untuk mendefinisikan target unit penjualan, estimasi harga satuan, dan modal kas awal untuk analisis kelayakan usaha."),
            listOf("Impor ke Aplikasi", "Buka menu 'Berkas dan Data' pada aplikasi Mankeu, lalu pilih 'Ambil dari Excel' untuk mengimpor data dari berkas ini."),
            listOf("Catatan Keamanan", "Data pembukuan Anda disimpan secara lokal dan privat pada perangkat Anda (100% offline-first)."),
        ),
    )

    private fun dailyCashSheet() = WorkbookTable(
        name = "Buku Kas Harian",
        headers = listOf("Tanggal", "Jenis", "Keterangan", "Kategori", "Pengeluaran (Rp)", "Pemasukan (Rp)", "Catatan"),
        rows = listOf(
            listOf("2026-08-01", "Modal Masuk", "Setoran Modal Awal", "Modal Sendiri", null, 5_000_000L, "Kas awal usaha, bukan omzet"),
            listOf("2026-08-01", "Uang Keluar", "Belanja Stok Bahan Baku", "Bahan Baku", 1_200_000L, null, "Beli bahan di pasar"),
            listOf("2026-08-01", "Uang Keluar", "Beli Kemasan dan Plastik", "Kemasan", 350_000L, null, "Kemasan 500 pcs"),
            listOf("2026-08-02", "Uang Masuk", "Penjualan Kasir Hari 1", "Penjualan Produk", null, 850_000L, "Omzet penjualan tunai"),
            listOf("2026-08-03", "Uang Keluar", "Tagihan Listrik dan Internet", "Operasional", 250_000L, null, "Tagihan bulanan"),
            listOf("2026-08-03", "Uang Masuk", "Penjualan Kasir Hari 2", "Penjualan Produk", null, 920_000L, "Omzet penjualan tunai"),
            listOf("2026-08-04", "Uang Keluar", "Gaji Karyawan Mingguan", "Tenaga Kerja", 600_000L, null, "Upah 1 karyawan"),
        ),
        currencyColumns = setOf(4, 5),
    )

    private fun salesAndCogsSheet() = WorkbookTable(
        name = "Penjualan dan HPP",
        headers = listOf(
            "Tanggal",
            "Nama Produk / Jasa",
            "Jumlah Terjual",
            "Satuan",
            "Harga Jual (Rp)",
            "Total Pendapatan (Rp)",
            "HPP Satuan (Rp)",
            "Total HPP (Rp)",
            "Laba Kotor (Rp)",
        ),
        rows = listOf(
            listOf("2026-08-02", "Paket Menu Spesial A", 50, "porsi", 25_000L, 1_250_000L, 14_000L, 700_000L, 550_000L),
            listOf("2026-08-03", "Paket Menu Hemat B", 40, "porsi", 18_000L, 720_000L, 10_000L, 400_000L, 320_000L),
            listOf("2026-08-04", "Minuman Es Segar", 60, "cup", 8_000L, 480_000L, 3_500L, 210_000L, 270_000L),
        ),
        currencyColumns = setOf(4, 5, 6, 7, 8),
    )

    private fun planSheet() = WorkbookTable(
        name = "Rencana Usaha (Proyeksi)",
        headers = listOf("Tanggal", "Jenis", "Keterangan", "Kategori", "Pengeluaran (Rp)", "Pemasukan (Rp)", "Catatan"),
        rows = listOf(
            listOf("2026-08-01", "Modal Masuk", "Modal Usaha Direncanakan", "Modal Sendiri", null, 5_000_000L, "Rencana modal awal"),
            listOf("2026-08-01", "Uang Keluar", "Belanja Bahan Baku", "Bahan Baku", 1_500_000L, null, "Kebutuhan bahan baku"),
            listOf("2026-08-01", "Uang Keluar", "Box Kemasan dan Stiker", "Kemasan", 600_000L, null, "Kebutuhan kemasan"),
            listOf("2026-08-01", "Uang Keluar", "Sewa Tempat Usaha", "Operasional", 1_500_000L, null, "Sewa tempat bulanan"),
            listOf("2026-08-01", "Beli Alat Tunai", "Peralatan Usaha", "Peralatan", 900_000L, null, "Peralatan operasional, masuk arus kas investasi"),
            listOf("2026-08-01", "Uang Keluar", "Gaji Karyawan", "Tenaga Kerja", 2_000_000L, null, "Gaji bulanan"),
            listOf("2026-08-02", "Uang Masuk", "Target Penjualan Produk", "Penjualan Produk", null, 8_000_000L, "Target omzet penjualan"),
        ),
        currencyColumns = setOf(4, 5),
    )

    private fun assumptionsSheet() = WorkbookTable(
        name = "Asumsi dan Target Usaha",
        headers = listOf("Parameter Asumsi", "Target / Nilai", "Satuan", "Keterangan"),
        rows = listOf(
            listOf("Target Volume Penjualan", "500", "porsi", "Estimasi unit produk terjual dalam satu siklus/periode"),
            listOf("Harga Jual Satuan", "25000", "Rp", "Harga jual rata-rata per unit produk atau jasa"),
            listOf("Modal Kas Awal", "5000000", "Rp", "Estimasi uang tunai/kas yang disiapkan sebelum siklus berjalan"),
        ),
    )
}
