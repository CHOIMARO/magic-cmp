package com.company.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens of the magic design.
 *
 * The values come from the OKLCH colors in the Claude Design file.
 */
object MagicColors {
    // ===========================================================
    // Neutral
    // ===========================================================

    /** Screen background. */
    val Background = Color(0xFF0D0C10)

    /** Cards, sheets, and secondary buttons. */
    val Surface = Color(0xFF1B1A20)

    /** Selected rows and placeholders on [Surface]. */
    val SurfaceHigh = Color(0xFF26242C)

    /** Buttons and option circles on [Surface]. */
    val SurfaceHighest = Color(0xFF2B2932)

    /** Bottom navigation bar. */
    val NavigationBar = Color(0xFF15141A)

    /** Track of a switch in the off state. */
    val SwitchTrackOff = Color(0xFF3A3842)

    /** Main text and icons. */
    val OnSurface = Color(0xFFF3F1F6)

    /** Text and icons on [Accent]. */
    val OnAccent = Color(0xFF1A0F24)

    /** Thin dividers between rows. */
    val Divider = Color.White.copy(alpha = 0.06f)

    // ===========================================================
    // Accent
    // ===========================================================

    /** Main accent. oklch(0.82 0.13 300). */
    val Accent = Color(0xFFD1B0FF)

    /** Sparkle next to the logo. oklch(0.8 0.13 300). */
    val AccentLogo = Color(0xFFCBAAFF)

    /** Kind label in the template sheet. oklch(0.86 0.1 300). */
    val AccentSoft = Color(0xFFDBC2FF)

    /** Sparkle on the magic screen. oklch(0.86 0.12 300). */
    val AccentSparkle = Color(0xFFDDBFFF)

    /** Label of a selected accent item. oklch(0.88 0.1 300). */
    val AccentLabel = Color(0xFFE1C9FF)

    /** Text on a translucent accent background. oklch(0.9 0.07 300). */
    val AccentPale = Color(0xFFE5D4FF)

    // ===========================================================
    // Button states (Magic Buttons spec)
    // ===========================================================

    /** Pressed primary button. oklch(0.74 0.13 300). */
    val AccentPressed = Color(0xFFB897F0)

    /** Pressed light button, for example a selected chip. */
    val LightPressed = Color(0xFFD6D2DC)

    /** Disabled secondary button, icon button, and tool. */
    val SurfaceDisabled = Color(0xFF16151A)

    /** Pressed destructive button. */
    val DangerPressed = Color(0xFF3A3036)

    /** Disabled destructive button. */
    val DangerDisabled = Color(0xFF1F1E24)

    /** Pressed switch track in the off state. */
    val SwitchTrackOffPressed = Color(0xFF45434D)

    // ===========================================================
    // Status
    // ===========================================================

    /** Destructive text, for example "삭제". oklch(0.78 0.12 25). */
    val Danger = Color(0xFFFB9890)

    /** Destructive icon. oklch(0.74 0.16 25). */
    val DangerIcon = Color(0xFFFF7E76)

    /** Record shutter. oklch(0.65 0.21 25). */
    val Record = Color(0xFFF54748)

    /** "최신 버전이에요" badge. oklch(0.85 0.1 160). */
    val Success = Color(0xFF92E2B7)

    /** Music track text. oklch(0.9 0.08 160). */
    val Music = Color(0xFFB0EFCC)

    /** Music track background. oklch(0.8 0.12 160 / .18). */
    val MusicContainer = Color(0xFF71D6A3).copy(alpha = 0.18f)

    /** Small yellow sparkle on the magic screen. oklch(0.88 0.12 85). */
    val Sparkle = Color(0xFFFCD176)

    // ===========================================================
    // Text alpha helpers
    // ===========================================================

    /** [OnSurface] at 80 % opacity. */
    val OnSurface80 = OnSurface.copy(alpha = 0.8f)

    /** [OnSurface] at 70 % opacity. */
    val OnSurface70 = OnSurface.copy(alpha = 0.7f)

    /** [OnSurface] at 60 % opacity. */
    val OnSurface60 = OnSurface.copy(alpha = 0.6f)

    /** [OnSurface] at 55 % opacity. */
    val OnSurface55 = OnSurface.copy(alpha = 0.55f)

    /** [OnSurface] at 50 % opacity. */
    val OnSurface50 = OnSurface.copy(alpha = 0.5f)

    /** [OnSurface] at 45 % opacity. */
    val OnSurface45 = OnSurface.copy(alpha = 0.45f)

    /** [OnSurface] at 40 % opacity. */
    val OnSurface40 = OnSurface.copy(alpha = 0.4f)

    /** Black scrim behind badges on thumbnails. */
    val BadgeScrim = Color.Black.copy(alpha = 0.55f)
}
