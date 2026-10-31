package com.growtotype.fpsmeterandhud.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growtotype.fpsmeterandhud.ui.components.FpsSpeedometerCard
import com.growtotype.fpsmeterandhud.ui.components.MetricGridItem
import com.growtotype.fpsmeterandhud.ui.theme.AccentAmber
import com.growtotype.fpsmeterandhud.ui.theme.AccentBlue
import com.growtotype.fpsmeterandhud.ui.theme.AccentGreen
import com.growtotype.fpsmeterandhud.ui.theme.AccentRed
import com.growtotype.fpsmeterandhud.viewmodel.MainViewModel

@Composable
fun MonitorTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sample by viewModel.hardwareSample.collectAsState()
    val recordingStatus by viewModel.recordingStatus.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val overlaySettings by viewModel.overlaySettings.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var selectedGameTag by remember { mutableStateOf("Genshin Impact") }
    var customGameTag by remember { mutableStateOf("") }
    var isCustomTagSelected by remember { mutableStateOf(false) }

    val gameTags = listOf("Genshin Impact", "PUBG Mobile", "Call of Duty", "Emulation", "Wild Rift", "Custom")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Live FPS Speedometer Card
        item {
            FpsSpeedometerCard(
                sample = sample,
                modifier = Modifier.testTag("fps_speedometer_card")
            )
        }

        // 2. Floating Overlay Control Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (overlaySettings.fpsOnlyMode) Icons.Default.CloseFullscreen else Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isOverlayActive) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (overlaySettings.fpsOnlyMode) "Pixel FPS Overlay" else "Floating HUD Overlay",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOverlayActive) "Active on screen • Drag anywhere" else "Show live FPS over games",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayActive) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                val success = viewModel.toggleOverlay(context)
                                if (!success) {
                                    showPermissionDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayActive) AccentRed else AccentGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("toggle_overlay_button")
                        ) {
                            Icon(
                                imageVector = if (isOverlayActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isOverlayActive) Color.White else Color(0xFF052E1E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOverlayActive) "Stop" else "Launch",
                                color = if (isOverlayActive) Color.White else Color(0xFF052E1E),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean 2-Option Mode Switcher (Pixel FPS-Only vs Full HUD)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (overlaySettings.fpsOnlyMode) AccentGreen else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings(overlaySettings.copy(fpsOnlyMode = true))
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pixel FPS-Only",
                                color = if (overlaySettings.fpsOnlyMode) Color(0xFF052E1E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!overlaySettings.fpsOnlyMode) AccentGreen else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings(overlaySettings.copy(fpsOnlyMode = false))
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Full HUD Mode",
                                color = if (!overlaySettings.fpsOnlyMode) Color(0xFF052E1E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 3. Session Performance Recorder Card (Spacious & Unclipped Record Button)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (recordingStatus.isRecording) AccentRed else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (recordingStatus.isRecording) AccentRed else AccentBlue,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (recordingStatus.isRecording) "Recording Active" else "Session Recorder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (recordingStatus.isRecording) AccentRed else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (recordingStatus.isRecording) {
                            Box(
                                modifier = Modifier
                                    .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${recordingStatus.elapsedSeconds}s • ${recordingStatus.currentAvgFps.toInt()} FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (recordingStatus.isRecording)
                            "Capturing real-time FPS, 1% lows, CPU, GPU, and RAM telemetry (${recordingStatus.sampleCount} samples logged)."
                        else
                            "Record your gaming session to inspect FPS timeline graphs, 1% lows, and memory usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Game Tag Selector when idle
                    AnimatedVisibility(visible = !recordingStatus.isRecording) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Text(
                                text = "Select Game / Session Tag:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(gameTags) { tag ->
                                    val isSelected = (tag == "Custom" && isCustomTagSelected) || (!isCustomTagSelected && tag == selectedGameTag)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) AccentGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) AccentGreen else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (tag == "Custom") {
                                                    isCustomTagSelected = true
                                                } else {
                                                    isCustomTagSelected = false
                                                    selectedGameTag = tag
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (isCustomTagSelected) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = customGameTag,
                                    onValueChange = { customGameTag = it },
                                    label = { Text("Custom Game Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full-Width Dedicated Record Button (Never cut off!)
                    Button(
                        onClick = {
                            if (recordingStatus.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                val tag = if (isCustomTagSelected && customGameTag.isNotBlank()) customGameTag else selectedGameTag
                                viewModel.startRecording(tag)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recordingStatus.isRecording) AccentRed else AccentBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_recording_button")
                    ) {
                        Icon(
                            imageVector = if (recordingStatus.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (recordingStatus.isRecording)
                                "Stop & Save Session (${recordingStatus.elapsedSeconds}s)"
                            else
                                "Start Recording Session",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. Real-Time Hardware Telemetry Grid (Cleanly Spaced)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Hardware Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricGridItem(
                        title = "CPU Usage",
                        value = "${sample.cpuUsage.toInt()}%",
                        subValue = if (sample.cpuTempC > 0) "${sample.cpuTempC.toInt()}°C Core" else "Active Load",
                        progress = sample.cpuUsage / 100f,
                        accentColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGridItem(
                        title = "GPU Load",
                        value = if (sample.gpuUsage >= 0) "${sample.gpuUsage.toInt()}%" else "Standby",
                        subValue = if (sample.gpuUsage >= 0) "Active Clock" else "Standard Mode",
                        progress = if (sample.gpuUsage >= 0) sample.gpuUsage / 100f else 0.15f,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricGridItem(
                        title = "RAM Usage",
                        value = "${(sample.ramUsedMb / 1024f).let { String.format("%.1f", it) }} GB",
                        subValue = "${sample.ramPercent.toInt()}% of ${(sample.ramTotalMb / 1024f).let { String.format("%.1f", it) }} GB",
                        progress = sample.ramPercent / 100f,
                        accentColor = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGridItem(
                        title = "Temperature",
                        value = "${sample.cpuTempC.toInt()}°C",
                        subValue = sample.thermalStatus,
                        progress = (sample.cpuTempC / 100f).coerceIn(0f, 1f),
                        accentColor = if (sample.cpuTempC > 55f) AccentRed else AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Overlay Permission Required", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "To display the live FPS counter over your games, please grant the 'Display over other apps' permission.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Open Settings", color = Color(0xFF052E1E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
