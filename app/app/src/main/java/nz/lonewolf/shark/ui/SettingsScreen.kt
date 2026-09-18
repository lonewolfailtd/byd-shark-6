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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.adb.LocalAdb
import nz.lonewolf.shark.core.byd.BydSdkLoader
import nz.lonewolf.shark.core.byd.CommandResult
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.core.imu.Inclinometer
import nz.lonewolf.shark.data.Profile
import nz.lonewolf.shark.service.VehicleService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(inclinometer: Inclinometer) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by Vehicle.profiles.profiles.collectAsStateWithLifecycle()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val s by VehicleService.seats.collectAsStateWithLifecycle()
    val l by VehicleService.lights.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val running by VehicleService.running.collectAsStateWithLifecycle()
    val adb by LocalAdb.status.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun write(label: String, block: () -> CommandResult) {
        scope.launch { status = withContext(Dispatchers.IO) {
            val r = block()
            runCatching { VehicleService.lights.value = Vehicle.lights.read(); VehicleService.energy.value = Vehicle.energy.read() }
            if (r.ok) "$label done" else "$label refused: ${r.detail}"
        } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("Profiles") {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("New profile name") }, singleLine = true, modifier = Modifier.width(320.dp))
                Tile("Save current state", false, Modifier.width(200.dp)) {
                    if (name.isNotBlank()) { Vehicle.profiles.save(Profile.capture(name.trim(), c, s)); status = "Saved ${name.trim()}"; name = "" }
                }
                Tile("Export", false, Modifier.width(120.dp)) { status = "Exported to ${Vehicle.profiles.exportTo().path}" }
            }
            Spacer(Modifier.height(8.dp))
            profiles.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.name, color = Shark.text, fontSize = 17.sp, modifier = Modifier.width(160.dp))
                    Text(describe(p), color = Shark.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Tile(if (p.applyOnStart) "On start ✓" else "Apply on start", p.applyOnStart, Modifier.width(150.dp), height = 48.dp) {
                        if (p.applyOnStart) Vehicle.profiles.save(p.copy(applyOnStart = false)) else Vehicle.profiles.setApplyOnStart(p.name)
                    }
                    if (!p.builtIn) Tile("Delete", false, Modifier.width(100.dp), height = 48.dp) { Vehicle.profiles.delete(p.name) }
                }
            }
        }
        Panel("Lighting") {
            Text("Ambient: on ${l?.on} colour ${l?.colour} brightness ${l?.brightness} mode ${l?.mode}", color = Shark.muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Tile("Ambient on", l?.on == true, Modifier.width(130.dp)) { write("Ambient") { Vehicle.lights.setAmbientOn(l?.on != true) } }
                Tile("Colour +10", false, Modifier.width(120.dp)) { write("Colour") { Vehicle.lights.nudgeColour(+10) } }
                Tile("Colour −10", false, Modifier.width(120.dp)) { write("Colour") { Vehicle.lights.nudgeColour(-10) } }
                Tile("Bright +1", false, Modifier.width(120.dp)) { write("Brightness") { Vehicle.lights.nudgeBrightness(+1) } }
                Tile("Bright −1", false, Modifier.width(120.dp)) { write("Brightness") { Vehicle.lights.nudgeBrightness(-1) } }
                Tile("Tub light", l?.cargoLight == 1, Modifier.width(120.dp)) { write("Tub light") { Vehicle.lights.setCargoLight(l?.cargoLight != 1) } }
                Tile("Welcome light", l?.welcomeLight == 1, Modifier.width(150.dp)) { write("Welcome light") { Vehicle.lights.setWelcomeLight(l?.welcomeLight != 1) } }
                Tile("Daytime lights", l?.drl == 1, Modifier.width(150.dp)) { write("Daytime lights") { Vehicle.lights.setDaytimeRunningLights(l?.drl != 1) } }
            }
        }
        Panel("Screen") {
            var night by remember { mutableStateOf(VehicleService.nightShade) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(if (night) "Night shade on" else "Night shade", night, Modifier.width(180.dp), sub = "dims every screen") { night = !night; VehicleService.setNightShade(night) }
            }
            Text("BYD's own brightness is on a light sensor and overrides manual changes within seconds, so night mode is a dark shade over the whole screen instead. The shark bubble also has a Night button.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Panel("System") {
            StatRow("Vehicle SDK", "${BydSdkLoader.mode} ${BydSdkLoader.lastError ?: ""}", BydSdkLoader.mode != BydSdkLoader.Mode.UNAVAILABLE)
            StatRow("Vehicle link", if (running) "running" else "stopped", running)
            StatRow("Permissions", adb.toString().substringAfterLast('.'), adb is LocalAdb.Status.Done || VehicleService.telemetry.value != null)
            StatRow("VIN", t?.vin ?: "--", t?.vin != null)
            val fw = Vehicle.firmware
            StatRow("Head unit build", fw.current, !fw.changed)
            if (fw.changed) Text("BYD has updated the head unit since this app was last checked (was ${fw.tested}). Expect ADB to be off and some controls to need re testing.", color = Shark.bad, fontSize = 13.sp)
            Tile(if (fw.tested.isBlank()) "Mark this build as tested" else "Tested on ${fw.tested}", !fw.changed && fw.tested.isNotBlank(), Modifier.width(260.dp), height = 48.dp) { fw.markTested(); status = "Marked ${fw.current} as tested" }
            StatRow("Tilt sensor", inclinometer.availableSensors().joinToString())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                var floating by remember { mutableStateOf(Vehicle.prefs.floatingPanel) }
                androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { floating = Vehicle.prefs.floatingPanel; kotlinx.coroutines.delay(1000) } }
                Tile("Floating quick panel", floating, Modifier.width(200.dp), sub = "shark bubble over other apps") { floating = !floating; VehicleService.setFloating(ctx, floating) }
                Tile("Restart vehicle link", false, Modifier.width(200.dp)) { VehicleService.stop(ctx); VehicleService.start(ctx) }
                Tile("Grant permissions", false, Modifier.width(200.dp)) { scope.launch { LocalAdb.setup(ctx) } }
                Tile("Clear tilt zero", false, Modifier.width(160.dp)) { inclinometer.clearCalibration() }
            }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFFFD54F), fontSize = 14.sp)
    }
}

private fun describe(p: Profile): String = listOfNotNull(
    p.climateOn?.let { if (it) "climate on" else "climate off" },
    p.driverTemp?.let { "driver $it°" }, p.passengerTemp?.let { "passenger $it°" }, p.fan?.let { "fan $it" },
    p.auto?.let { if (it) "auto" else null }, p.recirc?.let { if (it) "recirc" else "fresh" }, p.compressor?.let { if (it) "A/C" else null },
    p.frontDemist?.let { if (it) "demist" else null }, p.rearHeat?.let { if (it) "rear heat" else null },
    p.driverHeat?.let { "seat heat ${it.name.lowercase()}" }, p.driverVent?.let { "seat vent ${it.name.lowercase()}" },
).joinToString(", ")
