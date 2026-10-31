export interface KotlinSourceFile {
  path: string;
  packageName: string;
  summary: string;
  code: string;
}

export const KOTLIN_SOURCE_FILES: KotlinSourceFile[] = [
  {
    path: 'data/model/Transaction.kt',
    packageName: 'com.example.allowancetracker.data.model',
    summary:
      'Room @Entity and @Serializable Transaction, KidProfile, and CategoryEntity models with unified DefaultCategories pool.',
    code: `package com.example.allowancetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    CREDIT,
    DEBIT
}

@Serializable
@Entity(
    tableName = "kids",
    indices = [Index(value = ["name"], unique = true)]
)
data class KidProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String
)

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kidName: String = DEFAULT_KID_NAME,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String = DefaultCategories.DEFAULT_CATEGORIES.first(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0.0) { "Transaction amount must always be positive." }
        require(title.isNotBlank()) { "Transaction title cannot be blank." }
        require(kidName.isNotBlank()) { "Kid name cannot be blank." }
    }

    companion object {
        const val DEFAULT_KID_NAME = "Maya"
    }
}

object DefaultCategories {
    val DEFAULT_CATEGORIES: List<String> = listOf(
        "Allowance",
        "Gift",
        "Job",
        "Video Game",
        "Toy",
        "Book"
    )
}`,
  },
  {
    path: 'data/local/TransactionDao.kt',
    packageName: 'com.example.allowancetracker.data.local',
    summary:
      'Room DAO with multi-kid profile queries, kid-scoped running balance, and date-range debit category aggregation.',
    code: `package com.example.allowancetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

data class CategorySum(
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "total")
    val total: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT name FROM kids ORDER BY createdAt ASC, id ASC")
    fun getAllKidNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKid(kid: KidProfile): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE kidName = :kidName ORDER BY timestamp DESC, id DESC")
    fun getTransactionsForKid(kidName: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        """
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0.0 END), 0.0) -
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE kidName = :kidName
        """
    )
    fun getRunningBalanceForKid(kidName: String): Flow<Double?>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'DEBIT'
          AND kidName = :kidName
          AND timestamp >= :startTimestamp
          AND timestamp <= :endTimestamp
        GROUP BY category
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun getDebitTotalsByCategoryForKidInRange(
        kidName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>>
}`,
  },
  {
    path: 'data/local/AppDatabase.kt',
    packageName: 'com.example.allowancetracker.data.local',
    summary:
      'RoomDatabase singleton configured with TransactionTypeConverters and fallbackToDestructiveMigration().',
    code: `package com.example.allowancetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType

class TransactionTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        TransactionType.valueOf(value)
}

@Database(
    entities = [Transaction::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TransactionTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "allowance_tracker_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}`,
  },
  {
    path: 'repository/TransactionRepository.kt',
    packageName: 'com.example.allowancetracker.repository',
    summary:
      'Repository connecting TransactionDao flows and providing kotlinx.serialization JSON stream export and import.',
    code: `package com.example.allowancetracker.repository

import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.local.TransactionDao
import com.example.allowancetracker.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val jsonSerializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    val runningBalance: Flow<Double?> = transactionDao.getRunningBalance()

    val debitTotalsByCategory: Flow<List<CategorySum>> = transactionDao.getDebitTotalsByCategory()

    fun getDebitTotalsByCategoryInRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySum>> {
        return transactionDao.getDebitTotalsByCategoryInRange(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    suspend fun insertTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.insert(transaction)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.delete(transaction)
        }
    }

    suspend fun exportToJson(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTransactions = transactionDao.getAllTransactions().first()
            val jsonPayload = jsonSerializer.encodeToString(currentTransactions)
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }
            currentTransactions.size
        }
    }

    suspend fun importFromJson(
        inputStream: InputStream,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val importedTransactions = jsonSerializer.decodeFromString<List<Transaction>>(rawJson)
            if (replaceExisting) {
                transactionDao.deleteAll()
            }
            val normalizedTransactions = if (replaceExisting) {
                importedTransactions
            } else {
                importedTransactions.map { it.copy(id = 0L) }
            }
            transactionDao.insertAll(normalizedTransactions)
            normalizedTransactions.size
        }
    }
}`,
  },
  {
    path: 'ui/viewmodel/TrackerViewModel.kt',
    packageName: 'com.example.allowancetracker.ui.viewmodel',
    summary:
      'ViewModel exposing balance, filtered transactions, categorySpending, and selectedFilterTag StateFlows plus SAF stream actions.',
    code: `package com.example.allowancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class TrackerViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val selectedFilterTag: MutableStateFlow<String?> = MutableStateFlow(null)

    val startDateMillis: MutableStateFlow<Long?> = MutableStateFlow(null)
    val endDateMillis: MutableStateFlow<Long?> = MutableStateFlow(null)

    private val _uiMessages = MutableSharedFlow<String>()
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    val balance: StateFlow<Double> = repository.runningBalance
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
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

    val categorySpending: StateFlow<List<CategorySum>> = combine(
        startDateMillis,
        endDateMillis
    ) { start, end ->
        (start ?: 0L) to (end ?: Long.MAX_VALUE)
    }.flatMapLatest { (startTimestamp, endTimestamp) ->
        repository.getDebitTotalsByCategoryInRange(startTimestamp, endTimestamp)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        startDateMillis.value = startMillis
        endDateMillis.value = endMillis
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val transaction = Transaction(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                timestamp = timestamp
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiMessages.emit("Deleted \\"\${transaction.title}\\"")
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
                    _uiMessages.emit("Backup export failed: \${error.localizedMessage ?: "Unknown error"}")
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
                    _uiMessages.emit("Restore failed: \${error.localizedMessage ?: "Invalid JSON format"}")
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
            throw IllegalArgumentException("Unknown ViewModel class: \${modelClass.name}")
        }
    }
}`,
  },
  {
    path: 'ui/components/SpendingCharts.kt',
    packageName: 'com.example.allowancetracker.ui.components',
    summary:
      'Native Compose Canvas CategoryDonutChart and FlowRow CategoryLegend with animated sweeps and empty state handling.',
    code: `package com.example.allowancetracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.data.local.CategorySum
import java.text.NumberFormat
import java.util.Locale

@Composable
fun rememberCategoryColorMap(): Map<String, Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        mapOf(
            "Allowance" to Color(0xFF2E7D32),
            "Gift" to Color(0xFF00897B),
            "Snack" to Color(0xFFE65100),
            "Toy" to Color(0xFF6A1B9A),
            "Book" to Color(0xFF1565C0),
            "Other" to Color(0xFF546E7A)
        )
    }
}

fun getCategoryColor(
    category: String,
    colorMap: Map<String, Color>,
    fallbackPalette: List<Color>
): Color {
    return colorMap[category] ?: fallbackPalette[
        (category.hashCode() and Int.MAX_VALUE) % fallbackPalette.size
    ]
}

@Composable
fun CategoryDonutChart(
    categorySpending: List<CategorySum>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 180.dp,
    strokeWidth: Dp = 26.dp
) {
    val colorMap = rememberCategoryColorMap()
    val scheme = MaterialTheme.colorScheme
    val fallbackPalette = remember(scheme) {
        listOf(
            scheme.primary,
            scheme.secondary,
            scheme.tertiary,
            scheme.error,
            scheme.primaryContainer,
            scheme.secondaryContainer
        )
    }

    val totalSpending = remember(categorySpending) {
        categorySpending.sumOf { it.total }.coerceAtLeast(0.0)
    }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(categorySpending) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered && totalSpending > 0.0) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "DonutSweepAnimation"
    )

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeftOffset = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            if (totalSpending <= 0.0 || categorySpending.isEmpty()) {
                drawArc(
                    color = emptyRingColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                var currentStartAngle = -90f
                val gapDegrees = if (categorySpending.size > 1) 2.5f else 0f

                categorySpending.forEach { item ->
                    val proportion = (item.total / totalSpending).toFloat()
                    val rawSweep = proportion * 360f * animatedProgress
                    val adjustedSweep = (rawSweep - gapDegrees).coerceAtLeast(1f)
                    val segmentColor = getCategoryColor(item.category, colorMap, fallbackPalette)

                    drawArc(
                        color = segmentColor,
                        startAngle = currentStartAngle + (gapDegrees / 2f),
                        sweepAngle = adjustedSweep,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    currentStartAngle += rawSweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (totalSpending <= 0.0) {
                Text(
                    text = "No Spends",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$0.00",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormatter.format(totalSpending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    categorySpending: List<CategorySum>,
    modifier: Modifier = Modifier
) {
    val colorMap = rememberCategoryColorMap()
    val scheme = MaterialTheme.colorScheme
    val fallbackPalette = remember(scheme) {
        listOf(
            scheme.primary,
            scheme.secondary,
            scheme.tertiary,
            scheme.error,
            scheme.primaryContainer,
            scheme.secondaryContainer
        )
    }

    val totalSpending = remember(categorySpending) {
        categorySpending.sumOf { it.total }.coerceAtLeast(0.0)
    }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    if (categorySpending.isEmpty() || totalSpending <= 0.0) {
        Text(
            text = "Record a debit transaction to see spending breakdown by category.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorySpending.forEach { item ->
            val percentage = if (totalSpending > 0.0) {
                ((item.total / totalSpending) * 100.0).toInt()
            } else {
                0
            }
            val dotColor = getCategoryColor(item.category, colorMap, fallbackPalette)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\${currencyFormatter.format(item.total)} ($percentage%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}`,
  },
  {
    path: 'ui/screens/TrackerMainScreen.kt',
    packageName: 'com.example.allowancetracker.ui.screens',
    summary:
      'Main Jetpack Compose Material 3 screen with Ledger, Income, and Expenses tabs, matching category FilterChips per tab, persistent ExtendedFloatingActionButton ("+ Entry"), and ModalBottomSheet with unified categories.',
    code: `package com.example.allowancetracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.R
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel

enum class TrackerTab(val title: String) {
    RUNNING_TALLY("Ledger"),
    INCOME("Income"),
    EXPENSES("Expenses")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackerMainScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val matchingIncomeCategories by viewModel.matchingIncomeCategories.collectAsState()
    val matchingExpenseCategories by viewModel.matchingExpenseCategories.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(TrackerTab.RUNNING_TALLY.ordinal) }
    var showEntrySheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KidProfileSelectorDropdown(viewModel = viewModel)
                        Image(
                            painter = painterResource(id = R.drawable.bodad_logo),
                            contentDescription = "boDad Logo",
                            modifier = Modifier.height(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEntrySheet = true },
                expanded = true,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Entry") }
            )
        }
    ) { innerPadding ->
        // Ledger, Income (matchingIncomeCategories + Income Donut), and Expenses (matchingExpenseCategories + Expenses Donut) tabs...
    }

    if (showEntrySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedType by rememberSaveable { mutableStateOf(TransactionType.CREDIT) }
        var selectedCategory by rememberSaveable {
            mutableStateOf(categories.firstOrNull() ?: "Allowance")
        }
        var showNewCategoryPrompt by rememberSaveable { mutableStateOf(false) }
        var newCategoryName by rememberSaveable { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { showEntrySheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf("Income" to TransactionType.CREDIT, "Expense" to TransactionType.DEBIT)
                    options.forEachIndexed { index, (label, type) ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(label, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                    AssistChip(
                        onClick = {
                            showNewCategoryPrompt = true
                            newCategoryName = ""
                        },
                        label = { Text("+ New", fontWeight = FontWeight.Bold) }
                    )
                }
                if (showNewCategoryPrompt) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New Category Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newCategoryName.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addCategory(trimmed)
                                    selectedCategory = trimmed
                                    showNewCategoryPrompt = false
                                    newCategoryName = ""
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
                // Amount, Note inputs and "Save Entry" Button...
            }
        }
    }
}`,
  },
  {
    path: 'MainActivity.kt',
    packageName: 'com.example.allowancetracker',
    summary:
      'Entry point ComponentActivity wiring AppDatabase, TransactionRepository, TrackerViewModel.Factory, and TrackerMainScreen.',
    code: `package com.example.allowancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowancetracker.data.local.AppDatabase
import com.example.allowancetracker.repository.TransactionRepository
import com.example.allowancetracker.ui.screens.TrackerMainScreen
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TransactionRepository(database.transactionDao())
        val viewModelFactory = TrackerViewModel.Factory(repository)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val trackerViewModel: TrackerViewModel = viewModel(factory = viewModelFactory)
                TrackerMainScreen(viewModel = trackerViewModel)
            }
        }
    }
}`,
  },
];
