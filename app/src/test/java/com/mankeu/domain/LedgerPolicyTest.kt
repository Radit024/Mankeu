package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LedgerPolicyTest {
    @Test
    fun `every supported money event produces an exact balanced journal`() {
        FinancialEventType.entries.filterNot { it == FinancialEventType.REVERSAL }.forEach { type ->
            val event = MoneyEvent(
                id = type.name,
                businessId = "business",
                projectId = "project",
                type = type,
                amountRupiah = 7_000_001L,
                date = LocalDate.of(2026, 8, 15),
                description = "Test ${type.name}",
                category = "Test",
            )
            val journal = LedgerPolicy.journalFor(event)
            assertEquals(7_000_001L, journal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah })
            assertEquals(7_000_001L, journal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah })
        }
    }

    @Test
    fun `reversal swaps every posting side and retains the exact amount`() {
        val originalEvent = MoneyEvent(
            id = "sale",
            businessId = "business",
            projectId = "project",
            type = FinancialEventType.CREDIT_SALE,
            amountRupiah = 1_250_000,
            date = LocalDate.of(2026, 8, 15),
            description = "Penjualan kredit",
            category = "Penjualan",
        )
        val original = LedgerPolicy.journalFor(originalEvent)
        val reversalEvent = originalEvent.copy(
            id = "reversal",
            type = FinancialEventType.REVERSAL,
            description = "Pembalikan penjualan kredit",
            reversesEventId = originalEvent.id,
        )

        val reversal = LedgerPolicy.reversalFor(original, reversalEvent)

        assertEquals(original.id, reversal.reversalOfJournalId)
        assertEquals(
            original.postings.map { if (it.side == PostingSide.DEBIT) PostingSide.CREDIT else PostingSide.DEBIT },
            reversal.postings.map { it.side },
        )
        assertEquals(1_250_000L, reversal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah })
        assertEquals(1_250_000L, reversal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero rupiah event is rejected`() {
        MoneyEvent("id", "business", "project", FinancialEventType.CASH_EXPENSE, 0, LocalDate.now(), "Invalid", "Test")
    }
}
