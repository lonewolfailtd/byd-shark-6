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
            Panel("Passenger", Modifier.width(220.dp)) {
                SeatCard(s?.passenger, onHeat = { write("Passenger heat") { st.setHeat(SeatBridge.PASSENGER, it) } }, onVent = { write("Passenger vent") { st.setVent(SeatBridge.PASSENGER, it) } })
            }
            Panel("Climate", Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Stepper("Passenger", fmt(c?.passengerTemp, "°"), valueColour = tempColour(c?.passengerTemp), valueWidth = 100.dp,
                        onMinus = { write("Passenger temp") { cl.nudgePassengerTemp(-1) } }, onPlus = { write("Passenger temp") { cl.nudgePassengerTemp(+1) } })
                    FanArc(c?.fan, onMinus = { write("Fan") { cl.nudgeFan(-1) } }, onPlus = { write("Fan") { cl.nudgeFan(+1) } })
                    Stepper("Driver", fmt(c?.driverTemp, "°"), valueColour = tempColour(c?.driverTemp), valueWidth = 100.dp,
                        onMinus = { write("Driver temp") { cl.nudgeDriverTemp(-1) } }, onPlus = { write("Driver temp") { cl.nudgeDriverTemp(+1) } })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Tile(if (on) "Power on" else "Power off", on, Modifier.weight(1f), height = 60.dp) { write("Climate power") { cl.power(!on) } }
                    Tile("Auto", c?.auto == true, Modifier.weight(1f), height = 60.dp) { write("Auto") { cl.setAuto(c?.auto != true) } }
                    Tile("A/C", c?.compressorOn == true, Modifier.weight(1f), height = 60.dp) { write("A/C") { cl.setCompressor(c?.compressorOn != true) } }
                    Tile("Recirc", c?.recirc == true, Modifier.weight(1f), height = 60.dp) { write("Recirc") { cl.setRecirc(c?.recirc != true) } }
                    Tile("Dual", c?.synced == false, Modifier.weight(1f), height = 60.dp) { write("Dual") { cl.setSynced(c?.synced == false) } }
                    Tile("Demist", c?.frontDemist == true, Modifier.weight(1f), height = 60.dp) { write("Demist") { cl.setFrontDemist(c?.frontDemist != true) } }
                    Tile("Rear demist", c?.rearHeat == true, Modifier.weight(1f), height = 60.dp) { write("Rear demist") { cl.setRearHeat(c?.rearHeat != true) } }
                    var rear by remember { mutableStateOf<Boolean?>(null) }
                    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { rear = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { cl.rearAirOn() }.getOrNull() }; kotlinx.coroutines.delay(2000) } }
                    Tile("Rear air", rear == true, Modifier.weight(1f), height = 60.dp) { write("Rear air") { cl.setRearAir(rear != true).also { r -> if (r.ok) rear = rear != true } } }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    profiles.forEach { p -> Tile(p.name, p.applyOnStart, Modifier.width(110.dp), height = 56.dp, sub = if (p.applyOnStart) "on start" else "preset") { applyProfile(p) } }
                }
                Text("Outside ${fmt(c?.outsideTemp, "°C")}. ${if (status.isNotBlank()) status else "Save your own presets in Settings."}", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Panel("Driver", Modifier.width(220.dp)) {
                SeatCard(s?.driver, onHeat = { write("Driver heat") { st.setHeat(SeatBridge.DRIVER, it) } }, onVent = { write("Driver vent") { st.setVent(SeatBridge.DRIVER, it) } }, ventFirst = true)
            }
        }
        PetPanel(write = { label, block -> write(label, block) })
    }
    }
}

/** Pet mode lives with climate because that is what it sets. The notice screen is in PetScreen.kt. */
@Composable
private fun PetPanel(write: (String, () -> CommandResult) -> Unit) {
    val prefs = Vehicle.prefs
    // Starts from whatever the climate is set to now; adjust from there if the day needs colder or warmer.
    val climateNow by VehicleService.climate.collectAsStateWithLifecycle()
    var touched by remember { mutableStateOf(false) }
    var temp by remember { androidx.compose.runtime.mutableIntStateOf(climateNow?.driverTemp ?: prefs.petTemp) }
    androidx.compose.runtime.LaunchedEffect(climateNow?.driverTemp) { if (!touched) climateNow?.driverTemp?.let { temp = it.coerceIn(17, 30) } }
    var message by remember { mutableStateOf(prefs.petMessage) }
    var phone by remember { mutableStateOf(prefs.petPhone) }
    Panel {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(190.dp)) {
                Text("PET MODE", color = Shark.accent, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Keeps the cabin at a set temperature and shows a notice on the screen. The ute must stay on.", color = Shark.muted, fontSize = 12.sp)
            }
            Tile("−", false, Modifier.width(56.dp), height = 52.dp) { touched = true; temp = (temp - 1).coerceAtLeast(17); prefs.petTemp = temp }
            Text("$temp°", color = Shark.text, fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Tile("+", false, Modifier.width(56.dp), height = 52.dp) { touched = true; temp = (temp + 1).coerceAtMost(30); prefs.petTemp = temp }
            androidx.compose.material3.OutlinedTextField(message, { message = it.take(80); prefs.petMessage = message }, label = { Text("Message for people walking past") }, singleLine = true, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(phone, { phone = it.take(20); prefs.petPhone = phone }, label = { Text("Your phone number") }, singleLine = true, modifier = Modifier.width(200.dp))
            Tile("Turn pet mode on", false, Modifier.width(190.dp), height = 56.dp) {
                write("Pet mode") {
                    val r = Vehicle.climate.power(true)
                    Vehicle.climate.setSynced(true); Vehicle.climate.setDriverTemp(temp); Vehicle.climate.setCompressor(true); Vehicle.climate.setRecirc(false)
                    r
                }
                PetMode.temp = temp; PetMode.message = message; PetMode.phone = phone; PetMode.active = true
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
        val seatPic = rememberArt("seat", coloured = false)
        androidx.compose.foundation.layout.Box(Modifier.width(150.dp).height(180.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            if (seatPic != null) androidx.compose.foundation.Image(seatPic, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val centre = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.45f)
                if (heat > 0) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFF3C28).copy(alpha = if (heat == 2) 0.7f else 0.4f), androidx.compose.ui.graphics.Color.Transparent), centre, size.width * 0.42f), size.width * 0.42f, centre)
                if (vent > 0) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3CA0FF).copy(alpha = if (vent == 2) 0.65f else 0.38f), androidx.compose.ui.graphics.Color.Transparent), centre, size.width * 0.42f), size.width * 0.42f, centre)
            }
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
        Tile("II", level == 2, Modifier.fillMaxWidth(), height = 52.dp) { onSet(SeatBridge.Level.HIGH) }
        Tile("I", level == 1, Modifier.fillMaxWidth(), height = 52.dp) { onSet(SeatBridge.Level.LOW) }
        Tile("Off", level == 0, Modifier.fillMaxWidth(), height = 48.dp) { onSet(SeatBridge.Level.OFF) }
    }
}

/** Fan speed as an arc, like the dash, with the buttons under it. */
@Composable
private fun FanArc(fan: Int?, onMinus: () -> Unit, onPlus: () -> Unit) {
    val accent = Shark.accent
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(Modifier.width(180.dp).height(110.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
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
            Tile("−", false, Modifier.width(72.dp), height = 52.dp, onClick = onMinus)
            Tile("+", false, Modifier.width(72.dp), height = 52.dp, onClick = onPlus)
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
