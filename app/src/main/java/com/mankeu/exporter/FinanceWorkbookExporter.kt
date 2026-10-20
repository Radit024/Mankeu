package com.mankeu.exporter

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ImportBatchEntity
import com.mankeu.data.ImportNoticeEntity
import com.mankeu.data.PlanAssembly
import com.mankeu.data.ProductWithComponents
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ProvenanceEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.Recurrence
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
    val products: List<ProductWithComponents> = emptyList(),
    /** The business plan's BEP (DEC-014), computed by the repository from the same rows. */
    val plan: PlanAnalysis? = null,
    val generatedAtEpochMillis: Long = System.currentTimeMillis(),
)

/** A category as the template's reference sheet lists it. */
data class TemplateCategory(val name: String, val role: CategoryRole, val nature: CostNature?)

/**
 * Writes [MankeuTemplate] (DEC-012, v3). [template] is the blank form; [export] is the same sheets
 * filled from a business unit, so an export imports back without loss. The export also appends
 * two calculated sheets (Laporan, Kelayakan) that the importer ignores.
 */
class FinanceWorkbookExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun template(categories: List<TemplateCategory>): ByteArray = writer.write(
        listOf(
            guide(emptyList()),
            WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS, emptyList(), setOf(3, 4)),
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList(), setOf(5, 6)),
            WorkbookTable(MankeuTemplate.PRODUCTS, MankeuTemplate.PRODUCT_HEADERS, emptyList(), setOf(2)),
            WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, emptyList(), setOf(2)),
            WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList(), setOf(2)),
            categoryTable(categories),
        ),
    )

    fun export(bundle: FinanceExportBundle): ByteArray {
        val books = bundle.books.associateBy { BookMode.valueOf(it.book.mode) }
        val categories = bundle.categories.associateBy { it.id }
        val productNames = bundle.products.associate { it.product.id to it.product.name }
        return writer.write(
            listOfNotNull(
                guide(
                    listOf(
                        listOf("Usaha", bundle.business.name),
                        listOf("Unit usaha", bundle.project.name),
                        listOf("Dibuat pada", Instant.ofEpochMilli(bundle.generatedAtEpochMillis).toString()),
                    ),
                ),
                transactions(books[BookMode.REALIZATION]?.events.orEmpty(), productNames),
                budget(books[BookMode.PROJECTION]?.budgets.orEmpty(), categories),
                products(bundle.products),
                recipes(bundle.products),
                stock(bundle.stockItems),
                categoryTable(bundle.categories.filter { it.active }.map { TemplateCategory(it.name, CategoryRole.parse(it.role), PlanAssembly.nature(it.costNature)) }),
                reportTable(bundle.books),
                bundle.plan?.let(::feasibilityTable),
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
            listOf("Transaksi", "Satu baris satu transaksi buku Realisasi. Isi Pemasukan ATAU Pengeluaran saja. Tanggal YYYY-MM-DD. Produk & Jumlah opsional untuk penjualan. Sifat Biaya (Tetap/Variabel) opsional, mengganti sifat kategori. Umur Pakai hanya untuk beli aset."),
            listOf("Kategori", "Tulis nama kategori. Kategori yang belum ada dibuat otomatis saat impor. Lihat daftar di lembar Kategori."),
            listOf("Modal & prive", "Setoran modal: Pemasukan berkategori 'Setoran Modal Pemilik'. Ambil uang pribadi: Pengeluaran berkategori 'Ambil untuk Pribadi (Prive)'. Beli alat: Pengeluaran berkategori 'Beli Peralatan & Aset'."),
            listOf("Rencana Anggaran", "Biaya tetap, biaya sekali bayar, aset, dan rencana setoran modal buku Proyeksi. Berulang: Bulanan (tiap bulan periode) atau Sekali. Bulan YYYY-MM."),
            listOf("Produk & Resep", "Produk: harga jual dan target terjual per bulan. Resep: biaya variabel per produk; 'Untuk Berapa Unit' untuk bahan yang dibeli per kemasan (mis. Rp15.000 untuk 50 cup)."),
            listOf("Stok Barang", "Barang yang namanya sudah ada di aplikasi dilewati."),
            listOf("Aturan", "Jangan ubah nama lembar atau judul kolom. Lembar Laporan dan Kelayakan hanya hasil hitungan dan diabaikan saat impor."),
        ),
    )

    /** Posted, unreversed cash rows only: what an owner would write in a cash book. */
    private fun transactions(events: List<FinancialEventEntity>, productNames: Map<String, String>) = WorkbookTable(
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
                    event.productId?.let(productNames::get),
                    event.quantityText.takeIf { event.productId != null },
                    MankeuTemplate.natureLabel(PlanAssembly.nature(event.costNature)),
                    event.usefulLifeMonths,
                )
            },
        currencyColumns = setOf(3, 4),
    )

    private fun budget(items: List<BudgetItemEntity>, categories: Map<String, CategoryEntity>) = WorkbookTable(
        name = MankeuTemplate.BUDGET,
        headers = MankeuTemplate.BUDGET_HEADERS,
        rows = items.filterNot { it.archived }.sortedBy { it.sortOrder }.map { item ->
            listOf(
                item.plannedDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)).toString() },
                categories[item.categoryId]?.name ?: item.categoryId,
                item.name,
                item.quantityText,
                item.unit,
                item.unitPriceRupiah,
                item.plannedTotalRupiah,
                if (item.recurrence == Recurrence.MONTHLY.name) MankeuTemplate.MONTHLY else MankeuTemplate.ONCE,
                MankeuTemplate.natureLabel(PlanAssembly.nature(item.costNature)),
                item.usefulLifeMonths,
            )
        },
        currencyColumns = setOf(5, 6),
    )

    private fun products(products: List<ProductWithComponents>) = WorkbookTable(
        name = MankeuTemplate.PRODUCTS,
        headers = MankeuTemplate.PRODUCT_HEADERS,
        rows = products.map { (p, _) -> listOf(p.name, p.unit, p.priceRupiah, p.targetPerMonthText) },
        currencyColumns = setOf(2),
    )

    private fun recipes(products: List<ProductWithComponents>) = WorkbookTable(
        name = MankeuTemplate.RECIPES,
        headers = MankeuTemplate.RECIPE_HEADERS,
        rows = products.flatMap { (p, components) -> components.map { listOf(p.name, it.name, it.amountRupiah, it.yieldText) } },
        currencyColumns = setOf(2),
    )

    private fun stock(items: List<StockItemEntity>) = WorkbookTable(
        name = MankeuTemplate.STOCK,
        headers = MankeuTemplate.STOCK_HEADERS,
        rows = items.filterNot { it.archived }.map { listOf(it.name, it.unit, it.basePriceRupiah, it.currentStockText, it.minAlertStockText) },
        currencyColumns = setOf(2),
    )

    private fun categoryTable(categories: List<TemplateCategory>) = WorkbookTable(
        name = MankeuTemplate.CATEGORIES,
        headers = MankeuTemplate.CATEGORY_HEADERS,
        rows = categories.map { listOf(it.name, roleLabel(it.role), MankeuTemplate.natureLabel(it.nature)) },
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

    /** The plan's BEP laid out like the lecturer's benchmark: cost items, margins, BEP, ratios. */
    private fun feasibilityTable(plan: PlanAnalysis) = WorkbookTable(
        name = "Kelayakan",
        headers = listOf("Bagian", "Rincian", "Nilai (Rp / unit)"),
        rows = buildList {
            plan.fixedCost.recurring.forEach { add(listOf("Biaya tetap / bulan", it.first, it.second)) }
            plan.fixedCost.oneOffSpread.forEach { add(listOf("Biaya tetap / bulan", "${it.first} (sekali, dibagi rata)", it.second)) }
            plan.fixedCost.depreciation.forEach { add(listOf("Biaya tetap / bulan", "Penyusutan ${it.first}", it.second)) }
            add(listOf("Biaya tetap / bulan", "Total biaya tetap (FC)", plan.fixedCost.totalRupiah))
            plan.products.forEach { p ->
                add(listOf(p.product.name, "Harga jual (P)", p.product.priceRupiah))
                p.product.components.forEach { add(listOf(p.product.name, "Biaya variabel: ${it.name}", PlanAnalysisFormat.money(it.perUnit))) }
                add(listOf(p.product.name, "Biaya variabel per unit (VC)", p.variableCostPerUnit))
                add(listOf(p.product.name, "Margin kontribusi (CM)", p.contributionMargin))
                add(listOf(p.product.name, "Porsi penjualan", p.mixShare?.let { PlanAnalysisFormat.percent(it) }))
                add(listOf(p.product.name, "BEP (${p.product.unit})", p.breakEvenUnits?.stripTrailingZeros()?.toPlainString()))
                add(listOf(p.product.name, "BEP (Rp)", p.breakEvenRupiah))
            }
            add(listOf("Total", "BEP semua produk (unit)", plan.breakEvenUnits?.stripTrailingZeros()?.toPlainString()))
            add(listOf("Total", "BEP omzet (Rp)", plan.breakEvenRupiah))
            add(listOf("Total", "Omzet target / bulan", plan.revenueRupiah))
            add(listOf("Total", "Total biaya / bulan", plan.totalCostRupiah))
            add(listOf("Total", "Laba / bulan", plan.profitRupiah))
            add(listOf("Total", "R/C Ratio", plan.revenueCostRatio?.toPlainString()))
            add(listOf("Total", "B/C Ratio", plan.benefitCostRatio?.toPlainString()))
            add(listOf("Total", "Status kelayakan", plan.feasible?.let { if (it) "LAYAK" else "BELUM LAYAK" }))
            add(listOf("Total", "Kebutuhan modal (defisit kas terdalam)", plan.capitalNeedRupiah))
        },
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

/** Formatting shared by the workbook and PDF breakdowns. */
internal object PlanAnalysisFormat {
    fun money(value: java.math.BigDecimal): Long = value.setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    fun percent(share: java.math.BigDecimal): String =
        share.multiply(java.math.BigDecimal(100)).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"
}
