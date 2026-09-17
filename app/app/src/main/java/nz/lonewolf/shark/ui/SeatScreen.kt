package nz.lonewolf.shark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.SeatPositionBridge
import nz.lonewolf.shark.core.byd.SeatPositionBridge.Axis
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService

@Composable
fun SeatScreen() {
    val scope = rememberCoroutineScope()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val pos by VehicleService.seatPosition.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf("") }
    val sp = Vehicle.seatPosition
    val parked = t?.gear == 1 && (t?.speedKmh ?: 0) == 0
    val seat = SeatPositionBridge.DRIVER

    fun io(label: String, block: () -> nz.lonewolf.shark.core.byd.CommandResult) {
        scope.launch { status = withContext(Dispatchers.IO) { val r = block(); if (r.ok) "$label done" else "$label refused: ${r.detail}" } }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("Driver seat, hold to move", Modifier.weight(2f)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    JogPair("Fore / aft", "Forward", "Back", Axis.FORE_AFT, seat, sp)
                    JogPair("Height", "Up", "Down", Axis.HEIGHT, seat, sp)
                    JogPair("Backrest", "Upright", "Recline", Axis.BACKREST, seat, sp)
                    JogPair("Cushion", "Up", "Down", Axis.CUSHION, seat, sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Position: fore/aft ${fmt(pos?.horizontal)}  height ${fmt(pos?.height)}  backrest ${fmt(pos?.backrest)}  cushion ${fmt(pos?.cushion)}",
                    color = Shark.muted, fontSize = 13.sp,
                )
            }
            Panel("Memory", Modifier.weight(1f)) {
                (1..3).forEach { slot ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("Recall $slot", false, Modifier.weight(1f), height = 60.dp, sub = if (parked) null else "park first") {
                            if (parked) io("Recall $slot") { sp.recall(seat, slot) } else status = "Recall only works in Park with the ute stopped"
                        }
                        Tile("Save", false, Modifier.width(90.dp), height = 60.dp) { io("Save $slot") { sp.save(seat, slot) } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Tile("Easy entry", pos?.easyEntry == 1, Modifier.fillMaxWidth(), height = 56.dp, sub = "seat slides back when you get out") {
                    io("Easy entry") { sp.setEasyEntry(pos?.easyEntry != 1) }
                }
            }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFFFD54F), fontSize = 14.sp)
        Text("Movement stops the moment you lift your finger. Memory recall moves the seat, so it is locked out unless the ute is in Park.", color = Shark.muted, fontSize = 12.sp)
    }
}

@Composable
private fun JogPair(label: String, a: String, b: String, axis: Axis, seat: Int, sp: SeatPositionBridge) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Shark.muted, fontSize = 13.sp)
        HoldButton(a) { down -> if (down) sp.jog(seat, axis, 1) else sp.stop(seat, axis) }
        HoldButton(b) { down -> if (down) sp.jog(seat, axis, 2) else sp.stop(seat, axis) }
    }
}

/** Fires on press and again on release, so the seat motor runs only while held. */
@Composable
private fun HoldButton(label: String, onChange: (Boolean) -> Unit) {
    var held by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.width(120.dp).height(70.dp).clip(RoundedCornerShape(14.dp))
            .background(if (held) Shark.accentDim else Color(0xFF1B2638))
            .border(1.5.dp, if (held) Shark.accent else Shark.panelLine, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held = true
                        scope.launch(Dispatchers.IO) { runCatching { onChange(true) } }
                        try { tryAwaitRelease() } finally {
                            held = false
                            scope.launch(Dispatchers.IO) { runCatching { onChange(false) } }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Shark.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}
