package com.vivificavit.shortstack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    modifier: Modifier = Modifier
) {
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
}

/**
 * Picks the Trends date range in one calendar: tap a start day, then an end day.
 * Apply with only a start shows from then until today; Clear goes back to everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsDateRangeDialog(
    startDateMillis: Long?,
    endDateMillis: Long?,
    onDismiss: () -> Unit,
    onApply: (startMillis: Long?, endMillis: Long?) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDateMillis,
        initialSelectedEndDateMillis = endDateMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onApply(null, null) }) {
                    Text(stringResource(R.string.action_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f)
        )
    }
}
