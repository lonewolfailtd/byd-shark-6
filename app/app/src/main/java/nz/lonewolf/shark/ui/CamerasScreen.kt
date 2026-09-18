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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.camera.QCarCam

private const val PREVIEW_W = 960
private const val PREVIEW_H = 650

/**
 * Phase 3 groundwork: prove the cameras. Probe the AIS library, scan input ids,
 * open one stream and show it live. Recording comes once this works on the ute.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CamerasScreen() {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf(if (QCarCam.loaded) "Native bridge loaded. Tap Probe." else "Native bridge failed: ${QCarCam.loadError}") }
    var streaming by remember { mutableStateOf(false) }
    var cameraId by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    val bitmap = remember { Bitmap.createBitmap(PREVIEW_W, PREVIEW_H, Bitmap.Config.ARGB_8888) }
    var frameTick by remember { mutableIntStateOf(0) }

    fun io(block: () -> String) { scope.launch { report = withContext(Dispatchers.IO) { block() } } }

    LaunchedEffect(streaming) {
        if (!streaming) return@LaunchedEffect
        var frames = 0; var last = System.currentTimeMillis()
        while (isActive && streaming) {
            val px = withContext(Dispatchers.IO) { QCarCam.frame(PREVIEW_W, PREVIEW_H) }
            if (px != null) {
                bitmap.setPixels(px, 0, PREVIEW_W, 0, 0, PREVIEW_W, PREVIEW_H)
                frameTick++
                frames++
                val now = System.currentTimeMillis()
                if (now - last >= 1000) { fps = frames; frames = 0; last = now }
            } else delay(30)
        }
    }
    // Never leave a stream open when the page goes away; BYD's 360 view needs the cameras.
    DisposableEffect(Unit) { onDispose { if (streaming) { streaming = false; Thread { QCarCam.stop() }.start() } } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("Camera feed", Modifier.weight(2f)) {
                Box(Modifier.fillMaxWidth().aspectRatio(PREVIEW_W.toFloat() / PREVIEW_H).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.Black)) {
                    if (streaming) {
                        @Suppress("UNUSED_EXPRESSION") frameTick
                        Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else Text("No stream", color = Shark.muted, modifier = Modifier.padding(16.dp))
                }
                Text(if (streaming) "Camera $cameraId live, $fps fps" else "Stopped", color = Shark.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Panel("Controls", Modifier.weight(1f)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("Probe library", false, Modifier.width(150.dp)) { io { QCarCam.probe() } }
                    // Only the inputs the probe reported. Probing ids that do not exist restarted the head unit.
                    listOf(0, 1, 2, 3, 4, 5, 8, 9).forEach { id ->
                        Tile("Open $id", streaming && cameraId == id, Modifier.width(100.dp)) {
                            if (streaming) return@Tile
                            cameraId = id
                            scope.launch {
                                report = withContext(Dispatchers.IO) { QCarCam.open(id) }
                                streaming = report.startsWith("STREAM_STARTED")
                            }
                        }
                    }
                    Tile("Stop", false, Modifier.width(100.dp)) {
                        streaming = false
                        io { QCarCam.stop() }
                    }
                }
                Text("Park first. Stop the stream before opening BYD's own 360 view. 0 cabin, 4 5 8 9 exterior.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Text(report, color = Shark.text, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
