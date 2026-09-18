package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService

@Composable
fun TowingScreen() {
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val e by VehicleService.energy.collectAsStateWithLifecycle()
    val tr = e?.trailer
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    fun write(label: String, block: () -> nz.lonewolf.shark.core.byd.CommandResult) {
        scope.launch { status = withContext(Dispatchers.IO) { val r = block(); runCatching { VehicleService.energy.value = Vehicle.energy.read() }; if (r.ok) "$label done" else "$label: ${r.detail}" } }
    }
    val unit = t?.tyres?.unit ?: "kPa"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("Range with the load on") {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Gauge("Combined", fmt(t?.combinedRangeKm), t?.combinedRangeKm?.let { it / 800f }, unit = "km")
                Gauge("Fuel", fmt(t?.fuelRangeKm), t?.fuelRangeKm?.let { it / 800f }, unit = "km", colour = Shark.warm)
                Gauge("EV", fmt(t?.evRangeKm), t?.evRangeKm?.let { it / 100f }, unit = "km", colour = Shark.cool)
                Gauge("Battery", fmt(t?.soc, "%"), t?.soc?.let { it / 100f })
            }
            Text("Towing roughly halves range. Plan fuel stops from the fuel figure, not combined.", color = Shark.muted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("Tyres ($unit)", Modifier.weight(1f)) {
                StatRow("Front left", fmt(t?.tyres?.fl, " psi", 1))
                StatRow("Front right", fmt(t?.tyres?.fr, " psi", 1))
                StatRow("Rear left", fmt(t?.tyres?.rl, " psi", 1))
                StatRow("Rear right", fmt(t?.tyres?.rr, " psi", 1))
                Text("Loaded target: 36 psi front, 42 psi rear", color = Shark.muted, fontSize = 12.sp)
            }
            Panel("Tow mode (live)", Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tile(if (tr?.active == true) "Tow mode ON" else "Tow mode off", tr?.active == true, Modifier.weight(1f), height = 52.dp) { write("Tow mode") { Vehicle.energy.setTowMode(tr?.active != true) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Tile("Small", tr?.dragType == 1, Modifier.weight(1f), height = 52.dp) { write("Small trailer") { Vehicle.energy.setTrailerSize(1) } }
                    Tile("Medium", tr?.dragType == 2, Modifier.weight(1f), height = 52.dp) { write("Medium trailer") { Vehicle.energy.setTrailerSize(2) } }
                    Tile("Large", tr?.dragType == 3, Modifier.weight(1f), height = 52.dp) { write("Large trailer") { Vehicle.energy.setTrailerSize(3) } }
                }
                if (status.isNotBlank()) Text(status, color = androidx.compose.ui.graphics.Color(0xFFFFD54F), fontSize = 13.sp)
                StatRow("Tow mode", when (tr?.active) { true -> "ON"; false -> "off"; null -> "--" }, tr?.active)
                StatRow("Trailer size set", tr?.sizeName ?: "--")
                StatRow("Trailer lights check", when (tr?.lightCheck) { null -> "--"; 0 -> "not run"; 1 -> "passed"; else -> "state ${tr?.lightCheck}" })
                StatRow("Towing prohibited", when (tr?.towingProhibited) { null -> "--"; 0 -> "no"; else -> "yes (${tr?.towingProhibited})" })
                StatRow("Km in tow mode", fmt(tr?.modeMileage, " km"))
                StatRow("Size limits", "${fmt(tr?.smallLimit)} / ${fmt(tr?.middleLimit)} / ${fmt(tr?.largeLimit)} kg")
                Text("Tow mode arms itself 15 s after the 7 pin plug goes in. It locks Normal mode and disables 10 driver aids.", color = Shark.muted, fontSize = 12.sp)
            }
            Panel("Limits (Premium)", Modifier.weight(1f)) {
                StatRow("Braked trailer", "2,500 kg")
                StatRow("Unbraked trailer", "750 kg")
                StatRow("Tow ball", "250 kg")
                StatRow("GVM", "3,500 kg")
                StatRow("Payload", "790 kg")
                StatRow("Tow mode", "auto after 15 s on the 7 pin plug; locks Normal mode and disables 10 driver aids")
            }
            Panel("Before you go", Modifier.weight(1f)) {
                listOf("Chains crossed, breakaway cable on", "Trailer lights and brakes checked", "Mirrors set for the trailer", "Rear tyres to 42 psi", "Load 60/40 forward, ball weight 10%", "Fuel above half before the hills").forEach {
                    Text("• $it", color = Shark.text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
    }
}
