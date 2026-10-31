package com.example.allowancetracker.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.R
import com.example.allowancetracker.data.local.CategorySum
import com.example.allowancetracker.ui.components.CategoryDonutChart
import com.example.allowancetracker.ui.components.CategoryLegend
import com.example.allowancetracker.ui.components.getCategoryColor
import com.example.allowancetracker.ui.components.rememberCategoryColorMap
import com.example.allowancetracker.ui.theme.CreditGreen
import com.example.allowancetracker.ui.theme.DebitRed
import com.example.allowancetracker.util.DateRanges
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Which side of the ledger a breakdown tab shows, with its labels. */
enum class BreakdownKind(
    val accentColor: Color,
    @StringRes val titleRes: Int,
    @StringRes val chartTitleRes: Int,
    @StringRes val rangeHintRes: Int,
    @StringRes val categoriesTitleRes: Int,
    @StringRes val noMatchRes: Int,
    @StringRes val allRecordedRes: Int,
    @StringRes val totalRes: Int,
    @StringRes val emptyRes: Int
) {
    INCOME(
        accentColor = CreditGreen,
        titleRes = R.string.tab_income,
        chartTitleRes = R.string.breakdown_income_chart_title,
        rangeHintRes = R.string.breakdown_income_range_hint,
        categoriesTitleRes = R.string.breakdown_income_categories,
        noMatchRes = R.string.breakdown_income_no_match,
        allRecordedRes = R.string.breakdown_income_all_recorded,
        totalRes = R.string.breakdown_income_total,
        emptyRes = R.string.breakdown_income_empty
    ),
    EXPENSES(
        accentColor = DebitRed,
        titleRes = R.string.tab_expenses,
        chartTitleRes = R.string.breakdown_expenses_chart_title,
        rangeHintRes = R.string.breakdown_expenses_range_hint,
        categoriesTitleRes = R.string.breakdown_expenses_categories,
        noMatchRes = R.string.breakdown_expenses_no_match,
        allRecordedRes = R.string.breakdown_expenses_all_recorded,
        totalRes = R.string.breakdown_expenses_total,
        emptyRes = R.string.breakdown_expenses_empty
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBreakdownTabContent(
    kidName: String,
    kind: BreakdownKind,
    matchingCategories: List<String>,
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    categorySums: List<CategorySum>,
    startDateMillis: Long?,
    endDateMillis: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = kind.accentColor
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
                                text = stringResource(R.string.breakdown_range_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(kind.rangeHintRes, kidName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (startDateMillis != null || endDateMillis != null) {
                            TextButton(onClick = { onDateRangeSelected(null, null) }) {
                                Text(stringResource(R.string.breakdown_range_reset))
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
                                text = startDateMillis?.let(::formatPickerDate)
                                    ?: stringResource(R.string.breakdown_start_date)
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
                                text = endDateMillis?.let(::formatPickerDate)
                                    ?: stringResource(R.string.breakdown_end_date)
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
                        text = stringResource(kind.categoriesTitleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    if (matchingCategories.isEmpty()) {
                        Text(
                            text = stringResource(kind.noMatchRes),
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
                                                contentDescription = stringResource(R.string.cd_selected),
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
                        text = stringResource(kind.chartTitleRes, kidName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateRangeSubtitle(startDateMillis, endDateMillis, kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(kind.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryDonutChart(
                        categorySpending = categorySums,
                        totalLabel = stringResource(kind.totalRes),
                        emptyLabel = stringResource(kind.emptyRes),
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
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** DatePicker values are midnight UTC, so read them as UTC dates to show the chosen day. */
private fun formatPickerDate(pickerUtcMillis: Long): String =
    DateRanges.pickerMillisToLocalDate(pickerUtcMillis).format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    )

@Composable
private fun dateRangeSubtitle(
    startDateMillis: Long?,
    endDateMillis: Long?,
    kind: BreakdownKind
): String {
    if (startDateMillis == null && endDateMillis == null) {
        return stringResource(kind.allRecordedRes)
    }
    val startText = startDateMillis?.let(::formatPickerDate) ?: stringResource(R.string.breakdown_range_beginning)
    val endText = endDateMillis?.let(::formatPickerDate) ?: stringResource(R.string.breakdown_range_today)
    return stringResource(R.string.breakdown_range, startText, endText)
}
