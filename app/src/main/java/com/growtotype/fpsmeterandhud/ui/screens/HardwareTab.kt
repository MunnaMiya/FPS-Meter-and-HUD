package com.growtotype.fpsmeterandhud.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import com.growtotype.fpsmeterandhud.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growtotype.fpsmeterandhud.ui.theme.NeonAmber
import com.growtotype.fpsmeterandhud.ui.theme.NeonCyan
import com.growtotype.fpsmeterandhud.ui.theme.NeonGreen
import com.growtotype.fpsmeterandhud.viewmodel.MainViewModel

@Composable
fun HardwareTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val privilegeStatus by viewModel.privilegeStatus.collectAsState()
    val sample by viewModel.hardwareSample.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardware Access & Privileges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Access Mode: ${privilegeStatus.activeMode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.refreshPrivileges() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("refresh_privileges_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recheck",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recheck", color = NeonCyan)
                    }
                }
            }
        }

        // Root Access Card
        item {
            PrivilegeCard(
                title = "Root Access (Superuser)",
                status = when {
                    privilegeStatus.isRootGranted -> "Granted & Active"
                    privilegeStatus.isRootAvailable -> "Binary Found (Permission Needed)"
                    else -> "Not Detected"
                },
                isActive = privilegeStatus.isRootGranted,
                isAvailable = privilegeStatus.isRootAvailable,
                description = "Enables direct SurfaceFlinger frame pacing latency hooks, full /proc/stat CPU time accounting, Adreno/Mali GPU hardware clock registers, and unrestricted thermal zones.",
                icon = Icons.Default.Terminal
            )
        }

        // Shizuku Access Card
        item {
            PrivilegeCard(
                title = "Shizuku Service (ADB)",
                status = when {
                    privilegeStatus.isShizukuRunning -> "Running & Bound"
                    privilegeStatus.isShizukuInstalled -> "Installed (Service Stopped)"
                    else -> "Not Installed"
                },
                isActive = privilegeStatus.isShizukuRunning,
                isAvailable = privilegeStatus.isShizukuInstalled,
                description = "Allows system-level dumpsys execution and hardware telemetry without rooting your device. Pairs via Wireless Debugging on Android 11+ or USB ADB.",
                icon = Icons.Default.Security
            )
        }

        // Standard / Non-Root Fallback Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Non-Root Android 10+ Engine (Active)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When Root or Shizuku are not present, the app automatically runs its zero-permission hardware telemetry suite:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FeatureBullet(title = "Choreographer Frame Pacing", desc = "Listens directly to Android VSYNC frame signals and calculates nanosecond frame deltas with moving window smoothing.")
                    FeatureBullet(title = "Display Refresh Rate Tracking", desc = "Detects 60Hz, 90Hz, 120Hz, 144Hz high refresh rate screens via DisplayManager.")
                    FeatureBullet(title = "System RAM Statistics", desc = "Reads exact total, used, and free memory via ActivityManager.MemoryInfo without needing root.")
                    FeatureBullet(title = "Android 10+ Thermal Status", desc = "Subscribes to PowerManager.OnThermalStatusChangedListener to detect throttling stages.")
                    FeatureBullet(title = "CPU Core Frequencies", desc = "Reads /sys/devices/system/cpu clock scaling frequencies to compute core load.")
                }
            }
        }

        // Device & SoC Audit
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
                        text = "Device & Hardware Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SpecRow(label = "Device Model", value = "${Build.MANUFACTURER} ${Build.MODEL}")
                    SpecRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    SpecRow(label = "Hardware / Board", value = "${Build.HARDWARE} (${Build.BOARD})")
                    SpecRow(label = "CPU Cores", value = "${Runtime.getRuntime().availableProcessors()} Cores")
                    SpecRow(label = "Display Target Rate", value = "${sample.targetFps} Hz")
                    SpecRow(
                        label = "Total Physical RAM",
                        value = "${(sample.ramTotalMb / 1024f).let { String.format("%.2f", it) }} GB"
                    )
                }
            }
        }

        // About Section (GrowToType / Munna Miya)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .testTag("about_section_card"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_custom),
                            contentDescription = "FPS Meter & HUD App Icon",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "About FPS Meter & HUD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "An app from GrowToType. Designed by Munna Miya",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Lightweight, high-precision real-time frame rate HUD overlay, CPU/GPU thermal monitor, memory consumption tracker, and gaming session recorder built for Android 10 through Android 17.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    SpecRow(label = "Publisher", value = "GrowToType")
                    SpecRow(label = "Designer", value = "Munna Miya")
                    SpecRow(label = "App Version", value = "1.0 (Production)")
                    SpecRow(label = "OS Support", value = "Android 10 – Android 17 (API 29–36+)")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PrivilegeCard(
    title: String,
    status: String,
    isActive: Boolean,
    isAvailable: Boolean,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val statusColor = when {
        isActive -> NeonGreen
        isAvailable -> NeonAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeatureBullet(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("• ", color = NeonGreen, fontWeight = FontWeight.Bold)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
