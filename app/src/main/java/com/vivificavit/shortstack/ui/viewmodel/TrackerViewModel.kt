package com.vivificavit.shortstack.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.local.CategorySum
import com.vivificavit.shortstack.data.local.SelectedKidStore
import com.vivificavit.shortstack.data.model.DefaultCategories
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.repository.TransactionRepository
import com.vivificavit.shortstack.util.DateRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class TrackerViewModel(
    private val repository: TransactionRepository,
    private val selectedKidStore: SelectedKidStore
) : ViewModel() {

    /** Wraps the stored choice so "not loaded yet" (null) differs from "nothing stored". */
    private data class PreferredKid(val name: String?)

    /** The kid the user last picked (may be stale if that kid was removed); null until loaded. */
    private val preferredKid: StateFlow<PreferredKid?> = selectedKidStore.lastSelectedKid
        .map { PreferredKid(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /** Null until the database has loaded the kid list. */
    private val loadedKids: StateFlow<List<String>?> = repository.kidNames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val kids: StateFlow<List<String>> = loadedKids
        .map { it.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** True once loaded with no kids yet, so the UI can prompt to add one. */
    val hasNoKids: StateFlow<Boolean> = loadedKids
        .map { it != null && it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /** The preferred kid if they still exist, otherwise the first kid; null when there are none. */
    private val _selectedKid: StateFlow<String?> = combine(loadedKids, preferredKid) { names, preferred ->
        when {
            names.isNullOrEmpty() || preferred == null -> null
            preferred.name != null && preferred.name in names -> preferred.name
            else -> names.first()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )
    val selectedKid: StateFlow<String?> = _selectedKid

    val categories: StateFlow<List<String>> = repository.categories
        .map { list ->
            if (list.isEmpty()) DefaultCategories.DEFAULT_CATEGORIES else list
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DefaultCategories.DEFAULT_CATEGORIES
        )

    /**
     * The category of the most recent entry of each type (across all kids), used to
     * pre-select the category in the entry form. Types with no entries are absent.
     */
    val lastCategoryByType: StateFlow<Map<TransactionType, String>> =
        repository.allTransactions
            .map { list ->
                // allTransactions is newest first, so the first match per type wins.
                list.groupBy { it.type }.mapValues { (_, entries) -> entries.first().category }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    /** Kids who have at least one entry. */
    val kidsWithEntries: StateFlow<Set<String>> =
        repository.allTransactions
            .map { list -> list.map { tx -> tx.kidName }.toSet() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    val usedCategories: StateFlow<Set<String>> =
        repository.allTransactions
            .map { list ->
                list.map { tx -> tx.category.lowercase() }.toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    /**
     * Lower-cased categories the user switched off in the charts. Tracking the
     * hidden ones (rather than the shown ones) means every category, including
     * ones added later, is shown by default.
     */
    private val _hiddenCategories = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCategories: StateFlow<Set<String>> = _hiddenCategories

    // Raw DatePicker selections (midnight UTC); see DateRanges.toLocalRange.
    private val _startDateMillis = MutableStateFlow<Long?>(null)
    val startDateMillis: StateFlow<Long?> = _startDateMillis
    private val _endDateMillis = MutableStateFlow<Long?>(null)
    val endDateMillis: StateFlow<Long?> = _endDateMillis

    private val _uiMessages = MutableSharedFlow<UiMessage>()
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    private suspend fun emitMessage(@StringRes resId: Int, vararg args: String) {
        _uiMessages.emit(UiMessage(resId, args.toList()))
    }

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded()
        }
    }

    /** The selected kid's balance and entries; null until they have loaded. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val ledger: StateFlow<LedgerState?> = _selectedKid
        .flatMapLatest { kidName ->
            if (kidName == null) {
                flowOf(null)
            } else {
                // Balance comes from the same list, so the two always match.
                repository.getTransactionsForKid(kidName).map { entries ->
                    val balance = entries.sumOf { if (it.type == TransactionType.CREDIT) it.amountCents else -it.amountCents }
                    LedgerState(balance, entries)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawCategoryIncome = combine(
        _selectedKid,
        _startDateMillis,
        _endDateMillis
    ) { kidName, start, end ->
        kidName to DateRanges.toLocalRange(start, end)
    }.flatMapLatest { (kidName, range) ->
        if (kidName == null) flowOf(emptyList())
        else repository.getCreditTotalsByCategoryForKidInRange(kidName, range.first, range.last)
    }

    /** Income per category in the date range, including categories hidden from the chart. */
    val incomeSums: StateFlow<List<CategorySum>> = rawCategoryIncome
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawCategorySpending = combine(
        _selectedKid,
        _startDateMillis,
        _endDateMillis
    ) { kidName, start, end ->
        kidName to DateRanges.toLocalRange(start, end)
    }.flatMapLatest { (kidName, range) ->
        if (kidName == null) flowOf(emptyList())
        else repository.getDebitTotalsByCategoryForKidInRange(kidName, range.first, range.last)
    }

    /** Spending per category in the date range, including categories hidden from the chart. */
    val spendingSums: StateFlow<List<CategorySum>> = rawCategorySpending
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun toggleCategorySelection(category: String) {
        val key = category.trim().lowercase()
        if (key.isBlank()) return
        _hiddenCategories.update { hidden ->
            if (key in hidden) hidden - key else hidden + key
        }
    }

    private fun showCategory(category: String) {
        _hiddenCategories.update { it - category.trim().lowercase() }
    }

    fun selectKid(kidName: String) {
        val trimmed = kidName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { selectedKidStore.setLastSelectedKid(trimmed) }
    }

    fun addKid(kidName: String) {
        val trimmed = kidName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertKid(trimmed)
            selectedKidStore.setLastSelectedKid(trimmed)
            emitMessage(R.string.message_switched_kid, trimmed)
        }
    }

    /** Removes the kid and all of their entries; the UI confirms first when they have entries. */
    fun removeKid(kidName: String) {
        viewModelScope.launch {
            repository.deleteKidAndEntries(kidName)
            emitMessage(R.string.message_kid_removed, kidName)
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        showCategory(trimmed)
        viewModelScope.launch {
            repository.addCategory(trimmed)
            emitMessage(R.string.message_category_added, trimmed)
        }
    }

    fun deleteUnusedCategory(name: String) {
        viewModelScope.launch {
            val deleted = repository.deleteUnusedCategory(name)
            if (deleted) {
                emitMessage(R.string.message_category_removed, name)
            } else {
                emitMessage(R.string.message_category_in_use, name)
            }
        }
    }

    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        _startDateMillis.value = startMillis
        _endDateMillis.value = endMillis
    }

    fun addTransaction(
        title: String,
        amountCents: Long,
        type: TransactionType,
        category: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val trimmedCategory = category.trim()
        if (title.isBlank() || amountCents <= 0L || trimmedCategory.isBlank()) return
        val currentKid = _selectedKid.value ?: return
        showCategory(trimmedCategory)
        viewModelScope.launch {
            val transaction = Transaction(
                kidName = currentKid,
                title = title.trim(),
                amountCents = amountCents,
                type = type,
                category = trimmedCategory,
                timestamp = timestamp
            )
            repository.insertTransaction(transaction)
        }
    }

    /** Saves changes to an existing entry (matched by id). */
    fun updateTransaction(transaction: Transaction) {
        showCategory(transaction.category)
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    /** Deletes right away; the snackbar offers Undo, which puts the entry back as it was. */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiMessages.emit(
                UiMessage(
                    resId = R.string.message_entry_deleted,
                    args = listOf(transaction.title),
                    onUndo = { viewModelScope.launch { repository.insertTransaction(transaction) } }
                )
            )
        }
    }

    fun exportToJson(outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportToJson(outputStream)
                .onSuccess { count ->
                    emitMessage(R.string.message_export_succeeded, count.toString())
                }
                .onFailure { error ->
                    emitMessage(R.string.message_export_failed, error.localizedMessage.orEmpty())
                }
        }
    }

    fun exportToCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportToCsv(outputStream)
                .onSuccess { count ->
                    emitMessage(R.string.message_csv_export_succeeded, count.toString())
                }
                .onFailure { error ->
                    emitMessage(R.string.message_csv_export_failed, error.localizedMessage.orEmpty())
                }
        }
    }

    fun importFromJson(inputStream: InputStream) {
        viewModelScope.launch {
            repository.importFromJson(inputStream, replaceExisting = true)
                .onSuccess { count ->
                    emitMessage(R.string.message_restore_succeeded, count.toString())
                }
                .onFailure { error ->
                    emitMessage(R.string.message_restore_failed, error.localizedMessage.orEmpty())
                }
        }
    }

    class Factory(
        private val repository: TransactionRepository,
        private val selectedKidStore: SelectedKidStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
                return TrackerViewModel(repository, selectedKidStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
