package com.mankeu.domain

data class PostedRecord(
    val eventId: String,
    val cashFlowRole: CashFlowRole,
    val account: LedgerAccount,
    val side: PostingSide,
    val amountRupiah: Long,
)

object PostingReportCalculator {
    fun calculate(
        records: List<PostedRecord>,
        obligations: List<ObligationBalance> = emptyList(),
    ): FinancialSummary {
        val income = creditBalance(records, LedgerAccount.REVENUE)
        val operatingExpense = debitBalance(records, LedgerAccount.OPERATING_EXPENSE)
        val interestExpense = debitBalance(records, LedgerAccount.INTEREST_EXPENSE)
        val cashBalance = debitBalance(records, LedgerAccount.CASH)

        fun cashFlow(role: CashFlowRole): Long = records
            .filter { it.account == LedgerAccount.CASH && it.cashFlowRole == role }
            .sumOf { if (it.side == PostingSide.DEBIT) it.amountRupiah else -it.amountRupiah }

        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = operatingExpense + interestExpense,
            profitRupiah = income - operatingExpense - interestExpense,
            operatingCashFlowRupiah = cashFlow(CashFlowRole.OPERATING),
            financingCashFlowRupiah = cashFlow(CashFlowRole.FINANCING),
            investingCashFlowRupiah = cashFlow(CashFlowRole.INVESTING),
            cashBalanceRupiah = cashBalance,
            receivableOutstandingRupiah = obligations.filter {
                it.kind == ObligationKind.RECEIVABLE && !it.reversed
            }.sumOf { it.outstandingAmountRupiah },
            payableOutstandingRupiah = obligations.filter {
                it.kind == ObligationKind.PAYABLE && !it.reversed
            }.sumOf { it.outstandingAmountRupiah },
        )
    }

    private fun debitBalance(records: List<PostedRecord>, account: LedgerAccount): Long = records
        .filter { it.account == account }
        .sumOf { if (it.side == PostingSide.DEBIT) it.amountRupiah else -it.amountRupiah }

    private fun creditBalance(records: List<PostedRecord>, account: LedgerAccount): Long = records
        .filter { it.account == account }
        .sumOf { if (it.side == PostingSide.CREDIT) it.amountRupiah else -it.amountRupiah }
}
