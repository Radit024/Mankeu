package com.arinahub.arinaagri.calculator.presentation

import androidx.annotation.StringRes
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.domain.CategoryRole
import com.arinahub.arinaagri.calculator.domain.FinancialEventType

/**
 * Single translation layer between the accounting-correct domain model and the words the owner
 * actually uses.
 *
 * The domain deliberately keeps precise names (`REVENUE`, `EXPENSE`, `ASSET`, `EQUITY`,
 * `LIABILITY`) because the posting/journal rules depend on them being unambiguous. But those are
 * bookkeeping terms a smallholder farmer has no reason to know, and the owner's review called
 * them out directly ("istilah yang sebenernya ga harus diketahui sama petani").
 *
 * The vocabulary below is taken from the owner's own reference workbook,
 * `arina-agri/references/CATATAN KEUANGAN PADI 1 Ha ADE.xlsx`, which uses only:
 * Pemasukan / Pengeluaran / Volume / Satuan / Harga Satuan / Laba-Rugi / Arus Kas, grouped into
 * Input Usaha Tani, Tenaga Kerja, Jasa Alsintan, and Lain-lain. Nothing in the UI should show a
 * raw enum name — `MasterDataDialog` previously rendered `category.role` directly, which is how
 * "REVENUE"/"EXPENSE" were reaching the screen.
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

