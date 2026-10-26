package com.thermoguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Ember & Frost" palette — cold blues flow into ember oranges as heat rises.
val Frost = Color(0xFF4FC3F7)
val Cool = Color(0xFF26A69A)
val Warm = Color(0xFFFFB74D)
val Hot = Color(0xFFFF7043)
val Critical = Color(0xFFE53935)

val Ink = Color(0xFF0E1116)
val Panel = Color(0xFF161B22)
val Track = Color(0xFF242B35)
val TextBright = Color(0xFFE6EDF3)
val TextDim = Color(0xFF8B949E)

private val Scheme = darkColorScheme(
    primary = Hot,
    onPrimary = Ink,
    secondary = Frost,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextBright,
    surface = Panel,
    onSurface = TextBright,
    surfaceVariant = Track,
    onSurfaceVariant = TextDim,
    error = Critical,
)

/** Single source of truth for "what colour is this temperature?" */
fun tempColor(celsius: Float?): Color = when {
    celsius == null -> TextDim
    celsius < 30f -> Frost
    celsius < 37f -> Cool
    celsius < 42f -> Warm
    celsius < 47f -> Hot
    else -> Critical
}

fun statusColor(status: Int?): Color = when (status) {
    null -> TextDim
    0 -> Cool
    1 -> Warm
    2 -> Hot
    else -> Critical
}

@Composable
fun ThermoGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
