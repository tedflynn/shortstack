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
    /** The goal moves each entry made (its split, purchase or cover), by entry id. */
    val movesByEntry: Map<Long, List<GoalMove>> = emptyMap()
)
