package com.hllous.plantravel.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseTravelRepositoryImplTest {

    @Test
    fun detectsMissingCategoryColumnFromSchemaCacheMessage() {
        val message =
            "Could not find the 'category' column of 'expense_groups' in the schema cache"

        assertTrue(isMissingExpenseGroupsCategoryColumnError(message))
    }

    @Test
    fun detectsMissingCategoryColumnFromPostgresColumnMessage() {
        val message =
            "column expense_groups.category does not exist"

        assertTrue(isMissingExpenseGroupsCategoryColumnError(message))
    }

    @Test
    fun ignoresUnrelatedErrors() {
        val message = "permission denied for table expense_groups"

        assertFalse(isMissingExpenseGroupsCategoryColumnError(message))
    }
}
