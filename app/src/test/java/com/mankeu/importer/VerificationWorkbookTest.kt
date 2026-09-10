package com.mankeu.importer

import com.mankeu.domain.AnalysisAssumptions
import com.mankeu.domain.BookMode
import com.mankeu.domain.BudgetPlanCalculator
import com.mankeu.domain.BusinessAnalysisCalculator
import com.mankeu.domain.BusinessSector
import com.mankeu.domain.BusinessTemplate
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.MonthlyCashFlowCalculator
import com.mankeu.domain.ObligationBalance
import com.mankeu.domain.ObligationKind
import com.mankeu.domain.PlanBudgetLine
import com.mankeu.domain.ReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * End-to-end reconciliation against `simulasi_kelayakan_umkm_verifikasi.xlsx`.
 *
 * The workbook is a complete MSME feasibility study — a bottled-sambal producer over one
 * three-month cycle — written by `scripts/generate_verification_workbook.py`. Every figure on its
 * report sheets is a live Excel formula carrying the cached value that formula evaluates to, and
 * every one of those cached values was computed with the same arithmetic the Kotlin domain uses.
 *
 * This test reads the workbook through the real importer and asserts that
 * [ReportCalculator], [MonthlyCashFlowCalculator], [BudgetPlanCalculator] and
 * [BusinessAnalysisCalculator] reproduce the workbook exactly, to the rupiah.
 *
 * The golden constants below are duplicated in
 * `docs/evidence/verification-workbook/expected.json`, which the generator writes at the same
 * time. If the case study is ever regenerated with different figures, this test fails loudly
 * rather than quietly agreeing with whatever the new file happens to say.
 */
class VerificationWorkbookTest {

    // ---- Golden values -----------------------------------------------------------------
    private val transactionCount = 56
    private val budgetLineCount = 45
    private val outputUnit = "botol"
    private val outputPrice = 28_000L

    private val planExpense = 128_050_000L
    private val planAsset = 13_000_000L
    private val planRevenue = 168_000_000L

    private val actualRevenue = 149_240_000L
    private val actualOperatingExpense = 133_590_000L
    private val actualProfit = 15_650_000L
    private val actualOperatingCash = 12_250_000L
    private val actualInvestingCash = -16_200_000L
    private val actualFinancingCash = 31_000_000L
    private val actualCashBalance = 27_050_000L
    private val actualReceivableOutstanding = 3_400_000L
    private val actualSoldQuantity = 5_330L

    // ------------------------------------------------------------------------------------

    @Test
    fun `the verification workbook is committed and readable`() {
        // Not assumeTrue: a missing workbook here means the reconciliation never ran, and a
        // silently skipped test that keeps the build green is exactly the failure round 4 found.
        assertTrue(
            "simulasi_kelayakan_umkm_verifikasi.xlsx harus ada di akar repositori. " +
                "Jalankan: py scripts/generate_verification_workbook.py",
            workbookFile().isFile,
        )
        val preview = preview()
        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(
            listOf(
                "Petunjuk dan Parameter",
                "RAB (Proyeksi)",
                "Transaksi Harian (Realisasi)",
                "Laporan Laba Rugi",
                "Arus Kas dan Analisis Kelayakan",
            ),
            preview.recognizedSheets,
        )
        // The only warning is the one announcing the recovered parameters; nothing was skipped.
        assertEquals(1, preview.warnings.size)
        assertTrue(preview.warnings.single(), "parameter perencanaan" in preview.warnings.single())
        // The workbook states which book each row belongs to — RAB is the plan, the daily ledger
        // is what happened — so no target-book picker is offered and neither book can swallow the
        // other.
        assertTrue(preview.carriesBookMode)
        assertEquals(budgetLineCount, preview.budgetCandidates.count { it.bookMode == BookMode.PROJECTION })
        assertEquals(transactionCount, preview.candidates.count { it.bookMode == BookMode.REALIZATION })
    }

