package de.stationpilot.gopilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── GoPilot Farb-Palette ─────────────────────────────────────────────────────
val BluePrimary      = Color(0xFF1565C0)   // Kräftiges Blau
val BlueOnPrimary    = Color(0xFFFFFFFF)
val BlueContainer    = Color(0xFFD6E4FF)
val BlueOnContainer  = Color(0xFF001D4A)
val BlueSecondary    = Color(0xFF00B4D8)   // Cyan-Blau Akzent
val BlueSurface      = Color(0xFFF8FAFF)   // Fast Weiß mit Blaustich
val BlueBackground   = Color(0xFFF0F4FF)
val OnSurface        = Color(0xFF0D1B2A)
val ErrorColor       = Color(0xFFD32F2F)
val SuccessColor     = Color(0xFF2E7D32)

private val GoPilotColorScheme = lightColorScheme(
    primary          = BluePrimary,
    onPrimary        = BlueOnPrimary,
    primaryContainer = BlueContainer,
    onPrimaryContainer = BlueOnContainer,
    secondary        = BlueSecondary,
    onSecondary      = Color(0xFF001F25),
    surface          = BlueSurface,
    onSurface        = OnSurface,
    background       = BlueBackground,
    onBackground     = OnSurface,
    error            = ErrorColor,
    onError          = Color.White,
)

@Composable
fun GoPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GoPilotColorScheme,
        typography  = GoPilotTypography,
        content     = content,
    )
}
