package com.example.allowancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.local.SelectedKidStore
import com.example.allowancetracker.data.model.DefaultCategories
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.repository.TransactionRepository
import com.example.allowancetracker.util.DateRanges
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

    /** The kid the user last picked; may be stale if that kid was removed. */
    private val _preferredKid = MutableStateFlow(selectedKidStore.lastSelectedKid)

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
    private val _selectedKid: StateFlow<String?> = combine(loadedKids, _preferredKid) { names, preferred ->
        when {
            names.isNullOrEmpty() -> null
            preferred != null && preferred in names -> preferred
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

    val filterBarCategories: StateFlow<List<String>> = categories
        .map { list -> list.distinct() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DefaultCategories.DEFAULT_CATEGORIES
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

    val selectedFilterTag: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Lower-cased categories the user switched off in the charts. Tracking the
     * hidden ones (rather than the shown ones) means every category, including
     * ones added later, is shown by default.
     */
    private val _hiddenCategories = MutableStateFlow<Set<String>>(emptySet())

    // Raw DatePicker selections (midnight UTC); see DateRanges.toLocalRange.
    val startDateMillis: MutableStateFlow<Long?> = MutableStateFlow(null)
    val endDateMillis: MutableStateFlow<Long?> = MutableStateFlow(null)

    private val _uiMessages = MutableSharedFlow<String>()
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val balanceCents: StateFlow<Long> = _selectedKid
        .flatMapLatest { kidName ->
            if (kidName == null) flowOf(0L)
            else repository.getRunningBalanceForKid(kidName).map { it ?: 0L }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<Transaction>> = _selectedKid
        .flatMapLatest { kidName ->
            if (kidName == null) flowOf(emptyList())
            else repository.getTransactionsForKid(kidName)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        selectedFilterTag
    ) { list, filterTag ->
        if (filterTag.isNullOrBlank() || filterTag.equals("All", ignoreCase = true)) {
            list
        } else {
            list.filter { it.category.equals(filterTag, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawCategoryIncome = combine(
        _selectedKid,
        startDateMillis,
        endDateMillis
    ) { kidName, start, end ->
        kidName to DateRanges.toLocalRange(start, end)
    }.flatMapLatest { (kidName, range) ->
        if (kidName == null) flowOf(emptyList())
        else repository.getCreditTotalsByCategoryForKidInRange(kidName, range.first, range.last)
    }

    val matchingIncomeCategories: StateFlow<List<String>> = rawCategoryIncome
        .map { sums -> sums.filter { it.totalCents > 0 }.map { it.category } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val categoryIncome: StateFlow<List<CategorySum>> = combine(
        rawCategoryIncome,
        _hiddenCategories
    ) { sums, hidden ->
        sums.filter { it.category.lowercase() !in hidden }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawCategorySpending = combine(
        _selectedKid,
        startDateMillis,
        endDateMillis
    ) { kidName, start, end ->
        kidName to DateRanges.toLocalRange(start, end)
    }.flatMapLatest { (kidName, range) ->
        if (kidName == null) flowOf(emptyList())
        else repository.getDebitTotalsByCategoryForKidInRange(kidName, range.first, range.last)
    }

    val matchingExpenseCategories: StateFlow<List<String>> = rawCategorySpending
        .map { sums -> sums.filter { it.totalCents > 0 }.map { it.category } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val categorySpending: StateFlow<List<CategorySum>> = combine(
        rawCategorySpending,
        _hiddenCategories
    ) { sums, hidden ->
        sums.filter { it.category.lowercase() !in hidden }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Categories currently shown in the charts (all of them unless switched off). */
    val selectedCategories: StateFlow<Set<String>> = combine(
        matchingIncomeCategories,
        matchingExpenseCategories,
        _hiddenCategories
    ) { income, expenses, hidden ->
        (income + expenses).filter { it.lowercase() !in hidden }.toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
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
        _preferredKid.value = trimmed
        selectedKidStore.lastSelectedKid = trimmed
    }

    fun addKid(kidName: String) {
        val trimmed = kidName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertKid(trimmed)
            selectKid(trimmed)
            _uiMessages.emit("Switched to $trimmed's ledger")
        }
    }

    fun removeKidIfUnused(kidName: String) {
        viewModelScope.launch {
            if (repository.deleteUnusedKid(kidName)) {
                _uiMessages.emit("Removed $kidName")
            } else {
                _uiMessages.emit("Can't remove $kidName because they have ledger entries")
            }
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        showCategory(trimmed)
        viewModelScope.launch {
            repository.addCategory(trimmed)
            _uiMessages.emit("Added \"$trimmed\" to categories")
        }
    }

    fun deleteUnusedCategory(name: String) {
        viewModelScope.launch {
            val deleted = repository.deleteUnusedCategory(name)
            if (deleted) {
                if (selectedFilterTag.value.equals(name, ignoreCase = true)) {
                    selectedFilterTag.value = null
                }
                _uiMessages.emit("Removed unused category \"$name\"")
            } else {
                _uiMessages.emit("Cannot delete \"$name\" because it is used in transactions")
            }
        }
    }

    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        startDateMillis.value = startMillis
        endDateMillis.value = endMillis
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

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiMessages.emit("Deleted \"${transaction.title}\"")
        }
    }

    fun setFilterTag(tag: String?) {
        selectedFilterTag.value = if (tag.equals("All", ignoreCase = true)) null else tag
    }

    fun exportToJson(outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportToJson(outputStream)
                .onSuccess { count ->
                    _uiMessages.emit("Exported $count transactions to backup")
                }
                .onFailure { error ->
                    _uiMessages.emit("Backup export failed: ${error.localizedMessage ?: "Unknown error"}")
                }
        }
    }

    fun importFromJson(inputStream: InputStream) {
        viewModelScope.launch {
            repository.importFromJson(inputStream, replaceExisting = true)
                .onSuccess { count ->
                    _uiMessages.emit("Restored $count transactions from backup")
                }
                .onFailure { error ->
                    _uiMessages.emit("Restore failed: ${error.localizedMessage ?: "Invalid JSON format"}")
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
