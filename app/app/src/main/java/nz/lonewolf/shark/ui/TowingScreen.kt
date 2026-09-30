package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
    Backdrop("bg_towing", wash = 0.15f, align = androidx.compose.ui.Alignment.BottomCenter) {
    var more by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TOWING", color = Shark.text, fontSize = 30.sp, letterSpacing = 4.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("GO FURTHER TOGETHER", color = Shark.muted, fontSize = 11.sp, letterSpacing = 3.sp)
            }
            Text("TRAILER", color = Shark.muted, fontSize = 11.sp, letterSpacing = 2.sp)
            Tile("Small", tr?.dragType == 1, Modifier.width(120.dp), height = 52.dp) { write("Small trailer") { Vehicle.energy.setTrailerSize(1) } }
            Tile("Medium", tr?.dragType == 2, Modifier.width(120.dp), height = 52.dp) { write("Medium trailer") { Vehicle.energy.setTrailerSize(2) } }
            Tile("Large", tr?.dragType == 3, Modifier.width(120.dp), height = 52.dp) { write("Large trailer") { Vehicle.energy.setTrailerSize(3) } }
            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
            Tile(if (tr?.active == true) "Tow mode ON" else "Tow mode off", tr?.active == true, Modifier.width(180.dp), height = 52.dp) { write("Tow mode") { Vehicle.energy.setTowMode(tr?.active != true) } }
        }
        Text(
            if (status.isNotBlank()) status else if (tr?.active == true) "Tow mode on. Trailer ${tr?.sizeName ?: "--"}. Locks Normal mode and disables 10 driver aids." else "Tow mode off. It arms itself 15 s after the 7 pin plug goes in.",
            color = if (status.isNotBlank()) androidx.compose.ui.graphics.Color(0xFFFFD54F) else Shark.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(250.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TowStat("Combined range", fmt(t?.combinedRangeKm, " km"), t?.combinedRangeKm?.let { it / 900f }, Shark.cool, Modifier.weight(1f))
            TowStat("Fuel range", fmt(t?.fuelRangeKm, " km"), t?.fuelRangeKm?.let { it / 800f }, Shark.warm, Modifier.weight(1f))
            TowStat("EV range", fmt(t?.evRangeKm, " km"), t?.evRangeKm?.let { it / 100f }, Shark.accent, Modifier.weight(1f))
            TowStat("Battery", fmt(t?.soc, "%"), t?.soc?.let { it / 100f }, Shark.accent, Modifier.weight(1f))
            Glass(Modifier.width(230.dp), pad = 10) {
                Text("VEHICLE TYRES", color = Shark.accent, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("FL ${fmt(t?.tyres?.fl, " psi", 1)}  FR ${fmt(t?.tyres?.fr, " psi", 1)}", color = Shark.text, fontSize = 15.sp)
                Text("RL ${fmt(t?.tyres?.rl, " psi", 1)}  RR ${fmt(t?.tyres?.rr, " psi", 1)}", color = Shark.text, fontSize = 15.sp)
            }
        }
        Tile(if (more) "Hide checklist" else "Checklist and limits", more, Modifier.width(220.dp), height = 44.dp) { more = !more }
        if (more) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel("Limits (Premium)", Modifier.weight(1f)) {
                StatRow("Braked trailer", "2,500 kg"); StatRow("Unbraked trailer", "750 kg"); StatRow("Tow ball", "250 kg"); StatRow("GVM", "3,500 kg"); StatRow("Payload", "790 kg")
                StatRow("Trailer lights check", when (tr?.lightCheck) { null -> "--"; 0 -> "not run"; 1 -> "passed"; else -> "state ${tr?.lightCheck}" })
                StatRow("Km in tow mode", fmt(tr?.modeMileage, " km"))
                StatRow("Size limits", "${fmt(tr?.smallLimit)} / ${fmt(tr?.middleLimit)} / ${fmt(tr?.largeLimit)} kg")
            }
            Panel("Before you go", Modifier.weight(1f)) {
                listOf("Chains crossed, breakaway cable on", "Trailer lights and brakes checked", "Mirrors set for the trailer", "Rear tyres to 42 psi", "Load 60/40 forward, ball weight 10%", "Fuel above half before the hills").forEach {
                    Text("• $it", color = Shark.text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
                Text("Towing roughly halves range. Plan fuel stops from the fuel figure, not combined.", color = Shark.muted, fontSize = 12.sp)
            }
        }
    }
    }
}

@Composable
private fun TowStat(label: String, value: String, fraction: Float?, colour: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Glass(modifier, pad = 10) {
        Text(label.uppercase(), color = Shark.accent, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(value, color = androidx.compose.ui.graphics.Color.White, fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Bar(fraction, colour)
    }
}
