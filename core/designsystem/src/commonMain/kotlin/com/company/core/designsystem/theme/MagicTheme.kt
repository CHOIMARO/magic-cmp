package com.company.core.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.company.core.designsystem.resources.Res
import com.company.core.designsystem.resources.instrument_serif_italic
import com.company.core.designsystem.resources.pretendard_variable
import org.jetbrains.compose.resources.Font

/**
 * Font families of the magic design.
 *
 * @property sans Pretendard. The app uses it for all body text.
 * @property serif Instrument Serif Italic. The app uses it for the "magic" logo and serif styles.
 */
@Immutable
data class MagicFonts(
    val sans: FontFamily,
    val serif: FontFamily,
)

/** Fonts of the current [MagicTheme]. */
val LocalMagicFonts = staticCompositionLocalOf {
    MagicFonts(sans = FontFamily.SansSerif, serif = FontFamily.Serif)
}

private val PretendardWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
)

/**
 * Creates the fonts from the bundled font files.
 *
 * Pretendard is one variable font file. Each weight sets the "wght" axis of that file.
 */
@Composable
private fun rememberMagicFonts(): MagicFonts {
    val sans = FontFamily(
        PretendardWeights.map { weight ->
            Font(
                resource = Res.font.pretendard_variable,
                weight = weight,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        }
    )
    val serif = FontFamily(
        Font(
            resource = Res.font.instrument_serif_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
        )
    )
    return MagicFonts(sans = sans, serif = serif)
}

/**
 * Applies the dark magic theme.
 *
 * The theme sets the Material color scheme, the default text style, and [LocalMagicFonts].
 */
@Composable
fun MagicTheme(content: @Composable () -> Unit) {
    val fonts = rememberMagicFonts()
    val colorScheme = darkColorScheme(
        primary = MagicColors.Accent,
        onPrimary = MagicColors.OnAccent,
        background = MagicColors.Background,
        onBackground = MagicColors.OnSurface,
        surface = MagicColors.Surface,
        onSurface = MagicColors.OnSurface,
        surfaceVariant = MagicColors.SurfaceHigh,
        onSurfaceVariant = MagicColors.OnSurface60,
    )
    val baseText = TextStyle(fontFamily = fonts.sans, color = MagicColors.OnSurface)
    val typography = Typography().let { t ->
        Typography(
            displayLarge = t.displayLarge.merge(baseText),
            displayMedium = t.displayMedium.merge(baseText),
            displaySmall = t.displaySmall.merge(baseText),
            headlineLarge = t.headlineLarge.merge(baseText),
            headlineMedium = t.headlineMedium.merge(baseText),
            headlineSmall = t.headlineSmall.merge(baseText),
            titleLarge = t.titleLarge.merge(baseText),
            titleMedium = t.titleMedium.merge(baseText),
            titleSmall = t.titleSmall.merge(baseText),
            bodyLarge = t.bodyLarge.merge(baseText),
            bodyMedium = t.bodyMedium.merge(baseText),
            bodySmall = t.bodySmall.merge(baseText),
            labelLarge = t.labelLarge.merge(baseText),
            labelMedium = t.labelMedium.merge(baseText),
            labelSmall = t.labelSmall.merge(baseText),
        )
    }

    CompositionLocalProvider(LocalMagicFonts provides fonts) {
        MaterialTheme(colorScheme = colorScheme, typography = typography) {
            // Material의 bodyLarge 대신 디자인 기본값(자간 0, 기본 줄 높이)을 쓴다.
            CompositionLocalProvider(
                LocalTextStyle provides baseText,
                LocalContentColor provides MagicColors.OnSurface,
                content = content,
            )
        }
    }
}

/** Shortcut to the fonts of the current theme. */
object MagicTheme {
    /** Fonts of the current theme. */
    val fonts: MagicFonts
        @Composable get() = LocalMagicFonts.current
}
