package com.mankeu.presentation

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.CategoryTotalRow
import com.mankeu.data.ProductCostComponentEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductWithComponents
import com.mankeu.data.ProjectEntity
import com.mankeu.domain.ActualMonthAnalysis
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostComponent
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.FixedCostBreakdown
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.PlanProduct
import com.mankeu.domain.ProductBep
import com.mankeu.domain.StockItem
import com.mankeu.presentation.components.ActualMonthCard
import com.mankeu.presentation.components.PlanFeasibilityCard
import com.mankeu.presentation.screens.EntryScreen
import com.mankeu.presentation.screens.HomeScreen
import com.mankeu.presentation.screens.PlanScreen
import com.mankeu.presentation.screens.ReportsScreen
import com.mankeu.presentation.screens.StockCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CardSimplificationTest {
    @get:Rule val compose = createComposeRule()

    private val sampleBusiness = BusinessEntity(
        id = "biz-1",
        name = "Usaha Tani Makmur",
        createdAtEpochMillis = 1000L,
    )
    private val sampleProject = ProjectEntity(
        id = "proj-1",
        businessId = "biz-1",
        name = "Kebun Jagung",
        isGeneral = false,
        createdAtEpochMillis = 1000L,
    )

    private val sampleSummary = FinancialSummary(
        operatingIncomeRupiah = 4_500_000L,
        operatingExpenseRupiah = 3_150_000L,
        profitRupiah = 1_350_000L,
        operatingCashFlowRupiah = 1_350_000L,
        financingCashFlowRupiah = 0L,
        cashBalanceRupiah = 5_000_000L,
        investingCashFlowRupiah = 0L,
    )

    private val sampleActualMonth = ActualMonthAnalysis(
        month = YearMonth.of(2026, 10),
        fixedCostRupiah = 500_000L,
        revenueRupiah = 2_000_000L,
        breakEvenRupiah = 1_500_000L,
        reached = true,
        unitsSold = emptyMap(),
        standardVariableCostRupiah = 600_000L,
        variableVarianceRupiah = 0L,
        totalCostRupiah = 1_100_000L,
        profitRupiah = 900_000L,
        revenueCostRatio = BigDecimal("1.82"),
        benefitCostRatio = BigDecimal("0.82"),
    )

    @Composable
    private fun IndonesianContent(content: @Composable () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val indonesian = remember {
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("id"))
            }
        }
        val localized = remember { context.createConfigurationContext(indonesian) }
        val host = LocalContext.current
        val wrapped = remember(host) {
            object : ContextWrapper(host) {
                override fun getResources(): Resources = localized.resources
            }
        }
        CompositionLocalProvider(
            LocalContext provides wrapped,
            LocalConfiguration provides indonesian,
        ) {
            MankeuTheme(motion = MankeuMotion.Off) {
                content()
            }
        }
    }

    @Test
    fun homeShowsCashBalanceAndProfitWithoutNeedingDisclosure() {
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.REALIZATION,
            summary = sampleSummary,
            actualMonth = sampleActualMonth,
        )

        compose.setContent {
            IndonesianContent {
                HomeScreen(
                    state = state,
                    onNavigate = {},
                    onReverse = { _, _ -> },
                    onOpenData = {},
                )
            }
        }

        // Headline numbers are visible immediately
        compose.onNodeWithText("Rp5.000.000").assertIsDisplayed()
        compose.onAllNodesWithText("Rp1.350.000").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Laba / rugi kas").assertIsDisplayed()
        compose.onAllNodesWithText("Seluruh catatan").onFirst().assertIsDisplayed()

        // Secondary cash details and help are hidden until disclosure requested
        compose.onNodeWithText("Laba atau rugi dari penerimaan dan pengeluaran kas operasional, belum memperhitungkan penyusutan aset.").assertDoesNotExist()

        // Click to expand calculation
        compose.onAllNodesWithText("Lihat perhitungan").onFirst().performClick()
        compose.onNodeWithText("Laba atau rugi dari penerimaan dan pengeluaran kas operasional, belum memperhitungkan penyusutan aset.").assertIsDisplayed()

        // Click to collapse
        compose.onAllNodesWithText("Sembunyikan perhitungan").onFirst().performClick()
        compose.onNodeWithText("Laba atau rugi dari penerimaan dan pengeluaran kas operasional, belum memperhitungkan penyusutan aset.").assertDoesNotExist()
    }

    @Test
    fun homeShowsDeficitBadgeWhenCashIsNegative() {
        val deficitSummary = sampleSummary.copy(
            cashBalanceRupiah = -750_000L,
            profitRupiah = -300_000L,
        )
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.REALIZATION,
            summary = deficitSummary,
        )

        compose.setContent {
            IndonesianContent {
                HomeScreen(
                    state = state,
                    onNavigate = {},
                    onReverse = { _, _ -> },
                    onOpenData = {},
                )
            }
        }

        // Negative numbers and Defisit badges are visible without disclosure
        compose.onNodeWithText("-Rp750.000").assertIsDisplayed()
        compose.onNodeWithText("-Rp300.000").assertIsDisplayed()
        compose.onAllNodesWithText("Defisit").onFirst().assertIsDisplayed()
    }

    @Test
    fun scopeSwitchResetsDisclosureState() {
        var state by mutableStateOf(
            AppUiState(
                isLoading = false,
                businesses = listOf(sampleBusiness),
                projects = listOf(sampleProject),
                selectedBusinessId = "biz-1",
                selectedProjectId = "proj-1",
                selectedBookMode = BookMode.REALIZATION,
                summary = sampleSummary,
            )
        )

        compose.setContent {
            IndonesianContent {
                HomeScreen(
                    state = state,
                    onNavigate = {},
                    onReverse = { _, _ -> },
                    onOpenData = {},
                )
            }
        }

        // Open details in unit 1
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Laba atau rugi dari penerimaan dan pengeluaran kas operasional, belum memperhitungkan penyusutan aset.").assertIsDisplayed()

        // Switch to unit 2
        compose.runOnIdle {
            state = state.copy(selectedProjectId = "proj-2")
        }

        // Details should be closed for new scope
        compose.onNodeWithText("Laba atau rugi dari penerimaan dan pengeluaran kas operasional, belum memperhitungkan penyusutan aset.").assertDoesNotExist()
        compose.onNodeWithText("Lihat perhitungan").assertIsDisplayed()
    }

    @Test
    fun expenseDistributionShowsTopThreeAndDisclosureForRemainder() {
        val totals = listOf(
            CategoryTotalRow("Pupuk & Nutrisi", 1_200_000L),
            CategoryTotalRow("Bibit & Benih", 900_000L),
            CategoryTotalRow("Tenaga Kerja Olah Lahan", 600_000L),
            CategoryTotalRow("Pestisida", 400_000L),
            CategoryTotalRow("Bahan Bakar Mesin", 150_000L),
        )
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.REALIZATION,
            categoryTotals = totals,
            summary = sampleSummary,
        )

        compose.setContent {
            IndonesianContent {
                HomeScreen(
                    state = state,
                    onNavigate = {},
                    onReverse = { _, _ -> },
                    onOpenData = {},
                )
            }
        }

        // Scroll if needed to reveal expense distribution
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Pupuk & Nutrisi"))

        // Total sum of all 5 is Rp3.250.000
        compose.onNodeWithText("Rp3.250.000").assertIsDisplayed()

        // Top 3 are visible immediately
        compose.onNodeWithText("Pupuk & Nutrisi").assertIsDisplayed()
        compose.onNodeWithText("Rp1.200.000").assertIsDisplayed()
        compose.onNodeWithText("Bibit & Benih").assertIsDisplayed()
        compose.onNodeWithText("Rp900.000").assertIsDisplayed()
        compose.onNodeWithText("Tenaga Kerja Olah Lahan").assertIsDisplayed()
        compose.onNodeWithText("Rp600.000").assertIsDisplayed()

        // 4th and 5th categories are NOT visible initially
        compose.onNodeWithText("Pestisida").assertDoesNotExist()
        compose.onNodeWithText("Bahan Bakar Mesin").assertDoesNotExist()

        // Expand all categories
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Lihat semua kategori"))
        compose.onNodeWithText("Lihat semua kategori").performClick()
        compose.onNodeWithText("Pestisida").assertIsDisplayed()
        compose.onNodeWithText("Rp400.000").assertIsDisplayed()
        compose.onNodeWithText("Bahan Bakar Mesin").assertIsDisplayed()
        compose.onNodeWithText("Rp150.000").assertIsDisplayed()

        // Collapse categories
        compose.onNodeWithText("Tampilkan lebih sedikit").performClick()
        compose.onNodeWithText("Pestisida").assertDoesNotExist()
    }

    @Test
    fun stockCardCompactAndFullModes() {
        val lowStockItem = StockItem(
            id = "item-1",
            businessId = "biz-1",
            name = "Pupuk Urea",
            unit = "kg",
            basePriceRupiah = 15_000L,
            currentStock = BigDecimal("3"),
            minAlertStock = BigDecimal("10"),
        )
        val normalStockItem = StockItem(
            id = "item-2",
            businessId = "biz-1",
            name = "Benih Jagung",
            unit = "bungkus",
            basePriceRupiah = 50_000L,
            currentStock = BigDecimal("20"),
            minAlertStock = BigDecimal("5"),
        )
        var isCompact by mutableStateOf(true)

        compose.setContent {
            IndonesianContent {
                StockCard(
                    items = listOf(lowStockItem, normalStockItem),
                    onManage = {},
                    compact = isCompact,
                )
            }
        }

        // In compact mode, low stock warning badge is shown, total inventory value is omitted
        compose.onNodeWithText("1 barang menipis").assertIsDisplayed()
        compose.onNodeWithText("Total nilai persediaan").assertDoesNotExist()

        // Toggle to full mode on Stock tab
        compose.runOnIdle { isCompact = false }

        // In full mode, inventory valuation is displayed
        compose.onNodeWithText("Total nilai persediaan").assertIsDisplayed()
        compose.onNodeWithText("Rp1.045.000").assertIsDisplayed() // 3*15000 + 20*50000 = 45000 + 1000000 = 1045000
    }

    @Test
    fun planFeasibilityCardShowsWarningsOutsideDisclosureAndDetailsInside() {
        val lossProduct = PlanProduct(
            id = "prod-1",
            name = "Kopi Susu",
            unit = "cup",
            priceRupiah = 10_000L,
            targetPerMonth = BigDecimal("100"),
            components = listOf(CostComponent("Bahan", 12_000L, BigDecimal.ONE)),
        )
        val productBep = ProductBep(
            product = lossProduct,
            variableCostPerUnit = 12_000L,
            contributionMargin = -2_000L,
            mixShare = BigDecimal.ONE,
            breakEvenUnits = null,
            breakEvenRupiah = null,
            sellsAtALoss = true,
        )
        val plan = PlanAnalysis(
            months = listOf(YearMonth.of(2026, 1)),
            fixedCost = FixedCostBreakdown(emptyList(), emptyList(), emptyList()),
            products = listOf(productBep),
            mixDefined = true,
            weightedContributionMargin = BigDecimal("-2000"),
            breakEvenUnits = null,
            breakEvenRupiah = null,
            revenueRupiah = 1_000_000L,
            variableCostRupiah = 1_200_000L,
            totalCostRupiah = 1_200_000L,
            profitRupiah = -200_000L,
            revenueCostRatio = BigDecimal("0.83"),
            benefitCostRatio = BigDecimal("-0.17"),
            breakEvenBaseRupiah = 0L,
            schedule = emptyList(),
            capitalNeedRupiah = 1_500_000L,
            plannedCapitalRupiah = 0L,
            capitalShortfallRupiah = 1_500_000L,
            summary = sampleSummary,
        )

        compose.setContent {
            IndonesianContent {
                PlanFeasibilityCard(plan = plan)
            }
        }

        // Critical warnings are visible WITHOUT opening disclosure
        compose.onNodeWithText("Kopi Susu dijual di bawah biaya variabelnya, jadi setiap penjualan menambah rugi.").assertIsDisplayed()
        compose.onNodeWithText("Perkiraan rugi Rp200.000 per bulan pada target penjualan.").assertIsDisplayed()
        compose.onNodeWithText("Kekurangan modal kerja Rp1.500.000.").assertIsDisplayed()

        // Details inside disclosure are hidden
        compose.onNodeWithText("R/C").assertDoesNotExist()

        // Expand disclosure
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Laba per bulan (pada target)").assertIsDisplayed()
        compose.onNodeWithText("Tutup bantuan").assertDoesNotExist()
    }

    @Test
    fun actualMonthCardShowsStatusAndWarningsWithDisclosure() {
        val deficitActual = sampleActualMonth.copy(
            revenueRupiah = 1_000_000L,
            breakEvenRupiah = 1_500_000L,
            reached = false,
            profitRupiah = -200_000L,
            variableVarianceRupiah = 50_000L,
            standardVariableCostRupiah = 400_000L,
        )

        compose.setContent {
            IndonesianContent {
                ActualMonthCard(actual = deficitActual, products = emptyList())
            }
        }

        // Headline status and warnings visible outside disclosure
        compose.onNodeWithText("Belum impas").assertIsDisplayed()
        compose.onNodeWithText("Rugi setelah penyusutan Rp200.000 pada Oktober 2026.").assertIsDisplayed()
        compose.onNodeWithText("Kurang Rp500.000 lagi untuk mencapai titik impas (Rp1.500.000).").assertIsDisplayed()

        // Detailed rows inside disclosure are hidden
        compose.onNodeWithText("Biaya tetap dan penyusutan").assertDoesNotExist()

        // Expand disclosure
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Biaya tetap dan penyusutan").assertIsDisplayed()
    }

    @Test
    fun planProductRowDisplaysEconomicsAndLossWarning() {
        val productNormal = ProductWithComponents(
            product = ProductEntity(
                id = "prod-1",
                projectId = "proj-1",
                name = "Kopi Susu",
                unit = "cup",
                priceRupiah = 15_000L,
                createdAtEpochMillis = 1000L,
            ),
            components = listOf(
                ProductCostComponentEntity(
                    id = "c-1",
                    productId = "prod-1",
                    name = "Bahan",
                    amountRupiah = 12_000L,
                    yieldText = "1",
                )
            ),
        )
        val productLoss = ProductWithComponents(
            product = ProductEntity(
                id = "prod-2",
                projectId = "proj-1",
                name = "Kopi Rugi",
                unit = "cup",
                priceRupiah = 10_000L,
                createdAtEpochMillis = 1000L,
            ),
            components = listOf(
                ProductCostComponentEntity(
                    id = "c-2",
                    productId = "prod-2",
                    name = "Bahan",
                    amountRupiah = 12_000L,
                    yieldText = "1",
                )
            ),
        )
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.PROJECTION,
            products = listOf(productNormal, productLoss),
        )

        compose.setContent {
            IndonesianContent {
                PlanScreen(
                    state = state,
                    onSaveProduct = { _, _, _, _, _, _, _ -> },
                    onArchiveProduct = {},
                    onSavePlanLine = { _, _, _, _, _, _, _, _ -> },
                    onArchivePlanLine = {},
                    onSetPeriod = { _, _ -> },
                    onOpenData = {},
                )
            }
        }

        compose.onNodeWithText("Kopi Susu").assertIsDisplayed()
        compose.onAllNodesWithText("Biaya variabel Rp12.000 / cup").assertCountEquals(2)
        compose.onNodeWithText("Margin kontribusi Rp3.000 / cup").assertIsDisplayed()

        compose.onNodeWithText("Kopi Rugi").assertIsDisplayed()
        compose.onNodeWithText("Margin kontribusi -Rp2.000 / cup · Margin tidak positif").assertIsDisplayed()
    }

    @Test
    fun entryFormGuidanceDisclosure() {
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.REALIZATION,
            categories = listOf(
                CategoryEntity(
                    id = "cat-inc",
                    businessId = "biz-1",
                    role = CategoryRole.REVENUE.name,
                    name = "Penjualan",
                    normalizedName = "penjualan",
                    createdAtEpochMillis = 1000L,
                ),
            ),
        )

        compose.setContent {
            IndonesianContent {
                EntryScreen(
                    state = state,
                    onRecord = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onReverse = { _, _ -> },
                    onDelete = {},
                    onOpenData = {},
                    requestedKind = FinancialEventType.CASH_INCOME,
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val showLabel = context.getString(R.string.form_guidance_show)
        val hideLabel = context.getString(R.string.form_guidance_hide)
        val helpText = context.getString(R.string.kind_cash_in_help)

        // Guidance paragraph is hidden initially
        compose.onNodeWithText(helpText).assertDoesNotExist()

        // Open guidance
        compose.onNodeWithText(showLabel).performClick()
        compose.onNodeWithText(helpText).assertIsDisplayed()

        // Close guidance
        compose.onNodeWithText(hideLabel).performClick()
        compose.onNodeWithText(helpText).assertDoesNotExist()
    }

    @Test
    fun reportsFinancialsSimplificationWithDisclosure() {
        val state = AppUiState(
            isLoading = false,
            businesses = listOf(sampleBusiness),
            projects = listOf(sampleProject),
            selectedBusinessId = "biz-1",
            selectedProjectId = "proj-1",
            selectedBookMode = BookMode.REALIZATION,
            eventCount = 1,
            summary = sampleSummary,
        )

        compose.setContent {
            IndonesianContent {
                ReportsScreen(state = state)
            }
        }

        // Headline cash profit is visible
        compose.onNodeWithText("Laba / rugi kas").assertIsDisplayed()
        compose.onAllNodesWithText("Rp1.350.000").onFirst().assertIsDisplayed()

        // Detailed income note is hidden
        compose.onNodeWithText("Pendapatan dan biaya dihitung murni dari transaksi operasional usaha.").assertDoesNotExist()

        // Open disclosure
        compose.onNodeWithText("Lihat perhitungan").performClick()
        compose.onNodeWithText("Pendapatan dan biaya dihitung murni dari transaksi operasional usaha.").assertIsDisplayed()
    }
}
