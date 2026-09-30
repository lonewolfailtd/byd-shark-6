package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.CommandResult
import nz.lonewolf.shark.core.byd.SeatBridge
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.data.Profile
import nz.lonewolf.shark.service.VehicleService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClimateScreen() {
    val scope = rememberCoroutineScope()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val s by VehicleService.seats.collectAsStateWithLifecycle()
    val profiles by Vehicle.profiles.profiles.collectAsStateWithLifecycle()
    val e by VehicleService.energy.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf("") }

    fun write(label: String, block: () -> CommandResult) {
        scope.launch {
            status = withContext(Dispatchers.IO) {
                val r = block()
                runCatching {
                    VehicleService.climate.value = Vehicle.climate.read()
                    VehicleService.seats.value = Vehicle.seats.read()
                }
                if (r.ok) "$label done" else "$label refused: ${r.detail}"
            }
        }
    }
    fun applyProfile(p: Profile) {
        scope.launch { status = withContext(Dispatchers.IO) {
            val res = Vehicle.profiles.apply(p)
            runCatching { VehicleService.climate.value = Vehicle.climate.read(); VehicleService.seats.value = Vehicle.seats.read() }
            "${p.name}: " + res.count { it.endsWith("ok") } + " of ${res.size} applied" + res.filter { !it.endsWith("ok") }.joinToString("") { ", $it" }
        } }
    }

    val cl = Vehicle.climate
    val st = Vehicle.seats
    val on = c?.powerOn == true

    Backdrop("bg_climate", coloured = false, wash = 0.35f) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel("Passenger", Modifier.width(250.dp)) {
                SeatCard(s?.passenger, onHeat = { write("Passenger heat") { st.setHeat(SeatBridge.PASSENGER, it) } }, onVent = { write("Passenger vent") { st.setVent(SeatBridge.PASSENGER, it) } })
            }
            Panel("Climate", Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Stepper("Passenger", fmt(c?.passengerTemp, "°"), valueColour = tempColour(c?.passengerTemp),
                        onMinus = { write("Passenger temp") { cl.nudgePassengerTemp(-1) } }, onPlus = { write("Passenger temp") { cl.nudgePassengerTemp(+1) } })
                    FanArc(c?.fan, onMinus = { write("Fan") { cl.nudgeFan(-1) } }, onPlus = { write("Fan") { cl.nudgeFan(+1) } })
                    Stepper("Driver", fmt(c?.driverTemp, "°"), valueColour = tempColour(c?.driverTemp),
                        onMinus = { write("Driver temp") { cl.nudgeDriverTemp(-1) } }, onPlus = { write("Driver temp") { cl.nudgeDriverTemp(+1) } })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                    Tile(if (on) "Power on" else "Power off", on, Modifier.width(120.dp), height = 60.dp) { write("Climate power") { cl.power(!on) } }
                    Tile("Auto", c?.auto == true, Modifier.width(100.dp), height = 60.dp) { write("Auto") { cl.setAuto(c?.auto != true) } }
                    Tile("A/C", c?.compressorOn == true, Modifier.width(100.dp), height = 60.dp) { write("A/C") { cl.setCompressor(c?.compressorOn != true) } }
                    Tile("Recirc", c?.recirc == true, Modifier.width(110.dp), height = 60.dp) { write("Recirc") { cl.setRecirc(c?.recirc != true) } }
                    Tile("Sync", c?.synced == true, Modifier.width(100.dp), height = 60.dp) { write("Sync") { cl.setSynced(c?.synced != true) } }
                    Tile("Demist", c?.frontDemist == true, Modifier.width(110.dp), height = 60.dp) { write("Demist") { cl.setFrontDemist(c?.frontDemist != true) } }
                    Tile("Rear heat", c?.rearHeat == true, Modifier.width(120.dp), height = 60.dp) { write("Rear heat") { cl.setRearHeat(c?.rearHeat != true) } }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    profiles.forEach { p -> Tile(p.name, p.applyOnStart, Modifier.width(110.dp), height = 56.dp, sub = if (p.applyOnStart) "on start" else "preset") { applyProfile(p) } }
                }
                Text("Outside ${fmt(c?.outsideTemp, "°C")}. ${if (status.isNotBlank()) status else "Save your own presets in Settings."}", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Panel("Driver", Modifier.width(250.dp)) {
                SeatCard(s?.driver, onHeat = { write("Driver heat") { st.setHeat(SeatBridge.DRIVER, it) } }, onVent = { write("Driver vent") { st.setVent(SeatBridge.DRIVER, it) } }, ventFirst = true)
            }
        }
        Panel("Camp and V2L") {
            val v = e?.v2l
            val watts = v?.watts
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) {
                    StatRow("V2L", when (v?.on) { true -> "ON"; false -> "off"; null -> "--" }, v?.on)
                    StatRow("Output", if (watts != null) "%.0f W (%d V, %.1f A)".format(watts, v?.volts, v?.amps) else "--")
                    StatRow("Energy this session", fmt(v?.energyKwh, " kWh"))
                    StatRow("Time remaining", v?.remainMin?.let { "${it / 60} h ${it % 60} min" } ?: "--")
                }
                Column(Modifier.weight(1f)) {
                    StatRow("Battery", fmt(t?.soc, "%"))
                    StatRow("Stops at", fmt(v?.limitPercent, "%"))
                    StatRow("Camping balance", when (v?.campingBalance) { null -> "--"; 1 -> "on"; 2, 0 -> "off"; else -> "state ${v?.campingBalance}" })
                    StatRow("Runtime at this draw", if (watts != null && watts > 50) "%.1f h to the floor".format(((t?.soc ?: 0) - (v?.limitPercent ?: 15)).coerceAtLeast(0) / 100.0 * 29.58 * 1000 / watts) else "--")
                }
            }
            val hrs = v?.timeHours
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Run time " + (if (hrs != null) "%.0f h".format(hrs) else "--"), color = Shark.text, fontSize = 16.sp, modifier = Modifier.width(130.dp))
                listOf(5, 8, 12, 24).forEach { h -> Tile("$h h", hrs != null && kotlin.math.abs(hrs - h) < 0.5, Modifier.width(80.dp), height = 48.dp) { write("V2L $h h") { Vehicle.energy.setDischargeHours(h) } } }
                Text(if (v?.timeUnit == null) "The ute reports ${v?.timeSetting ?: "nothing"} for run time, so the unit is not known yet and these stay off." else "Sets how long V2L runs in one go. Raw ${v.timeSetting} ${v.timeUnit}", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Text("V2L is switched on from BYD's Energy screen (Charging and Discharging). The engine will start itself below the floor. Camp preset above keeps the cabin comfortable at low fan.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
    }
}

