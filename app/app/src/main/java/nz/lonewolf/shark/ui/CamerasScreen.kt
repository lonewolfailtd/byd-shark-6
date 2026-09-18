package nz.lonewolf.shark.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.camera.QCarCam
import nz.lonewolf.shark.camera.QCarCam.Cam
import nz.lonewolf.shark.core.byd.Vehicle

private const val PW = 1280
private const val PH = 868

/**
 * Live view of the ute's cameras and the drive recorder.
 * Tap a camera to switch. Preview is paused while recording so the encoders get the CPU.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CamerasScreen() {
    val scope = rememberCoroutineScope()
    val rec by Vehicle.recorder.status.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf<Cam?>(null) }
    var report by remember { mutableStateOf("") }
    var fps by remember { mutableIntStateOf(0) }
    val bitmap = remember { Bitmap.createBitmap(PW, PH, Bitmap.Config.ARGB_8888) }
    var tick by remember { mutableIntStateOf(0) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefsView = ctx.getSharedPreferences("camera_view", 0)
    var flat by remember { mutableStateOf(prefsView.getBoolean("flat", true)) }
    var focal by remember { mutableStateOf(prefsView.getFloat("focal", 580f)) }
    var fov by remember { mutableStateOf(prefsView.getFloat("fov", 110f)) }

    fun show(cam: Cam) {
        scope.launch {
            report = withContext(Dispatchers.IO) {
                current?.let { if (it != cam && !rec.recording) QCarCam.stopOne(it.id) }
                QCarCam.open(cam.id)
            }
            current = cam
        }
    }

    LaunchedEffect(current, rec.recording, flat, focal, fov) {
        val cam = current ?: return@LaunchedEffect
        if (rec.recording) return@LaunchedEffect
        var n = 0; var last = System.currentTimeMillis()
        while (isActive) {
            val px = withContext(Dispatchers.IO) { QCarCam.frame(cam.id, PW, PH, flat && cam.exterior, focal, fov) }
            if (px != null) {
                bitmap.setPixels(px, 0, PW, 0, 0, PW, PH); tick++; n++
                val now = System.currentTimeMillis(); if (now - last >= 1000) { fps = n; n = 0; last = now }
            } else delay(40)
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (!Vehicle.recorder.status.value.recording) Thread { QCarCam.stopAll() }.start() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(current?.label ?: "Camera", Modifier.weight(2f)) {
                Box(Modifier.fillMaxWidth().aspectRatio(PW.toFloat() / PH).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    when {
                        rec.recording -> Text("Recording ${rec.cameras.joinToString { it.label }}\n${rec.clip ?: ""}\n" + rec.fps.entries.joinToString("  ") { "${Cam.byId(it.key)?.label} ${it.value} fps" }, color = Shark.accent, modifier = Modifier.padding(16.dp))
                        current != null -> { @Suppress("UNUSED_EXPRESSION") tick; Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                        else -> Text("Tap a camera", color = Shark.muted, modifier = Modifier.padding(16.dp))
                    }
                }
                Text(if (current != null && !rec.recording) "$fps fps" else "", color = Shark.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Panel("Cameras", Modifier.weight(1f)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cam.entries.forEach { cam -> Tile(cam.label, current == cam, Modifier.width(130.dp)) { show(cam) } }
                    Tile("Stop view", false, Modifier.width(130.dp)) { current = null; scope.launch { withContext(Dispatchers.IO) { if (!rec.recording) QCarCam.stopAll() } } }
                    Tile("BYD 360 view", false, Modifier.width(130.dp)) {
                        current = null
                        scope.launch {
                            withContext(Dispatchers.IO) { if (!rec.recording) QCarCam.stopAll() }
                            runCatching { ctx.startActivity(android.content.Intent().setClassName("com.byd.avm", "com.byd.avm.MainActivity").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile(if (flat) "Flat" else "Original", flat, Modifier.width(110.dp), sub = "lens") { flat = !flat; prefsView.edit().putBoolean("flat", flat).apply() }
                    Tile("Less curve", false, Modifier.width(110.dp)) { focal = (focal + 20f).coerceAtMost(900f); prefsView.edit().putFloat("focal", focal).apply() }
                    Tile("More curve", false, Modifier.width(110.dp)) { focal = (focal - 20f).coerceAtLeast(300f); prefsView.edit().putFloat("focal", focal).apply() }
                    Tile("Wider", false, Modifier.width(100.dp)) { fov = (fov + 10f).coerceAtMost(150f); prefsView.edit().putFloat("fov", fov).apply() }
                    Tile("Narrower", false, Modifier.width(100.dp)) { fov = (fov - 10f).coerceAtLeast(60f); prefsView.edit().putFloat("fov", fov).apply() }
                }
                Text("Lens ${focal.toInt()}  view ${fov.toInt()} degrees. Tune until fence lines and the horizon are straight.", color = Shark.muted, fontSize = 12.sp)
                Text("Park first. Stop the view before opening BYD's own 360 view.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Panel("Drive recorder and sentry") {
            val prefs = Vehicle.prefs
            var autoRec by remember { mutableStateOf(prefs.autoRecord) }
            var autoSentry by remember { mutableStateOf(prefs.autoSentry) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!rec.recording) {
                    Tile("Record now", false, Modifier.width(150.dp)) { current = null; scope.launch { withContext(Dispatchers.IO) { QCarCam.stopAll(); Vehicle.recorder.start(Cam.entries.filter { it.exterior }) } } }
                    Tile("Arm sentry", false, Modifier.width(150.dp), sub = "parked, ute on") { current = null; scope.launch { withContext(Dispatchers.IO) { QCarCam.stopAll(); Vehicle.recorder.start(Cam.entries.filter { it.exterior }, nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) } } }
                } else {
                    Tile(if (rec.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) "Disarm sentry" else "Stop recording", true, Modifier.width(160.dp)) { scope.launch { withContext(Dispatchers.IO) { Vehicle.recorder.stop() } } }
                    Tile("Save event", false, Modifier.width(150.dp), sub = "keeps last clips") { Vehicle.recorder.markEvent() }
                }
                Tile("Auto record when driving", autoRec, Modifier.width(230.dp)) { autoRec = !autoRec; prefs.autoRecord = autoRec }
                Tile("Auto sentry when parked", autoSentry, Modifier.width(230.dp), sub = "while the ute stays on") { autoSentry = !autoSentry; prefs.autoSentry = autoSentry }
            }
            val modeText = if (rec.recording) (if (rec.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) "Sentry armed" else "Recording") + " on ${rec.cameras.joinToString { it.label }}" else "Idle"
            Text("$modeText. Saves to ${rec.folder ?: Vehicle.recorder.storageRoot().path}. Events kept: ${rec.eventsKept}.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            rec.lastAlert?.let { Text(it, color = Shark.accent, fontSize = 16.sp) }
            rec.error?.let { Text(it, color = Shark.bad, fontSize = 12.sp) }
            val clips = remember(rec.clip, rec.recording) { Vehicle.recorder.clips().take(4) }
            clips.forEach { f -> Text("${f.name}  ${f.length() / 1_000_000} MB", color = Shark.text, fontSize = 12.sp) }
        }
        if (report.isNotBlank()) Text(report, color = Shark.muted, fontSize = 11.sp)
    }
}
