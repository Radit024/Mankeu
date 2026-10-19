package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

data class AnalysisAssumptions(
    val openingCashRupiah: Long = 0,
    val ownerCapitalRupiah: Long = 0,
    val otherFinancingCostRupiah: Long = 0,
    val outputQuantityText: String? = null,
    val outputUnit: String? = null,
    val outputPriceRupiah: Long? = null,
    val fixedCostRupiah: Long? = null,
    val variableCostPerUnitRupiah: Long? = null,
)

data class BusinessAnalysis(
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
    val revenueCostRatio: BigDecimal? = null,
    val fixedCostRupiah: Long? = null,
    val variableCostPerUnitRupiah: Long? = null,
    val contributionMarginRupiah: Long? = null,
    val contributionMarginRatio: BigDecimal? = null,
    val estimatedInterestRupiah: Long = 0L,
)

data class CvpAnalysisResult(
    val priceRupiah: Long,
    val variableCostPerUnitRupiah: Long,
    val fixedCostRupiah: Long,
    val contributionMarginRupiah: Long,
    val contributionMarginRatio: BigDecimal,
    val breakEvenQuantity: BigDecimal,
    val breakEvenRevenueRupiah: Long,
    val actualQuantity: BigDecimal? = null,
    val totalRevenueRupiah: Long? = null,
    val totalCostRupiah: Long? = null,
    val netProfitRupiah: Long? = null,
    val revenueCostRatio: BigDecimal? = null,
    val benefitCostRatio: BigDecimal? = null,
    val isEfficient: Boolean? = null,
    val isFeasible: Boolean? = null,
)

