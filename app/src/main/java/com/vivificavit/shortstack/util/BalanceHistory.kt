package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Where a kid's money stood at the end of [day]. */
data class BalancePoint(val day: LocalDate, val balanceCents: Long, val savedCents: Long)

/** A kid's balance and goal savings day by day, for the Trends chart. */
object BalanceHistory {

    /** Long ranges are sampled down to about this many days, so the chart stays quick to draw. */
    const val MAX_POINTS = 120

    /**
     * The balance and what goals held at the end of each day from [startDate]
     * (or the kid's first entry) to [endDate] (or [today]). Everything before
     * the start still counts, since a balance carries over. A long range is
     * sampled evenly, always keeping its first and last day.
     */
    fun build(
        transactions: List<Transaction>,
        moves: List<GoalMove>,
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<BalancePoint> {
        fun dayOf(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val entries = transactions
            .map { dayOf(it.timestamp) to if (it.type == TransactionType.CREDIT) it.amountCents else -it.amountCents }
            .sortedBy { it.first }
        val goalMoves = moves.map { dayOf(it.timestamp) to it.amountCents }.sortedBy { it.first }

        var first = startDate ?: listOfNotNull(entries.firstOrNull()?.first, goalMoves.firstOrNull()?.first).minOrNull() ?: today
        var last = endDate ?: today
        if (first.isAfter(last)) first = last.also { last = first }

        val span = ChronoUnit.DAYS.between(first, last)
        val step = (span + MAX_POINTS - 1) / MAX_POINTS
        val days = if (step <= 1L) {
            (0..span).map { first.plusDays(it) }
        } else {
            (generateSequence(first) { it.plusDays(step) }.takeWhile { it.isBefore(last) } + last).toList()
        }

        var entryIndex = 0
        var moveIndex = 0
        var balance = 0L
        var saved = 0L
        return days.map { day ->
            while (entryIndex < entries.size && !entries[entryIndex].first.isAfter(day)) balance += entries[entryIndex++].second
            while (moveIndex < goalMoves.size && !goalMoves[moveIndex].first.isAfter(day)) saved += goalMoves[moveIndex++].second
            BalancePoint(day, balance, saved.coerceAtLeast(0L))
        }
    }
}
