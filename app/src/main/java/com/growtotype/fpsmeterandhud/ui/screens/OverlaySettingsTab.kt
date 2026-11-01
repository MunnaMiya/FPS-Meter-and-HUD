package com.growtotype.fpsmeterandhud.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growtotype.fpsmeterandhud.model.OverlaySettings
import com.growtotype.fpsmeterandhud.ui.theme.AccentBlue
import com.growtotype.fpsmeterandhud.ui.theme.AccentGreen
import com.growtotype.fpsmeterandhud.viewmodel.MainViewModel

@Composable
fun OverlaySettingsTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.overlaySettings.collectAsState()

    val scalePresets = listOf(
        Pair("50% Tiny", 0.50f),
        Pair("75% Small", 0.75f),
        Pair("100% Normal", 1.00f),
        Pair("125% Large", 1.25f)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Live Overlay Preview Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Overlay Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.fpsOnlyMode) "Pixel Minimal FPS-Only Pill" else "Full Telemetry HUD",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${(settings.scale * 100).toInt()}% Size",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OverlayHudPreviewCard(settings = settings)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 2. App Appearance (1 Dark Theme & 1 Light Theme Only)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch between Dark and Light interface & overlay themes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThemeOptionButton(
                            label = "Dark Theme",
                            icon = Icons.Default.Brightness4,
                            isSelected = settings.isDarkTheme,
                            onClick = { viewModel.updateSettings(settings.copy(isDarkTheme = true)) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            label = "Light Theme",
                            icon = Icons.Default.Brightness7,
                            isSelected = !settings.isDarkTheme,
                            onClick = { viewModel.updateSettings(settings.copy(isDarkTheme = false)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Pixel FPS-Only Mode Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (settings.fpsOnlyMode) AccentGreen else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloseFullscreen,
                                contentDescription = null,
                                tint = if (settings.fpsOnlyMode) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pixel FPS-Only Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Small pill showing only live FPS digits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.fpsOnlyMode,
                            onCheckedChange = { isChecked ->
                                viewModel.updateSettings(settings.copy(fpsOnlyMode = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF052E1E),
                                checkedTrackColor = AccentGreen
                            ),
                            modifier = Modifier.testTag("pixel_fps_mode_switch")
                        )
                    }
                }
            }
        }

        // 4. Custom Resize & Transparency Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overlay Size & Opacity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${(settings.scale * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = settings.scale,
                        onValueChange = { viewModel.updateSettings(settings.copy(scale = it)) },
                        valueRange = 0.50f..1.30f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGreen,
                            activeTrackColor = AccentGreen
                        ),
                        modifier = Modifier.testTag("scale_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scalePresets) { (label, presetScale) ->
                            val isSelected = (settings.scale - presetScale) in -0.05f..0.05f
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) AccentGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGreen else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateSettings(settings.copy(scale = presetScale)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HUD Opacity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(settings.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentGreen
                        )
                    }
                    Slider(
                        value = settings.opacity,
                        onValueChange = { viewModel.updateSettings(settings.copy(opacity = it)) },
                        valueRange = 0.25f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGreen,
                            activeTrackColor = AccentGreen
                        ),
                        modifier = Modifier.testTag("opacity_slider")
                    )
                }
            }
        }

        // 5. Expanded HUD Metrics Toggles
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Expanded HUD Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which telemetry stats appear in Full HUD mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingToggleRow(
                        title = "CPU Usage %",
                        description = "Show real-time processor utilization",
                        checked = settings.showCpu,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showCpu = it)) }
                    )
                    SettingToggleRow(
                        title = "GPU Load %",
                        description = "Show graphics load when supported",
                        checked = settings.showGpu,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showGpu = it)) }
                    )
                    SettingToggleRow(
                        title = "RAM Consumption",
                        description = "Show used and total system memory",
                        checked = settings.showRam,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showRam = it)) }
                    )
                    SettingToggleRow(
                        title = "Hardware Temperature",
                        description = "Show thermal sensor temperature in °C",
                        checked = settings.showTemp,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showTemp = it)) }
                    )
                    SettingToggleRow(
                        title = "Live FPS Sparkline",
                        description = "Show real-time frame pacing graph at bottom of HUD",
                        checked = settings.showSparkline,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showSparkline = it)) }
                    )
                }
            }
        }

        // 6. SurfaceFlinger Real Render FPS (Android 10 60Hz Lock Fix)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (settings.useSurfaceFlingerFps) AccentGreen else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Android 10 Render-Rate Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Auto-enabled on Android 10 (API 29). Leave off on Android 11–17 unless needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "SurfaceFlinger Real Render FPS",
                        description = "Reads actual game buffer page-flips via persistent root shell instead of 60Hz screen VSYNC lock",
                        checked = settings.useSurfaceFlingerFps,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(useSurfaceFlingerFps = it))
                        }
                    )
                }
            }
        }

        // 7. Control Center Quick Settings Tile Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Control Center Tile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Swipe down your notification shade twice, tap the Edit pencil icon, and add the 'FPS Overlay' tile to toggle the FPS counter anytime during gameplay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF052E1E),
                checkedTrackColor = AccentGreen
            )
        )
    }
}

