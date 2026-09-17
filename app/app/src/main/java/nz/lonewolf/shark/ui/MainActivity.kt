package nz.lonewolf.shark.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.core.imu.Inclinometer
import nz.lonewolf.shark.service.VehicleService

/**
 * Phase 1 deliverable: a Diagnostics screen. Every bridge call, its live value or its error,
 * plus the setup button that grants the app its permissions through the local ADB.
 * The real pages (Climate, Gauges, Off Road, Towing, Profiles) build on top of this.
 */
class MainActivity : ComponentActivity() {
    private lateinit var inclinometer: Inclinometer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Vehicle.init(this)
        inclinometer = Inclinometer(this)
        // Load the SDK and start the vehicle link as soon as the app opens; the buttons stay as a manual retry.
        Thread {
            runCatching { BydSdkLoader.ensure(applicationContext) }
                .onFailure { android.util.Log.w("Shark", "sdk ensure failed", it) }
            runCatching { VehicleService.start(applicationContext) }
                .onFailure { android.util.Log.w("Shark", "service start failed", it) }
        }.start()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF3DDC84), background = Color(0xFF0B1220))) {
                Diagnostics(inclinometer)
            }
        }
    }

    override fun onResume() { super.onResume(); inclinometer.start() }
    override fun onPause() { inclinometer.stop(); super.onPause() }
}

private data class Row(val label: String, val value: String, val ok: Boolean?)

