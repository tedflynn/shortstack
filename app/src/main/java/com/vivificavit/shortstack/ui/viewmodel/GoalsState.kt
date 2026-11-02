package com.vivificavit.shortstack.ui.viewmodel

import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.GoalMoveReason
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.util.GoalMath
import kotlin.math.abs

/** What the Goals tab shows for one kid. */
data class GoalsState(
    val pocketCents: Long,
    /** Oldest first. */
    val active: List<GoalProgress>,
    /** Most recently bought first. */
    val done: List<GoalProgress>,
    /** Each goal's history, newest first, by goal id. */
    val history: Map<Long, List<GoalHistoryItem>>
)

data class GoalProgress(
    val goal: Goal,
    val savedCents: Long,
    /** What it cost, once bought with goal savings. */
    val paidCents: Long? = null
) {
    val remainingCents: Long get() = GoalMath.remainingCents(goal, savedCents)
    val isReached: Boolean get() = savedCents >= goal.targetCents
    val isBought: Boolean get() = goal.completedAt != null
    val fraction: Float get() = (savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
}

/** One line in a goal's history. */
data class GoalHistoryItem(
    val id: Long,
    val kind: Kind,
    /** Positive when money went into the goal, negative when it came out. */
    val amountCents: Long,
    val timestamp: Long,
    /** The other goal's name, or the entry's description, depending on [kind]. */
    val detail: String? = null
) {
    enum class Kind { ADDED, TAKEN_OUT, MOVED_IN, MOVED_OUT, SPLIT, BOUGHT, COVERED, ADJUSTED }
}

/** Works out both tabs' states from one kid's entries, goals and moves (newest first). */
internal fun buildMoneyStates(
    transactions: List<Transaction>,
    goals: List<Goal>,
    moves: List<GoalMove>
): Pair<LedgerState, GoalsState> {
    val balance = transactions.sumOf { if (it.type == TransactionType.CREDIT) it.amountCents else -it.amountCents }
    val saved = GoalMath.savedByGoal(moves)
    val savedTotal = GoalMath.savedCents(goals, saved)
    val goalsById = goals.associateBy { it.id }
    val entriesById = transactions.associateBy { it.id }
    val (done, active) = goals.partition { it.completedAt != null }

    val goalTags = moves
        .mapNotNull { move ->
            val entryId = move.transactionId ?: return@mapNotNull null
            val goal = goalsById[move.goalId] ?: return@mapNotNull null
            val kind = when (move.reason) {
                GoalMoveReason.SPLIT -> GoalTag.Kind.SAVED
                GoalMoveReason.PURCHASE -> GoalTag.Kind.BOUGHT
                GoalMoveReason.COVER -> GoalTag.Kind.COVERED
                else -> return@mapNotNull null
            }
            entryId to GoalTag(kind, goal.name, abs(move.amountCents))
        }
        .groupBy({ it.first }, { it.second })

    val paidByGoal = moves
        .filter { it.reason == GoalMoveReason.PURCHASE }
        .mapNotNull { move -> entriesById[move.transactionId]?.let { move.goalId to it.amountCents } }
        .toMap()

    // A goal-to-goal move is two manual moves made at the same moment.
    val manualByTime = moves.filter { it.reason == GoalMoveReason.MANUAL }.groupBy { it.timestamp }
    val history = moves.groupBy { it.goalId }.mapValues { (_, goalMoves) ->
        goalMoves.map { move ->
            val entryTitle = move.transactionId?.let { entriesById[it]?.title }
            val (kind, detail) = when (move.reason) {
                GoalMoveReason.MANUAL -> {
                    val other = manualByTime[move.timestamp]
                        ?.firstOrNull { it.goalId != move.goalId && it.amountCents == -move.amountCents }
                        ?.let { goalsById[it.goalId]?.name }
                    when {
                        other != null && move.amountCents > 0 -> GoalHistoryItem.Kind.MOVED_IN to other
                        other != null -> GoalHistoryItem.Kind.MOVED_OUT to other
                        move.amountCents > 0 -> GoalHistoryItem.Kind.ADDED to null
                        else -> GoalHistoryItem.Kind.TAKEN_OUT to null
                    }
                }
                GoalMoveReason.SPLIT -> GoalHistoryItem.Kind.SPLIT to entryTitle
                GoalMoveReason.PURCHASE -> GoalHistoryItem.Kind.BOUGHT to entryTitle
                GoalMoveReason.COVER -> GoalHistoryItem.Kind.COVERED to entryTitle
                GoalMoveReason.ADJUST -> GoalHistoryItem.Kind.ADJUSTED to null
            }
            GoalHistoryItem(move.id, kind, move.amountCents, move.timestamp, detail)
        }
    }

    val ledger = LedgerState(
        balanceCents = balance,
        transactions = transactions,
        pocketCents = balance - savedTotal,
        savedCents = savedTotal,
        activeGoalCount = active.size,
        goalTags = goalTags,
        movesByEntry = moves.filter { it.transactionId != null }.groupBy { it.transactionId!! }
    )
    val goalsState = GoalsState(
        pocketCents = balance - savedTotal,
        active = active.map { GoalProgress(it, saved[it.id] ?: 0L) },
        done = done
            .sortedByDescending { it.completedAt }
            .map { GoalProgress(it, savedCents = 0L, paidCents = paidByGoal[it.id]) },
        history = history
    )
    return ledger to goalsState
}
