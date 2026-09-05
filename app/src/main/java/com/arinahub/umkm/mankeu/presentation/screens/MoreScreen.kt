package com.arinahub.umkm.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arinahub.umkm.mankeu.R
import com.arinahub.umkm.mankeu.backup.BackupPreview
import com.arinahub.umkm.mankeu.importer.WorkbookImportPreview
import com.arinahub.umkm.mankeu.data.CategoryEntity
import com.arinahub.umkm.mankeu.data.ImportBatchEntity
import com.arinahub.umkm.mankeu.data.UnitEntity
import com.arinahub.umkm.mankeu.domain.BookMode
import com.arinahub.umkm.mankeu.domain.CategoryRole
import com.arinahub.umkm.mankeu.presentation.AppUiState
import com.arinahub.umkm.mankeu.presentation.appLocale
import com.arinahub.umkm.mankeu.presentation.ChoiceMenu
import com.arinahub.umkm.mankeu.presentation.BusinessWords
import com.arinahub.umkm.mankeu.presentation.ImportUiState
import com.arinahub.umkm.mankeu.presentation.LocalArinaSemanticColors
import com.arinahub.umkm.mankeu.presentation.TransferUiState
import com.arinahub.umkm.mankeu.presentation.resolve
import com.arinahub.umkm.mankeu.presentation.components.ArinaSheet
import com.arinahub.umkm.mankeu.presentation.components.NoticeCard
import com.arinahub.umkm.mankeu.presentation.components.ReportRow
import com.arinahub.umkm.mankeu.presentation.components.SectionTitle
import com.arinahub.umkm.mankeu.presentation.components.TourTarget
import com.arinahub.umkm.mankeu.presentation.components.tourTarget
import com.arinahub.umkm.mankeu.presentation.toRupiah
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.arinahub.umkm.mankeu.auth.AuthUser
import com.arinahub.umkm.mankeu.backup.cloud.DriveBackupFile

private enum class BackupDialogMode {
    CHOOSE,
    CREATE_LOCAL,
    RESTORE_LOCAL,
    CREATE_CLOUD,
    RESTORE_CLOUD,
}

/**
 * Berkas dan Data: bringing a season in from a spreadsheet, saving records out, encrypted backup,
 * plus the farm/field and category/unit administration those tasks depend on.
 *
 * Split away from Pengaturan in design round 3. Import/export used to sit inside settings, which
 * the owner flagged as misplaced — importing a workbook is work you do *with* your records, not
 * a preference you configure. Appearance/language/motion/account now live in [SettingsScreen].
 */