@Composable
private fun Diagnostics(inclinometer: Inclinometer) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val adbStatus by LocalAdb.status.collectAsStateWithLifecycle()
    val running by VehicleService.running.collectAsStateWithLifecycle()
    val telemetry by VehicleService.telemetry.collectAsStateWithLifecycle()
    val climate by VehicleService.climate.collectAsStateWithLifecycle()
    val seats by VehicleService.seats.collectAsStateWithLifecycle()
    val tilt by inclinometer.reading.collectAsStateWithLifecycle()
    var log by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf<List<String>>(emptyList()) }

    val rows = buildList {
        add(Row("SDK", "${BydSdkLoader.mode} ${BydSdkLoader.lastError ?: ""}", BydSdkLoader.mode != BydSdkLoader.Mode.UNAVAILABLE))
        add(Row("Local ADB port", if (LocalAdb.portOpen()) "open" else "closed", LocalAdb.portOpen()))
        add(Row("Grants", adbStatus.toString().substringAfterLast('.'), adbStatus is LocalAdb.Status.Done))
        add(Row("Service", if (running) "running" else "stopped", running))
        climate?.let { c ->
            add(Row("Climate bound", c.error ?: "yes", c.bound))
            add(Row("Climate", "power ${c.powerOn} driver ${c.driverTemp} passenger ${c.passengerTemp} outside ${c.outsideTemp} fan ${c.fan} auto ${c.auto} recirc ${c.recirc} demist ${c.frontDemist} rear ${c.rearHeat} sync ${c.synced}", null))
        }
        seats?.let { s ->
            add(Row("Seats bound", s.error ?: "yes", s.bound))
            add(Row("Seats", "driver heat ${s.driver.heat} vent ${s.driver.vent}; passenger heat ${s.passenger.heat} vent ${s.passenger.vent}; features heat ${s.hasHeat} vent ${s.hasVent}", null))
        }
        telemetry?.let { t ->
            add(Row("SOC / kWh", "${t.soc}% ${t.usableKwh}", t.soc != null))
            add(Row("Range EV / fuel / all", "${t.evRangeKm} / ${t.fuelRangeKm} / ${t.combinedRangeKm} km, fuel ${t.fuelPercent}%", t.evRangeKm != null))
            add(Row("Odometer", "${t.odometerKm} km, EV ${t.evMileageKm} km", t.odometerKm != null))
            add(Row("Speed / pedals / gear", "${t.speedKmh} km/h, acc ${t.accelerator}, brake ${t.brake}, gear ${t.gear}", t.speedKmh != null))
            add(Row("Engine", "${t.engineRpm} rpm, ${t.enginePowerKw} kW, coolant ${t.coolantC}, oil ${t.oilLevel}", t.engineRpm != null))
            add(Row("Modes", "energy ${t.energyMode}, operation ${t.operationMode}, sport ${t.sportMode}, power level ${t.powerLevel}", t.energyMode != null))
            add(Row("12 V / SOH / charging", "${t.battery12v} V, SOH ${t.soh}, charging ${t.chargingState}", t.battery12v != null))
            add(Row("Tyres (${t.tyres.unit})", "FL ${t.tyres.fl} FR ${t.tyres.fr} RL ${t.tyres.rl} RR ${t.tyres.rr}", t.tyres.fl != null))
            add(Row("Steering / VIN", "${t.steeringAngle}, ${t.vin}", t.vin != null))
        }
        add(Row("Tilt", "pitch %.1f roll %.1f (%s)".format(tilt.pitch, tilt.roll, tilt.sensorName ?: "no sensor"), tilt.sensorName != null))
        add(Row("Accelerometers", inclinometer.availableSensors().joinToString(" | "), null))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Lonewolf Shark diagnostics", fontSize = 26.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { LocalAdb.setup(ctx) } }) { Text("1. Grant permissions") }
            Button(onClick = { VehicleService.start(ctx) }) { Text("2. Start vehicle link") }
            OutlinedButton(onClick = { inclinometer.calibrate() }) { Text("Level here") }
            OutlinedButton(onClick = {
                scope.launch { methods = withContext(Dispatchers.IO) {
                    Vehicle.climate.device.methodDump() + listOf("== setting ==") + Vehicle.seats.device.methodDump() +
                        listOf("== light ==") + Vehicle.lights.device.methodDump() +
                        Vehicle.telemetry.devices.flatMap { (n, d) -> listOf("== $n ==") + d.methodDump() }
                } }
            }) { Text("Dump methods") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { log = withContext(Dispatchers.IO) { Vehicle.climate.setDriverTemp((climate?.driverTemp ?: 22) + 1).toString() } } }) { Text("Driver temp +1") }
            OutlinedButton(onClick = { scope.launch { log = withContext(Dispatchers.IO) { Vehicle.climate.setDriverTemp((climate?.driverTemp ?: 22) - 1).toString() } } }) { Text("Driver temp -1") }
            OutlinedButton(onClick = { scope.launch { log = withContext(Dispatchers.IO) { Vehicle.climate.setFan((climate?.fan ?: 3) + 1).toString() } } }) { Text("Fan +1") }
            OutlinedButton(onClick = { scope.launch { log = withContext(Dispatchers.IO) { Vehicle.seats.setHeat(1, nz.lonewolf.shark.core.byd.SeatBridge.Level.LOW).toString() } } }) { Text("Driver seat heat low") }
            OutlinedButton(onClick = { scope.launch { log = withContext(Dispatchers.IO) { Vehicle.seats.setHeat(1, nz.lonewolf.shark.core.byd.SeatBridge.Level.OFF).toString() } } }) { Text("Driver seat heat off") }
        }
        if (log.isNotBlank()) Text(log, color = Color(0xFFFFD54F), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(rows) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val dot = when (r.ok) { true -> Color(0xFF3DDC84); false -> Color(0xFFFF5252); null -> Color.Gray }
                    Text("●", color = dot, modifier = Modifier.width(24.dp))
                    Text(r.label, color = Color.White, modifier = Modifier.width(200.dp), fontSize = 15.sp)
                    Text(r.value, color = Color(0xFFB0BEC5), fontSize = 15.sp)
                }
            }
            if (methods.isNotEmpty()) {
                items(methods) { m -> Text(m, color = Color(0xFF80CBC4), fontSize = 12.sp) }
            }
        }
    }
}
