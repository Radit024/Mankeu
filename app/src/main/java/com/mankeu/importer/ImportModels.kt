package com.mankeu.importer

import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import java.time.LocalDate
import java.time.YearMonth

/**
 * The one Excel format Mankeu reads and writes (DEC-012). The blank template is the export with no
 * rows; [FinanceWorkbookImporter] reads exactly this and rejects anything else. Sheet names and
 * headers live here only, so the exporter and the importer cannot drift apart.
 *
 * Version 3 (DEC-014): products and recipes replace the BEP parameter sheet, and plan lines and
 * transactions carry cost nature, recurrence, asset life, product and quantity.
 */
object MankeuTemplate {
    const val MARKER = "MANKEU_TEMPLATE"
    const val VERSION = 4

    const val GUIDE = "Petunjuk"
    const val TRANSACTIONS = "Transaksi"
    const val BUDGET = "Rencana Anggaran"
    const val PRODUCTS = "Produk"
    const val RECIPES = "Resep Produk"
    const val STOCK = "Stok Barang"
    const val CATEGORIES = "Kategori"

    val TRANSACTION_HEADERS = listOf(
        "Tanggal", "Keterangan", "Kategori", "Pemasukan (Rp)", "Pengeluaran (Rp)",
        "Produk", "Jumlah", "Sifat Biaya", "Umur Pakai (bulan)",
    )
    val BUDGET_HEADERS = listOf(
        "Bulan", "Kategori", "Item", "Jumlah", "Satuan", "Harga Satuan (Rp)", "Total (Rp)",
        "Berulang", "Sifat Biaya", "Umur Pakai (bulan)",
    )
    val PRODUCT_HEADERS = listOf("Nama", "Satuan", "Harga Jual (Rp)", "Target per Bulan")
    val RECIPE_HEADERS = listOf("Produk", "Komponen", "Biaya (Rp)", "Untuk Berapa Unit")
    val STOCK_HEADERS = listOf("Nama", "Satuan", "Harga Beli (Rp)", "Jumlah Stok", "Batas Minimum")
    val CATEGORY_HEADERS = listOf("Nama", "Peran", "Sifat Biaya")
    val GUIDE_HEADERS = listOf("Kunci", "Nilai")

    const val NATURE_FIXED = "Tetap"
    const val NATURE_VARIABLE = "Variabel"
    const val MONTHLY = "Bulanan"
    /** Optional `Petunjuk` row: the unit's plan period as `YYYY-MM s.d. YYYY-MM`. */
    const val PLAN_PERIOD = "Periode rencana"
    const val PERIOD_SEPARATOR = " s.d. "
    const val ONCE = "Sekali"

    /**
     * Columns added after a sheet was first published, in order after its required headers. A file
     * from before (v3) leaves them out and still imports; when present, the header must match.
     */
    val OPTIONAL = mapOf(PRODUCTS to listOf("Mulai Dijual (bulan ke-)"))

    /** Sheets that must exist with exactly these headers in row 1. [CATEGORIES] is reference only. */
    val REQUIRED = linkedMapOf(
        GUIDE to GUIDE_HEADERS,
        TRANSACTIONS to TRANSACTION_HEADERS,
        BUDGET to BUDGET_HEADERS,
        PRODUCTS to PRODUCT_HEADERS,
        RECIPES to RECIPE_HEADERS,
        STOCK to STOCK_HEADERS,
    )

    fun natureLabel(nature: CostNature?): String? = when (nature) {
        CostNature.FIXED -> NATURE_FIXED
        CostNature.VARIABLE -> NATURE_VARIABLE
        null -> null
    }
}

/** One `Transaksi` row. The cash type is decided at commit from the category's role (DEC-011 K-2). */
data class ImportTransaction(
    val source: String,
    val date: LocalDate,
    val description: String,
    val categoryName: String,
    val incoming: Boolean,
    val amountRupiah: Long,
    val productName: String? = null,
    val quantityText: String? = null,
    val costNature: CostNature? = null,
    val usefulLifeMonths: Int? = null,
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
    val recurrence: Recurrence = Recurrence.ONCE,
    val costNature: CostNature? = null,
    val usefulLifeMonths: Int? = null,
)

data class ImportComponent(val name: String, val amountRupiah: Long, val yieldText: String)

/** One `Produk` row with its `Resep Produk` lines. */
data class ImportProduct(
    val source: String,
    val name: String,
    val unit: String,
    val priceRupiah: Long,
    val targetPerMonthText: String?,
    val components: List<ImportComponent>,
    val sellFromMonth: Int = 1,
)

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
    val products: List<ImportProduct> = emptyList(),
    val stockItems: List<ImportStockItem> = emptyList(),
    /** The plan period the file was exported with; applied to a unit that has none yet. */
    val planPeriod: Pair<YearMonth, YearMonth>? = null,
    /** Problems that make the file unusable: not a template, renamed columns, invalid cells. */
    val errors: List<String> = emptyList(),
) {
    val canCommit: Boolean get() = errors.isEmpty() &&
        (transactions.isNotEmpty() || budgetLines.isNotEmpty() || products.isNotEmpty() || stockItems.isNotEmpty())
    val rowCount: Int get() = transactions.size + budgetLines.size + products.size + stockItems.size
    val totalIncomeRupiah: Long get() = transactions.filter { it.incoming }.sumOf { it.amountRupiah }
    val totalExpenseRupiah: Long get() = transactions.filterNot { it.incoming }.sumOf { it.amountRupiah }
}
