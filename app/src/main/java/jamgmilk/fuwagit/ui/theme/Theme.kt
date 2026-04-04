package jamgmilk.fuwagit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
        sakuraGlow = if (darkTheme) Sakura80 else Sakura70
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
