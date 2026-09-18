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
