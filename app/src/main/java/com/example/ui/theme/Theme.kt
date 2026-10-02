package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DjezzyRedLight,
    onPrimary = Color.White,
    primaryContainer = DjezzyRedContainer,
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = SafetyAmberLight,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = OperationalEmeraldLight,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = OperationalEmeraldDark,
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = HazardRed,
    onError = Color.White,
    background = ObsidianDarkBg,
    onBackground = SlateTextPrimary,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateCardSurface,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = DjezzyRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEF),
    onPrimaryContainer = DjezzyRedDark,
    secondary = SafetyAmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = OperationalEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = OperationalEmeraldDark,
    error = HazardRed,
    onError = Color.White,
    background = SandLightBg,
    onBackground = SandTextPrimary,
    surface = SandSurfaceLight,
    onSurface = SandTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SandTextSecondary,
    outline = SandCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Industrial FieldOps defaults to high-contrast dark theme for sunlight & rugged use
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
