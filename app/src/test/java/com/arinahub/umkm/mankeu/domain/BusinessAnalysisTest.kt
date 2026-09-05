package com.arinahub.umkm.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BusinessAnalysisTest {
    @Test
    fun `financing and feasibility inputs affect explicit outputs`() {
        val summary = PostingReportCalculator.calculate(emptyList()).copy(
            operatingExpenseRupiah = 8_000_000,
            operatingCashFlowRupiah = -10_000_000,
            investingCashFlowRupiah = -2_000_000,
        )
        val result = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                openingCashRupiah = 1_000_000,
                ownerCapitalRupiah = 3_000_000,
                loanAmountRupiah = 5_000_000,
                interestRateBasisPoints = 1_000,
                otherFinancingCostRupiah = 500_000,
                outputQuantityText = "1000",
                outputUnit = "kg",
                outputPriceRupiah = 8_000,
            ),
        )

        assertEquals(500_000, result.estimatedInterestRupiah)
        assertEquals(11_000_000, result.financingGapRupiah)
        assertEquals(-4_000_000, result.endingCashAfterAssumptionsRupiah)
        assertEquals(8_000_000L, result.estimatedRevenueRupiah)
        assertEquals(9_000L, result.costPerOutputRupiah)
        assertEquals(BigDecimal("1125.000"), result.breakEvenQuantity)
        assertEquals(9_000_000L, result.breakEvenRevenueRupiah)
        assertEquals(0L, result.grossProfitRupiah)
        assertEquals(-1_000_000L, result.netProfitRupiah)
        assertEquals(BigDecimal("0.00"), result.grossProfitMarginPercent)
        assertEquals(BigDecimal("-12.50"), result.netProfitMarginPercent)
        assertEquals(BigDecimal("0.889"), result.benefitCostRatio)
        assertFalse(result.feasible!!)
    }

    @Test
    fun `undefined feasibility denominators stay explicit`() {
        val result = BusinessAnalysisCalculator.calculate(
            PostingReportCalculator.calculate(emptyList()),
            AnalysisAssumptions(outputQuantityText = "0", outputPriceRupiah = 0),
        )

        assertNull(result.estimatedRevenueRupiah)
        assertNull(result.costPerOutputRupiah)
        assertNull(result.breakEvenQuantity)
        assertNull(result.breakEvenRevenueRupiah)
        assertNull(result.grossProfitRupiah)
        assertNull(result.netProfitRupiah)
        assertNull(result.grossProfitMarginPercent)
        assertNull(result.netProfitMarginPercent)
        assertNull(result.benefitCostRatio)
        assertNull(result.feasible)
    }

    @Test
    fun `profitable MSME calculates gross and net margins and BEP revenue correctly`() {
        val summary = PostingReportCalculator.calculate(emptyList()).copy(
            operatingExpenseRupiah = 6_000_000,
        )
        val result = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                outputQuantityText = "1000",
                outputUnit = "porsi",
                outputPriceRupiah = 15_000,
                loanAmountRupiah = 10_000_000,
                interestRateBasisPoints = 1_200,
                otherFinancingCostRupiah = 800_000,
            ),
        )

        assertEquals(15_000_000L, result.estimatedRevenueRupiah)
        assertEquals(8_000_000L, result.totalCostRupiah)
        assertEquals(9_000_000L, result.grossProfitRupiah)
        assertEquals(BigDecimal("60.00"), result.grossProfitMarginPercent)
        assertEquals(7_000_000L, result.netProfitRupiah)
        assertEquals(BigDecimal("46.67"), result.netProfitMarginPercent)
        assertEquals(BigDecimal("533.333"), result.breakEvenQuantity)
        assertEquals(8_000_000L, result.breakEvenRevenueRupiah)
        assertEquals(8_000L, result.costPerOutputRupiah)
        assertEquals(BigDecimal("1.875"), result.benefitCostRatio)
        assertEquals(true, result.feasible)
    }

    @Test
    fun `monthly cash flow includes zero months and reversals`() {
        val sale = event("sale", FinancialEventType.CASH_INCOME, 1_000_000, LocalDate.of(2026, 1, 5))
        val expense = event("expense", FinancialEventType.CASH_EXPENSE, 200_000, LocalDate.of(2026, 3, 2))
        val reversal = event("undo", FinancialEventType.REVERSAL, 200_000, LocalDate.of(2026, 3, 3)).copy(
            cashFlowRole = expense.cashFlowRole,
            reversesEventId = expense.id,
        )

        val months = MonthlyCashFlowCalculator.calculate(listOf(sale, expense, reversal), openingCashRupiah = 50_000)

        assertEquals(3, months.size)
        assertEquals(0, months[1].netRupiah)
        assertEquals(0, months[2].netRupiah)
        assertEquals(1_050_000, months.last().cumulativeRupiah)
    }

    @Test
    fun `working capital need considers peak monthly cumulative deficit`() {
        val summary = PostingReportCalculator.calculate(emptyList()).copy(
            operatingIncomeRupiah = 45_500_000,
            operatingExpenseRupiah = 22_159_000,
            operatingCashFlowRupiah = 23_341_000,
        )
        val august = event("aug", FinancialEventType.CASH_EXPENSE, 13_464_000, LocalDate.of(2026, 8, 1))
        val september = event("sep", FinancialEventType.CASH_EXPENSE, 1_330_000, LocalDate.of(2026, 9, 1))
        val october = event("oct", FinancialEventType.CASH_EXPENSE, 840_000, LocalDate.of(2026, 10, 1))
        val november = event("nov", FinancialEventType.CASH_EXPENSE, 3_235_000, LocalDate.of(2026, 11, 1))
        val december = event("dec", FinancialEventType.CASH_INCOME, 45_500_000, LocalDate.of(2026, 12, 1))
        val decExpense = event("dec_exp", FinancialEventType.CASH_EXPENSE, 3_290_000, LocalDate.of(2026, 12, 1))

        val monthlyFlows = MonthlyCashFlowCalculator.calculate(
            listOf(august, september, october, november, december, decExpense),
            openingCashRupiah = 0,
        )

        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(openingCashRupiah = 0),
            monthlyFlows = monthlyFlows,
        )

        // Peak cumulative deficit in November is 13464000 + 1330000 + 840000 + 3235000 = 18869000
        assertEquals(18_869_000L, analysis.financingGapRupiah)
    }

    private fun event(id: String, type: FinancialEventType, amount: Long, date: LocalDate) = MoneyEvent(
        id = id,
        businessId = "business",
        projectId = "project",
        bookId = "book",
        type = type,
        amountRupiah = amount,
        date = date,
        description = id,
        category = "test",
    )
}
