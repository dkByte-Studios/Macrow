package com.macrow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrow.app.R

/** Colour roles Material doesn't have: soft tints for pills and tiles, the macro colours and the hero gradient. */
@Immutable
class MacrowColors(
    val dark: Boolean,
    val bg: Color,
    val card: Color,
    val ink: Color,
    val sub: Color,
    val muted: Color,
    val line: Color,
    val green: Color,
    val greenLight: Color,
    val greenDeep: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val tints: Map<Tone, Pair<Color, Color>>,
    val shadow: Color,
) {
    val hero: List<Color> get() = listOf(greenDeep, greenLight)
    fun tint(tone: Tone): Pair<Color, Color> = tints.getValue(tone)
}

enum class Tone { Green, Gold, Berry, Blue }

private val LightColors = MacrowColors(
    dark = false, bg = Color(0xFFF3F8F5), card = Color.White, ink = Color(0xFF14281F), sub = Color(0xFF4B6157), muted = Color(0xFF5F7469),
    line = Color(0xFFDDE9E3), green = Color(0xFF0E8F63), greenLight = Color(0xFF1DB982), greenDeep = Color(0xFF0A6A4A),
    protein = Color(0xFFE0306F), carbs = Color(0xFFFFB627), fat = Color(0xFF3D6BFF),
    good = Color(0xFF16A36F), warn = Color(0xFFF0A400), bad = Color(0xFFD42A62),
    tints = mapOf(
        Tone.Green to (Color(0xFFE3F5EC) to Color(0xFF0C6B4A)),
        Tone.Gold to (Color(0xFFFFF0CC) to Color(0xFF6B4A00)),
        Tone.Berry to (Color(0xFFFFE3EE) to Color(0xFF8E1250)),
        Tone.Blue to (Color(0xFFE0E9FF) to Color(0xFF1F3FA0)),
    ),
    shadow = Color(0xFF0A3A28),
)

private val DarkColors = MacrowColors(
    dark = true, bg = Color(0xFF0E1713), card = Color(0xFF17231D), ink = Color(0xFFEAF4EF), sub = Color(0xFFB0C2B9), muted = Color(0xFF84988E),
    line = Color(0xFF263A31), green = Color(0xFF3DD69A), greenLight = Color(0xFF1FA877), greenDeep = Color(0xFF0B4A35),
    protein = Color(0xFFFF5C93), carbs = Color(0xFFFFC857), fat = Color(0xFF7C9BFF),
    good = Color(0xFF3DD69A), warn = Color(0xFFFFC857), bad = Color(0xFFFF6B93),
    tints = mapOf(
        Tone.Green to (Color(0xFF1B3A2E) to Color(0xFF8FE3BF)),
        Tone.Gold to (Color(0xFF3A3219) to Color(0xFFF4D679)),
        Tone.Berry to (Color(0xFF40202E) to Color(0xFFFFA7C3)),
        Tone.Blue to (Color(0xFF1E2A4F) to Color(0xFFA9BEFF)),
    ),
    shadow = Color.Black,
)

val LocalMacrow = staticCompositionLocalOf { LightColors }

val MaterialTheme.macrow: MacrowColors
    @Composable @ReadOnlyComposable get() = LocalMacrow.current

private fun variable(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Figtree = FontFamily(variable(R.font.figtree, 400), variable(R.font.figtree, 500), variable(R.font.figtree, 600), variable(R.font.figtree, 700))
val Bricolage = FontFamily(variable(R.font.bricolage_grotesque, 600), variable(R.font.bricolage_grotesque, 700), variable(R.font.bricolage_grotesque, 800))

private fun type(c: Color) = Typography(
    displayLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp, color = c),
    displayMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, color = c),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = c),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = c),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = c),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = c),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = c),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = c),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = c),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = c),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, color = c),
)

@Composable
fun MacrowTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.green, onPrimary = Color(0xFF042417), primaryContainer = p.tint(Tone.Green).first, onPrimaryContainer = p.tint(Tone.Green).second,
            secondary = p.carbs, onSecondary = Color(0xFF2B1D00), tertiary = p.protein, onTertiary = Color.White,
            background = p.bg, onBackground = p.ink, surface = p.card, onSurface = p.ink, surfaceVariant = p.line, onSurfaceVariant = p.sub,
            outline = p.muted, outlineVariant = p.line, error = p.bad, surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerHighest = p.line,
        )
    } else {
        lightColorScheme(
            primary = p.green, onPrimary = Color.White, primaryContainer = p.tint(Tone.Green).first, onPrimaryContainer = p.tint(Tone.Green).second,
            secondary = p.carbs, onSecondary = Color(0xFF2B1D00), tertiary = p.protein, onTertiary = Color.White,
            background = p.bg, onBackground = p.ink, surface = p.card, onSurface = p.ink, surfaceVariant = p.line, onSurfaceVariant = p.sub,
            outline = p.muted, outlineVariant = p.line, error = p.bad, surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerHighest = p.line,
        )
    }
    CompositionLocalProvider(LocalMacrow provides p) {
        MaterialTheme(
            colorScheme = scheme,
            typography = type(p.ink),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
            ),
        ) {
            // Icons without their own tint (back arrows, menus) follow the theme's text colour.
            CompositionLocalProvider(LocalContentColor provides p.ink, content = content)
        }
    }
}
