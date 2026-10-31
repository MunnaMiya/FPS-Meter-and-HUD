package com.growtotype.fpsmeterandhud.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growtotype.fpsmeterandhud.model.GamingSession
import com.growtotype.fpsmeterandhud.ui.components.FpsTimelineChart
import com.growtotype.fpsmeterandhud.ui.components.HardwareLoadChart
import com.growtotype.fpsmeterandhud.ui.components.MemoryPatternChart
import com.growtotype.fpsmeterandhud.ui.components.parseSamplesJson
import com.growtotype.fpsmeterandhud.ui.theme.NeonAmber
import com.growtotype.fpsmeterandhud.ui.theme.NeonCrimson
import com.growtotype.fpsmeterandhud.ui.theme.NeonCyan
import com.growtotype.fpsmeterandhud.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: GamingSession,
    onBack: () -> Unit,
    onDelete: (GamingSession) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val parsedSamples = remember(session.samplesJson) {
        parseSamplesJson(session.samplesJson)
    }

    val dateStr = remember(session.startTime) {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(session.startTime))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareBody = """
                                🎮 Gaming Session Performance Report: ${session.gameTag}
                                ━━━━━━━━━━━━━━━━━━━━━━━━━
                                • Duration: ${session.durationSeconds / 60}m ${session.durationSeconds % 60}s
                                • Average FPS: ${session.avgFps} FPS
                                • 1% Low FPS: ${session.onePercentLowFps} FPS
                                • 0.1% Low FPS: ${session.pointOnePercentLowFps} FPS
                                • Stability Score: ${session.stabilityScore}%
                                • Peak CPU: ${session.peakCpuUsage}% (Avg: ${session.avgCpuUsage}%)
                                • RAM Peak: ${(session.peakRamUsedMb / 1024f).let { String.format("%.2f", it) }} GB
                                • Peak Temperature: ${session.maxTempC}°C
                                • Privilege Mode: ${session.privilegeMode}
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Session Report"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = NeonCyan)
                    }

                    IconButton(onClick = { onDelete(session) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NeonCrimson)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Impact KPI Cards (Average FPS, 1% Low, 0.1% Low, Stability Score)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "AVERAGE FPS",
                        value = "${session.avgFps.toInt()}",
                        subText = "Range: ${session.minFps.toInt()} - ${session.maxFps.toInt()}",
                        accentColor = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "1% LOW FPS",
                        value = "${session.onePercentLowFps.toInt()}",
                        subText = "Stutter Threshold",
                        accentColor = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "0.1% LOW FPS",
                        value = "${session.pointOnePercentLowFps.toInt()}",
                        subText = "Severe Frame Drops",
                        accentColor = NeonCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "STABILITY SCORE",
                        value = "${session.stabilityScore.toInt()}%",
                        subText = if (session.stabilityScore > 90) "Ultra Smooth" else "Frame Variation",
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive FPS Timeline Chart
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FPS Timeline & Frame Drop Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Touch and drag along the chart to scrub exact timestamps and FPS values",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FpsTimelineChart(
                            samples = parsedSamples,
                            avgFps = session.avgFps,
                            onePercentLow = session.onePercentLowFps
                        )
                    }
                }
            }

            // Memory Consumption Pattern Report
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Memory Consumption Pattern",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Track RAM growth, allocation spikes, and memory leak patterns",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Avg: ${(session.avgRamUsedMb / 1024f).let { String.format("%.1f", it) }}GB",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MemoryPatternChart(
                            samples = parsedSamples,
                            totalRamMb = session.totalRamMb
                        )
                    }
                }
            }

            // CPU & GPU & Thermal Multi-metric Timeline
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hardware Load & Temperature Curve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Peak Temp: ${session.maxTempC}°C | Peak CPU: ${session.peakCpuUsage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HardwareLoadChart(samples = parsedSamples)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
