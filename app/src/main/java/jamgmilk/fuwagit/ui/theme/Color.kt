package jamgmilk.fuwagit.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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

val CatNight = Color(0xFF2C1E2A)
val CatCream = Color(0xFFFFF5FA)
val CatNightSoft = Color(0xFF20151F)
val SakuraNightGlow = Color(0xFF4A3446)

val SakuraPinkLight = Color(0xFFFFB6C1)
val SakuraPinkDark = Color(0xFFFF69B4)
val Lavender = Color(0xFFE1BEE7)
val MintGreen = Color(0xFFA5D6A7)
val SkyBlue = Color(0xFF81D4FA)
val Peach = Color(0xFFFFCCBC)
val Cream = Color(0xFFFFF8E1)
val Rose = Color(0xFFF8BBD9)

object AppColors {
    val GitGreen = Color(0xFF4CAF50)
    val GitGreenLight = Color(0xFF4CAF50).copy(alpha = 0.15f)

    val GitBlue = Color(0xFF1E88E5)
    val GitBlueLight = Color(0xFF1E88E5).copy(alpha = 0.15f)

    val GitRed = Color(0xFFE53935)
    val GitRedLight = Color(0xFFE53935).copy(alpha = 0.15f)

    val GitOrange = Color(0xFFFF9800)
    val GitOrangeLight = Color(0xFFFF9800).copy(alpha = 0.15f)

    val GitPurple = Color(0xFF9C27B0)
    val GitPurpleLight = Color(0xFF9C27B0).copy(alpha = 0.15f)

    val GitCyan = Color(0xFF00BCD4)
    val GitCyanLight = Color(0xFF00BCD4).copy(alpha = 0.15f)

    val GitPink = Color(0xFFE91E63)
    val GitPinkLight = Color(0xFFE91E63).copy(alpha = 0.15f)

    val GitBrown = Color(0xFF795548)
    val GitBrownLight = Color(0xFF795548).copy(alpha = 0.15f)

    val GitGrey = Color(0xFF607D8B)
    val GitGreyLight = Color(0xFF607D8B).copy(alpha = 0.15f)

    val GitDeepOrange = Color(0xFFFF5722)
    val GitDeepOrangeLight = Color(0xFFFF5722).copy(alpha = 0.15f)

    val GitAmber = Color(0xFFFFA000)
    val GitAmberLight = Color(0xFFFFA000).copy(alpha = 0.15f)

    val GitTeal = Color(0xFF00796B)
    val GitTealLight = Color(0xFF00796B).copy(alpha = 0.15f)

    val GitIndigo = Color(0xFF3F51B5)
    val GitIndigoLight = Color(0xFF3F51B5).copy(alpha = 0.15f)

    val GitLime = Color(0xFFCDDC39)
    val GitLimeLight = Color(0xFFCDDC39).copy(alpha = 0.15f)

    val GitLightGreen = Color(0xFF8BC34A)
    val GitLightGreenLight = Color(0xFF8BC34A).copy(alpha = 0.15f)

    val GitDeepPurple = Color(0xFF673AB7)
    val GitDeepPurpleLight = Color(0xFF673AB7).copy(alpha = 0.15f)

    val GitYellow = Color(0xFFFFEB3B)
    val GitYellowLight = Color(0xFFFFEB3B).copy(alpha = 0.15f)

    val GitLightBlue = Color(0xFF03A9F4)
    val GitLightBlueLight = Color(0xFF03A9F4).copy(alpha = 0.15f)

    val GitPink2 = Color(0xFFF06292)
    val GitPink2Light = Color(0xFFF06292).copy(alpha = 0.15f)

    val GitDarkPink = Color(0xFFD81B60)
    val GitDarkPinkLight = Color(0xFFD81B60).copy(alpha = 0.15f)

    val GitBlueGrey = Color(0xFF78909C)
    val GitBlueGreyLight = Color(0xFF78909C).copy(alpha = 0.15f)

    val Untracked = GitBlueGrey
    val UntrackedLight = GitBlueGreyLight

    val Conflicting = GitDarkPink
    val ConflictingLight = GitDarkPinkLight
}

fun appBackgroundBrush(darkTheme: Boolean): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            listOf(CatNightSoft, CatNight, SakuraNightGlow)
        )
    } else {
        Brush.verticalGradient(
            listOf(Sakura10, Sakura30, Sakura40)
        )
    }
}
