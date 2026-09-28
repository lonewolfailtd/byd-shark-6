package nz.lonewolf.shark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.data.TripLog
import nz.lonewolf.shark.service.VehicleService
import java.util.Date

/** Fuel in litres, with support for a long range tank the factory gauge cannot see. Nothing here writes to the ute. */
@Composable
fun FuelScreen() {
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val fills by Vehicle.fuel.fills.collectAsStateWithLifecycle()
    val f = Vehicle.fuel
    var extended by remember { mutableIntStateOf(f.extendedLitres) }
    var cal by remember { mutableIntStateOf(f.calibrationPct) }
    var basis by remember { mutableStateOf(f.basis) }
    var add by remember { mutableIntStateOf(0) }
    var bump by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }

    @Suppress("UNUSED_EXPRESSION") bump
    val measured = f.measuredConsumption()
    val use = measured ?: basis
    val total = f.totalLitres
    val left = f.remaining(t?.fuelPercent, t?.odometerKm, t?.evMileageKm, use)
    val percent = left?.let { (it / total * 100).toInt().coerceIn(0, 100) }
    val range = left?.let { (it / use * 100).toInt() }

    Backdrop("bg_fuel", wash = 0.3f) {
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(0.75f))
            Column(Modifier.weight(2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glass(Modifier.weight(1f)) {
                        Head("Total tank size"); Number("$total L")
                        Line("Stock tank", "60 L")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Extra tank", color = Shark.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Tile("−", false, Modifier.width(52.dp), height = 40.dp) { extended = (extended - 5).coerceAtLeast(0); f.extendedLitres = extended; bump++ }
                            Text("$extended L", color = Shark.text, fontSize = 16.sp, modifier = Modifier.width(54.dp))
                            Tile("+", false, Modifier.width(52.dp), height = 40.dp) { extended = (extended + 5).coerceAtMost(120); f.extendedLitres = extended; bump++ }
                        }
                    }
                    Glass(Modifier.weight(1f)) {
                        Head("Fuel remaining"); Number(if (left == null) "--" else "%.1f L".format(left), Shark.warm)
                        Bar(percent?.let { it / 100f }, Shark.warm)
                        Line("Of the full tank", fmt(percent, "%"))
                        Line("Factory gauge", fmt(t?.fuelPercent, "%"))
                    }
                    Glass(Modifier.weight(1f)) {
                        Head("Range"); Number("${fmt(range)} km", Shark.cool)
                        Line("On battery", fmt(t?.evRangeKm, " km"))
                        Line("BYD fuel range", fmt(t?.fuelRangeKm, " km"))
                        Line("Full tank", "${(total / use * 100).toInt()} km")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glass(Modifier.weight(1.3f)) {
                        Head("Add fuel")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tile("−5", false, Modifier.width(60.dp), height = 48.dp) { add = (add - 5).coerceAtLeast(0) }
                            Tile("−1", false, Modifier.width(60.dp), height = 48.dp) { add = (add - 1).coerceAtLeast(0) }
                            Text("$add L", color = Shark.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                            Tile("+1", false, Modifier.width(60.dp), height = 48.dp) { add = (add + 1).coerceAtMost(total) }
                            Tile("+5", false, Modifier.width(60.dp), height = 48.dp) { add = (add + 5).coerceAtMost(total) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Tile("Add to tank", add > 0, Modifier.weight(1f), height = 48.dp) {
                                if (add > 0) { f.addToTank(add.toDouble(), left, t?.odometerKm, t?.evMileageKm, t?.fuelPercent); status = "$add L added"; add = 0; bump++ }
                            }
                            Tile("Filled to full", false, Modifier.weight(1f), height = 48.dp) {
                                f.fillTank(t?.odometerKm, t?.evMileageKm, t?.fuelPercent); status = "Full tank logged at ${fmt(t?.odometerKm, " km")}"; bump++
                            }
                        }
                    }
                    Glass(Modifier.weight(1f)) {
                        Head("Fuel used"); Number(if (left == null) "--" else "%.1f L".format(total - left))
                        Text(if (f.tracking) "Tracked from your last fill" else "From the factory gauge", color = Shark.muted, fontSize = 12.sp)
                    }
                    Glass(Modifier.weight(1f)) {
                        Head("Consumption"); Number("%.1f".format(use))
                        Text(if (measured != null) "L/100 km, measured between fills" else "L/100 km, your setting", color = Shark.muted, fontSize = 12.sp)
                        if (measured == null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Tile("−", false, Modifier.width(56.dp), height = 38.dp) { basis = (basis - 0.5).coerceAtLeast(3.0); f.basis = basis }
                            Tile("+", false, Modifier.width(56.dp), height = 38.dp) { basis = (basis + 0.5).coerceAtMost(25.0); f.basis = basis }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glass(Modifier.weight(2f)) {
                        Head("Gauge calibration")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile("−", false, Modifier.width(60.dp), height = 46.dp) { cal = (cal - 1).coerceAtLeast(-20); f.calibrationPct = cal; bump++ }
                            Text("${if (cal > 0) "+" else ""}$cal%", color = Shark.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                            Tile("+", false, Modifier.width(60.dp), height = 46.dp) { cal = (cal + 1).coerceAtMost(20); f.calibrationPct = cal; bump++ }
                            Text("Nudges the litres worked out from the factory gauge. Fill up, compare with the pump, then adjust until they agree.", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Glass(Modifier.weight(1f)) {
                        Tile(if (confirmReset) "Tap again to reset" else "Reset fuel data", false, Modifier.fillMaxWidth(), height = 52.dp) {
                            if (!confirmReset) confirmReset = true else { confirmReset = false; f.reset(); status = "Fuel data reset"; bump++ }
                        }
                        Tile("Undo last fill", false, Modifier.fillMaxWidth().padding(top = 6.dp), height = 44.dp) { f.deleteLast(); status = "Last fill removed"; bump++ }
                    }
                }
                if (status.isNotBlank()) Text(status, color = Color(0xFFFFD54F), fontSize = 14.sp)
                Glass(Modifier.fillMaxWidth()) {
                    Head("How this works")
                    Text(
                        if (extended == 0) "With the stock tank the litres come straight from the factory gauge. Set an extra tank size if you have fitted a long range tank."
                        else "The factory gauge cannot see the extra tank, so after each fill the app counts fuel down from the distance driven on the engine and your consumption figure. Log every fill to keep it honest.",
                        color = Shark.muted, fontSize = 13.sp,
                    )
                    fills.takeLast(4).reversed().forEach { fl ->
                        Text("${TripLog.day.format(Date(fl.time))} ${TripLog.time.format(Date(fl.time))}  ${if (fl.toFull) "full tank" else "${fl.litres} L"}  at ${fl.odometer} km", color = Shark.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable private fun Head(text: String) = Text(text.uppercase(), color = Shark.muted, fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
@Composable private fun Number(text: String, colour: Color = Shark.text) = Text(text, color = colour, fontSize = 36.sp, fontWeight = FontWeight.Bold)
@Composable private fun Line(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Text(label, color = Shark.muted, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(value, color = Shark.text, fontSize = 13.sp)
}
