package com.vivificavit.bodad.ui.screens

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivificavit.bodad.R
import com.vivificavit.bodad.data.local.CategorySum
import com.vivificavit.bodad.ui.components.CategoryDonutChart
import com.vivificavit.bodad.ui.components.CategoryLegend
import com.vivificavit.bodad.ui.components.categoryColor
import com.vivificavit.bodad.util.DateRanges
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Which side of the ledger a breakdown tab shows, with its labels. */
enum class BreakdownKind(
    @StringRes val allRecordedRes: Int,
    @StringRes val totalRes: Int,
    @StringRes val emptyRes: Int
) {
    INCOME(
        allRecordedRes = R.string.breakdown_income_all_recorded,
        totalRes = R.string.breakdown_income_total,
        emptyRes = R.string.breakdown_income_empty
    ),
    EXPENSES(
        allRecordedRes = R.string.breakdown_expenses_all_recorded,
        totalRes = R.string.breakdown_expenses_total,
        emptyRes = R.string.breakdown_expenses_empty
    )
}

private val PresetLabels = mapOf(
    DateRanges.Preset.THIS_MONTH to R.string.breakdown_preset_this_month,
    DateRanges.Preset.LAST_MONTH to R.string.breakdown_preset_last_month,
    DateRanges.Preset.THIS_YEAR to R.string.breakdown_preset_this_year,
    DateRanges.Preset.ALL_TIME to R.string.breakdown_preset_all_time
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBreakdownTabContent(
    kind: BreakdownKind,
    allSums: List<CategorySum>,
    hiddenCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    allCategories: List<String>,
    startDateMillis: Long?,
    endDateMillis: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickingStartDate by rememberSaveable { mutableStateOf(false) }
    var pickingEndDate by rememberSaveable { mutableStateOf(false) }

    val colorFor = remember(allCategories) { { category: String -> categoryColor(category, allCategories) } }
    val shownSums = remember(allSums, hiddenCategories) {
        allSums.filter { it.category.lowercase() !in hiddenCategories }
    }
    val today = remember { LocalDate.now() }

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
                    Text(
                        text = stringResource(R.string.breakdown_range_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateRanges.Preset.entries.forEach { preset ->
                            val range = DateRanges.presetRange(preset, today)
                            FilterChip(
                                selected = range == (startDateMillis to endDateMillis),
                                onClick = { onDateRangeSelected(range.first, range.second) },
                                label = { Text(stringResource(PresetLabels.getValue(preset))) }
                            )
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
                        text = dateRangeSubtitle(startDateMillis, endDateMillis, kind),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CategoryDonutChart(
                        categorySpending = shownSums,
                        colorFor = colorFor,
                        totalLabel = stringResource(kind.totalRes),
                        emptyLabel = stringResource(kind.emptyRes),
                        chartSize = 156.dp,
                        strokeWidth = 22.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CategoryLegend(
                        allSums = allSums,
                        hiddenCategories = hiddenCategories,
                        onToggleCategory = onToggleCategory,
                        colorFor = colorFor
                    )
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
