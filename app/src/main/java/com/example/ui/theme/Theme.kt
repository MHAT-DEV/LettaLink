package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ConsoleDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidBlack,
    primaryContainer = ElectricCyanMuted,
    onPrimaryContainer = ElectricCyan,
    secondary = AmberGlow,
    onSecondary = VoidBlack,
    secondaryContainer = AmberMuted,
    onSecondaryContainer = AmberGlow,
    tertiary = EmeraldNeon,
    onTertiary = VoidBlack,
    tertiaryContainer = EmeraldMuted,
    onTertiaryContainer = EmeraldNeon,
    error = CrimsonLaser,
    onError = VoidBlack,
    errorContainer = CrimsonMuted,
    onErrorContainer = CrimsonLaser,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderHighlight
)

@Composable
fun LettaLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ConsoleDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LettaLinkTheme(content = content)
}
