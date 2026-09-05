package com.mankeu.domain

import com.mankeu.importer.CategoryClassifier
import com.mankeu.presentation.BusinessWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessTemplateTest {

    @Test
    fun `all sectors have valid default templates`() {
        val sectors = BusinessSector.entries
        assertEquals(4, sectors.size)

        sectors.forEach { sector ->
            val template = BusinessTemplate.forSector(sector)
            assertEquals(sector, template.sector)
            assertTrue("Sector $sector should have categories", template.defaultCategories.isNotEmpty())
            assertTrue("Sector $sector should have units", template.defaultUnits.isNotEmpty())

            val incomeCategories = template.defaultCategories.filter { it.role == CategoryRole.REVENUE }
            val expenseCategories = template.defaultCategories.filter { it.role == CategoryRole.EXPENSE }

            assertTrue("Sector $sector must have at least one revenue category", incomeCategories.isNotEmpty())
            assertTrue("Sector $sector must have multiple expense categories", expenseCategories.size >= 4)
        }
    }

    @Test
    fun `agriculture template preserves exact legacy categories and units`() {
        val agri = BusinessTemplate.AGRICULTURE_TEMPLATE
        val categoryNames = agri.defaultCategories.map { it.name }.toSet()
        val unitSymbols = agri.defaultUnits.map { it.symbol }.toSet()

        assertTrue(categoryNames.contains("Benih dan Bibit"))
        assertTrue(categoryNames.contains("Pupuk"))
        assertTrue(categoryNames.contains("Obat dan Pestisida"))
        assertTrue(categoryNames.contains("Tenaga Kerja"))
        assertTrue(categoryNames.contains("Jasa Alsintan"))
        assertTrue(categoryNames.contains("Pengairan"))
        assertTrue(categoryNames.contains("Transport"))
        assertTrue(categoryNames.contains("Sewa Lahan"))
        assertTrue(categoryNames.contains("Lain-lain"))
        assertTrue(categoryNames.contains("Hasil Panen"))

        assertTrue(unitSymbols.contains("Kg"))
        assertTrue(unitSymbols.contains("Sak"))
        assertTrue(unitSymbols.contains("Liter"))
        assertTrue(unitSymbols.contains("hari"))
        assertTrue(unitSymbols.contains("HOK"))
    }

    @Test
    fun `general msme template contains standard trade categories and units`() {
        val msme = BusinessTemplate.GENERAL_MSME_TEMPLATE
        val categoryNames = msme.defaultCategories.map { it.name }.toSet()
        val unitSymbols = msme.defaultUnits.map { it.symbol }.toSet()

        assertTrue(categoryNames.contains("Penjualan Barang"))
        assertTrue(categoryNames.contains("Stok & Belanja Dagangan"))
        assertTrue(categoryNames.contains("Gaji & Upah Karyawan"))
        assertTrue(categoryNames.contains("Sewa Tempat / Kios"))
        assertTrue(categoryNames.contains("Kemasan & Plastik"))
        assertTrue(categoryNames.contains("Listrik, Air & Internet"))

        assertTrue(unitSymbols.contains("pcs"))
        assertTrue(unitSymbols.contains("paket"))
        assertTrue(unitSymbols.contains("dus"))
    }

    @Test
    fun `culinary fb template contains food and beverage specific items`() {
        val culinary = BusinessTemplate.CULINARY_TEMPLATE
        val categoryNames = culinary.defaultCategories.map { it.name }.toSet()
        val unitSymbols = culinary.defaultUnits.map { it.symbol }.toSet()

        assertTrue(categoryNames.contains("Penjualan Makanan & Minuman"))
        assertTrue(categoryNames.contains("Bahan Baku Makanan"))
        assertTrue(categoryNames.contains("Gas, Listrik & Air"))
        assertTrue(categoryNames.contains("Kemasan & Packaging"))

        assertTrue(unitSymbols.contains("porsi"))
        assertTrue(unitSymbols.contains("cup"))
    }

    @Test
    fun `services template contains service-oriented items`() {
        val services = BusinessTemplate.SERVICES_TEMPLATE
        val categoryNames = services.defaultCategories.map { it.name }.toSet()
        val unitSymbols = services.unitsOrDef().toSet()

        assertTrue(categoryNames.contains("Pendapatan Jasa Servis"))
        assertTrue(categoryNames.contains("Pembelian Suku Cadang"))
        assertTrue(categoryNames.contains("Gaji Teknisi & Staf"))

        assertTrue(unitSymbols.contains("jam"))
        assertTrue(unitSymbols.contains("paket"))
        assertTrue(unitSymbols.contains("unit"))
    }

    private fun BusinessTemplate.unitsOrDef(): List<String> = defaultUnits.map { it.symbol }

    @Test
    fun `classifier recognises general msme keywords`() {
        val salaryCategory = CategoryClassifier.classify("Gaji kasir toko bulan Juni", CategoryRole.EXPENSE)
        assertNotNull(salaryCategory)
        assertEquals("Tenaga Kerja", salaryCategory)

        val shippingCategory = CategoryClassifier.classify("Ongkir ekspedisi JNE paket pelanggan", CategoryRole.EXPENSE)
        assertNotNull(shippingCategory)
        assertEquals("Transport", shippingCategory)

        val packagingCategory = CategoryClassifier.classify("Beli kantong kresek dan kardus", CategoryRole.EXPENSE)
        assertNotNull(packagingCategory)
        assertEquals("Lain-lain", packagingCategory)

        val cateringIncome = CategoryClassifier.classify("Pembayaran pesanan katering arisan", CategoryRole.REVENUE)
        assertNotNull(cateringIncome)
        assertEquals("Penjualan Lain", cateringIncome)

        val serviceIncome = CategoryClassifier.classify("Servis motor operasional", CategoryRole.REVENUE)
        assertNotNull(serviceIncome)
        assertEquals("Penjualan Lain", serviceIncome)
    }

    @Test
    fun `business words provides valid labels for all roles and event types`() {
        CategoryRole.entries.forEach { role ->
            assertTrue(BusinessWords.roleLabel(role) != 0)
        }
        FinancialEventType.entries.forEach { type ->
            assertTrue(BusinessWords.eventLabel(type) != 0)
            assertTrue(BusinessWords.isIncoming(type) == (type in BusinessWords.INCOMING_TYPES))
        }
    }
}
