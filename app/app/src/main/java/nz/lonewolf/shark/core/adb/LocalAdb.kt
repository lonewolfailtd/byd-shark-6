package nz.lonewolf.shark.core.adb

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Talks to the head unit's own adbd on port 5555 so the app can grant itself what a
 * sideloaded app cannot ask for through the normal permission dialogs: the BYD vehicle
 * permissions, secure settings, the overlay window and the hidden API policy the SDK
 * loader needs. The first connection shows BYD's "allow USB debugging" prompt once.
 * The same commands are in tools/install.ps1 for a laptop side install.
 */
object LocalAdb {
    private const val TAG = "LocalAdb"
    private const val PORT = 5555
    private const val PREFS = "local_adb"
    private const val PREF_DONE = "grants_done_v1"
    private val lock = Any()
    @Volatile private var lastHost = "127.0.0.1"

    sealed class Status {
        object Idle : Status(); object Connecting : Status(); object WaitingForApproval : Status()
        object Granting : Status(); object Done : Status(); data class Failed(val reason: String) : Status()
    }

    data class ShellResult(val exit: Int, val out: String)

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status

    val bydPermissions = listOf(
        "AC_COMMON", "AC_GET", "AC_SET", "SETTING_COMMON", "SETTING_GET", "SETTING_SET",
        "LIGHT_COMMON", "LIGHT_GET", "LIGHT_SET", "BODYWORK_COMMON", "BODYWORK_GET",
        "SENSOR_COMMON", "SENSOR_GET", "TYRE_COMMON", "TYRE_GET", "STATISTIC_COMMON", "STATISTIC_GET",
        "SPEED_COMMON", "SPEED_GET", "GEARBOX_COMMON", "GEARBOX_GET", "ENGINE_COMMON", "ENGINE_GET",
        "CHARGING_COMMON", "CHARGING_GET", "ENERGY_COMMON", "ENERGY_GET", "INSTRUMENT_COMMON", "INSTRUMENT_GET",
        "VEHICLEHEALTH_COMMON", "VEHICLEHEALTH_GET", "OTA_COMMON", "OTA_GET", "COLLECTDATA_COMMON", "COLLECTDATA_GET",
    ).map { "android.permission.BYDAUTO_$it" }

    private val corePermissions = listOf(
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.READ_LOGS",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.ACCESS_FINE_LOCATION",
    )

    /** Everything the installer runs, in order. `$pkg` is replaced with our package name. */
    fun grantCommands(pkg: String): List<String> = buildList {
        corePermissions.forEach { add("pm grant $pkg $it") }
        bydPermissions.forEach { add("pm grant $pkg $it") }
        add("settings put global hidden_api_policy 1")
        add("settings put global hidden_api_blacklist_exemptions 'Lcom/ts/,Ldalvik/system/'")
        add("appops set $pkg SYSTEM_ALERT_WINDOW allow")
        add("appops set $pkg START_ACTIVITIES_FROM_BACKGROUND allow")
        add("appops set $pkg RUN_IN_BACKGROUND allow")
        add("appops set $pkg RUN_ANY_IN_BACKGROUND allow")
        add("appops set $pkg WAKE_LOCK allow")
        add("dumpsys deviceidle whitelist +$pkg")
        add("settings put global byd_float_app_list \"\$(settings get global byd_float_app_list),$pkg\"")
    }

    fun grantsDone(context: Context): Boolean {
        if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_DONE, false)) return true
        val pm = context.packageManager
        return corePermissions.take(2).all { pm.checkPermission(it, context.packageName) == PackageManager.PERMISSION_GRANTED }
    }

    /** Connect to local adbd, wait for the user to tap Allow on the ute, run every grant. */
    suspend fun setup(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (grantsDone(context)) { _status.value = Status.Done; return@withContext true }
        _status.value = Status.Connecting
        if (!portOpen()) {
            _status.value = Status.Failed("ADB is not enabled on the ute. Car > System > Version, tap Factory Reset ten times, rotate the screen, then CONNECT USB TO ENABLE DEBUGGING MODE.")
            return@withContext false
        }
        val keys = keyPair(context)
        var dadb = connect(keys, 3_000)
        if (dadb == null) {
            _status.value = Status.WaitingForApproval
            repeat(60) {
                delay(3_000)
                dadb = connect(keys, 2_000)
                if (dadb != null) return@repeat
            }
        }
        val d = dadb ?: run {
            _status.value = Status.Failed("Timed out waiting for the USB debugging approval on the ute.")
            return@withContext false
        }
        try {
            _status.value = Status.Granting
            var allOk = true
            for (cmd in grantCommands(context.packageName)) {
                val r = runCatching { d.shell(cmd) }.getOrNull()
                val ok = r != null && (r.exitCode == 0 || r.allOutput.contains("Success", true))
                if (!ok && cmd.startsWith("pm grant") && corePermissions.any { cmd.endsWith(it) }) allOk = false
                Log.d(TAG, "$cmd -> ${r?.exitCode} ${r?.allOutput?.trim()?.take(80)}")
            }
            if (allOk) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF_DONE, true).apply()
            _status.value = if (allOk) Status.Done else Status.Failed("Some core grants failed; see logcat")
            allOk
        } finally { runCatching { d.close() } }
    }

    fun shell(context: Context, command: String): ShellResult = synchronized(lock) {
        if (!portOpen()) return ShellResult(-1, "local ADB port not open")
        val d = connect(keyPair(context), 3_000) ?: return ShellResult(-1, "ADB not authorised")
        try {
            val r = d.shell(command.trim())
            ShellResult(r.exitCode, r.allOutput.trim())
        } catch (e: Exception) { ShellResult(-1, "failed: ${e.message}") } finally { runCatching { d.close() } }
    }

    fun portOpen(): Boolean = (listOf(lastHost) + hosts()).distinct().any { h ->
        runCatching { Socket().use { it.connect(InetSocketAddress(h, PORT), 400); true } }.getOrDefault(false).also { if (it) lastHost = h }
    }

    private fun connect(keys: AdbKeyPair, timeoutMs: Long): Dadb? {
        for (h in hosts()) {
            var result: Dadb? = null
            val t = Thread {
                runCatching {
                    val d = Dadb.create(h, PORT, keys)
                    if (d.shell("echo ok").exitCode == 0) result = d else d.close()
                }
            }
            t.start(); t.join(timeoutMs)
            if (t.isAlive) { t.interrupt(); runCatching { result?.close() }; continue }
            result?.let { lastHost = h; return it }
        }
        return null
    }

    private fun hosts(): List<String> {
        val out = linkedSetOf("127.0.0.1", "::1")
        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()?.filter { it.isUp && !it.isLoopback }?.forEach { i ->
                i.inetAddresses.toList().filter { !it.isLoopbackAddress && it is java.net.Inet4Address }.forEach { out += it.hostAddress!! }
            }
        }
        return out.toList()
    }

    private fun keyPair(context: Context): AdbKeyPair {
        val priv = File(context.filesDir, "adbkey"); val pub = File(context.filesDir, "adbkey.pub")
        if (!(priv.exists() && pub.exists())) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }
}
