package com.growtotype.fpsmeterandhud.model

import androidx.compose.runtime.Immutable

/**
 * Real-time hardware telemetry sample captured at a point in time.
 */
@Immutable
data class HardwareSample(
    val timestampMs: Long = System.currentTimeMillis(),
    val fps: Float = 60f,
    val targetFps: Int = 60,
    val cpuUsage: Float = 0f,         // 0 - 100%
    val cpuTempC: Float = 0f,         // Degrees Celsius
    val gpuUsage: Float = -1f,        // 0 - 100% (-1 if unavailable)
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    val thermalStatus: String = "Normal",
    val modeUsed: String = "Standard" // "Root", "Shizuku", or "Standard"
) {
    val ramPercent: Float
        get() = if (ramTotalMb > 0) (ramUsedMb.toFloat() / ramTotalMb.toFloat()) * 100f else 0f
}
