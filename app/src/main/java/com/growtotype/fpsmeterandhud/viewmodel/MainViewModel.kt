package com.growtotype.fpsmeterandhud.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.growtotype.fpsmeterandhud.data.SettingsRepository
import com.growtotype.fpsmeterandhud.db.AppDatabase
import com.growtotype.fpsmeterandhud.model.GamingSession
import com.growtotype.fpsmeterandhud.model.HardwareSample
import com.growtotype.fpsmeterandhud.model.OverlaySettings
import com.growtotype.fpsmeterandhud.overlay.FpsOverlayService
import com.growtotype.fpsmeterandhud.telemetry.HardwareMonitor
import com.growtotype.fpsmeterandhud.telemetry.PrivilegeStatus
import com.growtotype.fpsmeterandhud.telemetry.RecordingStatus
import com.growtotype.fpsmeterandhud.telemetry.SessionRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val hardwareMonitor = HardwareMonitor.getInstance(application)
    val sessionRecorder = SessionRecorder.getInstance(database, hardwareMonitor)

    val hardwareSample: StateFlow<HardwareSample> = hardwareMonitor.currentSample
    val privilegeStatus: StateFlow<PrivilegeStatus> = hardwareMonitor.privilegeStatus
    val recordingStatus: StateFlow<RecordingStatus> = sessionRecorder.status
    val overlaySettings: StateFlow<OverlaySettings> = settingsRepo.settings

    val savedSessions: StateFlow<List<GamingSession>> = database.sessionDao()
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSession = MutableStateFlow<GamingSession?>(null)
    val selectedSession: StateFlow<GamingSession?> = _selectedSession.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(FpsOverlayService.isServiceRunning)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    init {
        hardwareMonitor.start()

        viewModelScope.launch {
            while (isActive) {
                _isOverlayActive.value = FpsOverlayService.isServiceRunning
                delay(1000L)
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun toggleOverlay(context: Context): Boolean {
        if (!hasOverlayPermission(context)) {
            return false
        }

        if (_isOverlayActive.value) {
            FpsOverlayService.stopOverlay(context)
            _isOverlayActive.value = false
        } else {
            FpsOverlayService.startOverlay(context)
            _isOverlayActive.value = true
        }
        return true
    }

    fun startRecording(gameTag: String) {
        sessionRecorder.startRecording(gameTag)
    }

    fun stopRecording() {
        sessionRecorder.stopRecording { saved ->
            _selectedSession.value = saved
        }
    }

    fun selectSession(session: GamingSession?) {
        _selectedSession.value = session
    }

    fun deleteSession(session: GamingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            database.sessionDao().deleteSession(session)
            if (_selectedSession.value?.id == session.id) {
                _selectedSession.value = null
            }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            database.sessionDao().clearAll()
            _selectedSession.value = null
        }
    }

    fun updateSettings(newSettings: OverlaySettings) {
        settingsRepo.updateSettings(newSettings)
    }

    fun refreshPrivileges() {
        hardwareMonitor.refreshPrivileges()
    }
}