@Composable
fun DataScreen(
    state: AppUiState,
    importState: ImportUiState,
    transferState: TransferUiState,
    onCreateCategory: (String, CategoryRole) -> Unit,
    onArchiveCategory: (String) -> Unit,
    onCreateUnit: (String, String) -> Unit,
    onArchiveUnit: (String) -> Unit,
    onChooseWorkbook: () -> Unit,
    onCommitImport: (BookMode) -> Unit,
    onReverseImportBatch: (String) -> Unit,
    onExportWorkbook: () -> Unit,
    onExportPdf: () -> Unit,
    onExportTemplate: () -> Unit = {},
    onCreateBackup: (String) -> Unit,
    onRestoreBackup: (String) -> Unit,
    recoveryAvailable: Boolean,
    onExportRecovery: () -> Unit,
    onManageScope: () -> Unit,
    authUser: AuthUser?,
    driveBackups: List<DriveBackupFile>,
    isDriveLoading: Boolean,
    onFetchDriveBackups: () -> Unit,
    onBackupToDrive: (String) -> Unit,
    onRestoreDriveBackup: (String, String) -> Unit,
    onSignInGoogle: () -> Unit,
    onResetImport: () -> Unit = {},
    onStartTour: (() -> Unit)? = null,
) {
    var masterDataDialog by rememberSaveable { mutableStateOf(false) }
    var backupDialog by rememberSaveable { mutableStateOf(false) }
    val transferWorking = transferState is TransferUiState.Working
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(
                    title = stringResource(R.string.data_title),
                    subtitle = stringResource(R.string.data_subtitle),
                    modifier = Modifier.weight(1f),
                )
                if (onStartTour != null) {
                    TextButton(onClick = onStartTour) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_learn_page), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item { ImportCard(importState, state.selectedBookMode, onChooseWorkbook, onCommitImport, onResetImport) }
        // Add/rename used to live here as four cramped buttons that could only touch the *active*
        // farm and field. Everything scope-shaped now lives on one screen that can see the whole
        // tree; this card is just the way in.
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.manage_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            stringResource(R.string.data_manage_count, state.businesses.size, state.projects.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = onManageScope) { Text(stringResource(R.string.action_manage)) }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.manage_categories), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.data_master_count, state.categories.size, state.units.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { masterDataDialog = true }) { Text(stringResource(R.string.action_manage)) }
                }
            }
        }
        if (state.importBatches.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.data_import_history), stringResource(R.string.data_import_history_sub)) }
            items(state.importBatches, key = { it.id }) { batch -> ImportBatchCard(batch, onReverseImportBatch) }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.DATA_EXPORT), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.data_export_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.data_export_sub), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onExportWorkbook, enabled = !transferWorking, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Download, null); Text(" Excel") }
                        OutlinedButton(onClick = onExportPdf, enabled = !transferWorking, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PictureAsPdf, null); Text(" PDF") }
                    }
                    OutlinedButton(onClick = onExportTemplate, enabled = !transferWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, null)
                        Text(" " + stringResource(R.string.data_export_template))
                    }
                    OutlinedButton(onClick = { backupDialog = true }, enabled = !transferWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Backup, null)
                        Text(" " + stringResource(R.string.data_export_backup))
                    }
                    val semantic = LocalArinaSemanticColors.current
                    when (transferState) {
                        TransferUiState.Idle -> Text(stringResource(R.string.data_export_idle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is TransferUiState.Working -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(transferState.label.resolve()) }
                        is TransferUiState.Success -> Text(transferState.message.resolve(), color = semantic.success, fontWeight = FontWeight.Bold)
                        is TransferUiState.Failure -> Text(transferState.message.resolve(), color = semantic.danger, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (state.notices.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.data_notice_title), stringResource(R.string.data_notice_sub)) }
            items(state.notices, key = { it.id }) { notice ->
                NoticeCard(
                    notice.title,
                    stringResource(R.string.data_notice_source, notice.explanation, notice.sourceEvidence),
                    Icons.Default.Notifications,
                )
            }
        }
    }
    if (masterDataDialog) MasterDataDialog(
        categories = state.categories,
        units = state.units,
        onDismiss = { masterDataDialog = false },
        onCreateCategory = onCreateCategory,
        onArchiveCategory = onArchiveCategory,
        onCreateUnit = onCreateUnit,
        onArchiveUnit = onArchiveUnit,
    )
    if (backupDialog) BackupDialog(
        onDismiss = { backupDialog = false },
        onCreate = { password -> backupDialog = false; onCreateBackup(password) },
        onRestore = { password -> backupDialog = false; onRestoreBackup(password) },
        recoveryAvailable = recoveryAvailable,
        onExportRecovery = { backupDialog = false; onExportRecovery() },
        authUser = authUser,
        driveBackups = driveBackups,
        isDriveLoading = isDriveLoading,
        onFetchDriveBackups = onFetchDriveBackups,
        onBackupToDrive = { password -> backupDialog = false; onBackupToDrive(password) },
        onRestoreDriveBackup = { fileId, password -> backupDialog = false; onRestoreDriveBackup(fileId, password) },
        onSignInGoogle = onSignInGoogle,
    )
}

