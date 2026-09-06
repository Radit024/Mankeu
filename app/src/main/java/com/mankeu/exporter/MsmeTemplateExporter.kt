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
            listOf("Lembar 'Penjualan & HPP'", "Gunakan lembar ini untuk merinci produk atau jasa terjual beserta perhitungan Harga Pokok Penjualan (HPP) dan laba kotor per transaksi."),
            listOf("Lembar 'Rencana Usaha (Proyeksi)'", "Gunakan lembar ini untuk mencatat simulasi dan target rencana transaksi keuangan (Proyeksi) sebelum periode usaha berjalan."),
            listOf("Lembar 'Asumsi & Target Usaha'", "Gunakan lembar ini untuk mendefinisikan target unit penjualan, estimasi harga satuan, dan modal kas awal untuk analisis kelayakan usaha."),
            listOf("Impor ke Aplikasi", "Buka menu 'Berkas & Data' pada aplikasi Mankeu, lalu pilih 'Ambil dari Excel' untuk mengimpor data dari berkas ini."),
            listOf("Catatan Keamanan", "Data pembukuan Anda disimpan secara lokal dan privat pada perangkat Anda (100% offline-first)."),
        ),
    )

    private fun dailyCashSheet() = WorkbookTable(
        name = "Buku Kas Harian",
        headers = listOf("Tanggal", "Keterangan", "Kategori", "Pengeluaran (Rp)", "Pemasukan (Rp)", "Catatan"),
        rows = listOf(
            listOf("2026-08-01", "Setoran Modal Awal", "Modal Sendiri", null, 5_000_000L, "Kas awal usaha"),
            listOf("2026-08-01", "Belanja Stok Bahan Baku", "Bahan Baku", 1_200_000L, null, "Beli bahan di pasar"),
            listOf("2026-08-01", "Beli Kemasan & Plastik", "Kemasan", 350_000L, null, "Kemasan 500 pcs"),
            listOf("2026-08-02", "Penjualan Kasir Hari 1", "Penjualan Produk", null, 850_000L, "Omzet penjualan tunai"),
            listOf("2026-08-03", "Tagihan Listrik & Internet", "Operasional", 250_000L, null, "Tagihan bulanan"),
            listOf("2026-08-03", "Penjualan Kasir Hari 2", "Penjualan Produk", null, 920_000L, "Omzet penjualan tunai"),
            listOf("2026-08-04", "Gaji Karyawan Mingguan", "Tenaga Kerja", 600_000L, null, "Upah 1 karyawan"),
        ),
        currencyColumns = setOf(3, 4),
    )

    private fun salesAndCogsSheet() = WorkbookTable(
        name = "Penjualan & HPP",
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
        headers = listOf("Tanggal", "Keterangan", "Kategori", "Pengeluaran (Rp)", "Pemasukan (Rp)", "Catatan"),
        rows = listOf(
            listOf("2026-08-01", "Modal Usaha Direncanakan", "Modal Sendiri", null, 5_000_000L, "Rencana modal awal"),
            listOf("2026-08-01", "Belanja Bahan Baku", "Bahan Baku", 1_500_000L, null, "Kebutuhan bahan baku"),
            listOf("2026-08-01", "Box Kemasan & Stiker", "Kemasan", 600_000L, null, "Kebutuhan kemasan"),
            listOf("2026-08-01", "Sewa Tempat Usaha", "Operasional", 1_500_000L, null, "Sewa tempat bulanan"),
            listOf("2026-08-01", "Peralatan Usaha", "Peralatan", 900_000L, null, "Peralatan operasional"),
            listOf("2026-08-01", "Gaji Karyawan", "Tenaga Kerja", 2_000_000L, null, "Gaji bulanan"),
            listOf("2026-08-02", "Target Penjualan Produk", "Penjualan Produk", null, 8_000_000L, "Target omzet penjualan"),
        ),
        currencyColumns = setOf(3, 4),
    )

    private fun assumptionsSheet() = WorkbookTable(
        name = "Asumsi & Target Usaha",
        headers = listOf("Parameter Asumsi", "Target / Nilai", "Satuan", "Keterangan"),
        rows = listOf(
            listOf("Target Volume Penjualan", "500", "porsi", "Estimasi unit produk terjual dalam satu siklus/periode"),
            listOf("Harga Jual Satuan", "25000", "Rp", "Harga jual rata-rata per unit produk atau jasa"),
            listOf("Modal Kas Awal", "5000000", "Rp", "Estimasi uang tunai/kas yang disiapkan sebelum siklus berjalan"),
        ),
    )
}
