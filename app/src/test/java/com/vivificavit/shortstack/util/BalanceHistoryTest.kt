package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class BalanceHistoryTest {
    private val zone = ZoneOffset.UTC
    private fun at(date: String) = LocalDate.parse(date).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun entry(date: String, cents: Long, type: TransactionType = TransactionType.CREDIT) =
        Transaction(kidName = "Sam", title = "Entry", amountCents = cents, type = type, timestamp = at(date))

    private fun move(date: String, cents: Long) =
        GoalMove(kidName = "Sam", goalId = 1, amountCents = cents, reason = GoalMoveReason.MANUAL, timestamp = at(date))

    @Test
    fun eachDayCarriesTheBalanceAndSavingsSoFar() {
        val points = BalanceHistory.build(
            transactions = listOf(entry("2026-11-01", 1_000), entry("2026-11-03", 300, TransactionType.DEBIT)),
            moves = listOf(move("2026-11-02", 400), move("2026-11-04", -400)),
            startDate = null,
            endDate = null,
            today = LocalDate.parse("2026-11-04"),
            zone = zone
        )
        assertEquals(
            listOf(1_000L to 0L, 1_000L to 400L, 700L to 400L, 700L to 0L),
            points.map { it.balanceCents to it.savedCents }
        )
        assertEquals(LocalDate.parse("2026-11-01"), points.first().day)
    }

    @Test
    fun aRangeStartsFromWhatCameBeforeIt() {
        val points = BalanceHistory.build(
            transactions = listOf(entry("2026-10-01", 5_000), entry("2026-11-05", 1_000)),
            moves = emptyList(),
            startDate = LocalDate.parse("2026-11-04"),
            endDate = LocalDate.parse("2026-11-06"),
            today = LocalDate.parse("2026-11-20"),
            zone = zone
        )
        assertEquals(listOf(5_000L, 6_000L, 6_000L), points.map { it.balanceCents })
    }

    @Test
    fun longRangesAreSampledButKeepBothEnds() {
        val points = BalanceHistory.build(
            transactions = listOf(entry("2024-01-01", 100), entry("2026-11-06", 100)),
            moves = emptyList(),
            startDate = null,
            endDate = null,
            today = LocalDate.parse("2026-11-06"),
            zone = zone
        )
        assert(points.size <= BalanceHistory.MAX_POINTS + 1)
        assertEquals(LocalDate.parse("2024-01-01"), points.first().day)
        assertEquals(LocalDate.parse("2026-11-06"), points.last().day)
        assertEquals(200L, points.last().balanceCents)
    }

    @Test
    fun noEntriesIsOneFlatDay() {
        val points = BalanceHistory.build(emptyList(), emptyList(), null, null, LocalDate.parse("2026-11-06"), zone)
        assertEquals(listOf(BalancePoint(LocalDate.parse("2026-11-06"), 0L, 0L)), points)
    }
}
