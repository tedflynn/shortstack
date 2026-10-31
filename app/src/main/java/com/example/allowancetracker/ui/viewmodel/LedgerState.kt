package com.example.allowancetracker.ui.viewmodel

import com.example.allowancetracker.data.model.Transaction

/** What the Ledger tab shows for one kid. */
data class LedgerState(
    val balanceCents: Long,
    val transactions: List<Transaction>
)
