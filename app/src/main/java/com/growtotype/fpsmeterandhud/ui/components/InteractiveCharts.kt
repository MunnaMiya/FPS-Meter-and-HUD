package com.growtotype.fpsmeterandhud.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growtotype.fpsmeterandhud.ui.theme.AccentAmber
import com.growtotype.fpsmeterandhud.ui.theme.AccentBlue
import com.growtotype.fpsmeterandhud.ui.theme.AccentGreen
import com.growtotype.fpsmeterandhud.ui.theme.AccentRed
import org.json.JSONArray
import kotlin.math.max

@Immutable
data class ChartPoint(
    val timeSec: Long,
    val fps: Float,
    val cpu: Float,
    val gpu: Float,
    val ramMb: Int,
    val tempC: Float
)

fun parseSamplesJson(json: String): List<ChartPoint> {
    val list = ArrayList<ChartPoint>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ChartPoint(
                    timeSec = obj.optLong("t", i.toLong()),
                    fps = obj.optDouble("f", 60.0).toFloat(),
                    cpu = obj.optDouble("c", 0.0).toFloat(),
                    gpu = obj.optDouble("g", -1.0).toFloat(),
                    ramMb = obj.optInt("r", 0),
                    tempC = obj.optDouble("tm", 0.0).toFloat()
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

@Composable
fun FpsTimelineChart(
    samples: List<ChartPoint>,
    avgFps: Float,
    onePercentLow: Float,
    modifier: Modifier = Modifier
) {
    val chartBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val cursorColor = MaterialTheme.colorScheme.onSurface

    if (samples.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(chartBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No sample data recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxFpsVal = max(120f, (samples.maxOfOrNull { it.fps } ?: 60f) * 1.1f)

    Column(modifier = modifier) {
        // Scrubbing Info Header with generous spacing
        val activePoint = selectedIndex?.let { samples.getOrNull(it) } ?: samples.last()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time: ${activePoint.timeSec}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${activePoint.fps.toInt()} FPS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (activePoint.fps < onePercentLow) AccentRed else AccentGreen
                )
                Text(
                    text = "CPU: ${activePoint.cpu.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentBlue
                )
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(chartBgColor, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
                .pointerInput(samples) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val step = width / (samples.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / step).toInt().coerceIn(0, samples.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(samples) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val step = width / (samples.size - 1).coerceAtLeast(1)
                        val idx = (change.position.x / step).toInt().coerceIn(0, samples.lastIndex)
                        selectedIndex = idx
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val count = samples.size
                val stepX = w / (count - 1).coerceAtLeast(1)

                val referenceLevels = listOf(30f, 60f, 90f, 120f)
                referenceLevels.forEach { ref ->
                    if (ref <= maxFpsVal) {
                        val y = h - ((ref / maxFpsVal) * h)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5f
                        )
                    }
                }

                if (onePercentLow > 0f) {
                    val lowY = h - ((onePercentLow / maxFpsVal) * h)
                    drawLine(
                        color = AccentRed.copy(alpha = 0.55f),
                        start = Offset(0f, lowY),
                        end = Offset(w, lowY),
                        strokeWidth = 2f
                    )
                }

                if (avgFps > 0f) {
                    val avgY = h - ((avgFps / maxFpsVal) * h)
                    drawLine(
                        color = AccentAmber.copy(alpha = 0.55f),
                        start = Offset(0f, avgY),
                        end = Offset(w, avgY),
                        strokeWidth = 2f
                    )
                }

                val fpsPath = Path()
                samples.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = h - ((pt.fps / maxFpsVal) * h).coerceIn(0f, h)
                    if (i == 0) {
                        fpsPath.moveTo(x, y)
                    } else {
                        fpsPath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = fpsPath,
                    color = AccentGreen,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                selectedIndex?.let { idx ->
                    val x = idx * stepX
                    val pt = samples[idx]
                    val y = h - ((pt.fps / maxFpsVal) * h).coerceIn(0f, h)

                    drawLine(
                        color = cursorColor.copy(alpha = 0.6f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.5f
                    )

                    drawCircle(
                        color = cursorColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = if (pt.fps < onePercentLow) AccentRed else AccentGreen,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cleanly spaced legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = AccentGreen, label = "FPS")
            LegendItem(color = AccentAmber, label = "Avg (${avgFps.toInt()})")
            LegendItem(color = AccentRed, label = "1% Low (${onePercentLow.toInt()})")
        }
    }
}

@Composable
fun MemoryPatternChart(
    samples: List<ChartPoint>,
    totalRamMb: Int,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) return

    val chartBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val minRam = (samples.minOfOrNull { it.ramMb } ?: 0).toFloat()
    val maxRam = (samples.maxOfOrNull { it.ramMb } ?: totalRamMb).toFloat()
    val lowerBound = max(0f, minRam - 200f)
    val upperBound = max(lowerBound + 200f, maxRam + 200f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RAM Usage Over Time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Peak: ${(maxRam / 1024f).let { String.format("%.2f", it) }} GB",
                style = MaterialTheme.typography.bodySmall,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(chartBgColor, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val count = samples.size
                val stepX = w / (count - 1).coerceAtLeast(1)

                val ramPath = Path()
                val fillPath = Path()

                samples.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val norm = ((pt.ramMb - lowerBound) / (upperBound - lowerBound)).coerceIn(0f, 1f)
                    val y = h - (norm * h)

                    if (i == 0) {
                        ramPath.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        ramPath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(w, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    color = AccentBlue.copy(alpha = 0.16f)
                )

                drawPath(
                    path = ramPath,
                    color = AccentBlue,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun HardwareLoadChart(
    samples: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) return

    val chartBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(modifier = modifier) {
        // Legend on its own clean row so it never collides with title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = AccentBlue, label = "CPU %")
            LegendItem(color = AccentAmber, label = "GPU %")
            LegendItem(color = AccentRed, label = "Temp °C")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(chartBgColor, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val count = samples.size
                val stepX = w / (count - 1).coerceAtLeast(1)

                val cpuPath = Path()
                val gpuPath = Path()
                val tempPath = Path()
                var hasGpu = false

                samples.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val cpuY = h - ((pt.cpu / 100f).coerceIn(0f, 1f) * h)
                    val tempY = h - ((pt.tempC / 100f).coerceIn(0f, 1f) * h)

                    if (i == 0) {
                        cpuPath.moveTo(x, cpuY)
                        tempPath.moveTo(x, tempY)
                    } else {
                        cpuPath.lineTo(x, cpuY)
                        tempPath.lineTo(x, tempY)
                    }

                    if (pt.gpu >= 0f) {
                        hasGpu = true
                        val gpuY = h - ((pt.gpu / 100f).coerceIn(0f, 1f) * h)
                        if (i == 0) gpuPath.moveTo(x, gpuY) else gpuPath.lineTo(x, gpuY)
                    }
                }

                drawPath(path = cpuPath, color = AccentBlue, style = Stroke(width = 2.5f))
                if (hasGpu) {
                    drawPath(path = gpuPath, color = AccentAmber, style = Stroke(width = 2.5f))
                }
                drawPath(path = tempPath, color = AccentRed, style = Stroke(width = 2f))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
