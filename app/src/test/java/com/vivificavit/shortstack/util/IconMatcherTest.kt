package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconMatcherTest {
    @Test
    fun everyDefaultCategoryGetsAnIcon() {
        assertEquals(
            listOf("payments", "gift", "work", "game", "toy", "book"),
            DefaultCategories.DEFAULT_CATEGORIES.map { IconMatcher.match(it) }
        )
    }

    @Test
    fun matchesPluralsCaseAndPunctuation() {
        assertEquals("toy", IconMatcher.match("LEGO sets"))
        assertEquals("pets", IconMatcher.match("Walked the puppies"))
        assertEquals("game", IconMatcher.match("Fortnite: V-Bucks"))
        assertEquals("cake", IconMatcher.match("Grandma's birthday money"))
    }

    @Test
    fun firstRecognizedWordWins() {
        assertEquals("baseball", IconMatcher.match("Baseball game tickets"))
        assertEquals("movie", IconMatcher.match("Movie tickets"))
    }

    @Test
    fun twoWordPhrasesBeatTheirSingleWords() {
        assertEquals("icecream", IconMatcher.match("Ice cream with Sam"))
        assertEquals("dice", IconMatcher.match("New board game"))
        assertEquals("star", IconMatcher.match("Tooth fairy"))
    }

    @Test
    fun unknownTextHasNoMatch() {
        assertNull(IconMatcher.match("Grandma's fund"))
        assertNull(IconMatcher.match(""))
    }
}
