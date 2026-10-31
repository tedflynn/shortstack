package com.vivificavit.bodad.ui.viewmodel

import androidx.annotation.StringRes

/** A snackbar message; the UI resolves it to text so it can be translated. */
data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<String> = emptyList(),
    /** When set, the snackbar shows an Undo button that runs this. */
    val onUndo: (() -> Unit)? = null
)
