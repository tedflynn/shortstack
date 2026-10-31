package com.example.allowancetracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.data.model.Transaction
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.ui.components.CategoryDonutChart
import com.example.allowancetracker.ui.components.CategoryLegend
import com.example.allowancetracker.ui.components.getCategoryColor
import com.example.allowancetracker.ui.components.rememberCategoryColorMap
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CreditGreen = Color(0xFF2E7D32)
private val DebitRed = Color(0xFFC62828)

enum class TrackerTab(val title: String) {
    RUNNING_TALLY("Ledger"),
    INCOME("Income"),
    EXPENSES("Expenses")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerMainScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kids by viewModel.kids.collectAsState()
    val selectedKid by viewModel.selectedKid.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categoryIncome by viewModel.categoryIncome.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val matchingIncomeCategories by viewModel.matchingIncomeCategories.collectAsState()
    val matchingExpenseCategories by viewModel.matchingExpenseCategories.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filterBarCategories by viewModel.filterBarCategories.collectAsState()
    val usedCategories by viewModel.usedCategories.collectAsState()
    val selectedFilterTag by viewModel.selectedFilterTag.collectAsState()
    val startDateMillis by viewModel.startDateMillis.collectAsState()
    val endDateMillis by viewModel.endDateMillis.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var kidMenuExpanded by remember { mutableStateOf(false) }
    var showAddKidDialog by rememberSaveable { mutableStateOf(false) }
    var showManageCategoriesDialog by rememberSaveable { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(TrackerTab.RUNNING_TALLY.ordinal) }
    var showEntrySheet by rememberSaveable { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            context.contentResolver.openOutputStream(targetUri)?.let { outputStream ->
                viewModel.exportToJson(outputStream)
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            context.contentResolver.openInputStream(sourceUri)?.let { inputStream ->
                viewModel.importFromJson(inputStream)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            OutlinedButton(
                                onClick = { kidMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedKid,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Kid"
                                )
                            }

                            DropdownMenu(
                                expanded = kidMenuExpanded,
                                onDismissRequest = { kidMenuExpanded = false }
                            ) {
                                kids.forEach { kidName ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = kidName,
                                                fontWeight = if (kidName == selectedKid) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        trailingIcon = {
                                            if (kidName == selectedKid) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected"
                                                )
                                            }
                                        },
                                        onClick = {
                                            kidMenuExpanded = false
                                            viewModel.selectKid(kidName)
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "+ Kid",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        kidMenuExpanded = false
                                        showAddKidDialog = true
                                    }
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = "Allowance Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Backup Options"
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Backup to Drive") },
                                onClick = {
                                    overflowExpanded = false
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                                        .format(Date())
                                    exportBackupLauncher.launch("allowance_backup_$timestamp.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restore Backup") },
                                onClick = {
                                    overflowExpanded = false
                                    restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                        }
                    }
                )

                TabRow(selectedTabIndex = selectedTabIndex) {
                    TrackerTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTabIndex == index) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEntrySheet = true },
                expanded = true,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text(text = "Entry", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        when (TrackerTab.entries[selectedTabIndex]) {
            TrackerTab.RUNNING_TALLY -> {
                RunningTallyTabContent(
                    kidName = selectedKid,
                    balance = balance,
                    transactions = transactions,
                    filterCategories = filterBarCategories,
                    selectedFilterTag = selectedFilterTag,
                    onSelectFilterTag = viewModel::setFilterTag,
                    onOpenManageCategories = { showManageCategoriesDialog = true },
                    onDeleteTransaction = viewModel::deleteTransaction,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            TrackerTab.INCOME -> {
                CategoryBreakdownTabContent(
                    kidName = selectedKid,
                    tabTitle = "Income",
                    accentColor = CreditGreen,
                    matchingCategories = matchingIncomeCategories,
                    selectedCategories = selectedCategories,
                    onToggleCategory = viewModel::toggleCategorySelection,
                    categorySums = categoryIncome,
                    totalLabel = "Total Income",
                    emptyLabel = "No Income",
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    onDateRangeSelected = viewModel::setDateRange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            TrackerTab.EXPENSES -> {
                CategoryBreakdownTabContent(
                    kidName = selectedKid,
                    tabTitle = "Expenses",
                    accentColor = DebitRed,
                    matchingCategories = matchingExpenseCategories,
                    selectedCategories = selectedCategories,
                    onToggleCategory = viewModel::toggleCategorySelection,
                    categorySums = categorySpending,
                    totalLabel = "Total Expenses",
                    emptyLabel = "No Expenses",
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    onDateRangeSelected = viewModel::setDateRange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showAddKidDialog) {
        AddKidDialog(
            onDismiss = { showAddKidDialog = false },
            onConfirmKid = { newKidName ->
                viewModel.addKid(newKidName)
                showAddKidDialog = false
            }
        )
    }

    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = categories,
            usedCategories = usedCategories,
            onAddCategory = viewModel::addCategory,
            onDeleteUnusedCategory = viewModel::deleteUnusedCategory,
            onDismiss = { showManageCategoriesDialog = false }
        )
    }

    if (showEntrySheet) {
        AddTransactionBottomSheet(
            kidName = selectedKid,
            categories = categories,
            onDismiss = { showEntrySheet = false },
            onSaveTransaction = { title, amount, type, category ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category
                )
                showEntrySheet = false
            }
        )
    }
}

@Composable
fun CategoryFilterBar(
    categories: List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    onOpenManageCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterOptions = remember(categories) {
        listOf("All") + categories
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filterOptions) { tag ->
            val isSelected = if (tag == "All") {
                selectedTag == null
            } else {
                selectedTag.equals(tag, ignoreCase = true)
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectTag(if (tag == "All") null else tag) },
                label = { Text(text = tag) }
            )
        }

        item(key = "manage_categories_chip") {
            AssistChip(
                onClick = onOpenManageCategories,
                label = {
                    Text(
                        text = "+/-",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

@Composable
fun ManageCategoriesDialog(
    categories: List<String>,
    usedCategories: Set<String>,
    onAddCategory: (String) -> Unit,
    onDeleteUnusedCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        label = { Text("New Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCategoryInput.isNotBlank()) {
                                onAddCategory(newCategoryInput.trim())
                                newCategoryInput = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories, key = { it }) { catName ->
                        val isUsed = usedCategories.contains(catName.lowercase())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isUsed) "In use" else "Unused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteUnusedCategory(catName) },
                                enabled = !isUsed
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete $catName"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun AddKidDialog(
    onDismiss: () -> Unit,
    onConfirmKid: (String) -> Unit
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Kid Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        nameError = null
                    },
                    label = { Text("Kid's Name") },
                    placeholder = { Text("e.g., Leo, Sam...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isBlank()) {
                        nameError = "Please enter a name."
                    } else {
                        onConfirmKid(nameInput.trim())
                    }
                }
            ) {
                Text("Add Kid")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RunningTallyTabContent(
    kidName: String,
    balance: Double,
    transactions: List<Transaction>,
    filterCategories: List<String>,
    selectedFilterTag: String?,
    onSelectFilterTag: (String?) -> Unit,
    onOpenManageCategories: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header_card") {
            BalanceHeaderCard(
                kidName = kidName,
                runningBalance = balance
            )
        }

        item(key = "filter_bar") {
            CategoryFilterBar(
                categories = filterCategories,
                selectedTag = selectedFilterTag,
                onSelectTag = onSelectFilterTag,
                onOpenManageCategories = onOpenManageCategories
            )
        }

        if (transactions.isEmpty()) {
            item(key = "empty_state") {
                EmptyTransactionsCard(selectedFilter = selectedFilterTag)
            }
        } else {
            items(
                items = transactions,
                key = { transaction -> transaction.id }
            ) { transaction ->
                SwipeableTransactionItem(
                    transaction = transaction,
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBreakdownTabContent(
    kidName: String,
    tabTitle: String,
    accentColor: Color,
    matchingCategories: List<String>,
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    categorySums: List<CategorySum>,
    totalLabel: String,
    emptyLabel: String,
    startDateMillis: Long?,
    endDateMillis: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    var pickingStartDate by rememberSaveable { mutableStateOf(false) }
    var pickingEndDate by rememberSaveable { mutableStateOf(false) }

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

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "date_range_selector_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Summary Date Range",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select start and end dates for $kidName's ${tabTitle.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (startDateMillis != null || endDateMillis != null) {
                            TextButton(onClick = { onDateRangeSelected(null, null) }) {
                                Text("Reset All")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickingStartDate = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = startDateMillis?.let { dateFormatter.format(Date(it)) }
                                    ?: "Start Date"
                            )
                        }

                        OutlinedButton(
                            onClick = { pickingEndDate = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = endDateMillis?.let { dateFormatter.format(Date(it)) }
                                    ?: "End Date"
                            )
                        }
                    }
                }
            }
        }

        item(key = "matching_categories_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$tabTitle Categories",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    if (matchingCategories.isEmpty()) {
                        Text(
                            text = "No matching ${tabTitle.lowercase()} categories in this date range",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            matchingCategories.forEach { cat ->
                                val isSelected = selectedCategories.any {
                                    it.equals(cat, ignoreCase = true)
                                }
                                val chipColor = getCategoryColor(cat, colorMap, fallbackPalette)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleCategory(cat) },
                                    label = { Text(text = cat) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Transparent,
                                        labelColor = chipColor,
                                        selectedContainerColor = chipColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = chipColor,
                                        selectedBorderColor = chipColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "donut_chart_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$kidName's $tabTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildDateRangeSubtitle(
                            startDateMillis,
                            endDateMillis,
                            dateFormatter,
                            tabTitle
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryDonutChart(
                        categorySpending = categorySums,
                        totalLabel = totalLabel,
                        emptyLabel = emptyLabel,
                        chartSize = 156.dp,
                        strokeWidth = 22.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryLegend(categorySpending = categorySums)
                }
            }
        }
    }

    if (pickingStartDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeSelected(pickerState.selectedDateMillis, endDateMillis)
                        pickingStartDate = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (pickingEndDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeSelected(startDateMillis, pickerState.selectedDateMillis)
                        pickingEndDate = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun buildDateRangeSubtitle(
    startDateMillis: Long?,
    endDateMillis: Long?,
    formatter: SimpleDateFormat,
    tabTitle: String
): String {
    val startText = startDateMillis?.let { formatter.format(Date(it)) } ?: "Beginning"
    val endText = endDateMillis?.let { formatter.format(Date(it)) } ?: "Today"
    return if (startDateMillis == null && endDateMillis == null) {
        "All recorded ${tabTitle.lowercase()}"
    } else {
        "$startText – $endText"
    }
}

@Composable
fun BalanceHeaderCard(
    kidName: String,
    runningBalance: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val balanceColor = if (runningBalance < 0.0) {
        DebitRed
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "$kidName's Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currencyFormatter.format(runningBalance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = balanceColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            if (targetValue == SwipeToDismissBoxValue.EndToStart ||
                targetValue == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Transaction",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            TransactionRowCard(
                transaction = transaction,
                onDeleteClick = onDelete
            )
        }
    )
}

@Composable
fun TransactionRowCard(
    transaction: Transaction,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US) }

    val isCredit = transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) CreditGreen else DebitRed
    val formattedSignedAmount = if (isCredit) {
        "+${currencyFormatter.format(transaction.amount)}"
    } else {
        "-${currencyFormatter.format(transaction.amount)}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCredit) "+" else "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = dateFormatter.format(Date(transaction.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedSignedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${transaction.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard(
    selectedFilter: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (selectedFilter == null) {
                    "No transactions recorded yet"
                } else {
                    "No transactions in \"$selectedFilter\""
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap '+ Entry' below to add an entry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    kidName: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSaveTransaction: (title: String, amount: Double, type: TransactionType, category: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.CREDIT) }
    val defaultCat = categories.firstOrNull() ?: "Allowance"
    var selectedCategory by rememberSaveable { mutableStateOf(defaultCat) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New Entry for $kidName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val segments = listOf(
                    TransactionType.CREDIT to "Income",
                    TransactionType.DEBIT to "Expense"
                )
                segments.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = segments.size
                        ),
                        label = {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountInput = newValue
                        inputError = null
                    }
                },
                label = { Text("Amount ($)") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = noteInput,
                onValueChange = {
                    noteInput = it
                    inputError = null
                },
                label = { Text("Item Note / Description") },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.CREDIT) {
                            "Weekly allowance, Birthday gift, Lawn mowing..."
                        } else {
                            "Video game, Lego set, Book..."
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (inputError != null) {
                Text(
                    text = inputError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val parsedAmount = amountInput.toDoubleOrNull()
                    when {
                        parsedAmount == null || parsedAmount <= 0.0 -> {
                            inputError = "Please enter a valid positive dollar amount."
                        }
                        noteInput.isBlank() -> {
                            inputError = "Please enter a note for this transaction."
                        }
                        else -> {
                            onSaveTransaction(
                                noteInput.trim(),
                                parsedAmount,
                                selectedType,
                                selectedCategory
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Save Entry",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