    @Test
    fun `every declared transaction kind survives the import`() {
        val preview = preview()
        assertEquals(transactionCount, preview.candidates.size)
        assertEquals(0, preview.enrichmentCount)
        assertEquals(transactionCount, preview.directCount)

        // The whole point of the Jenis column: without it these eleven rows would all have
        // arrived as plain CASH_INCOME or CASH_EXPENSE.
        val byType = preview.candidates.groupingBy { it.type }.eachCount()
        assertEquals(1, byType[FinancialEventType.OWNER_CAPITAL])
        assertEquals(1, byType[FinancialEventType.OWNER_DRAWING])
        assertEquals(1, byType[FinancialEventType.LOAN_RECEIVED])
        assertEquals(2, byType[FinancialEventType.LOAN_PRINCIPAL_REPAID])
        assertEquals(2, byType[FinancialEventType.LOAN_INTEREST_PAID])
        assertEquals(6, byType[FinancialEventType.ASSET_PURCHASE_CASH])
        assertEquals(2, byType[FinancialEventType.CREDIT_SALE])
        assertEquals(2, byType[FinancialEventType.CREDIT_PURCHASE_EXPENSE])
        assertEquals(2, byType[FinancialEventType.RECEIVABLE_COLLECTION])
        assertEquals(2, byType[FinancialEventType.PAYABLE_SETTLEMENT])

        // Credit rows carry the counterparty and due date the obligation needs.
        preview.candidates.filter { it.type == FinancialEventType.CREDIT_SALE }.forEach {
            assertNotNull(it.counterparty)
            assertNotNull(it.dueDate)
            assertNotNull(it.sourceObligationId)
        }
        // Settlements point back at the obligation they clear.
        preview.candidates.filter { it.type == FinancialEventType.RECEIVABLE_COLLECTION }.forEach {
            assertNotNull(it.sourceObligationId)
        }

        // Volume, unit and unit price come across rather than being retyped by hand.
        val chilli = preview.candidates.single { it.description == "Belanja cabai rawit merah" && it.date.monthValue == 2 }
        assertEquals("400", chilli.quantityText)
        assertEquals("kg", chilli.unit)
        assertEquals(45_000L, chilli.unitPriceRupiah)
    }

    @Test
    fun `realization report reproduces the workbook profit and loss`() {
        val summary = ReportCalculator.calculate(realizationEvents(), realizationObligations())

        assertEquals(actualRevenue, summary.operatingIncomeRupiah)
        assertEquals(actualOperatingExpense, summary.operatingExpenseRupiah)
        assertEquals(actualProfit, summary.profitRupiah)
        assertEquals(actualOperatingCash, summary.operatingCashFlowRupiah)
        assertEquals(actualInvestingCash, summary.investingCashFlowRupiah)
        assertEquals(actualFinancingCash, summary.financingCashFlowRupiah)
        assertEquals(actualCashBalance, summary.cashBalanceRupiah)
        assertEquals(actualReceivableOutstanding, summary.receivableOutstandingRupiah)
        assertEquals(0L, summary.payableOutstandingRupiah)
        assertEquals(
            summary.cashBalanceRupiah,
            summary.operatingCashFlowRupiah + summary.investingCashFlowRupiah + summary.financingCashFlowRupiah,
        )
    }

    @Test
    fun `monthly cash flow reproduces the workbook cash flow sheet`() {
        val months = MonthlyCashFlowCalculator.calculate(realizationEvents(), openingCashRupiah = 0)

        assertEquals(3, months.size)
        val expected = listOf(
            Quintuple(YearMonth.of(2026, 2), -8_320_000L, -13_000_000L, 35_000_000L, 13_680_000L),
            Quintuple(YearMonth.of(2026, 3), 12_640_000L, 0L, -1_000_000L, 25_320_000L),
            Quintuple(YearMonth.of(2026, 4), 7_930_000L, -3_200_000L, -3_000_000L, 27_050_000L),
        )
        months.forEachIndexed { index, month ->
            val (yearMonth, operating, investing, financing, cumulative) = expected[index]
            assertEquals(yearMonth, month.month)
            assertEquals("operasi ${month.month}", operating, month.operatingRupiah)
            assertEquals("investasi ${month.month}", investing, month.investingRupiah)
            assertEquals("pendanaan ${month.month}", financing, month.financingRupiah)
            assertEquals("bersih ${month.month}", operating + investing + financing, month.netRupiah)
            assertEquals("kumulatif ${month.month}", cumulative, month.cumulativeRupiah)
        }
        assertEquals(actualCashBalance, months.last().cumulativeRupiah)
    }

