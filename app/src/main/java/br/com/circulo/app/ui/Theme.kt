package br.com.circulo.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.circulo.app.domain.Appearance
import br.com.circulo.app.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF252A27), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4DF), onPrimaryContainer = Color(0xFF202320),
    secondary = Color(0xFF656A64), onSecondary = Color.White,
    secondaryContainer = Color(0xFF202321), onSecondaryContainer = Color(0xFFF5F6F2),
    tertiary = Color(0xFF665F55), tertiaryContainer = Color(0xFFEAE5DC),
    onTertiaryContainer = Color(0xFF403A31),
    background = Color(0xFFF7F7F3), onBackground = Color(0xFF202421),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF202421),
    surfaceVariant = Color(0xFFE8EAE4), onSurfaceVariant = Color(0xFF5D645C),
    surfaceContainer = Color(0xFFEFEFEC), surfaceContainerLow = Color(0xFFF2F3EF),
    surfaceContainerHigh = Color(0xFFE4E6E0),
    inversePrimary = Color(0xFFE2E2DD), inverseSurface = Color(0xFF242824), inverseOnSurface = Color(0xFFF8F9F4),
    outline = Color(0xFF797E76), outlineVariant = Color(0xFFD9DDD4),
    error = Color(0xFFA34841), onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6E8E1), onPrimary = Color(0xFF1A1E1A),
    primaryContainer = Color(0xFF30352F), onPrimaryContainer = Color(0xFFEEF0E8),
    secondary = Color(0xFFB7BDB3), onSecondary = Color(0xFF242B23),
    secondaryContainer = Color(0xFF252A26), onSecondaryContainer = Color(0xFFF1F3ED),
    tertiary = Color(0xFFCEC5B7), tertiaryContainer = Color(0xFF3B3832),
    onTertiaryContainer = Color(0xFFEEE7DD),
    background = Color(0xFF111312), onBackground = Color(0xFFF1F2EC),
    surface = Color(0xFF171A17), onSurface = Color(0xFFF1F2EC),
    surfaceVariant = Color(0xFF2A3029), onSurfaceVariant = Color(0xFFBCC3B7),
    surfaceContainer = Color(0xFF20241F), surfaceContainerLow = Color(0xFF1A1E1A),
    surfaceContainerHigh = Color(0xFF2B3029),
    inversePrimary = Color(0xFFDCE0D6), inverseSurface = Color(0xFFF0F2E9), inverseOnSurface = Color(0xFF202421),
    outline = Color(0xFF90998A), outlineVariant = Color(0xFF353D31),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5E1712),
)

@OptIn(ExperimentalTextApi::class)
private val Manrope = FontFamily(Font(R.font.manrope, weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))))

private val CirculoTypography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 53.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1.0).sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun CirculoTheme(appearance: Appearance, content: @Composable (Boolean) -> Unit) {
    val dark = when (appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    // Material 3's native sheets read uiMode for system-bar contrast. Keep their
    // effective configuration aligned with the in-app choice as well as colors.
    val configuration = LocalConfiguration.current
    val themedConfiguration = remember(configuration, dark) {
        Configuration(configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
    }
    CompositionLocalProvider(LocalConfiguration provides themedConfiguration) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = CirculoTypography) {
            content(dark)
        }
    }
}