/** A drawn seat that glows red for heat and blue for vent, with the level buttons under it. */
@Composable
private fun SeatCard(seat: SeatBridge.Seat?, onHeat: (SeatBridge.Level) -> Unit, onVent: (SeatBridge.Level) -> Unit, ventFirst: Boolean = false) {
    val heat = when (seat?.heat) { SeatBridge.Level.LOW -> 1; SeatBridge.Level.HIGH -> 2; else -> 0 }
    val vent = when (seat?.vent) { SeatBridge.Level.LOW -> 1; SeatBridge.Level.HIGH -> 2; else -> 0 }
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.width(150.dp).height(180.dp)) {
            val u = size.width / 120f
            val face = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF2A3650), androidx.compose.ui.graphics.Color(0xFF0F1626)))
            val line = androidx.compose.ui.graphics.Color(0xFF4A5C7A)
            fun part(x: Float, y: Float, w: Float, h: Float, r: Float) {
                drawRoundRect(face, androidx.compose.ui.geometry.Offset(x * u, y * u), androidx.compose.ui.geometry.Size(w * u, h * u), androidx.compose.ui.geometry.CornerRadius(r * u))
                drawRoundRect(line, androidx.compose.ui.geometry.Offset(x * u, y * u), androidx.compose.ui.geometry.Size(w * u, h * u), androidx.compose.ui.geometry.CornerRadius(r * u), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            part(38f, 4f, 44f, 26f, 10f); part(18f, 34f, 84f, 76f, 10f); part(8f, 112f, 104f, 34f, 10f)
            for (x in listOf(40f, 60f, 80f)) drawLine(androidx.compose.ui.graphics.Color(0xFF3A4B66), androidx.compose.ui.geometry.Offset(x * u, 44f * u), androidx.compose.ui.geometry.Offset(x * u, 98f * u), 2f)
            if (heat > 0) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFF3C28).copy(alpha = if (heat == 2) 0.75f else 0.45f), androidx.compose.ui.graphics.Color.Transparent)), 46f * u, androidx.compose.ui.geometry.Offset(60f * u, 78f * u))
            if (vent > 0) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3CA0FF).copy(alpha = if (vent == 2) 0.7f else 0.4f), androidx.compose.ui.graphics.Color.Transparent)), 46f * u, androidx.compose.ui.geometry.Offset(60f * u, 78f * u))
        }
        val cols: List<@Composable () -> Unit> = listOf(
            { Levels("Heat", heat, onHeat) },
            { Levels("Vent", vent, onVent) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (if (ventFirst) cols.reversed() else cols).forEach { Column(Modifier.weight(1f)) { it() } }
        }
    }
}

