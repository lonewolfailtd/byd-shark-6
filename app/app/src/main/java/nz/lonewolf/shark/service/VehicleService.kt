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
        var startApplied = false
        var reverseClearTicks = 0
        var parkedTicks = 0
        while (true) {
            runCatching {
                telemetry.value = Vehicle.telemetry.read()
                climate.value = Vehicle.climate.read()
                seats.value = Vehicle.seats.read()
                lights.value = Vehicle.lights.read()
                slope.value = Vehicle.lights.slope()
                seatPosition.value = Vehicle.seatPosition.read()
            }.onFailure { android.util.Log.e("SharkProbe", "poll failed", it) }
            // Camera rules: reverse hands the cameras to BYD; auto record on drive; sentry when parked.
            val gear = telemetry.value?.gear
            val rec = Vehicle.recorder
            val recording = rec.status.value.recording
            val driving = gear != null && gear != 1 && gear != 3      // anything but Park or Neutral
            if (gear == 2 && recording) rec.pause("reverse selected")
            else if (gear != null && gear != 2 && rec.isPaused) { reverseClearTicks++; if (reverseClearTicks >= 3) { reverseClearTicks = 0; rec.resume() } }
            else reverseClearTicks = 0
            if (driving) parkedTicks = 0 else if (gear == 1) parkedTicks++
            val prefs = Vehicle.prefs
            if (prefs.autoRecord && driving && !recording && !rec.isPaused) rec.start(nz.lonewolf.shark.camera.QCarCam.Cam.entries.filter { it.exterior }, nz.lonewolf.shark.camera.Recorder.Mode.DRIVE)
            if (recording && rec.status.value.mode == nz.lonewolf.shark.camera.Recorder.Mode.DRIVE && gear == 1 && parkedTicks >= prefs.parkStopSeconds) rec.stop()
            if (prefs.autoSentry && gear == 1 && parkedTicks >= prefs.parkStopSeconds + 5 && !recording && !rec.isPaused) rec.start(nz.lonewolf.shark.camera.QCarCam.Cam.entries.filter { it.exterior }, nz.lonewolf.shark.camera.Recorder.Mode.SENTRY)
            if (recording && rec.status.value.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY && driving) rec.stop()
            if (!startApplied && tick >= 4 && climate.value?.bound == true) {
                startApplied = true
                Vehicle.profiles.startProfile()?.let { p -> runCatching { android.util.Log.w("SharkProbe", "start profile ${p.name}: ${Vehicle.profiles.apply(p)}") } }
            }
            // Every 10 s write a probe report so it can be read over adb without the screen.
            if (tick++ % 10 == 0) runCatching {
                val report = buildString {
                    appendLine("time=${System.currentTimeMillis()} sdk=${nz.lonewolf.shark.core.byd.BydSdkLoader.mode} err=${nz.lonewolf.shark.core.byd.BydSdkLoader.lastError}")
                    appendLine("telemetry=${telemetry.value}")
                    appendLine("climate=${climate.value}")
                    appendLine("seats=${seats.value}")
                    appendLine("lights=${lights.value}")
                    appendLine("slope=${slope.value}")
                    appendLine("seatPosition=${seatPosition.value}")
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
        val seatPosition = MutableStateFlow<nz.lonewolf.shark.core.byd.SeatPositionBridge.Positions?>(null)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VehicleService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VehicleService::class.java))
        }
    }
}
