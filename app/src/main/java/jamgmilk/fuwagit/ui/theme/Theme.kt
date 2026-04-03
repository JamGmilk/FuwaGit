package jamgmilk.fuwagit.ui.theme

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun LightThemePreview() {
    FuwaGitTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Light Theme Preview", style = MaterialTheme.typography.headlineMedium)
            Text("This is a sample text.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun DarkThemePreview() {
    FuwaGitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dark Theme Preview", style = MaterialTheme.typography.headlineMedium)
            Text("This is a sample text.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

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

@Immutable
data class FuwaGitExtraColors(
    val cardContainer: Color,
    val cardBorder: Color,
    val terminalBackground: Color,
    val terminalText: Color,
    val navBarContainer: Color,
    val backgroundBrush: Brush,
    val gitAdded: Color,
    val gitAddedLight: Color,
    val gitModified: Color,
    val gitModifiedLight: Color,
    val gitDeleted: Color,
    val gitDeletedLight: Color,
    val gitUntracked: Color,
    val gitConflicting: Color
)

private val LightExtraColors = FuwaGitExtraColors(
    cardContainer = Sakura20,
    cardBorder = Sakura50.copy(alpha = 0.40f),
    terminalBackground = CatNightSoft,
    terminalText = CatCream,
    navBarContainer = Sakura20.copy(alpha = 0.95f),
    backgroundBrush = LightBackgroundBrush,
    gitAdded = AppColors.GitGreen,
    gitAddedLight = AppColors.GitGreenLight,
    gitModified = AppColors.GitBlue,
    gitModifiedLight = AppColors.GitBlueLight,
    gitDeleted = AppColors.GitRed,
    gitDeletedLight = AppColors.GitRedLight,
    gitUntracked = AppColors.Untracked,
    gitConflicting = AppColors.Conflicting
)

private val DarkExtraColors = FuwaGitExtraColors(
    cardContainer = ColorTokens.surfaceDark,
    cardBorder = Sakura70.copy(alpha = 0.40f),
    terminalBackground = CatNightSoft,
    terminalText = Sakura30,
    navBarContainer = CatNight,
    backgroundBrush = DarkBackgroundBrush,
    gitAdded = AppColors.GitGreen,
    gitAddedLight = AppColors.GitGreenLight,
    gitModified = AppColors.GitBlue,
    gitModifiedLight = AppColors.GitBlueLight,
    gitDeleted = AppColors.GitRed,
    gitDeletedLight = AppColors.GitRedLight,
    gitUntracked = AppColors.Untracked,
    gitConflicting = AppColors.Conflicting
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
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    // 使用 updateTransition 统一管理所有颜色动画
    val transition = updateTransition(
        targetState = darkTheme,
        label = "ThemeTransition"
    )

    // 只动画关键背景色，其余颜色直接切换
    val animatedBackground by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "Background"
    ) { isDark ->
        if (isDark) DarkColorScheme.background else LightColorScheme.background
    }

    val animatedSurface by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "Surface"
    ) { isDark ->
        if (isDark) DarkColorScheme.surface else LightColorScheme.surface
    }

    val animatedSurfaceVariant by transition.animateColor(
        transitionSpec = { tween(durationMillis = 400) },
        label = "SurfaceVariant"
    ) { isDark ->
        if (isDark) DarkColorScheme.surfaceVariant else LightColorScheme.surfaceVariant
    }

    // 构建动画后的 ColorScheme
    val animatedColorScheme = targetColorScheme.copy(
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
    )

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
