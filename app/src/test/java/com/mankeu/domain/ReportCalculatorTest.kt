package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportCalculatorTest {
    @Test
    fun `capital and loan affect cash but never operating income or profit`() {
        val events = listOf(
            event("income", FinancialEventType.CASH_INCOME, 10_000_000),
            event("expense", FinancialEventType.CASH_EXPENSE, 4_000_000),
            event("capital", FinancialEventType.OWNER_CAPITAL, 3_000_000),
            event("loan", FinancialEventType.LOAN_RECEIVED, 5_000_000),
            event("interest", FinancialEventType.LOAN_INTEREST_PAID, 250_000),
            event("principal", FinancialEventType.LOAN_PRINCIPAL_REPAID, 1_000_000),
        )

        val summary = ReportCalculator.calculate(events)

        assertEquals(10_000_000, summary.operatingIncomeRupiah)
        assertEquals(4_250_000, summary.operatingExpenseRupiah)
        assertEquals(5_750_000, summary.profitRupiah)
        assertEquals(7_000_000, summary.financingCashFlowRupiah)
        assertEquals(12_750_000, summary.cashBalanceRupiah)
    }

    @Test
    fun `credit recognition and later settlement never duplicate revenue or expense`() {
        val events = listOf(
            event("credit-sale", FinancialEventType.CREDIT_SALE, 2_000_000),
            event("collection", FinancialEventType.RECEIVABLE_COLLECTION, 750_000),
            event("credit-purchase", FinancialEventType.CREDIT_PURCHASE_EXPENSE, 900_000),
            event("settlement", FinancialEventType.PAYABLE_SETTLEMENT, 400_000),
        )
        val obligations = listOf(
            ObligationBalance("receivable", ObligationKind.RECEIVABLE, 2_000_000, 750_000, null),
            ObligationBalance("payable", ObligationKind.PAYABLE, 900_000, 400_000, null),
        )

        val summary = ReportCalculator.calculate(events, obligations)

        assertEquals(2_000_000, summary.operatingIncomeRupiah)
        assertEquals(900_000, summary.operatingExpenseRupiah)
        assertEquals(1_100_000, summary.profitRupiah)
        assertEquals(350_000, summary.operatingCashFlowRupiah)
        assertEquals(350_000, summary.cashBalanceRupiah)
        assertEquals(1_250_000, summary.receivableOutstandingRupiah)
        assertEquals(500_000, summary.payableOutstandingRupiah)
    }

    @Test
    fun `reversed source event is excluded from every report amount`() {
        val reversed = event("income", FinancialEventType.CASH_INCOME, 10_000_000)
            .copy(reversedByEventId = "reversal")
        val reversal = event("reversal", FinancialEventType.REVERSAL, 10_000_000)

        val summary = ReportCalculator.calculate(listOf(reversed, reversal))

        assertEquals(0, summary.operatingIncomeRupiah)
        assertEquals(0, summary.profitRupiah)
        assertEquals(0, summary.cashBalanceRupiah)
    }

    private fun event(id: String, type: FinancialEventType, amount: Long) = MoneyEvent(
        id,
        "business",
        "project",
        type,
        amount,
        LocalDate.of(2026, 8, 15),
        id,
        "test",
    )
}
