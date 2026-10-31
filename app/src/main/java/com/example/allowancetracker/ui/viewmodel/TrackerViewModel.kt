package com.example.allowancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.allowancetracker.data.local.CategorySum
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class TrackerViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedKid = MutableStateFlow(Transaction.DEFAULT_KID_NAME)
    val selectedKid: StateFlow<String> = _selectedKid.asStateFlow()

    val kids: StateFlow<List<String>> = repository.kidNames
        .map { names ->
            if (names.isEmpty()) listOf(Transaction.DEFAULT_KID_NAME) else names
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = listOf(Transaction.DEFAULT_KID_NAME)
        )

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

    private val _selectedCategories = MutableStateFlow<Set<String>>(
        DefaultCategories.DEFAULT_CATEGORIES.toSet()
    )
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

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
            repository.getRunningBalanceForKid(kidName).map { it ?: 0L }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<Transaction>> = _selectedKid
        .flatMapLatest { kidName ->
            repository.getTransactionsForKid(kidName)
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
        repository.getCreditTotalsByCategoryForKidInRange(kidName, range.first, range.last)
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
        _selectedCategories
    ) { sums, selectedSet ->
        val normalizedSelected = selectedSet.map { it.lowercase() }.toSet()
        sums.filter { normalizedSelected.contains(it.category.lowercase()) }
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
        repository.getDebitTotalsByCategoryForKidInRange(kidName, range.first, range.last)
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
        _selectedCategories
    ) { sums, selectedSet ->
        val normalizedSelected = selectedSet.map { it.lowercase() }.toSet()
        sums.filter { normalizedSelected.contains(it.category.lowercase()) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun toggleCategorySelection(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        _selectedCategories.update { current ->
            val match = current.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            if (match != null) {
                current - match
            } else {
                current + trimmed
            }
        }
    }

    fun selectKid(kidName: String) {
        if (kidName.isBlank()) return
        _selectedKid.value = kidName.trim()
    }

    fun addKid(kidName: String) {
        val trimmed = kidName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertKid(trimmed)
            _selectedKid.value = trimmed
            _uiMessages.emit("Switched to $trimmed's ledger")
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _selectedCategories.update { it + trimmed }
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
        val currentKid = _selectedKid.value
        _selectedCategories.update { it + trimmedCategory }
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
        private val repository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
                return TrackerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
