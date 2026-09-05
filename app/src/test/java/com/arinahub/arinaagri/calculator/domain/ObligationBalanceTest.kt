package com.arinahub.arinaagri.calculator.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObligationBalanceTest {
    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun `status distinguishes open partial overdue paid and reversed`() {
        assertEquals(
            ObligationStatus.OPEN,
            ObligationBalance("open", ObligationKind.RECEIVABLE, 1_000, 0, today.plusDays(1)).status(today),
        )
        assertEquals(
            ObligationStatus.PARTIAL,
            ObligationBalance("partial", ObligationKind.RECEIVABLE, 1_000, 400, today.plusDays(1)).status(today),
        )
        assertEquals(
            ObligationStatus.OVERDUE,
            ObligationBalance("overdue", ObligationKind.PAYABLE, 1_000, 400, today.minusDays(1)).status(today),
        )
        assertEquals(
            ObligationStatus.PAID,
            ObligationBalance("paid", ObligationKind.PAYABLE, 1_000, 1_000, today.minusDays(1)).status(today),
        )
        assertEquals(
            ObligationStatus.REVERSED,
            ObligationBalance("reversed", ObligationKind.PAYABLE, 1_000, 0, today.minusDays(1), reversed = true).status(today),
        )
    }

    @Test
    fun `outstanding amount never becomes negative`() {
        assertEquals(
            0,
            ObligationBalance("overpaid", ObligationKind.RECEIVABLE, 1_000, 1_001, null).outstandingAmountRupiah,
        )
    }
}
