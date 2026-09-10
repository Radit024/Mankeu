package com.mankeu.importer

import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import java.time.LocalDate

enum class ImportClassification { DIRECT, NORMALIZED, ENRICHED_DETERMINISTIC, ASSUMPTION_AMBIGUOUS, CONFLICT }

data class ImportCandidate(
    val type: FinancialEventType,
    val amountRupiah: Long,
    val date: LocalDate,
    val description: String,
    val category: String,
    val classification: ImportClassification,
    val sourceEvidence: List<String>,
    val rationale: String,
    val inferredFields: List<String> = emptyList(),
    val counterparty: String? = null,
    val dueDate: LocalDate? = null,
    val reference: String? = null,
    val sourceRecordId: String? = null,
    val sourceObligationId: String? = null,
    /**
     * Which book this row belongs in. Defaults to REALIZATION because every non-canonical source
     * (the WB-BASE-001 five-view workbook, a generic single-table sheet) is a record of what
     * actually happened. Only Mankeu's own export can say otherwise, and it does.
     */
    val bookMode: BookMode = BookMode.REALIZATION,
    /**
     * Volume, unit and unit price, when the workbook states them.
     *
     * A ledger row that says "400 kg @ Rp45.000" carries information the total alone does not,
     * and the app has fields for all three. Dropping them on import would have forced the owner
     * to retype what the file already contained.
     */
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
)

/**
 * Everyday Indonesian names for the event kinds, as a `Jenis` column in a daily ledger.
 *
 * Without this the daily-ledger reader had exactly two outcomes per row — money in or money
 * out — so a capital injection, a loan disbursement, a sale on credit and a debt repayment all
 * landed as ordinary revenue or ordinary expense. The totals looked plausible and the profit,
 * the operating cash flow and the financing cash flow were all wrong, which is the worst shape
 * a bookkeeping error can take. A workbook that states the kind is now believed.
 *
 * The canonical `FinancialEventType` names are accepted too, so a file that was produced by
 * Mankeu's own exporter and then flattened by hand still reads back.
 */
object TransactionKindLabels {
    private val byLabel: Map<String, FinancialEventType> = mapOf(
        "uang masuk" to FinancialEventType.CASH_INCOME,
        "pemasukan" to FinancialEventType.CASH_INCOME,
        "penjualan tunai" to FinancialEventType.CASH_INCOME,
        "uang keluar" to FinancialEventType.CASH_EXPENSE,
        "pengeluaran" to FinancialEventType.CASH_EXPENSE,
        "belanja tunai" to FinancialEventType.CASH_EXPENSE,
        "penjualan tempo" to FinancialEventType.CREDIT_SALE,
        "piutang" to FinancialEventType.CREDIT_SALE,
        "jual tempo" to FinancialEventType.CREDIT_SALE,
        "belanja tempo" to FinancialEventType.CREDIT_PURCHASE_EXPENSE,
        "utang" to FinancialEventType.CREDIT_PURCHASE_EXPENSE,
        "beli tempo" to FinancialEventType.CREDIT_PURCHASE_EXPENSE,
        "beli alat tempo" to FinancialEventType.CREDIT_PURCHASE_ASSET,
        "terima piutang" to FinancialEventType.RECEIVABLE_COLLECTION,
        "pelunasan piutang" to FinancialEventType.RECEIVABLE_COLLECTION,
        "bayar utang" to FinancialEventType.PAYABLE_SETTLEMENT,
        "pelunasan utang" to FinancialEventType.PAYABLE_SETTLEMENT,
        "modal masuk" to FinancialEventType.OWNER_CAPITAL,
        "setoran modal" to FinancialEventType.OWNER_CAPITAL,
        "modal keluar" to FinancialEventType.OWNER_DRAWING,
        "prive" to FinancialEventType.OWNER_DRAWING,
        "pinjaman masuk" to FinancialEventType.LOAN_RECEIVED,
        "terima pinjaman" to FinancialEventType.LOAN_RECEIVED,
        "bayar pokok pinjaman" to FinancialEventType.LOAN_PRINCIPAL_REPAID,
        "angsuran pokok" to FinancialEventType.LOAN_PRINCIPAL_REPAID,
        "bayar bunga" to FinancialEventType.LOAN_INTEREST_PAID,
        "bunga pinjaman" to FinancialEventType.LOAN_INTEREST_PAID,
        "beli alat tunai" to FinancialEventType.ASSET_PURCHASE_CASH,
        "beli alat" to FinancialEventType.ASSET_PURCHASE_CASH,
    )

    /** Types whose money arrives; used to pick a revenue or expense category for the row. */
    val incoming = setOf(
        FinancialEventType.CASH_INCOME,
        FinancialEventType.CREDIT_SALE,
        FinancialEventType.RECEIVABLE_COLLECTION,
        FinancialEventType.OWNER_CAPITAL,
        FinancialEventType.LOAN_RECEIVED,
    )

