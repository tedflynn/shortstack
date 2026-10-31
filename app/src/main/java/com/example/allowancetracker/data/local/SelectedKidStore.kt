package com.example.allowancetracker.data.local

import android.content.Context
import androidx.core.content.edit

/** Remembers which kid's ledger was open, so the app reopens on it. */
interface SelectedKidStore {
    var lastSelectedKid: String?
}

class SharedPrefsSelectedKidStore(context: Context) : SelectedKidStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("allowance_tracker_prefs", Context.MODE_PRIVATE)

    override var lastSelectedKid: String?
        get() = prefs.getString(KEY_LAST_SELECTED_KID, null)
        set(value) = prefs.edit { putString(KEY_LAST_SELECTED_KID, value) }

    private companion object {
        const val KEY_LAST_SELECTED_KID = "last_selected_kid"
    }
}
