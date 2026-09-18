package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nz.lonewolf.shark.service.VehicleService
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.data.BatteryLog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GaugesScreen() {
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val tyreUnit = t?.tyres?.unit ?: "kPa"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("Live") {
            FlowRow(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Gauge("HV battery", fmt(t?.soc, "%"), t?.soc?.let { it / 100f }, colour = Shark.accent)
                Gauge("Fuel", fmt(t?.fuelPercent, "%"), t?.fuelPercent?.let { it / 100f }, colour = Shark.warm)
                Gauge("EV range", fmt(t?.evRangeKm), t?.evRangeKm?.let { it / 100f }, unit = "km")
                Gauge("Fuel range", fmt(t?.fuelRangeKm), t?.fuelRangeKm?.let { it / 800f }, unit = "km", colour = Shark.warm)
                Gauge("Speed", fmt(t?.speedKmh), t?.speedKmh?.let { it / 180f }, unit = "km/h", colour = Shark.cool)
                Gauge("Engine", fmt(t?.engineRpm), t?.engineRpm?.let { it / 6000f }, unit = "rpm", colour = Shark.warm)
                Gauge("12 V", fmt(t?.battery12v, " V"), t?.battery12v?.let { ((it - 10) / 6).toFloat() }, colour = if ((t?.battery12v ?: 13.0) < 12.2) Shark.bad else Shark.accent)
                Gauge("Battery health", fmt(t?.soh, "%"), t?.soh?.let { it / 100f })
            }
        }
        Panel("Battery log") {
            val rows by Vehicle.batteryLog.rows.collectAsStateWithLifecycle()
            val losses = Vehicle.batteryLog.parkedLosses()
            val avg = Vehicle.batteryLog.averagePerDay()
            val v12 = t?.battery12v
            if (v12 != null && v12 < 12.2) Text("12 V battery is low at ${fmt(v12, " V")}. Under 12.2 V at start means it is struggling; the Shark's 13.8 Ah 12 V is a known weak point.", color = Shark.bad, fontSize = 14.sp)
            Text(if (avg == null) "Parked loss: not enough data yet. It needs two starts at the same odometer at least three hours apart." else "Parked loss: about %.1f%% of traction battery per day over ${losses.size} parked periods.".format(avg), color = Shark.text, fontSize = 15.sp)
            losses.takeLast(5).reversed().forEach { l -> Text("${BatteryLog.stamp.format(java.util.Date(l.start))} to ${BatteryLog.stamp.format(java.util.Date(l.end))}: ${l.socDrop}%% in %.0f h".format(l.hours), color = Shark.muted, fontSize = 12.sp) }
            rows.takeLast(6).reversed().forEach { r -> Text("${BatteryLog.stamp.format(java.util.Date(r.time))}  12 V ${fmt(r.v12, " V")}  battery ${fmt(r.soc, "%")}  fuel ${fmt(r.fuel, "%")}  ${fmt(r.odometer, " km")}", color = Shark.muted, fontSize = 12.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("Tyres ($tyreUnit)", Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Column { Tyre("Front left", t?.tyres?.fl); Tyre("Rear left", t?.tyres?.rl) }
                    Column { Tyre("Front right", t?.tyres?.fr); Tyre("Rear right", t?.tyres?.rr) }
                }
                Text("Handbook: 36 psi unladen, 42 psi rear when loaded", color = Shark.muted, fontSize = 12.sp)
            }
            Panel("Trip and vehicle", Modifier.weight(1f)) {
                StatRow("Odometer", fmt(t?.odometerKm, " km"), t?.odometerKm != null)
                StatRow("EV kilometres", fmt(t?.evMileageKm, " km"), t?.evMileageKm != null)
                StatRow("Combined range", fmt(t?.combinedRangeKm, " km"), t?.combinedRangeKm != null)
                StatRow("Usable battery", fmt(t?.usableKwh, " kWh"), t?.usableKwh != null)
                StatRow("Coolant", fmt(t?.coolantC, "°C"), t?.coolantC != null)
                StatRow("Engine power", fmt(t?.enginePowerKw, " kW"), t?.enginePowerKw != null)
                StatRow("Outside", fmt(c?.outsideTemp, "°C"), c?.outsideTemp != null)
                StatRow("Drive mode", modeName(t?.energyMode, t?.operationMode, t?.sportMode), t?.energyMode != null)
                StatRow("Gear", gearName(t?.gear), t?.gear != null)
                StatRow("Charging", if (t?.chargingState == null) "--" else if (t?.chargingState == 0) "not charging" else "state ${t?.chargingState}", t?.chargingState != null)
            }
        }
    }
}

@Composable
private fun Tyre(label: String, v: Double?) {
    Column(Modifier.padding(8.dp)) {
        Text(label, color = Shark.muted, fontSize = 12.sp)
        val colour = when { v == null || v == 0.0 -> Shark.muted; v < 30 -> Shark.bad; else -> Shark.text }
        Text(if (v == null || v == 0.0) "--" else "%.1f".format(v), color = colour, fontSize = 34.sp)
    }
}

private fun modeName(energy: Int?, op: Int?, sport: Int?): String {
    if (energy == null) return "--"
    val e = when (energy) { 0 -> "EV"; 1 -> "HEV"; else -> "energy $energy" }
    val s = when (sport) { 1 -> "Eco"; 2 -> "Normal"; 3 -> "Sport"; null -> ""; else -> "mode $sport" }
    return listOf(e, s, "op $op").filter { it.isNotBlank() }.joinToString(" · ")
}

private fun gearName(g: Int?) = when (g) { null -> "--"; 1 -> "P"; 2 -> "R"; 3 -> "N"; 4 -> "D"; else -> "gear $g" }
