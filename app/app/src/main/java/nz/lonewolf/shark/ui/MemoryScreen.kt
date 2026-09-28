package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.AssistBridge
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService

/** Remembers the driver assist warnings the ute forgets on every start. */
@Composable
fun MemoryScreen() {
    val scope = rememberCoroutineScope()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val memory = Vehicle.assistMemory
    val bridge = Vehicle.assist
    var live by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var safety by remember { mutableStateOf<AssistBridge.Safety?>(null) }
    var restore by remember { mutableStateOf(memory.restoreOnStart) }
    var bump by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(memory.lastReport) }

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { runCatching { live = bridge.readAll(); safety = bridge.safety() } }
            delay(2000)
        }
    }
    @Suppress("UNUSED_EXPRESSION") bump

    Backdrop("bg_climate", coloured = false, wash = 0.4f) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("SETTINGS MEMORY", color = Shark.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("Set each one the way you like it on BYD's own screen, then tap Remember.", color = Shark.muted, fontSize = 14.sp)
                }
                Tile(if (restore) "Restore at start: on" else "Restore at start: off", restore, Modifier.width(250.dp), height = 56.dp, sub = "only while in Park") { restore = !restore; memory.restoreOnStart = restore }
                Tile("Restore now", false, Modifier.width(160.dp), height = 56.dp) {
                    if (t?.gear != null && t?.gear != 1) status = "Put the ute in Park first"
                    else scope.launch { status = withContext(Dispatchers.IO) { memory.restoreAll(bridge).joinToString("\n").ifBlank { "Nothing remembered yet" } } }
                }
            }
            bridge.items.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { item ->
                        val now = live[item.key]
                        val kept = memory.remembered(item.key)
                        Glass(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.label, color = Shark.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                    Text(item.note, color = Shark.muted, fontSize = 12.sp)
                                    Text(
                                        when {
                                            now == null -> "Not available on this ute"
                                            kept == null -> "Ute now: $now. Nothing remembered"
                                            kept == now -> "Ute now: $now. Matches what you saved"
                                            else -> "Ute now: $now. Will restore $kept"
                                        },
                                        color = when { now == null -> Shark.muted; kept != null && kept != now -> Shark.warm; kept != null -> Shark.accent; else -> Shark.muted }, fontSize = 13.sp,
                                    )
                                }
                                if (now != null) Tile(if (kept == now) "Saved" else "Remember", kept == now, Modifier.width(130.dp), height = 52.dp) { memory.remember(item.key, now); bump++ }
                                if (kept != null) Tile("Forget", false, Modifier.width(100.dp), height = 52.dp) { memory.forget(item.key); bump++ }
                            }
                        }
                    }
                    if (pair.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
            if (status.isNotBlank()) Glass(Modifier.fillMaxWidth()) { Text(status, color = Shark.text, fontSize = 14.sp) }
            Glass(Modifier.fillMaxWidth()) {
                Text("Never changed by this app", color = Shark.warm, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Emergency braking, stability control, ABS and emergency lane keeping are shown for reference only.", color = Shark.muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text("Auto hold ${fmt(safety?.autoHold)}", color = Shark.text, fontSize = 14.sp)
                    Text("Hill descent ${fmt(safety?.hillDescent)}", color = Shark.text, fontSize = 14.sp)
                    Text("Stability ${fmt(safety?.stability)}", color = Shark.text, fontSize = 14.sp)
                    Text("Emergency braking ${fmt(safety?.emergencyBraking)}", color = Shark.text, fontSize = 14.sp)
                }
            }
        }
    }
}
