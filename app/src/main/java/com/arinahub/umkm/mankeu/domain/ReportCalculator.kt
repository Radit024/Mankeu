package com.arinahub.umkm.mankeu.domain

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

                else -> 0
            }
        }
        val investingCash = -activeEvents.filter { it.type == FinancialEventType.ASSET_PURCHASE_CASH }
            .sumOf { it.amountRupiah }
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
