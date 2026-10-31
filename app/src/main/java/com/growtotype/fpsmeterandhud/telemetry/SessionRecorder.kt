package com.growtotype.fpsmeterandhud.telemetry

import androidx.compose.runtime.Immutable
import com.growtotype.fpsmeterandhud.db.AppDatabase
import com.growtotype.fpsmeterandhud.model.GamingSession
import com.growtotype.fpsmeterandhud.model.HardwareSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
data class RecordingStatus(
    val isRecording: Boolean = false,
    val gameTag: String = "General",
    val elapsedSeconds: Long = 0L,
    val sampleCount: Int = 0,
    val currentAvgFps: Float = 0f
)

class SessionRecorder private constructor(
    private val database: AppDatabase,
    private val hardwareMonitor: HardwareMonitor
) {
    // Dedicated permanent scope so stopping the overlay service never cancels active recordings or DB saves
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null

    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private val samples = ArrayList<HardwareSample>(600)
    private var startTimestampMs: Long = 0L

    fun startRecording(gameTag: String = "General") {
        val cleanTag = gameTag.trim().ifEmpty { "General" }
        recordingJob?.cancel()

        val initialSample = hardwareMonitor.currentSample.value.copy(
            timestampMs = System.currentTimeMillis()
        )

        synchronized(samples) {
            samples.clear()
            samples.add(initialSample)
            startTimestampMs = initialSample.timestampMs
            _status.value = RecordingStatus(
                isRecording = true,
                gameTag = cleanTag,
                elapsedSeconds = 0L,
                sampleCount = 1,
                currentAvgFps = initialSample.fps
            )
        }

        // Dedicated 1-second ticker that records live hardware samples reliably
        recordingJob = recorderScope.launch {
            while (isActive && _status.value.isRecording) {
                delay(1000L)
                if (!_status.value.isRecording) break
                val latest = hardwareMonitor.currentSample.value.copy(
                    timestampMs = System.currentTimeMillis()
                )
                addSample(latest)
            }
        }
    }

    fun addSample(sample: HardwareSample) {
        if (!_status.value.isRecording) return

        synchronized(samples) {
            samples.add(sample)
            val elapsed = max(1L, (System.currentTimeMillis() - startTimestampMs) / 1000L)
            var sumFps = 0f
            for (i in 0 until samples.size) {
                sumFps += samples[i].fps
            }
            val avg = sumFps / samples.size

            _status.value = _status.value.copy(
                elapsedSeconds = elapsed,
                sampleCount = samples.size,
                currentAvgFps = (avg * 10f).roundToInt() / 10f
            )
        }
    }

    fun stopRecording(onSessionSaved: ((GamingSession) -> Unit)? = null) {
        if (!_status.value.isRecording) return

        recordingJob?.cancel()
        recordingJob = null

        val gameTag = _status.value.gameTag
        val endTimestampMs = System.currentTimeMillis()
        val duration = max(1L, (endTimestampMs - startTimestampMs) / 1000L)

        val recordedSamples = synchronized(samples) {
            if (samples.isEmpty()) {
                samples.add(hardwareMonitor.currentSample.value.copy(timestampMs = endTimestampMs))
            }
            // If only 1 sample was captured (very short recording), add a second point so timeline charts draw a clean line
            if (samples.size == 1) {
                samples.add(samples[0].copy(timestampMs = startTimestampMs + 1000L))
            }
            val list = ArrayList(samples)
            samples.clear()
            list
        }

        _status.value = RecordingStatus(isRecording = false)

        recorderScope.launch {
            val fpsList = recordedSamples.map { it.fps }.sorted()
            val avgFps = recordedSamples.map { it.fps }.average().toFloat()
            val minFps = fpsList.firstOrNull() ?: 60f
            val maxFps = fpsList.lastOrNull() ?: 60f

            val onePercentIndex = max(0, (fpsList.size * 0.01).toInt())
            val onePercentLow = fpsList.getOrNull(onePercentIndex) ?: minFps

            val pointOnePercentIndex = max(0, (fpsList.size * 0.001).toInt())
            val pointOnePercentLow = fpsList.getOrNull(pointOnePercentIndex) ?: minFps

            val stableFrames = recordedSamples.count { it.fps >= avgFps * 0.90f }
            val stability = (stableFrames.toFloat() / recordedSamples.size.toFloat()) * 100f

            val cpuList = recordedSamples.map { it.cpuUsage }
            val avgCpu = if (cpuList.isNotEmpty()) cpuList.average().toFloat() else 0f
            val peakCpu = cpuList.maxOrNull() ?: 0f

            val validGpu = recordedSamples.map { it.gpuUsage }.filter { it >= 0f }
            val avgGpu = if (validGpu.isNotEmpty()) validGpu.average().toFloat() else -1f

            val ramList = recordedSamples.map { it.ramUsedMb }
            val avgRam = if (ramList.isNotEmpty()) ramList.average().toInt() else 0
            val peakRam = ramList.maxOrNull() ?: 0
            val totalRam = recordedSamples.lastOrNull()?.ramTotalMb ?: 0

            val maxTemp = recordedSamples.map { it.cpuTempC }.maxOrNull() ?: 0f
            val modeUsed = recordedSamples.lastOrNull()?.modeUsed ?: "Standard"

            val jsonArray = JSONArray()
            recordedSamples.forEachIndexed { idx, s ->
                val obj = JSONObject()
                val relSec = max(idx.toLong(), (s.timestampMs - startTimestampMs) / 1000L)
                obj.put("t", relSec)
                obj.put("f", s.fps.toDouble())
                obj.put("c", s.cpuUsage.toDouble())
                obj.put("g", s.gpuUsage.toDouble())
                obj.put("r", s.ramUsedMb)
                obj.put("tm", s.cpuTempC.toDouble())
                jsonArray.put(obj)
            }

            val session = GamingSession(
                title = "$gameTag Session",
                gameTag = gameTag,
                startTime = startTimestampMs,
                endTime = endTimestampMs,
                durationSeconds = duration,
                avgFps = (avgFps * 10f).roundToInt() / 10f,
                minFps = (minFps * 10f).roundToInt() / 10f,
                maxFps = (maxFps * 10f).roundToInt() / 10f,
                onePercentLowFps = (onePercentLow * 10f).roundToInt() / 10f,
                pointOnePercentLowFps = (pointOnePercentLow * 10f).roundToInt() / 10f,
                stabilityScore = (stability * 10f).roundToInt() / 10f,
                avgCpuUsage = (avgCpu * 10f).roundToInt() / 10f,
                peakCpuUsage = (peakCpu * 10f).roundToInt() / 10f,
                avgGpuUsage = if (avgGpu >= 0f) (avgGpu * 10f).roundToInt() / 10f else -1f,
                avgRamUsedMb = avgRam,
                peakRamUsedMb = peakRam,
                totalRamMb = totalRam,
                maxTempC = (maxTemp * 10f).roundToInt() / 10f,
                privilegeMode = modeUsed,
                samplesJson = jsonArray.toString()
            )

            val id = database.sessionDao().insertSession(session)
            val savedSession = session.copy(id = id)
            withContext(Dispatchers.Main) {
                onSessionSaved?.invoke(savedSession)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionRecorder? = null

        fun getInstance(database: AppDatabase, hardwareMonitor: HardwareMonitor): SessionRecorder {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionRecorder(database, hardwareMonitor)
                INSTANCE = instance
                instance
            }
        }
    }
}
