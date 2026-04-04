package jamgmilk.fuwagit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val MizuiroPrimaryLight = Color(0xFF89CFF0)
val MizuiroOnPrimaryLight = Color(0xFFFFFFFF)
val MizuiroPrimaryContainerLight = Color(0xFFD0E8FF)
val MizuiroOnPrimaryContainerLight = Color(0xFF00325A)

val MizuiroSecondaryLight = Color(0xFFB3E5FC)
val MizuiroOnSecondaryLight = Color(0xFF00334C)
val MizuiroSecondaryContainerLight = Color(0xFFE1F5FE)
val MizuiroOnSecondaryContainerLight = Color(0xFF004C6D)

val MizuiroTertiaryLight = Color(0xFFE1BEE7)
val MizuiroOnTertiaryLight = Color(0xFFFFFFFF)
val MizuiroTertiaryContainerLight = Color(0xFFF3E5F5)
val MizuiroOnTertiaryContainerLight = Color(0xFF4A148C)

val MizuiroBackgroundLight = Color(0xFFF9FDFF)
val MizuiroOnBackgroundLight = Color(0xFF191C1E)
val MizuiroSurfaceLight = Color(0xFFF9FDFF)
val MizuiroOnSurfaceLight = Color(0xFF191C1E)

val MizuiroPrimaryDark = Color(0xFF63A4FF)
val MizuiroOnPrimaryDark = Color(0xFF00325A)
val MizuiroPrimaryContainerDark = Color(0xFF00497D)
val MizuiroOnPrimaryContainerDark = Color(0xFFD0E8FF)

val MizuiroSecondaryDark = Color(0xFF81D4FA)
val MizuiroOnSecondaryDark = Color(0xFF00334C)
val MizuiroSecondaryContainerDark = Color(0xFF004C6D)
val MizuiroOnSecondaryContainerDark = Color(0xFFE1F5FE)

val MizuiroTertiaryDark = Color(0xFFCE93D8)
val MizuiroOnTertiaryDark = Color(0xFF4A148C)
val MizuiroTertiaryContainerDark = Color(0xFF6A1B9A)
val MizuiroOnTertiaryContainerDark = Color(0xFFF3E5F5)

val MizuiroBackgroundDark = Color(0xFF191C1E)
val MizuiroOnBackgroundDark = Color(0xFFE1E2E4)
val MizuiroSurfaceDark = Color(0xFF191C1E)
val MizuiroOnSurfaceDark = Color(0xFFE1E2E4)

val PastelOrange = Color(0xFFFFCC80)


fun Color.light(amount: Float = 0.6f): Color = lerp(this, Color.White, amount)
fun Color.dark(amount: Float = 0.4f): Color = lerp(this, Color.Black, amount)

object GitColors {
    val GitGreen = Color(0xFF4CAF50)
    val GitBlue = Color(0xFF1E88E5)
    val GitRed = Color(0xFFFF5252)
    val GitOrange = Color(0xFFFF9800)
    val GitPurple = Color(0xFF9C27B0)
    val GitCyan = Color(0xFF00BCD4)
    val GitPink = Color(0xFFE91E63)
    val GitAmber = Color(0xFFFFA000)
    val GitBlueGrey = Color(0xFF78909C)
    val GitDarkPink = Color(0xFFD81B60)

    val Untracked = GitBlueGrey
    val Conflicting = GitDarkPink
}

@Immutable
data class ExtraColors(
    val catCream: Color = Color.Unspecified,
    val mizuiroGlow: Color = Color.Unspecified,
    val mintAccent: Color = Color.Unspecified,
    val mizuiroAccent: Color = Color.Unspecified,
    val mizuiroAccentLight: Color = Color.Unspecified,
    val mizuiroAccentDark: Color = Color.Unspecified,
    val cardContainer: Color = Color.Unspecified,
    val cardBorder: Color = Color.Unspecified,
    val terminalBackground: Color = Color.Unspecified,
    val terminalText: Color = Color.Unspecified,
    val navBarContainer: Color = Color.Unspecified,
    val backgroundBrush: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
    val cuteGradientBrush: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
    val gitAdded: Color = Color.Unspecified,
    val gitAddedLight: Color = Color.Unspecified,
    val gitModified: Color = Color.Unspecified,
    val gitModifiedLight: Color = Color.Unspecified,
    val gitDeleted: Color = Color.Unspecified,
    val gitDeletedLight: Color = Color.Unspecified,
    val gitUntracked: Color = Color.Unspecified,
    val gitConflicting: Color = Color.Unspecified,
    val softShadow: Color = Color.Unspecified
)

object FuwaGitThemeExtras {
    val colors: ExtraColors
        @Composable
        get() = LocalExtraColors.current
}

val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }
