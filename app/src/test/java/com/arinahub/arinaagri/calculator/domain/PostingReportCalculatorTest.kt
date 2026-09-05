package com.arinahub.arinaagri.calculator.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PostingReportCalculatorTest {
    @Test
    fun `reports derive from balanced postings and cash roles`() {
        val events = listOf(
            event("credit-sale", FinancialEventType.CREDIT_SALE, 2_000_000),
            event("collection", FinancialEventType.RECEIVABLE_COLLECTION, 750_000),
            event("expense", FinancialEventType.CASH_EXPENSE, 400_000),
            event("capital", FinancialEventType.OWNER_CAPITAL, 1_000_000),
        )
        val records = events.flatMap { event ->
            LedgerPolicy.journalFor(event).postings.map { posting ->
                PostedRecord(event.id, event.cashFlowRole, posting.account, posting.side, posting.amountRupiah)
            }
        }

        val summary = PostingReportCalculator.calculate(records)

        assertEquals(2_000_000, summary.operatingIncomeRupiah)
        assertEquals(400_000, summary.operatingExpenseRupiah)
        assertEquals(1_600_000, summary.profitRupiah)
        assertEquals(350_000, summary.operatingCashFlowRupiah)
        assertEquals(1_000_000, summary.financingCashFlowRupiah)
        assertEquals(1_350_000, summary.cashBalanceRupiah)
    }

    @Test
    fun `original plus reversing postings net every report back to zero`() {
        val event = event("sale", FinancialEventType.CASH_INCOME, 5_000_000)
        val original = LedgerPolicy.journalFor(event)
        val reversalEvent = event("reversal", FinancialEventType.REVERSAL, 5_000_000).copy(
            cashFlowRole = event.cashFlowRole,
        )
        val reversal = LedgerPolicy.reversalFor(original, reversalEvent)
        val records = (original.postings.map { event.cashFlowRole to it } +
            reversal.postings.map { reversalEvent.cashFlowRole to it }).mapIndexed { index, (role, posting) ->
            PostedRecord("event-$index", role, posting.account, posting.side, posting.amountRupiah)
        }

        val summary = PostingReportCalculator.calculate(records)

        assertEquals(0, summary.operatingIncomeRupiah)
        assertEquals(0, summary.profitRupiah)
        assertEquals(0, summary.operatingCashFlowRupiah)
        assertEquals(0, summary.cashBalanceRupiah)
    }

    private fun event(id: String, type: FinancialEventType, amount: Long) = MoneyEvent(
        id = id,
        businessId = "business",
        projectId = "project",
        bookId = "book",
        type = type,
        amountRupiah = amount,
        date = LocalDate.of(2026, 8, 15),
        description = id,
        category = "test",
    )
}
