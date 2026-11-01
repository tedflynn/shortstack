package com.vivificavit.shortstack.ui

import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.ui.icons.EntryIcons
import com.vivificavit.shortstack.util.IconMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryIconsTest {
    private fun entry(title: String, category: String, type: TransactionType = TransactionType.DEBIT) =
        Transaction(kidName = "Sam", title = title, amountCents = 500, type = type, category = category)

    @Test
    fun everyWordListIconExistsAndKeysAreUnique() {
        val keys = EntryIcons.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        val missing = IconMatcher.allKeys - keys.toSet()
        assertTrue("word list names icons that don't exist: $missing", missing.isEmpty())
    }

    @Test
    fun chosenCategoryIconBeatsTheWordList() {
        val icon = EntryIcons.forEntry(entry("Lego set", "Toy"), chosen = mapOf("toy" to "star"))
        assertEquals("star", icon.key)
    }

    @Test
    fun noteBeatsCategoryNameWhenNothingIsChosen() {
        assertEquals("pizza", EntryIcons.forEntry(entry("Pizza night", "Toy"), emptyMap()).key)
        assertEquals("toy", EntryIcons.forEntry(entry("From Sam's friend", "Toy"), emptyMap()).key)
    }

    @Test
    fun unmatchedEntriesGetAGenericIconForTheirType() {
        assertEquals(
            "money",
            EntryIcons.forEntry(entry("From Grandma", "Grandma's fund", TransactionType.CREDIT), emptyMap()).key
        )
        assertEquals("shopping", EntryIcons.forEntry(entry("Thing", "Misc"), emptyMap()).key)
    }

    @Test
    fun chosenIconsMatchCategoriesIgnoringCaseAndUnknownKeysFallBack() {
        assertEquals("star", EntryIcons.forEntry(entry("x", "TOY"), mapOf("toy" to "star")).key)
        // A key from a newer app version this one doesn't know: treated as automatic.
        assertEquals("toy", EntryIcons.forEntry(entry("x", "Toy"), mapOf("toy" to "rocket-ship")).key)
    }

    private fun suggestedKeys(category: String, vararg notes: String) =
        EntryIcons.suggestedFor(category, notes.toList()).map { it.key }

    @Test
    fun suggestsNameMatchThenNoteMatchesThenVariationsThenTheRestOfTheTheme() {
        assertEquals(
            listOf("book", "pizza", "book_open", "books", "school", "art", "science", "music", "food", "lunch"),
            suggestedKeys("Book", "New book", "Comic", "Pizza night")
        )
    }

    @Test
    fun moreCommonNoteMatchesComeFirst() {
        assertEquals(
            listOf("candy", "icecream", "pizza"),
            suggestedKeys("Treats", "Pizza", "Ice cream", "Sundae").take(3)
        )
    }

    @Test
    fun notesAloneCanSuggestAndNothingMatchingSuggestsNothing() {
        assertEquals("toy", suggestedKeys("Stuff", "Lego set").first())
        assertTrue(suggestedKeys("Grandma's fund").isEmpty())
        assertTrue(suggestedKeys("Grandma's fund", "Something").isEmpty())
    }
}
