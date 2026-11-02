package com.vivificavit.shortstack.util

import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason

/**
 * The money rules for goals. Setting money aside never changes a kid's balance;
 * it only stops that money counting as pocket money. For each kid:
 * - no goal holds less than nothing, and a bought goal holds nothing;
 * - goals never hold more than the kid has, so pocket money only goes below
 *   zero when the balance itself does.
 */
object GoalMath {

    /** Each goal's saved amount: the sum of its moves. Goals with no moves are absent. */
    fun savedByGoal(moves: List<GoalMove>): Map<Long, Long> =
        moves.groupingBy { it.goalId }.fold(0L) { sum, move -> sum + move.amountCents }

    /** Money set aside across the goals still being saved for. */
    fun savedCents(goals: List<Goal>, saved: Map<Long, Long>): Long =
        goals.filter { it.completedAt == null }.sumOf { saved[it.id] ?: 0L }

    /** How much more [goal] needs; zero once it's reached. */
    fun remainingCents(goal: Goal, savedCents: Long): Long =
        (goal.targetCents - savedCents).coerceAtLeast(0L)

    /** [percent] of [amountCents], rounded down to the cent so a split never exceeds the entry. */
    fun splitCents(amountCents: Long, percent: Int): Long = amountCents * percent / 100

    /**
     * The moves that bring a kid's goals back in line with the rules above.
     *
     * Spending that pocket money can't cover is taken from [preferredGoalId]
     * first, then from the newest goals. Those moves are linked to
     * [transactionId], the entry that caused them, so deleting or editing that
     * entry gives the money back.
     */
    fun rebalance(
        kidName: String,
        balanceCents: Long,
        goals: List<Goal>,
        moves: List<GoalMove>,
        preferredGoalId: Long? = null,
        transactionId: Long? = null,
        timestamp: Long = System.currentTimeMillis()
    ): List<GoalMove> {
        val saved = savedByGoal(moves).toMutableMap()
        val fixes = mutableListOf<GoalMove>()

        goals.forEach { goal ->
            val current = saved[goal.id] ?: 0L
            val allowed = if (goal.completedAt != null) 0L else current.coerceAtLeast(0L)
            if (current != allowed) {
                fixes += GoalMove(
                    kidName = kidName,
                    goalId = goal.id,
                    amountCents = allowed - current,
                    reason = GoalMoveReason.ADJUST,
                    timestamp = timestamp
                )
                saved[goal.id] = allowed
            }
        }

        val active = goals.filter { it.completedAt == null }
        var shortfall = savedCents(active, saved) - balanceCents.coerceAtLeast(0L)
        if (shortfall > 0L) {
            val order = active.sortedWith(
                compareByDescending<Goal> { it.id == preferredGoalId }
                    .thenByDescending { it.createdAt }
                    .thenByDescending { it.id }
            )
            for (goal in order) {
                if (shortfall <= 0L) break
                val take = minOf(saved[goal.id] ?: 0L, shortfall)
                if (take > 0L) {
                    fixes += GoalMove(
                        kidName = kidName,
                        goalId = goal.id,
                        amountCents = -take,
                        reason = GoalMoveReason.COVER,
                        timestamp = timestamp,
                        transactionId = transactionId
                    )
                    shortfall -= take
                }
            }
        }
        return fixes
    }
}
