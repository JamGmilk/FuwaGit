package jamgmilk.fuwagit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// --- Sakura Primary Palette ---
val Sakura95 = Color(0xFFFFF1F4) // Softest Pink (Backgrounds)
val Sakura90 = Color(0xFFFFD9E2) // Light Container
val Sakura80 = Color(0xFFFFB1C1) // Main Sakura Pink (Buttons/Brand)
val Sakura70 = Color(0xFFF075AB) // Vibrant Pink (Accents)
val Sakura60 = Color(0xFFE05599) // Medium vibrant pink
val Sakura50 = Color(0xFFD04088) // Medium deep pink
val Sakura40 = Color(0xFF723346) // Deep Rose (Text/On-Primary)
val Sakura30 = Color(0xFF5A2838) // Darker Rose
val Sakura20 = Color(0xFF401C28) // Very Dark Rose
val Sakura10 = Color(0xFF20000B) // Darkest Maroon (Body Text)

// --- Dark Mode / Night Sakura Palette ---
val SakuraDarkPrimary = Color(0xFFFFB1C1)
val SakuraDarkBackground = Color(0xFF1A1113)
val SakuraDarkSurface = Color(0xFF25191C)
val SakuraDarkOnSurface = Color(0xFFECE0E1)

// --- Custom "Extra" Colors ---
val CatCream = Color(0xFFFFF9F0)
val MintAccent = Color(0xFFB2F2BB)

// HELPER FUNCTIONS
fun Color.light(amount: Float = 0.6f): Color = lerp(this, Color.White, amount)
fun Color.dark(amount: Float = 0.4f): Color = lerp(this, Color.Black, amount)

// GIT STATUS COLORS
object GitColors {
    // Basic Git colors
    val GitGreen = Color(0xFF4CAF50)
    val GitGreenLight = GitGreen.light()
    val GitBlue = Color(0xFF1E88E5)
    val GitBlueLight = GitBlue.light()
    val GitRed = Color(0xFFFF5252)
    val GitRedLight = GitRed.light()
    val GitOrange = Color(0xFFFF9800)
    val GitOrangeLight = GitOrange.light()
    val GitPurple = Color(0xFF9C27B0)
    val GitPurpleLight = GitPurple.light()
    val GitCyan = Color(0xFF00BCD4)
    val GitCyanLight = GitCyan.light()
    val GitPink = Color(0xFFE91E63)
    val GitPinkLight = GitPink.light()
    val GitBrown = Color(0xFF795548)
    val GitBrownLight = GitBrown.light()
    val GitGrey = Color(0xFF607D8B)
    val GitGreyLight = GitGrey.light()
    val GitDeepOrange = Color(0xFFFF5722)
    val GitDeepOrangeLight = GitDeepOrange.light()
    val GitAmber = Color(0xFFFFA000)
    val GitAmberLight = GitAmber.light()
    val GitTeal = Color(0xFF00796B)
    val GitTealLight = GitTeal.light()
    val GitIndigo = Color(0xFF3F51B5)
    val GitIndigoLight = GitIndigo.light()
    val GitLime = Color(0xFFCDDC39)
    val GitLimeLight = GitLime.light()
    val GitLightGreen = Color(0xFF8BC34A)
    val GitLightGreenLight = GitLightGreen.light()
    val GitDeepPurple = Color(0xFF673AB7)
    val GitDeepPurpleLight = GitDeepPurple.light()
    val GitYellow = Color(0xFFFFEB3B)
    val GitYellowLight = GitYellow.light()
    val GitLightBlue = Color(0xFF03A9F4)
    val GitLightBlueLight = GitLightBlue.light()
    val GitPink2 = Color(0xFFF06292)
    val GitPink2Light = GitPink2.light()
    val GitDarkPink = Color(0xFFD81B60)
    val GitDarkPinkLight = GitDarkPink.light()
    val GitBlueGrey = Color(0xFF78909C)
    val GitBlueGreyLight = GitBlueGrey.light()
    
    // Semantic git status colors
    val Untracked = GitBlueGrey
    val UntrackedLight = GitBlueGreyLight
    val Conflicting = GitDarkPink
    val ConflictingLight = GitDarkPinkLight
}

// EXTRA COLORS (Provided via CompositionLocal)
@Immutable
data class ExtraColors(
    // Original extra colors
    val catCream: Color = Color.Unspecified,
    val sakuraGlow: Color = Color.Unspecified,
    val mintAccent: Color = Color.Unspecified,
    
    // UI component colors
    val cardContainer: Color = Color.Unspecified,
    val cardBorder: Color = Color.Unspecified,
    val terminalBackground: Color = Color.Unspecified,
    val terminalText: Color = Color.Unspecified,
    val navBarContainer: Color = Color.Unspecified,
    val backgroundBrush: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
    val cuteGradientBrush: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
    
    // Git status colors
    val gitAdded: Color = Color.Unspecified,
    val gitAddedLight: Color = Color.Unspecified,
    val gitModified: Color = Color.Unspecified,
    val gitModifiedLight: Color = Color.Unspecified,
    val gitDeleted: Color = Color.Unspecified,
    val gitDeletedLight: Color = Color.Unspecified,
    val gitUntracked: Color = Color.Unspecified,
    val gitConflicting: Color = Color.Unspecified,
    
    // Cute pastel colors for variety
    val pastelBlue: Color = Color.Unspecified,
    val pastelGreen: Color = Color.Unspecified,
    val pastelPurple: Color = Color.Unspecified,
    val pastelOrange: Color = Color.Unspecified,
    val pastelYellow: Color = Color.Unspecified,
    val pastelTeal: Color = Color.Unspecified,
    val pastelPink: Color = Color.Unspecified,
    val pastelIndigo: Color = Color.Unspecified,
    
    // Soft shadow colors
    val softShadow: Color = Color.Unspecified
)

object FuwaGitThemeExtras {
    val colors: ExtraColors
        @Composable
        get() = LocalExtraColors.current
}

val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }
