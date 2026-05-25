package com.hllous.plantravel.ui

import com.hllous.plantravel.ui.utils.isItemFullyAssigned
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseItemTest {

    @Test
    fun noneAssigned_notFullyAssigned() {
        assertFalse(isItemFullyAssigned(assignedQuantity = 0, totalQuantity = 5))
    }

    @Test
    fun partiallyAssigned_notFullyAssigned() {
        assertFalse(isItemFullyAssigned(assignedQuantity = 3, totalQuantity = 5))
    }

    @Test
    fun allAssigned_fullyAssigned() {
        assertTrue(isItemFullyAssigned(assignedQuantity = 5, totalQuantity = 5))
    }

    @Test
    fun zeroQuantityItem_fullyAssigned() {
        assertTrue(isItemFullyAssigned(assignedQuantity = 0, totalQuantity = 0))
    }
}
