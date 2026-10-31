package com.vivificavit.bodad.ui.viewmodel

import com.vivificavit.bodad.data.model.Transaction

/** What the Ledger tab shows for one kid. */
data class LedgerState(
    val balanceCents: Long,
    val transactions: List<Transaction>
)
