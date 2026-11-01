package com.vivificavit.shortstack.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.ui.components.AddKidDialog
import com.vivificavit.shortstack.ui.components.EntryBottomSheet
import com.vivificavit.shortstack.ui.components.EditKidsDialog
import com.vivificavit.shortstack.ui.components.ManageCategoriesDialog
import com.vivificavit.shortstack.ui.components.RestoreBackupDialog
import com.vivificavit.shortstack.ui.theme.EntryButtonNavy
import com.vivificavit.shortstack.ui.viewmodel.TrackerViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

enum class TrackerTab(@StringRes val titleRes: Int) {
    RUNNING_TALLY(R.string.tab_ledger),
    INCOME(R.string.tab_income),
    EXPENSES(R.string.tab_expenses)
}

/** Machine-readable, so the backup file name sorts by date in any locale. */
private val BackupFileTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerMainScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val kids by viewModel.kids.collectAsStateWithLifecycle()
    val selectedKid by viewModel.selectedKid.collectAsStateWithLifecycle()
    val hasNoKids by viewModel.hasNoKids.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val incomeSums by viewModel.incomeSums.collectAsStateWithLifecycle()
    val spendingSums by viewModel.spendingSums.collectAsStateWithLifecycle()
    val hiddenCategories by viewModel.hiddenCategories.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val usedCategories by viewModel.usedCategories.collectAsStateWithLifecycle()
    val kidsWithEntries by viewModel.kidsWithEntries.collectAsStateWithLifecycle()
    val lastCategoryByType by viewModel.lastCategoryByType.collectAsStateWithLifecycle()
    val startDateMillis by viewModel.startDateMillis.collectAsStateWithLifecycle()
    val endDateMillis by viewModel.endDateMillis.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var kidMenuExpanded by remember { mutableStateOf(false) }
    var showAddKidDialog by rememberSaveable { mutableStateOf(false) }
    var showEditKidsDialog by rememberSaveable { mutableStateOf(false) }
    var showManageCategoriesDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirm by rememberSaveable { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(TrackerTab.RUNNING_TALLY.ordinal) }
    var showEntrySheet by rememberSaveable { mutableStateOf(false) }
    // The id of the entry open for editing; null means the sheet adds a new one.
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            context.contentResolver.openOutputStream(targetUri)?.let { outputStream ->
                viewModel.exportToJson(outputStream)
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            context.contentResolver.openOutputStream(targetUri)?.let { outputStream ->
                viewModel.exportToCsv(outputStream)
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
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(message.resId, *message.args.toTypedArray()),
                actionLabel = message.onUndo?.let { resources.getString(R.string.action_undo) },
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) message.onUndo?.invoke()
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
                                    // Blank while loading, so "Add kid" only shows when there really are none.
                                    text = selectedKid
                                        ?: if (hasNoKids) stringResource(R.string.kid_menu_none) else "",
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.cd_select_kid)
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
                                                    contentDescription = stringResource(R.string.cd_selected)
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
                                            text = stringResource(R.string.menu_edit_kids),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        kidMenuExpanded = false
                                        showEditKidsDialog = true
                                    }
                                )
                            }
                        }
                    },
                    title = {
                        Image(
                            painter = painterResource(R.drawable.logo_shortstack),
                            contentDescription = stringResource(R.string.app_bar_title),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(28.dp)
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
                                contentDescription = stringResource(R.string.cd_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_export_backup)) },
                                onClick = {
                                    overflowExpanded = false
                                    val timestamp = LocalDateTime.now().format(BackupFileTimestamp)
                                    exportBackupLauncher.launch("shortstack_backup_$timestamp.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_export_csv)) },
                                onClick = {
                                    overflowExpanded = false
                                    val timestamp = LocalDateTime.now().format(BackupFileTimestamp)
                                    exportCsvLauncher.launch("shortstack_entries_$timestamp.csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_restore_backup)) },
                                onClick = {
                                    overflowExpanded = false
                                    showRestoreConfirm = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_manage_categories)) },
                                onClick = {
                                    overflowExpanded = false
                                    showManageCategoriesDialog = true
                                }
                            )
                        }
                    }
                )

                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    TrackerTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = stringResource(tab.titleRes),
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
            if (selectedKid != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingEntryId = null
                        showEntrySheet = true
                    },
                    expanded = true,
                    containerColor = EntryButtonNavy,
                    contentColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = { Text(text = stringResource(R.string.fab_entry), fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { innerPadding ->
        val kidName = selectedKid
        if (kidName == null) {
            if (hasNoKids) {
                AddFirstKidContent(
                    onAddKid = { showAddKidDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            return@Scaffold
        }
        when (TrackerTab.entries[selectedTabIndex]) {
            TrackerTab.RUNNING_TALLY -> {
                RunningTallyTabContent(
                    kidName = kidName,
                    ledger = ledger,
                    onEditTransaction = { transaction ->
                        editingEntryId = transaction.id
                        showEntrySheet = true
                    },
                    onDeleteTransaction = viewModel::deleteTransaction,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            TrackerTab.INCOME -> {
                CategoryBreakdownTabContent(
                    kind = BreakdownKind.INCOME,
                    allSums = incomeSums,
                    hiddenCategories = hiddenCategories,
                    onToggleCategory = viewModel::toggleCategorySelection,
                    allCategories = categories,
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
                    kind = BreakdownKind.EXPENSES,
                    allSums = spendingSums,
                    hiddenCategories = hiddenCategories,
                    onToggleCategory = viewModel::toggleCategorySelection,
                    allCategories = categories,
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

    if (showEditKidsDialog) {
        EditKidsDialog(
            kids = kids,
            kidsWithEntries = kidsWithEntries,
            onAddKid = viewModel::addKid,
            onRemoveKid = viewModel::removeKid,
            onDismiss = { showEditKidsDialog = false }
        )
    }

    if (showRestoreConfirm) {
        RestoreBackupDialog(
            onConfirm = {
                showRestoreConfirm = false
                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = { showRestoreConfirm = false }
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

    val entryKid = selectedKid
    // While editing, wait for the entry to be found in the loaded ledger.
    val editingEntry = editingEntryId?.let { id -> ledger?.transactions?.firstOrNull { it.id == id } }
    if (showEntrySheet && entryKid != null && (editingEntryId == null || editingEntry != null)) {
        fun closeSheet() {
            showEntrySheet = false
            editingEntryId = null
        }
        // Keyed so switching between entries starts the form fresh.
        key(editingEntryId) {
            EntryBottomSheet(
                kidName = entryKid,
                categories = categories,
                lastCategoryByType = lastCategoryByType,
                existing = editingEntry,
                onDismiss = ::closeSheet,
                onAddCategory = viewModel::addCategory,
                onSave = { title, amountCents, type, category, timestamp ->
                    if (editingEntry == null) {
                        viewModel.addTransaction(
                            title = title,
                            amountCents = amountCents,
                            type = type,
                            category = category,
                            timestamp = timestamp
                        )
                    } else {
                        viewModel.updateTransaction(
                            editingEntry.copy(
                                title = title,
                                amountCents = amountCents,
                                type = type,
                                category = category,
                                timestamp = timestamp
                            )
                        )
                    }
                    closeSheet()
                },
                onDelete = {
                    editingEntry?.let(viewModel::deleteTransaction)
                    closeSheet()
                }
            )
        }
    }
}

@Composable
fun AddFirstKidContent(
    onAddKid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddKid) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.welcome_add_kid))
        }
    }
}
