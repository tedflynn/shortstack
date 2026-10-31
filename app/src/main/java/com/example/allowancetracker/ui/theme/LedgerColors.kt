package com.example.allowancetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Palette from the original AI Studio design (Tailwind teal, slate, emerald and red).
internal val Teal50 = Color(0xFFF0FDFA)
internal val Teal100 = Color(0xFFCCFBF1)
internal val Teal700 = Color(0xFF0F766E)
internal val Teal800 = Color(0xFF115E59)
internal val Teal900 = Color(0xFF134E4A)
internal val Slate50 = Color(0xFFF8FAFC)
internal val Slate100 = Color(0xFFF1F5F9)
internal val Slate200 = Color(0xFFE2E8F0)
internal val Slate300 = Color(0xFFCBD5E1)
internal val Slate500 = Color(0xFF64748B)
internal val Slate700 = Color(0xFF334155)
internal val Slate900 = Color(0xFF0F172A)
internal val Emerald100 = Color(0xFFD1FAE5)
internal val Emerald900 = Color(0xFF064E3B)
internal val Red100 = Color(0xFFFEE2E2)
internal val Red400 = Color(0xFFF87171)
internal val Red800 = Color(0xFF991B1B)
internal val Red900 = Color(0xFF7F1D1D)

/** Amount colors for money coming in and going out. */
val CreditGreen = Color(0xFF047857) // emerald-700
val DebitRed = Color(0xFFB91C1C) // red-700

/** Light backgrounds behind the selected Income / Expense choice. */
val CreditContainer = Emerald100
val OnCreditContainer = Emerald900
val DebitContainer = Red100
val OnDebitContainer = Red900

/** Negative balances on the dark balance card need a lighter red to stay readable. */
val NegativeOnDark = Red400
