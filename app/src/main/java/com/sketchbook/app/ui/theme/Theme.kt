package com.sketchbook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand accents (mirroring the iOS Theme.swift)
val BrandPrimary = Color(0xFF665CDB)   // indigo
val BrandSecondary = Color(0xFF2EB8B8) // teal
val BrandHighlight = Color(0xFFFA9E33) // amber

val LightBackground = Color(0xFFFAF7F2)
val LightSurface = Color(0xFFFFFFFF)
val LightInk = Color(0xFF1F1F29)
val LightMutedInk = Color(0xFF6B6B7A)

val DarkBackground = Color(0xFF121217)
val DarkSurface = Color(0xFF24242B)
val DarkInk = Color(0xFFF2F2F7)
val DarkMutedInk = Color(0xFFA6A6B3)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E1FA),
    onPrimaryContainer = Color(0xFF2A2377),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    tertiary = BrandHighlight,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = Color(0xFFF0EDE6),
    onSurfaceVariant = LightMutedInk,
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B348C),
    onPrimaryContainer = Color(0xFFE4E1FA),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    tertiary = BrandHighlight,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF31313A),
    onSurfaceVariant = DarkMutedInk,
)

@Composable
fun SketchbookTheme(theme: String = "light", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
