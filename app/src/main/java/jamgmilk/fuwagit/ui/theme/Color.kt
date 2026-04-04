package jamgmilk.fuwagit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val primaryLight = Color(0xFF34618D)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD0E4FF)
val onPrimaryContainerLight = Color(0xFF164974)
val secondaryLight = Color(0xFF526070)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFD6E4F7)
val onSecondaryContainerLight = Color(0xFF3B4857)
val tertiaryLight = Color(0xFF6A5779)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFF1DAFF)
val onTertiaryContainerLight = Color(0xFF514060)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = Color(0xFFF8F9FF)
val onBackgroundLight = Color(0xFF191C20)
val surfaceLight = Color(0xFFF8F9FF)
val onSurfaceLight = Color(0xFF191C20)
val surfaceVariantLight = Color(0xFFDFE3EB)
val onSurfaceVariantLight = Color(0xFF42474E)
val outlineLight = Color(0xFF73777F)
val outlineVariantLight = Color(0xFFC2C7CF)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF2D3135)
val inverseOnSurfaceLight = Color(0xFFEFF0F7)
val inversePrimaryLight = Color(0xFF9ECAFC)
val surfaceDimLight = Color(0xFFD8DAE0)
val surfaceBrightLight = Color(0xFFF8F9FF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF2F3F9)
val surfaceContainerLight = Color(0xFFECEEF4)
val surfaceContainerHighLight = Color(0xFFE6E8EE)
val surfaceContainerHighestLight = Color(0xFFE0E2E8)

val primaryDark = Color(0xFF9ECAFC)
val onPrimaryDark = Color(0xFF003256)
val primaryContainerDark = Color(0xFF164974)
val onPrimaryContainerDark = Color(0xFFD0E4FF)
val secondaryDark = Color(0xFFBAC8DB)
val onSecondaryDark = Color(0xFF243140)
val secondaryContainerDark = Color(0xFF3B4857)
val onSecondaryContainerDark = Color(0xFFD6E4F7)
val tertiaryDark = Color(0xFFD5BEE5)
val onTertiaryDark = Color(0xFF3A2948)
val tertiaryContainerDark = Color(0xFF514060)
val onTertiaryContainerDark = Color(0xFFF1DAFF)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF101418)
val onBackgroundDark = Color(0xFFE0E2E8)
val surfaceDark = Color(0xFF101418)
val onSurfaceDark = Color(0xFFE0E2E8)
val surfaceVariantDark = Color(0xFF42474E)
val onSurfaceVariantDark = Color(0xFFC2C7CF)
val outlineDark = Color(0xFF8C9199)
val outlineVariantDark = Color(0xFF42474E)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE0E2E8)
val inverseOnSurfaceDark = Color(0xFF2D3135)
val inversePrimaryDark = Color(0xFF34618D)
val surfaceDimDark = Color(0xFF101418)
val surfaceBrightDark = Color(0xFF36393E)
val surfaceContainerLowestDark = Color(0xFF0B0E12)
val surfaceContainerLowDark = Color(0xFF191C20)
val surfaceContainerDark = Color(0xFF1D2024)
val surfaceContainerHighDark = Color(0xFF272A2F)
val surfaceContainerHighestDark = Color(0xFF32353A)

// 切割喵~

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
