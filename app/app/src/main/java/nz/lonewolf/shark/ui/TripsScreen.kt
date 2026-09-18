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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.data.TripLog
import nz.lonewolf.shark.service.VehicleService
import java.util.Calendar
import java.util.Date

@Composable
fun TripsScreen() {
    val trips by Vehicle.trips.trips.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf("") }
    val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    val (allKm, bizKm, evKm) = Vehicle.trips.totals()
    val (mKm, mBiz, mEv) = Vehicle.trips.totals(monthStart)
    val cur = Vehicle.trips.current
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val fills by Vehicle.fuel.fills.collectAsStateWithLifecycle()
    var basis by remember { mutableStateOf(Vehicle.fuel.basis) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("This month", Modifier.weight(1f)) {
                StatRow("Kilometres", "$mKm km")
                StatRow("Business", "$mBiz km (${if (mKm > 0) mBiz * 100 / mKm else 0}%)")
                StatRow("On battery", "$mEv km (${if (mKm > 0) mEv * 100 / mKm else 0}%)")
            }
            Panel("All logged", Modifier.weight(1f)) {
                StatRow("Kilometres", "$allKm km")
                StatRow("Business", "$bizKm km")
                StatRow("On battery", "$evKm km")
                StatRow("Trips", "${trips.size}")
            }
            Panel("Logbook", Modifier.weight(1f)) {
                Tile("Export CSV", false, Modifier.fillMaxWidth(), sub = "for the mileage claim") { status = "Saved ${Vehicle.trips.exportCsv().path}" }
                Text("Trips start when you leave Park and end a minute after you park. Tap a trip to mark it business.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                if (cur != null) Text("Trip in progress: ${cur.km} km so far", color = Shark.accent, fontSize = 14.sp)
            }
        }
        Panel("Fuel") {
            val f = Vehicle.fuel
            val litres = f.litresLeft(t?.fuelPercent)
            val measured = f.measuredConsumption()
            val use = measured ?: basis
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Gauge("Fuel left", fmt(t?.fuelPercent, "%"), t?.fuelPercent?.let { it / 100f }, unit = fmt(litres, " L of 60"), colour = Shark.warm)
                Gauge("Range at %.1f L/100".format(use), fmt(f.rangeAt(use, t?.fuelPercent)), f.rangeAt(use, t?.fuelPercent)?.let { it / 800f }, unit = "km", colour = Shark.warm)
                Gauge("BYD says", fmt(t?.fuelRangeKm), t?.fuelRangeKm?.let { it / 800f }, unit = "km")
                Gauge("Full tank", "${(f.tankLitres / use * 100).toInt()}", 1f, unit = "km at %.1f".format(use))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Consumption basis", color = Shark.muted, fontSize = 13.sp)
                Tile("−0.5", false, Modifier.width(80.dp), height = 48.dp) { basis = (basis - 0.5).coerceAtLeast(3.0); f.basis = basis }
                Text("%.1f L/100 km".format(basis), color = Shark.text, fontSize = 16.sp)
                Tile("+0.5", false, Modifier.width(80.dp), height = 48.dp) { basis = (basis + 0.5).coerceAtMost(20.0); f.basis = basis }
                Text(if (measured != null) "Measured between your last two full fills: %.1f L/100 km, and that is what the range uses.".format(measured) else "Log two full tank fills and the range will switch to your measured figure.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Tile("Filled to full", false, Modifier.width(150.dp), height = 52.dp) { f.filledToFull(t?.odometerKm, t?.fuelPercent); status = "Full tank logged at ${t?.odometerKm} km" }
                Tile("Added 10 L", false, Modifier.width(130.dp), height = 52.dp) { f.addFuel(10.0, t?.odometerKm, t?.fuelPercent); status = "10 L logged" }
                Tile("Added 20 L", false, Modifier.width(130.dp), height = 52.dp) { f.addFuel(20.0, t?.odometerKm, t?.fuelPercent); status = "20 L logged" }
                Tile("Undo last", false, Modifier.width(120.dp), height = 52.dp) { f.deleteLast(); status = "Last fill removed" }
            }
            fills.takeLast(4).reversed().forEach { fl -> Text("${TripLog.day.format(Date(fl.time))} ${TripLog.time.format(Date(fl.time))}  ${if (fl.toFull) "full tank" else "${fl.litres} L"}  at ${fl.odometer} km", color = Shark.muted, fontSize = 12.sp) }
            Text("Tap Filled to full at the pump before you drive off; the gauge reading at that moment is what makes the measured figure accurate.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Panel("Trips") {
            if (trips.isEmpty()) Text("No trips yet. Drive somewhere with the app open.", color = Shark.muted, fontSize = 14.sp)
            trips.reversed().take(40).forEach { t -> TripRow(t) }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFFFD54F), fontSize = 14.sp)
    }
}

@Composable
private fun TripRow(t: TripLog.Trip) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${TripLog.day.format(Date(t.start))}  ${TripLog.time.format(Date(t.start))} to ${TripLog.time.format(Date(t.end))}", color = Shark.text, fontSize = 14.sp, modifier = Modifier.width(300.dp))
        Text("${t.km} km, ${t.evKm} on battery, ${t.minutes} min", color = Shark.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Tile(if (t.business) "Business" else "Private", t.business, Modifier.width(120.dp), height = 44.dp) { Vehicle.trips.setBusiness(t.id, !t.business) }
        Tile("Delete", false, Modifier.width(90.dp), height = 44.dp) { Vehicle.trips.delete(t.id) }
    }
}
