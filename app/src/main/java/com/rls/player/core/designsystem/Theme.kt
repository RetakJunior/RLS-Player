package com.rls.player.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RlsDarkColorScheme = darkColorScheme(
    primary = Cream,
    onPrimary = Night,
    primaryContainer = SurfaceSelected,
    onPrimaryContainer = Cream,
    secondary = Cream,
    onSecondary = Night,
    background = Night,
    onBackground = Cream,
    surface = Surface,
    onSurface = Cream,
    surfaceVariant = SurfaceSelected,
    onSurfaceVariant = MutedCream,
    outline = DividerColor
)

@Composable
fun RlsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RlsDarkColorScheme,
        typography = RlsTypography,
        shapes = RlsShapes,
        content = content
    )
}