@Composable
private fun Levels(label: String, level: Int, onSet: (SeatBridge.Level) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(label, color = Shark.muted, fontSize = 12.sp)
        Tile("II", level == 2, Modifier.fillMaxWidth(), height = 44.dp) { onSet(SeatBridge.Level.HIGH) }
        Tile("I", level == 1, Modifier.fillMaxWidth(), height = 44.dp) { onSet(SeatBridge.Level.LOW) }
        Tile("Off", level == 0, Modifier.fillMaxWidth(), height = 40.dp) { onSet(SeatBridge.Level.OFF) }
    }
}

/** Fan speed as an arc, like the dash, with the buttons under it. */
@Composable
private fun FanArc(fan: Int?, onMinus: () -> Unit, onPlus: () -> Unit) {
    val accent = Shark.accent
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(Modifier.width(220.dp).height(120.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val d = size.width - 20.dp.toPx()
                val tl = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx())
                val sz = androidx.compose.ui.geometry.Size(d, d)
                drawArc(androidx.compose.ui.graphics.Color(0xFF223047), 180f, 180f, false, tl, sz, style = stroke)
                if (fan != null && fan > 0) {
                    drawArc(accent.copy(alpha = 0.25f), 180f, 180f * fan / 7f, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(accent, 180f, 180f * fan / 7f, false, tl, sz, style = stroke)
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(fmt(fan), color = androidx.compose.ui.graphics.Color.White, fontSize = 40.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(if ((fan ?: 0) > 0) "FAN" else "OFF", color = Shark.muted, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("−", false, Modifier.width(64.dp), height = 44.dp, onClick = onMinus)
            Tile("+", false, Modifier.width(64.dp), height = 44.dp, onClick = onPlus)
        }
    }
}

@Composable
private fun SeatControls(seat: SeatBridge.Seat?, onHeat: (SeatBridge.Level) -> Unit, onVent: (SeatBridge.Level) -> Unit) {
    Text("Heat", color = Shark.muted, fontSize = 13.sp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Tile("High", seat?.heat == SeatBridge.Level.HIGH, Modifier.fillMaxWidth(), height = 56.dp) { onHeat(SeatBridge.Level.HIGH) }
        Tile("Low", seat?.heat == SeatBridge.Level.LOW, Modifier.fillMaxWidth(), height = 56.dp) { onHeat(SeatBridge.Level.LOW) }
        Tile("Off", seat?.heat == SeatBridge.Level.OFF || seat?.heat == null, Modifier.fillMaxWidth(), height = 56.dp) { onHeat(SeatBridge.Level.OFF) }
    }
    Spacer(Modifier.height(10.dp))
    Text("Vent", color = Shark.muted, fontSize = 13.sp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Tile("High", seat?.vent == SeatBridge.Level.HIGH, Modifier.fillMaxWidth(), height = 56.dp) { onVent(SeatBridge.Level.HIGH) }
        Tile("Low", seat?.vent == SeatBridge.Level.LOW, Modifier.fillMaxWidth(), height = 56.dp) { onVent(SeatBridge.Level.LOW) }
        Tile("Off", seat?.vent == SeatBridge.Level.OFF || seat?.vent == null, Modifier.fillMaxWidth(), height = 56.dp) { onVent(SeatBridge.Level.OFF) }
    }
}

private fun tempColour(t: Int?) = when {
    t == null -> Shark.muted
    t <= 19 -> Shark.cool
    t >= 25 -> Shark.warm
    else -> Shark.text
}