@Composable
fun RestorePreviewDialog(
    preview: BackupPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val semantic = LocalArinaSemanticColors.current
    val created = Instant.ofEpochMilli(preview.createdAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", appLocale()))
    ArinaSheet(
        title = stringResource(R.string.restore_title),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.restore_confirm),
        destructive = true,
        onConfirm = onConfirm,
    ) {
        Text(stringResource(R.string.restore_verified))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.restore_created))
            Text(created, fontWeight = FontWeight.Black)
        }
        ReportRow(stringResource(R.string.restore_schema), preview.schemaVersion.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_tables), preview.tableCount.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_businesses), preview.businessCount.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_projects), preview.projectCount.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_budget_items), preview.budgetItemCount.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_events), preview.financialEventCount.toLong(), asMoney = false)
        ReportRow(stringResource(R.string.restore_rows), preview.rowCount.toLong(), asMoney = false)
        Text(
            stringResource(R.string.restore_warning),
            color = semantic.danger,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ImportCard(
    importState: ImportUiState,
    currentBookMode: BookMode,
    onChooseWorkbook: () -> Unit,
    onCommit: (BookMode) -> Unit,
    onResetImport: () -> Unit,
) {
    val semantic = LocalArinaSemanticColors.current
    // Defaults to the book the user is already looking at, which is nearly always what they mean —
    // the same awareness the web prototype's import dialog has. Still a visible, changeable control,
    // because "which book did that land in" is not a question anyone should have to answer by
    // reading a report afterwards.
    var target by rememberSaveable(currentBookMode) { mutableStateOf(currentBookMode) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.import_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.import_sub), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (importState) {
                ImportUiState.Idle -> Button(
                    onClick = onChooseWorkbook,
                    modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.IMPORT_FILE_PICKER),
                ) { Text(stringResource(R.string.import_pick)) }
                ImportUiState.Loading -> {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.import_loading), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = onResetImport) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                is ImportUiState.Failure -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.danger.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = semantic.danger)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                importState.message.resolve(),
                                color = semantic.danger,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onChooseWorkbook, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.import_pick_other))
                        }
                        OutlinedButton(onClick = onResetImport) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
                is ImportUiState.Committed -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.success.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = semantic.success)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.import_done, importState.count, importState.enrichmentCount),
                                color = semantic.success,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onChooseWorkbook, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.import_pick_another_workbook))
                        }
                        OutlinedButton(onClick = onResetImport) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
                is ImportUiState.Ready -> {
                    val preview = importState.preview
                    Text(
                        stringResource(
                            R.string.import_sheets,
                            preview.recognizedSheets.joinToString().ifBlank { stringResource(R.string.import_sheets_none) },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ReportRow(stringResource(R.string.import_ready_transactions), preview.candidates.size.toLong(), asMoney = false)
                    if (preview.budgetCandidates.isNotEmpty()) {
                        ReportRow(stringResource(R.string.import_ready_budget), preview.budgetCandidates.size.toLong(), asMoney = false)
                    }
                    ReportRow(stringResource(R.string.import_ready_enriched), preview.enrichmentCount.toLong(), asMoney = false)
                    ReportRow(stringResource(R.string.import_total_income), preview.totalIncomeRupiah)
                    ReportRow(stringResource(R.string.import_total_expense), preview.totalExpenseRupiah)
                    preview.candidates.filter { it.inferredFields.isNotEmpty() }.take(3).forEach { candidate ->
                        NoticeCard(
                            "${candidate.description} • ${candidate.amountRupiah.toRupiah()}",
                            stringResource(R.string.data_notice_source, candidate.rationale, candidate.sourceEvidence.joinToString()),
                            Icons.Default.Info,
                        )
                    }
                    preview.warnings.forEach { NoticeCard(stringResource(R.string.import_review), it, Icons.Default.Info) }
                    preview.errors.forEach { Text(it, color = semantic.danger, fontWeight = FontWeight.Bold) }
                    if (preview.carriesBookMode) {
                        Text(
                            stringResource(R.string.import_target_from_file),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(stringResource(R.string.import_target_label), fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow(
                            Modifier.fillMaxWidth().tourTarget(TourTarget.IMPORT_TARGET_BOOK),
                        ) {
                            BookMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = target == mode,
                                    onClick = { target = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = BookMode.entries.size),
                                    label = { Text(stringResource(if (mode == BookMode.PROJECTION) R.string.scope_mode_plan else R.string.scope_mode_actual)) },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { onCommit(target) },
                        enabled = preview.canCommit,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.import_commit, preview.candidates.size + preview.budgetCandidates.size)) }
                    OutlinedButton(onClick = onChooseWorkbook, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_pick_another_workbook)) }
                }
            }
        }
    }
}

