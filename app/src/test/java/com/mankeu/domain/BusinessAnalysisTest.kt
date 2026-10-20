package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                outputQuantityText = "1000",
                outputUnit = "kg",
                outputPriceRupiah = 8_000,
            ),
        )

        assertEquals(0L, result.estimatedInterestRupiah)
        assertEquals(11_000_000, result.financingGapRupiah)
        // Strict cash (DEC-011): no separate financing cost, so cost = operating expense only.
        assertEquals(-8_000_000, result.endingCashAfterAssumptionsRupiah)
        assertEquals(8_000_000L, result.estimatedRevenueRupiah)
        assertEquals(8_000L, result.costPerOutputRupiah)
        assertEquals(BigDecimal("1000.000"), result.breakEvenQuantity)
        assertEquals(8_000_000L, result.breakEvenRevenueRupiah)
        assertEquals(0L, result.grossProfitRupiah)
        assertEquals(0L, result.netProfitRupiah)
        assertEquals(BigDecimal("0.00"), result.grossProfitMarginPercent)
        assertEquals(BigDecimal("0.00"), result.netProfitMarginPercent)
        assertEquals(BigDecimal("1.000"), result.benefitCostRatio)
        assertTrue(result.feasible!!)
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
            ),
        )

        assertEquals(15_000_000L, result.estimatedRevenueRupiah)
        assertEquals(6_000_000L, result.totalCostRupiah)
        assertEquals(9_000_000L, result.grossProfitRupiah)
        assertEquals(BigDecimal("60.00"), result.grossProfitMarginPercent)
        assertEquals(9_000_000L, result.netProfitRupiah)
        assertEquals(BigDecimal("60.00"), result.netProfitMarginPercent)
        assertEquals(BigDecimal("400.000"), result.breakEvenQuantity)
        assertEquals(6_000_000L, result.breakEvenRevenueRupiah)
        assertEquals(6_000L, result.costPerOutputRupiah)
        assertEquals(BigDecimal("2.500"), result.benefitCostRatio)
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

    @Test
    fun `verified micro MSME benchmark Kedai Kopi Berkah produces exact BEP 400 and ratios`() {
        // Benchmark: Kedai Kopi & Teh Susu Berkah
        // P = Rp 10.000 / cup
        // VC = Rp 5.000 / cup
        // FC = Rp 2.000.000 / bulan
        // Benchmark CVP Pure Function:
        val cvp = BusinessAnalysisCalculator.calculateCvp(
            priceRupiah = 10_000,
            variableCostPerUnitRupiah = 5_000,
            fixedCostRupiah = 2_000_000,
            actualQuantityText = "700",
        )

        assertEquals(5_000L, cvp.contributionMarginRupiah)
        assertEquals(BigDecimal("0.5000"), cvp.contributionMarginRatio)
        assertEquals(BigDecimal("400.000"), cvp.breakEvenQuantity) // EXACT 400 units!
        assertEquals(4_000_000L, cvp.breakEvenRevenueRupiah)
        assertEquals(7_000_000L, cvp.totalRevenueRupiah)
        assertEquals(5_500_000L, cvp.totalCostRupiah)
        assertEquals(1_500_000L, cvp.netProfitRupiah)
        assertEquals(BigDecimal("1.273"), cvp.revenueCostRatio) // R/C Ratio = 1.27 (rounded)
        assertEquals(BigDecimal("0.273"), cvp.benefitCostRatio) // B/C Ratio = 0.27 (rounded)
        assertEquals(true, cvp.isEfficient)
        assertEquals(true, cvp.isFeasible)

        // Integrated BusinessAnalysisCalculator with assumptions:
        val summary = PostingReportCalculator.calculate(emptyList()).copy(
            operatingExpenseRupiah = 2_000_000,
        )
        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                fixedCostRupiah = 2_000_000,
                variableCostPerUnitRupiah = 5_000,
                outputPriceRupiah = 10_000,
                outputQuantityText = "700",
                outputUnit = "cup",
            ),
        )

        assertEquals(BigDecimal("400.000"), analysis.breakEvenQuantity)
        assertEquals(4_000_000L, analysis.breakEvenRevenueRupiah)
        assertEquals(7_000_000L, analysis.estimatedRevenueRupiah)
        assertEquals(5_500_000L, analysis.totalCostRupiah)
        assertEquals(1_500_000L, analysis.netProfitRupiah)
        assertEquals(BigDecimal("1.273"), analysis.revenueCostRatio)
        assertEquals(BigDecimal("0.273"), analysis.benefitCostRatio)
        assertEquals(true, analysis.feasible)
    }

    @Test
    fun `BEP calculates both unit and rupiah even when target quantity is null`() {
        // Micro business: Warung Bakso
        // P = Rp 15.000 / porsi
        // VC = Rp 9.000 / porsi
        // FC = Rp 3.000.000 / bulan
        // User has not entered a sales target yet
        val summary = PostingReportCalculator.calculate(emptyList())
        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                fixedCostRupiah = 3_000_000,
                variableCostPerUnitRupiah = 9_000,
                outputPriceRupiah = 15_000,
                outputUnit = "porsi",
                outputQuantityText = null, // No target volume
            ),
        )

        // CM = 15.000 - 9.000 = 6.000
        assertEquals(6_000L, analysis.contributionMarginRupiah)
        // BEP Unit = 3.000.000 / 6.000 = 500.000 porsi
        assertEquals(BigDecimal("500.000"), analysis.breakEvenQuantity)
        // BEP Rupiah = 500 * 15.000 = Rp 7.500.000
        assertEquals(7_500_000L, analysis.breakEvenRevenueRupiah)
        // Without target volume, revenue & net profit are null, but BEP is fully known!
        assertNull(analysis.estimatedRevenueRupiah)
        assertNull(analysis.netProfitRupiah)
    }

    @Test
    fun `BEP returns null when price is less than or equal to variable cost`() {
        val summary = PostingReportCalculator.calculate(emptyList())
        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                fixedCostRupiah = 2_000_000,
                variableCostPerUnitRupiah = 10_000,
                outputPriceRupiah = 8_000, // Losing Rp 2.000 per unit
                outputUnit = "unit",
            ),
        )

        assertNull(analysis.breakEvenQuantity)
        assertNull(analysis.breakEvenRevenueRupiah)
    }

    @Test
    fun `BEP falls back to operating expense when fixed cost assumption is omitted`() {
        // Recorded operating expense from transactions: Rp 4.000.000
        val summary = PostingReportCalculator.calculate(emptyList()).copy(
            operatingExpenseRupiah = 4_000_000,
        )
        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                fixedCostRupiah = null, // Left blank, should automatically use operatingExpenseRupiah
                variableCostPerUnitRupiah = 12_000,
                outputPriceRupiah = 20_000, // CM = 8.000
                outputUnit = "paket",
            ),
        )

        // BEP Unit = 4.000.000 / 8.000 = 500 paket
        assertEquals(BigDecimal("500.000"), analysis.breakEvenQuantity)
        // BEP Rupiah = 500 * 20.000 = Rp 10.000.000
        assertEquals(10_000_000L, analysis.breakEvenRevenueRupiah)
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