    @Test
    fun `realization feasibility reproduces the workbook analysis sheet`() {
        val summary = ReportCalculator.calculate(realizationEvents(), realizationObligations())
        val months = MonthlyCashFlowCalculator.calculate(realizationEvents(), openingCashRupiah = 0)
        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                outputQuantityText = actualSoldQuantity.toString(),
                outputUnit = outputUnit,
                outputPriceRupiah = outputPrice,
            ),
            months,
        )

        assertEquals(actualRevenue, analysis.estimatedRevenueRupiah)
        assertEquals(actualOperatingExpense, analysis.totalCostRupiah)
        assertEquals(25_064L, analysis.costPerOutputRupiah)
        assertEquals(BigDecimal("4771.071"), analysis.breakEvenQuantity)
        assertEquals(actualOperatingExpense, analysis.breakEvenRevenueRupiah)
        assertEquals(BigDecimal("1.117"), analysis.benefitCostRatio)
        assertEquals(actualProfit, analysis.netProfitRupiah)
        assertEquals(BigDecimal("10.49"), analysis.netProfitMarginPercent)
        assertEquals(true, analysis.feasible)
        // Operations and investment alone left the business Rp3.950.000 short; that is the
        // working capital the owner had to raise, and it is what the workbook's section B shows.
        assertEquals(-3_950_000L, analysis.cashBeforeAssumedFinancingRupiah)
        assertEquals(3_950_000L, analysis.financingGapRupiah)
    }

    @Test
    fun `plan feasibility reproduces the workbook projection analysis`() {
        val budgets = preview().budgetCandidates
        assertEquals(budgetLineCount, budgets.size)
        assertTrue(budgets.all { it.bookMode == BookMode.PROJECTION })

        val lines = budgets.map { PlanBudgetLine(it.plannedTotalRupiah, roleOf(it), it.plannedDate) }
        val summary = BudgetPlanCalculator.summarize(lines)
        assertEquals(planRevenue, summary.operatingIncomeRupiah)
        assertEquals(planExpense, summary.operatingExpenseRupiah)
        assertEquals(-planAsset, summary.investingCashFlowRupiah)

        val analysis = BusinessAnalysisCalculator.calculate(
            summary,
            AnalysisAssumptions(
                openingCashRupiah = 0,
                ownerCapitalRupiah = 15_000_000,
                loanAmountRupiah = 20_000_000,
                interestRateBasisPoints = 1_200,
                otherFinancingCostRupiah = 400_000,
                outputQuantityText = "6000",
                outputUnit = outputUnit,
                outputPriceRupiah = outputPrice,
            ),
            // Exactly what HomeScreen and ReportsScreen feed it in plan mode.
            BudgetPlanCalculator.schedule(lines),
        )

        assertEquals(2_400_000L, analysis.estimatedInterestRupiah)
        assertEquals(130_850_000L, analysis.totalCostRupiah)
        assertEquals(168_000_000L, analysis.estimatedRevenueRupiah)
        assertEquals(21_808L, analysis.costPerOutputRupiah)
        assertEquals(BigDecimal("4673.214"), analysis.breakEvenQuantity)
        assertEquals(130_850_000L, analysis.breakEvenRevenueRupiah)
        assertEquals(BigDecimal("1.284"), analysis.benefitCostRatio)
        assertEquals(37_150_000L, analysis.netProfitRupiah)
        assertEquals(BigDecimal("22.11"), analysis.netProfitMarginPercent)
        assertEquals(26_950_000L, analysis.cashBeforeAssumedFinancingRupiah)
        assertEquals(59_150_000L, analysis.endingCashAfterAssumptionsRupiah)
        assertEquals(true, analysis.feasible)
        // The plan is profitable over the cycle and still Rp14.850.000 short in February, because
        // the equipment and the first month's stock land before the sales ramp. That trough is the
        // working capital the owner has to raise, and an undated RAB could not have found it.
        assertEquals(14_850_000L, analysis.financingGapRupiah)
    }

    @Test
    fun `plan cash schedule reproduces the workbook plan schedule`() {
        val lines = preview().budgetCandidates.map {
            PlanBudgetLine(it.plannedTotalRupiah, roleOf(it), it.plannedDate)
        }
        val schedule = BudgetPlanCalculator.schedule(lines)

        assertEquals(3, schedule.size)
        val expected = listOf(
            Triple(YearMonth.of(2026, 2), -1_850_000L, -13_000_000L),
            Triple(YearMonth.of(2026, 3), 13_900_000L, 0L),
            Triple(YearMonth.of(2026, 4), 27_900_000L, 0L),
        )
        val cumulative = listOf(-14_850_000L, -950_000L, 26_950_000L)
        schedule.forEachIndexed { index, month ->
            val (yearMonth, operating, investing) = expected[index]
            assertEquals(yearMonth, month.month)
            assertEquals("operasi rencana ${month.month}", operating, month.operatingRupiah)
            assertEquals("investasi rencana ${month.month}", investing, month.investingRupiah)
            assertEquals(0L, month.financingRupiah)
            assertEquals(cumulative[index], month.cumulativeRupiah)
        }
        // The schedule closes onto the same plan cash position the summary reports.
        assertEquals(
            BudgetPlanCalculator.summarize(lines).cashBalanceRupiah,
            schedule.last().cumulativeRupiah,
        )
    }

    @Test
    fun `planning parameters are read from the workbook instead of retyped`() {
        val assumptions = requireNotNull(preview().assumptions) {
            "Lembar Petunjuk dan Parameter harus terbaca sebagai asumsi perencanaan."
        }
        assertEquals(0L, assumptions.openingCashRupiah)
        assertEquals(15_000_000L, assumptions.ownerCapitalRupiah)
        assertEquals(20_000_000L, assumptions.loanAmountRupiah)
        // 1.200 basis points, not a Rp1.200 loan: "Suku bunga pinjaman" names a loan too.
        assertEquals(1_200, assumptions.interestRateBasisPoints)
        assertEquals(400_000L, assumptions.otherFinancingCostRupiah)
        assertEquals("6000", assumptions.outputQuantityText)
        assertEquals(outputUnit, assumptions.outputUnit)
        assertEquals(outputPrice, assumptions.outputPriceRupiah)
        assertEquals(8, assumptions.fieldCount)
        assertEquals(7, assumptions.sourceEvidence.size)
        assertTrue(assumptions.sourceEvidence.all { it.startsWith("Petunjuk dan Parameter!") })
    }

    @Test
    fun `budget lines keep their volume unit price and planned month`() {
        val budgets = preview().budgetCandidates
        val bottles = budgets.filter { it.name == "Botol kaca dan tutup" }
        assertEquals(3, bottles.size)
        bottles.forEach {
            assertEquals("2000", it.quantityText)
            assertEquals("pcs", it.unit)
            assertEquals(2_500L, it.unitPriceRupiah)
            assertEquals(5_000_000L, it.plannedTotalRupiah)
        }
        assertEquals(15_000_000L, bottles.sumOf { it.plannedTotalRupiah })
        // Every plan line is dated, which is what lets the plan produce a cash schedule at all.
        assertEquals(emptyList<String>(), budgets.filter { it.plannedDate == null }.map { it.name })
        assertEquals(
            listOf(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)),
            bottles.mapNotNull { it.plannedDate }.sorted(),
        )
        // Every RAB line was placed on a category the Kuliner template already defines, so the
        // roles below are the app's own and not an EXPENSE fallback invented at import time.
        assertEquals(
            emptyList<String>(),
            budgets.map { it.categoryName }.distinct().filterNot { it in templateRoles.keys },
        )
        // And the workbook's own section headings carry the same roles, so the import is right
        // even on a business created from a different sector template.
        assertEquals(
            CategoryRole.ASSET,
            budgets.single { it.name == "Mesin giling cabai" }.categoryRole,
        )
        assertEquals(
            CategoryRole.REVENUE,
            budgets.first { it.name == "Target penjualan sambal kemasan" }.categoryRole,
        )
        assertEquals(
            CategoryRole.EXPENSE,
            budgets.first { it.name == "Cabai rawit merah" }.categoryRole,
        )
    }

    // ---- helpers -----------------------------------------------------------------------

    private data class Quintuple(
        val month: YearMonth,
        val operating: Long,
        val investing: Long,
        val financing: Long,
        val cumulative: Long,
    )

    private val templateRoles: Map<String, CategoryRole> by lazy {
        BusinessTemplate.forSector(BusinessSector.CULINARY_FB).defaultCategories.associate { it.name to it.role }
    }

    /**
     * Mirrors FinanceRepository.commitImport: a name the business already has keeps its role, and
     * a name it does not have is created with the role the workbook's own section implies.
     */
    private fun roleOf(candidate: ImportBudgetCandidate): CategoryRole =
        templateRoles[candidate.categoryName] ?: candidate.categoryRole

    private fun workbookFile(): File {
        val appDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val repositoryRoot = appDirectory.parentFile ?: appDirectory
        return repositoryRoot.resolve("simulasi_kelayakan_umkm_verifikasi.xlsx")
    }

    private fun preview(): WorkbookImportPreview {
        val file = workbookFile()
        return FinanceWorkbookImporter().preview(file.name, file.readBytes())
    }

    /** The imported rows as the domain sees them once committed. */
    private fun realizationEvents(): List<MoneyEvent> = preview().candidates
        .filter { it.bookMode == BookMode.REALIZATION }
        .mapIndexed { index, candidate ->
            MoneyEvent(
                id = "event-$index",
                businessId = "business",
                projectId = "project",
                bookId = "realization",
                type = candidate.type,
                amountRupiah = candidate.amountRupiah,
                date = candidate.date,
                description = candidate.description,
                category = candidate.category,
                counterparty = candidate.counterparty,
                dueDate = candidate.dueDate,
                quantityText = candidate.quantityText,
                unit = candidate.unit,
                unitPriceRupiah = candidate.unitPriceRupiah,
                obligationId = candidate.sourceObligationId,
            )
        }

    private fun realizationObligations(): List<ObligationBalance> {
        val candidates = preview().candidates
        val credit = candidates.filter {
            it.type in setOf(
                FinancialEventType.CREDIT_SALE,
                FinancialEventType.CREDIT_PURCHASE_EXPENSE,
                FinancialEventType.CREDIT_PURCHASE_ASSET,
            )
        }
        val settledByObligation = candidates
            .filter { it.type in setOf(FinancialEventType.RECEIVABLE_COLLECTION, FinancialEventType.PAYABLE_SETTLEMENT) }
            .groupBy { requireNotNull(it.sourceObligationId) }
            .mapValues { entry -> entry.value.sumOf { it.amountRupiah } }
        return credit.map { candidate ->
            val reference = requireNotNull(candidate.sourceObligationId)
            ObligationBalance(
                id = reference,
                kind = if (candidate.type == FinancialEventType.CREDIT_SALE) ObligationKind.RECEIVABLE else ObligationKind.PAYABLE,
                originalAmountRupiah = candidate.amountRupiah,
                settledAmountRupiah = settledByObligation[reference] ?: 0L,
                dueDate = candidate.dueDate,
            )
        }
    }
}
