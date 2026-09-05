package com.mankeu.presentation

import androidx.annotation.StringRes
import com.mankeu.R
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType

/**
 * Single translation layer between the accounting-correct domain model and the words small business owners
 * actually use.
 *
 * The domain deliberately keeps precise names (`REVENUE`, `EXPENSE`, `ASSET`, `EQUITY`,
 * `LIABILITY`) because the posting/journal rules depend on them being unambiguous.
 *
 * The vocabulary uses:
 * Pemasukan / Pengeluaran / Volume / Satuan / Harga Satuan / Laba-Rugi / Arus Kas.
 * Nothing in the UI should show a raw enum name.
 */
/**
 * Legacy compatibility alias for [BusinessWords].
 */
object FarmerWords {

    @StringRes
    fun roleLabel(role: CategoryRole): Int = BusinessWords.roleLabel(role)

    @StringRes
    fun roleLabel(rawRole: String): Int = BusinessWords.roleLabel(rawRole)

    @StringRes
    fun eventLabel(type: FinancialEventType): Int = BusinessWords.eventLabel(type)

    val INCOMING_TYPES = BusinessWords.INCOMING_TYPES

    fun isIncoming(type: FinancialEventType): Boolean = BusinessWords.isIncoming(type)
}

