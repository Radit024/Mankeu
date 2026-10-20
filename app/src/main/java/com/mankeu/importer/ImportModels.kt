package com.mankeu.importer

import java.time.LocalDate
import java.time.YearMonth

/**
 * The one Excel format Mankeu reads and writes (DEC-012). The blank template is the export with no
 * rows; [FinanceWorkbookImporter] reads exactly this and rejects anything else. Sheet names and
 * headers live here only, so the exporter and the importer cannot drift apart.
 */
object MankeuTemplate {
    const val MARKER = "MANKEU_TEMPLATE"
    const val VERSION = 2

    const val GUIDE = "Petunjuk"
    const val TRANSACTIONS = "Transaksi"
    const val BUDGET = "Rencana Anggaran"
    const val BEP = "Parameter BEP"
    const val STOCK = "Stok Barang"
    const val CATEGORIES = "Kategori"

    val TRANSACTION_HEADERS = listOf("Tanggal", "Keterangan", "Kategori", "Pemasukan (Rp)", "Pengeluaran (Rp)")
    val BUDGET_HEADERS = listOf("Bulan", "Kategori", "Item", "Jumlah", "Satuan", "Harga Satuan (Rp)", "Total (Rp)")
    val BEP_HEADERS = listOf("Parameter", "Nilai")
    val STOCK_HEADERS = listOf("Nama", "Satuan", "Harga Beli (Rp)", "Jumlah Stok", "Batas Minimum")
    val CATEGORY_HEADERS = listOf("Nama", "Peran")
    val GUIDE_HEADERS = listOf("Kunci", "Nilai")

    const val BEP_UNIT = "Satuan produk"
    const val BEP_PRICE = "Harga jual per satuan (Rp)"
    const val BEP_VARIABLE = "Biaya variabel per satuan (Rp)"
    const val BEP_FIXED = "Biaya tetap per bulan (Rp)"
    const val BEP_TARGET = "Target volume per bulan"
    val BEP_KEYS = listOf(BEP_UNIT, BEP_PRICE, BEP_VARIABLE, BEP_FIXED, BEP_TARGET)

    /** Sheets that must exist with exactly these headers in row 1. [CATEGORIES] is reference only. */
    val REQUIRED = linkedMapOf(
        GUIDE to GUIDE_HEADERS,
        TRANSACTIONS to TRANSACTION_HEADERS,
        BUDGET to BUDGET_HEADERS,
        BEP to BEP_HEADERS,
        STOCK to STOCK_HEADERS,
    )
}

/** One `Transaksi` row. The cash type is decided at commit from the category's role (DEC-011 K-2). */
data class ImportTransaction(
    val source: String,
    val date: LocalDate,
    val description: String,
    val categoryName: String,
    val incoming: Boolean,
    val amountRupiah: Long,
)

/** One `Rencana Anggaran` row, for the Proyeksi book. */
data class ImportBudgetLine(
    val source: String,
    val month: YearMonth?,
    val categoryName: String,
    val name: String,
    val quantityText: String?,
    val unit: String?,
    val unitPriceRupiah: Long?,
    val totalRupiah: Long,
)

/** The `Parameter BEP` sheet. Blank values stay null and leave the stored value untouched. */
data class ImportBepParameters(
    val unit: String? = null,
    val priceRupiah: Long? = null,
    val variableCostRupiah: Long? = null,
    val fixedCostRupiah: Long? = null,
    val targetQuantityText: String? = null,
) {
    val isEmpty: Boolean get() = listOfNotNull(unit, priceRupiah, variableCostRupiah, fixedCostRupiah, targetQuantityText).isEmpty()
}

/** One `Stok Barang` row. */
data class ImportStockItem(
    val source: String,
    val name: String,
    val unit: String,
    val priceRupiah: Long,
    val quantityText: String,
    val minAlertText: String,
)

data class WorkbookImportPreview(
    val sourceName: String,
    val sourceSha256: String,
    val transactions: List<ImportTransaction> = emptyList(),
    val budgetLines: List<ImportBudgetLine> = emptyList(),
    val bepParameters: ImportBepParameters? = null,
    val stockItems: List<ImportStockItem> = emptyList(),
    /** Problems that make the file unusable: not a template, renamed columns, invalid cells. */
    val errors: List<String> = emptyList(),
) {
    val canCommit: Boolean get() = errors.isEmpty() &&
        (transactions.isNotEmpty() || budgetLines.isNotEmpty() || bepParameters != null || stockItems.isNotEmpty())
    val rowCount: Int get() = transactions.size + budgetLines.size + stockItems.size
    val totalIncomeRupiah: Long get() = transactions.filter { it.incoming }.sumOf { it.amountRupiah }
    val totalExpenseRupiah: Long get() = transactions.filterNot { it.incoming }.sumOf { it.amountRupiah }
}
