package com.example.allowancetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LedgerColorScheme = lightColorScheme(
    primary = Teal800,
    onPrimary = Color.White,
    // The balance card: dark teal with white text.
    primaryContainer = Teal900,
    onPrimaryContainer = Color.White,
    secondary = Teal700,
    onSecondary = Color.White,
    // Selected chips, category tags and the toggle in the entry sheet.
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,
    tertiary = CreditGreen,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    // Cards, app bar and sheets are white on a slate-50 page.
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceTint = Teal800,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Slate100,
    outline = Slate300,
    outlineVariant = Slate200,
    error = DebitRed,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red800,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = Teal100
)

@Composable
fun AllowanceTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LedgerColorScheme, content = content)
}
