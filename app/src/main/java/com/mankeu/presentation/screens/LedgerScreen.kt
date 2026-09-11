package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.EventCard
import com.mankeu.presentation.components.NoticeCard
import com.mankeu.presentation.components.SectionTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

/**
 * Riwayat: the book of what has been recorded, searchable and filterable.
 *
 * This used to be the right-hand half of a two-segment switcher inside Catat, which put a
 * *browsing* surface behind a control on an *entry* surface. The two are different mental models —
 * one is "I am adding something", the other is "I am looking something up" — and the cost of
 * conflating them was that the ledger, with its search and its filters, was invisible to anyone
 * who had not thought to press the second segment of a control at the top of a form (D-070).
 *
 * It is now its own destination, so the bottom bar reads as what the app actually does: see the
 * business (Beranda), add to it (Catat), look through it (Riwayat), analyse it (Laporan).
 */
@Composable
fun LedgerScreen(
    state: AppUiState,
    onReverse: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRecord: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf("ALL") }

    val visibleEvents = state.events.filter { event ->
        val matchesText = search.isBlank() ||
            listOf(event.description, event.category, event.counterparty, event.reference)
                .filterNotNull()
                .any { it.contains(search, ignoreCase = true) }
        val incoming = event.type in setOf(
            FinancialEventType.CASH_INCOME,
            FinancialEventType.CREDIT_SALE,
            FinancialEventType.RECEIVABLE_COLLECTION,
            FinancialEventType.OWNER_CAPITAL,
            FinancialEventType.LOAN_RECEIVED,
        )
        // "Belum Lunas" is the question an owner actually asks of a ledger — who still owes me,
        // and who am I still owing. It reads the obligation's real outstanding balance rather than
        // the event type, so a credit sale that has since been collected drops off the filter.
        val unsettled = event.obligationId != null &&
            state.obligations.any { it.id == event.obligationId && it.outstandingAmountRupiah > 0 }
        matchesText && when (direction) {
            "IN" -> incoming
            "OUT" -> !incoming
            "UNPAID" -> unsettled
            else -> true
        }
    }

    LazyColumn(
        // bottom clears the speed dial; see HomeScreen for why the list needs it itself.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                title = stringResource(R.string.ledger_title),
                subtitle = stringResource(R.string.ledger_subtitle, state.eventCount),
            )
        }

        if (state.eventCount > 0) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                val filters = listOf(
                    "ALL" to stringResource(R.string.money_all),
                    "IN" to stringResource(R.string.money_in_short),
                    "OUT" to stringResource(R.string.money_out_short),
                    "UNPAID" to stringResource(R.string.money_unpaid_short),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (value, label) ->
                        FilterChip(
                            selected = direction == value,
                            onClick = { direction = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        when {
            state.eventCount == 0 -> item {
                EmptyCard(
                    stringResource(R.string.home_empty_title),
                    stringResource(R.string.home_empty_body),
                    stringResource(R.string.home_empty_action),
                    onRecord,
                )
            }

            visibleEvents.isEmpty() -> item {
                EmptyCard(
                    stringResource(R.string.home_no_match_title),
                    stringResource(R.string.home_no_match_body),
                )
            }

            else -> {
                items(visibleEvents, key = { it.id }) { event ->
                    EventCard(event = event, onReverse = onReverse, onDelete = onDelete)
                }
                // The list is capped for memory (D-068). Saying so is better than letting an owner
                // scroll to the bottom of a long book and quietly conclude the rest is gone.
                if (state.eventCount > state.events.size) {
                    item {
                        NoticeCard(
                            title = stringResource(R.string.ledger_truncated_title),
                            body = stringResource(
                                R.string.ledger_truncated_body,
                                state.events.size,
                                state.eventCount,
                            ),
                            icon = Icons.Default.Info,
                        )
                    }
                }
            }
        }
    }
}
