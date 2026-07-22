package com.beigel.famly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FamlyErrorColor = Color(0xFFB3261E)

private val FamlyLightColorScheme = lightColorScheme(
    primary = FamlyPetrolPrimary,
    onPrimary = FamlyWhite,
    primaryContainer = FamlyChipBackground,
    onPrimaryContainer = FamlyChipText,
    secondary = FamlyPetrolPrimaryHover,
    onSecondary = FamlyWhite,
    background = FamlyBackground,
    onBackground = FamlyTextPrimary,
    surface = FamlyWhite,
    onSurface = FamlyTextPrimary,
    surfaceVariant = FamlySurfaceTint,
    onSurfaceVariant = FamlyBodyText,
    outline = FamlyOutline,
    outlineVariant = FamlyDivider,
    error = FamlyErrorColor
)

private val FamlyDarkColorScheme = darkColorScheme(
    primary = FamlySurfaceLight,
    onPrimary = FamlyPetrolPrimaryHover,
    background = FamlyTextPrimary,
    onBackground = FamlyBackground,
    surface = FamlyPetrolPrimaryHover,
    onSurface = FamlyBackground
)

@Composable
fun FamlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FamlyDarkColorScheme else FamlyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamlyTypography,
        content = content
    )
}
