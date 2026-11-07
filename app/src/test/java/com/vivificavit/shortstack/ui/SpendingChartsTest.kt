package com.vivificavit.shortstack.ui

import com.vivificavit.shortstack.data.local.CategorySum
import com.vivificavit.shortstack.ui.components.categoryColor
import com.vivificavit.shortstack.ui.components.legendCategories
import com.vivificavit.shortstack.ui.theme.CategoryPalette
import com.vivificavit.shortstack.ui.theme.OtherCategoryColor
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingChartsTest {
    @Test
    fun categoriesPastThePaletteShareOneGrayInsteadOfRepeatingAColor() {
        val categories = (1..10).map { "Category $it" }
        assertEquals(CategoryPalette, categories.take(8).map { categoryColor(it, categories) })
        assertEquals(CategoryPalette[1], categoryColor("category 2", categories))
        assertEquals(OtherCategoryColor, categoryColor("Category 9", categories))
        assertEquals(OtherCategoryColor, categoryColor("Not listed", categories))
    }

    @Test
    fun legendListsEachCategoryOnceWithIncomeFirst() {
        val income = listOf(CategorySum("Gift", 5_000), CategorySum("Allowance", 2_000))
        val spending = listOf(CategorySum("Toy", 2_500), CategorySum("gift", 800))
        assertEquals(listOf("Gift", "Allowance", "Toy"), legendCategories(income, spending))
    }
}
