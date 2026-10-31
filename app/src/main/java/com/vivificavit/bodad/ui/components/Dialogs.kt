package com.vivificavit.bodad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.vivificavit.bodad.R

@Composable
fun ManageCategoriesDialog(
    categories: List<String>,
    usedCategories: Set<String>,
    onAddCategory: (String) -> Unit,
    onDeleteUnusedCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryInput by rememberSaveable { mutableStateOf("") }
    fun addCategory() {
        if (newCategoryInput.isNotBlank()) {
            onAddCategory(newCategoryInput.trim())
            newCategoryInput = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_categories_title), fontWeight = FontWeight.Bold) },
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
                        label = { Text(stringResource(R.string.manage_categories_new)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { addCategory() }),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { addCategory() }) {
                        Text(stringResource(R.string.action_add))
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
                                    text = if (isUsed) {
                                        stringResource(R.string.category_in_use)
                                    } else {
                                        stringResource(R.string.category_unused)
                                    },
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
                                    contentDescription = stringResource(R.string.cd_delete_item, catName)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
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
    var nameError by remember { mutableStateOf<Int?>(null) }
    fun confirm() {
        if (nameInput.isBlank()) {
            nameError = R.string.add_kid_name_error
        } else {
            onConfirmKid(nameInput.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.add_kid_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.add_kid_name_label)) },
                    placeholder = { Text(stringResource(R.string.add_kid_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth()
                )
                nameError?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { confirm() }) {
                Text(stringResource(R.string.add_kid_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Add or remove kids; removing a kid who has entries asks for confirmation first. */
@Composable
fun EditKidsDialog(
    kids: List<String>,
    kidsWithEntries: Set<String>,
    onAddKid: (String) -> Unit,
    onRemoveKid: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newKidInput by rememberSaveable { mutableStateOf("") }
    fun addKid() {
        if (newKidInput.isNotBlank()) {
            onAddKid(newKidInput.trim())
            newKidInput = ""
        }
    }
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }

    pendingRemoval?.let { kidName ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove_kid_title, kidName)) },
            text = { Text(stringResource(R.string.remove_kid_body, kidName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveKid(kidName)
                        pendingRemoval = null
                    }
                ) {
                    Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_kids_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newKidInput,
                        onValueChange = { newKidInput = it },
                        label = { Text(stringResource(R.string.add_kid_name_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { addKid() }),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { addKid() }) {
                        Text(stringResource(R.string.action_add))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(kids, key = { it }) { kidName ->
                        val hasEntries = kidName in kidsWithEntries
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = kidName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasEntries) {
                                        stringResource(R.string.kid_has_entries)
                                    } else {
                                        stringResource(R.string.kid_no_entries)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (hasEntries) pendingRemoval = kidName else onRemoveKid(kidName)
                                }
                            ) {
                                Text(stringResource(R.string.action_remove))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}
