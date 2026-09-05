package com.mankeu.importer

import com.mankeu.domain.BookMode
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
)

/**
 * A planned RAB line recovered from a workbook. Separate from [ImportCandidate] because a budget
 * item is not an event: it posts no journal, settles nothing, and is keyed by category rather than
 * by transaction type.
 */
data class ImportBudgetCandidate(
    val bookMode: BookMode,
    val categoryName: String,
    val name: String,
    val plannedTotalRupiah: Long,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedDate: LocalDate? = null,
    val sourceEvidence: List<String> = emptyList(),
)

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
) {
    val canCommit: Boolean get() = errors.isEmpty() && (candidates.isNotEmpty() || budgetCandidates.isNotEmpty())
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
