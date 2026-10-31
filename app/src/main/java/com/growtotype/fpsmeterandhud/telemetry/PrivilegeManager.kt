package com.growtotype.fpsmeterandhud.telemetry

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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

class PrivilegeManager(private val context: Context) {

    suspend fun checkPrivileges(): PrivilegeStatus = withContext(Dispatchers.IO) {
        val rootFound = checkRootBinary()
        var rootGranted = false
        if (rootFound) {
            rootGranted = testRootExecution()
        }

        val shizukuInstalled = isPackageInstalled("moe.shizuku.privilege.api")
        val shizukuRunning = if (shizukuInstalled) checkShizukuService() else false

        val active = when {
            rootGranted -> "Root"
            shizukuRunning -> "Shizuku"
            else -> "Standard"
        }

        PrivilegeStatus(
            isRootAvailable = rootFound,
            isRootGranted = rootGranted,
            isShizukuInstalled = shizukuInstalled,
            isShizukuRunning = shizukuRunning,
            activeMode = active
        )
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
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun testRootExecution(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            line != null && line.contains("uid=0")
        } catch (_: Exception) {
            false
        }
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

    suspend fun executeCommand(command: String, preferredMode: String = "AUTO"): ShellResult = withContext(Dispatchers.IO) {
        val tryRoot = (preferredMode == "AUTO" || preferredMode == "ROOT")
        if (tryRoot && checkRootBinary()) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                process.waitFor()
                if (process.exitValue() == 0 && output.isNotEmpty()) {
                    return@withContext ShellResult(true, output, "Root")
                }
            } catch (_: Exception) {}
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
}
