package jamgmilk.obsigit.ui.theme

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