object BusinessAnalysisCalculator {
    /**
     * Pure CVP (Cost-Volume-Profit) calculation conforming to standard micro-business accounting:
     * - CM = Price - VC
     * - CMR = CM / Price
     * - BEP Unit = FC / CM
     * - BEP Rupiah = FC / CMR = BEP Unit * Price
     * - R/C Ratio = Total Revenue / Total Cost
     * - B/C Ratio = Net Profit / Total Cost
     */
    fun calculateCvp(
        priceRupiah: Long,
        variableCostPerUnitRupiah: Long,
        fixedCostRupiah: Long,
        actualQuantityText: String? = null,
    ): CvpAnalysisResult {
        require(priceRupiah > 0) { "Harga jual harus lebih dari nol." }
        require(variableCostPerUnitRupiah >= 0) { "Biaya variabel tidak boleh negatif." }
        require(fixedCostRupiah >= 0) { "Biaya tetap tidak boleh negatif." }
        require(priceRupiah > variableCostPerUnitRupiah) {
            "Harga jual (Rp$priceRupiah) harus lebih besar dari biaya variabel satuan (Rp$variableCostPerUnitRupiah) untuk mencapai titik impas."
        }

        val cm = priceRupiah - variableCostPerUnitRupiah
        val cmr = BigDecimal.valueOf(cm).divide(BigDecimal.valueOf(priceRupiah), 4, RoundingMode.HALF_UP)
        val bepUnit = BigDecimal.valueOf(fixedCostRupiah).divide(BigDecimal.valueOf(cm), 3, RoundingMode.HALF_UP)
        val bepRupiah = bepUnit.multiply(BigDecimal.valueOf(priceRupiah)).setScale(0, RoundingMode.HALF_UP).longValueExact()

        val qty = TransactionCalculation.parseQuantity(actualQuantityText)
        val revenue = qty?.let {
            it.multiply(BigDecimal.valueOf(priceRupiah)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }
        val totalVc = qty?.let {
            it.multiply(BigDecimal.valueOf(variableCostPerUnitRupiah)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } ?: 0L
        val totalCost = if (qty != null) fixedCostRupiah + totalVc else null
        val netProfit = if (revenue != null && totalCost != null) revenue - totalCost else null

        val rcRatio = if (revenue != null && totalCost != null && totalCost > 0) {
            BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(totalCost), 3, RoundingMode.HALF_UP)
        } else null

        val bcRatio = if (netProfit != null && totalCost != null && totalCost > 0) {
            BigDecimal.valueOf(netProfit).divide(BigDecimal.valueOf(totalCost), 3, RoundingMode.HALF_UP)
        } else null

        return CvpAnalysisResult(
            priceRupiah = priceRupiah,
            variableCostPerUnitRupiah = variableCostPerUnitRupiah,
            fixedCostRupiah = fixedCostRupiah,
            contributionMarginRupiah = cm,
            contributionMarginRatio = cmr,
            breakEvenQuantity = bepUnit,
            breakEvenRevenueRupiah = bepRupiah,
            actualQuantity = qty,
            totalRevenueRupiah = revenue,
            totalCostRupiah = totalCost,
            netProfitRupiah = netProfit,
            revenueCostRatio = rcRatio,
            benefitCostRatio = bcRatio,
            isEfficient = rcRatio?.let { it > BigDecimal.ONE },
            isFeasible = bcRatio?.let { it > BigDecimal.ZERO },
        )
    }

    fun calculate(
        summary: FinancialSummary,
        assumptions: AnalysisAssumptions,
        monthlyFlows: List<MonthlyCashFlow> = emptyList(),
    ): BusinessAnalysis {
        val cashBeforeFinancing = assumptions.openingCashRupiah + summary.operatingCashFlowRupiah + summary.investingCashFlowRupiah
        val deepestCumulativeDeficit = if (monthlyFlows.isNotEmpty()) {
            monthlyFlows.minOfOrNull { it.cumulativeRupiah }?.let { if (it < 0) -it else 0L } ?: 0L
        } else 0L
        val financingGap = maxOf((-cashBeforeFinancing).coerceAtLeast(0), deepestCumulativeDeficit)
        val endingCash = cashBeforeFinancing + assumptions.ownerCapitalRupiah - assumptions.otherFinancingCostRupiah

        val quantity = TransactionCalculation.parseQuantity(assumptions.outputQuantityText)
        val price = assumptions.outputPriceRupiah?.takeIf { it > 0 }
        val vc = assumptions.variableCostPerUnitRupiah?.takeIf { it >= 0 }
        val fc = assumptions.fixedCostRupiah?.takeIf { it >= 0 }
            ?: (summary.operatingExpenseRupiah + assumptions.otherFinancingCostRupiah)

        val totalCost = if (vc != null) {
            val totalVc = if (quantity != null) {
                quantity.multiply(BigDecimal.valueOf(vc)).setScale(0, RoundingMode.HALF_UP).longValueExact()
            } else 0L
            fc + totalVc
        } else {
            summary.operatingExpenseRupiah + assumptions.otherFinancingCostRupiah
        }

        val revenue = if (quantity != null && price != null) {
            quantity.multiply(BigDecimal.valueOf(price)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } else if (summary.operatingIncomeRupiah > 0) {
            summary.operatingIncomeRupiah
        } else null

        val unitCost = quantity?.let {
            BigDecimal.valueOf(totalCost).divide(it, 0, RoundingMode.HALF_UP).longValueExact()
        }

        // CVP parameters:
        val unitCm = if (price != null && vc != null) price - vc else null
        val cmRatio = if (price != null && unitCm != null && price > 0) {
            BigDecimal.valueOf(unitCm).divide(BigDecimal.valueOf(price), 4, RoundingMode.HALF_UP)
        } else null

        val bep = if (vc != null) {
            if (unitCm != null && unitCm > 0) {
                BigDecimal.valueOf(fc).divide(BigDecimal.valueOf(unitCm), 3, RoundingMode.HALF_UP)
            } else null
        } else {
            price?.let {
                BigDecimal.valueOf(totalCost).divide(BigDecimal.valueOf(it), 3, RoundingMode.HALF_UP)
            }
        }

        val bepRevenue = if (vc != null) {
            if (bep != null && price != null) {
                bep.multiply(BigDecimal.valueOf(price)).setScale(0, RoundingMode.HALF_UP).longValueExact()
            } else null
        } else {
            if (price != null && price > 0) totalCost else null
        }

        val grossProfit = revenue?.let { it - (if (vc != null) (totalCost - fc) else summary.operatingExpenseRupiah) }
        val netProfit = revenue?.let { it - totalCost }

        val rcRatio = if (revenue != null && totalCost > 0) {
            BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(totalCost), 3, RoundingMode.HALF_UP)
        } else null

        val bcRatio = if (netProfit != null && totalCost > 0) {
            BigDecimal.valueOf(netProfit).divide(BigDecimal.valueOf(totalCost), 3, RoundingMode.HALF_UP)
        } else null

        val grossMargin = if (revenue != null && revenue > 0 && grossProfit != null) {
            BigDecimal.valueOf(grossProfit)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(revenue), 2, RoundingMode.HALF_UP)
        } else null

        val netMargin = if (revenue != null && revenue > 0 && netProfit != null) {
            BigDecimal.valueOf(netProfit)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(revenue), 2, RoundingMode.HALF_UP)
        } else null

