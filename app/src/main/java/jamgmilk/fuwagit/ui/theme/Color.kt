package jamgmilk.fuwagit.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Sakura Primary Palette ---
val Sakura95 = Color(0xFFFFF1F4) // Softest Pink (Backgrounds)
val Sakura90 = Color(0xFFFFD9E2) // Light Container
val Sakura80 = Color(0xFFFFB1C1) // Main Sakura Pink (Buttons/Brand)
val Sakura70 = Color(0xFFF075AB) // Vibrant Pink (Accents)
val Sakura40 = Color(0xFF723346) // Deep Rose (Text/On-Primary)
val Sakura10 = Color(0xFF20000B) // Darkest Maroon (Body Text)

// --- Dark Mode / Night Sakura Palette ---
val SakuraDarkPrimary = Color(0xFFFFB1C1) // Keep it vibrant to pop
val SakuraDarkBackground = Color(0xFF1A1113) // Deep charcoal-maroon
val SakuraDarkSurface = Color(0xFF25191C)    // Slightly lighter surface
val SakuraDarkOnSurface = Color(0xFFECE0E1)  // Soft white-pink text

// --- Custom "Extra" Colors ---
val CatCream = Color(0xFFFFF9F0) // Your signature cream
val MintAccent = Color(0xFFB2F2BB) // Complementary "ACG" Mint
val GitRed = Color(0xFFFF5252)    // Error color

@Immutable
data class ExtraColors(
    val catCream: Color = Color.Unspecified,
    val sakuraGlow: Color = Color.Unspecified,
    val mintAccent: Color = Color.Unspecified
)

val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }
