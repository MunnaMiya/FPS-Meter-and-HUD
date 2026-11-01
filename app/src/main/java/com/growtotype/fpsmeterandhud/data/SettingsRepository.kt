package com.growtotype.fpsmeterandhud.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.growtotype.fpsmeterandhud.model.OverlaySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fps_overlay_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<OverlaySettings> = _settings.asStateFlow()

    private fun loadSettings(): OverlaySettings {
        val defaultSfFps = (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
        return OverlaySettings(
            opacity = prefs.getFloat("opacity", 0.85f),
            scale = prefs.getFloat("scale", 1.0f),
            fpsOnlyMode = prefs.getBoolean("fpsOnlyMode", false),
            isDarkTheme = prefs.getBoolean("isDarkTheme", true),
            showFps = prefs.getBoolean("showFps", true),
            showCpu = prefs.getBoolean("showCpu", true),
            showGpu = prefs.getBoolean("showGpu", true),
            showRam = prefs.getBoolean("showRam", true),
            showTemp = prefs.getBoolean("showTemp", true),
            showSparkline = prefs.getBoolean("showSparkline", true),
            isCompactDefault = prefs.getBoolean("isCompactDefault", false),
            targetRefreshRate = prefs.getInt("targetRefreshRate", 0),
            preferredPrivilege = prefs.getString("preferredPrivilege", "AUTO") ?: "AUTO",
            useSurfaceFlingerFps = prefs.getBoolean("useSurfaceFlingerFps", defaultSfFps)
        )
    }

    fun updateSettings(newSettings: OverlaySettings) {
        prefs.edit()
            .putFloat("opacity", newSettings.opacity)
            .putFloat("scale", newSettings.scale)
            .putBoolean("fpsOnlyMode", newSettings.fpsOnlyMode)
            .putBoolean("isDarkTheme", newSettings.isDarkTheme)
            .putBoolean("showFps", newSettings.showFps)
            .putBoolean("showCpu", newSettings.showCpu)
            .putBoolean("showGpu", newSettings.showGpu)
            .putBoolean("showRam", newSettings.showRam)
            .putBoolean("showTemp", newSettings.showTemp)
            .putBoolean("showSparkline", newSettings.showSparkline)
            .putBoolean("isCompactDefault", newSettings.isCompactDefault)
            .putInt("targetRefreshRate", newSettings.targetRefreshRate)
            .putString("preferredPrivilege", newSettings.preferredPrivilege)
            .putBoolean("useSurfaceFlingerFps", newSettings.useSurfaceFlingerFps)
            .apply()

        _settings.value = newSettings
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
