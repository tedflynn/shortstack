package com.vivificavit.bodad.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivificavit.bodad.R
import com.vivificavit.bodad.data.local.CategorySum
import com.vivificavit.bodad.ui.theme.CategoryPalette
import com.vivificavit.bodad.util.Money
import com.vivificavit.bodad.util.Percentages

/**
 * Gives each category a steady color from its place in [allCategories], so it
 * looks the same on both tabs and whatever the date range.
 */
fun categoryColor(category: String, allCategories: List<String>): Color {
    val index = allCategories.indexOfFirst { it.equals(category, ignoreCase = true) }
    val slot = if (index >= 0) index else category.lowercase().hashCode() and Int.MAX_VALUE
    return CategoryPalette[slot % CategoryPalette.size]
}

@Composable
fun CategoryDonutChart(
    categorySpending: List<CategorySum>,
    colorFor: (String) -> Color,
    modifier: Modifier = Modifier,
    totalLabel: String,
    emptyLabel: String,
    chartSize: Dp = 148.dp,
    strokeWidth: Dp = 22.dp
) {
    val totalSpending = remember(categorySpending) {
        categorySpending.sumOf { it.totalCents }.coerceAtLeast(0L)
    }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(categorySpending) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered && totalSpending > 0L) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "DonutSweepAnimation"
    )

    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeftOffset = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            if (totalSpending <= 0L || categorySpending.isEmpty()) {
                drawArc(
                    color = emptyRingColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                var currentStartAngle = -90f
                val gapDegrees = if (categorySpending.size > 1) 2.5f else 0f

                categorySpending.forEach { item ->
                    val proportion = item.totalCents.toFloat() / totalSpending
                    val rawSweep = proportion * 360f * animatedProgress
                    val adjustedSweep = (rawSweep - gapDegrees).coerceAtLeast(1f)

                    drawArc(
                        color = colorFor(item.category),
                        startAngle = currentStartAngle + (gapDegrees / 2f),
                        sweepAngle = adjustedSweep,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    currentStartAngle += rawSweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (totalSpending <= 0L) {
                Text(
                    text = emptyLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Money.format(0L),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Money.format(totalSpending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Lists every category in the range. Tapping one hides it from the chart (or
 * shows it again); hidden ones stay listed, struck through, without a percentage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    allSums: List<CategorySum>,
    hiddenCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    colorFor: (String) -> Color,
    modifier: Modifier = Modifier
) {
    if (allSums.isEmpty()) {
        Text(
            text = stringResource(R.string.chart_legend_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    val isShown = { sum: CategorySum -> sum.category.lowercase() !in hiddenCategories }
    val shownSums = allSums.filter(isShown)
    val percentages = Percentages.of(shownSums.map { it.totalCents })
    val percentageFor = shownSums.map { it.category }.zip(percentages).toMap()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allSums.forEach { item ->
                val shown = isShown(item)
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = percentageFor[item.category]?.let { percentage ->
                                stringResource(R.string.chart_legend_item, Money.format(item.totalCents), percentage)
                            } ?: Money.format(item.totalCents),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.chart_legend_tap_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
