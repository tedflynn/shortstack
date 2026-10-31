package com.example.allowancetracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.allowancetracker.data.local.CategorySum
import androidx.compose.ui.res.stringResource
import com.example.allowancetracker.R
import com.example.allowancetracker.util.Money
import com.example.allowancetracker.util.Percentages

@Composable
fun rememberCategoryColorMap(): Map<String, Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        mapOf(
            "Allowance" to Color(0xFF2E7D32),
            "Gift" to Color(0xFF00897B),
            "Job" to Color(0xFF0277BD),
            "Video Game" to Color(0xFF3949AB),
            "Toy" to Color(0xFF6A1B9A),
            "Book" to Color(0xFF1565C0),
            "Snack" to Color(0xFFE65100),
            "Other" to Color(0xFF546E7A)
        )
    }
}

fun getCategoryColor(
    category: String,
    colorMap: Map<String, Color>,
    fallbackPalette: List<Color>
): Color {
    return colorMap[category] ?: fallbackPalette[
        (category.hashCode() and Int.MAX_VALUE) % fallbackPalette.size
    ]
}

@Composable
fun CategoryDonutChart(
    categorySpending: List<CategorySum>,
    modifier: Modifier = Modifier,
    totalLabel: String,
    emptyLabel: String,
    chartSize: Dp = 148.dp,
    strokeWidth: Dp = 22.dp
) {
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
                    val segmentColor = getCategoryColor(item.category, colorMap, fallbackPalette)

                    drawArc(
                        color = segmentColor,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    categorySpending: List<CategorySum>,
    modifier: Modifier = Modifier
) {
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

    val totalSpending = remember(categorySpending) {
        categorySpending.sumOf { it.totalCents }.coerceAtLeast(0L)
    }

    if (categorySpending.isEmpty() || totalSpending <= 0L) {
        Text(
            text = stringResource(R.string.chart_legend_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val percentages = Percentages.of(categorySpending.map { it.totalCents })
        categorySpending.forEachIndexed { index, item ->
            val percentage = percentages[index]
            val dotColor = getCategoryColor(item.category, colorMap, fallbackPalette)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chart_legend_item, Money.format(item.totalCents), percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
