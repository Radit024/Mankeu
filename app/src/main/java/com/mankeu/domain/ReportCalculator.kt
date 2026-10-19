package com.mankeu.domain

import java.time.YearMonth

/** One RAB line reduced to what a plan summary and a plan schedule need. */
data class PlanBudgetLine(
    val plannedTotalRupiah: Long,
    val role: CategoryRole,
    /** The period the line is planned for. Null on an undated RAB, which yields no schedule. */
    val plannedDate: java.time.LocalDate? = null,
)

/**
 * Turns a budget (RAB) into the same [FinancialSummary] shape a book of posted events produces.
 *
 * A projection book usually holds a plan and no transactions, and every summary in the app is
 * derived from postings — so the plan side of Beranda and Laporan read Rp0 across the board while
 * a fully costed RAB sat one tab away. Feasibility, break-even and B/C ratio all divide by a total
 * cost, so with no cost they showed nothing at all.
 *
 * This is a *declared* source class, not a merge: callers use it only when the projection book has
 * no events, so a book can never count a planned line and a posted event for the same money.
 * See `docs/DOMAIN_MODEL.md`, "A report must declare which source class it uses".
 */
object BudgetPlanCalculator {
    fun summarize(lines: List<PlanBudgetLine>): FinancialSummary {
        fun sum(role: CategoryRole) = lines.filter { it.role == role }.sumOf { it.plannedTotalRupiah }
        val income = sum(CategoryRole.REVENUE)
        val expense = sum(CategoryRole.EXPENSE)
        val investing = -sum(CategoryRole.ASSET)
        val operatingCash = income - expense
        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = expense,
            profitRupiah = income - expense,
            operatingCashFlowRupiah = operatingCash,
            financingCashFlowRupiah = 0,
            cashBalanceRupiah = operatingCash + investing,
            investingCashFlowRupiah = investing,
        )
    }

    /**
     * The plan's month-by-month cash schedule, in the same shape [MonthlyCashFlowCalculator]
     * produces for posted events, so [BusinessAnalysisCalculator] can consume either.
     *
     * This is what turns "Kebutuhan Modal Kerja" on the projection book from Rp0 into a real
     * figure. A plan that is profitable over a whole cycle can still be deeply short in month one
     * — buy the equipment and three months of stock in February, sell through April — and that
     * trough is exactly the working capital the owner has to raise. An undated RAB cannot say
     * when anything happens, so it returns no schedule rather than a made-up one, and the caller
     * falls back to the whole-cycle figure.
     *
     * Revenue lines are treated as operating inflows, expense lines as operating outflows, and
     * asset lines as investing outflows — the same role mapping [summarize] uses.
     */
    fun schedule(lines: List<PlanBudgetLine>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> {
        val dated = lines.filter { it.plannedDate != null && it.role != CategoryRole.EQUITY && it.role != CategoryRole.LIABILITY }
        if (dated.isEmpty()) return emptyList()
        val byMonth = dated.groupBy { YearMonth.from(it.plannedDate) }
        val first = byMonth.keys.min()
        val last = byMonth.keys.max()
        var cumulative = openingCashRupiah
        return generateSequence(first) { month -> month.plusMonths(1).takeIf { it <= last } }.map { month ->
            val rows = byMonth[month].orEmpty()
            fun sum(role: CategoryRole) = rows.filter { it.role == role }.sumOf { it.plannedTotalRupiah }
            val operating = sum(CategoryRole.REVENUE) - sum(CategoryRole.EXPENSE)
            val investing = -sum(CategoryRole.ASSET)
            val net = operating + investing
            cumulative += net
            MonthlyCashFlow(
                month = month,
                operatingRupiah = operating,
                investingRupiah = investing,
                financingRupiah = 0,
                netRupiah = net,
                cumulativeRupiah = cumulative,
            )
        }.toList()
    }
}

object ReportCalculator {
    fun calculate(
        events: List<MoneyEvent>,
        obligations: List<ObligationBalance> = emptyList(),
    ): FinancialSummary {
        val activeEvents = events.filter { it.status == RecordStatus.POSTED && it.reversedByEventId == null }
        val income = activeEvents.filter {
            it.type == FinancialEventType.CASH_INCOME || it.type == FinancialEventType.CREDIT_SALE
        }.sumOf { it.amountRupiah }
        val operatingExpense = activeEvents.filter {
            it.type == FinancialEventType.CASH_EXPENSE || it.type == FinancialEventType.CREDIT_PURCHASE_EXPENSE
        }.sumOf { it.amountRupiah }
        val interestExpense = activeEvents.filter { it.type == FinancialEventType.LOAN_INTEREST_PAID }.sumOf { it.amountRupiah }

        val cashIn = activeEvents.filter {
            it.type in setOf(
                FinancialEventType.CASH_INCOME,
                FinancialEventType.RECEIVABLE_COLLECTION,
                FinancialEventType.OWNER_CAPITAL,
                FinancialEventType.LOAN_RECEIVED,
            )
        }.sumOf { it.amountRupiah }
        val cashOut = activeEvents.filter {
            it.type in setOf(
                FinancialEventType.CASH_EXPENSE,
                FinancialEventType.PAYABLE_SETTLEMENT,
                FinancialEventType.OWNER_DRAWING,
                FinancialEventType.LOAN_PRINCIPAL_REPAID,
                FinancialEventType.LOAN_INTEREST_PAID,
                FinancialEventType.ASSET_PURCHASE_CASH,
            )
        }.sumOf { it.amountRupiah }

        val operatingCashIn = activeEvents.filter {
            it.type == FinancialEventType.CASH_INCOME || it.type == FinancialEventType.RECEIVABLE_COLLECTION
        }.sumOf { it.amountRupiah }
        val operatingCashOut = activeEvents.filter {
            it.type == FinancialEventType.CASH_EXPENSE ||
                it.type == FinancialEventType.PAYABLE_SETTLEMENT ||
                it.type == FinancialEventType.LOAN_INTEREST_PAID
        }.sumOf { it.amountRupiah }

        val financingCash = activeEvents.sumOf { event ->
            when (event.type) {
                FinancialEventType.OWNER_CAPITAL,
                FinancialEventType.LOAN_RECEIVED,
                -> event.amountRupiah

                FinancialEventType.OWNER_DRAWING,
                FinancialEventType.LOAN_PRINCIPAL_REPAID,
                -> -event.amountRupiah

                else -> 0L
            }
        }
        val investingCash = -activeEvents.filter {
            it.type == FinancialEventType.ASSET_PURCHASE_CASH || it.type == FinancialEventType.CREDIT_PURCHASE_ASSET
        }.sumOf { it.amountRupiah }
        val expense = operatingExpense + interestExpense

        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = expense,
            profitRupiah = income - expense,
            operatingCashFlowRupiah = operatingCashIn - operatingCashOut,
            financingCashFlowRupiah = financingCash,
            cashBalanceRupiah = cashIn - cashOut,
            investingCashFlowRupiah = investingCash,
            receivableOutstandingRupiah = obligations.filter { it.kind == ObligationKind.RECEIVABLE && !it.reversed }
                .sumOf { it.outstandingAmountRupiah },
            payableOutstandingRupiah = obligations.filter { it.kind == ObligationKind.PAYABLE && !it.reversed }
                .sumOf { it.outstandingAmountRupiah },
        )
    }
}
