package com.mankeu.exporter

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ImportBatchEntity
import com.mankeu.data.ImportNoticeEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ProvenanceEntity
import com.mankeu.data.ScenarioAssumptionEntity
import com.mankeu.data.toAnalysisAssumptions
import com.mankeu.domain.BookMode
import com.mankeu.domain.BusinessAnalysisCalculator
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.RecordStatus
import com.mankeu.domain.StockItemEntity
import com.mankeu.importer.MankeuTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class FinanceExportBook(
    val book: BookEntity,
    val budgets: List<BudgetItemEntity>,
    val events: List<FinancialEventEntity>,
    val summary: FinancialSummary,
    val assumptions: ScenarioAssumptionEntity?,
)

data class FinanceExportBundle(
    val business: BusinessEntity,
    val project: ProjectEntity,
    val categories: List<CategoryEntity>,
    val books: List<FinanceExportBook>,
    val importBatches: List<ImportBatchEntity>,
    val notices: List<ImportNoticeEntity>,
    val provenance: List<ProvenanceEntity>,
    val stockItems: List<StockItemEntity> = emptyList(),
    val generatedAtEpochMillis: Long = System.currentTimeMillis(),
)

/**
 * Writes [MankeuTemplate] (DEC-012). [template] is the blank form; [export] is the same sheets
 * filled from a project, so an export imports back without loss. The export also appends two
 * calculated sheets (Laporan, Kelayakan) that the importer ignores.
 */
class FinanceWorkbookExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun template(categories: List<Pair<String, CategoryRole>>): ByteArray = writer.write(
        listOf(
            guide(emptyList()),
            WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS, emptyList(), setOf(3, 4)),
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList(), setOf(5, 6)),
            bep(null),
            WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList(), setOf(2)),
            categoryTable(categories),
        ),
    )

    fun export(bundle: FinanceExportBundle): ByteArray {
        val books = bundle.books.associateBy { BookMode.valueOf(it.book.mode) }
        val plan = books[BookMode.PROJECTION]
        val actual = books[BookMode.REALIZATION]
        val categoryNames = bundle.categories.associate { it.id to it.name }
        return writer.write(
            listOf(
                guide(
                    listOf(
                        listOf("Usaha", bundle.business.name),
                        listOf("Unit usaha", bundle.project.name),
                        listOf("Dibuat pada", Instant.ofEpochMilli(bundle.generatedAtEpochMillis).toString()),
                    ),
                ),
                transactions(actual?.events.orEmpty()),
                budget(plan?.budgets.orEmpty(), categoryNames),
                bep(plan?.assumptions),
                stock(bundle.stockItems),
                categoryTable(bundle.categories.filter { it.active }.map { it.name to CategoryRole.parse(it.role) }),
                reportTable(bundle.books),
                feasibilityTable(bundle.books),
            ),
        )
    }

    private fun guide(context: List<List<Any?>>) = WorkbookTable(
        name = MankeuTemplate.GUIDE,
        headers = MankeuTemplate.GUIDE_HEADERS,
        rows = listOf(
            listOf("Format", MankeuTemplate.MARKER),
            listOf("Versi format", MankeuTemplate.VERSION),
        ) + context + listOf(
            listOf("Transaksi", "Satu baris satu transaksi buku Realisasi. Isi Pemasukan ATAU Pengeluaran saja. Tanggal YYYY-MM-DD."),
            listOf("Kategori", "Tulis nama kategori. Kategori yang belum ada dibuat otomatis saat impor. Lihat daftar di lembar Kategori."),
            listOf("Modal & prive", "Setoran modal: Pemasukan berkategori 'Setoran Modal Pemilik'. Ambil uang pribadi: Pengeluaran berkategori 'Ambil untuk Pribadi (Prive)'. Beli alat: Pengeluaran berkategori 'Beli Peralatan & Aset'."),
            listOf("Rencana Anggaran", "RAB buku Proyeksi. Bulan YYYY-MM. Isi Total, atau Jumlah dan Harga Satuan."),
            listOf("Parameter BEP", "Isi kolom Nilai saja; nama parameter jangan diubah. Biaya tetap per bulan."),
            listOf("Stok Barang", "Barang yang namanya sudah ada di aplikasi dilewati."),
            listOf("Aturan", "Jangan ubah nama lembar atau judul kolom. Lembar Laporan dan Kelayakan hanya hasil hitungan dan diabaikan saat impor."),
        ),
    )

    /** Posted, unreversed cash rows only: what an owner would write in a cash book. */
    private fun transactions(events: List<FinancialEventEntity>) = WorkbookTable(
        name = MankeuTemplate.TRANSACTIONS,
        headers = MankeuTemplate.TRANSACTION_HEADERS,
        rows = events
            .filter { it.status == RecordStatus.POSTED.name && it.reversedByEventId == null && it.type != FinancialEventType.REVERSAL.name }
            .sortedWith(compareBy({ it.dateEpochDay }, { it.createdAtEpochMillis }))
            .map { event ->
                val incoming = event.type == FinancialEventType.CASH_INCOME.name || event.type == FinancialEventType.OWNER_CAPITAL.name
                listOf(
                    LocalDate.ofEpochDay(event.dateEpochDay).toString(),
                    event.description,
                    event.category,
                    event.amountRupiah.takeIf { incoming },
                    event.amountRupiah.takeUnless { incoming },
                )
            },
        currencyColumns = setOf(3, 4),
    )

    private fun budget(items: List<BudgetItemEntity>, categoryNames: Map<String, String>) = WorkbookTable(
        name = MankeuTemplate.BUDGET,
        headers = MankeuTemplate.BUDGET_HEADERS,
        rows = items.filterNot { it.archived }.sortedBy { it.sortOrder }.map { item ->
            listOf(
                item.plannedDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)).toString() },
                categoryNames[item.categoryId] ?: item.categoryId,
                item.name,
                item.quantityText,
                item.unit,
                item.unitPriceRupiah,
                item.plannedTotalRupiah,
            )
        },
        currencyColumns = setOf(5, 6),
    )

    private fun bep(assumptions: ScenarioAssumptionEntity?) = WorkbookTable(
        name = MankeuTemplate.BEP,
        headers = MankeuTemplate.BEP_HEADERS,
        rows = listOf(
            listOf(MankeuTemplate.BEP_UNIT, assumptions?.outputUnit),
            listOf(MankeuTemplate.BEP_PRICE, assumptions?.outputPriceRupiah),
            listOf(MankeuTemplate.BEP_VARIABLE, assumptions?.variableCostPerUnitRupiah),
            listOf(MankeuTemplate.BEP_FIXED, assumptions?.fixedCostRupiah),
            listOf(MankeuTemplate.BEP_TARGET, assumptions?.outputQuantityText),
        ),
    )

    private fun stock(items: List<StockItemEntity>) = WorkbookTable(
        name = MankeuTemplate.STOCK,
        headers = MankeuTemplate.STOCK_HEADERS,
        rows = items.filterNot { it.archived }.map { listOf(it.name, it.unit, it.basePriceRupiah, it.currentStockText, it.minAlertStockText) },
        currencyColumns = setOf(2),
    )

    private fun categoryTable(categories: List<Pair<String, CategoryRole>>) = WorkbookTable(
        name = MankeuTemplate.CATEGORIES,
        headers = MankeuTemplate.CATEGORY_HEADERS,
        rows = categories.map { (name, role) -> listOf(name, roleLabel(role)) },
    )

    private fun reportTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Laporan",
        headers = listOf("Buku", "Metrik", "Nilai"),
        rows = books.flatMap { book ->
            val mode = BookMode.valueOf(book.book.mode).label()
            with(book.summary) {
                listOf(
                    listOf(mode, "Pendapatan", operatingIncomeRupiah),
                    listOf(mode, "Biaya operasional", operatingExpenseRupiah),
                    listOf(mode, "Laba atau rugi", profitRupiah),
                    listOf(mode, "Arus kas operasi", operatingCashFlowRupiah),
                    listOf(mode, "Arus kas investasi", investingCashFlowRupiah),
                    listOf(mode, "Arus kas pembiayaan (modal/prive)", financingCashFlowRupiah),
                    listOf(mode, "Saldo kas", cashBalanceRupiah),
                )
            }
        },
        currencyColumns = setOf(2),
    )

    private fun feasibilityTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Kelayakan",
        headers = listOf(
            "Buku", "Biaya Tetap (FC)", "Biaya Variabel/Unit (VC)", "Harga Jual Satuan", "Target Penjualan", "Satuan",
            "Omzet", "Total Biaya", "BEP Unit", "BEP Rupiah", "R/C Ratio", "B/C Ratio", "Status Kelayakan",
        ),
        rows = books.map { book ->
            val inputs = book.assumptions.toAnalysisAssumptions()
            val analysis = BusinessAnalysisCalculator.calculate(book.summary, inputs)
            listOf(
                BookMode.valueOf(book.book.mode).label(),
                inputs.fixedCostRupiah,
                inputs.variableCostPerUnitRupiah,
                inputs.outputPriceRupiah,
                inputs.outputQuantityText,
                inputs.outputUnit,
                analysis.estimatedRevenueRupiah,
                analysis.totalCostRupiah,
                analysis.breakEvenQuantity?.toPlainString(),
                analysis.breakEvenRevenueRupiah,
                analysis.revenueCostRatio?.toPlainString(),
                analysis.benefitCostRatio?.toPlainString(),
                analysis.feasible?.let { if (it) "LAYAK" else "BELUM LAYAK" },
            )
        },
        currencyColumns = setOf(1, 2, 3, 6, 7, 9),
    )

    private fun roleLabel(role: CategoryRole) = when (role) {
        CategoryRole.REVENUE -> "Pemasukan"
        CategoryRole.EXPENSE -> "Pengeluaran"
        CategoryRole.CAPITAL_IN -> "Setoran modal (Pemasukan)"
        CategoryRole.DRAWING -> "Ambil pribadi (Pengeluaran)"
        CategoryRole.ASSET -> "Beli aset (Pengeluaran)"
    }

    private fun BookMode.label(): String = when (this) {
        BookMode.PROJECTION -> "Proyeksi"
        BookMode.REALIZATION -> "Realisasi"
    }
}
