package nz.lonewolf.shark.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nz.lonewolf.shark.R
import nz.lonewolf.shark.core.byd.ClimateBridge
import nz.lonewolf.shark.core.byd.SeatBridge
import nz.lonewolf.shark.core.byd.TelemetryBridge
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.ui.MainActivity

/**
 * Foreground service that keeps the vehicle link alive while the ute is on and polls
 * telemetry once a second. Sideloaded apps cannot autostart after deep sleep on DiLink,
 * so this starts when the app is opened and runs until the head unit sleeps.
 */
class VehicleService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Vehicle.init(this)
        startForeground(NOTIFICATION_ID, notification())
        running.value = true
        scope.launch { poll() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running.value = false
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun poll() {
        var tick = 0
        while (true) {
            runCatching {
                telemetry.value = Vehicle.telemetry.read()
                climate.value = Vehicle.climate.read()
                seats.value = Vehicle.seats.read()
                lights.value = Vehicle.lights.read()
                slope.value = Vehicle.lights.slope()
            }.onFailure { android.util.Log.e("SharkProbe", "poll failed", it) }
            // Every 10 s write a probe report so it can be read over adb without the screen.
            if (tick++ % 10 == 0) runCatching {
                val report = buildString {
                    appendLine("time=${System.currentTimeMillis()} sdk=${nz.lonewolf.shark.core.byd.BydSdkLoader.mode} err=${nz.lonewolf.shark.core.byd.BydSdkLoader.lastError}")
                    appendLine("telemetry=${telemetry.value}")
                    appendLine("climate=${climate.value}")
                    appendLine("seats=${seats.value}")
                    appendLine("lights=${lights.value}")
                    appendLine("slope=${slope.value}")
                    Vehicle.telemetry.devices.forEach { (n, d) -> appendLine("device $n bound=${d.bound} err=${d.bindError}") }
                }
                android.util.Log.e("SharkProbe", report)
                java.io.File(getExternalFilesDir(null), "probe.txt").writeText(report)
                val methodsFile = java.io.File(getExternalFilesDir(null), "methods.txt")
                if (!methodsFile.exists()) methodsFile.writeText(buildString {
                    val all = linkedMapOf(
                        "ac" to Vehicle.climate.device, "setting" to Vehicle.seats.device,
                        "light" to Vehicle.lights.device, "sensor" to Vehicle.lights.sensor,
                    ) + Vehicle.telemetry.devices
                    all.forEach { (n, d) -> appendLine("== $n ${d.className}"); d.methodDump().forEach { appendLine(it) } }
                })
            }
            delay(1_000)
        }
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.service_channel), NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "vehicle"
        private const val NOTIFICATION_ID = 1

        val running = MutableStateFlow(false)
        val telemetry = MutableStateFlow<TelemetryBridge.Snapshot?>(null)
        val climate = MutableStateFlow<ClimateBridge.State?>(null)
        val seats = MutableStateFlow<SeatBridge.State?>(null)
        val lights = MutableStateFlow<nz.lonewolf.shark.core.byd.LightBridge.State?>(null)
        val slope = MutableStateFlow<Int?>(null)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VehicleService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VehicleService::class.java))
        }
    }
}
