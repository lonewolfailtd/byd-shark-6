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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Passenger seat
            Panel("Passenger seat", Modifier.weight(1f)) {
                SeatControls(s?.passenger, onHeat = { write("Passenger heat") { st.setHeat(SeatBridge.PASSENGER, it) } }, onVent = { write("Passenger vent") { st.setVent(SeatBridge.PASSENGER, it) } })
            }
            // Climate centre
            Panel("Climate", Modifier.weight(2.2f)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Stepper("Passenger", fmt(c?.passengerTemp, "°"), valueColour = tempColour(c?.passengerTemp),
                        onMinus = { write("Passenger temp") { cl.nudgePassengerTemp(-1) } }, onPlus = { write("Passenger temp") { cl.nudgePassengerTemp(+1) } })
                    Stepper("Fan", fmt(c?.fan), onMinus = { write("Fan") { cl.nudgeFan(-1) } }, onPlus = { write("Fan") { cl.nudgeFan(+1) } })
                    Stepper("Driver", fmt(c?.driverTemp, "°"), valueColour = tempColour(c?.driverTemp),
                        onMinus = { write("Driver temp") { cl.nudgeDriverTemp(-1) } }, onPlus = { write("Driver temp") { cl.nudgeDriverTemp(+1) } })
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile(if (on) "Power on" else "Power off", on, Modifier.width(120.dp)) { write("Climate power") { cl.power(!on) } }
                    Tile("Auto", c?.auto == true, Modifier.width(100.dp)) { write("Auto") { cl.setAuto(c?.auto != true) } }
                    Tile("A/C", c?.compressorOn == true, Modifier.width(100.dp)) { write("A/C") { cl.setCompressor(c?.compressorOn != true) } }
                    Tile("Recirc", c?.recirc == true, Modifier.width(110.dp)) { write("Recirc") { cl.setRecirc(c?.recirc != true) } }
                    Tile("Sync", c?.synced == true, Modifier.width(100.dp)) { write("Sync") { cl.setSynced(c?.synced != true) } }
                    Tile("Demist", c?.frontDemist == true, Modifier.width(110.dp)) { write("Demist") { cl.setFrontDemist(c?.frontDemist != true) } }
                    Tile("Rear heat", c?.rearHeat == true, Modifier.width(120.dp)) { write("Rear heat") { cl.setRearHeat(c?.rearHeat != true) } }
                    Tile("Max cool", c?.maxCool == true, Modifier.width(120.dp)) { write("Max cool") { cl.setMaxCool(c?.maxCool != true) } }
                    Tile("Air only", c?.airOnly == true, Modifier.width(110.dp)) { write("Air only") { cl.setAirOnly(c?.airOnly != true) } }
                }
                Spacer(Modifier.height(8.dp))
                Text("Outside ${fmt(c?.outsideTemp, "°C")}   ${status}", color = Shark.muted, fontSize = 14.sp)
            }
            // Driver seat
            Panel("Driver seat", Modifier.weight(1f)) {
                SeatControls(s?.driver, onHeat = { write("Driver heat") { st.setHeat(SeatBridge.DRIVER, it) } }, onVent = { write("Driver vent") { st.setVent(SeatBridge.DRIVER, it) } })
            }
        }
        Panel("Profiles") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { p ->
                    Tile(p.name, p.applyOnStart, Modifier.width(130.dp), sub = if (p.applyOnStart) "on start" else null) { applyProfile(p) }
                }
            }
            Text("Tap to apply. Save and manage profiles in Settings.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
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
            Text("V2L is switched on from BYD's Energy screen (Charging and Discharging). The engine will start itself below the floor. Camp profile above keeps the cabin comfortable at low fan.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
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
