package com.arinahub.arinaagri.calculator.domain

import java.time.LocalDate

enum class BookMode { PROJECTION, REALIZATION }

enum class FinancialEventType {
    CASH_INCOME,
    CASH_EXPENSE,
    CREDIT_SALE,
    CREDIT_PURCHASE_EXPENSE,
    CREDIT_PURCHASE_ASSET,
    RECEIVABLE_COLLECTION,
    PAYABLE_SETTLEMENT,
    OWNER_CAPITAL,
    OWNER_DRAWING,
    LOAN_RECEIVED,
    LOAN_PRINCIPAL_REPAID,
    LOAN_INTEREST_PAID,
    ASSET_PURCHASE_CASH,
    REVERSAL,
}

enum class PostingSide { DEBIT, CREDIT }

enum class LedgerAccount {
    CASH,
    ACCOUNTS_RECEIVABLE,
    ACCOUNTS_PAYABLE,
    REVENUE,
    OPERATING_EXPENSE,
    INTEREST_EXPENSE,
    OWNER_EQUITY,
    OWNER_DRAWING,
    LOAN_PAYABLE,
    EQUIPMENT_ASSET,
}

enum class CashFlowRole { OPERATING, INVESTING, FINANCING, NON_CASH }

enum class CategoryRole { REVENUE, EXPENSE, ASSET, EQUITY, LIABILITY }

enum class ObligationKind { RECEIVABLE, PAYABLE }

enum class ObligationStatus { OPEN, PARTIAL, PAID, OVERDUE, REVERSED }

enum class RecordStatus { POSTED, REVERSED }

data class MoneyEvent(
    val id: String,
    val businessId: String,
    val projectId: String,
    val type: FinancialEventType,
    val amountRupiah: Long,
    val date: LocalDate,
    val description: String,
    val category: String,
    val bookId: String = "",
    val categoryId: String? = null,
    val counterparty: String? = null,
    val reference: String? = null,
    val dueDate: LocalDate? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val obligationId: String? = null,
    val importBatchId: String? = null,
    val cashFlowRole: CashFlowRole = defaultCashFlowRole(type),
    val status: RecordStatus = RecordStatus.POSTED,
    val reversesEventId: String? = null,
    val reversedByEventId: String? = null,
) {
    init {
        require(amountRupiah > 0) { "Amount must be a positive integer rupiah value." }
        require(description.isNotBlank()) { "Description is required." }
        require(businessId.isNotBlank() && projectId.isNotBlank()) { "Business and project scope are required." }
        if (unitPriceRupiah != null) require(unitPriceRupiah >= 0) { "Unit price cannot be negative." }
    }

    companion object {
        fun defaultCashFlowRole(type: FinancialEventType): CashFlowRole = when (type) {
            FinancialEventType.OWNER_CAPITAL,
            FinancialEventType.OWNER_DRAWING,
            FinancialEventType.LOAN_RECEIVED,
            FinancialEventType.LOAN_PRINCIPAL_REPAID,
            -> CashFlowRole.FINANCING

            FinancialEventType.ASSET_PURCHASE_CASH -> CashFlowRole.INVESTING
            FinancialEventType.CREDIT_SALE,
            FinancialEventType.CREDIT_PURCHASE_EXPENSE,
            FinancialEventType.CREDIT_PURCHASE_ASSET,
            FinancialEventType.REVERSAL,
            -> CashFlowRole.NON_CASH

            else -> CashFlowRole.OPERATING
        }
    }
}

data class Posting(
    val account: LedgerAccount,
    val side: PostingSide,
    val amountRupiah: Long,
)

data class JournalEntry(
    val id: String,
    val sourceEventId: String,
    val date: LocalDate,
    val description: String,
    val postings: List<Posting>,
    val reversalOfJournalId: String? = null,
) {
    fun requireBalanced(): JournalEntry {
        val debit = postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah }
        val credit = postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah }
        require(postings.isNotEmpty() && debit == credit) { "Journal entry is not balanced: debit=$debit credit=$credit" }
        require(postings.all { it.amountRupiah > 0 }) { "Every posting must contain a positive integer rupiah value." }
        return this
    }
}

data class FinancialSummary(
    val operatingIncomeRupiah: Long,
    val operatingExpenseRupiah: Long,
    val profitRupiah: Long,
    val operatingCashFlowRupiah: Long,
    val financingCashFlowRupiah: Long,
    val cashBalanceRupiah: Long,
    val investingCashFlowRupiah: Long = 0,
    val receivableOutstandingRupiah: Long = 0,
    val payableOutstandingRupiah: Long = 0,
)

data class ObligationBalance(
    val id: String,
    val kind: ObligationKind,
    val originalAmountRupiah: Long,
    val settledAmountRupiah: Long,
    val dueDate: LocalDate?,
    val reversed: Boolean = false,
) {
    val outstandingAmountRupiah: Long = (originalAmountRupiah - settledAmountRupiah).coerceAtLeast(0)

    fun status(today: LocalDate): ObligationStatus = when {
        reversed -> ObligationStatus.REVERSED
        outstandingAmountRupiah == 0L -> ObligationStatus.PAID
        dueDate != null && dueDate.isBefore(today) -> ObligationStatus.OVERDUE
        settledAmountRupiah > 0 -> ObligationStatus.PARTIAL
        else -> ObligationStatus.OPEN
    }
}
