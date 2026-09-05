package com.arinahub.umkm.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class AnalysisAssumptions(
    val openingCashRupiah: Long = 0,
    val ownerCapitalRupiah: Long = 0,
    val loanAmountRupiah: Long = 0,
    val interestRateBasisPoints: Int = 0,
    val otherFinancingCostRupiah: Long = 0,
    val outputQuantityText: String? = null,
    val outputUnit: String? = null,
    val outputPriceRupiah: Long? = null,
)

data class BusinessAnalysis(
    val estimatedInterestRupiah: Long,
    val cashBeforeAssumedFinancingRupiah: Long,
    val financingGapRupiah: Long,
    val endingCashAfterAssumptionsRupiah: Long,
    val estimatedRevenueRupiah: Long?,
    val totalCostRupiah: Long,
    val costPerOutputRupiah: Long?,
    val breakEvenQuantity: BigDecimal?,
    val benefitCostRatio: BigDecimal?,
    val feasible: Boolean?,
    val outputUnit: String?,
    val grossProfitRupiah: Long? = null,
    val netProfitRupiah: Long? = null,
    val grossProfitMarginPercent: BigDecimal? = null,
    val netProfitMarginPercent: BigDecimal? = null,
    val breakEvenRevenueRupiah: Long? = null,
)

object BusinessAnalysisCalculator {
    fun calculate(
        summary: FinancialSummary,
        assumptions: AnalysisAssumptions,
        monthlyFlows: List<MonthlyCashFlow> = emptyList(),
    ): BusinessAnalysis {
        require(assumptions.interestRateBasisPoints >= 0) { "Bunga tidak boleh negatif." }
        val interest = BigDecimal.valueOf(assumptions.loanAmountRupiah)
            .multiply(BigDecimal.valueOf(assumptions.interestRateBasisPoints.toLong()))
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
            .longValueExact()
        val cashBeforeFinancing = assumptions.openingCashRupiah + summary.operatingCashFlowRupiah + summary.investingCashFlowRupiah
        val deepestCumulativeDeficit = if (monthlyFlows.isNotEmpty()) {
            monthlyFlows.minOfOrNull { it.cumulativeRupiah }?.let { if (it < 0) -it else 0L } ?: 0L
        } else 0L
        val financingGap = maxOf((-cashBeforeFinancing).coerceAtLeast(0), deepestCumulativeDeficit)
        val endingCash = cashBeforeFinancing + assumptions.ownerCapitalRupiah + assumptions.loanAmountRupiah - interest - assumptions.otherFinancingCostRupiah
        val totalCost = summary.operatingExpenseRupiah + interest + assumptions.otherFinancingCostRupiah
        val quantity = assumptions.outputQuantityText?.trim()?.replace(',', '.')?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        val price = assumptions.outputPriceRupiah?.takeIf { it > 0 }
        val revenue = if (quantity != null && price != null) {
            quantity.multiply(BigDecimal.valueOf(price)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } else null
        val unitCost = quantity?.let {
            BigDecimal.valueOf(totalCost).divide(it, 0, RoundingMode.HALF_UP).longValueExact()
        }
        val bep = price?.let {
            BigDecimal.valueOf(totalCost).divide(BigDecimal.valueOf(it), 3, RoundingMode.HALF_UP)
        }
        val ratio = if (revenue != null && totalCost > 0) {
            BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(totalCost), 3, RoundingMode.HALF_UP)
        } else null
        val grossProfit = revenue?.let { it - summary.operatingExpenseRupiah }
        val netProfit = revenue?.let { it - totalCost }
        val grossMargin = if (revenue != null && revenue > 0) {
            BigDecimal.valueOf(grossProfit!!)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(revenue), 2, RoundingMode.HALF_UP)
        } else null
        val netMargin = if (revenue != null && revenue > 0) {
            BigDecimal.valueOf(netProfit!!)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(revenue), 2, RoundingMode.HALF_UP)
        } else null
        val bepRevenue = if (price != null && price > 0) totalCost else null

        return BusinessAnalysis(
            estimatedInterestRupiah = interest,
            cashBeforeAssumedFinancingRupiah = cashBeforeFinancing,
            financingGapRupiah = financingGap,
            endingCashAfterAssumptionsRupiah = endingCash,
            estimatedRevenueRupiah = revenue,
            totalCostRupiah = totalCost,
            costPerOutputRupiah = unitCost,
            breakEvenQuantity = bep,
            benefitCostRatio = ratio,
            feasible = ratio?.let { it >= BigDecimal.ONE },
            outputUnit = assumptions.outputUnit,
            grossProfitRupiah = grossProfit,
            netProfitRupiah = netProfit,
            grossProfitMarginPercent = grossMargin,
            netProfitMarginPercent = netMargin,
            breakEvenRevenueRupiah = bepRevenue,
        )
    }
}

