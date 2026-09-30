package nz.lonewolf.shark.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxHeight
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
private const val TW = 320
private const val TH = 216

/**
 * Live view of the ute's cameras and the drive recorder.
 * Tap a camera to switch. Preview is paused while recording so the encoders get the CPU.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CamerasScreen() {
    var section by remember { mutableStateOf("Live") }
    val quad = section == "Live"
    val browsing = section == "Recordings"
    val thumbs = remember { Cam.entries.filter { it.exterior }.associateWith { Bitmap.createBitmap(TW, TH, Bitmap.Config.ARGB_8888) } }
    var thumbTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val rec by Vehicle.recorder.status.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf<Cam?>(null) }
    var lensOpen by remember { mutableStateOf(false) }
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
                current?.let { if (it != cam && !rec.recording && !(quad && it.exterior)) QCarCam.stopOne(it.id) }
                QCarCam.open(cam.id)
            }
            current = cam
        }
    }

    LaunchedEffect(current, rec.recording, flat, focal, fov, quad) {
        if (rec.recording) return@LaunchedEffect
        val cam = current
        val small = if (quad) thumbs.keys.toList() else emptyList()
        if (cam == null && small.isEmpty()) return@LaunchedEffect
        if (small.isNotEmpty()) withContext(Dispatchers.IO) { small.forEach { QCarCam.open(it.id) } }
        var n = 0; var last = System.currentTimeMillis(); var turn = 0
        while (isActive) {
            var got = false
            if (cam != null) {
                val px = withContext(Dispatchers.IO) { QCarCam.frame(cam.id, PW, PH, flat && cam.exterior, focal, fov) }
                if (px != null) {
                    bitmap.setPixels(px, 0, PW, 0, 0, PW, PH); tick++; n++; got = true
                    val now = System.currentTimeMillis(); if (now - last >= 1000) { fps = n; n = 0; last = now }
                }
            }
            // One small picture every few big ones, all on this one loop so the cameras are never read from two places at once.
            if (small.isNotEmpty() && (cam == null || turn % 4 == 0)) {
                val c = small[(turn / (if (cam == null) 1 else 4)) % small.size]
                val px = withContext(Dispatchers.IO) { QCarCam.frame(c.id, TW, TH, flat, focal, fov) }
                if (px != null) { thumbs[c]?.setPixels(px, 0, TW, 0, 0, TW, TH); thumbTick++; got = true }
            }
            turn++
            if (!got) delay(40) else if (cam == null) delay(120)
        }
    }
    // Open on the front camera, unless the recorder already owns the cameras.
    LaunchedEffect(Unit) { if (current == null && !Vehicle.recorder.status.value.recording) show(Cam.FRONT) }
    DisposableEffect(Unit) {
        onDispose { if (!Vehicle.recorder.status.value.recording) Thread { QCarCam.stopAll() }.start() }
    }

    Backdrop("bg_cameras", coloured = false, wash = 0.55f) {
    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left: what you are looking at
        Column(Modifier.width(190.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Live" to "all four cameras", "Recordings" to "watch, save, share", "Settings" to "lens and auto record").forEach { (name, sub) ->
                Tile(name, section == name, Modifier.fillMaxWidth(), height = 72.dp, sub = sub) {
                    if (name != "Live" && !rec.recording) scope.launch { withContext(Dispatchers.IO) { QCarCam.stopAll() } }
                    if (name == "Live") current?.let { show(it) } ?: show(Cam.FRONT)
                    section = name
                }
            }
            Spacer(Modifier.weight(1f))
            Tile("BYD 360 view", false, Modifier.fillMaxWidth(), height = 56.dp, sub = "BYD's own screen") {
                current = null; section = "Settings"
                scope.launch {
                    withContext(Dispatchers.IO) { if (!rec.recording) QCarCam.stopAll() }
                    runCatching { ctx.startActivity(android.content.Intent().setClassName("com.byd.avm", "com.byd.avm.MainActivity").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
        }
        // Middle: the picture and the four cameras under it
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (section) {
                "Recordings" -> RecordingsScreen { section = "Live"; show(Cam.FRONT) }
                "Settings" -> {
                    Panel("Lens") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile(if (flat) "Flat" else "Original", flat, Modifier.width(140.dp), sub = "fisheye removed") { flat = !flat; prefsView.edit().putBoolean("flat", flat).apply() }
                            Tile("Less curve", false, Modifier.width(130.dp)) { focal = (focal + 20f).coerceAtMost(900f); prefsView.edit().putFloat("focal", focal).apply() }
                            Tile("More curve", false, Modifier.width(130.dp)) { focal = (focal - 20f).coerceAtLeast(300f); prefsView.edit().putFloat("focal", focal).apply() }
                            Tile("Wider", false, Modifier.width(110.dp)) { fov = (fov + 10f).coerceAtMost(150f); prefsView.edit().putFloat("fov", fov).apply() }
                            Tile("Narrower", false, Modifier.width(120.dp)) { fov = (fov - 10f).coerceAtLeast(60f); prefsView.edit().putFloat("fov", fov).apply() }
                        }
                        Text("Lens ${focal.toInt()}  view ${fov.toInt()} degrees. Tune until fence lines and the horizon are straight.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Panel("Automatic") {
                        val prefs = Vehicle.prefs
                        var autoRec by remember { mutableStateOf(prefs.autoRecord) }
                        var autoSentry by remember { mutableStateOf(prefs.autoSentry) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile("Record when driving", autoRec, Modifier.weight(1f), sub = "starts when you leave Park") { autoRec = !autoRec; prefs.autoRecord = autoRec }
                            Tile("Sentry when parked", autoSentry, Modifier.weight(1f), sub = "while the ute stays on") { autoSentry = !autoSentry; prefs.autoSentry = autoSentry }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black).border(1.2.dp, Shark.edge, RoundedCornerShape(14.dp))) {
                        when {
                            rec.recording -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (rec.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) "Sentry is watching" else "Recording", color = Shark.accent, fontSize = 26.sp)
                                Text(rec.cameras.joinToString { it.label }, color = Shark.text, fontSize = 15.sp)
                                Text("The live picture pauses while recording so every camera gets full quality.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                            current != null -> { @Suppress("UNUSED_EXPRESSION") tick; Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            else -> Text("Starting camera", color = Shark.muted, modifier = Modifier.align(Alignment.Center))
                        }
                        if (current != null && !rec.recording) Text("${current?.label} camera  |  ${if (flat) "Flat" else "Original"}  |  $fps fps", color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(112.dp)) {
                        thumbs.forEach { (cam, bmp) ->
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(Color.Black)
                                .border(if (current == cam) 2.dp else 1.dp, if (current == cam) Shark.accent else Shark.panelLine, RoundedCornerShape(10.dp)).clickable { show(cam) }) {
                                @Suppress("UNUSED_EXPRESSION") thumbTick
                                if (!rec.recording) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Text(cam.label, color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
                            }
                        }
                    }
                }
            }
        }
        // Right: what you do with it
        Column(Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!rec.recording) {
                Tile("Record now", false, Modifier.fillMaxWidth(), height = 84.dp, sub = "all four cameras") { current = null; scope.launch { withContext(Dispatchers.IO) { QCarCam.stopAll(); Vehicle.recorder.start(Cam.entries.filter { it.exterior }) } } }
                Tile("Arm sentry", false, Modifier.fillMaxWidth(), height = 64.dp, sub = "parked, ute on") { current = null; scope.launch { withContext(Dispatchers.IO) { QCarCam.stopAll(); Vehicle.recorder.start(Cam.entries.filter { it.exterior }, nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) } } }
            } else {
                Tile(if (rec.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) "Disarm sentry" else "Stop recording", true, Modifier.fillMaxWidth(), height = 84.dp, sub = rec.clip) {
                    scope.launch { withContext(Dispatchers.IO) { Vehicle.recorder.stop() }; show(Cam.FRONT) }
                }
                Tile("Save clip", false, Modifier.fillMaxWidth(), height = 64.dp, sub = "keeps the last few minutes") { Vehicle.recorder.markEvent() }
            }
            var use by remember { mutableStateOf<nz.lonewolf.shark.camera.Recorder.Usage?>(null) }
            LaunchedEffect(rec.clip, rec.eventsKept) { use = withContext(Dispatchers.IO) { runCatching { Vehicle.recorder.usage() }.getOrNull() } }
            Glass(Modifier.fillMaxWidth()) {
                Text("STORAGE", color = Shark.accent, fontSize = 12.sp, letterSpacing = 1.6.sp)
                val cap = Vehicle.prefs.storageCapGb
                Text(use?.let { "%.1f GB of %d GB".format(it.clipBytes / 1_073_741_824.0, cap) } ?: "--", color = Color.White, fontSize = 20.sp)
                Bar(use?.let { (it.clipBytes / 1_073_741_824.0 / cap).toFloat() }, Shark.accent)
                Text(use?.let { if (it.folder.contains("emulated")) "Inside the ute. Plug in a USB stick." else "USB stick" } ?: "", color = Shark.muted, fontSize = 12.sp)
            }
            Glass(Modifier.fillMaxWidth()) {
                Text("LOOP", color = Shark.accent, fontSize = 12.sp, letterSpacing = 1.6.sp)
                Text("3 minute clips, oldest removed first. Saved clips are never removed.", color = Shark.text, fontSize = 13.sp)
            }
            rec.lastAlert?.let { Text(it, color = Shark.accent, fontSize = 14.sp) }
            rec.error?.let { Text(it, color = Shark.bad, fontSize = 12.sp) }
        }
    }
    }
}
