package com.arinahub.umkm.mankeu.exporter

/**
 * Generates an intuitive, ready-to-use Excel (.xlsx) template for MSMEs (UMKM)
 * covering daily cash transactions, sales with unit COGS (HPP), and budget planning (RAB).
 */
class MsmeTemplateExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun generateTemplate(): ByteArray {
        val tables = listOf(
            instructionsSheet(),
            dailyCashSheet(),
            salesAndCogsSheet(),
            budgetSheet(),
        )
        return writer.write(tables)
    }

    private fun instructionsSheet() = WorkbookTable(
        name = "Petunjuk Pengisian",
        headers = listOf("Topik", "Panduan Pengisian"),
        rows = listOf(
            listOf("Format Berkas", "Template Excel ini dirancang khusus untuk pembukuan UMKM dan kompatibel langsung dengan aplikasi UMKM Man Keu."),
            listOf("Format Tanggal", "Gunakan format tanggal YYYY-MM-DD (contoh: 2026-08-01) atau format tanggal standar Excel."),
            listOf("Lembar 'Buku Kas Harian'", "Gunakan lembar ini untuk mencatat uang masuk (penjualan tunai, piutang cair, modal) dan uang keluar (belanja stok, biaya operasional, gaji, dsb)."),
            listOf("Lembar 'Penjualan & HPP'", "Gunakan lembar ini untuk merinci produk atau jasa terjual beserta perhitungan Harga Pokok Penjualan (HPP) dan laba kotor per transaksi."),
            listOf("Lembar 'RAB & Anggaran'", "Gunakan lembar ini untuk menyusun rencana anggaran biaya operasional atau target penjualan sebelum periode usaha berjalan."),
            listOf("Impor ke Aplikasi", "Buka menu 'Berkas & Data' pada aplikasi UMKM Man Keu, lalu pilih 'Ambil dari Excel' untuk mengimpor data dari berkas ini."),
            listOf("Catatan Keamanan", "Data pembukuan Anda disimpan secara lokal dan privat pada perangkat (offline-first)."),
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

    private fun budgetSheet() = WorkbookTable(
        name = "RAB & Anggaran",
        headers = listOf("Kategori", "Item / Kebutuhan", "Jumlah", "Satuan", "Harga Satuan (Rp)", "Total Rencana (Rp)", "Keterangan"),
        rows = listOf(
            listOf("Bahan Baku", "Tepung dan Bumbu", 100, "kg", 15_000L, 1_500_000L, "Kebutuhan 2 minggu"),
            listOf("Kemasan", "Box Kemasan & Stiker", 500, "pcs", 1_200L, 600_000L, "Cetak kemasan baru"),
            listOf("Operasional", "Sewa Tempat Usaha", 1, "bulan", 1_500_000L, 1_500_000L, "Sewa tempat"),
            listOf("Peralatan", "Wajan & Kompor Tambahan", 2, "unit", 450_000L, 900_000L, "Peralatan masak"),
            listOf("Tenaga Kerja", "Upah Asisten Toko", 1, "bulan", 2_000_000L, 2_000_000L, "Gaji bulanan"),
        ),
        currencyColumns = setOf(4, 5),
    )
}
