package com.mankeu.presentation

import androidx.annotation.StringRes
import com.mankeu.R
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent

/**
 * Universal translation layer between domain accounting rules and intuitive, non-accounting language
 * suitable for MSMEs (UMKM) and small businesses.
 */
object BusinessWords {

    @StringRes
    fun roleLabel(role: CategoryRole): Int = when (role) {
        CategoryRole.REVENUE -> R.string.role_income
        CategoryRole.EXPENSE -> R.string.role_expense
        CategoryRole.ASSET -> R.string.role_tools
        CategoryRole.EQUITY -> R.string.role_own_capital
        CategoryRole.LIABILITY -> R.string.role_own_capital
    }

    /** Same as [roleLabel] but tolerant of the raw stored string on CategoryEntity.role. */
    @StringRes
    fun roleLabel(rawRole: String): Int =
        runCatching { CategoryRole.valueOf(rawRole) }.getOrNull()
            ?.let { roleLabel(it) }
            ?: R.string.role_expense

    @StringRes
    fun eventLabel(type: FinancialEventType): Int = when (type) {
        FinancialEventType.CASH_INCOME -> R.string.kind_cash_in
        FinancialEventType.CASH_EXPENSE -> R.string.kind_cash_out
        FinancialEventType.OWNER_CAPITAL -> R.string.kind_capital_in
        FinancialEventType.OWNER_DRAWING -> R.string.kind_capital_out
        FinancialEventType.ASSET_PURCHASE_CASH -> R.string.kind_tool_cash
        FinancialEventType.REVERSAL -> R.string.kind_cash_out
        else -> R.string.kind_cash_out
    }

    /** Event types that increase cash on hand. Used for the +/- sign and the in/out colour. */
    val INCOMING_TYPES = setOf(
        FinancialEventType.CASH_INCOME,
        FinancialEventType.OWNER_CAPITAL,
    )

    fun isIncoming(type: FinancialEventType): Boolean = type in INCOMING_TYPES

    /**
     * Which way [event] moves cash. A REVERSAL moves it opposite to its [source] — undoing an expense
     * puts the money back — so it cannot be read from its own type. Null when the source is not in
     * memory (the ledger keeps a bounded window): no sign is better than a wrong one.
     */
    fun isIncoming(event: MoneyEvent, source: MoneyEvent?): Boolean? =
        if (event.type == FinancialEventType.REVERSAL) source?.let { !isIncoming(it.type) } else isIncoming(event.type)
}
