package com.vivificavit.shortstack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.local.CategorySum
import com.vivificavit.shortstack.ui.theme.CategoryPalette
import com.vivificavit.shortstack.ui.theme.CreditGreen
import com.vivificavit.shortstack.ui.theme.DebitRed
import com.vivificavit.shortstack.ui.theme.OtherCategoryColor
import com.vivificavit.shortstack.util.Money
import com.vivificavit.shortstack.util.Percentages

/**
 * Gives each category a steady color from its place in [allCategories], so it
 * looks the same in both bars and whatever the date range. Categories past the
 * palette (or not in the list) share [OtherCategoryColor] rather than repeating one.
 */
fun categoryColor(category: String, allCategories: List<String>): Color {
    val index = allCategories.indexOfFirst { it.equals(category, ignoreCase = true) }
    return CategoryPalette.getOrNull(index) ?: OtherCategoryColor
}

/** One column of [StackedBars]: its label and the categories stacked in it, largest first. */
data class StackedBar(val label: String, val parts: List<CategorySum>)

/** Square at the baseline, rounded at the top. */
private val BarShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

/**
 * Columns side by side on one shared scale, so the taller one is the bigger
 * total. Each is split into its categories, largest at the bottom, with its
 * total on top and its label under the baseline.
 */
@Composable
fun StackedBars(
    bars: List<StackedBar>,
    colorFor: (String) -> Color,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 200.dp,
    barWidth: Dp = 40.dp
) {
    val maxTotal = bars.maxOfOrNull { bar -> bar.parts.sumOf { it.totalCents } }?.coerceAtLeast(1L) ?: 1L
    // Bottom-aligned, so the bars share a baseline even when a total's text is taller.
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            StackedColumn(
                bar = bar,
                maxTotal = maxTotal,
                colorFor = colorFor,
                plotHeight = plotHeight,
                barWidth = barWidth,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StackedColumn(
    bar: StackedBar,
    maxTotal: Long,
    colorFor: (String) -> Color,
    plotHeight: Dp,
    barWidth: Dp,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val parts = bar.parts.filter { it.totalCents > 0L }
    val total = parts.sumOf { it.totalCents }
    val description = if (parts.isEmpty()) {
        stringResource(R.string.cd_stacked_bar, bar.label, Money.format(total))
    } else {
        stringResource(
            R.string.cd_stacked_bar_parts,
            bar.label,
            Money.format(total),
            parts.joinToString { resources.getString(R.string.cd_stacked_bar_part, it.category, Money.format(it.totalCents)) }
        )
    }

    // Grows from the baseline on first show, then eases to each new total.
    val heightFraction = remember { Animatable(0f) }
    LaunchedEffect(total, maxTotal) {
        heightFraction.animateTo(
            targetValue = total.toFloat() / maxTotal,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Money.format(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .width(barWidth)
                .height(plotHeight * heightFraction.value)
                .clip(BarShape)
        ) {
            if (total <= 0L) return@Canvas
            // A 2dp gap between segments lets the card show through, unless the bar is too short for gaps.
            val gap = 2.dp.toPx()
            val gaps = gap * (parts.size - 1)
            val withGaps = size.height > gaps + parts.size
            val usable = if (withGaps) size.height - gaps else size.height
            var bottom = size.height
            parts.forEach { part ->
                val segment = usable * part.totalCents / total
                drawRect(
                    color = colorFor(part.category),
                    topLeft = Offset(0f, bottom - segment),
                    size = Size(size.width, segment)
                )
                bottom -= segment + if (withGaps) gap else 0f
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = bar.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** A category in the legend, with what came in and went out under it (0 if none). */
data class LegendEntry(val category: String, val incomeCents: Long, val expenseCents: Long)

/** Every category in either list once: income ones first, then the rest, each largest first. */
fun legendEntries(incomeSums: List<CategorySum>, spendingSums: List<CategorySum>): List<LegendEntry> {
    val spentBy = spendingSums.associateBy { it.category.lowercase() }
    val incomeKeys = incomeSums.map { it.category.lowercase() }.toSet()
    return incomeSums.map { LegendEntry(it.category, it.totalCents, spentBy[it.category.lowercase()]?.totalCents ?: 0L) } +
        spendingSums
            .filter { it.category.lowercase() !in incomeKeys }
            .map { LegendEntry(it.category, 0L, it.totalCents) }
}

/**
 * Lists every category in either bar once, with +income and -spending and each
 * one's share of its bar. Tapping one hides it from both bars (or shows it again);
 * hidden ones stay listed, struck through, without a percentage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    incomeSums: List<CategorySum>,
    spendingSums: List<CategorySum>,
    hiddenCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    colorFor: (String) -> Color,
    modifier: Modifier = Modifier
) {
    val isShown = { category: String -> category.lowercase() !in hiddenCategories }
    val entries = remember(incomeSums, spendingSums) { legendEntries(incomeSums, spendingSums) }
    val incomeShare = sharesOf(incomeSums.filter { isShown(it.category) })
    val expenseShare = sharesOf(spendingSums.filter { isShown(it.category) })

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEach { item ->
            val shown = isShown(item.category)
            val dotColor = colorFor(item.category)

            Surface(
                checked = shown,
                onCheckedChange = { onToggleCategory(item.category) },
                shape = RoundedCornerShape(12.dp),
                color = if (shown) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                },
                border = if (shown) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .then(
                                if (shown) {
                                    Modifier.background(dotColor)
                                } else {
                                    Modifier.border(1.5.dp, dotColor, CircleShape)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (shown) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textDecoration = if (shown) null else TextDecoration.LineThrough
                    )
                    if (item.incomeCents > 0L) {
                        LegendAmount(
                            text = "+${Money.format(item.incomeCents)}",
                            percentage = incomeShare[item.category].takeIf { shown },
                            color = if (shown) CreditGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.expenseCents > 0L) {
                        LegendAmount(
                            text = "-${Money.format(item.expenseCents)}",
                            percentage = expenseShare[item.category].takeIf { shown },
                            color = if (shown) DebitRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Each category's whole-number share of [sums], by name. */
private fun sharesOf(sums: List<CategorySum>): Map<String, Int> =
    sums.map { it.category }.zip(Percentages.of(sums.map { it.totalCents })).toMap()

/** "+$50.00 (59%)" in the ledger's income green, or "-$8.00 (22%)" in its expense red. */
@Composable
private fun LegendAmount(text: String, percentage: Int?, color: Color) {
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = percentage?.let { stringResource(R.string.chart_legend_item, text, it) } ?: text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
