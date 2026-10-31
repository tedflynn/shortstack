package com.example.allowancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.R
import com.example.allowancetracker.data.model.TransactionType
import com.example.allowancetracker.util.Money

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    kidName: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSaveTransaction: (title: String, amountCents: Long, type: TransactionType, category: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.CREDIT) }
    val defaultCat = categories.firstOrNull() ?: "Allowance"
    var selectedCategory by rememberSaveable { mutableStateOf(defaultCat) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<Int?>(null) }

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
                text = stringResource(R.string.entry_title, kidName),
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
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = segments.size
                        ),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            )

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
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
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
                onClick = {
                    val parsedCents = Money.parseToCents(amountInput)
                    when {
                        parsedCents == null -> {
                            inputError = R.string.entry_amount_error
                        }
                        noteInput.isBlank() -> {
                            inputError = R.string.entry_note_error
                        }
                        else -> {
                            onSaveTransaction(
                                noteInput.trim(),
                                parsedCents,
                                selectedType,
                                selectedCategory
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.entry_save),
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
