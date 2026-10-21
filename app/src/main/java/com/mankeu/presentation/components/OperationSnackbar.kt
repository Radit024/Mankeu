package com.mankeu.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.UiText

enum class OperationKind { INFO, SUCCESS, ERROR }

data class OperationSnackbarVisuals(
    override val message: String,
    val kind: OperationKind,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

/** Classify resource IDs, never translated prose. An instruction must not receive a success tick. */
fun UiText.operationKind(): OperationKind = when {
    isError -> OperationKind.ERROR
    this is UiText.Res && id in CONFIRMED_MESSAGES -> OperationKind.SUCCESS
    else -> OperationKind.INFO
}

private val CONFIRMED_MESSAGES = setOf(
    R.string.msg_business_created, R.string.msg_project_created,
    R.string.msg_business_renamed, R.string.msg_project_renamed, R.string.msg_project_updated,
    R.string.msg_business_archived, R.string.msg_project_archived,
    R.string.msg_business_deleted, R.string.msg_project_deleted,
    R.string.msg_category_added, R.string.msg_category_archived,
    R.string.msg_event_saved, R.string.msg_event_deleted, R.string.msg_event_reversed,
    R.string.msg_plan_saved, R.string.msg_plan_removed, R.string.msg_product_saved,
    R.string.msg_stock_item_added, R.string.msg_stock_movement_saved,
    R.string.msg_batch_reversed, R.string.msg_batch_reversed_period,
    R.string.msg_sign_in_ok, R.string.msg_sign_out_ok,
)

@Composable
fun OperationSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { OperationSnackbar(it) }
}

/** Keep Material's message, action/dismiss callbacks and host timeout/accessibility behaviour. */
@Composable
fun OperationSnackbar(data: SnackbarData) {
    val kind = (data.visuals as? OperationSnackbarVisuals)?.kind ?: OperationKind.INFO
    val scheme = MaterialTheme.colorScheme
    Snackbar(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        containerColor = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        action = data.visuals.actionLabel?.let { label ->
            { TextButton(onClick = data::performAction) { Text(label, color = scheme.primary) } }
        },
        dismissAction = if (data.visuals.withDismissAction) {
            {
                IconButton(onClick = data::dismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
        } else null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (kind == OperationKind.SUCCESS) {
                StateIllustration(StateArtwork.SUCCESS, Modifier.size(32.dp))
            } else {
                Icon(
                    if (kind == OperationKind.ERROR) Icons.Default.ErrorOutline else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (kind == OperationKind.ERROR) scheme.error else scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
