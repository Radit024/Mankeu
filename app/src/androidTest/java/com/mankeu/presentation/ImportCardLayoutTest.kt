package com.mankeu.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.ImportReview
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.presentation.screens.ImportCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ImportCardLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun importCardReadyRowsAreArrangedVerticallyWithoutOverlap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preview = WorkbookImportPreview(
            sourceName = "simulasi.xlsx",
            sourceSha256 = "ui-fixture",
            transactions = listOf(
                ImportTransaction(
                    source = "Transaksi",
                    date = LocalDate.now(),
                    description = "Penjualan",
                    categoryName = "Penjualan",
                    incoming = true,
                    amountRupiah = 50000,
                )
            ),
        )
        val review = ImportReview(
            newCategories = emptyList(),
            conflicts = emptyList(),
            existingStockNames = emptyList(),
            previouslyImported = false,
            addedRowCount = 1,
        )
        val importState = ImportUiState.Ready(preview, review)
        var commitCount = 0

        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                ImportCard(
                    importState = importState,
                    onChooseWorkbook = {},
                    onCommit = { commitCount++ },
                    onResetImport = {},
                    onDownloadTemplate = {},
                )
            }
        }

        val transactionLabel = context.getString(R.string.import_ready_transactions)
        val budgetLabel = context.getString(R.string.import_ready_budget)
        val stockLabel = context.getString(R.string.import_ready_stock)
        val productsLabel = context.getString(R.string.import_ready_products)
        val incomeLabel = context.getString(R.string.import_total_income)
        val expenseLabel = context.getString(R.string.import_total_expense)
        val commitLabel = context.getString(R.string.import_commit, 1)

        val bounds = listOf(
            compose.onNodeWithText(transactionLabel).getBoundsInRoot(),
            compose.onNodeWithText(budgetLabel).getBoundsInRoot(),
            compose.onNodeWithText(stockLabel).getBoundsInRoot(),
            compose.onNodeWithText(productsLabel).getBoundsInRoot(),
            compose.onNodeWithText(incomeLabel).getBoundsInRoot(),
            compose.onNodeWithText(expenseLabel).getBoundsInRoot(),
        )

        bounds.zipWithNext().forEach { (previous, next) ->
            assertTrue("Import rows overlap: prev bottom=${previous.bottom}, next top=${next.top}", next.top >= previous.bottom)
        }

        val commitBounds = compose.onNodeWithText(commitLabel).getBoundsInRoot()
        assertTrue("Commit action overlaps summary: commit top=${commitBounds.top}, last bottom=${bounds.last().bottom}", commitBounds.top >= bounds.last().bottom)

        compose.onNodeWithText(commitLabel).performClick()
        assertEquals("Commit callback should be called once", 1, commitCount)
    }

    @Test
    fun previouslyImportedWithConflictsDisablesCommitAndShowsWarnings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val conflictMessage = "Produk Kopi Susu mempunyai resep berbeda"
        val preview = WorkbookImportPreview(
            sourceName = "simulasi_konflik.xlsx",
            sourceSha256 = "conflict-fixture",
            transactions = listOf(
                ImportTransaction(
                    source = "Transaksi",
                    date = LocalDate.now(),
                    description = "Penjualan",
                    categoryName = "Penjualan",
                    incoming = true,
                    amountRupiah = 50000,
                )
            ),
        )
        val review = ImportReview(
            newCategories = emptyList(),
            conflicts = listOf(conflictMessage),
            existingStockNames = emptyList(),
            previouslyImported = true,
            addedRowCount = 1,
        )
        val importState = ImportUiState.Ready(preview, review)
        var commitCount = 0

        compose.setContent {
            MankeuTheme(motion = MankeuMotion.Off) {
                ImportCard(
                    importState = importState,
                    onChooseWorkbook = {},
                    onCommit = { commitCount++ },
                    onResetImport = {},
                    onDownloadTemplate = {},
                )
            }
        }

        // Source workbook name is displayed
        compose.onNodeWithText("simulasi_konflik.xlsx").assertIsDisplayed()

        // Duplicate warning is visible
        val duplicateTitle = context.getString(R.string.import_duplicate_title)
        val duplicateBody = context.getString(R.string.import_duplicate_body)
        compose.onNodeWithText(duplicateTitle).assertIsDisplayed()
        compose.onNodeWithText(duplicateBody).assertIsDisplayed()

        // Conflicts are visible
        compose.onNodeWithText(conflictMessage).assertIsDisplayed()

        // Commit button is disabled and clicking it does not call onCommit
        val commitLabel = context.getString(R.string.import_commit, 1)
        compose.onNodeWithText(commitLabel).assertIsNotEnabled()
        compose.onNodeWithText(commitLabel).performClick()
        assertEquals("Commit callback should not be called when conflicts exist", 0, commitCount)
    }
}
