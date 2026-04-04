package jamgmilk.fuwagit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import jamgmilk.fuwagit.ui.theme.GitColors.GitRed

// --- Light Scheme ---
private val SakuraLightColorScheme = lightColorScheme(
    primary = Sakura80,
    onPrimary = Sakura40,
    primaryContainer = Sakura90,
    onPrimaryContainer = Sakura40,

    secondary = Sakura70,
    onSecondary = Color.White,

    tertiary = MintAccent,
    onTertiary = Sakura40,

    background = CatCream,
    onBackground = Sakura10,

    surface = Sakura95,
    onSurface = Sakura10,

    error = GitRed,
    onError = Color.White,

    outline = Sakura70
)

// --- Dark Scheme (Yozakura Vibe) ---
private val SakuraDarkColorScheme = darkColorScheme(
    primary = Sakura80,
    onPrimary = Sakura40,
    primaryContainer = Sakura40,
    onPrimaryContainer = Sakura90,

    secondary = Sakura70,
    onSecondary = Sakura10,

    tertiary = MintAccent,
    onTertiary = Color(0xFF003919),

    background = SakuraDarkBackground,
    onBackground = SakuraDarkOnSurface,

    surface = SakuraDarkSurface,
    onSurface = SakuraDarkOnSurface,

    error = GitRed,
    onError = Color.Black,

    outline = Sakura40
)

@Composable
fun SakuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SakuraDarkColorScheme else SakuraLightColorScheme

    val extraColors = ExtraColors(
        catCream = if (darkTheme) Color(0xFF2D2621) else CatCream,
        sakuraGlow = if (darkTheme) Sakura80 else Sakura70,
        backgroundBrush = Brush.verticalGradient(
            if (darkTheme) listOf(SakuraDarkBackground, SakuraDarkSurface)
            else listOf(CatCream, Sakura95)
        ),
        cuteGradientBrush = Brush.horizontalGradient(
            if (darkTheme) listOf(SakuraDarkPrimary, Sakura70)
            else listOf(Sakura80, Sakura70)
        )
    )

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }


}
