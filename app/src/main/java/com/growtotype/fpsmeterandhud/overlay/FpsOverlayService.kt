package com.growtotype.fpsmeterandhud.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.growtotype.fpsmeterandhud.MainActivity
import com.growtotype.fpsmeterandhud.R
import com.growtotype.fpsmeterandhud.data.SettingsRepository
import com.growtotype.fpsmeterandhud.db.AppDatabase
import com.growtotype.fpsmeterandhud.telemetry.HardwareMonitor
import com.growtotype.fpsmeterandhud.telemetry.SessionRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FpsOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayHudView: OverlayHudView? = null
    private var hardwareMonitor: HardwareMonitor? = null
    private var sessionRecorder: SessionRecorder? = null
    private var settingsRepository: SettingsRepository? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true

        val database = AppDatabase.getInstance(this)
        val monitor = HardwareMonitor.getInstance(this)
        hardwareMonitor = monitor
        sessionRecorder = SessionRecorder.getInstance(database, monitor)
        settingsRepository = SettingsRepository.getInstance(this)

        startForegroundNotification()
        setupOverlayView()
        observeTelemetry()

        monitor.start()
    }

    private fun startForegroundNotification() {
        val channelId = "fps_overlay_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FPS Overlay Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live gaming telemetry and HUD overlay"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FPS Overlay Active")
            .setContentText("Monitoring live FPS & hardware telemetry.")
            .setSmallIcon(R.drawable.ic_fps_hud_tile)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // Android 14 to Android 17+ requires specifying foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 140
        }

        overlayHudView = OverlayHudView(
            context = this,
            windowManager = windowManager!!,
            layoutParams = layoutParams,
            onToggleRecording = {
                val recorder = sessionRecorder ?: return@OverlayHudView
                if (recorder.status.value.isRecording) {
                    recorder.stopRecording()
                } else {
                    recorder.startRecording("Overlay Session")
                }
            },
            onCloseRequested = {
                stopSelf()
            }
        )

        settingsRepository?.settings?.value?.let { currentSettings ->
            overlayHudView?.updateSettings(currentSettings)
        }

        try {
            windowManager?.addView(overlayHudView, layoutParams)
        } catch (_: Exception) {}
    }

    private fun observeTelemetry() {
        serviceScope.launch {
            hardwareMonitor?.currentSample?.collectLatest { sample ->
                overlayHudView?.updateSample(sample)
            }
        }

        serviceScope.launch {
            sessionRecorder?.status?.collectLatest { status ->
                overlayHudView?.updateRecordingState(status.isRecording, status.elapsedSeconds)
            }
        }

        serviceScope.launch {
            settingsRepository?.settings?.collectLatest { settings ->
                overlayHudView?.updateSettings(settings)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false

        overlayHudView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayHudView = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        @Volatile var isServiceRunning = false

        fun startOverlay(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