@Composable
private fun MasterDataDialog(
    categories: List<CategoryEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onCreateCategory: (String, CategoryRole) -> Unit,
    onArchiveCategory: (String) -> Unit,
    onCreateUnit: (String, String) -> Unit,
    onArchiveUnit: (String) -> Unit,
) {
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryRole by rememberSaveable { mutableStateOf(CategoryRole.EXPENSE) }
    var unitName by rememberSaveable { mutableStateOf("") }
    var unitSymbol by rememberSaveable { mutableStateOf("") }
    ArinaSheet(
        title = stringResource(R.string.manage_categories),
        onDismiss = onDismiss,
        confirmLabel = null,
        dismissLabel = stringResource(R.string.action_done),
    ) {
        Text(stringResource(R.string.master_add_category), fontWeight = FontWeight.Black)
        OutlinedTextField(
            categoryName,
            { categoryName = it },
            label = { Text(stringResource(R.string.master_category_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceMenu(
            label = stringResource(BusinessWords.roleLabel(categoryRole)),
            options = CategoryRole.entries.map { it.name to stringResource(BusinessWords.roleLabel(it)) },
            onSelected = { categoryRole = CategoryRole.valueOf(it) },
        )
        Button(
            onClick = { onCreateCategory(categoryName, categoryRole); categoryName = "" },
            enabled = categoryName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.master_add_category)) }
        categories.forEach { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.name, fontWeight = FontWeight.Bold)
                    // Was `category.role` — that rendered the raw enum name ("REVENUE",
                    // "EXPENSE") straight to the screen, which is exactly the jargon the
                    // owner flagged. Goes through BusinessWords now.
                    Text(
                        stringResource(BusinessWords.roleLabel(category.role)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onArchiveCategory(category.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.master_archive, category.name))
                }
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.master_add_unit), fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                unitName,
                { unitName = it },
                label = { Text(stringResource(R.string.master_unit_name)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                unitSymbol,
                { unitSymbol = it },
                label = { Text(stringResource(R.string.master_unit_symbol)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = { onCreateUnit(unitName, unitSymbol); unitName = ""; unitSymbol = "" },
            enabled = unitName.isNotBlank() && unitSymbol.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.master_add_unit)) }
        units.forEach { unit ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${unit.name} (${unit.symbol})", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton(onClick = { onArchiveUnit(unit.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.master_archive, unit.name))
                }
            }
        }
    }
}

@Composable
private fun ImportBatchCard(batch: ImportBatchEntity, onReverse: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(batch.sourceName, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.batch_summary, batch.recordCount, batch.totalAmountRupiah.toRupiah(), batch.state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (batch.state == "COMMITTED") TextButton(onClick = { confirm = true }) { Text(stringResource(R.string.batch_reverse)) }
        }
    }
    if (confirm) {
        ArinaSheet(
            title = stringResource(R.string.batch_reverse_title),
            onDismiss = { confirm = false },
            confirmLabel = stringResource(R.string.batch_reverse),
            destructive = true,
            onConfirm = { confirm = false; onReverse(batch.id) },
        ) {
            Text(stringResource(R.string.batch_reverse_body, batch.recordCount, batch.sourceName))
        }
    }
}

