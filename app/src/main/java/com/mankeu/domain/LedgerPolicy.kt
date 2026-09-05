package com.mankeu.domain

object LedgerPolicy {
    fun journalFor(event: MoneyEvent): JournalEntry {
        val amount = event.amountRupiah
        val postings = when (event.type) {
            FinancialEventType.CASH_INCOME -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.REVENUE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.CASH_EXPENSE -> pair(
                LedgerAccount.OPERATING_EXPENSE to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.CREDIT_SALE -> pair(
                LedgerAccount.ACCOUNTS_RECEIVABLE to PostingSide.DEBIT,
                LedgerAccount.REVENUE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.CREDIT_PURCHASE_EXPENSE -> pair(
                LedgerAccount.OPERATING_EXPENSE to PostingSide.DEBIT,
                LedgerAccount.ACCOUNTS_PAYABLE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.CREDIT_PURCHASE_ASSET -> pair(
                LedgerAccount.EQUIPMENT_ASSET to PostingSide.DEBIT,
                LedgerAccount.ACCOUNTS_PAYABLE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.RECEIVABLE_COLLECTION -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.ACCOUNTS_RECEIVABLE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.PAYABLE_SETTLEMENT -> pair(
                LedgerAccount.ACCOUNTS_PAYABLE to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.OWNER_CAPITAL -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.OWNER_EQUITY to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.OWNER_DRAWING -> pair(
                LedgerAccount.OWNER_DRAWING to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.LOAN_RECEIVED -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.LOAN_PAYABLE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.LOAN_PRINCIPAL_REPAID -> pair(
                LedgerAccount.LOAN_PAYABLE to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.LOAN_INTEREST_PAID -> pair(
                LedgerAccount.INTEREST_EXPENSE to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.ASSET_PURCHASE_CASH -> pair(
                LedgerAccount.EQUIPMENT_ASSET to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.REVERSAL -> error("Use reversalFor() with the original journal.")
        }
        return JournalEntry(
            id = "journal-${event.id}",
            sourceEventId = event.id,
            date = event.date,
            description = event.description,
            postings = postings,
        ).requireBalanced()
    }

    fun reversalFor(original: JournalEntry, reversalEvent: MoneyEvent): JournalEntry = JournalEntry(
        id = "journal-${reversalEvent.id}",
        sourceEventId = reversalEvent.id,
        date = reversalEvent.date,
        description = reversalEvent.description,
        postings = original.postings.map { posting ->
            posting.copy(side = if (posting.side == PostingSide.DEBIT) PostingSide.CREDIT else PostingSide.DEBIT)
        },
        reversalOfJournalId = original.id,
    ).requireBalanced()

    private fun pair(
        first: Pair<LedgerAccount, PostingSide>,
        second: Pair<LedgerAccount, PostingSide>,
        amount: Long,
    ) = listOf(
        Posting(first.first, first.second, amount),
        Posting(second.first, second.second, amount),
    )
}
