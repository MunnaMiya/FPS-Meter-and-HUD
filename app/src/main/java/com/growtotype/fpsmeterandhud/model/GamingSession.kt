package com.growtotype.fpsmeterandhud.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted gaming session performance record.
 */
@Immutable
@Entity(tableName = "gaming_sessions")
data class GamingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val gameTag: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val avgFps: Float,
    val minFps: Float,
    val maxFps: Float,
    val onePercentLowFps: Float,
    val pointOnePercentLowFps: Float,
    val stabilityScore: Float,       // 0 - 100%
    val avgCpuUsage: Float,
    val peakCpuUsage: Float,
    val avgGpuUsage: Float,
    val avgRamUsedMb: Int,
    val peakRamUsedMb: Int,
    val totalRamMb: Int,
    val maxTempC: Float,
    val privilegeMode: String,
    val samplesJson: String          // JSON array string for replay chart
)
