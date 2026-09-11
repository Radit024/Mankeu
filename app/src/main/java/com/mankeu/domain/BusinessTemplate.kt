package com.mankeu.domain

/**
 * The categories a new business starts with.
 *
 * This used to be four sector templates behind a `BusinessSector` enum (D-048), each with its own
 * category list and its own unit list. It was removed in round 9 for three reasons, all of them
 * visible in the code rather than argued from taste:
 *
 * 1. **Nobody could pick one for most of its life.** The picker was never wired up until round 8,
 *    so every business ever created was `GENERAL_MSME` regardless. Four lists were maintained,
 *    tested and localised; one was ever used.
 * 2. **The choice is unrecoverable.** A sector was applied once, at creation, and there was no way
 *    to change it afterwards — an owner who picked wrong had to delete the business or rebuild the
 *    category list by hand. A starting list that can be edited freely does not need to be right.
 * 3. **Categories are user data anyway.** Every category is editable, addable and archivable from
 *    Pengaturan, and an import creates whatever a workbook names with the role its section implies
 *    (D-060). The starting list is a convenience, not a schema.
 *
 * What remains is one general list that suits a warung, a bengkel, a kitchen or a farm equally,
 * because every name in it is a role rather than a trade. See D-063.
 */
data class DefaultCategoryTemplate(
    val name: String,
    val role: CategoryRole,
)

object DefaultMasterData {
    val categories: List<DefaultCategoryTemplate> = listOf(
        DefaultCategoryTemplate("Penjualan Barang", CategoryRole.REVENUE),
        DefaultCategoryTemplate("Pendapatan Jasa", CategoryRole.REVENUE),
        DefaultCategoryTemplate("Pendapatan Lain", CategoryRole.REVENUE),
        DefaultCategoryTemplate("Bahan Baku dan Stok", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Gaji dan Upah", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Sewa Tempat", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Listrik, Air dan Internet", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Kemasan dan Label", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Transport dan Pengiriman", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Pemasaran dan Promosi", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Perizinan dan Administrasi", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Biaya Bunga Pinjaman", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Biaya Lain-lain", CategoryRole.EXPENSE),
        DefaultCategoryTemplate("Peralatan dan Aset", CategoryRole.ASSET),
        DefaultCategoryTemplate("Modal Sendiri", CategoryRole.EQUITY),
        DefaultCategoryTemplate("Pinjaman Usaha", CategoryRole.LIABILITY),
    )
}
