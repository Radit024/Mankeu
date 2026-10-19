package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class StockManagementTest {

    @Test
    fun `stock level calculation respects initial plus in minus out flow`() {
        val initial = BigDecimal("50.0")
        val movements = listOf(
            StockMovement("1", "item-1", StockMovementType.IN, BigDecimal("30.0"), LocalDate.now()),
            StockMovement("2", "item-1", StockMovementType.OUT, BigDecimal("25.0"), LocalDate.now()),
            StockMovement("3", "item-1", StockMovementType.IN, BigDecimal("10.0"), LocalDate.now()),
            StockMovement("4", "item-1", StockMovementType.OUT, BigDecimal("15.0"), LocalDate.now()),
        )

        // 50 + 30 - 25 + 10 - 15 = 50
        val finalStock = StockCalculation.calculateStockLevel(initial, movements)
        assertEquals(BigDecimal("50.000"), finalStock)
    }

    @Test
    fun `stock adjustment resets current physical stock count`() {
        val initial = BigDecimal("100.0")
        val movements = listOf(
            StockMovement("1", "item-1", StockMovementType.OUT, BigDecimal("40.0"), LocalDate.now()), // 60
            StockMovement("2", "item-1", StockMovementType.ADJUSTMENT, BigDecimal("55.0"), LocalDate.now()), // Opname: 55
            StockMovement("3", "item-1", StockMovementType.IN, BigDecimal("20.0"), LocalDate.now()), // 75
        )

        val finalStock = StockCalculation.calculateStockLevel(initial, movements)
        assertEquals(BigDecimal("75.000"), finalStock)
    }

    @Test
    fun `inventory total value computation uses exact integer rupiah`() {
        val cupItem = StockItem(
            id = "1",
            businessId = "biz-1",
            name = "Cup 16oz",
            unit = "pcs",
            basePriceRupiah = 1_000,
            currentStock = BigDecimal("200"),
            minAlertStock = BigDecimal("50"),
        )
        val coffeeItem = StockItem(
            id = "2",
            businessId = "biz-1",
            name = "Kopi Susu Bubuk",
            unit = "cup",
            basePriceRupiah = 3_500,
            currentStock = BigDecimal("100"),
            minAlertStock = BigDecimal("20"),
        )

        // Cup: 200 * 1.000 = 200.000
        // Kopi: 100 * 3.500 = 350.000
        // Total = 550.000
        val totalValue = StockCalculation.computeInventoryValue(listOf(cupItem, coffeeItem))
        assertEquals(550_000L, totalValue)

        assertFalse(cupItem.isLowStock)
        assertFalse(coffeeItem.isLowStock)

        // Test low stock alert
        val lowCup = cupItem.copy(currentStock = BigDecimal("45"))
        assertTrue(lowCup.isLowStock)
    }
}
