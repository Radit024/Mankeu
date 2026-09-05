package com.arinahub.arinaagri.calculator.presentation

import androidx.annotation.StringRes
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.domain.CategoryRole
import com.arinahub.arinaagri.calculator.domain.FinancialEventType

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
        CategoryRole.LIABILITY -> R.string.role_loan
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
        FinancialEventType.CREDIT_SALE -> R.string.kind_sale_unpaid
        FinancialEventType.CREDIT_PURCHASE_EXPENSE -> R.string.kind_buy_unpaid
        FinancialEventType.CREDIT_PURCHASE_ASSET -> R.string.kind_tool_unpaid
        FinancialEventType.RECEIVABLE_COLLECTION -> R.string.kind_cash_in
        FinancialEventType.PAYABLE_SETTLEMENT -> R.string.kind_cash_out
        FinancialEventType.OWNER_CAPITAL -> R.string.kind_capital_in
        FinancialEventType.OWNER_DRAWING -> R.string.kind_capital_out
        FinancialEventType.LOAN_RECEIVED -> R.string.kind_loan_in
        FinancialEventType.LOAN_PRINCIPAL_REPAID -> R.string.kind_loan_principal
        FinancialEventType.LOAN_INTEREST_PAID -> R.string.kind_loan_interest
        FinancialEventType.ASSET_PURCHASE_CASH -> R.string.kind_tool_cash
        FinancialEventType.REVERSAL -> R.string.kind_cash_out
    }

    /** Event types that increase cash on hand. Used for the +/- sign and the in/out colour. */
    val INCOMING_TYPES = setOf(
        FinancialEventType.CASH_INCOME,
        FinancialEventType.CREDIT_SALE,
        FinancialEventType.RECEIVABLE_COLLECTION,
        FinancialEventType.OWNER_CAPITAL,
        FinancialEventType.LOAN_RECEIVED,
    )

    fun isIncoming(type: FinancialEventType): Boolean = type in INCOMING_TYPES
}
