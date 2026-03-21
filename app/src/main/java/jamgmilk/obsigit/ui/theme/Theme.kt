package jamgmilk.obsigit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    outline = Sakura70
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
    outline = Sakura50
)

private object ColorTokens {
    val surface = Sakura20
    val surfaceVariant = Sakura30
    val surfaceDark = Color(0xFF3B2A39)
    val surfaceVariantDark = Color(0xFF4A3446)
}

@Composable
fun ObsiGitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
