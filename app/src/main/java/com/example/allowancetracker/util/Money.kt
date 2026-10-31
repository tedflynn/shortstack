package com.example.allowancetracker.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Amounts are stored as whole cents (Long) so balances never pick up
 * floating-point rounding errors. Convert to dollars only for display.
 */
object Money {
    /** Largest single entry accepted: $1,000,000.00. */
    const val MAX_ENTRY_CENTS = 100_000_000L

    /**
     * Parses user input like "5", "5.5" or "$1,234.56" into cents.
     * Returns null for blank, non-numeric, non-positive, too large,
     * or more than two decimal places.
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim().removePrefix("$").replace(",", "")
        if (cleaned.isEmpty()) return null
        val value = try {
            BigDecimal(cleaned)
        } catch (e: NumberFormatException) {
            return null
        }
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2) return null
        val cents = value.movePointRight(2)
        if (cents > BigDecimal.valueOf(MAX_ENTRY_CENTS)) return null
        return cents.longValueExact()
    }

    /** Converts a legacy dollar amount (Double) to cents, rounding half-up. */
    fun dollarsToCents(dollars: Double): Long =
        BigDecimal.valueOf(dollars)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()

    /** Plain text for an amount field, e.g. 500 -> "5.00". */
    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

    fun format(cents: Long): String =
        NumberFormat.getCurrencyInstance(Locale.US).format(BigDecimal.valueOf(cents, 2))
}
