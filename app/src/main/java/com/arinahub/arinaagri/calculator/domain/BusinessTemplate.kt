package com.arinahub.arinaagri.calculator.domain

/**
 * Represents business sectors supported by the white-label financial platform.
 */
enum class BusinessSector(
    val title: String,
    val description: String,
) {
    GENERAL_MSME(
        title = "UMKM Umum / Dagang",
        description = "Cocok untuk toko kelontong, warung, reseller, dan perdagangan umum.",
    ),
    CULINARY_FB(
        title = "Kuliner / F&B",
        description = "Cocok untuk kafe, rumah makan, katering, dan jajanan kuliner.",
    ),
    SERVICES(
        title = "Jasa & Servis",
        description = "Cocok untuk bengkel, salon, laundry, rental, dan penyedia jasa.",
    ),
    AGRICULTURE(
        title = "Pertanian & Agribisnis",
        description = "Cocok untuk budidaya tanaman pangan, perkebunan, dan peternakan.",
    ),
}

data class DefaultCategoryTemplate(
    val name: String,
    val role: CategoryRole,
)

data class DefaultUnitTemplate(
    val name: String,
    val symbol: String,
)

/**
 * Predefined master data templates for different business sectors.
 */
data class BusinessTemplate(
    val sector: BusinessSector,
    val defaultUnits: List<DefaultUnitTemplate>,
    val defaultCategories: List<DefaultCategoryTemplate>,
) {
    companion object {
        val GENERAL_MSME_TEMPLATE = BusinessTemplate(
            sector = BusinessSector.GENERAL_MSME,
            defaultUnits = listOf(
                DefaultUnitTemplate("Pcs / Buah", "pcs"),
                DefaultUnitTemplate("Dus / Karton", "dus"),
                DefaultUnitTemplate("Pak / Bungkus", "pak"),
                DefaultUnitTemplate("Kilogram", "kg"),
                DefaultUnitTemplate("Gram", "g"),
                DefaultUnitTemplate("Liter", "L"),
                DefaultUnitTemplate("Unit", "unit"),
                DefaultUnitTemplate("Paket", "paket"),
                DefaultUnitTemplate("Bulan", "bln"),
                DefaultUnitTemplate("Hari", "hari"),
            ),
            defaultCategories = listOf(
                DefaultCategoryTemplate("Penjualan Barang", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Pendapatan Jasa", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Pendapatan Lain", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Stok & Belanja Dagangan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Gaji & Upah Karyawan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Sewa Tempat / Kios", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Listrik, Air & Internet", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Kemasan & Plastik", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Ongkir & Transport", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Pemasaran & Iklan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Operasional Lain", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Peralatan & Aset Toko", CategoryRole.ASSET),
                DefaultCategoryTemplate("Modal Sendiri", CategoryRole.EQUITY),
                DefaultCategoryTemplate("Pinjaman Usaha", CategoryRole.LIABILITY),
            ),
        )

        val CULINARY_TEMPLATE = BusinessTemplate(
            sector = BusinessSector.CULINARY_FB,
            defaultUnits = listOf(
                DefaultUnitTemplate("Porsi", "porsi"),
                DefaultUnitTemplate("Cup / Gelas", "cup"),
                DefaultUnitTemplate("Box / Kotak", "box"),
                DefaultUnitTemplate("Pcs", "pcs"),
                DefaultUnitTemplate("Kilogram", "kg"),
                DefaultUnitTemplate("Gram", "g"),
                DefaultUnitTemplate("Liter", "L"),
                DefaultUnitTemplate("Paket", "paket"),
                DefaultUnitTemplate("Hari", "hari"),
                DefaultUnitTemplate("Bulan", "bln"),
            ),
            defaultCategories = listOf(
                DefaultCategoryTemplate("Penjualan Makanan & Minuman", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Pendapatan Katering / Pesanan", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Bahan Baku Makanan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Bahan Minuman & Bumbu", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Gas, Listrik & Air", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Gaji Koki & Pegawai", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Kemasan & Packaging", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Sewa Tempat", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Transport & Pengiriman", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Operasional Dapur", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Peralatan Dapur & Kafe", CategoryRole.ASSET),
                DefaultCategoryTemplate("Modal Sendiri", CategoryRole.EQUITY),
                DefaultCategoryTemplate("Pinjaman Usaha", CategoryRole.LIABILITY),
            ),
        )

        val SERVICES_TEMPLATE = BusinessTemplate(
            sector = BusinessSector.SERVICES,
            defaultUnits = listOf(
                DefaultUnitTemplate("Unit / Kendaraan", "unit"),
                DefaultUnitTemplate("Jam", "jam"),
                DefaultUnitTemplate("Hari", "hari"),
                DefaultUnitTemplate("Paket", "paket"),
                DefaultUnitTemplate("Pcs", "pcs"),
                DefaultUnitTemplate("Bulan", "bln"),
            ),
            defaultCategories = listOf(
                DefaultCategoryTemplate("Pendapatan Jasa Servis", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Penjualan Sparepart / Produk", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Pembelian Suku Cadang", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Gaji Teknisi & Staf", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Sewa Bengkel / Tempat", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Listrik & Operasional", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Peralatan Kerja & Aset", CategoryRole.ASSET),
                DefaultCategoryTemplate("Modal Sendiri", CategoryRole.EQUITY),
                DefaultCategoryTemplate("Pinjaman Usaha", CategoryRole.LIABILITY),
                DefaultCategoryTemplate("Biaya Lain-lain", CategoryRole.EXPENSE),
            ),
        )

        val AGRICULTURE_TEMPLATE = BusinessTemplate(
            sector = BusinessSector.AGRICULTURE,
            defaultUnits = listOf(
                DefaultUnitTemplate("Kilogram", "Kg"),
                DefaultUnitTemplate("Gram", "g"),
                DefaultUnitTemplate("Ton", "ton"),
                DefaultUnitTemplate("Liter", "Liter"),
                DefaultUnitTemplate("Sak", "Sak"),
                DefaultUnitTemplate("Hari Orang Kerja", "HOK"),
                DefaultUnitTemplate("Paket", "Paket"),
                DefaultUnitTemplate("Lembar", "Lembar"),
                DefaultUnitTemplate("Buah", "buah"),
                DefaultUnitTemplate("Unit", "unit"),
                DefaultUnitTemplate("Jam", "jam"),
                DefaultUnitTemplate("Hari", "hari"),
            ),
            defaultCategories = listOf(
                DefaultCategoryTemplate("Hasil Panen", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Penjualan Lain", CategoryRole.REVENUE),
                DefaultCategoryTemplate("Benih dan Bibit", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Pupuk", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Obat dan Pestisida", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Tenaga Kerja", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Jasa Alsintan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Sewa Lahan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Pengairan", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Transport", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Lain-lain", CategoryRole.EXPENSE),
                DefaultCategoryTemplate("Alat dan Mesin", CategoryRole.ASSET),
                DefaultCategoryTemplate("Modal Sendiri", CategoryRole.EQUITY),
                DefaultCategoryTemplate("Pinjaman", CategoryRole.LIABILITY),
            ),
        )

        fun forSector(sector: BusinessSector): BusinessTemplate = when (sector) {
            BusinessSector.GENERAL_MSME -> GENERAL_MSME_TEMPLATE
            BusinessSector.CULINARY_FB -> CULINARY_TEMPLATE
            BusinessSector.SERVICES -> SERVICES_TEMPLATE
            BusinessSector.AGRICULTURE -> AGRICULTURE_TEMPLATE
        }
    }
}
