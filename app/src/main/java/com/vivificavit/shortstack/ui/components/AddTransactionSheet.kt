package com.vivificavit.shortstack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.ui.theme.CreditContainer
import com.vivificavit.shortstack.ui.theme.CreditGreen
import com.vivificavit.shortstack.ui.theme.DebitContainer
import com.vivificavit.shortstack.ui.theme.DebitRed
import com.vivificavit.shortstack.ui.theme.OnCreditContainer
import com.vivificavit.shortstack.ui.theme.OnDebitContainer
import com.vivificavit.shortstack.util.DateRanges
import com.vivificavit.shortstack.util.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Adds a new entry, or edits [existing] when it's given (which also offers Delete).
 * [onSave] gets the entry's fields; the caller decides whether to insert or update.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryBottomSheet(
    kidName: String,
    categories: List<String>,
    lastCategoryByType: Map<TransactionType, String>,
    existing: Transaction?,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onSave: (title: String, amountCents: Long, type: TransactionType, category: String, timestamp: Long) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }
    var selectedType by rememberSaveable { mutableStateOf(existing?.type ?: TransactionType.CREDIT) }
    // Suggest the last category used for this type, if it still exists.
    fun suggestedCategory(type: TransactionType): String? =
        lastCategoryByType[type]?.takeIf { it in categories }
    var selectedCategory by rememberSaveable {
        mutableStateOf(existing?.category ?: suggestedCategory(TransactionType.CREDIT))
    }
    // Once the user picks a category (or is editing one), switching type no longer replaces it.
    var categoryChosenByUser by rememberSaveable { mutableStateOf(existing != null) }
    var amountInput by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amountCents) } ?: "") }
    var noteInput by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var entryDate by rememberSaveable {
        mutableStateOf(existing?.let { DateRanges.timestampToLocalDate(it.timestamp) } ?: today)
    }
    var inputError by remember { mutableStateOf<Int?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var addingCategory by rememberSaveable { mutableStateOf(false) }

    // Shared by the Save button and the keyboard's Done key on the description field.
    fun trySave() {
        val parsedCents = Money.parseToCents(amountInput)
        val category = selectedCategory
        when {
            parsedCents == null -> inputError = R.string.entry_amount_error
            noteInput.isBlank() -> inputError = R.string.entry_note_error
            category == null -> inputError = R.string.entry_category_error
            else -> {
                // Keep the original time of day (or now, for a new entry) on the chosen date.
                val baseTimestamp = existing?.timestamp ?: System.currentTimeMillis()
                val timestamp = DateRanges.moveToDate(baseTimestamp, entryDate)
                onSave(noteInput.trim(), parsedCents, selectedType, category, timestamp)
            }
        }
    }

    // A new entry opens with the cursor in Amount and the number keyboard up.
    val amountFocus = remember { FocusRequester() }
    if (existing == null) {
        LaunchedEffect(Unit) { amountFocus.requestFocus() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // Scrolls so Save stays reachable in landscape, with large text or with many categories.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    if (existing == null) R.string.entry_title else R.string.entry_edit_title,
                    kidName
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val segments = listOf(
                    TransactionType.CREDIT to R.string.entry_type_income,
                    TransactionType.DEBIT to R.string.entry_type_expense
                )
                segments.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            if (!categoryChosenByUser) selectedCategory = suggestedCategory(type)
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = segments.size
                        ),
                        colors = if (type == TransactionType.CREDIT) {
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = CreditContainer,
                                activeContentColor = OnCreditContainer
                            )
                        } else {
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = DebitContainer,
                                activeContentColor = OnDebitContainer
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(label),
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
                label = { Text(stringResource(R.string.entry_amount_label)) },
                placeholder = { Text(stringResource(R.string.entry_amount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocus)
            )

            OutlinedTextField(
                value = noteInput,
                onValueChange = {
                    noteInput = it
                    inputError = null
                },
                label = { Text(stringResource(R.string.entry_note_label)) },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.CREDIT) {
                            stringResource(R.string.entry_note_hint_income)
                        } else {
                            stringResource(R.string.entry_note_hint_expense)
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { trySave() }),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.entry_date,
                        if (entryDate == today) {
                            stringResource(R.string.entry_date_today)
                        } else {
                            entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        }
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.entry_category_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show a just-added category right away, before the saved list catches up.
                    val chipCategories = selectedCategory
                        ?.takeIf { picked -> categories.none { it.equals(picked, ignoreCase = true) } }
                        ?.let { categories + it }
                        ?: categories
                    chipCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                                categoryChosenByUser = true
                                inputError = null
                            },
                            label = { Text(category) }
                        )
                    }
                    AssistChip(
                        onClick = { addingCategory = true },
                        label = { Text(stringResource(R.string.entry_new_category)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }

            inputError?.let { errorRes ->
                Text(
                    text = stringResource(errorRes, Money.format(1L), Money.format(Money.MAX_ENTRY_CENTS)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { trySave() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.CREDIT) CreditGreen else DebitRed
                )
            ) {
                Text(
                    text = stringResource(R.string.entry_save),
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            if (existing != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.entry_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateRanges.localDateToPickerMillis(entryDate),
            // No future dates: an entry records something that already happened.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !DateRanges.pickerMillisToLocalDate(utcTimeMillis).isAfter(today)

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            entryDate = DateRanges.pickerMillisToLocalDate(it)
                        }
                        pickingDate = false
                    }
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (addingCategory) {
        NewCategoryDialog(
            onDismiss = { addingCategory = false },
            onConfirm = { name ->
                val match = categories.firstOrNull { it.equals(name, ignoreCase = true) }
                if (match == null) onAddCategory(name)
                selectedCategory = match ?: name
                categoryChosenByUser = true
                inputError = null
                addingCategory = false
            }
        )
    }
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    fun confirm() {
        if (nameInput.isNotBlank()) onConfirm(nameInput.trim())
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_categories_new), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
        },
        confirmButton = {
            Button(onClick = { confirm() }, enabled = nameInput.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
