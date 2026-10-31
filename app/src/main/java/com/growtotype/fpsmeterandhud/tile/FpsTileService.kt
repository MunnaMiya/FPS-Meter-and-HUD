package com.growtotype.fpsmeterandhud.tile

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.growtotype.fpsmeterandhud.MainActivity
import com.growtotype.fpsmeterandhud.overlay.FpsOverlayService

class FpsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Check if overlay permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14 to Android 17+ requires PendingIntent for startActivityAndCollapse
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                @Suppress("DEPRECATION")
                startActivityAndCollapse(permIntent)
            }
            return
        }

        // Toggle Overlay
        if (FpsOverlayService.isServiceRunning) {
            FpsOverlayService.stopOverlay(this)
        } else {
            FpsOverlayService.startOverlay(this)
        }

        qsTile?.let { tile ->
            tile.state = if (FpsOverlayService.isServiceRunning) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.label = if (tile.state == Tile.STATE_ACTIVE) "FPS: Active" else "FPS Overlay"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (tile.state == Tile.STATE_ACTIVE) "Running" else "Off"
            }
            tile.updateTile()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = FpsOverlayService.isServiceRunning
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "FPS: Active" else "FPS Overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) "Running" else "Tap to Start"
        }
        tile.updateTile()
    }
}
