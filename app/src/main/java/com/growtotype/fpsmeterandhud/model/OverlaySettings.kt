package com.growtotype.fpsmeterandhud.model

import androidx.compose.runtime.Immutable

/**
 * Customization settings for the floating HUD overlay and app appearance.
 * Simplified to 1 Light and 1 Dark theme only.
 */
@Immutable
data class OverlaySettings(
    val opacity: Float = 0.85f,
    val scale: Float = 1.0f,               // 0.50f (ultra-small) to 1.30f
    val fpsOnlyMode: Boolean = false,      // Pixel-style minimal FPS-only overlay
    val isDarkTheme: Boolean = true,       // true = Dark Theme, false = Light Theme
    val showFps: Boolean = true,
    val showCpu: Boolean = true,
    val showGpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemp: Boolean = true,
    val showSparkline: Boolean = true,
    val isCompactDefault: Boolean = false,
    val targetRefreshRate: Int = 0,        // 0 = Auto-detect display rate
    val preferredPrivilege: String = "AUTO"
)