@Composable
fun ThemeOptionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                1.5.dp,
                if (isSelected) AccentGreen else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun OverlayHudPreviewCard(settings: OverlaySettings) {
    val alpha = settings.opacity.coerceIn(0.25f, 1f)
    val scale = settings.scale.coerceIn(0.50f, 1.30f)

    val bgColor = if (settings.isDarkTheme) Color(0xFF0B0F17).copy(alpha = alpha) else Color(0xFFFFFFFF).copy(alpha = alpha)
    val primaryText = if (settings.isDarkTheme) Color.White else Color(0xFF0F172A)
    val secondaryText = if (settings.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
    val controlBg = if (settings.isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    if (settings.fpsOnlyMode) {
        val pillWidth = 68.dp * scale
        val pillHeight = 28.dp * scale

        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(pillHeight)
                .background(bgColor, RoundedCornerShape(14.dp * scale))
                .border(1.5.dp, AccentGreen, RoundedCornerShape(14.dp * scale))
                .padding(horizontal = 6.dp * scale),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp * scale)
                        .background(AccentGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp * scale))
                Text(
                    text = "120",
                    color = primaryText,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (13.5f * scale).sp
                )
            }
        }
    } else {
        val previewWidth = 216.dp * scale
        val previewHeight = (if (settings.showSparkline) 168.dp else 132.dp) * scale

        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .background(bgColor, RoundedCornerShape(16.dp * scale))
                .border(1.5.dp, AccentGreen, RoundedCornerShape(16.dp * scale))
                .padding(12.dp * scale)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Row 1: Header + Unclipped [● REC] [—] [✕]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE HUD",
                        color = secondaryText,
                        fontSize = (9 * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp * scale),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AccentGreen, RoundedCornerShape(4.dp * scale))
                                .padding(horizontal = 6.dp * scale, vertical = 2.dp * scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "● REC",
                                color = Color(0xFF052E1E),
                                fontSize = (8 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(controlBg, RoundedCornerShape(4.dp * scale))
                                .padding(horizontal = 5.dp * scale, vertical = 2.dp * scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("—", color = secondaryText, fontSize = (8 * scale).sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(controlBg, RoundedCornerShape(4.dp * scale))
                                .padding(horizontal = 5.dp * scale, vertical = 2.dp * scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = secondaryText, fontSize = (8 * scale).sp)
                        }
                    }
                }

                // Row 2: Big FPS
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "120",
                        color = AccentGreen,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (22 * scale).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp * scale))
                    Text(
                        text = "FPS • 120Hz",
                        color = secondaryText,
                        fontSize = (9.5f * scale).sp,
                        modifier = Modifier.padding(bottom = 2.dp * scale)
                    )
                }

                // Row 3 & 4: Clean 2x2 Grid for CPU, GPU, RAM, TEMP
                Column(verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (settings.showCpu) {
                            Text(
                                text = "CPU: 38%",
                                color = primaryText,
                                fontSize = (9 * scale).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (settings.showGpu) {
                            Text(
                                text = "GPU: 54%",
                                color = primaryText,
                                fontSize = (9 * scale).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (settings.showRam) {
                            Text(
                                text = "RAM: 4.2/8GB",
                                color = secondaryText,
                                fontSize = (8.5f * scale).sp
                            )
                        }
                        if (settings.showTemp) {
                            Text(
                                text = "TEMP: 39°C",
                                color = secondaryText,
                                fontSize = (8.5f * scale).sp
                            )
                        }
                    }
                }

                // Row 5: Sparkline
                if (settings.showSparkline) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp * scale)
                            .background(controlBg, RoundedCornerShape(4.dp * scale))
                    ) {
                        val w = size.width
                        val h = size.height
                        val points = listOf(0.7f, 0.75f, 0.72f, 0.8f, 0.78f, 0.85f, 0.74f, 0.82f, 0.84f, 0.88f)
                        val step = w / (points.size - 1)
                        val p = Path()
                        points.forEachIndexed { i, pt ->
                            val x = i * step
                            val y = h - (pt * h)
                            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        drawPath(p, color = AccentGreen, style = Stroke(width = 2f * scale, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}