        // In legacy mode (vc == null), benefitCostRatio maps to revenue / totalCost for backward compatibility.
        // In real CVP mode (vc != null), benefitCostRatio is Net Profit / Total Cost, and revenueCostRatio is TR / TC.
        val reportedBcRatio = if (vc != null) bcRatio else rcRatio
        val effectiveRcRatio = rcRatio ?: (if (vc == null) reportedBcRatio else null)
        val isFeasible = if (vc != null) {
            bcRatio?.let { it >= BigDecimal.ZERO }
        } else {
            rcRatio?.let { it >= BigDecimal.ONE }
        }

        return BusinessAnalysis(
            estimatedInterestRupiah = 0L,
            cashBeforeAssumedFinancingRupiah = cashBeforeFinancing,
            financingGapRupiah = financingGap,
            endingCashAfterAssumptionsRupiah = endingCash,
            estimatedRevenueRupiah = revenue,
            totalCostRupiah = totalCost,
            costPerOutputRupiah = unitCost,
            breakEvenQuantity = bep,
            benefitCostRatio = reportedBcRatio,
            feasible = isFeasible,
            outputUnit = assumptions.outputUnit,
            grossProfitRupiah = grossProfit,
            netProfitRupiah = netProfit,
            grossProfitMarginPercent = grossMargin,
            netProfitMarginPercent = netMargin,
            breakEvenRevenueRupiah = bepRevenue,
            revenueCostRatio = effectiveRcRatio,
            fixedCostRupiah = fc,
            variableCostPerUnitRupiah = vc,
            contributionMarginRupiah = unitCm,
            contributionMarginRatio = cmRatio,
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

/**
 * The five facts a cash-flow schedule needs from an event.
 *
 * Separate from [MoneyEvent] so the calculation can be fed straight from a narrow database
 * projection: a book with thousands of transactions should not have to materialise every
 * description, category and counterparty to add up six figures a month. Mirrors what
 * `PostedRecord` already does for [PostingReportCalculator].
 */
data class CashFlowRecord(
    val id: String,
    val date: LocalDate,
    val type: FinancialEventType,
    val cashFlowRole: CashFlowRole,
    val amountRupiah: Long,
    val reversesEventId: String? = null,
)

object MonthlyCashFlowCalculator {
    fun calculate(events: List<MoneyEvent>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> =
        calculateFromRecords(
            events.map {
                CashFlowRecord(it.id, it.date, it.type, it.cashFlowRole, it.amountRupiah, it.reversesEventId)
            },
            openingCashRupiah,
        )

    fun calculateFromRecords(events: List<CashFlowRecord>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> {
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

    private fun cashMovement(event: CashFlowRecord): Long = when (event.type) {
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
