package jamgmilk.fuwagit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

@Composable
fun MizuiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

//    val extraColors = remember(darkTheme) {
//        if (darkTheme) DarkExtraColors else LightExtraColors
//    }

//    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
//    }
}

//@Composable
//fun MizuiroTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    dynamicColor: Boolean = false,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    val extraColors = remember(darkTheme) {
//        if (darkTheme) DarkExtraColors else LightExtraColors
//    }
//
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            val window = (view.context as Activity).window
//            window.statusBarColor = colorScheme.primary.toArgb()
//            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
//        }
//    }
//
//    CompositionLocalProvider(LocalExtraColors provides extraColors) {
//        MaterialTheme(
//            colorScheme = colorScheme,
//            typography = Typography,
//            shapes = AppShapes,
//            content = content
//        )
//    }
//}

//private val LightExtraColors = ExtraColors(
//    mizuiroAccent = MizuiroPrimaryLight,
//    mizuiroAccentLight = MizuiroSecondaryLight,
//    mizuiroAccentDark = MizuiroTertiaryLight,
//    cardContainer = MizuiroSurfaceLight,
//    cardBorder = MizuiroPrimaryLight.copy(alpha = 0.3f),
//    terminalBackground = Color(0xFF1E1E1E),
//    terminalText = Color(0xFFD4D4D4),
//    navBarContainer = MizuiroSurfaceLight.copy(alpha = 0.8f),
//    backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
//        listOf(MizuiroBackgroundLight, MizuiroPrimaryContainerLight.copy(alpha = 0.1f))
//    ),
//    cuteGradientBrush = androidx.compose.ui.graphics.Brush.linearGradient(
//        listOf(MizuiroPrimaryLight, MizuiroSecondaryLight, MizuiroTertiaryLight)
//    ),
//    gitAdded = GitColors.GitGreen,
//    gitAddedLight = GitColors.GitGreen.copy(alpha = 0.15f),
//    gitModified = GitColors.GitOrange,
//    gitModifiedLight = GitColors.GitOrange.copy(alpha = 0.15f),
//    gitDeleted = GitColors.GitRed,
//    gitDeletedLight = GitColors.GitRed.copy(alpha = 0.15f),
//    gitUntracked = GitColors.GitBlueGrey,
//    gitConflicting = GitColors.GitDarkPink,
//    softShadow = Color(0x1A000000)
//)
//
//private val DarkExtraColors = ExtraColors(
//    mizuiroAccent = MizuiroPrimaryDark,
//    mizuiroAccentLight = MizuiroSecondaryDark,
//    mizuiroAccentDark = MizuiroTertiaryDark,
//    cardContainer = MizuiroSurfaceDark,
//    cardBorder = MizuiroPrimaryDark.copy(alpha = 0.3f),
//    terminalBackground = Color(0xFF121212),
//    terminalText = Color(0xFFE0E0E0),
//    navBarContainer = MizuiroSurfaceDark.copy(alpha = 0.8f),
//    backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
//        listOf(MizuiroBackgroundDark, MizuiroPrimaryContainerDark.copy(alpha = 0.1f))
//    ),
//    cuteGradientBrush = androidx.compose.ui.graphics.Brush.linearGradient(
//        listOf(MizuiroPrimaryDark, MizuiroSecondaryDark, MizuiroTertiaryDark)
//    ),
//    gitAdded = GitColors.GitGreen,
//    gitAddedLight = GitColors.GitGreen.copy(alpha = 0.15f),
//    gitModified = GitColors.GitOrange,
//    gitModifiedLight = GitColors.GitOrange.copy(alpha = 0.15f),
//    gitDeleted = GitColors.GitRed,
//    gitDeletedLight = GitColors.GitRed.copy(alpha = 0.15f),
//    gitUntracked = GitColors.GitBlueGrey,
//    gitConflicting = GitColors.GitDarkPink,
//    softShadow = Color(0x4D000000)
//)
