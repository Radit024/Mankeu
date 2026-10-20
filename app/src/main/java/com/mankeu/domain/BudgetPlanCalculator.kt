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
        val financing = sum(CategoryRole.CAPITAL_IN) - sum(CategoryRole.DRAWING)
        val operatingCash = income - expense
        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = expense,
            profitRupiah = income - expense,
            operatingCashFlowRupiah = operatingCash,
            financingCashFlowRupiah = financing,
            cashBalanceRupiah = operatingCash + investing + financing,
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
        val dated = lines.filter { it.plannedDate != null }
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
            val financing = sum(CategoryRole.CAPITAL_IN) - sum(CategoryRole.DRAWING)
            val net = operating + investing + financing
            cumulative += net
            MonthlyCashFlow(
                month = month,
                operatingRupiah = operating,
                investingRupiah = investing,
                financingRupiah = financing,
                netRupiah = net,
                cumulativeRupiah = cumulative,
            )
        }.toList()
    }
}
