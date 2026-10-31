package com.growtotype.fpsmeterandhud.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.growtotype.fpsmeterandhud.model.HardwareSample
import com.growtotype.fpsmeterandhud.model.OverlaySettings
import kotlin.math.abs
import kotlin.math.min

@SuppressLint("ViewConstructor")
class OverlayHudView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onToggleRecording: () -> Unit,
    private val onCloseRequested: () -> Unit
) : View(context) {

    private var settings: OverlaySettings = OverlaySettings()
    private var sample: HardwareSample = HardwareSample()
    private var isRecording: Boolean = false
    private var recordingSeconds: Long = 0L

    var isCompact: Boolean = false
        private set

    // Ring buffer for sparkline
    private val fpsHistory = FloatArray(30) { 60f }
    private var fpsHistoryIndex = 0

    // Pre-allocated Paints (zero allocation during onDraw)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val sparklinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bgRect = RectF()
    private val graphRect = RectF()
    private val sparklinePath = Path()

    // Touch dragging state
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging: Boolean = false
    private val touchSlop = 10f

    // Interactive button rects (Expanded mode)
    private val recButtonRect = RectF()
    private val modeButtonRect = RectF()
    private val closeButtonRect = RectF()

    private val accentGreen = Color.parseColor("#10B981")
    private val accentRed = Color.parseColor("#EF4444")

    init {
        isCompact = settings.isCompactDefault || settings.fpsOnlyMode
        updateDimensions()
    }

    fun updateSettings(newSettings: OverlaySettings) {
        val prevScale = settings.scale
        val prevFpsOnly = settings.fpsOnlyMode
        val prevSpark = settings.showSparkline
        settings = newSettings
        if (settings.fpsOnlyMode) {
            isCompact = true
        }
        if (prevScale != newSettings.scale || prevFpsOnly != newSettings.fpsOnlyMode || prevSpark != newSettings.showSparkline) {
            updateDimensions()
        }
        invalidate()
    }

    fun updateSample(newSample: HardwareSample) {
        sample = newSample
        fpsHistory[fpsHistoryIndex % fpsHistory.size] = newSample.fps
        fpsHistoryIndex++
        invalidate()
    }

    fun updateRecordingState(recording: Boolean, seconds: Long) {
        isRecording = recording
        recordingSeconds = seconds
        invalidate()
    }

    private fun updateDimensions() {
        val density = resources.displayMetrics.density
        val scale = settings.scale.coerceIn(0.50f, 1.30f)

        val widthDp: Float
        val heightDp: Float

        if (settings.fpsOnlyMode) {
            // Pixel FPS-only minimal pill
            widthDp = 68f * scale
            heightDp = 28f * scale
        } else if (isCompact) {
            // Standard compact pill
            widthDp = 106f * scale
            heightDp = 34f * scale
        } else {
            // Full telemetry HUD with clean spacing
            widthDp = 216f * scale
            heightDp = if (settings.showSparkline) 168f * scale else 132f * scale
        }

        layoutParams.width = (widthDp * density).toInt().coerceAtLeast(1)
        layoutParams.height = (heightDp * density).toInt().coerceAtLeast(1)

        try {
            windowManager.updateViewLayout(this, layoutParams)
        } catch (_: Exception) {}
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgRect.set(2f, 2f, w - 2f, h - 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cornerRadius = if (settings.fpsOnlyMode || isCompact) h / 2f else 16f * resources.displayMetrics.density * settings.scale

        val alphaInt = (settings.opacity * 255).toInt().coerceIn(35, 255)
        if (settings.isDarkTheme) {
            bgPaint.color = Color.argb(alphaInt, 11, 15, 23)
            borderPaint.color = Color.argb(min(220, alphaInt + 30), 16, 185, 129)
        } else {
            bgPaint.color = Color.argb(alphaInt, 255, 255, 255)
            borderPaint.color = Color.argb(min(220, alphaInt + 30), 16, 185, 129)
        }

        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)

        if (settings.fpsOnlyMode) {
            drawPixelFpsOnly(canvas, w, h)
        } else if (isCompact) {
            drawCompactPill(canvas, w, h)
        } else {
            drawExpandedHud(canvas, w, h)
        }
    }

    private fun drawPixelFpsOnly(canvas: Canvas, w: Float, h: Float) {
        val centerY = h / 2f
        val primaryTextColor = if (settings.isDarkTheme) Color.WHITE else Color.parseColor("#0F172A")

        buttonBgPaint.color = if (isRecording) accentRed else accentGreen
        val dotRadius = (h * 0.14f).coerceAtLeast(2.5f)
        val dotX = w * 0.24f
        canvas.drawCircle(dotX, centerY, dotRadius, buttonBgPaint)

        centerTextPaint.color = primaryTextColor
        centerTextPaint.textSize = h * 0.52f
        val fm = centerTextPaint.fontMetrics
        val textBaseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText("${sample.fps.toInt()}", w * 0.60f, textBaseline, centerTextPaint)
    }

    private fun drawCompactPill(canvas: Canvas, w: Float, h: Float) {
        val centerY = h / 2f
        val primaryTextColor = if (settings.isDarkTheme) Color.WHITE else Color.parseColor("#0F172A")

        buttonBgPaint.color = if (isRecording) accentRed else accentGreen
        val dotRadius = (h * 0.14f).coerceAtLeast(3f)
        val dotX = w * 0.16f
        canvas.drawCircle(dotX, centerY, dotRadius, buttonBgPaint)

        centerTextPaint.color = primaryTextColor
        centerTextPaint.textSize = h * 0.44f
        val fm = centerTextPaint.fontMetrics
        val textBaseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText("${sample.fps.toInt()} FPS", w * 0.56f, textBaseline, centerTextPaint)
    }

    private fun drawExpandedHud(canvas: Canvas, w: Float, h: Float) {
        val density = resources.displayMetrics.density
        val scale = settings.scale.coerceIn(0.50f, 1.30f)
        val unit = density * scale

        val primaryTextColor = if (settings.isDarkTheme) Color.WHITE else Color.parseColor("#0F172A")
        val secondaryTextColor = if (settings.isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
        val controlBgColor = if (settings.isDarkTheme) Color.parseColor("#1E293B") else Color.parseColor("#E2E8F0")

        val padX = 12f * unit
        val topY = 10f * unit
        val btnH = 18f * unit

        // Header Left: Short clean title
        subTextPaint.color = secondaryTextColor
        subTextPaint.textSize = 9.5f * unit
        val headerCenterY = topY + btnH / 2f
        val subFm = subTextPaint.fontMetrics
        canvas.drawText("LIVE HUD", padX, headerCenterY - (subFm.ascent + subFm.descent) / 2f, subTextPaint)

        // Header Right: [ ● REC ]  [ — ]  [ ✕ ] with clean spacing
        var rightEdge = w - padX

        // 1. Close button [ ✕ ]
        val iconBtnW = 20f * unit
        closeButtonRect.set(rightEdge - iconBtnW, topY, rightEdge, topY + btnH)
        buttonBgPaint.color = controlBgColor
        canvas.drawRoundRect(closeButtonRect, 5f * unit, 5f * unit, buttonBgPaint)
        centerTextPaint.color = secondaryTextColor
        centerTextPaint.textSize = 9.5f * unit
        val btnFm = centerTextPaint.fontMetrics
        val btnBaseline = headerCenterY - (btnFm.ascent + btnFm.descent) / 2f
        canvas.drawText("✕", closeButtonRect.centerX(), btnBaseline, centerTextPaint)

        rightEdge -= (iconBtnW + 5f * unit)

        // 2. Minimize button [ — ]
        modeButtonRect.set(rightEdge - iconBtnW, topY, rightEdge, topY + btnH)
        buttonBgPaint.color = controlBgColor
        canvas.drawRoundRect(modeButtonRect, 5f * unit, 5f * unit, buttonBgPaint)
        canvas.drawText("—", modeButtonRect.centerX(), btnBaseline, centerTextPaint)

        rightEdge -= (iconBtnW + 5f * unit)

        // 3. Record button [ ● REC ] / [ ■ 12s ] - wide enough so text is never cut off
        val recBtnW = 48f * unit
        recButtonRect.set(rightEdge - recBtnW, topY, rightEdge, topY + btnH)
        buttonBgPaint.color = if (isRecording) accentRed else accentGreen
        canvas.drawRoundRect(recButtonRect, 5f * unit, 5f * unit, buttonBgPaint)
        centerTextPaint.color = if (isRecording) Color.WHITE else Color.parseColor("#052E1E")
        centerTextPaint.textSize = 8.5f * unit
        val recFm = centerTextPaint.fontMetrics
        val recBaseline = headerCenterY - (recFm.ascent + recFm.descent) / 2f
        val recLabel = if (isRecording) "■ ${recordingSeconds}s" else "● REC"
        canvas.drawText(recLabel, recButtonRect.centerX(), recBaseline, centerTextPaint)

        // Row 2: Big FPS Display (Well separated below header)
        var currentY = topY + btnH + 26f * unit
        if (settings.showFps) {
            textPaint.color = accentGreen
            textPaint.textSize = 24f * unit
            val fpsStr = "${sample.fps.toInt()}"
            canvas.drawText(fpsStr, padX, currentY, textPaint)

            subTextPaint.color = secondaryTextColor
            subTextPaint.textSize = 10f * unit
            val fpsWidth = textPaint.measureText(fpsStr)
            canvas.drawText("FPS • ${sample.targetFps}Hz", padX + fpsWidth + 8f * unit, currentY - 3f * unit, subTextPaint)

            currentY += 19f * unit
        } else {
            currentY = topY + btnH + 16f * unit
        }

        // Row 3: CPU & GPU (Two cleanly separated columns)
        val col1X = padX
        val col2X = w * 0.53f
        subTextPaint.textSize = 9.5f * unit
        subTextPaint.color = primaryTextColor

        if (settings.showCpu || settings.showGpu) {
            if (settings.showCpu) {
                canvas.drawText("CPU: ${sample.cpuUsage.toInt()}%", col1X, currentY, subTextPaint)
            }
            if (settings.showGpu) {
                val gpuText = if (sample.gpuUsage >= 0f) "GPU: ${sample.gpuUsage.toInt()}%" else "GPU: --"
                canvas.drawText(gpuText, col2X, currentY, subTextPaint)
            }
            currentY += 15f * unit
        }

        // Row 4: RAM & Temperature (Two cleanly separated columns)
        if (settings.showRam || settings.showTemp) {
            subTextPaint.color = secondaryTextColor
            if (settings.showRam) {
                val ramGb = String.format("%.1f", sample.ramUsedMb / 1024f)
                val ramTotGb = String.format("%.0f", sample.ramTotalMb / 1024f)
                canvas.drawText("RAM: ${ramGb}/${ramTotGb}GB", col1X, currentY, subTextPaint)
            }
            if (settings.showTemp) {
                val tempText = if (sample.cpuTempC > 0f) "TEMP: ${sample.cpuTempC.toInt()}°C" else "TEMP: --"
                canvas.drawText(tempText, col2X, currentY, subTextPaint)
            }
            currentY += 10f * unit
        }

        // Row 5: Sparkline Graph
        if (settings.showSparkline) {
            val graphTop = currentY + 4f * unit
            val graphBottom = h - 10f * unit
            drawSparkline(canvas, padX, graphTop, w - padX * 2f, graphBottom - graphTop, controlBgColor)
        }
    }

    private fun drawSparkline(canvas: Canvas, x: Float, y: Float, graphW: Float, graphH: Float, trackColor: Int) {
        if (graphW <= 10f || graphH <= 8f) return

        graphRect.set(x, y, x + graphW, y + graphH)
        bgPaint.color = trackColor
        canvas.drawRoundRect(graphRect, 6f, 6f, bgPaint)

        val target = sample.targetFps.toFloat().coerceAtLeast(30f)
        val maxVal = target * 1.15f
        val minVal = 0f

        sparklinePath.reset()
        val count = fpsHistory.size
        val stepX = graphW / (count - 1).toFloat()

        for (i in 0 until count) {
            val idx = (fpsHistoryIndex + i) % count
            val fpsVal = fpsHistory[idx].coerceIn(minVal, maxVal)
            val normY = 1f - ((fpsVal - minVal) / (maxVal - minVal))
            val posX = x + (i * stepX)
            val posY = (y + 2f) + (normY * (graphH - 4f))

            if (i == 0) {
                sparklinePath.moveTo(posX, posY)
            } else {
                sparklinePath.lineTo(posX, posY)
            }
        }

        sparklinePaint.color = accentGreen
        canvas.drawPath(sparklinePath, sparklinePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    layoutParams.x = initialX + dx.toInt()
                    layoutParams.y = initialY + dy.toInt()
                    try {
                        windowManager.updateViewLayout(this, layoutParams)
                    } catch (_: Exception) {}
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    handleClick(event.x, event.y)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleClick(x: Float, y: Float) {
        if (settings.fpsOnlyMode) {
            return
        }

        if (isCompact) {
            isCompact = false
            updateDimensions()
            invalidate()
            return
        }

        when {
            recButtonRect.contains(x, y) -> onToggleRecording()
            modeButtonRect.contains(x, y) -> {
                isCompact = true
                updateDimensions()
                invalidate()
            }
            closeButtonRect.contains(x, y) -> onCloseRequested()
        }
    }
}
