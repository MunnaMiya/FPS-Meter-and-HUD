package com.growtotype.fpsmeterandhud.telemetry

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Immutable
data class PrivilegeStatus(
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val isShizukuInstalled: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val activeMode: String = "Standard" // "Root", "Shizuku", "Standard"
)

data class ShellResult(
    val isSuccess: Boolean,
    val output: String,
    val mode: String
)

class PrivilegeManager private constructor(private val context: Context) {

    private val shellMutex = Mutex()

    // Persistent interactive root shell process so 'su' is spawned at most ONCE per app session
    private var rootProcess: Process? = null
    private var rootWriter: BufferedWriter? = null
    private var rootReader: BufferedReader? = null

    @Volatile private var hasCheckedOnce = false
    @Volatile private var rootDeniedOrFailed = false
    @Volatile private var cachedStatus = PrivilegeStatus()

    suspend fun checkPrivileges(forceRecheck: Boolean = false): PrivilegeStatus = withContext(Dispatchers.IO) {
        if (hasCheckedOnce && !forceRecheck) {
            return@withContext cachedStatus
        }

        shellMutex.withLock {
            if (hasCheckedOnce && !forceRecheck) {
                return@withLock cachedStatus
            }

            if (forceRecheck) {
                rootDeniedOrFailed = false
            }

            val rootFound = checkRootBinary()
            var rootGranted = false
            if (rootFound && !rootDeniedOrFailed) {
                rootGranted = ensurePersistentRootSessionLocked()
                if (!rootGranted) {
                    rootDeniedOrFailed = true
                }
            }

            val shizukuInstalled = isPackageInstalled("moe.shizuku.privilege.api")
            val shizukuRunning = if (shizukuInstalled) checkShizukuService() else false

            val active = when {
                rootGranted -> "Root"
                shizukuRunning -> "Shizuku"
                else -> "Standard"
            }

            cachedStatus = PrivilegeStatus(
                isRootAvailable = rootFound,
                isRootGranted = rootGranted,
                isShizukuInstalled = shizukuInstalled,
                isShizukuRunning = shizukuRunning,
                activeMode = active
            )
            hasCheckedOnce = true
            cachedStatus
        }
    }

    private fun checkRootBinary(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/debug_ramdisk/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Opens a single persistent interactive `su` shell session if not already open,
     * and verifies `uid=0` through the existing stdin/stdout streams.
     * Because `su` is kept alive, Magisk/KernelSU/SuperSU only shows the grant toast ONCE.
     */
    private suspend fun ensurePersistentRootSessionLocked(): Boolean {
        try {
            val existing = rootProcess
            if (existing != null && isProcessAlive(existing) && rootWriter != null && rootReader != null) {
                val idOutput = runInPersistentRootLocked("id")
                if (idOutput != null && idOutput.contains("uid=0")) {
                    return true
                }
                closeRootSessionLocked()
            }

            val process = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            rootProcess = process
            rootWriter = writer
            rootReader = reader

            val idOutput = runInPersistentRootLocked("id")
            if (idOutput != null && idOutput.contains("uid=0")) {
                return true
            } else {
                closeRootSessionLocked()
                return false
            }
        } catch (_: Exception) {
            closeRootSessionLocked()
            return false
        }
    }

    private suspend fun runInPersistentRootLocked(command: String): String? {
        val writer = rootWriter ?: return null
        val reader = rootReader ?: return null
        val process = rootProcess ?: return null
        if (!isProcessAlive(process)) return null

        return withTimeoutOrNull(2500L) {
            try {
                val sentinel = "__FPS_HUD_CMD_DONE__"
                writer.write("$command\necho $sentinel\n")
                writer.flush()

                val sb = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.contains(sentinel)) {
                        break
                    }
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(line)
                }
                sb.toString().trim()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun isProcessAlive(process: Process): Boolean {
        return try {
            process.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    private fun closeRootSessionLocked() {
        try { rootWriter?.close() } catch (_: Exception) {}
        try { rootReader?.close() } catch (_: Exception) {}
        try { rootProcess?.destroy() } catch (_: Exception) {}
        rootWriter = null
        rootReader = null
        rootProcess = null
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun checkShizukuService(): Boolean {
        return try {
            val authority = "moe.shizuku.privilege.api.provider"
            val providerInfo = context.packageManager.resolveContentProvider(authority, 0)
                ?: return false
            providerInfo.enabled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Executes a shell command reusing the already-open persistent root session if active.
     * NEVER spawns a new `su` process on background polling ticks.
     */
    suspend fun executeCommand(command: String, preferredMode: String = "AUTO"): ShellResult = withContext(Dispatchers.IO) {
        val allowRoot = (preferredMode == "AUTO" || preferredMode == "ROOT") && cachedStatus.isRootGranted && !rootDeniedOrFailed
        if (allowRoot) {
            val rootOutput = shellMutex.withLock {
                // Only use the already-open persistent session; never spawn a new su process here
                runInPersistentRootLocked(command)
            }
            if (!rootOutput.isNullOrEmpty()) {
                return@withContext ShellResult(true, rootOutput, "Root")
            }
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            if (output.isNotEmpty()) {
                return@withContext ShellResult(true, output, "Standard")
            }
        } catch (_: Exception) {}

        ShellResult(false, "", "Standard")
    }

    companion object {
        @Volatile
        private var INSTANCE: PrivilegeManager? = null

        fun getInstance(context: Context): PrivilegeManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PrivilegeManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
