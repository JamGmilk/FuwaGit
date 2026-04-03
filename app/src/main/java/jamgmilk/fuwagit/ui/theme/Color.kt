package jamgmilk.fuwagit.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, name = "Color Palette")
@Composable
fun ColorPalettePreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Color Palette Preview", style = MaterialTheme.typography.headlineMedium)
        // Add color swatches here if desired
    }
}

// Sakura Series (Light theme base)
val Sakura10 = Color(0xFFFFF7FA)
val Sakura20 = Color(0xFFFFEFF6)
val Sakura30 = Color(0xFFFFE3EF)
val Sakura40 = Color(0xFFFDD2E5)
val Sakura50 = Color(0xFFF5AFCB)
val Sakura60 = Color(0xFFEB8EB6)
val Sakura70 = Color(0xFFD86A9A)
val Sakura80 = Color(0xFFB94D7E)
val Sakura90 = Color(0xFF8B355F)
val Sakura95 = Color(0xFF5E213F)

// Cat Night Series (Dark theme base)
val CatNight = Color(0xFF2C1E2A)
val CatCream = Color(0xFFFFF5FA)
val CatNightSoft = Color(0xFF20151F)
val SakuraNightGlow = Color(0xFF4A3446)

// Additional Accent Colors
val SakuraPinkLight = Color(0xFFFFB6C1)
val SakuraPinkDark = Color(0xFFFF69B4)
val Lavender = Color(0xFFE1BEE7)
val MintGreen = Color(0xFFA5D6A7)
val SkyBlue = Color(0xFF81D4FA)
val Peach = Color(0xFFFFCCBC)
val Cream = Color(0xFFFFF8E1)
val Rose = Color(0xFFF8BBD9)

object ColorTokens {
    val surface = Sakura20
    val surfaceVariant = Sakura30
    val surfaceDark = Color(0xFF3B2A39)
    val surfaceVariantDark = Color(0xFF4A3446)
}

private fun Color.light(): Color = this.copy(alpha = 0.15f)

object AppColors {
    val GitGreen = Color(0xFF4CAF50)
    val GitGreenLight = GitGreen.light()

    val GitBlue = Color(0xFF1E88E5)
    val GitBlueLight = GitBlue.light()

    val GitRed = Color(0xFFE53935)
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

    val Untracked = GitBlueGrey
    val UntrackedLight = GitBlueGreyLight

    val Conflicting = GitDarkPink
    val ConflictingLight = GitDarkPinkLight
}

val LightBackgroundBrush = Brush.verticalGradient(
    listOf(Sakura10, Sakura30, Sakura40)
)

val DarkBackgroundBrush = Brush.verticalGradient(
    listOf(CatNightSoft, CatNight, SakuraNightGlow)
)

val GitColorMap = mapOf(
    "green" to AppColors.GitGreen,
    "blue" to AppColors.GitBlue,
    "red" to AppColors.GitRed,
    "orange" to AppColors.GitOrange,
    "purple" to AppColors.GitPurple,
    "cyan" to AppColors.GitCyan,
    "pink" to AppColors.GitPink,
    "brown" to AppColors.GitBrown,
    "grey" to AppColors.GitGrey
)
