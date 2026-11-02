package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalMathTest {
    private fun goal(id: Long, createdAt: Long = id, completedAt: Long? = null) =
        Goal(id = id, kidName = "Sam", name = "Goal $id", targetCents = 7_000, createdAt = createdAt, completedAt = completedAt)

    private fun move(goalId: Long, cents: Long, reason: GoalMoveReason = GoalMoveReason.MANUAL) =
        GoalMove(kidName = "Sam", goalId = goalId, amountCents = cents, reason = reason, timestamp = 0)

    @Test
    fun savedAndRemainingAddUpMoves() {
        val moves = listOf(move(1, 2_000), move(1, -500), move(2, 300))
        val saved = GoalMath.savedByGoal(moves)
        assertEquals(mapOf(1L to 1_500L, 2L to 300L), saved)
        assertEquals(1_800L, GoalMath.savedCents(listOf(goal(1), goal(2)), saved))
        assertEquals(5_500L, GoalMath.remainingCents(goal(1), 1_500))
        assertEquals(0L, GoalMath.remainingCents(goal(1), 9_000))
    }

    @Test
    fun boughtGoalsDontCountAsSaved() {
        val saved = mapOf(1L to 2_000L, 2L to 500L)
        assertEquals(500L, GoalMath.savedCents(listOf(goal(1, completedAt = 5), goal(2)), saved))
    }

    @Test
    fun splitRoundsDownToTheCent() {
        assertEquals(200L, GoalMath.splitCents(1_000, 20))
        // 10% of $0.99 is 9.9 cents.
        assertEquals(9L, GoalMath.splitCents(99, 10))
        assertEquals(999L, GoalMath.splitCents(999, 100))
    }

    @Test
    fun nothingToFixWhenGoalsFitTheBalance() {
        val fixes = GoalMath.rebalance("Sam", 5_000, listOf(goal(1)), listOf(move(1, 2_000)))
        assertEquals(emptyList<GoalMove>(), fixes)
    }

    @Test
    fun overspendingTakesFromThePreferredGoalFirstThenTheNewest() {
        val goals = listOf(goal(1), goal(2), goal(3))
        val moves = listOf(move(1, 1_000), move(2, 1_000), move(3, 1_000))
        // $30 saved but only $15 left: $15 must come out of goals.
        val fixes = GoalMath.rebalance("Sam", 1_500, goals, moves, preferredGoalId = 1, transactionId = 42)
        assertEquals(listOf(1L to -1_000L, 3L to -500L), fixes.map { it.goalId to it.amountCents })
        fixes.forEach {
            assertEquals(GoalMoveReason.COVER, it.reason)
            assertEquals(42L, it.transactionId)
        }
    }

    @Test
    fun negativeBalanceEmptiesEveryGoal() {
        val goals = listOf(goal(1), goal(2))
        val fixes = GoalMath.rebalance("Sam", -300, goals, listOf(move(1, 400), move(2, 600)))
        assertEquals(setOf(1L to -400L, 2L to -600L), fixes.map { it.goalId to it.amountCents }.toSet())
    }

    @Test
    fun goalsBelowZeroAndBoughtGoalsAreSetStraight() {
        val goals = listOf(goal(1), goal(2, completedAt = 9))
        // Goal 1 lost a split it had already moved on; goal 2 was bought but kept $2.
        val moves = listOf(move(1, -200), move(2, 200))
        val fixes = GoalMath.rebalance("Sam", 10_000, goals, moves)
        assertEquals(listOf(1L to 200L, 2L to -200L), fixes.map { it.goalId to it.amountCents })
        fixes.forEach {
            assertEquals(GoalMoveReason.ADJUST, it.reason)
            assertEquals(null, it.transactionId)
        }
    }
}
