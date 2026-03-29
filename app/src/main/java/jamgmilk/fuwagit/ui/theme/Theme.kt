package jamgmilk.fuwagit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Sakura60,
    onPrimary = CatCream,
    primaryContainer = Sakura95,
    onPrimaryContainer = Sakura20,
    secondary = Sakura50,
    onSecondary = CatCream,
    secondaryContainer = Sakura90,
    onSecondaryContainer = Sakura20,
    tertiary = Sakura70,
    onTertiary = CatCream,
    background = CatNight,
    onBackground = Sakura20,
    surface = ColorTokens.surfaceDark,
    onSurface = Sakura20,
    surfaceVariant = ColorTokens.surfaceVariantDark,
    onSurfaceVariant = Sakura30,
    outline = Sakura70,
    error = AppColors.GitRed,
    onError = CatCream
)

private val LightColorScheme = lightColorScheme(
    primary = Sakura80,
    onPrimary = CatCream,
    primaryContainer = Sakura40,
    onPrimaryContainer = Sakura95,
    secondary = Sakura70,
    onSecondary = CatCream,
    secondaryContainer = Sakura30,
    onSecondaryContainer = Sakura95,
    tertiary = Sakura80,
    onTertiary = CatCream,
    background = Sakura10,
    onBackground = Sakura95,
    surface = ColorTokens.surface,
    onSurface = Sakura95,
    surfaceVariant = ColorTokens.surfaceVariant,
    onSurfaceVariant = Sakura90,
    outline = Sakura50,
    error = AppColors.GitRed,
    onError = CatCream
)

private object ColorTokens {
    val surface = Sakura20
    val surfaceVariant = Sakura30
    val surfaceDark = Color(0xFF3B2A39)
    val surfaceVariantDark = Color(0xFF4A3446)
}

@Immutable
data class FuwaGitExtraColors(
    val cardContainer: Color,
    val cardBorder: Color,
    val terminalBackground: Color,
    val terminalText: Color,
    val navBarContainer: Color
)

private val LightExtraColors = FuwaGitExtraColors(
    cardContainer = Sakura20,
    cardBorder = Sakura50.copy(alpha = 0.40f),
    terminalBackground = CatNightSoft,
    terminalText = Sakura30,
    navBarContainer = Sakura20.copy(alpha = 0.95f)
)

private val DarkExtraColors = FuwaGitExtraColors(
    cardContainer = ColorTokens.surfaceDark,
    cardBorder = Sakura70.copy(alpha = 0.40f),
    terminalBackground = CatNightSoft,
    terminalText = Sakura30,
    navBarContainer = CatNight
)

private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }

object FuwaGitThemeExtras {
    val colors: FuwaGitExtraColors
        @Composable
        get() = LocalExtraColors.current
}

@Composable
fun FuwaGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
