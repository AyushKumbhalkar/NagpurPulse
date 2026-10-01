//java/com/nagpurpulse/ui/theme/Color.kt

package com.nagpurpulse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── AMOLED Dark Palette ────────────────────────────────────────────────────────
val BackgroundDark       = Color(0xFF080808)   // true AMOLED black
val SurfaceOne           = Color(0xFF111111)   // card base
val SurfaceTwo           = Color(0xFF181818)   // elevated card
val SurfaceThree         = Color(0xFF1F1F1F)   // nav bar / sheet
val SurfaceFour          = Color(0xFF262626)   // input / chip
val DividerColor         = Color(0xFF1E1E1E)
val CardDark             = SurfaceOne
val CardDarker           = Color(0xFF0D0D0D)

// ── Brand ──────────────────────────────────────────────────────────────────────
val OrangePrimary        = Color(0xFFFF6B00)
val OrangeLight          = Color(0xFFFF8A30)
val OrangeGlow           = Color(0x33FF6B00)   // 20% alpha for glow
val OrangeSubtle         = Color(0x14FF6B00)   // 8% alpha
val OrangeDim            = Color(0xFF7A3200)   // pressed / dark accent

// ── Semantic ──────────────────────────────────────────────────────────────────
val RedAlert             = Color(0xFFFF453A)
val RedSubtle            = Color(0x20FF453A)
val GreenSuccess         = Color(0xFF32D74B)
val GreenSubtle          = Color(0x2032D74B)
val BlueInfo             = Color(0xFF0A84FF)
val BlueSubtle           = Color(0x200A84FF)
val YellowWarn           = Color(0xFFFFD60A)
val YellowSubtle         = Color(0x20FFD60A)
val PurpleNight          = Color(0xFF7C3AED)
val PurpleSubtle         = Color(0x207C3AED)

// ── Category Colors ───────────────────────────────────────────────────────────
val GreenCollege         = Color(0xFF34C759)
val BlueJobs             = Color(0xFF0A84FF)
val TealNeighborhood     = Color(0xFF5AC8FA)
val YellowLost           = Color(0xFFFF9F0A)
val PinkEvents           = Color(0xFFFF375F)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary          = Color(0xFFF2F2F7)
val TextSecondary        = Color(0xFF8E8E93)
val TextTertiary         = Color(0xFF636366)
val TextDisabled         = Color(0xFF48484A)

// ── Light Theme ───────────────────────────────────────────────────────────────
val BackgroundLight      = Color(0xFFF2F2F7)
val SurfaceLight         = Color(0xFFFFFFFF)
val SurfaceLightTwo      = Color(0xFFF8F8FA)
val TextPrimaryLight     = Color(0xFF1C1C1E)
val TextSecondaryLight   = Color(0xFF636366)
val DividerLight         = Color(0xFFE5E5EA)

val SurfaceVariant = SurfaceTwo



val Background: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) BackgroundDark else BackgroundLight

val Surface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) SurfaceOne else SurfaceLight

val SurfaceAlt: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) SurfaceThree else SurfaceLightTwo

val PrimaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) TextPrimary else TextPrimaryLight

val SecondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) TextSecondary else TextSecondaryLight

val TertiaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) TextTertiary else TextSecondaryLight.copy(alpha = 0.8f)

val Divider: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DividerColor else DividerLight
