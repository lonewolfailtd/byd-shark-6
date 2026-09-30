package nz.lonewolf.shark.ui

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.camera.Recorder
import nz.lonewolf.shark.core.byd.Vehicle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shown = SimpleDateFormat("EEE d MMM  HH:mm:ss", Locale.US)

private fun gb(bytes: Long) = "%.1f GB".format(bytes / 1_073_741_824.0)

/** Browse, play, protect and delete what the recorder has saved. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val rec by Vehicle.recorder.status.collectAsStateWithLifecycle()
    var all by remember { mutableStateOf<List<Recorder.Saved>>(emptyList()) }
    var usage by remember { mutableStateOf<Recorder.Usage?>(null) }
    var filter by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<Recorder.Saved?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var capGb by remember { mutableIntStateOf(Vehicle.prefs.storageCapGb) }
    var stamp by remember { mutableStateOf(Vehicle.prefs.clipStamp) }
    var reload by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var sharing by remember { mutableStateOf(Vehicle.clipServer.running) }

    LaunchedEffect(reload, rec.clip, rec.eventsKept) {
        val (list, use) = withContext(Dispatchers.IO) { Vehicle.recorder.saved() to Vehicle.recorder.usage() }
        all = list; usage = use
        selected = selected?.let { s -> list.firstOrNull { it.name == s.name } }
    }

    val cameras = remember(all) { all.map { it.camera }.distinct().sorted() }
    val list = remember(all, filter) {
        when (filter) { "All" -> all; "Protected" -> all.filter { it.protectedClip }; else -> all.filter { it.camera == filter } }
    }
    fun step(by: Int) {
        val i = list.indexOfFirst { it.name == selected?.name }
        if (i < 0) return
        list.getOrNull(i + by)?.let { selected = it; confirmDelete = false }
    }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("Recordings", Modifier.weight(1f).fillMaxHeight()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Tile("Back", false, Modifier.width(90.dp), height = 48.dp) { onBack() }
                (listOf("All", "Protected") + cameras).forEach { f ->
                    Tile(f, filter == f, Modifier.width(if (f.length > 9) 150.dp else 110.dp), height = 48.dp) { filter = f }
                }
            }
            Text("${list.size} clips", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.name }) { c ->
                    val sel = c.name == selected?.name
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Shark.accentDim else Color(0xFF1B2638))
                            .border(1.dp, if (sel) Shark.accent else Shark.panelLine, RoundedCornerShape(10.dp))
                            .clickable { selected = c; confirmDelete = false; note = "" }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.camera, color = Shark.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(shown.format(Date(c.startedAt)), color = Shark.muted, fontSize = 13.sp)
                        }
                        if (c.protectedClip) Text("Protected", color = Shark.accent, fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp))
                        Text("${c.bytes / 1_000_000} MB", color = Shark.muted, fontSize = 13.sp)
                    }
                }
            }
        }
        Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(selected?.let { "${it.camera}  ${shown.format(Date(it.startedAt))}" } ?: "Player") {
                Box(Modifier.fillMaxWidth().aspectRatio(1280f / 864f).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    val clip = selected
                    if (clip == null) Text("Tap a clip to play it", color = Shark.muted)
                    else ClipPlayer(clip.file) { note = it }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Tile("Newer", false, Modifier.width(110.dp), height = 52.dp) { step(-1) }
                    Tile("Older", false, Modifier.width(110.dp), height = 52.dp) { step(1) }
                    val s = selected
                    if (s != null) {
                        Tile(if (s.protectedClip) "Unprotect" else "Protect", s.protectedClip, Modifier.width(130.dp), height = 52.dp) {
                            scope.launch { withContext(Dispatchers.IO) { Vehicle.recorder.setProtected(s, !s.protectedClip) }; reload++ }
                        }
                        Tile(if (confirmDelete) "Tap again to delete" else "Delete", false, Modifier.width(if (confirmDelete) 210.dp else 110.dp), height = 52.dp) {
                            if (!confirmDelete) confirmDelete = true
                            else { confirmDelete = false; step(1); scope.launch { withContext(Dispatchers.IO) { Vehicle.recorder.delete(s) }; reload++ } }
                        }
                    }
                }
                if (note.isNotBlank()) Text(note, color = Shark.bad, fontSize = 12.sp)
            }
            Panel("Storage") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("Less", false, Modifier.width(90.dp), height = 52.dp) { capGb = (capGb - 2).coerceAtLeast(2); Vehicle.prefs.storageCapGb = capGb }
                    Text("$capGb GB", color = Shark.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                    Tile("More", false, Modifier.width(90.dp), height = 52.dp) { capGb = (capGb + 2).coerceAtMost(64); Vehicle.prefs.storageCapGb = capGb }
                    Tile("Time stamp on clips", stamp, Modifier.width(220.dp), height = 52.dp, sub = "date, speed, position") { stamp = !stamp; Vehicle.prefs.clipStamp = stamp }
                }
                val u = usage
                if (u != null) {
                    Text("Rolling clips ${gb(u.clipBytes)} of $capGb GB. Protected ${gb(u.protectedBytes)}. Free on the drive ${gb(u.freeBytes)}.", color = Shark.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Saving to ${u.folder}", color = Shark.muted, fontSize = 12.sp)
                    if (capGb * 1_073_741_824L > u.clipBytes + u.freeBytes) Text("The limit is bigger than the space left on the drive.", color = Shark.warm, fontSize = 12.sp)
                }
                Text("Oldest rolling clips are removed once the limit is reached. Protected clips are never removed automatically.", color = Shark.muted, fontSize = 12.sp)
            }
            Panel("Send to your phone") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tile(if (sharing) "Sharing on" else "Share clips", sharing, Modifier.width(150.dp), height = 52.dp, sub = "same wifi as the ute") {
                        if (sharing) Vehicle.clipServer.stop() else runCatching { Vehicle.clipServer.start() }.onFailure { note = "Could not start sharing: ${it.message}" }
                        sharing = Vehicle.clipServer.running
                    }
                    val url = if (sharing) Vehicle.clipServer.url() else null
                    if (url != null) {
                        QrCode(url, Modifier.width(96.dp).height(96.dp))
                        Column { Text(url, color = Shark.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Put your phone on the ute's hotspot, scan this, then tap a clip to watch or save it. Switch it off when you are done.", color = Shark.muted, fontSize = 12.sp) }
                    } else if (sharing) Text("The ute has no wifi address yet. Turn on the hotspot in BYD settings.", color = Shark.warm, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ClipPlayer(file: File, onError: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener { it.isLooping = false; start() }
                setOnErrorListener { _, what, extra -> onError("Could not play this clip ($what, $extra)"); true }
                tag = ""
            }
        },
        update = { view ->
            if (view.tag != file.path) {
                view.tag = file.path
                view.stopPlayback()
                view.setVideoPath(file.path)
            }
        },
        onRelease = { it.stopPlayback() },
    )
}