data class MonthlyCashFlow(
    val month: YearMonth,
    val operatingRupiah: Long,
    val investingRupiah: Long,
    val financingRupiah: Long,
    val netRupiah: Long,
    val cumulativeRupiah: Long,
)

object MonthlyCashFlowCalculator {
    fun calculate(events: List<MoneyEvent>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> {
        if (events.isEmpty()) return emptyList()
        val byId = events.associateBy { it.id }
        val movements = events.mapNotNull { event ->
            val amount = when (event.type) {
                FinancialEventType.CASH_INCOME,
                FinancialEventType.RECEIVABLE_COLLECTION,
                FinancialEventType.OWNER_CAPITAL,
                FinancialEventType.LOAN_RECEIVED,
                -> event.amountRupiah
                FinancialEventType.CASH_EXPENSE,
                FinancialEventType.PAYABLE_SETTLEMENT,
                FinancialEventType.OWNER_DRAWING,
                FinancialEventType.LOAN_PRINCIPAL_REPAID,
                FinancialEventType.LOAN_INTEREST_PAID,
                FinancialEventType.ASSET_PURCHASE_CASH,
                -> -event.amountRupiah
                FinancialEventType.REVERSAL -> event.reversesEventId?.let(byId::get)?.let { -cashMovement(it) } ?: 0
                else -> 0
            }
            amount.takeIf { it != 0L }?.let { Triple(YearMonth.from(event.date), event.cashFlowRole, it) }
        }
        if (movements.isEmpty()) return emptyList()
        val first = movements.minOf { it.first }
        val last = movements.maxOf { it.first }
        val grouped = movements.groupBy { it.first }
        var cumulative = openingCashRupiah
        return generateSequence(first) { month -> month.plusMonths(1).takeIf { it <= last } }.map { month ->
            val values = grouped[month].orEmpty()
            fun role(role: CashFlowRole) = values.filter { it.second == role }.sumOf { it.third }
            val operating = role(CashFlowRole.OPERATING)
            val investing = role(CashFlowRole.INVESTING)
            val financing = role(CashFlowRole.FINANCING)
            val net = operating + investing + financing
            cumulative += net
            MonthlyCashFlow(month, operating, investing, financing, net, cumulative)
        }.toList()
    }

    private fun cashMovement(event: MoneyEvent): Long = when (event.type) {
        FinancialEventType.CASH_INCOME,
        FinancialEventType.RECEIVABLE_COLLECTION,
        FinancialEventType.OWNER_CAPITAL,
        FinancialEventType.LOAN_RECEIVED,
        -> event.amountRupiah
        FinancialEventType.CASH_EXPENSE,
        FinancialEventType.PAYABLE_SETTLEMENT,
        FinancialEventType.OWNER_DRAWING,
        FinancialEventType.LOAN_PRINCIPAL_REPAID,
        FinancialEventType.LOAN_INTEREST_PAID,
        FinancialEventType.ASSET_PURCHASE_CASH,
        -> -event.amountRupiah
        else -> 0
    }
}