@Composable
private fun BackupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRestore: (String) -> Unit,
    recoveryAvailable: Boolean,
    onExportRecovery: () -> Unit,
    authUser: AuthUser?,
    driveBackups: List<DriveBackupFile>,
    isDriveLoading: Boolean,
    onFetchDriveBackups: () -> Unit,
    onBackupToDrive: (String) -> Unit,
    onRestoreDriveBackup: (String, String) -> Unit,
    onSignInGoogle: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(BackupDialogMode.CHOOSE) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var selectedFileId by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val errShort = stringResource(R.string.backup_err_short)
    val errMismatch = stringResource(R.string.backup_err_mismatch)

    val sheetTitle = when (mode) {
        BackupDialogMode.CHOOSE -> stringResource(R.string.backup_title)
        BackupDialogMode.CREATE_LOCAL -> stringResource(R.string.backup_create_title)
        BackupDialogMode.RESTORE_LOCAL -> stringResource(R.string.backup_restore_title)
        BackupDialogMode.CREATE_CLOUD -> stringResource(R.string.data_export_drive_backup)
        BackupDialogMode.RESTORE_CLOUD -> stringResource(R.string.data_export_drive_restore)
    }

    val confirmLabel = when (mode) {
        BackupDialogMode.CHOOSE -> null
        BackupDialogMode.CREATE_LOCAL -> stringResource(R.string.backup_pick_location)
        BackupDialogMode.RESTORE_LOCAL -> stringResource(R.string.backup_pick_file)
        BackupDialogMode.CREATE_CLOUD -> "Unggah ke Drive"
        BackupDialogMode.RESTORE_CLOUD -> "Unduh & Pulihkan"
    }

    ArinaSheet(
        title = sheetTitle,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        confirmEnabled = if (mode == BackupDialogMode.RESTORE_CLOUD) selectedFileId.isNotBlank() && password.length >= 8 else true,
        dismissLabel = stringResource(if (mode == BackupDialogMode.CHOOSE) R.string.action_close else R.string.action_back_step),
        onDismissClick = {
            if (mode == BackupDialogMode.CHOOSE) {
                onDismiss()
            } else {
                mode = BackupDialogMode.CHOOSE
                password = ""
                confirmation = ""
                selectedFileId = ""
                error = null
            }
        },
        onConfirm = {
            error = when {
                password.length < 8 -> errShort
                (mode == BackupDialogMode.CREATE_LOCAL || mode == BackupDialogMode.CREATE_CLOUD) && password != confirmation -> errMismatch
                mode == BackupDialogMode.RESTORE_CLOUD && selectedFileId.isBlank() -> "Pilih berkas cadangan terlebih dahulu."
                else -> null
            }
            if (error == null) {
                when (mode) {
                    BackupDialogMode.CREATE_LOCAL -> onCreate(password)
                    BackupDialogMode.RESTORE_LOCAL -> onRestore(password)
                    BackupDialogMode.CREATE_CLOUD -> onBackupToDrive(password)
                    BackupDialogMode.RESTORE_CLOUD -> onRestoreDriveBackup(selectedFileId, password)
                    BackupDialogMode.CHOOSE -> {}
                }
            }
        },
    ) {
        when (mode) {
            BackupDialogMode.CHOOSE -> {
                Text(stringResource(R.string.backup_choose_body))
                Button(onClick = { mode = BackupDialogMode.CREATE_LOCAL }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_create_cta))
                }
                OutlinedButton(onClick = { mode = BackupDialogMode.RESTORE_LOCAL }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_restore_cta))
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Pencadangan Google Drive", fontWeight = FontWeight.Bold)

                if (authUser != null) {
                    FilledTonalButton(onClick = { mode = BackupDialogMode.CREATE_CLOUD }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.data_export_drive_backup))
                    }
                    OutlinedButton(
                        onClick = {
                            onFetchDriveBackups()
                            mode = BackupDialogMode.RESTORE_CLOUD
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.data_export_drive_restore))
                    }
                } else {
                    Text(
                        stringResource(R.string.data_drive_sign_in_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onSignInGoogle()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.account_sign_in_google))
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                OutlinedButton(
                    onClick = onExportRecovery,
                    enabled = recoveryAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.backup_export_recovery)) }
                Text(
                    stringResource(
                        if (recoveryAvailable) R.string.backup_recovery_available
                        else R.string.backup_recovery_none,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BackupDialogMode.CREATE_LOCAL, BackupDialogMode.CREATE_CLOUD -> {
                Text(
                    stringResource(
                        if (mode == BackupDialogMode.CREATE_CLOUD) R.string.account_drive_note
                        else R.string.backup_create_hint,
                    ),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; error = null },
                    label = { Text(stringResource(R.string.backup_password_repeat)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            BackupDialogMode.RESTORE_LOCAL -> {
                Text(stringResource(R.string.backup_restore_hint))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            BackupDialogMode.RESTORE_CLOUD -> {
                Text(stringResource(R.string.data_drive_select_backup), fontWeight = FontWeight.Bold)
                if (isDriveLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (driveBackups.isEmpty()) {
                    Text(
                        stringResource(R.string.data_drive_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        driveBackups.forEach { file ->
                            val formattedTime = Instant.ofEpochMilli(file.createdTimeEpochMillis)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", appLocale()))
                            val isSelected = selectedFileId == file.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedFileId = file.id },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedFileId = file.id },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "$formattedTime • ${file.sizeBytes / 1024} KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.backup_restore_hint))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun NameDialog(title: String, label: String, onDismiss: () -> Unit, initialValue: String = "", onSave: (String) -> Unit) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    ArinaSheet(
        title = title,
        onDismiss = onDismiss,
        confirmEnabled = value.isNotBlank(),
        onConfirm = { onSave(value) },
    ) {
        OutlinedTextField(
            value,
            { value = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
