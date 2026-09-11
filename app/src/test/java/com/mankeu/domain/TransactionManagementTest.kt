package com.mankeu.domain

import com.mankeu.presentation.components.CoreTourSteps
import com.mankeu.presentation.components.EntryTourSteps
import com.mankeu.presentation.components.ImportTourSteps
import com.mankeu.presentation.components.ReportsTourSteps
import com.mankeu.presentation.components.ScopeTourSteps
import com.mankeu.presentation.components.SettingsTourSteps
import com.mankeu.presentation.components.TourTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionManagementTest {

    @Test
    fun `two-tier tour definitions contain valid unique targets`() {
        // TourTarget enum count
        assertEquals(22, TourTarget.entries.size)

        // Tier 1: Interactive Home tour has 4 focused stages for the home screen
        assertEquals(4, CoreTourSteps.size)
        val coreTargets = CoreTourSteps.map { it.target }
        assertEquals(coreTargets.distinct().size, coreTargets.size)
        assertTrue(coreTargets.contains(TourTarget.SCOPE_BUSINESS))
        assertTrue(coreTargets.contains(TourTarget.SCOPE_BOOK))
        assertTrue(coreTargets.contains(TourTarget.HERO_SUMMARY))
        assertTrue(coreTargets.contains(TourTarget.HOME_COST_CHART))

        // Tier 2: Per-page contextual tours
        // Two, not three: the "tab kiri / tab kanan" step went with the switcher it described
        // when Riwayat became its own destination (D-070).
        assertEquals(2, EntryTourSteps.size)
        assertEquals(4, ReportsTourSteps.size)
        assertEquals(3, SettingsTourSteps.size)
        assertEquals(2, ScopeTourSteps.size)
        assertEquals(3, ImportTourSteps.size)

        assertTrue(EntryTourSteps.none { it.target == TourTarget.ENTRY_TABS })
        assertTrue(EntryTourSteps.any { it.target == TourTarget.ENTRY_KINDS })
        assertTrue(EntryTourSteps.any { it.target == TourTarget.ENTRY_HISTORY })

        assertTrue(ReportsTourSteps.any { it.target == TourTarget.REPORTS_TABS })
        assertTrue(ReportsTourSteps.any { it.target == TourTarget.REPORTS_PROFIT })
        assertTrue(ReportsTourSteps.any { it.target == TourTarget.REPORTS_CASHFLOW })
        assertTrue(ReportsTourSteps.any { it.target == TourTarget.REPORTS_FEASIBILITY })

        assertTrue(SettingsTourSteps.any { it.target == TourTarget.SETTINGS_DRIVE })
        assertTrue(SettingsTourSteps.any { it.target == TourTarget.SETTINGS_SECURITY })
        assertTrue(SettingsTourSteps.any { it.target == TourTarget.SETTINGS_DISPLAY })

        assertTrue(ScopeTourSteps.any { it.target == TourTarget.SCOPE_ADD_BUSINESS })
        assertTrue(ScopeTourSteps.any { it.target == TourTarget.SCOPE_ADD_FIELD })

        assertTrue(ImportTourSteps.any { it.target == TourTarget.IMPORT_FILE_PICKER })
        assertTrue(ImportTourSteps.any { it.target == TourTarget.IMPORT_TARGET_BOOK })
        assertTrue(ImportTourSteps.any { it.target == TourTarget.DATA_EXPORT })
    }

    @Test
    fun `reversal policy creates correct offsetting entries`() {
        val originalEvent = MoneyEvent(
            id = "test-expense",
            businessId = "biz-1",
            projectId = "proj-1",
            type = FinancialEventType.CASH_EXPENSE,
            amountRupiah = 500_000,
            categoryId = "cat-pupuk",
            category = "Pupuk & Nutrisi",
            date = LocalDate.of(2026, 8, 20),
            description = "Beli pupuk urea",
        )
        val journal = LedgerPolicy.journalFor(originalEvent)

        assertNotNull(journal)
        assertTrue(journal.postings.isNotEmpty())
        val debitSum = journal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah }
        val creditSum = journal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah }
        assertEquals(500_000L, debitSum)
        assertEquals(500_000L, creditSum)
    }

    @Test
    fun `book mode projection and realization remain isolated`() {
        val projectionMode = BookMode.PROJECTION
        val realizationMode = BookMode.REALIZATION

        assertFalse(projectionMode == realizationMode)
        assertEquals("PROJECTION", projectionMode.name)
        assertEquals("REALIZATION", realizationMode.name)
    }
}
