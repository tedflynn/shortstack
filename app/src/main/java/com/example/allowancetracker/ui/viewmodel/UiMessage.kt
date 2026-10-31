package com.example.allowancetracker.ui.viewmodel

import androidx.annotation.StringRes

/** A snackbar message; the UI resolves it to text so it can be translated. */
data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<String> = emptyList()
)