    fun parse(raw: String?): FinancialEventType? {
        val text = raw?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")?.ifBlank { null } ?: return null
        byLabel[text]?.let { return it }
        return runCatching { FinancialEventType.valueOf(raw.trim().uppercase()) }.getOrNull()
            ?.takeIf { it != FinancialEventType.REVERSAL }
    }
}

/**
 * A planned RAB line recovered from a workbook. Separate from [ImportCandidate] because a budget
 * item is not an event: it posts no journal, settles nothing, and is keyed by category rather than
 * by transaction type.
 */
data class ImportBudgetCandidate(
    val bookMode: BookMode,
    val categoryName: String,
    /**
     * The role the workbook's own section heading implies.
     *
     * `commitImport` used to create every category an import invented as EXPENSE. On a workbook
     * whose category names happen to match the business's template that never showed, but on any
     * other one a revenue target and a Rp13 juta equipment budget both became operating cost —
     * so the plan's profit, its investing cash flow, and therefore its break-even and B/C ratio
     * were all wrong, and wrong quietly.
     */
    val categoryRole: CategoryRole = CategoryRole.EXPENSE,
    val name: String,
    val plannedTotalRupiah: Long,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedDate: LocalDate? = null,
    val sourceEvidence: List<String> = emptyList(),
)

/**
 * Planning assumptions read out of a workbook's parameter sheet.
 *
 * These are the eight figures the app cannot derive from any transaction — capacity, selling price,
 * own capital, loan, interest, financing cost, opening cash — and every workbook written for a
 * feasibility study states them on its first sheet. Until this existed the importer read the
 * ledger, the budget and the reports out of that same file and then left the owner to retype the
 * eight numbers by hand into the Asumsi sheet, against the rule in AGENTS.md: *never force a user
 * to manually recreate information already recoverable from the supplied workbook*.
 *
 * Every field is nullable and only the ones actually found are written, so a partial parameter
 * sheet contributes what it has instead of zeroing what it does not mention.
 */
data class ImportedAssumptions(
    val openingCashRupiah: Long? = null,
    val ownerCapitalRupiah: Long? = null,
    val loanAmountRupiah: Long? = null,
    val interestRateBasisPoints: Int? = null,
    val otherFinancingCostRupiah: Long? = null,
    val outputQuantityText: String? = null,
    val outputUnit: String? = null,
    val outputPriceRupiah: Long? = null,
    /** `sheet!cell` for each value, in the order the fields above are listed. */
    val sourceEvidence: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = openingCashRupiah == null && ownerCapitalRupiah == null &&
        loanAmountRupiah == null && interestRateBasisPoints == null && otherFinancingCostRupiah == null &&
        outputQuantityText == null && outputUnit == null && outputPriceRupiah == null

    val fieldCount: Int get() = listOfNotNull(
        openingCashRupiah, ownerCapitalRupiah, loanAmountRupiah, interestRateBasisPoints,
        otherFinancingCostRupiah, outputQuantityText, outputUnit, outputPriceRupiah,
    ).size
}

data class WorkbookImportPreview(
    val sourceName: String,
    val sourceSha256: String,
    val recognizedSheets: List<String>,
    val candidates: List<ImportCandidate>,
    val warnings: List<String>,
    val errors: List<String>,
    val budgetCandidates: List<ImportBudgetCandidate> = emptyList(),
    /**
     * True when the workbook states which book each row belongs to.
     *
     * Only a Mankeu export does: it has a `Transaksi Proyeksi` sheet and a `Transaksi Realisasi`
     * sheet, so the rows already know where they go and overriding them would collapse two books
     * into one (D-020). Every other workbook — the reference five-view case study, a single-table
     * ledger — says nothing about plan versus actual, so the user chooses.
     */
    val carriesBookMode: Boolean = false,
    /**
     * Planning assumptions the workbook states outright. They belong to the projection book: they
     * describe the plan, not what happened, and the realization book's own output volume is
     * derived from its ledger rather than assumed.
     */
    val assumptions: ImportedAssumptions? = null,
) {
    val canCommit: Boolean get() = errors.isEmpty() &&
        (candidates.isNotEmpty() || budgetCandidates.isNotEmpty() || assumptions != null)
    val directCount: Int get() = candidates.count { it.classification == ImportClassification.DIRECT }
    val enrichmentCount: Int get() = candidates.count { it.classification == ImportClassification.ENRICHED_DETERMINISTIC }
    val totalIncomeRupiah: Long get() = candidates.filter {
        it.type in setOf(FinancialEventType.CASH_INCOME, FinancialEventType.CREDIT_SALE)
    }.sumOf { it.amountRupiah }
    val totalExpenseRupiah: Long get() = candidates.filter {
        it.type in setOf(
            FinancialEventType.CASH_EXPENSE,
            FinancialEventType.CREDIT_PURCHASE_EXPENSE,
            FinancialEventType.LOAN_INTEREST_PAID,
        )
    }.sumOf { it.amountRupiah }
}
