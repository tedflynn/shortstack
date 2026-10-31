package com.vivificavit.shortstack.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentagesTest {
    @Test
    fun roundsToNearestAndSumsTo100() {
        // $2.50 and $1.15 of $3.65 are 68.49% and 31.51%.
        assertEquals(listOf(68, 32), Percentages.of(listOf(250L, 115L)))
        assertEquals(listOf(34, 33, 33), Percentages.of(listOf(1L, 1L, 1L)))
        assertEquals(listOf(100), Percentages.of(listOf(999L)))
        assertEquals(listOf(50, 50), Percentages.of(listOf(5L, 5L)))
    }

    @Test
    fun emptyOrZeroTotals() {
        assertEquals(emptyList<Int>(), Percentages.of(emptyList()))
        assertEquals(listOf(0, 0), Percentages.of(listOf(0L, 0L)))
    }
}
