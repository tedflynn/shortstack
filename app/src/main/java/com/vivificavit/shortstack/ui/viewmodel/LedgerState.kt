package com.vivificavit.shortstack.ui.viewmodel

import com.vivificavit.shortstack.data.model.Transaction

/** What the Ledger tab shows for one kid. */
data class LedgerState(
    val balanceCents: Long,
    val transactions: List<Transaction>
)
