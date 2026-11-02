package com.vivificavit.shortstack.ui.viewmodel

import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.Transaction

/** What the Ledger tab shows for one kid. */
data class LedgerState(
    /** Everything the kid has: income minus spending, including money set aside for goals. */
    val balanceCents: Long,
    val transactions: List<Transaction>,
    /** The balance minus what's set aside for goals still being saved for. */
    val pocketCents: Long = balanceCents,
    val savedCents: Long = 0L,
    val activeGoalCount: Int = 0,
    /** How each entry touched a goal, by entry id. */
    val goalTags: Map<Long, List<GoalTag>> = emptyMap(),
    /** The goal moves each entry made (its split, purchase or cover), by entry id. */
    val movesByEntry: Map<Long, List<GoalMove>> = emptyMap()
)

/** A note on a ledger entry about a goal it saved for, paid for or took money from. */
data class GoalTag(
    val kind: Kind,
    val goalName: String,
    val amountCents: Long
) {
    enum class Kind { SAVED, BOUGHT, COVERED }
}
