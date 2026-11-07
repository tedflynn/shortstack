package com.vivificavit.shortstack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.local.CategorySum
import com.vivificavit.shortstack.ui.components.BalanceChart
import com.vivificavit.shortstack.ui.components.CategoryLegend
import com.vivificavit.shortstack.ui.components.StackedBar
import com.vivificavit.shortstack.ui.components.StackedBars
import com.vivificavit.shortstack.ui.components.categoryColor
import com.vivificavit.shortstack.util.BalancePoint
import com.vivificavit.shortstack.util.DateRanges
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Income and expenses side by side for a date range, each split by category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsTabContent(
    balanceHistory: List<BalancePoint>,
    incomeSums: List<CategorySum>,
    spendingSums: List<CategorySum>,
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
    val isShown = { sum: CategorySum -> sum.category.lowercase() !in hiddenCategories }
    val incomeLabel = stringResource(R.string.trends_income)
    val expensesLabel = stringResource(R.string.trends_expenses)
    val bars = remember(incomeSums, spendingSums, hiddenCategories, incomeLabel, expensesLabel) {
        listOf(
            StackedBar(incomeLabel, incomeSums.filter(isShown)),
            StackedBar(expensesLabel, spendingSums.filter(isShown))
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
                    Text(
                        text = stringResource(R.string.breakdown_range_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

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

        item(key = "balance_chart_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                BalanceChart(
                    points = balanceHistory,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        item(key = "trends_chart_card") {
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
                    StackedBars(bars = bars, colorFor = colorFor)
                    Spacer(modifier = Modifier.height(20.dp))
                    if (incomeSums.isEmpty() && spendingSums.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chart_legend_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CategoryLegend(
                            incomeSums = incomeSums,
                            spendingSums = spendingSums,
                            hiddenCategories = hiddenCategories,
                            onToggleCategory = onToggleCategory,
                            colorFor = colorFor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.chart_legend_tap_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
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
                Row {
                    // With no quick ranges, this is how a range goes back to everything.
                    TextButton(
                        onClick = {
                            onDateRangeSelected(null, endDateMillis)
                            pickingStartDate = false
                        }
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                    TextButton(onClick = { pickingStartDate = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
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
                Row {
                    TextButton(
                        onClick = {
                            onDateRangeSelected(startDateMillis, null)
                            pickingEndDate = false
                        }
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                    TextButton(onClick = { pickingEndDate = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
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
