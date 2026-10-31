package com.vivificavit.bodad.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesValidAmounts() {
        assertEquals(500L, Money.parseToCents("5"))
        assertEquals(550L, Money.parseToCents("5.5"))
        assertEquals(1999L, Money.parseToCents("19.99"))
        assertEquals(1L, Money.parseToCents("0.01"))
        assertEquals(123456L, Money.parseToCents("$1,234.56"))
        assertEquals(Money.MAX_ENTRY_CENTS, Money.parseToCents("1000000"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        listOf("", " ", "0", "0.00", "-5", "abc", "NaN", "Infinity", "1.001", "1000000.01", "1e100000")
            .forEach { assertNull("expected null for \"$it\"", Money.parseToCents(it)) }
    }

    @Test
    fun convertsLegacyDollarsWithoutDrift() {
        assertEquals(30L, Money.dollarsToCents(0.1 + 0.2))
        assertEquals(115L, Money.dollarsToCents(1.15))
        assertEquals(1999L, Money.dollarsToCents(19.99))
    }

    @Test
    fun formatsCents() {
        assertEquals("$1,234.56", Money.format(123456L))
        assertEquals("-$0.05", Money.format(-5L))
    }
}
